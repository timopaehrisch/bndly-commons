package org.bndly.common.osgi.config;

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

import org.bndly.common.crypto.impl.shared.Base64Util;
import org.bndly.common.json.api.ConversionContext;
import org.bndly.common.json.api.ConversionContextBuilder;
import org.bndly.common.json.api.Deserializer;
import org.bndly.common.json.api.Serializer;
import org.bndly.common.json.model.JSMember;
import org.bndly.common.json.model.JSObject;
import org.bndly.common.json.model.JSValue;
import org.bndly.common.json.parsing.JSONParser;
import org.bndly.common.json.serializing.JSONSerializer;
import org.bndly.common.osgi.config.impl.Config;
import org.bndly.common.osgi.config.spi.CipherProvider;
import org.bndly.common.osgi.util.ServiceRegistrationBuilder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.AlgorithmParameters;
import java.security.spec.InvalidParameterSpecException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import org.apache.felix.cm.PersistenceManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogService;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class SecuredPersistenceManagerImpl implements PersistenceManager {

	private static final Logger LOG = LoggerFactory.getLogger(SecuredPersistenceManagerImpl.class);
	
	/**
	 * General persistent identifier of a factory component.
	 */
	public static final String FACTORY_PID = "factory.pid";
	/**
	 * List of known persist identifiers of existing factory component instances.
	 */
	public static final String FACTORY_PID_LIST = "factory.pidList";

	private static final String CONFIG_DIR = "config";
	private static final String CONFIG_SUFFIX = ".json";
	public static final String PROP_CIPHER_PROVIDER_ALIAS = "org.bndly.common.osgi.config.SecuredPersistenceManagerImpl.cipherprovider";
	
	private static final String JSON_PLAIN = "plain";
	private static final String JSON_IV = "iv";
	private static final String JSON_ENC = "enc";
	
	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
	private final Lock readLock = lock.readLock();
	private final Lock writeLock = lock.writeLock();
	private Path location;
	
	private ServiceRegistration<PersistenceManager> registration;
	private ServiceTracker<CipherProvider, CipherProvider> cipherProviderTracker;
	private final Lock cipherProviderLock = new ReentrantLock();
	private String cipherProviderAlias;
	
	private ConfigReaderProvider configReaderProvider;
	private ConversionContext jsonConversionContext;
	private static final Deserializer CONFIG_DESERIALIZER = new Deserializer() {
		@Override
		public boolean canDeserialize(Type targetType, ConversionContext conversionContext, JSValue value) {
			return Config.class.equals(targetType);
		}

		@Override
		public Object deserialize(Type targetType, ConversionContext conversionContext, JSValue value) {
			Config config = new Config();
			if (JSObject.class.isInstance(value)) {
				Set<JSMember> members = ((JSObject) value).getMembers();
				if (members != null) {
					for (JSMember member : members) {
						Config.ConfigEntry entry = (Config.ConfigEntry) conversionContext.deserialize(Config.ConfigEntry.class, member.getValue());
						if (entry != null) {
							config.put(member.getName().getValue(), entry);
						}
					}
				}
			}
			return config;
		}
	};
	private static final Serializer CONFIG_SERIALIZER = new Serializer() {
		@Override
		public boolean canSerialize(Type sourceType, ConversionContext conversionContext, Object javaValue) {
			return Config.class.equals(sourceType);
		}

		@Override
		public JSValue serialize(Type sourceType, ConversionContext conversionContext, Object javaValue) {
			JSObject jsObject = new JSObject();
			Set<Map.Entry<String, Config.ConfigEntry>> entrySet = ((Config) javaValue).entrySet();
			for (Map.Entry<String, Config.ConfigEntry> entry : entrySet) {
				JSValue serialized = conversionContext.serialize(Config.ConfigEntry.class, entry.getValue());
				if (serialized != null) {
					jsObject.createMemberValue(entry.getKey(), serialized);
				}
			}
			return jsObject;
		}
	};
	private BundleContext context;
	
	public void activate(BundleContext context) {
		this.context = context;
		location = context.getDataFile(CONFIG_DIR).toPath().toAbsolutePath();
		LOG.info("init of SecuredPersistenceManagerImpl in location {}", location);
		cipherProviderAlias = getCipherProviderAlias();
		final SecuredPersistenceManagerImpl that = this;
		if (cipherProviderAlias != null) {
			cipherProviderTracker = new ServiceTracker<CipherProvider, CipherProvider>(context, CipherProvider.class, null) {
				@Override
				public CipherProvider addingService(ServiceReference<CipherProvider> reference) {
					final CipherProvider instance = super.addingService(reference);
					cipherProviderLock.lock();
					try {
						if (cipherProviderAlias.equals(instance.getAlias())) {
							if (registration == null) {
								jsonConversionContext = new ConversionContextBuilder()
										.initDefaults()
										.deserializer(CONFIG_DESERIALIZER)
										.serializer(CONFIG_SERIALIZER)
										.deserializer(new Deserializer() {
											@Override
											public boolean canDeserialize(Type targetType, ConversionContext conversionContext, JSValue value) {
												return targetType.equals(Config.ConfigEntry.class) && JSObject.class.isInstance(value);
											}

											@Override
											public Object deserialize(Type targetType, ConversionContext conversionContext, JSValue value) {
												JSObject valueObject = ((JSObject) value);
												String plain = valueObject.getMemberStringValue(JSON_PLAIN);
												if (plain == null) {
													String enc = valueObject.getMemberStringValue(JSON_ENC);
													if (enc == null) {
														// data is corrupted
														return null;
													}
													byte[] buf = new byte[1024];
													String iv = valueObject.getMemberStringValue(JSON_IV);
													Cipher cipher = instance.restoreDecryptionCipher(cipherProviderAlias, iv);
													try (InputStream cis = new CipherInputStream(new ByteArrayInputStream(Base64Util.decode(enc)), cipher)) {
														ByteArrayOutputStream bos = new ByteArrayOutputStream();
														int i;
														while ((i = cis.read(buf)) > -1) {
															bos.write(buf, 0, i);
														}
														bos.flush();
														return new Config.ConfigEntry(false, bos.toString("UTF-8"));
													} catch (IOException e) {
														LOG.error("could not deserialize stored secured config value", e);
														return null;
													}
												} else {
													return new Config.ConfigEntry(true, plain);
												}
											}
										})
										.serializer(new Serializer() {
											@Override
											public boolean canSerialize(Type sourceType, ConversionContext conversionContext, Object javaValue) {
												return Config.ConfigEntry.class.isInstance(javaValue);
											}

											@Override
											public JSValue serialize(Type sourceType, ConversionContext conversionContext, Object javaValue) {
												Config.ConfigEntry e = (Config.ConfigEntry) javaValue;
												if (e.isPlain()) {
													return new JSObject().createMemberValue(JSON_PLAIN, e.getValue());
												} else {
													Cipher cipher = instance.restoreEncryptionCipher(cipherProviderAlias);
													if (cipher == null) {
														throw new IllegalStateException("could not serialize config because cipher " + cipherProviderAlias + " was missing");
													}
													ByteArrayOutputStream bos = new ByteArrayOutputStream();
													AlgorithmParameters params = cipher.getParameters();
													String initVectorEncoded;
													try {
														if (params != null) {
															byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();
															initVectorEncoded = Base64Util.encode(iv);
														} else {
															initVectorEncoded = null;
														}
													} catch (IOException ex) {
														throw new IllegalStateException("could not serialize config because the encryption failed");
													} catch (InvalidParameterSpecException ex) {
														initVectorEncoded = null;
													}
													try (CipherOutputStream cos = new CipherOutputStream(bos, cipher)) {
														cos.write(e.getValue().getBytes("UTF-8"));
														cos.flush();

														String enc = Base64Util.encode(bos.toByteArray());

														return new JSObject().createMemberValue(JSON_ENC, enc).createMemberValue(JSON_IV, initVectorEncoded);
													} catch (IOException ex) {
														throw new IllegalStateException("could not serialize config because the encryption failed");
													}
												}
											}
										})
										.build();
								registration = ServiceRegistrationBuilder
										.newInstance(PersistenceManager.class, that)
										.pid(that.getClass().getName())
										.property("secured", "true")
										.register(context);
							}
						}
					} finally {
						cipherProviderLock.unlock();
					}
					return instance;
				}

				@Override
				public void removedService(ServiceReference<CipherProvider> reference, CipherProvider instance) {
					cipherProviderLock.lock();
					try {
						if (cipherProviderAlias.equals(instance.getAlias())) {
							if (registration != null) {
								registration.unregister();
								registration = null;
								jsonConversionContext = null;
							}
						}
					} finally {
						cipherProviderLock.unlock();
					}
					super.removedService(reference, instance);
				}

			};
			cipherProviderTracker.open();

		} else {
			jsonConversionContext = new ConversionContextBuilder()
					.initDefaults()
					.deserializer(CONFIG_DESERIALIZER)
					.serializer(CONFIG_SERIALIZER)
					.deserializer(new Deserializer() {
						@Override
						public boolean canDeserialize(Type targetType, ConversionContext conversionContext, JSValue value) {
							return targetType.equals(Config.ConfigEntry.class) && JSObject.class.isInstance(value);
						}

						@Override
						public Object deserialize(Type targetType, ConversionContext conversionContext, JSValue value) {
							JSObject valueObject = ((JSObject) value);
							String plain = valueObject.getMemberStringValue(JSON_PLAIN);
							if (plain == null) {
								// data is corrupted
								return null;
							} else {
								return new Config.ConfigEntry(true, plain);
							}
						}
					})
					.serializer(new Serializer() {
						@Override
						public boolean canSerialize(Type sourceType, ConversionContext conversionContext, Object javaValue) {
							return Config.ConfigEntry.class.isInstance(javaValue);
						}

						@Override
						public JSValue serialize(Type sourceType, ConversionContext conversionContext, Object javaValue) {
							Config.ConfigEntry e = (Config.ConfigEntry) javaValue;
							return new JSObject().createMemberValue(JSON_PLAIN, e.getValue());
						}
					})
					.build();
			
			// we can register right away, because we are in no-op mode
			registration = ServiceRegistrationBuilder
					.newInstance(PersistenceManager.class, that)
					.pid(that.getClass().getName())
					.property("secured", "false")
					.property("service.ranking", 0)
					.register(context);
			
		}
	}

	public void deactivate() {
		if (cipherProviderTracker != null) {
			cipherProviderTracker.close();
		}
		if (registration != null) {
			registration.unregister();
			registration = null;
		}
	}
	
	@Override
	public Dictionary load(String pid) throws IOException {
		if (!isDecryptionAvailable()) {
			return null;
		}
		writeLock.lock();
		try (InputStream is = Files.newInputStream(getConfigLocation(pid), StandardOpenOption.READ)) {
			Hashtable ht = new Hashtable();
			JSValue parsed = new JSONParser().parse(is, "UTF-8");
			Config config = (Config) jsonConversionContext.deserialize(Config.class, parsed);
			ConfigReader reader = getConfigReaderForStoredConfig(config);
			for (Map.Entry<String, Config.ConfigEntry> entry : config.entrySet()) {
				String propertyName = entry.getKey();
				String rawStringValue = entry.getValue().getValue();
				Object converted;
				if (reader != null) {
					ConfigPropertyAdapter propertyReader = reader.getPropertyAdaptersByPropertyName().get(propertyName);
					if (propertyReader == null) {
						converted = rawStringValue;
					} else {
						converted = propertyReader.deserialize(rawStringValue);
					}
				} else {
					converted = rawStringValue;
				}
				if (converted != null) {
					ht.put(propertyName, converted);
				}
				
			}
			return ht;
		} finally {
			writeLock.unlock();
		}
	}

	private ConfigReader getConfigReaderForStoredConfig(Config config) {
		String pid = config.getString(ConfigurationAdmin.SERVICE_FACTORYPID);
		if (pid == null) {
			pid = config.getString(Constants.SERVICE_PID);
		}
		if (pid == null) {
			String factoryPid = config.getString(FACTORY_PID);
			if (factoryPid != null) {
				return configReaderProvider.getFactoryComponentReader();
			}
			return null;
		}
		return configReaderProvider.getConfigReaderForPID(pid);
	}
	
	private boolean isDecryptionAvailable() {
		if (cipherProviderAlias == null) {
			return true;
		}
		CipherProviderService cipherProviderService = CipherProviderService.getInstance();
		return cipherProviderService.getCipherProviderByAlias(cipherProviderAlias) != null;
	}

	@Override
	public Enumeration getDictionaries() throws IOException {
		if (!isDecryptionAvailable()) {
			LOG.info("no cipher provider alias available. returning empty dictionary enumeration. set the {} system property, if you want to look for secured config dictionaries.", PROP_CIPHER_PROVIDER_ALIAS);
			return Collections.emptyEnumeration();
		}
		writeLock.lock();
		try {
			final List<Dictionary> dictionaries = new ArrayList<>();
			Files.walkFileTree(location, new FileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					if (dir.equals(location)) {
						return FileVisitResult.CONTINUE;
					} else {
						return FileVisitResult.SKIP_SUBTREE;
					}
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (Files.isRegularFile(file)) {
						String name = file.getName(file.getNameCount() - 1).toString();
						if (name.endsWith(CONFIG_SUFFIX)) {
							String pid = name.substring(0, name.length() - CONFIG_SUFFIX.length());
							dictionaries.add(createLazyDictionary(pid));
						}
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
					return FileVisitResult.TERMINATE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					if (dir.equals(location)) {
						return FileVisitResult.CONTINUE;
					} else {
						return FileVisitResult.SKIP_SUBTREE;
					}
				}
			});
			return Collections.enumeration(dictionaries);
		} finally {
			writeLock.unlock();
		}
	}

	private Dictionary createLazyDictionary(final String pid) {
		return new Dictionary() {
			
			boolean didInit = false;
			private Dictionary wrapped;
			
			private Dictionary assertInit() {
				if (!didInit) {
					didInit = true;
					try {
						wrapped = load(pid);
					} catch (IOException ex) {
						throw new IllegalStateException("could not load dictionary: " + ex.getMessage(), ex);
					}
				}
				return wrapped;
			}
			
			@Override
			public int size() {
				return assertInit().size();
			}

			@Override
			public boolean isEmpty() {
				return assertInit().isEmpty();
			}

			@Override
			public Enumeration keys() {
				return assertInit().keys();
			}

			@Override
			public Enumeration elements() {
				return assertInit().elements();
			}

			@Override
			public Object get(Object key) {
				return assertInit().get(key);
			}

			@Override
			public Object put(Object key, Object value) {
				return assertInit().put(key, value);
			}

			@Override
			public Object remove(Object key) {
				return assertInit().remove(key);
			}
		};
	}
	
	@Override
	public boolean exists(String string) {
		writeLock.lock();
		try {
			return Files.isRegularFile(getConfigLocation(string));
		} finally {
			writeLock.unlock();
		}
	}

	@Override
	public void store(final String string, final Dictionary dctnr) throws IOException {
		storeInternal(string, dctnr);
	}
	
	private void storeInternal(final String string, final Dictionary dctnr) throws IOException {
		Object fileName = dctnr.get(ConfigurationContext.FILENAME);
		Object securedConfig = dctnr.get(SecuredConfigArtifactInstaller.SECURED_MARKER);
		if (securedConfig == null) {
			// skip this, because it is not a secured config.
			LOG.debug("storing non-secured config: {}", string);
		}
		
		cipherProviderLock.lock();
		writeLock.lock();
		try {
			ConfigReader configReaderForPID;
			String factoryPID = asString(dctnr.get(ConfigurationAdmin.SERVICE_FACTORYPID));
			String servicePID = asString(dctnr.get(Constants.SERVICE_PID));
			if (factoryPID == null && servicePID == null) {
				factoryPID = asString(dctnr.get(FACTORY_PID));
				if (factoryPID != null) {
					configReaderForPID = configReaderProvider.getFactoryComponentReader();
				} else {
					LOG.error("don't know what to persist: " + string);
					configReaderForPID = null;
				}
			} else {
				configReaderForPID = configReaderProvider.getConfigReaderForPID(factoryPID != null ? factoryPID : servicePID);
			}
			Map<String, ConfigPropertyAdapter> adapters = configReaderForPID == null ? Collections.EMPTY_MAP : configReaderForPID.getPropertyAdaptersByPropertyName();

			Config config = new Config();
			for (Map.Entry<String, ConfigPropertyAdapter> entry : adapters.entrySet()) {
				String property = entry.getKey();
				Object valueToStore = dctnr.get(property);
				if (valueToStore == null) {
					continue;
				}
				if (String.class.isInstance(valueToStore)) {
					config.put(property, new Config.ConfigEntry(fileName == null, (String) valueToStore));
				} else {
					ConfigPropertyAdapter adapter = entry.getValue();
					String serialized = adapter.serialize(valueToStore);
					if (serialized != null) {
						config.put(property, new Config.ConfigEntry(fileName == null, serialized));
					}
				}
			}

			Enumeration keysEnum = dctnr.keys();
			while (keysEnum.hasMoreElements()) {
				Object tmp = keysEnum.nextElement();
				if (!String.class.isInstance(tmp)) {
					continue;
				}
				String key = (String) tmp;
				if (config.containsKey(key)) {
					continue;
				}
				Object rawValue = dctnr.get(key);
				final String rawString;
				if (String.class.isInstance(rawValue)) {
					rawString = (String) rawValue;
				} else {
					Class<? extends Object> cls = rawValue.getClass();
					if (cls.isArray()) {
						LOG.debug("converting array to string", rawValue.getClass());
						Object[] arr = (Object[]) rawValue;
						List<String> stringList = new ArrayList<>();
						for (Object object : arr) {
							convertValueForStrage(object, stringList);
						}
						rawString = MultiValuedStringGrammar.concat(stringList);
					} else if (cls.isAssignableFrom(Vector.class)) {
						LOG.debug("converting array to string", rawValue.getClass());
						Vector vector = (Vector) rawValue;
						List<String> stringList = new ArrayList<>();
						for (Object object : vector) {
							convertValueForStrage(object, stringList);
						}
						rawString = MultiValuedStringGrammar.concat(stringList);
					} else {
						LOG.debug("converting {} to string", rawValue.getClass());
						rawString = rawValue.toString();
					}
				}
				config.put(key, new Config.ConfigEntry(fileName == null, rawString));
			}

			JSValue serialized = jsonConversionContext.serialize(Config.class, config);

			Path targetFile = getConfigLocation(string);
			Files.createDirectories(location);
			Files.deleteIfExists(targetFile);
			Files.createFile(targetFile);
			try (OutputStream os = Files.newOutputStream(targetFile, StandardOpenOption.WRITE)) {
				new JSONSerializer().serialize(serialized, os, "UTF-8");
				os.flush();
			}
		} finally {
			writeLock.unlock();
			cipherProviderLock.unlock();
		}
	}

	private void convertValueForStrage(Object object, List<String> stringList) {
		String val;
		if (String.class.isInstance(object)) {
			val = (String) object;
		} else {
			if (object != null) {
				LOG.debug("converting {} to string", object.getClass());
				val = object.toString();
			} else {
				val = null;
			}
		}
		if (val != null) {
			stringList.add(val);
		}
	}

	private Path getConfigLocation(String pid) {
		Path targetFile = location.resolve(pid + CONFIG_SUFFIX);
		return targetFile;
	}

	@Override
	public void delete(String pid) throws IOException {
		writeLock.lock();
		try {
			Files.deleteIfExists(getConfigLocation(pid));
		} finally {
			writeLock.unlock();
		}
	}

	public void initConfigurationAdmin(ConfigurationAdmin configurationAdmin, LogService logService) {
		Enumeration dicts = Collections.emptyEnumeration();
		List<String> pids = new ArrayList<>();
		while (dicts.hasMoreElements()) {
			Object nextElement = dicts.nextElement();
			if (Dictionary.class.isInstance(nextElement)) {
				Object servicePid = ((Dictionary) nextElement).get(Constants.SERVICE_PID);
				if (String.class.isInstance(servicePid)) {
					pids.add((String) servicePid);
				}
			}
		}
		for (String pid : pids) {
			try {
				Configuration config = configurationAdmin.getConfiguration(pid, null);
				try {
					config.update(load(pid));
				} catch (IOException ex) {
					if (logService != null) {
						logService.log(LogService.LOG_ERROR, "could not update configuration " + pid, ex);
					}
				}
			} catch (IOException ex) {
				if (logService != null) {
					logService.log(LogService.LOG_ERROR, "could not load configuration " + pid, ex);
				}
			}
		}
	}
	
	private String getCipherProviderAlias() {
		return System.getProperty(PROP_CIPHER_PROVIDER_ALIAS);
	}

	private String asString(Object value) {
		if (value instanceof String) {
			return (String) value;
		}
		return null;
	}

	public void setConfigReaderProvider(ConfigReaderProvider configReaderProvider) {
		this.configReaderProvider = configReaderProvider;
	}

}
