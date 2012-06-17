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
 * <p>Title: CollectDItem</p>
 * <p>Description: Represents a hierarchical set of collectd metrics submitted in one datagram.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.examples.codec.collectd.CollectDItem</code></p>
 */

public class CollectDItem {
    /** The name of the host to associate with associated data values  */
    protected String hostName;
    /** The timestamp to associate with associated data values, UTC time format  */
    protected long time;
    /** The interval to associate with associated data values, UTC time format  */
    protected long interval;    
    
    /** The plugin name to associate with associated data values, e.g. "cpu"  */
    protected String pluginName;
    
    
    /** The plugin instance name to associate with associated data values, e.g. "1"  */
    protected String pluginInstance;
    /** The type name to associate with associated data values, e.g. "cpu"  */
    protected String typeName;
    /** The type instance name to associate with associated data values, e.g. "idle"  */
    protected String typeInstanceName;

}
