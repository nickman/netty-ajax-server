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

import java.net.SocketAddress;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.helios.netty.examples.CustomServerBootstrapBinder;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.Channels;

/**
 * <p>Title: CustomServerBootstrap</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.examples.state.CustomServerBootstrap</code></p>
 */

public class CustomServerBootstrap extends ServerBootstrap {

	/**
	 * Creates a new CustomServerBootstrap
	 */
	public CustomServerBootstrap() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Creates a new CustomServerBootstrap
	 * @param channelFactory
	 */
	public CustomServerBootstrap(ChannelFactory channelFactory) {
		super(channelFactory);
		// TODO Auto-generated constructor stub
	}
	
    public Channel bind(final SocketAddress localAddress) {
        if (localAddress == null) {
            throw new NullPointerException("localAddress");
        }

        final BlockingQueue<ChannelFuture> futureQueue =
            new LinkedBlockingQueue<ChannelFuture>();

        ChannelHandler binder = new CustomServerBootstrapBinder(this, localAddress, futureQueue);
        ChannelHandler parentHandler = getParentHandler();

        ChannelPipeline bossPipeline =Channels. pipeline();
        bossPipeline.addLast("binder", binder);
        if (parentHandler != null) {
            bossPipeline.addLast("userHandler", parentHandler);
        }

        Channel channel = getFactory().newChannel(bossPipeline);

        // Wait until the future is available.
        ChannelFuture future = null;
        boolean interrupted = false;
        do {
            try {
                future = futureQueue.poll(Integer.MAX_VALUE, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                interrupted = true;
            }
        } while (future == null);

        if (interrupted) {
            Thread.currentThread().interrupt();
        }

        // Wait for the future.
        future.awaitUninterruptibly();
        if (!future.isSuccess()) {
            future.getChannel().close().awaitUninterruptibly();
            throw new ChannelException("Failed to bind to: " + localAddress, future.getCause());
        }

        return channel;
    }

}
