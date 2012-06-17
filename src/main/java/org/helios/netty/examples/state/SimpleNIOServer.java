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
package org.helios.netty.examples.state;

import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import org.helios.netty.examples.jmx.BootstrapJMXManager;
import org.helios.netty.jmx.JMXHelper;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.ServerSocketChannel;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.serialization.CompatibleObjectEncoder;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;

/**
 * <p>Title: SimpleNIOServer</p>
 * <p>Description: Sample netty server to demonstrate custom executors, socket options and channel handler state</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.examples.state.SimpleNIOServer</code></p>
 */

public class SimpleNIOServer {
	/** The server bootstrap */
	protected final CustomServerBootstrap bootstrap;
	/** The server channel factory */
	protected final ChannelFactory channelFactory;
	/** The server channel pipeline factory */
	protected final ChannelPipelineFactory pipelineFactory;
	/** The inet socket address to listen on */
	protected final InetSocketAddress listenerSocket;
	/** The channel options */
	protected final Map<String, Object> channelOptions = new HashMap<String, Object>();
	/** The boss thread pool */
	protected final Executor bossPool;
	/** The worker thread pool */
	protected final Executor workerPool;
	/** The boss thread pool thread factory */
	protected final ThreadFactory  bossThreadFactory;
	/** The worker thread pool thread factory */
	protected final ThreadFactory  workerThreadFactory;
	/** Indicates if the server is started */
	protected AtomicBoolean started = new AtomicBoolean(false);
	
	
	
	/**
	 * Creates a new SimpleNIOServer
	 * @param port The port to listen on
	 * @param options The channel options
	 * @param providers An array of {@link ChannelHandlerProvider}s
	 */
	public SimpleNIOServer(int port, Map<String, Object> options, ChannelHandlerProvider...providers) {
		if(options!=null) {
			channelOptions.putAll(options);
		}
		listenerSocket = new InetSocketAddress("0.0.0.0", port);
		bossThreadFactory = new ExecutorThreadFactory("BossPool-[" + port + "]", true);
		workerThreadFactory = new ExecutorThreadFactory("WorkerPool-[" + port + "]", true);
		bossPool = Executors.newCachedThreadPool(bossThreadFactory);
		workerPool = Executors.newCachedThreadPool(workerThreadFactory);
		channelFactory = new NioServerSocketChannelFactory(bossPool, workerPool);
		pipelineFactory = new ChannelPipelineFactoryImpl(providers);
		bootstrap = new CustomServerBootstrap(channelFactory);
		bootstrap.setOptions(channelOptions);
		bootstrap.setPipelineFactory(pipelineFactory);
		new BootstrapJMXManager(bootstrap, "org.helios.netty:service=ServerBootstrap,name=SimpleNIOServer");
	}
	
	/**
	 * Starts this SimpleNIOServer instance
	 */
	public void start() {
		bootstrap.bind(listenerSocket);
		started.set(true);
		slog("Listening on 8080. PID:" + ManagementFactory.getRuntimeMXBean().getName().split("@")[0]); 
	}
	
	/**
	 * Stops this SimpleNIOServer instance
	 */
	public void stop() {		
		bootstrap.releaseExternalResources();
		started.set(false);
	}
	
	
//	ServerBootstrap bootstrap = new ServerBootstrap(
//			new NioServerSocketChannelFactory(
//					Executors.newCachedThreadPool(),
//					Executors.newCachedThreadPool()));

	// Set up the pipeline factory.
//	bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
//		public ChannelPipeline getPipeline() throws Exception {
//			return Channels.pipeline(
//				new ObjectDecoder(ClassResolvers.cacheDisabled(getClass().getClassLoader())),
//				new SimpleChannelHandler() {
//					public void messageReceived(ChannelHandlerContext ctx,MessageEvent e) throws Exception {
//						Date date = (Date)e.getMessage();
//						slog("Hey Guys !  I got a date ! [" + date + "]");
//						super.messageReceived(ctx, e);
//					}
//				}
//			);
//		};
//	});
//
//	// Bind and start to accept incoming connections.
//	bootstrap.bind(new InetSocketAddress("0.0.0.0", 8080));
//	slog("Listening on 8080");
//
	
	public static void slog(Object msg) {
		System.out.println("[Server][" + Thread.currentThread() + "]:" + msg);
	}

}
