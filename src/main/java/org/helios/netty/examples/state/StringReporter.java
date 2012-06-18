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
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelLocal;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.socket.SocketChannel;

/**
 * <p>Title: StringReporter</p>
 * <p>Description: An upstream channel handler that expects Strings as messages and returns the length of the string</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.examples.state.StringReporter</code></p>
 */
@Sharable
public class StringReporter extends SimpleChannelUpstreamHandler {
	/** Channel local field to hold the number of times the current channel's frame decoder was called */
	public static final ChannelLocal<Integer> frameDecodeCalls = new ChannelLocal<Integer>(true);
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#messageReceived(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
	 */
	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		Object msg = e.getMessage();
		if(msg instanceof String) {
			int[] ret = new int[3];
			ret[0] = ((String)msg).length();
			ret[1] = frameDecodeCalls.get(e.getChannel());
			ret[2] = ((SocketChannel)e.getChannel()).getConfig().getReceiveBufferSize();
			// =====================================================================
			//  Uncomment next section to ditch the CompatibleObjectEncoder from the pipeline
			// =====================================================================
//			ChannelBuffer intBuffer = ChannelBuffers.buffer(12);
//			for(int i = 0; i < 3; i++) {
//				intBuffer.writeInt(ret[i]);
//			}
//			ctx.sendDownstream(new DownstreamMessageEvent(e.getChannel(), Channels.future(e.getChannel()), intBuffer, e.getChannel().getRemoteAddress()));
			// =====================================================================
			
			// =====================================================================
			// Comment the next line to ditch the CompatibleObjectEncoder from the pipeline
			// =====================================================================
			ctx.sendDownstream(new DownstreamMessageEvent(e.getChannel(), Channels.future(e.getChannel()), ret, e.getChannel().getRemoteAddress()));
			
			
		} else {
			System.err.println("WTF..... anything getting here should be a string but we got a [" + msg.getClass().getName() + "]");
		}
		super.messageReceived(ctx, e);
	}
}
