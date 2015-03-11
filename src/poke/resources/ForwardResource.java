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
package poke.resources;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import poke.client.comm.CommConnection;
import poke.client.comm.CommHandler;
import poke.client.comm.CommListener;
import poke.loadBalancer.RequestCountMap;
import poke.server.ServerInitializer;
import poke.server.conf.NodeDesc;
import poke.server.conf.ServerConf;
import poke.server.managers.ConnectionManager;
import poke.server.managers.HeartbeatData;
import poke.server.managers.HeartbeatManager;
import poke.server.resources.Resource;
import poke.server.resources.ResourceUtil;
import eye.Comm.Header;
import eye.Comm.JobProposal;
import eye.Comm.Management;
import eye.Comm.MgmtHeader;
import eye.Comm.Network;
import eye.Comm.Payload;
import eye.Comm.PhotoHeader;
import eye.Comm.PhotoPayload;
import eye.Comm.PokeStatus;
import eye.Comm.Request;
import eye.Comm.RoutingPath;
import eye.Comm.Header.Routing;
import eye.Comm.Network.NetworkAction;
import eye.Comm.PhotoHeader.ResponseFlag;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;

/**
 * The forward resource is used by the ResourceFactory to send requests to a
 * destination that is not this server.
 * 
 * Strategies used by the Forward can include TTL (max hops), durable tracking,
 * endpoint hiding.
 * 
 * @author gash
 * 
 */
public class ForwardResource implements Resource {
	protected static Logger logger = LoggerFactory.getLogger("server");
	private static Map<Integer, Integer> requestCountMap = new HashMap<Integer, Integer>();

	private static ServerConf cfg;

	public ServerConf getCfg() {
		return cfg;
	}

	/**
	 * Set the server configuration information used to initialized the server.
	 * 
	 * @param cfg
	 */
	public void setCfg(ServerConf cfg) {
		this.cfg = cfg;
	}

	@Override
	public Request process(Request request) {
		System.out.println("Inside Forward Resource Method______________");
		Integer nextNode = determinenext();
		
		System.out.println("Next node_________" + nextNode);
		if (nextNode != null && nextNode != -1) {
			Request fwd = ResourceUtil.buildForwardMessage(request, cfg,
					nextNode);
			return fwd;

		} else {
			Request reply = null;
			// cannot forward the message - no one to forward request to as
			// the request has traveled all known/available edges of this node
			String statusMsg = "Unable to forward message, no paths or have already traversed";
			
			Request rtn = ResourceUtil.buildError(request.getHeader(),
					PokeStatus.NOREACHABLE, statusMsg);
			
			return rtn;
		}
	}

	/**
	 * Find the nearest node that has not received the request.
	 * 
	 * TODO this should use the heartbeat to determine which node is active in
	 * its list.
	 * 
	 * @param request
	 * @return
	 */

	/*
	 * this is main load balancing method which considers two criteria to decide load on workers.
	 * 1. current system usage of system as different system might have different capacity 
	 * 2. current request load in terms if unsatisfied request count 
	 * 
	 * */
	private Integer determinenext() {
		
		int reqcount = 1;
		int nextNode = -1;
		float maxCapacity = 0;
		float avgCapacity = 0;
		float availCapacity;
		for (HeartbeatData hd : HeartbeatManager.getInstance().outgoingHB
				.values()) {

			if (hd.getNodeId() == ServerConf.getInstance().getNodeId()) {
				continue;
			}
			
			reqcount = RequestCountMap.getReqCntMap().get(hd.getNodeId())+1;
			availCapacity = 100 - hd.getSystemUsage(); 
			avgCapacity = availCapacity/reqcount;
			if (maxCapacity < avgCapacity) {
				
				maxCapacity = avgCapacity;
				nextNode = hd.getNodeId();
			}
		}

		return nextNode;
	}

	
	public static void initialize(ServerConf conf) {
		ForwardResource.cfg = conf;

	}

}