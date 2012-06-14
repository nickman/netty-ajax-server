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

import static org.jboss.netty.channel.Channels.failedFuture;

import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.BlockingQueue;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.ChildChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

/**
 * <p>Title: CustomServerBootstrapBinder</p>
 * <p>Description: Bootstrap binder customized to read options for child channels straight from the bootstrap</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.examples.CustomServerBootstrapBinder</code></p>
 */
public class CustomServerBootstrapBinder extends SimpleChannelUpstreamHandler  {
	private final ServerBootstrap bootstrap;
    private final SocketAddress localAddress;
    private final BlockingQueue<ChannelFuture> futureQueue;
    

  public  CustomServerBootstrapBinder(ServerBootstrap bootstrap, SocketAddress localAddress, BlockingQueue<ChannelFuture> futureQueue) {
        this.localAddress = localAddress;
        this.futureQueue = futureQueue;
        this.bootstrap = bootstrap;
    }

    @Override
    public void channelOpen(
            ChannelHandlerContext ctx,
            ChannelStateEvent evt) {

        try {
            evt.getChannel().getConfig().setPipelineFactory(bootstrap.getPipelineFactory());

            // Split options into two categories: parent and child.
            Map<String, Object> allOptions = bootstrap.getOptions();
            Map<String, Object> parentOptions = new HashMap<String, Object>();
            for (Entry<String, Object> e: allOptions.entrySet()) {
                if (e.getKey().startsWith("child.")) {
                } else if (!e.getKey().equals("pipelineFactory")) {
                    parentOptions.put(e.getKey(), e.getValue());
                }
            }

            // Apply parent options.
            evt.getChannel().getConfig().setOptions(parentOptions);
        } finally {
            ctx.sendUpstream(evt);
        }

        boolean finished = futureQueue.offer(evt.getChannel().bind(localAddress));
        assert finished;
    }

    @Override
    public void childChannelOpen(
            ChannelHandlerContext ctx,
            ChildChannelStateEvent e) throws Exception {
        // Apply child options.
        try {            
            for (Entry<String, Object> eopt: bootstrap.getOptions().entrySet()) {
                if (eopt.getKey().startsWith("child.")) {
                	 e.getChildChannel().getConfig().setOption(eopt.getKey().substring(6), eopt.getValue());
                }            
            }
        	
           
        } catch (Throwable t) {
            Channels.fireExceptionCaught(e.getChildChannel(), t);
        }
        ctx.sendUpstream(e);
    }

    @Override
    public void exceptionCaught(
            ChannelHandlerContext ctx, ExceptionEvent e)
            throws Exception {
        boolean finished = futureQueue.offer(failedFuture(e.getChannel(), e.getCause()));
        assert finished;
        ctx.sendUpstream(e);
    }
}
