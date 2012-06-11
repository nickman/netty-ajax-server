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
package org.helios.netty.examples.state;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder;

/**
 * <p>Title: InstrumentedDelimiterBasedFrameDecoder</p>
 * <p>Description: Wrapped extension of {@link DelimiterBasedFrameDecoder} that counts the number of times it was called before returning a result.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.examples.state.InstrumentedDelimiterBasedFrameDecoder</code></p>
 */

public class InstrumentedDelimiterBasedFrameDecoder extends DelimiterBasedFrameDecoder {
	/** The number of calls with a null return */
	protected int callCount = 0;
	
    /**
     * {@inheritDoc}
     * @see org.jboss.netty.handler.codec.frame.DelimiterBasedFrameDecoder#decode(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.Channel, org.jboss.netty.buffer.ChannelBuffer)
     */
    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
    	Object decoded = super.decode(ctx, channel, buffer);
    	callCount++;
    	if(decoded!=null) {
    		StringReporter.frameDecodeCalls.set(channel, callCount);
    		//System.out.println("FD:"+  channel.getClass().getName());
    		callCount = 0;
    	}
    	return decoded;
    }


	/**
	 * Creates a new InstrumentedDelimiterBasedFrameDecoder
	 * @param maxFrameLength
	 * @param delimiter
	 */
	public InstrumentedDelimiterBasedFrameDecoder(int maxFrameLength,
			ChannelBuffer delimiter) {
		super(maxFrameLength, delimiter);
	}

	/**
	 * Creates a new InstrumentedDelimiterBasedFrameDecoder
	 * @param maxFrameLength
	 * @param delimiters
	 */
	public InstrumentedDelimiterBasedFrameDecoder(int maxFrameLength,
			ChannelBuffer... delimiters) {
		super(maxFrameLength, delimiters);
	}

	/**
	 * Creates a new InstrumentedDelimiterBasedFrameDecoder
	 * @param maxFrameLength
	 * @param stripDelimiter
	 * @param delimiter
	 */
	public InstrumentedDelimiterBasedFrameDecoder(int maxFrameLength,
			boolean stripDelimiter, ChannelBuffer delimiter) {
		super(maxFrameLength, stripDelimiter, delimiter);
	}

	/**
	 * Creates a new InstrumentedDelimiterBasedFrameDecoder
	 * @param maxFrameLength
	 * @param stripDelimiter
	 * @param delimiters
	 */
	public InstrumentedDelimiterBasedFrameDecoder(int maxFrameLength,
			boolean stripDelimiter, ChannelBuffer... delimiters) {
		super(maxFrameLength, stripDelimiter, delimiters);
	}

	/**
	 * Creates a new InstrumentedDelimiterBasedFrameDecoder
	 * @param maxFrameLength
	 * @param stripDelimiter
	 * @param failFast
	 * @param delimiter
	 */
	public InstrumentedDelimiterBasedFrameDecoder(int maxFrameLength,
			boolean stripDelimiter, boolean failFast, ChannelBuffer delimiter) {
		super(maxFrameLength, stripDelimiter, failFast, delimiter);
	}

	/**
	 * Creates a new InstrumentedDelimiterBasedFrameDecoder
	 * @param maxFrameLength
	 * @param stripDelimiter
	 * @param failFast
	 * @param delimiters
	 */
	public InstrumentedDelimiterBasedFrameDecoder(int maxFrameLength,
			boolean stripDelimiter, boolean failFast,
			ChannelBuffer... delimiters) {
		super(maxFrameLength, stripDelimiter, failFast, delimiters);
	}

}
