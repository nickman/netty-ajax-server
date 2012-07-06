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
package org.helios.netty.unsafe;

import java.lang.reflect.Field;

import sun.misc.Unsafe;
/**
 * <p>Title: UnsafeMemory</p>
 * <p>Description: Optimized byte array manipulation sample</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Martin Thompson (from http://mechanical-sympathy.blogspot.com/2012/07/native-cc-like-performance-for-java.html) 
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.unsafe.UnsafeMemory</code></p>
 */

class UnsafeMemory
{
    private static final Unsafe unsafe;
    static
    {
        try
        {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe)field.get(null);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
 
    private static final long byteArrayOffset = unsafe.arrayBaseOffset(byte[].class);
    private static final long longArrayOffset = unsafe.arrayBaseOffset(long[].class);
    private static final long doubleArrayOffset = unsafe.arrayBaseOffset(double[].class);
 
    private static final int SIZE_OF_BOOLEAN = 1;
    private static final int SIZE_OF_INT = 4;
    private static final int SIZE_OF_LONG = 8;
 
    private int pos = 0;
    private final byte[] buffer;
 
    public UnsafeMemory(final byte[] buffer)
    {
        if (null == buffer)
        {
            throw new NullPointerException("buffer cannot be null");
        }
 
        this.buffer = buffer;
    }
 
    public void reset()
    {
        this.pos = 0;
    }
 
    public void putBoolean(final boolean value)
    {
        unsafe.putBoolean(buffer, byteArrayOffset + pos, value);
        pos += SIZE_OF_BOOLEAN;
    }
 
    public boolean getBoolean()
    {
        boolean value = unsafe.getBoolean(buffer, byteArrayOffset + pos);
        pos += SIZE_OF_BOOLEAN;
 
        return value;
    }
 
    public void putInt(final int value)
    {
        unsafe.putInt(buffer, byteArrayOffset + pos, value);
        pos += SIZE_OF_INT;
    }
 
    public int getInt()
    {
        int value = unsafe.getInt(buffer, byteArrayOffset + pos);
        pos += SIZE_OF_INT;
 
        return value;
    }
 
    public void putLong(final long value)
    {
        unsafe.putLong(buffer, byteArrayOffset + pos, value);
        pos += SIZE_OF_LONG;
    }
 
    public long getLong()
    {
        long value = unsafe.getLong(buffer, byteArrayOffset + pos);
        pos += SIZE_OF_LONG;
 
        return value;
    }
 
    public void putLongArray(final long[] values)
    {
        putInt(values.length);
       
        long bytesToCopy = values.length << 3;
        unsafe.copyMemory(values, longArrayOffset,
                          buffer, byteArrayOffset + pos,
                          bytesToCopy);
        pos += bytesToCopy;
    }
 
    public long[] getLongArray()
    {
        int arraySize = getInt();
        long[] values = new long[arraySize];
 
        long bytesToCopy = values.length << 3;
        unsafe.copyMemory(buffer, byteArrayOffset + pos,
                          values, longArrayOffset,
                          bytesToCopy);
        pos += bytesToCopy;
 
        return values;
    }
 
    public void putDoubleArray(final double[] values)
    {
        putInt(values.length);
 
        long bytesToCopy = values.length << 3;
        unsafe.copyMemory(values, doubleArrayOffset,
                          buffer, byteArrayOffset + pos,
                          bytesToCopy);
        pos += bytesToCopy;
    }
 
    public double[] getDoubleArray()
    {
        int arraySize = getInt();
        double[] values = new double[arraySize];
 
        long bytesToCopy = values.length << 3;
        unsafe.copyMemory(buffer, byteArrayOffset + pos,
                          values, doubleArrayOffset,
                          bytesToCopy);
        pos += bytesToCopy;
 
        return values;
    }
}