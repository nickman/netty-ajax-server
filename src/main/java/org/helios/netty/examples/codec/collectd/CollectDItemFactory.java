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
package org.helios.netty.examples.codec.collectd;

import java.nio.CharBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.helios.netty.examples.codec.collectd.TypeDB.TypeDBEntry;

/**
 * <p>Title: CollectDItemFactory</p>
 * <p>Description: Creates a hierarchical set of collectd metrics submitted in one datagram.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.examples.codec.collectd.CollectDItemFactory</code></p>
 */

public class CollectDItemFactory {
	/** The incremental name builder */
	protected IncrementalName<CollectDKey> name = new IncrementalName<CollectDKey>("."); 	
	/** The type db iterator for the current vale set */
    protected Iterator<TypeDBEntry> typeEntries = null;
    /** The timestamp to associate with associated data values, UTC time format  */
    protected long time;
    /** The interval to associate with associated data values, UTC time format  */
    protected long interval;    
    /** The current set of values */
    protected final Set<CollectDValue> values = new HashSet<CollectDValue>();   
    
    
	/**
	 * Creates a new CollectDItemFactory
	 */
	public CollectDItemFactory() {
	}
	
	

    /**
     * <p>Title: CollectDItem</p>
     * <p>Description: Defines a set of metric values associated to one plugin instance for one time period</p> 
     * <p>Company: Helios Development Group LLC</p>
     * @author Whitehead (nwhitehead AT heliosdev DOT org)
     * <p><code>org.helios.netty.examples.codec.collectd.CollectDItem</code></p>
     */
    public static class CollectDItem {
        /** The name compound name to associate with associated data values  */
        protected final String metricName;
        /** The timestamp to associate with associated data values, UTC time format  */
        protected final long time;
        /** The interval to associate with associated data values, UTC time format  */
        protected final long interval;    
        /** The current set of values */
        protected final Set<CollectDValue> values;
        
		/**
		 * Creates a new CollectDItem
		 * @param metricName The fully qualified metric name
		 * @param time The metric time stamp
		 * @param interval The metric interval
		 * @param values The metric values
		 */
		public CollectDItem(String metricName, long time, long interval, Set<CollectDValue> values) {
			this.metricName = metricName;
			this.time = time;
			this.interval = interval;
			this.values = Collections.unmodifiableSet(values);
		}

		/**
		 * Returns the fully qualified metric name
		 * @return the metricName
		 */
		public String getMetricName() {
			return metricName;
		}

		/**
		 * Returns the timestamp for all metric values in this item
		 * @return the time
		 */
		public long getTime() {
			return time;
		}

		/**
		 * Returns the interval for all metric values in this item
		 * @return the interval
		 */
		public long getInterval() {
			return interval;
		}

		/**
		 * Returns the item's values
		 * @return the values
		 */
		public Set<CollectDValue> getValues() {
			return values;
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("CollectDItem [metricName=").append(metricName)
					.append(", time=").append(time).append(", interval=")
					.append(interval).append(", values=").append(values)
					.append("]");
			return builder.toString();
		}
		
		
    }
    
    
    /**
     * Adds a new value to the item in scope
     * @param pluginName The name of the plugin supplying the value
     * @param type The byte representing the data type of the value
     * @param value The numeric value
     */
    public void addValue(String pluginName, byte type, Number value) {
    	if(value==null) value = 0;
    	CollectDType ctype = CollectDType.decode(type);
//    	IternatorTypeDB.getInstance().getPluginEntries(pluginName);
//    	TypeDBEntry typeEntry = scope.typeEntries.next();    	
    }
    


}
