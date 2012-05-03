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
package org.helios.netty.jmx;

import java.lang.Thread.State;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ObjectName;

import org.helios.netty.ajax.SharedChannelGroup;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * <p>Title: MetricCollector</p>
 * <p>Description: Background task processor that periodically collects metrics and sends them to all active channels as a JSON object</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.jmx.MetricCollector</code></p>
 */
public class MetricCollector extends NotificationBroadcasterSupport implements MetricCollectorMXBean, Runnable {
	/** The memory mx bean */
	public static final MemoryMXBean memMxBean = ManagementFactory.getMemoryMXBean();
	/** The thread mx bean */
	public static final ThreadMXBean threadMxBean = ManagementFactory.getThreadMXBean();
	/** The NIO Direct MXBean ObjectName */
	public static final ObjectName directNio = JMXHelper.objectName("java.nio:type=BufferPool,name=direct");
	
	/** The NIO attributes we are interested in */
	public static final String[] NIO_ATTRS = new String[]{"Count", "MemoryUsed", "TotalCapacity"};
	/** Indicates if we have the NIO MXBean */
	protected final boolean haveNioMXBean;
	/** The period between collections */
	protected long period = 5000;
	/** Serial number factory for thread names */
	protected final AtomicInteger serial = new AtomicInteger(0);
	/** Serial number factory for notifications */
	protected final AtomicLong tick = new AtomicLong(0);
	/** The ObjectName for the metric collector */
	public static final ObjectName OBJECT_NAME = JMXHelper.objectName("org.helios.netty.jmx:service=MetricCollector");
	/** A set of the unique metric names */
	protected final Set<String> metricNames = new CopyOnWriteArraySet<String>();

	/** The schedule handle */
	protected ScheduledFuture<?> handle = null;
	/** The scheduler */
	protected final ScheduledThreadPoolExecutor scheduler;
	
	public MetricCollector(long period) {
		super();
		haveNioMXBean = ManagementFactory.getPlatformMBeanServer().isRegistered(directNio);
		this.period = period;
		try {			
			ManagementFactory.getPlatformMBeanServer().registerMBean(this, OBJECT_NAME);
			scheduler = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(2, new ThreadFactory(){
				public Thread newThread(Runnable r) {
					Thread t = new Thread(r, OBJECT_NAME.getKeyProperty("service") + "Thread#" + serial.incrementAndGet());
					t.setDaemon(true);
					return t;
				}
			});
			scheduler.schedule(this, period, TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			throw new RuntimeException("Failed to create MetricCollector", e);
		}
	}
	
	/**
	 * Executes the collection
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		try {
			Notification notif = new Notification(MetricProvider.METRIC_NOTIFICATION, OBJECT_NAME, tick.incrementAndGet(), System.currentTimeMillis());
			final JSONObject json = new JSONObject();
			notif.setUserData(json);
			json.put("ts", System.currentTimeMillis()); 
			json.put("heap", processMemoryUsage(memMxBean.getHeapMemoryUsage()));
			json.put("non-heap", processMemoryUsage(memMxBean.getNonHeapMemoryUsage()));	
			json.put("thread-states*", new JSONObject(getThreadStates()));
			if(haveNioMXBean) {
				json.put("direct-nio", new JSONObject(getNio()));				
			}
			sendNotification(notif);
			SharedChannelGroup.getInstance().write(json);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		} finally {
			scheduler.schedule(this, period, TimeUnit.MILLISECONDS);
		}
	}
	
	protected JSONObject processMemoryUsage(MemoryUsage usage) throws JSONException {
		JSONObject json = new JSONObject(usage);
		json.put("usedperc", calcPercent(usage.getUsed(), usage.getCommitted()));
		json.put("capacityperc", calcPercent(usage.getUsed(), usage.getMax()));		
		return json;
	}
	
	
	
	protected long calcPercent(double part, double whole) {
		if(part<1 || whole<1) return 0L;
		double d = part/whole*100;
		return (long)d;
	}
	
	protected void extractMetricNames(JSONObject json) {
		
	}
	
	/**
	 * Returns a simple map of NIO metrics
	 * @return a simple map of NIO metrics
	 */
	protected Map<String, Long> getNio() {
		Map<String, Long> map = new HashMap<String, Long>(NIO_ATTRS.length);
		try {
			AttributeList attrs = ManagementFactory.getPlatformMBeanServer().getAttributes(directNio, NIO_ATTRS);
			for(Attribute attr: attrs.asList()) {
				map.put(attr.getName(), (Long)attr.getValue());
			}
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		return map;
	}
	
	/**
	 * Collects the number of threads in each thread state
	 * @return an EnumMap with Thread states as the key and the number of threads in that state as the value
	 */
	public EnumMap<Thread.State, AtomicInteger> getThreadStates() {
		EnumMap<Thread.State, AtomicInteger> map = new EnumMap<State, AtomicInteger>(Thread.State.class);
		for(ThreadInfo ti : threadMxBean.getThreadInfo(threadMxBean.getAllThreadIds())) {
			State st = ti.getThreadState();
			AtomicInteger ai = map.get(st);
			if(ai==null) {
				ai = new AtomicInteger(0);
				map.put(st, ai);
			}
			ai.incrementAndGet();
		}
		return map;
	}

	/**
	 * Returns the collection period in ms.
	 * @return the period
	 */
	public long getPeriod() {
		return period;
	}

	/**
	 * Sets the collection period in ms.
	 * @param period the period to set
	 */
	public void setPeriod(long period) {
		this.period = period;
	}
}
