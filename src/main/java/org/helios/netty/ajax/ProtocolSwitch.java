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

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.jboss.netty.handler.codec.string.StringDecoder;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.logging.LoggingHandler;
import org.jboss.netty.util.CharsetUtil;

/**
 * <p>Title: ProtocolSwitch</p>
 * <p>Description: An upfront channel handler to determine if the incoming is HTTP or plain socket text for submissions</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.ajax.ProtocolSwitch</code></p>
 */

public class ProtocolSwitch extends FrameDecoder {
	/** The comma based string delimeter */
	private static final ChannelBuffer COMMA_DELIM = ChannelBuffers.wrappedBuffer(new byte[] { ',' });
	/** The maximum frame size */
	public static final int MAX_FRAME_SIZE = 65536;
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());

	/** An execution handler to hand off the metric submissions to */
	protected static final ExecutionHandler execHandler = new ExecutionHandler(Executors.newCachedThreadPool(			
			new ThreadFactory() {
				final AtomicInteger serial = new AtomicInteger(0);
				public Thread newThread(Runnable r) {
					Thread t = new Thread(r, "SocketMetricSubmissionThread#" + serial.incrementAndGet());
					t.setDaemon(true);
					return t;
				}
			}
	), false, true);
	/** The socket based metric submission handler */
	protected final SocketSubmissionHandler submissionHandler = new SocketSubmissionHandler();
	


	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.handler.codec.frame.FrameDecoder#decode(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.Channel, org.jboss.netty.buffer.ChannelBuffer)
	 */
	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
		// Will use the first two bytes to detect a protocol.
		if (buffer.readableBytes() < 2) {
			return null;
		}	
		ChannelPipeline pipeline = ctx.getPipeline();
		final int magic1 = buffer.getUnsignedByte(buffer.readerIndex());
		final int magic2 = buffer.getUnsignedByte(buffer.readerIndex() + 1);		
		if (!isHttp(magic1, magic2)) {
			log.info("Switching to Raw Socket");
			ChannelHandler ch = null;
			while((ch = pipeline.getFirst())!=null) {
				if(!(ch instanceof LoggingHandler)) {
					pipeline.remove(ch);
				}
			}
			pipeline.addLast("frameDecoder", new DelimiterBasedFrameDecoder(65536, COMMA_DELIM));
			pipeline.addLast("stringDecoder", new StringDecoder(CharsetUtil.UTF_8));
			pipeline.addLast("exec-handler", execHandler);
			pipeline.addLast("submission-handler", submissionHandler);
			
		} else {
			log.info("Switching to HTTP");
			ctx.getPipeline().remove(this);
		}		
		return buffer.readBytes(buffer.readableBytes());
	}
	
	/**
	 * Determines if the channel is carrying an HTTP request
	 * @param magic1 The first byte of the incoming request
	 * @param magic2 The second byte of the incoming request
	 * @return true if the incoming is HTTP, false otherwise
	 */
	private boolean isHttp(int magic1, int magic2) {
		 return
		 magic1 == 'G' && magic2 == 'E' || // GET
		 magic1 == 'P' && magic2 == 'O' || // POST
		 magic1 == 'P' && magic2 == 'U' || // PUT
		 magic1 == 'H' && magic2 == 'E' || // HEAD
		 magic1 == 'O' && magic2 == 'P' || // OPTIONS
		 magic1 == 'P' && magic2 == 'A' || // PATCH
		 magic1 == 'D' && magic2 == 'E' || // DELETE
		 magic1 == 'T' && magic2 == 'R' || // TRACE
		 magic1 == 'C' && magic2 == 'O';   // CONNECT
	}	

}
