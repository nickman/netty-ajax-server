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
package org.helios.netty.examples.timeout;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.helios.netty.examples.state.SimpleNIOClient;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Log4JLoggerFactory;

/**
 * <p>Title: TimeoutTest</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.examples.timeout.TimeoutTest</code></p>
 */

public class TimeoutTest {

	/**
	 * Test case for SimpleNIOClient
	 * @param args None for now
	 */
	public static void main(String[] args) {
		// Supress logging to keep output clean
		InternalLoggerFactory.setDefaultFactory(new Log4JLoggerFactory());
		BasicConfigurator.resetConfiguration();
		Logger.getRootLogger().setLevel(Level.OFF);
		// Timeout on I/O		
		timeoutTest(10, 500);
		// Wait a bit so output is clear
		try { Thread.sleep(2000); } catch (Exception e) {}
		System.out.println("===============================");
		// Timeout on future
		timeoutTest(500, 10);
		try { Thread.currentThread().join(5000); } catch (Exception e) {}
		
		
	}
	
	public static void timeoutTest(long ioTimeout, long futureTimeout) {
		// Create a map with the channel options
		Map<String, Object> options = new HashMap<String, Object>();
		options.put("connectTimeoutMillis", ioTimeout);
		// Create the client
		// Not providing any handlers since we're not using any for this test
		SimpleNIOClient client = new SimpleNIOClient(options);
		// Issue a connect operation
		ChannelFuture cf = client.connect("heliosapm.com", 80);		
		// Add a completion listener
		cf.addListener(new ChannelFutureListener() {
			public void operationComplete(ChannelFuture future) throws Exception {
				if(future.isSuccess()) {
					clog("F: Connected:" + future.isDone());
				} else {
					if(future.isCancelled()) {
						clog("Request Cancelled");
					} else {
						clog("Connect Exception:Success: " + future.isSuccess() + "  Done: " + future.isDone()  + "  Cause: "+ future.getCause());
					}
				}
			}
		});
		// Wait at least futureTimeout for the operation to complete
		cf.awaitUninterruptibly(futureTimeout);
		// If the operation is not complete, cancel it.
		if(!cf.isDone()) {
			clog("Channel Future Still Waiting. Cancelled:" + cf.cancel());
		}
	}
	
	public static void clog(Object msg) {
		System.out.println("[Client][" + Thread.currentThread() + "]:" + msg);
	}

}
