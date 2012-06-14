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
package org.helios.netty;

import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.jboss.netty.channel.ChannelHandler;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

/**
 * <p>Title: ChannelHandlerDump</p>
 * <p>Description: Utility class to analyze and categorize all ChannelHandler types found in the classpath</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.ChannelHandlerDump</code></p>
 */

public class ChannelHandlerDump {

	/**
	 * Boots the analyzer
	 * @param args None
	 */
	public static void main(String[] args) {
		Set<Class<? extends ChannelHandler>> sharable = new HashSet<Class<? extends ChannelHandler>>();
		Set<Class<? extends ChannelHandler>> notSharable = new HashSet<Class<? extends ChannelHandler>>();
		Set<URL> urls = new HashSet<URL>();
		for(Iterator<URL> urlIter = ClasspathHelper.forClassLoader().iterator(); urlIter.hasNext();) {
			URL url = urlIter.next();
			if(url.toString().toLowerCase().endsWith(".jar")) {
				urls.add(url);
			}
		}
		Reflections ref = new Reflections(new ConfigurationBuilder().setUrls(urls));
		for(Class<? extends ChannelHandler> clazz: ref.getSubTypesOf(ChannelHandler.class)) {
			if(Modifier.isAbstract(clazz.getModifiers())) continue;
			ChannelHandler.Sharable shareable = clazz.getAnnotation(ChannelHandler.Sharable.class);
			if(shareable==null) {
				notSharable.add(clazz);
			} else {
				sharable.add(clazz);
			}
		}
		StringBuilder b = new StringBuilder("\n\t==========================\n\tSharable Channel Handlers\n\t==========================");
		for(Class<? extends ChannelHandler> clazz: sharable) {
			b.append("\n\t\t").append(clazz.getName());
		}
		b.append("\n");
		log(b);
		b = new StringBuilder("\n\t==========================\n\tNon Sharable Channel Handlers\n\t==========================");
		for(Class<? extends ChannelHandler> clazz: notSharable) {
			b.append("\n\t\t").append(clazz.getName());
		}
		b.append("\n");
		log(b);		

	}
	
	/**
	 * Logger
	 * @param msg The message to print
	 */
	public static void log(Object msg) {
		System.out.println(msg);
	}

}
