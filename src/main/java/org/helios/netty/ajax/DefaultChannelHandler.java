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

import org.apache.log4j.Logger;
import org.helios.netty.ajax.handlergroups.fileserver.FileServer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpRequest;

/**
 * <p>Title: DefaultChannelHandler</p>
 * <p>Description: The initial and default channel handler inserted into all pipelines. This handler is intended to
 * examine the request URI and reconfigure the pipeline to handle the next request.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.ajax.DefaultChannelHandler</code></p>
 */

public class DefaultChannelHandler extends SimpleChannelUpstreamHandler {
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());

	protected FileServer fs = new FileServer();
	
	/**
	 * Creates a new DefaultChannelHandler
	 */
	public DefaultChannelHandler() {
		
		
	}
	
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) {    	
        HttpRequest request = (HttpRequest)e.getMessage();
        PipelineModifier modifier = getModifier(request.getUri());
        modifier.modifyPipeline(ctx.getPipeline());
        ctx.sendUpstream(e);
    }
    
    protected PipelineModifier getModifier(String uri) {
    	return fs;
    }

}
