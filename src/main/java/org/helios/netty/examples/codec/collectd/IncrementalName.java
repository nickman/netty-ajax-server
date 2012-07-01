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

import java.util.Comparator;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * <p>Title: IncrementalName</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.examples.codec.collectd.IncrementalName</code></p>
 * @param <E> The enum type of the key
 */

public class IncrementalName<E extends Enum<E>>  {
	/** The map of strings */
	private final ConcurrentSkipListMap<E, String> map = new ConcurrentSkipListMap<E, String>();
	/** The configured delimiter */
	private final CharSequence delimiter;
	
	/**
	 * Creates a new IncrementalName
	 * @param delimiter The delimiter that will separate each entry when rendered
	 */
	public IncrementalName(CharSequence delimiter) {
		this.delimiter = delimiter;
	}
	
	/**
	 * Pushes a value into the name, rewinding the name if the passed type is already in the map
	 * @param type The type of the value being pushed
	 * @param value The value to be pushed into the name
	 * @return this name
	 */
	public synchronized IncrementalName<E> push(E type, Object value) {
		if(type==null) throw new IllegalArgumentException("The passed type was null", new Throwable());
		if(value==null) throw new IllegalArgumentException("The passed value was null", new Throwable());		
		if(map.containsKey(type)) {
			map.tailMap(type, true).clear();
		}
		map.put(type, value.toString());
		return this;
	}
	
	/**
	 * Renders the name into a string
	 * @return the rendered name
	 */
	public String render() {
		TreeMap<E, String> tm = new TreeMap<E, String>(map);
		if(tm.isEmpty()) return "";
		StringBuilder b = new StringBuilder();
		int i = tm.size()-1;
		int cnt = 0;
		for(String s: tm.values()) {
			b.append(s);
			if(cnt<i) b.append(delimiter);
			cnt++;
		}		
		return b.toString();
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log("IncrementalName Test");
		IncrementalName<CollectDKey> name = new IncrementalName<CollectDKey>("/");
		for(CollectDKey ck: CollectDKey.values()) {
			name.push(ck, ck.name());
		}
		log(name.render());
		name.push(CollectDKey.PLUGIN, "MyPlugin");
		log(name.render());
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}


}
