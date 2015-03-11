package poke.external;

import poke.server.queue.PerChannelQueue;
import eye.Comm.Request;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class ExternalClusterHandler extends SimpleChannelInboundHandler<eye.Comm.Request>  {
	
	
	private PerChannelQueue sq;
	

	public ExternalClusterHandler() {
	
	}
	public ExternalClusterHandler(PerChannelQueue q) {
		this.sq = q;
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Request msg)
			throws Exception {
		// TODO Auto-generated method stub
		sq.enqueueRequest(msg, null);
		
	}

	

}
