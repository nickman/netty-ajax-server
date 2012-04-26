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

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.concurrent.TimeUnit;

import org.json.JSONObject;

/**
 * <p>Title: MemoryReporter</p>
 * <p>Description: Wakes up every <i>n</i> seconds and broadcasts the heap usage to all channels in the shared channel group</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.ajax.MemoryReporter</code></p>
 */
public class MemoryReporter extends Thread {
	/** The memory mx bean */
	public static final MemoryMXBean mxBean = ManagementFactory.getMemoryMXBean();
	/** The number of ms. to sleep */
	private final long sleepTime;
	/**
	 * Creates a new MemoryReporter
	 * @param frequency The frequency in seconds of the memory reporting
	 */
	public MemoryReporter(int frequency) {
		super("MemoryReporter-" + frequency);
		setDaemon(true);		
		sleepTime = TimeUnit.MILLISECONDS.convert(frequency, TimeUnit.SECONDS);
	}
	
	/**
	 * Broadcasts the heap memory usage every <code>sleepTime</code> ms.
	 * {@inheritDoc}
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		while(true) {
			try { join(sleepTime); } catch (Exception e) {}
			MemoryUsage musage = mxBean.getHeapMemoryUsage();
			SharedChannelGroup.getInstance().write(new JSONObject(musage));
		}
	}
}
