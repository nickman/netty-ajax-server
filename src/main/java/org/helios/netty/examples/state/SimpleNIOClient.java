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

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.handler.codec.string.StringDecoder;

/**
 * <p>Title: SimpleNIOClient</p>
 * <p>Description: Sample netty client to demonstrate custom executors, socket options and channel handler state</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.examples.state.SimpleNIOClient</code></p>
 */

public class SimpleNIOClient {
	/** The client bootstrap */
	protected final ClientBootstrap bootstrap;
	/** The server channel factory */
	protected final ChannelFactory channelFactory;
	/** The server channel pipeline factory */
	protected final ChannelPipelineFactory pipelineFactory;
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

	
	public static void clog(Object msg) {
		System.out.println("[Client][" + Thread.currentThread() + "]:" + msg);
	}
	
	/**
	 * Creates a new SimpleNIOClient
	 * @param options The channel options
	 * @param providers An array of {@link ChannelHandlerProvider}s
	 */
	public SimpleNIOClient(Map<String, Object> options, ChannelHandlerProvider...providers) {
		if(options!=null) {
			channelOptions.putAll(options);
		}		
		bossThreadFactory = new ExecutorThreadFactory("ClientBossPool", true);
		workerThreadFactory = new ExecutorThreadFactory("ClientWorkerPool", true);
		bossPool = Executors.newCachedThreadPool(bossThreadFactory);
		workerPool = Executors.newCachedThreadPool(workerThreadFactory);
		channelFactory = new NioClientSocketChannelFactory(bossPool, workerPool);
		pipelineFactory = new ChannelPipelineFactoryImpl(providers);
		bootstrap = new ClientBootstrap(channelFactory);
		bootstrap.setOptions(channelOptions);
		bootstrap.setPipelineFactory(pipelineFactory);
	}	
	
	/**
	 * Returns a channel bound to the specified server
	 * @param server The server name or ip address
	 * @param port The listening port
	 * @return a channel bound to the specified server
	 */
	public ChannelFuture connect(String server, int port) {
		InetSocketAddress sockAddr = new InetSocketAddress(server, port);		
		return bootstrap.connect(sockAddr);
		
	}

}
