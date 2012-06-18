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
package org.helios.netty.examples.socketbuffer;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.Map;

import org.helios.netty.examples.state.ChannelHandlerProvider;
import org.helios.netty.examples.state.ChannelHandlerProviderFactory;
import org.helios.netty.examples.state.InstrumentedDelimiterBasedFrameDecoder;
import org.helios.netty.examples.state.SimpleNIOServer;
import org.helios.netty.examples.state.StringReporter;
import org.helios.netty.jmx.JMXHelper;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.handler.codec.serialization.CompatibleObjectEncoder;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.codec.string.StringEncoder;

/**
 * <p>Title: AdjustableSocketBufferSizeServer</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.examples.socketbuffer.AdjustableSocketBufferSizeServer</code></p>
 */

public class AdjustableSocketBufferSizeServer {
	/**
	 * Boots a SimpleNIOServer instance
	 * @param args None
	 */
	public static void main(String[] args) {
		// The DelimiterBasedFrameDecoder is not sharable
		// so we need to wrap it's configuration in a ChannelHandlerProviderFactory
		// so that a new one is created for each new pipeline created
		// The ChannelPipelineFactoryImpl is a stickler for types, so all primitive supporting 
		// types are assumed to be primitive
		ChannelHandlerProvider frameDecoder = ChannelHandlerProviderFactory.getInstance(
				"frameDecoder", 
				//DelimiterBasedFrameDecoder.class,
				InstrumentedDelimiterBasedFrameDecoder.class,
					Integer.MAX_VALUE, 
					true, 
					true, 
					new ChannelBuffer[]{ChannelBuffers.wrappedBuffer("|".getBytes())}
		);
		// The String decoder is sharable so we wrap it in a simple SharedChannelHandlerProvider
		ChannelHandlerProvider stringDecoder = ChannelHandlerProviderFactory.getInstance("stringDecoder", new StringDecoder());
		// Lastly, we need a "business" handler. It is sharable so we wrap it in a simple SharedChannelHandlerProvider
		ChannelHandlerProvider stringReporter = ChannelHandlerProviderFactory.getInstance("stringReporter", new StringReporter());
		
		
		// We want to send some numbers back to the caller, so we need an ObjectEncoder
		// We're going to use a simple groovy client to submit strings, so it needs to be a CompatibleObjectEncoder
		// which is not sharable so we need to wrap it's configuration in a ChannelHandlerProviderFactory
		ChannelHandlerProvider objectEncoder = ChannelHandlerProviderFactory.getInstance(
				"objectEncoder", 
				CompatibleObjectEncoder.class); 
		
		ChannelHandlerProvider stringEncoder = ChannelHandlerProviderFactory.getInstance(
				"stringEncoder", 
				StringEncoder.class); 
		
		
		// Create a map for the channel options
		Map<String, Object> channelOptions = new HashMap<String, Object>();
		channelOptions.put("connectTimeoutMillis", 100);
		
		channelOptions.put("reuseAddress", true);
		channelOptions.put("tcpNoDelay", true );
		channelOptions.put("soLinger", 20000);
		channelOptions.put("keepAlive", true );
		channelOptions.put("receiveBufferSize", 43690 );
		channelOptions.put("sendBufferSize", 2048 );

		channelOptions.put("child.tcpNoDelay", true );
		channelOptions.put("child.keepAlive", true );
		channelOptions.put("child.receiveBufferSize", 43690 );
		channelOptions.put("child.sendBufferSize", 2048 );

		
		// Create the server and start it 
		// Note that the objectEncoder is the only downstream handler, so its position in the pipeline is unimportant
		// but all the others **must** be in this order  ---------------------------------------------------------------------\/-----------------\/---------------------\/  

		// =====================================================================
		// Comment the next line to ditch the CompatibleObjectEncoder from the pipeline
		// =====================================================================		
		SimpleNIOServer server = new SimpleNIOServer(8080, channelOptions, objectEncoder, frameDecoder, stringDecoder, stringReporter);
		
		// =====================================================================
		// Uncomment the next line to ditch the CompatibleObjectEncoder from the pipeline
		// =====================================================================				
		//SimpleNIOServer server = new SimpleNIOServer(8080, channelOptions,  frameDecoder, stringDecoder, stringReporter);
		
		server.start();		
		JMXHelper. fireUpRMIRegistry("0.0.0.0", 8003);
		JMXHelper.fireUpJMXServer("0.0.0.0", 100, "service:jmx:rmi://hserval:8002/jndi/rmi://hserval:8003/jmxrmi", ManagementFactory.getPlatformMBeanServer());
		
		try { 
			Thread.currentThread().join();
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}
	
	public static void slog(Object msg) {
		System.out.println("[Server][" + Thread.currentThread() + "]:" + msg);
	}


}
