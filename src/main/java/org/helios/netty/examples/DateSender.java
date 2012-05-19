/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2007, Helios Development Group and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org. 
 *
 */
package org.helios.netty.examples;

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.serialization.ClassResolvers;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;
import org.jboss.netty.logging.JdkLoggerFactory;

/**
 * <p>Title: DateSender</p>
 * <p>Description: Simple DateSender example for tutorial</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.examples.DateSender</code></p>
 */

public class DateSender {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		bootServer();
		clog("DateSender Example");
		try {
			Executor bossPool = Executors.newCachedThreadPool();
			Executor workerPool = Executors.newCachedThreadPool();
			final ChannelFactory channelFactory = new NioClientSocketChannelFactory(bossPool, workerPool);
			ChannelPipelineFactory pipelineFactory = new ChannelPipelineFactory() {
			  public ChannelPipeline getPipeline() throws Exception {
			    return Channels.pipeline(
			      new ObjectEncoder()
			    );
			  }
			};
			ClientBootstrap bootstrap = new ClientBootstrap(channelFactory);
			bootstrap.setPipelineFactory(pipelineFactory);
			// Phew. Ok. We built all that. Now what ?
			InetSocketAddress addressToConnectTo = new InetSocketAddress("localhost", 8080);
			//ChannelFuture cf = bootstrap.connect(addressToConnectTo);
			clog("Issuing Channel Connect...");
			// Waiting on a connect. (Pick one)
			ChannelFuture cf = bootstrap.connect(addressToConnectTo);
			// wait interruptibly
//			cf.await();
			// wait interruptibly with a timeout of 2000 ms.
//			cf.await(2000, TimeUnit.MILLISECONDS);
			// wait uninterruptibly
			clog("Waiting for Channel Connect...");
			cf.awaitUninterruptibly();
			// wait uninterruptibly with a timeout of 2000 ms.
//			cf.awaitUninterruptibly(2000, TimeUnit.MILLISECONDS);
			// add a ChannelFutureListener that writes the Date when the connect is complete
//			cf.addListener(new ChannelFutureListener(){
//				public void operationComplete(ChannelFuture future) throws Exception {
//					// chek to see if we succeeded
//					if(future.isSuccess()) {
//						Channel channel = future.getChannel();
//						channel.write(new Date());
//						// remember, the write is asynchronous too !
//					}
//				}
//			});
			// if a wait option was selected and the connect did not fail,
			// the Date can now be sent.
			clog("Connected. Sending Date");
			Channel channel = cf.getChannel();
			channel.write(new Date());
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
	
	
	
	public static void clog(Object msg) {
		System.out.println("[Client]:" + msg);
	}
	
	public static void slog(Object msg) {
		System.out.println("[Server]:" + msg);
	}
	
	public static void bootServer() {
		// More terse code to setup the server
		ServerBootstrap bootstrap = new ServerBootstrap(
				new NioServerSocketChannelFactory(
						Executors.newCachedThreadPool(),
						Executors.newCachedThreadPool()));

		// Set up the pipeline factory.
		bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
			public ChannelPipeline getPipeline() throws Exception {
				return Channels.pipeline(
					new ObjectDecoder(ClassResolvers.cacheDisabled(getClass().getClassLoader())),
					new SimpleChannelHandler() {
						public void messageReceived(ChannelHandlerContext ctx,MessageEvent e) throws Exception {
							Date date = (Date)e.getMessage();
							slog("Hey Guys !  I got a date ! [" + date + "]");
							super.messageReceived(ctx, e);
						}
					}
				);
			};
		});

		// Bind and start to accept incoming connections.
		bootstrap.bind(new InetSocketAddress("0.0.0.0", 8080));
		slog("Listening on 8080");
	}
}



