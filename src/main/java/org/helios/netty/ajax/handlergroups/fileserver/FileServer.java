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
package org.helios.netty.ajax.handlergroups.fileserver;

import org.helios.netty.ajax.PipelineModifier;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;

/**
 * <p>Title: FileServer</p>
 * <p>Description: Pipeline modifier to provide file server functionality</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.ajax.handlergroups.fileserver.FileServer</code></p>
 */

public class FileServer implements PipelineModifier {

	/**
	 * {@inheritDoc}
	 * @see org.helios.netty.ajax.PipelineModifier#modifyPipeline(org.jboss.netty.channel.ChannelPipeline)
	 */
	@Override
	public void modifyPipeline(final ChannelPipeline pipeline) {
//		for(String handlerName :pipeline.getNames()) {
//			pipeline.remove(handlerName);
//		}
//		pipeline.addLast("decoder", new HttpRequestDecoder());
//		pipeline.addLast("aggregator", new HttpChunkAggregator(65536));
//		pipeline.addLast("encoder", new HttpResponseEncoder());
//		pipeline.addLast("chunkedWriter", new ChunkedWriteHandler());
		if(pipeline.)
		pipeline.addLast("fileserver", new HttpStaticFileServerHandler());
	}

}
