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

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Title: CollectDType</p>
 * <p>Description: Enumerates the collectd data types</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.examples.codec.collectd.CollectDType</code></p>
 */

public enum CollectDType {
	/**  */
	COUNTER(new NumberType(){ public Long getNativeType(double dval) { return (long)dval;}}),
    /**  */
    GAUGE(new NumberType(){ public Double getNativeType(double dval) { return dval;}}), 
    /**  */
    DERIVE(new NumberType(){ public Integer getNativeType(double dval) { return (int)dval;}}), 
    /**  */
    ABSOLUTE(new NumberType(){ public Integer getNativeType(double dval) { return (int)dval;}});
	
	/** Decodes an ordinal into the corresponding CollectDType  */
	private static final Map<Byte, CollectDType> ORD2TYPE = new HashMap<Byte, CollectDType>(CollectDType.values().length);
	
	static {
		for(CollectDType type: CollectDType.values()) {
			ORD2TYPE.put(new Byte((byte) type.ordinal()), type);
		}
	}
	
	private static interface NumberType {
		public Number getNativeType(double dval);
	}
	
	private CollectDType(NumberType converter) {
		this.converter = converter;
	}
	
	private final NumberType converter;
	
	/**
	 * Converts the passed double to the type's native numeric
	 * @param d The double to convert
	 * @return the native numeric for this type
	 */
	public Number getNativeType(double d) {
		return converter.getNativeType(d);
	}
	
	/**
	 * Decodes an ordinal into the corresponding CollectDType 
	 * @param ordinal The oridnal to decode
	 * @return the type decoded from the passed ordinal
	 */
	public static CollectDType decode(byte ordinal) {
		CollectDType type = ORD2TYPE.get(ordinal);
		if(type==null) throw new IllegalArgumentException("The passed ordinal [" + ordinal + "] is not a valid CollectDType ordinal", new Throwable());
		return type;
	}
	
	/**
	 * 
	 * Returns the ordinal as a byte
	 * @return the ordinal as a byte
	 */
	public byte byteOrdinal() {
		return (byte)ordinal();
	}
}
