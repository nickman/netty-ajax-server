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
package org.helios.netty.examples.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * <p>Title: Utils</p>
 * <p>Description: Some generic utility functions</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.examples.util.Utils</code></p>
 */

public class Utils {
	/** A set of primitive supporting classes */
	@SuppressWarnings("unchecked")
	public static final Set<Class<?>> have_primitives = Collections.unmodifiableSet(new HashSet<Class<?>>(Arrays.asList(
			Byte.class, Boolean.class, Short.class, Integer.class, Long.class, Float.class, Double.class, Void.class 
	)));
	
	/**
	 * Returns the primitive for the passed class if applicable, otherwise returns the passed class
	 * @param clazz The class to return the primitive for
	 * @return a class
	 */
	public static  Class<?> primitive(Class<?> clazz) {
		if(have_primitives.contains(clazz)) {
			try {
				return (Class<?>)clazz.getDeclaredField("TYPE").get(null);
			} catch (Exception e) {
				throw new RuntimeException("Failed to get primitive for [" + clazz.getName() + "]", e);
			}
		} 
		return clazz;
	}

}
