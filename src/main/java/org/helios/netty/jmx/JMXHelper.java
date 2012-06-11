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

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMISocketFactory;
import java.util.Collections;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

/**
 * <p>Title: JMXHelper</p>
 * <p>Description: Static JMX Utility methods</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.jmx.JMXHelper</code></p>
 */
public class JMXHelper {
	/**
	 * Creates a JMX ObjectName from the passed string
	 * @param str The object name text
	 * @return a JMX ObjectName
	 */
	public static ObjectName objectName(CharSequence str) {
		if(str==null) throw new IllegalArgumentException("The passed string was null", new Throwable());
		try {
			return new ObjectName(str.toString());
		} catch (Exception e) {
			throw new RuntimeException("Failed to create object name for [" + str + "]");
		}
	}
	
	/**
	 * Creates, registers and starts a JMXConnectorServer
	 * @param bindInterface The interface to bind to
	 * @param serviceURL The JMXService URL
	 * @param server The MBeanServer to expose
	 */
	public static void fireUpJMXServer(final String bindInterface, final int serverSocketBacklog, CharSequence serviceURL, MBeanServer server) {
		try {
			fireUpJMXServer(bindInterface, serverSocketBacklog, new JMXServiceURL(serviceURL.toString()), server);
		} catch (Exception e) {
			throw new RuntimeException("Failed to start JMXServer on [" + serviceURL + "]", e);
		}
	}
	
	
	/**
	 * Creates, registers and starts a JMXConnectorServer
	 * @param bindInterface The interface to bind to
	 * @param serviceURL The JMXService URL
	 * @param server The MBeanServer to expose
	 */
	public static void fireUpJMXServer(final String bindInterface, final int serverSocketBacklog, JMXServiceURL serviceURL, MBeanServer server) {
		try {
			Map<String, Object> env = Collections.singletonMap("jmx.remote.rmi.server.socket.factory", (Object)new RMISocketFactory(){
				public ServerSocket createServerSocket(int port) throws IOException {
					return new ServerSocket(port, serverSocketBacklog, InetAddress.getByName(bindInterface));
				}
				public Socket createSocket(String host, int port) throws IOException {
					return new Socket(host, port);
				}
			});
			JMXConnectorServer jmxServer = JMXConnectorServerFactory.newJMXConnectorServer(serviceURL, env, server);
			server.registerMBean(jmxServer, JMXHelper.objectName("org.helios.netty:service=JMXConnectorServer,url=" + ObjectName.quote(serviceURL.toString())));
			jmxServer.start();
		} catch (Exception e) {
			throw new RuntimeException("Failed to start JMXServer on [" + serviceURL + "]", e);
		}
	}
	
	public static void fireUpRMIRegistry(final String bindInterface,  final int port)  {
		try {
			LocateRegistry.createRegistry(port);
		} catch (Exception e) {
			throw new RuntimeException("Failed to start RMIRegistry on [" + bindInterface + ":" + port + "]", e);
		}
	}
}
