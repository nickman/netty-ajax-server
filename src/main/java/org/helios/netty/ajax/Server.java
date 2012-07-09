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
package org.helios.netty.ajax;

import java.io.File;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import org.apache.log4j.Logger;
import org.helios.netty.ajax.handlergroups.URIHandler;
import org.helios.netty.ajax.handlergroups.fileserver.HttpStaticFileServerHandler;
import org.helios.netty.jmx.MetricCollector;
import org.helios.netty.jmx.ThreadPoolFactory;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Log4JLoggerFactory;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

/**
 * <p>Title: Server</p>
 * <p>Description: The server launcher.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.ajax.Server</code></p>
 */
public class Server {
	/** Static class logger */
	protected static final Logger LOG = Logger.getLogger(Server.class);
	
	/** The netty server boss pool */
	protected final Executor bossPool;
	/** The netty server worker pool */
	protected final Executor workerPool;
	/** The netty bootstrap */
	protected final ServerBootstrap bstrap;
	/** The netty pipeline factory */
	protected final ChannelPipelineFactory pipelineFactory;
	/** The netty channel factory */
	protected final ChannelFactory channelFactory;
	/** The Inet socket that the server will listen on */
	protected final InetSocketAddress isock;
	/** The static content directory */
	protected final String contentRoot;
	
	/** The default binding interface */
	public static final String DEFAULT_INTERFACE = "0.0.0.0";
	/** The default binding port */
	public static final int DEFAULT_PORT = 8087;
	/** The default static content path */
	public static final String DEFAULT_CONTENT_DIR = String.format(".%ssrc%smain%sresources%swww", File.separator, File.separator, File.separator, File.separator);
	
	
	
	/**
	 * Boots the server.
	 * @param args Command line args are:<ol>
	 * <li>The binding interface in the form of an IP address or a host name</li>
	 * <li>The binding port</li>
	 * <li>The static content root directory
	 * </ol>
	 */
	public static void main(String[] args) {
		//BasicConfigurator.configure();
		InternalLoggerFactory.setDefaultFactory(new Log4JLoggerFactory());
		String iface = null;
		String root = null;
		int port = -1;
		if(args.length>0) {
			iface = args[0];
		}
		if(args.length>1) {
			try {
				port = Integer.parseInt(args[1]);
			} catch (Exception e) {
				port = DEFAULT_PORT;
			}
		}
		if(args.length>2) {
			root = args[2];
		}
		
		if(iface==null) {
			iface = DEFAULT_INTERFACE;
		}
		if(port==-1) {
			port = DEFAULT_PORT;
		}
		if(root==null) {
			root = DEFAULT_CONTENT_DIR;
		}
		new Server(iface, port, root);
	}
	
	
	/**
	 * Creates a new Server
	 * @param iface The binding interface
	 * @param port the listening port
	 * @param root The root content directory
	 */
	public Server(String iface, int port, String root) {
		LOG.info("Starting Netty-Ajax Server on [" + iface + ":" + port + "]");
		this.contentRoot = root;
		HttpStaticFileServerHandler.contentRoot = root;
		isock = new InetSocketAddress(iface, port);
		MetricCollector collector = MetricCollector.getInstance(5000);
		bossPool = ThreadPoolFactory.newCachedThreadPool(getClass().getPackage().getName(), "boss");
		workerPool =  ThreadPoolFactory.newCachedThreadPool(getClass().getPackage().getName(), "worker");
		pipelineFactory = new ServerPipelineFactory(getPipelineModifiers());
		((ServerPipelineFactory)pipelineFactory).addModifier(collector.getName(), collector);
		channelFactory = new NioServerSocketChannelFactory(bossPool, workerPool);
		bstrap = new ServerBootstrap(channelFactory);
		bstrap.setPipelineFactory(pipelineFactory);
		bstrap.setOption("child.keepAlive", true);
		bstrap.bind(isock);
		LOG.info("Netty-Ajax Server Started with Root [" + contentRoot + "]");		
		try { Thread.currentThread().join(); } catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
	
	protected Map<String, PipelineModifier> getPipelineModifiers() {
		Map<String, PipelineModifier> map = new ConcurrentHashMap<String, PipelineModifier>();
		Set<URL> urls = new HashSet<URL>();
		for(Iterator<URL> urlIter = ClasspathHelper.forClassLoader().iterator(); urlIter.hasNext();) {
			URL url = urlIter.next();
			if(url.toString().toLowerCase().endsWith(".jar") || !url.toString().toLowerCase().contains(".")) {
				urls.add(url);
			}
		}
		Reflections ref = new Reflections(new ConfigurationBuilder().setUrls(urls));
		for(Class<?> clazz: ref.getTypesAnnotatedWith(URIHandler.class)) {
			if(PipelineModifier.class.isAssignableFrom(clazz)) {
				URIHandler uhandler = clazz.getAnnotation(URIHandler.class);
				try {
					PipelineModifier pm = (PipelineModifier)clazz.newInstance();
					String[] names = uhandler.uri();
					for(String name: names) {
						name = name.trim().toLowerCase();
						if(map.containsKey(name)) {
							LOG.warn("The handler [" + pm.getName() + "] offering URI [" + name + "] could not be registered as that URI is already registered" );
						} else {
							map.put(name, pm);
						}
					}
				} catch (Exception e) {
					LOG.error("Failed to create PipelineModifier instance from class [" + clazz.getName() + "]");
				}
			}
		}
		LOG.info("Discovered PipelineModifiers:" + map);
		return map;
	}

}
