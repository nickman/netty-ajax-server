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

import java.util.LinkedList;

import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;

/**
 * <p>Title: ChannelPipelineFactoryImpl</p>
 * <p>Description: A channel pipeline factory  that scopes {@link ChannelHandler}s for shareability.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.examples.state.ChannelPipelineFactoryImpl</code></p>
 */

public class ChannelPipelineFactoryImpl implements ChannelPipelineFactory {
	/** A linked list of channel handler providers */
	protected final LinkedList<ChannelHandlerProvider> handlers = new LinkedList<ChannelHandlerProvider>();
	
	/**
	 * Creates a new ChannelPipelineFactoryImpl
	 */
	public ChannelPipelineFactoryImpl() {
		
	}
	
	/**
	 * Creates a new ChannelPipelineFactoryImpl
	 * @param providers An array of providers that will be added in the order specified
	 */
	public ChannelPipelineFactoryImpl(ChannelHandlerProvider... providers) {
		if(providers!=null) {
			for(ChannelHandlerProvider provider: providers) {
				if(provider==null) continue;
				handlers.addLast(provider);
			}
		}
	}
	
	
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelPipelineFactory#getPipeline()
	 */
	@Override
	public ChannelPipeline getPipeline() throws Exception {
		ChannelPipeline pipeline = Channels.pipeline();
		for(ChannelHandlerProvider provider: handlers) {
			pipeline.addLast(provider.getHandlerName(), provider.getHandler());
		}
		return pipeline;
	}
	
	/**
	 * Adds a {@link ChannelHandlerProviderFactory} that will create the specified type of {@link ChannelHandler} to the end of created pipelines.
	 * @param name The name of the channel handler instance when it is placed in the pipeline
	 * @param handlerClass The channel handler class
	 * @param args The arguments to pass to the channel handler's constructor when creating a new instance
	 */
	public void addLast(String name, Class<ChannelHandler> handlerClass, Object...args) {		
		handlers.addLast(ChannelHandlerProviderFactory.getInstance(name, handlerClass, args));
	}
	
	/**
	 * Adds a {@link ChannelHandlerProviderFactory} that will create the specified type of {@link ChannelHandler} to the start of created pipelines.
	 * @param name The name of the channel handler instance when it is placed in the pipeline
	 * @param handlerClass The channel handler class
	 * @param args The arguments to pass to the channel handler's constructor when creating a new instance
	 */
	public void addFirst(String name, Class<ChannelHandler> handlerClass, Object...args) {
		handlers.addFirst(ChannelHandlerProviderFactory.getInstance(name, handlerClass, args));
	}
	
	/**
	 * Adds a {@link ChannelHandler}  to the end of created pipelines.
	 * @param name The name of the channel handler instance when it is placed in the pipeline
	 * @param handler The channel handler 
	 */
	public void addLast(String name, ChannelHandler handler) {		
		handlers.addLast(ChannelHandlerProviderFactory.getInstance(name, handler));
	}
	
	/**
	 * Adds a {@link ChannelHandler}  to the start  of  created pipelines.
	 * @param name The name of the channel handler instance when it is placed in the pipeline
	 * @param handler The channel handler 
	 */
	public void addFirst(String name, ChannelHandler handler) {		
		handlers.addFirst(ChannelHandlerProviderFactory.getInstance(name, handler));
	}

}
