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
package org.helios.netty.examples;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelDownstreamHandler;

/**
 * <p>Title: ConditionalCompressionHandler</p>
 * <p>Description: Adds or removes the compression handler depending on the size of the ChannelBuffer.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.examples.ConditionalCompressionHandler</code></p>
 */

		public class ConditionalCompressionHandler extends SimpleChannelDownstreamHandler {
			/** The minimum size of a payload to be compressed */
			protected final int sizeThreshold;
			/** The name of the handler to remove if the payload is smaller than specified sizeThreshold */
			protected final String nameOfCompressionHandler;
			/** The compression handler */
			protected volatile ChannelHandler compressionHandler = null;
			
			/**
			 * Creates a new ConditionalCompressionHandler
			 * @param sizeThreshold The minimum size of a payload to be compressed 
			 * @param nameOfCompressionHandler The name of the handler to remove if the payload is smaller than specified sizeThreshold
			 */
			public ConditionalCompressionHandler(int sizeThreshold, String nameOfCompressionHandler) {
				this.sizeThreshold = sizeThreshold;
				this.nameOfCompressionHandler = nameOfCompressionHandler;
			}
			
			/**
			 * see org.jboss.netty.channel.SimpleChannelDownstreamHandler
			 */
			public void writeRequested(ChannelHandlerContext ctx, MessageEvent e) {
				// If the message is not a ChannelBuffer, hello ClassCastException !
				ChannelBuffer cb = (ChannelBuffer)e.getMessage();
				// Check to see if we already removed the handler
				boolean pipelineContainsCompressor = ctx.getPipeline().getContext(nameOfCompressionHandler)!=null;
				if(cb.readableBytes()<sizeThreshold) {		
					if(pipelineContainsCompressor) {
						// The payload is too small to be compressed but the pipeline contains the compression handler
						// so we need to remove it.
						compressionHandler = ctx.getPipeline().remove(nameOfCompressionHandler);
					}
				} else {
					// We want to compress the payload, let's make sure the compressor is there
					if(!pipelineContainsCompressor) {
						// Oops, it's not there, so lets put it in
						ctx.getPipeline().addAfter(ctx.getName(), nameOfCompressionHandler , compressionHandler);
					}
				}
			}
			
		}
