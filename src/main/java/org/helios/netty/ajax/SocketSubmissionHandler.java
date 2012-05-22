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

import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;
import org.helios.netty.jmx.MetricCollector;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelHandler;

/**
 * <p>Title: SocketSubmissionHandler</p>
 * <p>Description: Last channel handler for handling socket metric submissions</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.ajax.SocketSubmissionHandler</code></p>
 */

public class SocketSubmissionHandler extends SimpleChannelHandler {
	/** Instance logger */
	protected final Logger log = Logger.getLogger(getClass());
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.SimpleChannelHandler#messageReceived(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
	 */
	@Override
	public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
		String metric = (String)e.getMessage();
		if(log.isDebugEnabled()) log.debug("\n\tProcessing Metric [" + metric + "]\n");
		try {
			String[] frags = metric.split(":");
			Double d = new Double(frags[1].trim());
			long value = d.longValue();
			MetricCollector.getInstance().submitMetric(frags[0], value);
			AtomicInteger counter = (AtomicInteger) ctx.getAttachment();
			if(counter==null) {
				counter = new AtomicInteger(0);
				ctx.setAttachment(counter);
			}
			counter.incrementAndGet();
			MetricCollector.getInstance().submitMetric(frags[0], value);
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
		super.messageReceived(ctx, e);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.SimpleChannelHandler#handleDownstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	@Override
	public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		AtomicInteger ai = (AtomicInteger) ctx.getAttachment();
		log.info("SocketSubmission Downstream:" + ai==null ? -1 : ai.get());
		super.handleDownstream(ctx, e);
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.SimpleChannelHandler#exceptionCaught(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ExceptionEvent)
	 */
	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
			throws Exception {
		log.error("Failed to process submission", e.getCause());
		super.exceptionCaught(ctx, e);
	}
}
