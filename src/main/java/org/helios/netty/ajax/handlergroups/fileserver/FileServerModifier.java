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
import org.helios.netty.ajax.handlergroups.URIHandler;
import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;

/**
 * <p>Title: FileServerModifier</p>
 * <p>Description: Pipeline modifier to provide file server functionality</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.ajax.handlergroups.fileserver.FileServerModifier</code></p>
 */
@URIHandler(uri={"ui", ""})
public class FileServerModifier implements PipelineModifier {
	/** The handler that this modifier adds at the end of the pipeline */
	protected final ChannelHandler handler = new HttpStaticFileServerHandler();
	/** The name of the handler this modifier adds */
	public static final String NAME = "fileserver";
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.netty.ajax.PipelineModifier#getChannelHandler()
	 */
	public ChannelHandler getChannelHandler() {
		return handler;
	}

	/**
	 * {@inheritDoc}
	 * @see org.helios.netty.ajax.PipelineModifier#modifyPipeline(org.jboss.netty.channel.ChannelPipeline)
	 */
	@Override
	public void modifyPipeline(final ChannelPipeline pipeline) {
		if(pipeline.get(NAME)==null) {
			pipeline.addLast(NAME, handler);
		}
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.helios.netty.ajax.PipelineModifier#getName()
	 */
	public String getName() {
		return NAME;
	}

}
