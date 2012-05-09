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

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.Notification;
import javax.management.ObjectName;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * <p>Title: ThreadPoolFactory</p>
 * <p>Description: JMX instrumented thread pool executor factory</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.jmx.ThreadPoolFactory</code></p>
 */
public class ThreadPoolFactory extends ThreadPoolExecutor implements MetricProvider, ThreadFactory, ThreadPoolMXBean {
	/**  */
	private static final long serialVersionUID = 5127908418248445054L;
	/** The ObjectName that will be used to register the thread pool management interface */
	protected final ObjectName objectName;
	/** Serial number factory for thread names */
	protected final AtomicInteger serial = new AtomicInteger(0);
	/** The pool name */
	protected final String name;
	/** The threading level json object */
	protected static final JSONObject threadingMetrics = new JSONObject();
	/** This pool's threading metrics json object */
	protected final Map<String, Number> poolMetrics = new HashMap<String, Number>();
	/** The supplied metric names */
	protected final Set<String> metricNames = new HashSet<String>();
	/** The metric points */
	protected final String[] points = new String[]{"activeThreads", "poolSize", "largestPoolSize", "completedTasks"};
	/**
	 * Creates a new ThreadPool
	 * @param domain The JMX domain where the MBean will be published 
	 * @param name The name property for the MBean ObjectName
	 * @return a new thread pool
	 */
	public static Executor newCachedThreadPool(String domain, String name) {
		return new ThreadPoolFactory(domain, name);
	}
	
	/**
	 * Creates a new ThreadPool
	 * @param domain The JMX domain where the MBean will be published 
	 * @param name The name property for the MBean ObjectName
	 */
	private ThreadPoolFactory(String domain, String name) {
		super(0, Integer.MAX_VALUE, 50L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
		setThreadFactory(this);
		this.name = name;
		try {
			objectName = new ObjectName(domain + ":service=ThreadPool,name=" + name);
			ManagementFactory.getPlatformMBeanServer().registerMBean(this, objectName);
			threadingMetrics.put(name, poolMetrics);
			ManagementFactory.getPlatformMBeanServer().addNotificationListener(MetricCollector.OBJECT_NAME, this, this, null);
			String prefix = "threadPools.[" + name + "].";
			for(String s: points) {
				metricNames.add(prefix + s);
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to register management interface for pool [" + domain + "/" + name + "]", e);
		}
		
	}
	

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationListener#handleNotification(javax.management.Notification, java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void handleNotification(Notification notification, Object handback) {
		String notifType = notification.getType();
		if(METRIC_NAME_NOTIFICATION.equals(notifType)) {
			Set<String> names = (Set<String>)notification.getUserData();
			names.addAll(metricNames);
		} else {
			addMetrics((JSONObject)notification.getUserData());
		}
	}

	/**
	 * {@inheritDoc}
	 * @throws JSONException 
	 * @see org.helios.netty.jmx.MetricProvider#addMetrics(org.json.JSONObject)
	 */
	@Override
	public void addMetrics(JSONObject json)  {
		try {
			if(!json.has("threadPools")) {
				json.put("threadPools", threadingMetrics);
			}
		} catch (Exception e) {}
		poolMetrics.put("activeThreads", this.getActiveCount());
		poolMetrics.put("poolSize", this.getPoolSize());
		poolMetrics.put("largestPoolSize", this.getLargestPoolSize());
		poolMetrics.put("completedTasks", this.getCompletedTaskCount());		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.netty.jmx.MetricProvider#getProvidedMetricNames()
	 */
	@Override
	public Set<String> getProvidedMetricNames() {
		return metricNames;
	}

	/**
	 * {@inheritDoc}
	 * @see java.util.concurrent.ThreadFactory#newThread(java.lang.Runnable)
	 */
	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(r, name + "Thread#" + serial.incrementAndGet());
		t.setDaemon(true);
		return t;
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.NotificationFilter#isNotificationEnabled(javax.management.Notification)
	 */
	@Override
	public boolean isNotificationEnabled(Notification notification) {
		String notifType = notification.getType();
		return (METRIC_NAME_NOTIFICATION.equals(notifType) && notification.getUserData() instanceof Set) ||  (METRIC_NOTIFICATION.equals(notifType) && notification.getUserData() instanceof JSONObject);
	}

}