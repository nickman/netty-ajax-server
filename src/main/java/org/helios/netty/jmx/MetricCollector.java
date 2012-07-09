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

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.lang.Thread.State;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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

import org.apache.log4j.Logger;
import org.helios.netty.ajax.PipelineModifier;
import org.helios.netty.ajax.SharedChannelGroup;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.util.CharsetUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * <p>Title: MetricCollector</p>
 * <p>Description: Background task processor that periodically collects metrics and sends them to all active channels as a JSON object</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.jmx.MetricCollector</code></p>
 */

public class MetricCollector extends NotificationBroadcasterSupport implements MetricCollectorMXBean, Runnable, PipelineModifier, ChannelDownstreamHandler, ChannelUpstreamHandler {
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
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	/** The schedule handle */
	protected ScheduledFuture<?> handle = null;
	/** The scheduler */
	protected final ScheduledThreadPoolExecutor scheduler;
	/**  A map of remotely submitted metrics keyed by the address that they came from */
	protected final Map<SocketAddress, Map<String, Long>> pendingRemoteMetrics = new ConcurrentHashMap<SocketAddress, Map<String, Long>> ();
	/** Dropped metric counter */
	protected final AtomicLong dropCounter = new AtomicLong(0);
	
	/** The singleton instance */
	private static volatile MetricCollector instance = null;
	/** The singleton ctor lock */
	private static final Object lock = new Object();
	
	/**
	 * Acquires the singleton instance. First call wins on the period.
	 * @param period The ms. between each metric collect.
	 * @return The metric collector singleton
	 */
	public static MetricCollector getInstance(long period) {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new MetricCollector(period);
				}
			}
		}
		return instance;
	}
	
	/**
	 * Acquires the singleton instance. 
	 * @return The metric collector singleton
	 */
	public static MetricCollector getInstance() {
		if(instance==null) {
			throw new RuntimeException("The metric collector has not been initialized", new Throwable());
		}
		return instance;
	}
	
	/**
	 * Creates a new MetricCollector
	 * @param period The period of collection
	 */
	private MetricCollector(long period) {
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
			initMetricNames();
			scheduler.schedule(this, period, TimeUnit.MILLISECONDS);
			log.info("Started MetricCollector with a period of " + period + " ms.");
		} catch (Exception e) {
			throw new RuntimeException("Failed to create MetricCollector", e);
		}
	}
	
	/**
	 * Submits a new metric and value
	 * @param metricName The metric name
	 * @param value The metric value
	 */
	public void submitMetric(String metricName, long value) {
		if(metricNames.add(metricName)) {
			JSONObject envelope = new JSONObject();
			try {
				envelope.put("metric-names", new JSONArray(new String[]{metricName}));
				SharedChannelGroup.getInstance().write(envelope);
				SharedChannelGroup.getInstance().write(packageMetricUpdate(Collections.singletonMap(metricName, value)));
			} catch (JSONException e) {
				log.error("Failed to submit metric", e);
			}			
		}
	}
	
	
	/**
	 * Submits a map of metrics from a remote socket submitter
	 * @param clientSocket The address of the submitting socket
	 * @param metrics A map of metric values keyed by metric name
	 */
	public void submitMetrics(SocketAddress clientSocket , Map<String, Long> metrics) {
		submitMetrics(metrics);
		if(pendingRemoteMetrics.put(clientSocket, metrics)!=null) {
			dropCounter.incrementAndGet();
		}
	}

	/**
	 * Returns the number of dropped metrics
	 * @return the number of dropped metrics
	 */
	public long getDroppedMetricCount() {
		return dropCounter.get();
	}
	
	/**
	 * Submits a map of metrics
	 * @param metrics A map of metric values keyed by metric name
	 */
	public void submitMetrics(Map<String, Long> metrics) {
		Set<String> newNames = new HashSet<String>();
		for(String s: metrics.keySet()) {
			if(metricNames.add(s)) {
				newNames.add(s);
			}
		}
		try {
			JSONObject envelope = new JSONObject();
			envelope.put("metric-names", new JSONArray(newNames.toArray(new String[newNames.size()])));
			SharedChannelGroup.getInstance().write(envelope);
		} catch (JSONException e) {
			log.error("Failed to submit metric names", e);
		}		
		try {
			SharedChannelGroup.getInstance().write(packageMetricUpdate(metrics));
		} catch (Exception e) {
			log.error("Failed to send metrics", e);
		}					
	}
	
	
	/**
	 * Packages the passed map of metrics into a JSONObject
	 * @param metrics A map of metric values keyed by metric name
	 * @return The JSONObject with the metrics
	 */
	protected JSONObject packageMetricUpdate(Map<String, Long> metrics) {
		try {
			final JSONObject top = new JSONObject();
			final JSONObject envelope = new JSONObject();
			envelope.put("metrics", top);
			for(Map.Entry<String, Long> entry: metrics.entrySet()) {
				insertMetric(entry.getKey(), entry.getValue(), top);
			}
			return envelope;
		} catch (Exception e) {
			log.error("Failed to package metric updates", e);
			return null;
		}
	}
	
	/**
	 * Inserts the metric name keys and value into the passed JSONObject
	 * @param metricName The metric name
	 * @param value The metric value
	 * @param top The JSONObject to insert into
	 * @throws JSONException
	 */
	protected void insertMetric(String metricName, long value, JSONObject top) throws JSONException {
		String[] frags = metricName.split("\\.");
		int fc = frags.length;
		JSONObject current = top;
		for(int i = 0; i < fc; i++) {
			if(i==fc-1) {
				current.put(frags[i], value);
			} else {
				JSONObject tmp = null;
				if(current.has(frags[i])) {
					tmp = current.getJSONObject(frags[i]);
				} else {
					tmp = new JSONObject();
					current.put(frags[i], tmp);					
				}
				current = tmp;
			}
		}
	}
	
	
	/**
	 * The MetricCollector is not a real MetricProvider, but it needs to supply the names of the metrics
	 * it published, so we add them to the registry here.
	 */
	protected void initMetricNames() {
		String[] memMetrics = new String[]{
				"[capacity(%)]", "[committed]", "[init]", "[max]", "[used]", "[consumed(%)]"
		};
		for(String m: memMetrics) {
			metricNames.add("[heap]." + m);
			metricNames.add("[non-heap]." + m);
		}
		if(haveNioMXBean) {
			for(String s: NIO_ATTRS) {
				metricNames.add("direct-nio." + s);
			}
		}
		metricNames.add("thread-states*");
	}
	
	/**
	 * Executes the collection
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		try {
			updateMetricNames();
			long channelCount = SharedChannelGroup.getInstance().size();
			//log.info("\n\tChanne,l Count:" + channelCount);
			submitMetric("netty.channels.count", channelCount);
			Notification notif = new Notification(MetricProvider.METRIC_NOTIFICATION, OBJECT_NAME, tick.incrementAndGet(), System.currentTimeMillis());
			final JSONObject json = new JSONObject();
			final JSONObject envelope = new JSONObject();
			envelope.put("metrics", json);
			notif.setUserData(json);
			pushRemoteMetrics(json);
			json.put("ts", System.currentTimeMillis());			
			json.put("heap", processMemoryUsage(memMxBean.getHeapMemoryUsage()));
			json.put("non-heap", processMemoryUsage(memMxBean.getNonHeapMemoryUsage()));	
			json.put("thread-states*", new JSONObject(getThreadStates()));
			if(haveNioMXBean) {
				json.put("direct-nio", new JSONObject(getNio()));				
			}
			sendNotification(notif);
			SharedChannelGroup.getInstance().write(envelope);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		} finally {
			scheduler.schedule(this, period, TimeUnit.MILLISECONDS);
		}
	}
	
	/**
	 * Retrieves the unsubmitted remote metrics and packages them into the passed json object
	 * @param json The JSON Object to package the metrics into
	 */
	protected void pushRemoteMetrics(final JSONObject json) {
		Map<SocketAddress, Map<String, Long>> tmpMap = new HashMap<SocketAddress, Map<String, Long>>(pendingRemoteMetrics);
		pendingRemoteMetrics.clear();
		for(Map.Entry<SocketAddress, Map<String, Long>> entry: tmpMap.entrySet()) {
			for(Map.Entry<String, Long> mentry: entry.getValue().entrySet()) {
				try {
					insertMetric(mentry.getKey(), mentry.getValue(), json);
				} catch (JSONException e) {
				}
			}
		}
		try {
			insertMetric("metriccollector.drops", getDroppedMetricCount(), json);
		} catch (Exception e) {}
		tmpMap.clear();
	}
	/**
	 * Collects metric names from participating metric providers
	 * @throws JSONException thrown on any json exception 
	 */
	protected void updateMetricNames() throws JSONException {
		Notification notif = new Notification(MetricProvider.METRIC_NAME_NOTIFICATION, OBJECT_NAME, tick.incrementAndGet(), System.currentTimeMillis());
		final Set<String> names = new HashSet<String>();
		final Set<String> newNames = new HashSet<String>();
		notif.setUserData(names);
		sendNotification(notif);
		for(String s: names) {
			if(metricNames.add(s)) {
				newNames.add(s);
			}
		}
		if(!newNames.isEmpty()) {
			JSONObject envelope = new JSONObject();
			envelope.put("metric-names", new JSONArray(newNames));
			SharedChannelGroup.getInstance().write(envelope);
		}
	}
	
	/**
	 * Generates a {@link JSONObject} representing memory usage, plus the percentage usage of:<ul>
	 * <li>Memory Allocated</li>
	 * <li>Memory Maximum Capacity</li>
	 * </ul>
	 * @param usage The memory usage provided by the {@link MemoryMXBean}
	 * @return A {@link JSONObject} 
	 * @throws JSONException I have no idea why this would be thrown
	 */
	protected JSONObject processMemoryUsage(MemoryUsage usage) throws JSONException {
		JSONObject json = new JSONObject(usage);
		json.put("consumed(%)", calcPercent(usage.getUsed(), usage.getCommitted()));
		json.put("capacity(%)", calcPercent(usage.getUsed(), usage.getMax()));		
		return json;
	}
	
	/**
	 * Returns a JSONArray of all the registered metric names 
	 * @return the metricNames
	 */
	public JSONObject getMetricNamesJSON() {
		JSONArray arr = new JSONArray(metricNames);
		JSONObject mn = new JSONObject();
		try {
			mn.put("metric-names", arr);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return mn;
	}
	
	/**
	 * Returns a JSON string of all the registered metric names 
	 * @return the metricNames
	 */
	public String getMetricNames() {
		JSONArray arr = new JSONArray(metricNames);
		JSONObject mn = new JSONObject();
		try {
			mn.put("metric-names", arr);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return mn.toString();
	}
	
	
	
	/**
	 * Simple percentage calculator
	 * @param part The part value
	 * @param whole The whole value
	 * @return The percentage that the part is of the whole as a long
	 */
	protected long calcPercent(double part, double whole) {
		if(part<1 || whole<1) return 0L;
		double d = part/whole*100;
		return (long)d;
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

	/**
	 * {@inheritDoc}
	 * @see org.helios.netty.ajax.PipelineModifier#modifyPipeline(org.jboss.netty.channel.ChannelPipeline)
	 */
	@Override
	public void modifyPipeline(ChannelPipeline pipeline) {
		if(pipeline.get("metricnames")==null) {
			pipeline.addLast("metricnames", this);
		}		
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.netty.ajax.PipelineModifier#getName()
	 */
	@Override
	public String getName() {
		return "metricnames";
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.netty.ajax.PipelineModifier#getChannelHandler()
	 */
	@Override
	public ChannelHandler getChannelHandler() {
		return this;
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelDownstreamHandler#handleDownstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	@Override
	public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		final Channel channel = e.getChannel();
		if(!channel.isOpen()) return;
		if(!(e instanceof MessageEvent)) {
            ctx.sendDownstream(e);
            return;
        }
		Object message = ((MessageEvent)e).getMessage();
		if(!(message instanceof JSONObject) && !(message instanceof CharSequence)) {
            ctx.sendDownstream(e);
            return;			
		}
		
		HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
		response.setContent(ChannelBuffers.copiedBuffer("\n" + message.toString() + "\n", CharsetUtil.UTF_8));
		response.setHeader(CONTENT_TYPE, "application/json");
		ChannelFuture cf = Channels.future(channel);
		cf.addListener(new ChannelFutureListener(){
			public void operationComplete(ChannelFuture f) throws Exception {
				channel.close();
			}
		});
		ctx.sendDownstream(new DownstreamMessageEvent(channel, cf, response, channel.getRemoteAddress()));
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelUpstreamHandler#handleUpstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		if(e instanceof MessageEvent) {
			Object msg = ((MessageEvent)e).getMessage();
			if(msg instanceof HttpRequest) {
				//ctx.sendDownstream(new DownstreamMessageEvent(e.getChannel(), Channels.future(e.getChannel()), getMetricNamesJSON(), ((MessageEvent) e).getRemoteAddress()));
				e.getChannel().write(getMetricNamesJSON());
			}
		}
		ctx.sendUpstream(e);
	}

}
