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

/**
 * <p>Title: CollectDValue</p>
 * <p>Description: Represents a collectd value part.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.examples.codec.collectd.CollectDValue</code></p>
 */

public class CollectDValue {
	/** The value name */
	private final String name;
	/** The part data type */
	private final CollectDType type;
	/** The part value */
	private final double  value;
	/**
	 * Creates a new CollectDValue
	 * @param name The value name 
	 * @param type The part data type
	 * @param value The part value
	 */
	public CollectDValue(String name, CollectDType type, double value) {
		this.name = name;
		this.type = type;
		this.value = value;
	}
	/**
	 * Returns the part data type
	 * @return the part data type
	 */
	public CollectDType getType() {
		return type;
	}
	/**
	 * Returns the part value
	 * @return the part value
	 */
	public double getValue() {
		return value;
	}
	/**
	 * Returns the value name 
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CollectDValue [name=").append(name).append(", type=")
				.append(type).append(", value=").append(value).append("]");
		return builder.toString();
	}
	
	
	
	
}
