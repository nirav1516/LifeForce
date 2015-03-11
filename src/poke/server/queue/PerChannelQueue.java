/*
 * copyright 2014, gash
 * 
 * Gash licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package poke.server.queue;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;

import java.beans.Beans;
import java.lang.Thread.State;
import java.util.concurrent.LinkedBlockingDeque;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.client.comm.CommHandler;
import poke.connection.CreateChannel;
import poke.external.ClusterDBServiceImplementation;
import poke.external.ClusterMapperManager;
import poke.external.ClusterMapperStorage;
import poke.external.DbConfigurations;
import poke.external.ExternalClusterHandler;
import poke.server.conf.NodeDesc;
import poke.server.conf.ServerConf;
import poke.server.managers.ConnectionManager;
import poke.server.managers.ElectionManager;
import poke.server.resources.Resource;
import poke.server.resources.ResourceFactory;
import poke.server.resources.ResourceUtil;

import com.google.protobuf.GeneratedMessage;

import eye.Comm.PhotoHeader;
import eye.Comm.PhotoHeader.ResponseFlag;
import eye.Comm.Header;
import eye.Comm.PokeStatus;
import eye.Comm.Request;
import eye.Comm.RoutingPath;

/**
 * A server queue exists for each connection (channel). A per-channel queue
 * isolates clients. However, with a per-client model. The server is required to
 * use a master scheduler/coordinator to span all queues to enact a QoS policy.
 * 
 * How well does the per-channel work when we think about a case where 1000+
 * connections?
 * 
 * @author gash
 * 
 */
public class PerChannelQueue implements ChannelQueue {
	protected static Logger logger = LoggerFactory.getLogger("server");

	private Channel channel;

	// The queues feed work to the inbound and outbound threads (workers). The
	// threads perform a blocking 'get' on the queue until a new event/task is
	// enqueued. This design prevents a wasteful 'spin-lock' design for the
	// threads
	private LinkedBlockingDeque<com.google.protobuf.GeneratedMessage> inbound;
	private LinkedBlockingDeque<com.google.protobuf.GeneratedMessage> outbound;

	// This implementation uses a fixed number of threads per channel
	private OutboundWorker oworker;
	private InboundWorker iworker;

	// Change: For reusing the same request at the time of forwarding.
	private Request myRequest;
	// not the best method to ensure uniqueness
	private ThreadGroup tgroup = new ThreadGroup("ServerQueue-"
			+ System.nanoTime());

	protected PerChannelQueue(Channel channel) {
		System.out.println("per channel queue created " );
		this.channel = channel;
		init();
	}

	protected void init() {
		inbound = new LinkedBlockingDeque<com.google.protobuf.GeneratedMessage>();
		outbound = new LinkedBlockingDeque<com.google.protobuf.GeneratedMessage>();

		iworker = new InboundWorker(tgroup, 1, this);
		iworker.start();

		oworker = new OutboundWorker(tgroup, 1, this);
		oworker.start();

		// let the handler manage the queue's shutdown
		// register listener to receive closing of channel
		// channel.getCloseFuture().addListener(new CloseListener(this));
	}

	protected Channel getChannel() {
		return channel;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see poke.server.ChannelQueue#shutdown(boolean)
	 */
	@Override
	public void shutdown(boolean hard) {
		logger.info("server is shutting down");

		channel = null;

		if (hard) {
			// drain queues, don't allow graceful completion
			inbound.clear();
			outbound.clear();
		}

		if (iworker != null) {
			iworker.forever = false;
			if (iworker.getState() == State.BLOCKED
					|| iworker.getState() == State.WAITING)
				iworker.interrupt();
			iworker = null;
		}

		if (oworker != null) {
			oworker.forever = false;
			if (oworker.getState() == State.BLOCKED
					|| oworker.getState() == State.WAITING)
				oworker.interrupt();
			oworker = null;
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see poke.server.ChannelQueue#enqueueRequest(eye.Comm.Finger)
	 */
	@Override
	public void enqueueRequest(Request req, Channel notused) {
		try {
			inbound.put(req);
		} catch (InterruptedException e) {
			logger.error("message not enqueued for processing", e);
		}
	}

	boolean forever = true;

	/*
	 * (non-Javadoc)
	 * 
	 * @see poke.server.ChannelQueue#enqueueResponse(eye.Comm.Response)
	 */
	@Override
	public void enqueueResponse(Request reply, Channel notused) {
		if (reply == null)
			return;

		try {
			System.out.println("output in enqeueue + " +  reply);
			outbound.put(reply);
		} catch (InterruptedException e) {
			logger.error("message not enqueued for reply", e);
		}

	}

	// For forwarding in case of failure from insternal cluster
	public void forwardToOtherCluster(Request reply) {
		final PerChannelQueue pq = this;
		boolean success = reply.getHeader().getPhotoHeader().getResponseFlag() == ResponseFlag.success;
		//if success then return reply to client
		if (success) {
			try {
				System.out.println("Inside enqueRes");
				enqueueResponse(reply, null);

			} catch (Exception e) {
				logger.error("message not enqueued for reply", e);
			}

		}else {
			
			/* first check for list of entry node which consists of clusters from which request has already passed through
			 * this is neccesary because we need to forward request to only that cluster from which request has not 
			 * passed through. 
			 * 
			 * If request is from client then we add our cluster id to the list an forward if it is not fulfilled locally
			 * 
			 * 
			 */ 
			String visitedNode;
			if (reply.getHeader().getPhotoHeader().hasEntryNode()) {
			
				visitedNode = reply.getHeader().getPhotoHeader().getEntryNode();
				visitedNode += ","
						+ String.valueOf(DbConfigurations.getClusterId());
             
			} else {
				visitedNode = String.valueOf(DbConfigurations.getClusterId());
             
			}
			//get details of next cluster to which we can send request 
			ClusterMapperStorage nextClusterLeader = ClusterMapperManager

			.getClusterDetails(visitedNode);

			if (nextClusterLeader != null) {

			
				Request.Builder bldr = Request.newBuilder(myRequest);

				Header.Builder hbldr = bldr.getHeaderBuilder();
				PhotoHeader.Builder phb = hbldr.getPhotoHeaderBuilder();
				phb.setEntryNode(visitedNode);
				hbldr.setPhotoHeader(phb.build());
				Request req = bldr.build();
				// Forwarding request to the cluster Leader
				ExternalClusterHandler handler = new ExternalClusterHandler(pq);
				Channel ch = CreateChannel.buildChannel(
						nextClusterLeader.getLeaderHostAddress(),
						nextClusterLeader.getPort(), handler);
				ch.writeAndFlush(bldr.build());

			} else {

				// all leaders have seen the request
				// - set failure is done - just
				// enqueue the response

				enqueueResponse(reply, null);

			}

		}
	}

	protected class OutboundWorker extends Thread {
		int workerId;
		PerChannelQueue sq;
		boolean forever = true;

		public OutboundWorker(ThreadGroup tgrp, int workerId, PerChannelQueue sq) {
			super(tgrp, "outbound-" + workerId);
			this.workerId = workerId;
			this.sq = sq;

			if (outbound == null)
				throw new RuntimeException(
						"connection worker detected null queue");
		}

		@Override
		public void run() {
			Channel conn = sq.channel;

			if (conn == null || !conn.isOpen()) {
				PerChannelQueue.logger
						.error("connection missing, no outbound communication");
				return;
			}

			while (true) {
				if (!forever && sq.outbound.size() == 0)
					break;

				try {
					// block until a message is enqueued
					GeneratedMessage msg = sq.outbound.take();
					System.out.println("Received Message" + msg);
					if (conn.isWritable()) {
						boolean rtn = false;
						System.out.println("Channel_______"
								+ channel.localAddress());
						if (channel != null && channel.isOpen()
								&& channel.isWritable()) {
							System.out.println("Channel is not null");
							ChannelFuture cf = channel.writeAndFlush(msg);
							
							cf.awaitUninterruptibly();

							rtn = cf.isSuccess();
							System.out.println("RTN___________" + rtn);
							if (!rtn)
								sq.outbound.putFirst(msg);
						}else{
							System.out.println("channel is closed for outbound queue in per channel qurue");
						}

					} else {
						System.out.println("Inside Else");
						sq.outbound.putFirst(msg);
					}
				} catch (InterruptedException ie) {
					break;
				} catch (Exception e) {
					PerChannelQueue.logger.error(
							"Unexpected communcation failure", e);
					break;
				}
			}

			if (!forever) {
				PerChannelQueue.logger.info("connection queue closing");
			}
		}
	}

	protected class InboundWorker extends Thread {
		int workerId;
		PerChannelQueue sq;
		boolean forever = true;

		public InboundWorker(ThreadGroup tgrp, int workerId, PerChannelQueue sq) {
			super(tgrp, "inbound-" + workerId);
			this.workerId = workerId;
			this.sq = sq;

			if (outbound == null)
				throw new RuntimeException(
						"connection worker detected null queue");
		}

		@Override
		public void run() {
			Channel conn = sq.channel;
			if (conn == null || !conn.isOpen()) {
				PerChannelQueue.logger
						.error("connection missing, no inbound communication");
				return;
			}

			while (true) {
				if (!forever && sq.inbound.size() == 0)
					break;

				try {
					// block until a message is enqueued
					GeneratedMessage msg = sq.inbound.take();
					int leaderId = -1;
					// process request and enqueue response
					if (msg instanceof Request) {
						Request req = ((Request) msg);
						sq.myRequest = req; // Change: Setting my request for
											// the purpose of forwarding
						if (ElectionManager.getInstance().whoIsTheLeader() != null) {
							leaderId = ElectionManager.getInstance()
									.whoIsTheLeader();
						}
						ServerConf cfg = ResourceFactory.getInstance().getCfg();
						if (cfg.getNodeId() == leaderId) {
							//if current node is acting as load balancer than it will not server request as far as
							//it can forward request to any worker node
							try {
								logger.info("I am the leader....I won't work....Going to forwar the request");
								Resource rsc = (Resource) Beans.instantiate(
										this.getClass().getClassLoader(),
										cfg.getForwardingImplementation());
								Request reply = rsc.process(req);

								if (reply.getHeader().getReplyCode() == PokeStatus.NOREACHABLE) {
									// Incase when no worker node is the in the
									// cluster.
									logger.info("Coudn't find any node to forward the request");
									handleLocally(sq, req);
								} else {
									// If a node is found in the neighbour
									int nodeId = reply.getHeader().getToNode();
									NodeDesc nextNode = cfg.getAdjacent()
											.getAdjacentNodes().get(nodeId);
									CommHandler handler = new CommHandler(sq);
									Channel ch = null;
									if(ConnectionManager.getConnection(nodeId, false) == null){
								    //will make new channel to only if currently there is no connected channel to the worker node 		
									 ch = CreateChannel.buildChannel(
											nextNode.getHost(),
											nextNode.getPort(), handler);
									 System.out.println("could not find any channel so making new one and chammel is " + ch);
									ConnectionManager.addConnection(nodeId, ch, false);
									}else{
									 //will get the channel from connection manager if channel is already established with the node 
									 ch = ConnectionManager.getConnection(nodeId, false);
									 ch.pipeline().addLast(new CommHandler(sq));
									
									}
									ch.writeAndFlush(reply);
									
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
						// handle it locally
						else {
							handleLocally(sq, req);
						}
					}

				} catch (InterruptedException ie) {
					break;
				} catch (Exception e) {
					PerChannelQueue.logger.error(
							"Unexpected processing failure", e);
					break;
				}
			}

			if (!forever) {
				PerChannelQueue.logger.info("connection queue closing");
			}
		}
		//if no worker nodes are found then load balancer handle is locally
		public void handleLocally(PerChannelQueue sq, Request req) {
			Resource rsc = ResourceFactory.getInstance().resourceInstance(
					req.getHeader());
			System.out.println("Resource_______" + rsc);
			Request reply = null;
			if (rsc == null) {
				logger.error("failed to obtain resource for " + req);
				reply = ResourceUtil.buildError(req.getHeader(),
						PokeStatus.NORESOURCE, "Request not processed");
			} else {
				System.out.println("Resource obtained is" + rsc);
				reply = rsc.process(req);
			}
			//if request is not satisfied in internal cluster then request is sent to outer cluster
			sq.forwardToOtherCluster(reply);
		}

	}

	public class CloseListener implements ChannelFutureListener {
		private ChannelQueue sq;

		public CloseListener(ChannelQueue sq) {
			this.sq = sq;
		}

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			sq.shutdown(true);
		}
	}
}
