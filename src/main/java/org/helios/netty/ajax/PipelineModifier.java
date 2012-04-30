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

import org.jboss.netty.channel.ChannelHandler;
import org.jboss.netty.channel.ChannelPipeline;

/**
 * <p>Title: PipelineModifier</p>
 * <p>Description: Defines a class that modifies a pipeline for a specific purpose</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.ajax.PipelineModifier</code></p>
 */

public interface PipelineModifier {
	/**
	 * Modifies the passed pipeline to provide specific functionality
	 * @param pipeline The pipeline to modify
	 */
	public void modifyPipeline(ChannelPipeline pipeline);
	
	/**
	 * Returns the name of this modifier
	 * @return the name of this modifier
	 */
	public String getName();
	
	/**
	 * Returns the channel handler to insert into the pipeline
	 * @return the channel handler to insert into the pipeline
	 */
	public ChannelHandler getChannelHandler();
}
