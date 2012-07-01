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
package org.helios.netty.examples.codec.collectd;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * <p>Title: TypeDB</p>
 * <p>Description: A datasource for managing collectd <a href="http://collectd.org/documentation/manpages/types.db.5.shtml">types.db</a> database entries.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>org.helios.netty.examples.codec.collectd.TypeDB</code></p>
 */

public class TypeDB {
	/** The built in types.db resource, loaded from the classpath */
	public static final String INTERNAL_RESOURCE = "collectd/types.db";
	/** White space parse regex */
	public static final Pattern LINE_SPLIT = Pattern.compile("\\s+");
	/** Comma or end of line parse regex */
	public static final Pattern COMMA_SEP_OR_EOL = Pattern.compile(",|\\$");
	/** Colons parse regex */
	public static final Pattern COLON = Pattern.compile(":");
	/** The singleton instance */
	private static volatile TypeDB instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** A map of TypeDBEntries keyed by the plugin name */
	protected final Map<String, Map<String, TypeDBEntry>> entries = new ConcurrentHashMap<String, Map<String, TypeDBEntry>>(1024);
	
	/**
	 * Acquires the TypeDB singleton instance
	 * @return the TypeDB singleton instance
	 */
	public static TypeDB getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new TypeDB();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Creates a new TypeDB
	 */
	private TypeDB() {
		TypeDBEntry.typeDB = this;
		log("Loading internal resource");
		URL url = getClass().getClassLoader().getResource(INTERNAL_RESOURCE);
		log("Internal Resource URL:" + url);
		loadFrom(url);
		int items = 0;
		for(Map<String, TypeDBEntry> map: entries.values()) {
			items += map.size();
		}
		log("TypeDB initialized with [" + entries.size() + "] plugins and [" + items + "] entries");
	}
	
	/**
	 * Returns the named entry
	 * @param pluginName The name of the owning plugin
	 * @param entryName The entry name
	 * @return The located TypeDBEntry or null if one was not found
	 */
	public TypeDBEntry getEntry(String pluginName, String entryName) {
		if(pluginName==null) throw new IllegalArgumentException("The passed plugin name was null", new Throwable());
		if(entryName==null) throw new IllegalArgumentException("The passed entry name was null", new Throwable());
		TypeDBEntry entry = null;
		Map<String, TypeDBEntry> map = entries.get(pluginName);
		if(map!=null) {
			entry = map.get(entryName);
		}
		return entry;
	}
	
	/**
	 * Adds a new entry to the repository if it does not exist already.
	 * @param pluginName The name of the owning plugin
	 * @param entry The entry to add
	 */
	public void addTypeDBEntry(String pluginName, TypeDBEntry entry) {
		if(pluginName==null) throw new IllegalArgumentException("The passed plugin name was null", new Throwable());
		if(entry==null) throw new IllegalArgumentException("The passed entry was null", new Throwable());
		Map<String, TypeDBEntry> map = entries.get(pluginName);		
		if(map==null) {
			synchronized(entries) {
				map = entries.get(pluginName);		
				if(map==null) {
					map = new LinkedHashMap<String, TypeDBEntry>(3, 0.75f, false);
					entries.put(pluginName, map);
				}
			}			
		}		
		map.put(entry.name, entry);		
	}
	
	/**
	 * Returns an iterator of the TypeDBEntries for the passed plugin name
	 * @param pluginName The name of the plugin
	 * @return an iterator of the TypeDBEntries for the passed plugin name
	 */
	public Iterator<TypeDBEntry> getPluginEntries(String pluginName) {
		if(pluginName==null) throw new IllegalArgumentException("The passed plugin name was null", new Throwable());
		Map<String, TypeDBEntry> map = entries.get(pluginName);
		Iterator<TypeDBEntry> entries = null;
		if(map!=null) {
			entries = Collections.unmodifiableCollection(map.values()).iterator();
		}  else {
			entries = Collections.singleton(TypeDBEntry.getInstance(pluginName)).iterator();
		}
		return entries;
	}
	
	/**
	 * Loads the types.db content from the passed URL into this instance.
	 * @param url The URL source of the types.db content
	 * @return the number of records loaded
	 */
	public int loadFrom(URL url) {
		int loaded = 0;
		InputStream is = null;		
		try {
			is = url.openStream();
			BufferedReader in = new BufferedReader(new InputStreamReader(is));
			String line = null;
			while((line = in.readLine())!=null) {
				String[] frags = LINE_SPLIT.split(line);
				String pluginName = frags[0];				
				String[] restCommaSep = COMMA_SEP_OR_EOL.split(line.replace(" ", "").replace("\t", "").replace(frags[0], ""));
				for(String ds: restCommaSep) {
					String[] parts = COLON.split(ds);
					try {
						String entryName = parts[0];
						CollectDType type = CollectDType.valueOf(parts[1].toUpperCase());
						double min, max;
						min = "U".equalsIgnoreCase(parts[2]) ?  Double.NaN : Double.parseDouble(parts[2]);
						max = "U".equalsIgnoreCase(parts[3]) ?  Double.NaN : Double.parseDouble(parts[3]);
						TypeDBEntry te= TypeDBEntry.getInstance(pluginName, entryName, type, min, max);
						log(te);
					} catch (Exception e) {
						System.err.println("Skipping record [" + line + "] on account of:" + e);
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to read types from [" + url + "]", e);
		} finally {
			try { is.close(); } catch (Exception e) {}
		}
		return loaded;
	}
	
	/**
	 * <p>Title: TypeDBEntry</p>
	 * <p>Description: Defines one entry for a plugin</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>org.helios.netty.examples.codec.collectd.TypeDB.TypeDBEntry</code></p>
	 */
	public static class TypeDBEntry {
		/** The owning plugin name */
		private final String pluginName;		
		/** The entry name */
		private final String name;
		/** Indicates if name is <code>value</code> */
		private final boolean defaultName;
		/** The entry type */
		private final CollectDType type;
		/** The entry minimum value */
		private final double min;
		/** The entry maximum value */
		private final double max;
		/** The typeDB reference */
		private static TypeDB typeDB = null;
		
		/**
		 * Returns the named TypeDBEntry, adding it to the repo if it does not already exist
		 * @param pluginName The owning plugin name
		 * @param name The entry name
		 * @param type The entry type
		 * @param min The entry minimum value
		 * @param max The entry maximum value
		 * @return the named TypeDBEntry
		 */
		private static TypeDBEntry getInstance(String pluginName, String name, CollectDType type, double min, double max) {
			if(pluginName==null) throw new IllegalArgumentException("The passed plugin name was null", new Throwable());
			if(name==null) throw new IllegalArgumentException("The passed entry name was null", new Throwable());
			if(type==null) throw new IllegalArgumentException("The passed collectd type was null", new Throwable());
			TypeDBEntry te = typeDB.getEntry(pluginName, name);
			if(te==null) {
				te = new TypeDBEntry(pluginName, name, type, min, max);
				typeDB.addTypeDBEntry(pluginName, te);
			}
			return te;			
		}
		
		/**
		 * Creates a flat plugin type db entry
		 * @param pluginName The plugin name
		 * @return a TypeDBEntry 
		 */
		private static TypeDBEntry getInstance(String pluginName) {
			if(pluginName==null) throw new IllegalArgumentException("The passed plugin name was null", new Throwable());
			return new TypeDBEntry(pluginName, pluginName, CollectDType.COUNTER, Double.NaN, Double.NaN);
		}
		
		/**
		 * Creates a new TypeDBEntry
		 * @param pluginName The owning plugin name
		 * @param name The entry name
		 * @param type The entry type
		 * @param min The entry minimum value
		 * @param max The entry maximum value
		 */
		private TypeDBEntry(String pluginName, String name, CollectDType type, double min, double max) {
			this.pluginName = pluginName;
			this.name = name;
			this.type = type;
			this.min = min;
			this.max = max;
			defaultName = "value".equalsIgnoreCase(pluginName);
		}
		
		/**
		 * Appends the entry name to the supplied prefix delimited by the passed delimiter
		 * If this entry is the default name, the prefix is returned as is.
		 * @param prefix An optional prefix
		 * @param delimiter An optional delimiter
		 * @return The appended string
		 */
		public String appendEntryName(CharSequence prefix, String delimiter) {
			if(defaultName) return prefix.toString();
			StringBuilder b = new StringBuilder();
			if(prefix!=null) {
				b.append(prefix);
				if(delimiter!=null) {
					b.append(delimiter);
				}				
			}
			b.append(name);
			return b.toString();
		}
		
		/**
		 * Indicates if the plugin name is the default name
		 * @return true if the plugin name is the default name
		 */
		public boolean isDefaultName() {
			return defaultName;
		}

		/**
		 * Returns the owning plugin name
		 * @return the pluginName
		 */
		public String getPluginName() {
			return pluginName;
		}

		/**
		 * Returns the entry name
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		/**
		 * Returns the entry type
		 * @return the type
		 */
		public CollectDType getType() {
			return type;
		}

		/**
		 * Returns the entry minimum value
		 * @return the min
		 */
		public double getMin() {
			return min;
		}

		/**
		 * Returns the entry maximum value
		 * @return the max
		 */
		public double getMax() {
			return max;
		}

		/**
		 * {@inheritDoc}
		 * @see java.lang.Object#toString()
		 */
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("TypeDBEntry [pluginName=").append(pluginName)
					.append(", name=").append(name).append(", type=")
					.append(type).append(", min=").append(min).append(", max=")
					.append(max).append("]");
			return builder.toString();
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
			result = prime * result
					+ ((pluginName == null) ? 0 : pluginName.hashCode());
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
			TypeDBEntry other = (TypeDBEntry) obj;
			if (name == null) {
				if (other.name != null) {
					return false;
				}
			} else if (!name.equals(other.name)) {
				return false;
			}
			if (pluginName == null) {
				if (other.pluginName != null) {
					return false;
				}
			} else if (!pluginName.equals(other.pluginName)) {
				return false;
			}
			return true;
		}
		
		
	}
	
	public static void main(String[] args) {
		log("TypesDB Load");
		new TypeDB();
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
}
