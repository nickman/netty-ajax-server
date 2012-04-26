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
package org.helios.netty.ajax.handlergroups.streamer;

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.PARTIAL_CONTENT;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import org.helios.netty.ajax.SharedChannelGroup;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelDownstreamHandler;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.util.CharsetUtil;
import org.json.JSONObject;

/**
 * <p>Title: StreamingHandler</p>
 * <p>Description: The handler for pushing events to the client using HTTP streaming</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.ajax.handlergroups.streamer.StreamingHandler</code></p>
 */
public class StreamingHandler implements ChannelDownstreamHandler, ChannelUpstreamHandler {
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelDownstreamHandler#handleDownstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	@Override
	public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		// This handler is probably the last handler, but in case there are more upstream
		// we will make sure that the ChannelEvent is a MessageEvent and that the message event's
		// message is a JSONObject or a CharSequence. If not, we send the event downstream.
		if(!(e instanceof MessageEvent)) {
            ctx.sendDownstream(e);
            return;
        }
		Object message = ((MessageEvent)e).getMessage();
		if(!(message instanceof JSONObject) && !(message instanceof CharSequence)) {
            ctx.sendDownstream(e);
            return;			
		}
		Channel channel = e.getChannel();
		StringBuilder b = new StringBuilder("\n").append(message).append("--").append(channel.getId()).append("\n");
		HttpResponse response = new DefaultHttpResponse(HTTP_1_1, PARTIAL_CONTENT);
		response.setContent(ChannelBuffers.copiedBuffer(b, CharsetUtil.UTF_8));
		response.setHeader(CONTENT_TYPE, "application/json");
		ctx.sendDownstream(new DownstreamMessageEvent(channel, Channels.future(channel), response, channel.getRemoteAddress()));
	}

	/**
	 * The upstream handler does nothing if the channel is already registered in the shared channel group.
	 * If it is not, it sends the multipart partial content HTTP response back to the client in order to initialize
	 * the stream and then registers the channel in the shared group.
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelUpstreamHandler#handleUpstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		final Channel channel = e.getChannel();
		if(channel.isOpen() && SharedChannelGroup.getInstance().add(channel)) {
			StringBuilder b = new StringBuilder("\n--").append(channel.getId()).append("\n");
			HttpResponse response = new DefaultHttpResponse(HTTP_1_1, PARTIAL_CONTENT);
			response.setContent(ChannelBuffers.copiedBuffer(b, CharsetUtil.UTF_8));
			response.setHeader(CONTENT_TYPE, String.format("multipart/x-mixed-replace;boundary=\"%s\"", channel.getId()));
			ctx.sendDownstream(new DownstreamMessageEvent(channel, Channels.future(channel), response, channel.getRemoteAddress()));
		}
		ctx.sendUpstream(e);		
	}

}
