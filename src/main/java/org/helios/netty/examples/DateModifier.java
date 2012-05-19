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
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.DefaultChannelPipeline;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.serialization.ClassResolvers;
import org.jboss.netty.handler.codec.serialization.ObjectDecoder;
import org.jboss.netty.handler.codec.serialization.ObjectEncoder;

/**
 * <p>Title: DateModifier</p>
 * <p>Description: Example that shows a server that increments the client passed Date by a random value and returns it. </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.examples.DateModifier</code></p>
 */

public class DateModifier {
	public static void main(String[] args) {
		
		bootServer();
		clog("DateModifier Example");
		try {
			Executor bossPool = Executors.newCachedThreadPool();
			Executor workerPool = Executors.newCachedThreadPool();
			final ChannelFactory channelFactory = new NioClientSocketChannelFactory(bossPool, workerPool);
			ChannelPipelineFactory pipelineFactory = new ChannelPipelineFactory() {
			  public ChannelPipeline getPipeline() throws Exception {
			    return Channels.pipeline(
			      new ObjectEncoder(),
			      new ObjectDecoder(ClassResolvers.cacheDisabled(getClass().getClassLoader())),
			      new ClientDateHandler()			      
			    );
			  }
			};
			ClientBootstrap bootstrap = new ClientBootstrap(channelFactory);
			bootstrap.setPipelineFactory(pipelineFactory);
			InetSocketAddress addressToConnectTo = new InetSocketAddress("localhost", 8080);
			clog("Issuing Channel Connect...");
			ChannelFuture cf = bootstrap.connect(addressToConnectTo);
			clog("Waiting for Channel Connect...");
			cf.awaitUninterruptibly();
			Date dt = new Date();
			clog("Connected. Sending Date [" + dt + "]");
			Channel channel = cf.getChannel();
			channel.write(dt);
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
					new ObjectEncoder(),
					new ServerDateHandler()
				);
			};
		});

		// Bind and start to accept incoming connections.
		bootstrap.bind(new InetSocketAddress("0.0.0.0", 8080));
		// Telling the default logging to pipe down
		java.util.logging.LogManager.getLogManager().getLogger(DefaultChannelPipeline.class.getName()).setLevel(Level.SEVERE);
		slog("Listening on 8080");
	}
	
	static class ClientDateHandler extends SimpleChannelHandler {
		public void messageReceived(ChannelHandlerContext ctx,MessageEvent e) throws Exception {
			Date date = (Date)e.getMessage();
			clog("Hey Guys !  I got back a modified date ! [" + date + "]");
		}
	}
	
	static class ServerDateHandler extends SimpleChannelHandler {
		final Random random = new Random(System.nanoTime());
		public void messageReceived(ChannelHandlerContext ctx,MessageEvent e) throws Exception {
			Date date = (Date)e.getMessage();
			// Here's the REALLY important business service at the end of the pipeline
			long newTime = (date.getTime() + random.nextInt());
			Date newDate = new Date(newTime);
			slog("Hey Guys !  I got a date ! [" + date + "] and I modified it to [" + newDate + "]");
			// Send back the reponse
			Channel channel = e.getChannel();
			ChannelFuture channelFuture = Channels.future(e.getChannel());
			ChannelEvent responseEvent = new DownstreamMessageEvent(channel, channelFuture, newDate, channel.getRemoteAddress());
			ctx.sendDownstream(responseEvent);
			// But still send it upstream because there might be another handler
			super.messageReceived(ctx, e);
		}		
	}

}
