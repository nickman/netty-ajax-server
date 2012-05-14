
package org.helios.netty.ajax.handlergroups.fileserver;

import static org.jboss.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static org.jboss.netty.handler.codec.http.HttpHeaders.setContentLength;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static org.jboss.netty.handler.codec.http.HttpMethod.GET;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.METHOD_NOT_ALLOWED;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.jboss.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import javax.activation.MimetypesFileTypeMap;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelFutureProgressListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.DefaultFileRegion;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.FileRegion;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.handler.stream.ChunkedFile;
import org.jboss.netty.util.CharsetUtil;

/**
 * Modified for Netty Ajax Server example project
 * to read content from a specified directory.
 * 
 * A simple handler that serves incoming HTTP requests to send their respective
 * HTTP responses.  It also implements {@code 'If-Modified-Since'} header to
 * take advantage of browser cache, as described in
 * <a href="http://tools.ietf.org/html/rfc2616#section-14.25">RFC 2616</a>.
 *
 * <h3>How Browser Caching Works</h3>
 *
 * Web browser caching works with HTTP headers as illustrated by the following
 * sample:
 * <ol>
 * <li>Request #1 returns the content of <code>/file1.txt</code>.</li>
 * <li>Contents of <code>/file1.txt</code> is cached by the browser.</li>
 * <li>Request #2 for <code>/file1.txt</code> does return the contents of the
 *     file again. Rather, a 304 Not Modified is returned. This tells the
 *     browser to use the contents stored in its cache.</li>
 * <li>The server knows the file has not been modified because the
 *     <code>If-Modified-Since</code> date is the same as the file's last
 *     modified date.</li>
 * </ol>
 *
 * <pre>
 * Request #1 Headers
 * ===================
 * GET /file1.txt HTTP/1.1
 *
 * Response #1 Headers
 * ===================
 * HTTP/1.1 200 OK
 * Date:               Tue, 01 Mar 2011 22:44:26 GMT
 * Last-Modified:      Wed, 30 Jun 2010 21:36:48 GMT
 * Expires:            Tue, 01 Mar 2012 22:44:26 GMT
 * Cache-Control:      private, max-age=31536000
 *
 * Request #2 Headers
 * ===================
 * GET /file1.txt HTTP/1.1
 * If-Modified-Since:  Wed, 30 Jun 2010 21:36:48 GMT
 *
 * Response #2 Headers
 * ===================
 * HTTP/1.1 304 Not Modified
 * Date:               Tue, 01 Mar 2011 22:44:28 GMT
 *
 * </pre>
 */
public class HttpStaticFileServerHandler extends SimpleChannelUpstreamHandler {

    public static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    public static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
    public static final int HTTP_CACHE_SECONDS = 60;
    protected static final Logger LOG = Logger.getLogger(HttpStaticFileServerHandler.class);
    /** Indicates if content will be loaded from a jar as resources, or from the file system */
    protected final boolean inJar;
    /** This class's class loader */
    protected final ClassLoader classLoader = getClass().getClassLoader();
    /** The content root representation */
    public static String contentRoot;
    
    /** A cache of loaded resources shared amongst file server handler instances */
    protected static final Map<String, HttpResponse> contentCache = new ConcurrentHashMap<String, HttpResponse>(1024);
    /** The Mime type assigner */
    protected static final MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
    
    static {
    	mimeTypesMap.addMimeTypes("application/js");
    }
    

    /**
     * Creates a new HttpStaticFileServerHandler
     */
    public HttpStaticFileServerHandler() {
    	inJar = getClass().getProtectionDomain().getCodeSource().getLocation().toString().toLowerCase().endsWith(".jar");
    	LOG.info("HTTP File Server Resource Mode:" + inJar);
    }
    
    
    /**
     * {@inheritDoc}
     * @see org.jboss.netty.channel.SimpleChannelUpstreamHandler#messageReceived(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.MessageEvent)
     */
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        HttpRequest request = (HttpRequest) e.getMessage();
        if (request.getMethod() != GET) {
            sendError(ctx, METHOD_NOT_ALLOWED);
            return;
        }

        String path = request.getUri();
        if (path == null) {
            sendError(ctx, FORBIDDEN);
            return;
        }

        if(path.equals("")) {
        	path = "index.html";
        }
        if(path.equals("/")) {
        	path = "index.html";
        }
        
        ChannelFuture writeFuture =  null;
        if(inJar) {
        	writeFuture = writeResponseFromResource(ctx, e, request, path);
        } else {
        	writeFuture = writeResponseFromFile(ctx, e, request, path);
        }

        // Decide whether to close the connection or not.
        if (writeFuture!=null && !isKeepAlive(request)) {
            // Close the connection when the whole content is written out.
            writeFuture.addListener(ChannelFutureListener.CLOSE);
        }
    }

	/**
	 * Writes the content request response from the jar
	 * @param ctx
	 * @param e
	 * @param request
	 * @param path
	 * @return
	 * @throws ParseException
	 * @throws IOException
	 */
	private ChannelFuture writeResponseFromResource(ChannelHandlerContext ctx,
			MessageEvent e, HttpRequest request, String path)
			throws ParseException, IOException {
		InputStream is = null;
		ByteArrayOutputStream baos = null;
		try {
			StringBuilder b = new StringBuilder(path);
			if(b.charAt(0)=='/') {
				b.deleteCharAt(0);
			}
			b.insert(0, "www/");
			String resourcePath = b.toString();
			LOG.info("HTTP File Server Request [" + resourcePath + "]");
			HttpResponse response = contentCache.get(resourcePath);
			if(response==null) {
				synchronized(contentCache) {
					response = contentCache.get(resourcePath);
					if(response==null) {
						is = classLoader.getResourceAsStream(b.toString());
						if(is.available()==0) {
				            sendError(ctx, NOT_FOUND);
				            return null;				
						}
						baos = new ByteArrayOutputStream(is.available());
						int bt = -1;
						while((bt=is.read())!=-1) {
							baos.write(bt);
						}
						byte[] content = baos.toByteArray();
						response = new DefaultHttpResponse(HTTP_1_1, OK);
				        setContentLength(response, content.length);
				        setContentTypeHeader(response, resourcePath);
				        setDateAndCacheHeaders(response, resourcePath);
				        response.setContent(ChannelBuffers.wrappedBuffer(content));
				        contentCache.put(resourcePath, response);
					}
				}
			}
	        Channel ch = e.getChannel();

	        // Write the initial line and the header.
	        return ch.write(response);

			
		} finally {
			if(is!=null) try { is.close(); } catch (Exception e2) {}
			if(baos!=null) try { baos.close(); } catch (Exception e3) {}
		}
	}

    
	/**
	 * Writes the content request response from the file system
	 * @param ctx
	 * @param e
	 * @param request
	 * @param path
	 * @return
	 * @throws ParseException
	 * @throws IOException
	 */
	private ChannelFuture writeResponseFromFile(ChannelHandlerContext ctx,
			MessageEvent e, HttpRequest request, String path)
			throws ParseException, IOException {
		File file = new File(contentRoot + File.separator + path);
        if (file.isHidden() || !file.exists()) {
            sendError(ctx, NOT_FOUND);
            return null;
        }
        if (!file.isFile()) {
            sendError(ctx, FORBIDDEN);
            return null;
        }

        // Cache Validation
        String ifModifiedSince = request.getHeader(HttpHeaders.Names.IF_MODIFIED_SINCE);
        if (ifModifiedSince != null && !ifModifiedSince.equals("")) {
            SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
            Date ifModifiedSinceDate = dateFormatter.parse(ifModifiedSince);

            // Only compare up to the second because the datetime format we send to the client does not have milliseconds 
            long ifModifiedSinceDateSeconds = ifModifiedSinceDate.getTime() / 1000;
            long fileLastModifiedSeconds = file.lastModified() / 1000;
            if (ifModifiedSinceDateSeconds == fileLastModifiedSeconds) {
                sendNotModified(ctx);
                return null;
            }
        }
        
        RandomAccessFile raf;
        try {
            raf = new RandomAccessFile(file, "r");
        } catch (FileNotFoundException fnfe) {
            sendError(ctx, NOT_FOUND);
            return null;
        }
        long fileLength = raf.length();

        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
        setContentLength(response, fileLength);
        setContentTypeHeader(response, file.getAbsolutePath());
        setDateAndCacheHeaders(response, file);
        
        Channel ch = e.getChannel();

        // Write the initial line and the header.
        ch.write(response);

        // Write the content.
        ChannelFuture writeFuture;
        if (ch.getPipeline().get(SslHandler.class) != null) {
            // Cannot use zero-copy with HTTPS.
            writeFuture = ch.write(new ChunkedFile(raf, 0, fileLength, 8192));
        } else {
            // No encryption - use zero-copy.
            final FileRegion region =
                new DefaultFileRegion(raf.getChannel(), 0, fileLength);
            writeFuture = ch.write(region);
            final String finalPath = path;
            writeFuture.addListener(new ChannelFutureProgressListener() {
                public void operationComplete(ChannelFuture future) {
                    region.releaseExternalResources();
                }

                public void operationProgressed(
                        ChannelFuture future, long amount, long current, long total) {
                    System.out.printf("%s: %d / %d (+%d)%n", finalPath, current, total, amount);
                }
            });
        }
		return writeFuture;
	}

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
            throws Exception {
        Channel ch = e.getChannel();
        Throwable cause = e.getCause();
        if (cause instanceof TooLongFrameException) {
            sendError(ctx, BAD_REQUEST);
            return;
        }

        cause.printStackTrace();
        if (ch.isConnected()) {
            sendError(ctx, INTERNAL_SERVER_ERROR);
        }
    }


    private void sendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, status);
        response.setHeader(CONTENT_TYPE, "text/plain; charset=UTF-8");
        response.setContent(ChannelBuffers.copiedBuffer(
                "Failure: " + status.toString() + "\r\n",
                CharsetUtil.UTF_8));

        // Close the connection as soon as the error message is sent.
        ctx.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
    }
    
    /**
     * When file timestamp is the same as what the browser is sending up, send a "304 Not Modified"
     * 
     * @param ctx
     *            Context
     */
    private void sendNotModified(ChannelHandlerContext ctx) {
        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.NOT_MODIFIED);
        setDateHeader(response);

        // Close the connection as soon as the error message is sent.
        ctx.getChannel().write(response).addListener(ChannelFutureListener.CLOSE);
    }
    
    /**
     * Sets the Date header for the HTTP response
     * 
     * @param response
     *            HTTP response
     */
    private void setDateHeader(HttpResponse response) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        Calendar time = new GregorianCalendar();
        response.setHeader(HttpHeaders.Names.DATE, dateFormatter.format(time.getTime()));
    }
    
    /**
     * Sets the Date and Cache headers for the HTTP Response
     * 
     * @param response
     *            HTTP response
     * @param fileToCache
     *            file to extract content type
     */
    private void setDateAndCacheHeaders(HttpResponse response, File fileToCache) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        // Date header
        Calendar time = new GregorianCalendar();
        response.setHeader(HttpHeaders.Names.DATE, dateFormatter.format(time.getTime()));

        // Add cache headers
        time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);
        response.setHeader(HttpHeaders.Names.EXPIRES, dateFormatter.format(time.getTime()));
        response.setHeader(HttpHeaders.Names.CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS);
        response.setHeader(HttpHeaders.Names.LAST_MODIFIED, dateFormatter.format(new Date(fileToCache.lastModified())));
    }
    
    /**
     * @param response
     * @param resourceName
     */
    private void setDateAndCacheHeaders(HttpResponse response, String resourceName) {
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        // Date header
        Calendar time = new GregorianCalendar();
        response.setHeader(HttpHeaders.Names.DATE, dateFormatter.format(time.getTime()));

        // Add cache headers
        time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);
        response.setHeader(HttpHeaders.Names.EXPIRES, dateFormatter.format(time.getTime()));
        response.setHeader(HttpHeaders.Names.CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS);
        response.setHeader(HttpHeaders.Names.LAST_MODIFIED, dateFormatter.format(new Date()));
    }
    

    /**
     * Sets the content type header for the HTTP Response
     * 
     * @param response
     *            HTTP response
     * @param file
     *            file to extract content type
     */
    private void setContentTypeHeader(HttpResponse response, String resourceName) {
        response.setHeader(HttpHeaders.Names.CONTENT_TYPE, mimeTypesMap.getContentType(resourceName));
    }

}