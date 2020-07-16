package org.bndly.common.osgi.config.impl;

/*-
 * #%L
 * OSGI Config
 * %%
 * Copyright (C) 2013 - 2020 Cybercon GmbH
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.bndly.common.osgi.config.ConfigPropertyAdapter;
import org.bndly.common.osgi.config.ConfigReader;
import org.bndly.common.osgi.config.ConfigReaderProvider;
import org.bndly.common.osgi.config.MultiValuedStringGrammar;
import org.bndly.common.osgi.config.SecuredPersistenceManagerImpl;
import org.bndly.common.osgi.config.TypeConverter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.service.metatype.AttributeDefinition;
import org.osgi.service.metatype.MetaTypeInformation;
import org.osgi.service.metatype.MetaTypeService;
import org.osgi.service.metatype.ObjectClassDefinition;
import org.osgi.util.tracker.BundleTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class MetaTypeInformationTracker implements ConfigReaderProvider {
	private static final Logger LOG = LoggerFactory.getLogger(MetaTypeInformationTracker.class);
	
	private MetaTypeService metaTypeService;
	
	private final Map<String, TrackedBundle> trackedBundlesByPID = new HashMap<>();
	private final ReadWriteLock trackedBundlesLock = new ReentrantReadWriteLock();
	private BundleTracker<TrackedBundle> bundleTracker;
	private ConfigReader factoryComponentReader;
	
	private final class TrackedBundle {

		private final MetaTypeInformation metaTypeInfo;
		private final Map<String, ConfigReader> configReadersByPID = new HashMap<>();

		public TrackedBundle(Bundle bundle, MetaTypeService metaTypeService) {
			metaTypeInfo = metaTypeService.getMetaTypeInformation(bundle);
			// can be null for fragment bundles
			if (metaTypeInfo != null) {
				String[] pids = metaTypeInfo.getPids();
				String[] factoryPids = metaTypeInfo.getFactoryPids();
				
				for (String pid : factoryPids) {
					compilePID(pid);
				}
				for (String pid : pids) {
					compilePID(pid);
				}
			}
		}

		private void compilePID(String pid) {
			LOG.debug("compiling config property adapter for pid: {}", pid);
			Map<String, ConfigPropertyAdapter> readers = new HashMap<>();
			ObjectClassDefinition objectClassDefinition = metaTypeInfo.getObjectClassDefinition(pid, null);
			AttributeDefinition[] attributeDefs = objectClassDefinition.getAttributeDefinitions(ObjectClassDefinition.ALL);
			if (attributeDefs != null) {
				for (AttributeDefinition attributeDef : attributeDefs) {
					ConfigPropertyAdapter configPropertyReader = compilePropertyReader(attributeDef);
					readers.put(attributeDef.getID(), configPropertyReader);
				}
			}
			final Map<String, ConfigPropertyAdapter> map = Collections.unmodifiableMap(readers);
			configReadersByPID.put(pid, new ConfigReader() {
				@Override
				public Map<String, ConfigPropertyAdapter> getPropertyAdaptersByPropertyName() {
					return map;
				}
			});
		}

		private ConfigPropertyAdapter compilePropertyReader(final AttributeDefinition attributeDef) {
			final TypeConverter typeConverter = MetaTypeAttributeConverters.get(attributeDef);
			int card = attributeDef.getCardinality();
			if (card < 0) {
				// use the vector class
				return new ConfigPropertyAdapter() {
					@Override
					public ConfigPropertyAdapter.CardinalityType getCardinalityType() {
						return CardinalityType.VECTOR;
					}

					@Override
					public TypeConverter getTypeConverter() {
						return typeConverter;
					}
					
					@Override
					public Object deserialize(String rawValue) {
						Iterable<String> split = MultiValuedStringGrammar.split(rawValue);
						Vector<Object> vector = new Vector<>();
						for (String string : split) {
							vector.add(typeConverter.convertFromString(string));
						}
						return vector;
					}

					@Override
					public String serialize(Object value) {
						Iterable iterable = (Iterable) value;
						List<String> stringList = new ArrayList<>();
						for (Object object : iterable) {
							stringList.add(typeConverter.convertToString(object));
						}
						return MultiValuedStringGrammar.concat(stringList);
					}
				};
			} else if (card > 0) {
				// use an array
				return new ConfigPropertyAdapter() {
					@Override
					public ConfigPropertyAdapter.CardinalityType getCardinalityType() {
						return CardinalityType.ARRAY;
					}

					@Override
					public TypeConverter getTypeConverter() {
						return typeConverter;
					}

					@Override
					public Object deserialize(String rawValue) {
						Iterable<String> split = MultiValuedStringGrammar.split(rawValue);
						ArrayList<Object> items = new ArrayList<>();
						for (String string : split) {
							items.add(typeConverter.convertFromString(string));
						}
						return items.toArray(typeConverter.createArray(items.size()));
					}

					@Override
					public String serialize(Object value) {
						Object[] v = (Object[]) value;
						List<String> stringList = new ArrayList<>();
						for (Object object : v) {
							stringList.add(typeConverter.convertToString(object));
						}
						return MultiValuedStringGrammar.concat(stringList);
					}
					
				};
			} else {
				// scalar value
				return new ConfigPropertyAdapter() {
					@Override
					public ConfigPropertyAdapter.CardinalityType getCardinalityType() {
						return CardinalityType.SCALAR;
					}

					@Override
					public TypeConverter getTypeConverter() {
						return typeConverter;
					}

					@Override
					public Object deserialize(String rawValue) {
						return typeConverter.convertFromString(rawValue);
					}

					@Override
					public String serialize(Object value) {
						return typeConverter.convertToString(value);
					}
					
				};
			}
		}
		
	}
	
	@Override
	public ConfigReader getConfigReaderForPID(String pid) {
		trackedBundlesLock.readLock().lock();
		try {
			TrackedBundle tb = trackedBundlesByPID.get(pid);
			if (tb == null) {
				return null;
			}
			return tb.configReadersByPID.get(pid);
		} finally {
			trackedBundlesLock.readLock().unlock();
		}
	}

	public void activate(BundleContext bundleContext) {
		final Map<String, ConfigPropertyAdapter> propertyAdapters = new HashMap<>();
		final TypeConverter stringTypeConverter = MetaTypeAttributeConverters.get(AttributeDefinition.STRING);
		propertyAdapters.put(SecuredPersistenceManagerImpl.FACTORY_PID, new ConfigPropertyAdapter() {
			@Override
			public ConfigPropertyAdapter.CardinalityType getCardinalityType() {
				return CardinalityType.SCALAR;
			}

			@Override
			public TypeConverter getTypeConverter() {
				return stringTypeConverter;
			}

			@Override
			public Object deserialize(String rawValue) {
				return stringTypeConverter.convertFromString(rawValue);
			}

			@Override
			public String serialize(Object value) {
				return stringTypeConverter.convertToString(value);
			}
		});
		propertyAdapters.put(SecuredPersistenceManagerImpl.FACTORY_PID_LIST, new ConfigPropertyAdapter() {
			@Override
			public ConfigPropertyAdapter.CardinalityType getCardinalityType() {
				return CardinalityType.ARRAY;
			}

			@Override
			public TypeConverter getTypeConverter() {
				return stringTypeConverter;
			}

			@Override
			public Object deserialize(String rawValue) {
				Iterable<String> split = MultiValuedStringGrammar.split(rawValue);
				ArrayList<Object> items = new ArrayList<>();
				for (String string : split) {
					items.add(stringTypeConverter.convertFromString(string));
				}
				return items.toArray(stringTypeConverter.createArray(items.size()));
			}

			@Override
			public String serialize(Object value) {
				Object[] v = (Object[]) value;
				List<String> stringList = new ArrayList<>();
				for (Object object : v) {
					stringList.add(stringTypeConverter.convertToString(object));
				}
				return MultiValuedStringGrammar.concat(stringList);
			}
		});
		factoryComponentReader = new ConfigReader() {
			@Override
			public Map<String, ConfigPropertyAdapter> getPropertyAdaptersByPropertyName() {
				return propertyAdapters;
			}
		};
		bundleTracker = new BundleTracker<TrackedBundle>(bundleContext, Bundle.INSTALLED | Bundle.RESOLVED | Bundle.STARTING | Bundle.STOPPING | Bundle.ACTIVE, null) {
			@Override
			public TrackedBundle addingBundle(Bundle bundle, BundleEvent event) {
				trackedBundlesLock.writeLock().lock();
				try {
					LOG.debug("tracking bundle {} for meta type information", bundle.getSymbolicName());
					TrackedBundle tb = new TrackedBundle(bundle, metaTypeService);
					if (!tb.configReadersByPID.isEmpty()) {
						LOG.info("found meta type information in bundle {}", bundle.getSymbolicName());
					}
					for (Map.Entry<String, ConfigReader> entry : tb.configReadersByPID.entrySet()) {
						String pid = entry.getKey();
						ConfigReader value = entry.getValue();
						trackedBundlesByPID.put(pid, tb);
						String[] attributes = value.getPropertyAdaptersByPropertyName().keySet().toArray(new String[value.getPropertyAdaptersByPropertyName().keySet().size()]);;
						LOG.debug("found meta type information for PID {} for attributes: {}", pid, attributes);
					}
					return tb;
				} finally {
					trackedBundlesLock.writeLock().unlock();
				}
			}

			@Override
			public void removedBundle(Bundle bundle, BundleEvent event, TrackedBundle object) {
				trackedBundlesLock.writeLock().lock();
				try {
					Iterator<Map.Entry<String, TrackedBundle>> iterator = trackedBundlesByPID.entrySet().iterator();
					while (iterator.hasNext()) {
						Map.Entry<String, TrackedBundle> entry = iterator.next();
						if (entry.getValue() == object) {
							LOG.info("dropping meta type information for bundle {}", bundle.getSymbolicName());
							iterator.remove();
						}

					}
				} finally {
					trackedBundlesLock.writeLock().unlock();
				}
			}

		};
		bundleTracker.open();
	}

	@Override
	public ConfigReader getFactoryComponentReader() {
		return factoryComponentReader;
	}

	public void deactivate(BundleContext bundleContext) {
		bundleTracker.close();
		bundleTracker = null;
	}

	public void setMetaTypeService(MetaTypeService metaTypeService) {
		this.metaTypeService = metaTypeService;
	}
	
}
