package poke.connection;

import poke.external.ExternalClusterHandler;
import poke.server.managers.ConnectionManager;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
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

public class CreateChannel {
//creates and returns  new channel with given host and port and sets its handler with given one.
	public static Channel buildChannel(String host,Integer port,final SimpleChannelInboundHandler<eye.Comm.Request> handler){
		EventLoopGroup group = new NioEventLoopGroup();

		Bootstrap b = new Bootstrap();
		b.group(group).channel(NioSocketChannel.class).handler(
						new ChannelInitializer<SocketChannel>() {
							@Override
							protected void initChannel(
									SocketChannel ch)
									throws Exception {
							
								ChannelPipeline pipeline = ch
										.pipeline();
								pipeline.addLast(
										"frameDecoder",
										new LengthFieldBasedFrameDecoder(
												67108864,
												0,
												4,
												0,
												4));
								pipeline.addLast(
										"protobufDecoder",
										new ProtobufDecoder(
												eye.Comm.Request
														.getDefaultInstance()));
								pipeline.addLast(
										"frameEncoder",
										new LengthFieldPrepender(
												4));
								pipeline.addLast(
										"protobufEncoder",
										new ProtobufEncoder());
								
								pipeline.addLast("handler",handler);
							}
						});
		b.option(
				ChannelOption.CONNECT_TIMEOUT_MILLIS,
				10000);
		b.option(ChannelOption.TCP_NODELAY, true);
		b.option(ChannelOption.SO_KEEPALIVE, true);
		

		ChannelFuture cf=null;
		
			cf = b.connect(host,port).awaitUninterruptibly();
			return cf.channel();
	}
	
}
