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

import java.lang.reflect.Constructor;

import org.helios.netty.examples.util.Utils;
import org.jboss.netty.channel.ChannelHandler;

/**
 * <p>Title: ChannelHandlerProviderFactory</p>
 * <p>Description: A factory for creating {@link ChannelHandlerProvider}s.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.examples.state.ChannelHandlerProviderFactory</code></p>
 */

public class ChannelHandlerProviderFactory  {
	
	/**
	 * Creates a new ChannelHandlerProvider for a shared {@link ChannelHandler}.
	 * @param handlerName The name of this handler. Must be unique within one pipeline
	 * @param sharedHandler The shared channel handler instance
	 * @return a new ChannelHandlerProvider 
	 */
	public static ChannelHandlerProvider getInstance(String handlerName, ChannelHandler sharedHandler) {
		if(sharedHandler.getClass().getAnnotation(ChannelHandler.Sharable.class)==null) {
			System.err.println("Warning: The channel handler [" + sharedHandler.getClass().getName() + "] is not annotated as @Sharable but has been configured in a shared ChannelHandlerProvider");
		}
		return new SharedChannelHandlerProvider(sharedHandler, handlerName);
	}
	
	/**
	 * Creates a new ChannelHandlerProvider for an unsharable {@link ChannelHandler}.
	 * @param name The name of the channel handler instance when it is placed in the pipeline
	 * @param handlerClass The channel handler class
	 * @param args The arguments to pass to the channel handler's constructor when creating a new instance
	 * @return a new ChannelHandlerProvider 
	 */
	public static ChannelHandlerProvider getInstance(String name, Class<? extends ChannelHandler> handlerClass, Object...args) {
		return new ExclusiveChannelHandlerProvider(name, handlerClass, args);
	}

	
	/**
	 * <p>Title: ExclusiveChannelHandlerProvider</p>
	 * <p>Description: A {@link ChannelHandlerProvider} implementation for non-sharable handlers.</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.netty.examples.state.ChannelHandlerProviderFactory.ExclusiveChannelHandlerProvider</code></p>
	 */
	public static class ExclusiveChannelHandlerProvider implements ChannelHandlerProvider {
		/** The name of the channel handler instance when it is placed in the pipeline  */
		protected final String name;
		/** The channel handler class */
		protected final Class<? extends ChannelHandler> handlerClass;
		/** The reflected channel handler class constructor */
		protected final Constructor<? extends ChannelHandler> handlerCtor;
		/** The arguments to pass to the channel handler's constructor when creating a new instance */
		protected final Object[] handlerArgs;
		
		/**
		 * Creates a new ChannelHandlerProviderFactory
		 * @param name The name of the channel handler instance when it is placed in the pipeline
		 * @param handlerClass The channel handler class
		 * @param args The arguments to pass to the channel handler's constructor when creating a new instance
		 */
		public ExclusiveChannelHandlerProvider(String name, Class<? extends ChannelHandler> handlerClass, Object...args) {
			this.name = name;
			this.handlerClass = handlerClass;
			this.handlerArgs = args==null ? new Object[0] : args;
			try {
				Class<?>[] argTypes = new Class<?>[handlerArgs.length];
				for(int i = 0; i < handlerArgs.length; i++) {
					argTypes[i] = Utils.primitive(handlerArgs[i].getClass());
				}
				Constructor<? extends ChannelHandler> tmpCtor = null; 
				try {
					tmpCtor = handlerClass.getDeclaredConstructor(argTypes);
				} catch (Exception ex) {
					tmpCtor = handlerClass.getConstructor(argTypes);
				}
				handlerCtor = tmpCtor;
			} catch (Exception e) {
				throw new RuntimeException("Failed to reflect constructor for ChannelHandler class [" + handlerClass.getName() + "]", e);
			}
		}
		
		/**
		 * Returns a new instance of the ChannelHandler
		 * @return a new instance of the ChannelHandler
		 */
		@Override
		public ChannelHandler getHandler() {
			try {
				return handlerCtor.newInstance(handlerArgs);
			} catch (Exception e) {
				throw new RuntimeException("Failed to create instance of [" + handlerClass.getName() + "]", e);
			}
		}
		
		/**
		 * Returns the channel handler name
		 * @return the channel handler name
		 */
		@Override
		public String getHandlerName() {
			return name;
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			return result;
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			ExclusiveChannelHandlerProvider other = (ExclusiveChannelHandlerProvider) obj;
			if (name == null) {
				if (other.name != null) {
					return false;
				}
			} else if (!name.equals(other.name)) {
				return false;
			}
			return true;
		}
		
	}
	
	/**
	 * <p>Title: SharedChannelHandlerProvider</p>
	 * <p>Description: A simple channel handler provider that returns the same shared handler instance</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.netty.examples.state.ChannelHandlerProviderFactory.SharedChannelHandlerProvider</code></p>
	 */

	public static class SharedChannelHandlerProvider implements ChannelHandlerProvider {
		/** The shared channel handler  */
		protected final ChannelHandler handler;
		/** The shared channel handler's name  */
		protected final String handlerName;
		
		
		/**
		 * Creates a new SharedChannelHandlerProvider
		 * @param handler The shared channel handler
		 * @param handlerName The shared channel handler's name
		 */
		public SharedChannelHandlerProvider(ChannelHandler handler, String handlerName) {
			this.handler = handler;
			this.handlerName = handlerName;
		}


		/**
		 * {@inheritDoc}
		 * @see org.helios.netty.examples.state.ChannelHandlerProvider#getHandler()
		 */
		@Override
		public ChannelHandler getHandler() {
			return handler;
		}


		/**
		 * Returns the channel handler name
		 * @return the channel handler name
		 */
		@Override
		public String getHandlerName() {
			return handlerName;
		}


		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((handlerName == null) ? 0 : handlerName.hashCode());
			return result;
		}


		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			SharedChannelHandlerProvider other = (SharedChannelHandlerProvider) obj;
			if (handlerName == null) {
				if (other.handlerName != null) {
					return false;
				}
			} else if (!handlerName.equals(other.handlerName)) {
				return false;
			}
			return true;
		}

	}
	
	
	
	
}
