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
package org.helios.netty.examples.jmx;

import java.lang.management.ManagementFactory;
import java.util.Map;

import javax.management.ObjectName;

import org.helios.netty.jmx.JMXHelper;
import org.jboss.netty.bootstrap.Bootstrap;

/**
 * <p>Title: BootstrapJMXManager</p>
 * <p>Description: JMX MBean to add a management layer onto a {@link Bootstrap}.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.examples.jmx.BootstrapJMXManager</code></p>
 */

public class BootstrapJMXManager implements BootstrapJMXManagerMBean  {
	/** The boostrap to manager */
	protected final Bootstrap bootstrap;
	/** The JMX ObjectName that the management interface will be registered as */
	protected final ObjectName objectName;
	/**
	 * Creates a new BootstrapJMXManager 
	 * @param bootstrap The bootstrap to manager
	 * @param objectName The JMX ObjectName that the management interface will be registered as
	 */
	public BootstrapJMXManager(Bootstrap bootstrap, CharSequence objectName) {
		this.bootstrap = bootstrap;
		this.objectName = JMXHelper.objectName(objectName);
		try {
			ManagementFactory.getPlatformMBeanServer().registerMBean(this, this.objectName);
		} catch (Exception e) {
			throw new RuntimeException("Failed to register BootstrapJMXManager [" + objectName + "]", e );
		}
	}
	
	/**
	 * Returns the map of channel options
	 * @return the map of channel options
	 */
	public Map<String, Object> getChannelOptions() {
		return bootstrap.getOptions();
	}
	
	/**
	 * Returns the option value for the passed option name
	 * @param name The name of the channel option
	 * @return The value of the channel option or null if it has not been set
	 */
	public Object getChannelOption(String name) {
		return bootstrap.getOption(name);
	}
	
	/**
	 * Sets a channel option
	 * @param name The name of the option to set
	 * @param value The value to set the option to
	 */
	public void setChannelOption(String name, int value) {
		bootstrap.setOption(name, value);
	}
	
	/**
	 * Sets a channel option
	 * @param name The name of the option to set
	 * @param value The value to set the option to
	 */
	public void setChannelOption(String name, boolean value) {
		bootstrap.setOption(name, value);
	}
	
	/**
	 * Sets a channel option
	 * @param name The name of the option to set
	 * @param value The value to set the option to
	 */
	public void setChannelOption(String name, String value) {
		bootstrap.setOption(name, value);
	}

	
	
	
}
