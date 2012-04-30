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
package org.helios.netty.ajax.handlergroups.longpoll;

import java.net.SocketAddress;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.log4j.Logger;
import org.helios.netty.ajax.SharedChannelGroup;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelConfig;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.util.HashedWheelTimer;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;

/**
 * <p>Title: TimeoutChannel</p>
 * <p>Description: A wrapper for a netty channel that sets up a timeout when waiting for an event to deliver</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.ajax.handlergroups.longpoll.TimeoutChannel</code></p>
 */
public class TimeoutChannel implements Channel, TimerTask {
	/** The wrapped channel */
	protected final Channel channel;
	/** Indicates if this session is keep alive */
	protected final boolean keepAlive;
	/** The client specified timeout */
	protected final long timeout;
	/** The current timeout */
	protected AtomicReference<Timeout> timerTimeout = new AtomicReference<Timeout>(null);
	
	/** The timer for implementing timeouts */
	protected static final HashedWheelTimer timer = new HashedWheelTimer();
	/** Instance logger */
	protected final Logger log;
	
	static {
		timer.start();
	}
	
	/**
	 * Creates a new TimeoutChannel
	 * @param channel The channel to wrap
	 * @param timeout The client specified timeout in ms.
	 * @param keepAlive Indicates if this session is keep alive
	 */
	public TimeoutChannel(Channel channel, long timeout, boolean keepAlive) {
		this.channel = channel;
		this.timeout = timeout;
		this.keepAlive = keepAlive;
		log = Logger.getLogger(getClass().getName() + "." + channel.getId());
		timerTimeout.getAndSet(timer.newTimeout(this, this.timeout, TimeUnit.MILLISECONDS));
		SharedChannelGroup.getInstance().add(this);
	}
	
	/**
	 * Called when the channels timeout fires.
	 * If the channel is keep alive, we signal a downsteam timeout and then reset the timer.
	 * Otherwise, we close the channel.
	 * {@inheritDoc}
	 * @see org.jboss.netty.util.TimerTask#run(org.jboss.netty.util.Timeout)
	 */
	public void run(Timeout tout) {
		if(!tout.isCancelled()) {
			if(keepAlive && channel.isOpen()) {
				log.info("Timeout on waiting channel");
				write(String.format("{\"timeout\":%s}", timeout));
//				resetTimer();
//				channel.getPipeline().getContext(LongPollModifier.NAME).sendDownstream(
//						new DownstreamMessageEvent(channel, Channels.future(channel), String.format("{\"timeout\":%s}", timeout) , channel.getRemoteAddress())
//				);
			} else {
				close();
			}
		}
	}
	
	/**
	 * Resets the timeout on this channel
	 */
	protected void resetTimer() {
		Timeout t = timerTimeout.getAndSet(timer.newTimeout(this, this.timeout, TimeUnit.MILLISECONDS));
		if(t!=null) {
			t.cancel();
		}
	}
	
	/**
	 * When a write request is processed, the timeout is reset.
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#write(java.lang.Object)
	 */
	public ChannelFuture write(Object message) {
		SharedChannelGroup.getInstance().remove(this);
		return channel.write(message);
	}

	/**
	 * When a write request is processed, the timeout is reset.
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.Channel#write(java.lang.Object, java.net.SocketAddress)
	 */
	public ChannelFuture write(Object message, SocketAddress remoteAddress) {
		SharedChannelGroup.getInstance().remove(this);
		return channel.write(message, remoteAddress);
	}
	
	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#close()
	 */
	public ChannelFuture close() {
		Timeout t = timerTimeout.getAndSet(null);
		if(t!=null) t.cancel();
		return channel.close();
	}
	
	

	/**
	 * @param o
	 * @return
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(Channel o) {
		return channel.compareTo(o);
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#getId()
	 */
	public Integer getId() {
		return channel.getId();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#getFactory()
	 */
	public ChannelFactory getFactory() {
		return channel.getFactory();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#getParent()
	 */
	public Channel getParent() {
		return channel.getParent();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#getConfig()
	 */
	public ChannelConfig getConfig() {
		return channel.getConfig();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#getPipeline()
	 */
	public ChannelPipeline getPipeline() {
		return channel.getPipeline();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#isOpen()
	 */
	public boolean isOpen() {
		return channel.isOpen();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#isBound()
	 */
	public boolean isBound() {
		return channel.isBound();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#isConnected()
	 */
	public boolean isConnected() {
		return channel.isConnected();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#getLocalAddress()
	 */
	public SocketAddress getLocalAddress() {
		return channel.getLocalAddress();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#getRemoteAddress()
	 */
	public SocketAddress getRemoteAddress() {
		return channel.getRemoteAddress();
	}


	/**
	 * @param localAddress
	 * @return
	 * @see org.jboss.netty.channel.Channel#bind(java.net.SocketAddress)
	 */
	public ChannelFuture bind(SocketAddress localAddress) {
		return channel.bind(localAddress);
	}

	/**
	 * @param remoteAddress
	 * @return
	 * @see org.jboss.netty.channel.Channel#connect(java.net.SocketAddress)
	 */
	public ChannelFuture connect(SocketAddress remoteAddress) {
		return channel.connect(remoteAddress);
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#disconnect()
	 */
	public ChannelFuture disconnect() {
		return channel.disconnect();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#unbind()
	 */
	public ChannelFuture unbind() {
		return channel.unbind();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#getCloseFuture()
	 */
	public ChannelFuture getCloseFuture() {
		return channel.getCloseFuture();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#getInterestOps()
	 */
	public int getInterestOps() {
		return channel.getInterestOps();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#isReadable()
	 */
	public boolean isReadable() {
		return channel.isReadable();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.Channel#isWritable()
	 */
	public boolean isWritable() {
		return channel.isWritable();
	}

	/**
	 * @param interestOps
	 * @return
	 * @see org.jboss.netty.channel.Channel#setInterestOps(int)
	 */
	public ChannelFuture setInterestOps(int interestOps) {
		return channel.setInterestOps(interestOps);
	}

	/**
	 * @param readable
	 * @return
	 * @see org.jboss.netty.channel.Channel#setReadable(boolean)
	 */
	public ChannelFuture setReadable(boolean readable) {
		return channel.setReadable(readable);
	}
	
}
