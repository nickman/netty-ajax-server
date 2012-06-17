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

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

import org.jboss.netty.util.ThreadNameDeterminer;
import org.jboss.netty.util.ThreadRenamingRunnable;

/**
 * <p>Title: ExecutorThreadFactory</p>
 * <p>Description: A Thread</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.examples.state.ExecutorThreadFactory</code></p>
 */

public class ExecutorThreadFactory implements ThreadFactory {
	/** The thread group that this factory's threads are created in */
	protected final ThreadGroup threadGroup;
	/** The thread serial number generator */
	protected final AtomicLong serial = new AtomicLong(0L);
	/** The thread group and thread name prefix */
	protected final String prefix;
	/** Indicates if the thread factory should create daemon threads */
	protected final boolean daemon;
	
	
	
	/**
	 * Creates a new ExecutorThreadFactory
	 * @param prefix The thread group and thread name prefix
	 * @param daemon Indicates if the thread factory should create daemon threads
	 */
	public ExecutorThreadFactory(String prefix, boolean daemon) {
		this.prefix = prefix;
		this.daemon = daemon;
		threadGroup = new ThreadGroup(prefix + "ThreadGroup");
	}

	static {
		ThreadRenamingRunnable.setThreadNameDeterminer(new ThreadNameDeterminer(){
			/**
			 * {@inheritDoc}
			 * @see org.jboss.netty.util.ThreadNameDeterminer#determineThreadName(java.lang.String, java.lang.String)
			 */
			@Override
			public String determineThreadName(String currentThreadName, String proposedThreadName) throws Exception {
				return currentThreadName;
			}
		});
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
	 */
	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(threadGroup, r, prefix + "Thread#" + serial.incrementAndGet());
		t.setDaemon(daemon);		
		return t;
	}

}
