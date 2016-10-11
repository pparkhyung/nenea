package nenea.client.operation;

/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
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

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * Connects to a server periodically to measure and print the uptime of the
 * server. This example demonstrates how to implement reliable reconnection
 * mechanism in Netty.
 */
public final class UptimeClient {

	static final boolean SSL = System.getProperty("ssl") != null;
	static final String HOST = System.getProperty("host", "127.0.0.1");
	static final int PORT = Integer.parseInt(System.getProperty("port", SSL ? "6443" : "6443"));

	// Sleep 5 seconds before a reconnection attempt.
	static final int RECONNECT_DELAY = Integer.parseInt(System.getProperty("reconnectDelay", "10"));
	// Reconnect when the server sends nothing for 100 seconds.
	static final int READ_TIMEOUT = Integer.parseInt(System.getProperty("readTimeout", "300"));

	// handler
	private static final UptimeClientHandler uptimeHandler = new UptimeClientHandler();
	private static final OperationClientHandler operationhandler = new OperationClientHandler();

	public static void start() throws Exception {
			configureBootstrap(new Bootstrap()).connect();
	}

	private static Bootstrap configureBootstrap(Bootstrap b) throws Exception {
		return configureBootstrap(b, new NioEventLoopGroup());
	}

	static Bootstrap configureBootstrap(Bootstrap b, EventLoopGroup g) throws Exception {
		
		 System.out.println("ssl 정보: " + SSL + ", port : " +PORT);

		// Configure SSL.
		final SslContext sslCtx;
		if (SSL) {
			// 인증서 검증 없음. 모든 x.509인증서를 신뢰함 
			sslCtx = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
			//sslCtx = SslContextBuilder.forClient().trustManager(SimpleTrustManagerFactory.getInstance("nene")).build();
		} else {
			sslCtx = null;
		}

		b.group(g).channel(NioSocketChannel.class).remoteAddress(HOST, PORT)
				.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					public void initChannel(SocketChannel ch) throws Exception {
						ChannelPipeline p = ch.pipeline();
						if (sslCtx != null) {
							p.addLast(sslCtx.newHandler(ch.alloc(), HOST, PORT));
						}
						p.addLast(
								// IdleStateHandler는 @sharable이 아니라 new 해줘야함.
								new IdleStateHandler(READ_TIMEOUT, 0, 0), uptimeHandler, operationhandler);
					}
				});

		return b;
	}

	static void connect(Bootstrap b) {
		b.connect().addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				if (future.cause() != null) {
					uptimeHandler.startTime = -1;
					uptimeHandler.println("Failed to connect: " + future.cause());
				}
			}
		});
	}
}