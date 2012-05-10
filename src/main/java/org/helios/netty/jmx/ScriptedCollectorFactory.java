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

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.management.ManagementFactory;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

/**
 * <p>Title: ScriptedCollectorFactory</p>
 * <p>Description: A factory to create scripted collectors from an uploaded script source.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.jmx.ScriptedCollectorFactory</code></p>
 * <p>Sample Script:<pre>
importPackage(javax.management);
importPackage(java.util);
importClass(org.helios.netty.jmx.MetricProvider);

var provider = new Object();
provider.addMetrics = function addMetrics(json) {}
provider.getProvidedMetricNames = function getProvidedMetricNames() { 
  println('MBeanServer:' + mbeanserver.getDefaultDomain());
  println('JMXHelper:' + jmxhelper);
  println('ObjectName:' + jmxhelper.objectName('java.lang:type=Threading'));
  return this.names; 
  
  
}
provider.handleNotification = function handleNotification(notification, handback) {}
provider.isNotificationEnabled  = function isNotificationEnabled(notification) {
  var notifType = notification.getType();
  return (MetricProvider.METRIC_NAME_NOTIFICATION==notifType) ||  (MetricProvider.METRIC_NOTIFICATION==notifType); 
}
provider.names = new HashSet();
provider.names.add("foo");
provider.names.add("bar");
</pre></p> 
 */

public class ScriptedCollectorFactory {
	/** The script engine instance */
	protected final ScriptEngine engine = new ScriptEngineManager().getEngineByExtension("js");
	/** The invoker */
	protected final Invocable invoker = (Invocable)engine;
	
	/**
	 * Creates a new ScriptedCollectorFactory
	 */
	public ScriptedCollectorFactory() {
		engine.put("mbeanserver", ManagementFactory.getPlatformMBeanServer());
		engine.put("jmxhelper", new JMXHelper());
	}
	
	
	/**
	 * Compiles the passed source text into a {@link MetricProvider}
	 * @param source The script source to compile
	 * @return the compiled {@link MetricProvider}
	 */
	public MetricProvider createMetricProvider(String source) {
		try {
			engine.eval(source);
			Object obj = engine.get("provider");
			MetricProvider mp = invoker.getInterface(obj, MetricProvider.class);
			return mp;
		} catch (Exception e) {
			throw new RuntimeException("Failed to compile script", e);
		}
	}
	
	public static void main(String[] args) {
		log("Script Compiler Test");
		FileReader reader = null;
		 BufferedReader breader = null;
		try {
			reader = new FileReader("/tmp/script.js");
			breader = new BufferedReader(reader);
			StringBuilder b = new StringBuilder();
			String line = null;
			while((line=breader.readLine())!=null) {
				b.append(line).append("\n");
			}
			log("Compiling....");
			MetricProvider mp = new ScriptedCollectorFactory().createMetricProvider(b.toString());
			log("Created MetricProvider [" + mp + "]");
			log("Names:" + mp.getProvidedMetricNames());
		} catch (Exception e) {
			e.printStackTrace(System.err);
		} finally {
			try { breader.close(); } catch (Exception e) {}
			try { reader.close(); } catch (Exception e) {}
		}
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
}
