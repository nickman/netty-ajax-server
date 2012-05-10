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
package org.helios.netty.ajax.handlergroups.submission;

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.helios.netty.jmx.MetricCollector;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.DownstreamMessageEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.jboss.netty.util.CharsetUtil;

/**
 * <p>Title: SubmissionHandler</p>
 * <p>Description: A channel handler to accept external metric submissions via HTTP. </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.ajax.handlergroups.submission.SubmissionHandler</code></p>
 */

public class SubmissionHandler implements ChannelUpstreamHandler  {

	/**
	 * {@inheritDoc}
	 * @see org.jboss.netty.channel.ChannelUpstreamHandler#handleUpstream(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.ChannelEvent)
	 */
	@Override
	public void handleUpstream(ChannelHandlerContext ctx, ChannelEvent e) throws Exception {
		final Channel channel = e.getChannel();
		if(e instanceof MessageEvent) {
			MessageEvent messageEvent = (MessageEvent)e;
			Object message = messageEvent.getMessage();
			if(message instanceof HttpRequest) {
				HttpRequest request = (HttpRequest)message;
				int metricCount = processMetric(request);
				HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
				response.setContent(ChannelBuffers.copiedBuffer("\n" + metricCount + "\n", CharsetUtil.UTF_8));
				response.setHeader(CONTENT_TYPE, "text/plain");
				ChannelFuture future = Channels.future(channel);
				ctx.sendDownstream(new DownstreamMessageEvent(channel, future, response, channel.getRemoteAddress()));
				future.addListener(ChannelFutureListener.CLOSE);				
			}
		}
		ctx.sendUpstream(e);
	}
	
	/**
	 * Parses the metric stream and passes the results to the metric collector
	 * @param request The submitted http request
	 * @return The number of metrics successfully extracted and processed.
	 */
	protected int processMetric(HttpRequest request) {
		QueryStringDecoder decoder = new QueryStringDecoder(request.getUri());
		Map<String,List<String>> params = decoder.getParameters();
		Map<String, Long> metrics = new HashMap<String, Long>();
		
		for(Map.Entry<String,List<String>> entry: params.entrySet()) {
			try {
				String metricName = entry.getKey();
				String value = entry.getValue().iterator().next();
				long lvalue = Long.parseLong(value.trim());
				metrics.put(metricName, lvalue);
			} catch (Exception e) {}
		}
		MetricCollector.getInstance().submitMetrics(metrics);
		return metrics.size();
	}

}
