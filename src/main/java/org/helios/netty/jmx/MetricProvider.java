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

import java.util.Set;

import javax.management.NotificationFilter;
import javax.management.NotificationListener;

import org.json.JSONObject;

/**
 * <p>Title: MetricProvider</p>
 * <p>Description: Interface to be implemented by MBeans providing metrics to be delivered to the client</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.jmx.MetricProvider</code></p>
 */
public interface MetricProvider extends NotificationListener, NotificationFilter {
	/** The notification type for a metric collection */
	public static final String METRIC_NOTIFICATION = "metric.request";
	/** The notification type for a metric name collection */
	public static final String METRIC_NAME_NOTIFICATION = "metric.name.request"; 
	
	/**
	 * Writes the provided metric instances into the passed {@link JSONObject}
	 * @param json The {@link JSONObject} to write the metrics into
	 */
	public void addMetrics(JSONObject json);
	/**
	 * Returns a set of the fully qualified metric names provided by this provider
	 * @return a set of the fully qualified metric names provided by this provider
	 */
	public Set<String> getProvidedMetricNames();
}
