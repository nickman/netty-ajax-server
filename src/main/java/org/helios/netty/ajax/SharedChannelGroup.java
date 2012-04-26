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
package org.helios.netty.ajax;

import java.net.SocketAddress;
import java.util.Collection;
import java.util.Iterator;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelLocal;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;

/**
 * <p>Title: SharedChannelGroup</p>
 * <p>Description: A netty channel group managed as a singleton so it can be accessed anywhere.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.ajax.SharedChannelGroup</code></p>
 */
public class SharedChannelGroup implements ChannelGroup, ChannelFutureListener {
	/** The singleton instance */
	private static volatile SharedChannelGroup instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** The core channel group */
	private ChannelGroup channelGroup = new DefaultChannelGroup("Netty Ajax Server Channel Group");
	
	/**
	 * Retrieves the SharedChannelGroup singleton instance
	 * @return the SharedChannelGroup singleton instance
	 */
	public static SharedChannelGroup getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new SharedChannelGroup();
				}
			}
		}
		return instance;
	}
	
	private SharedChannelGroup() {
		
	}
	
	/**
	 * ChannelFutureListener impl that removes Channels from the group when they close.
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelFutureListener#operationComplete(org.jboss.netty.channel.ChannelFuture)
	 */
	public void operationComplete(ChannelFuture f) throws Exception {
		if(f.isDone()) {
			remove(f.getChannel());
		}
	}
	
	
	/**
	 * Adds a channel to this group 
	 * @param channel The channel to add
	 * @return true if the channel was not already in the group
	 * @see java.util.Set#add(java.lang.Object)
	 */
	public boolean add(Channel channel) {
		return channelGroup.add(channel);
	}
	
	/**
	 * Removes a channel from the ChannelGroup
	 * @param channnel The channel to remove
	 * @return true if the channel was present and was removed
	 * @see java.util.Set#remove(java.lang.Object)
	 */
	public boolean remove(Channel channel) {
		return channelGroup.remove(channel);
	}	
	

	/**
	 * {@inheritDoc}
	 * @see java.util.Set#remove(java.lang.Object)
	 */
	public boolean remove(Object obj) {
		if(obj!=null && obj instanceof Channel) {
			return remove((Channel)obj);
		} 
		return false;
	}
	/**
	 * @return
	 * @see java.util.Set#size()
	 */
	public int size() {
		return channelGroup.size();
	}

	/**
	 * @return
	 * @see java.util.Set#isEmpty()
	 */
	public boolean isEmpty() {
		return channelGroup.isEmpty();
	}

	/**
	 * @param o
	 * @return
	 * @see java.util.Set#contains(java.lang.Object)
	 */
	public boolean contains(Object o) {
		return channelGroup.contains(o);
	}

	/**
	 * @param o
	 * @return
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(ChannelGroup o) {
		return channelGroup.compareTo(o);
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.group.ChannelGroup#getName()
	 */
	public String getName() {
		return channelGroup.getName();
	}

	/**
	 * @param id
	 * @return
	 * @see org.jboss.netty.channel.group.ChannelGroup#find(java.lang.Integer)
	 */
	public Channel find(Integer id) {
		return channelGroup.find(id);
	}

	/**
	 * @return
	 * @see java.util.Set#iterator()
	 */
	public Iterator<Channel> iterator() {
		return channelGroup.iterator();
	}

	/**
	 * @param interestOps
	 * @return
	 * @see org.jboss.netty.channel.group.ChannelGroup#setInterestOps(int)
	 */
	public ChannelGroupFuture setInterestOps(int interestOps) {
		return channelGroup.setInterestOps(interestOps);
	}

	/**
	 * @return
	 * @see java.util.Set#toArray()
	 */
	public Object[] toArray() {
		return channelGroup.toArray();
	}

	/**
	 * @param readable
	 * @return
	 * @see org.jboss.netty.channel.group.ChannelGroup#setReadable(boolean)
	 */
	public ChannelGroupFuture setReadable(boolean readable) {
		return channelGroup.setReadable(readable);
	}

	/**
	 * @param message
	 * @return
	 * @see org.jboss.netty.channel.group.ChannelGroup#write(java.lang.Object)
	 */
	public ChannelGroupFuture write(Object message) {
		return channelGroup.write(message);
	}

	/**
	 * @param a
	 * @return
	 * @see java.util.Set#toArray(T[])
	 */
	public <T> T[] toArray(T[] a) {
		return channelGroup.toArray(a);
	}

	/**
	 * @param message
	 * @param remoteAddress
	 * @return
	 * @see org.jboss.netty.channel.group.ChannelGroup#write(java.lang.Object, java.net.SocketAddress)
	 */
	public ChannelGroupFuture write(Object message, SocketAddress remoteAddress) {
		return channelGroup.write(message, remoteAddress);
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.group.ChannelGroup#disconnect()
	 */
	public ChannelGroupFuture disconnect() {
		return channelGroup.disconnect();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.group.ChannelGroup#unbind()
	 */
	public ChannelGroupFuture unbind() {
		return channelGroup.unbind();
	}

	/**
	 * @return
	 * @see org.jboss.netty.channel.group.ChannelGroup#close()
	 */
	public ChannelGroupFuture close() {
		return channelGroup.close();
	}




	/**
	 * @param c
	 * @return
	 * @see java.util.Set#containsAll(java.util.Collection)
	 */
	public boolean containsAll(Collection<?> c) {
		return channelGroup.containsAll(c);
	}

	/**
	 * @param c
	 * @return
	 * @see java.util.Set#addAll(java.util.Collection)
	 */
	public boolean addAll(Collection<? extends Channel> c) {
		return channelGroup.addAll(c);
	}

	/**
	 * @param c
	 * @return
	 * @see java.util.Set#retainAll(java.util.Collection)
	 */
	public boolean retainAll(Collection<?> c) {
		return channelGroup.retainAll(c);
	}

	/**
	 * @param c
	 * @return
	 * @see java.util.Set#removeAll(java.util.Collection)
	 */
	public boolean removeAll(Collection<?> c) {
		return channelGroup.removeAll(c);
	}

	/**
	 * 
	 * @see java.util.Set#clear()
	 */
	public void clear() {
		channelGroup.clear();
	}

	/**
	 * @param o
	 * @return
	 * @see java.util.Set#equals(java.lang.Object)
	 */
	public boolean equals(Object o) {
		return channelGroup.equals(o);
	}

	/**
	 * @return
	 * @see java.util.Set#hashCode()
	 */
	public int hashCode() {
		return channelGroup.hashCode();
	}
}
