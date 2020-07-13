package org.bndly.common.data.impl;

/*-
 * #%L
 * File System Data Impl
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

import org.bndly.common.data.api.Data;
import org.bndly.common.data.api.DataStore;
import org.bndly.common.data.api.DataStoreListener;
import org.bndly.common.data.api.FileExtensionContentTypeMapper;
import org.bndly.common.data.api.FolderWatcher;
import org.bndly.common.data.api.FolderWatcherFactory;
import org.bndly.common.data.api.FolderWatcherListener;
import org.bndly.common.data.io.ReplayableInputStream;
import org.bndly.common.osgi.util.DictionaryAdapter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.apache.commons.io.IOUtils;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
		service = DataStore.class,
		configurationPolicy = ConfigurationPolicy.REQUIRE,
		configurationPid = "org.bndly.common.data.api.DataStore.fs"
)
@Designate(ocd = FileSystemDataStoreImpl.Configuration.class)
public class FileSystemDataStoreImpl implements DataStore, FolderWatcherListener {

	private static final Logger LOG = LoggerFactory.getLogger(FileSystemDataStoreImpl.class);

	@ObjectClassDefinition(
			name = "Filesystem Data Store",
			description = "A data store implementation, that is based on a filesystem folder"
	)
	public @interface Configuration {

		@AttributeDefinition(
				name = "Root folder",
				description = "root path for data store content"
		)
		String root() default "datastore";

		@AttributeDefinition(
				name = "Name",
				description = "name of the data store"
		)
		String name() default "fs";
	}
	
	private String root;
	@Reference
	private FolderWatcherFactory folderWatcherFactory;
	private final List<DataStoreListener> listeners = new ArrayList<>();
	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	private final String pathSeparator = "/";
	private String name;
	private Properties contentTypeMappings;
	private Properties contentTypeMappingsInverse;
	private boolean ready;
	private FolderWatcher watcher;
	private ServiceRegistration<FileExtensionContentTypeMapper> reg;

	@Activate
	public void init(ComponentContext componentContext) {
		DictionaryAdapter dictionaryAdapter = new DictionaryAdapter(componentContext.getProperties());
		root = dictionaryAdapter.getString("root", "datastore");
		name = dictionaryAdapter.getString("name", "fs");
		prepareContentTypeMappings();
		if (root != null && name != null) {
			this.watcher = folderWatcherFactory.createFolderWatcher(root, name);
			watcher.addListener(this);
		}
		this.ready = true;
		loadContentTypeMappings(componentContext);
		lock.readLock().lock();
		try {
			for (DataStoreListener dataStoreListener : listeners) {
				dataStoreListener.dataStoreIsReady(this);
			}
		} finally {
			lock.readLock().unlock();
		}
	}

	private void loadContentTypeMappings(ComponentContext componentContext) {
		Data d = findByName("contenttypemappings.properties");
		if (d != null) {
			ReplayableInputStream is = d.getInputStream();
			if (is != null) {
				try (ReplayableInputStream contentTypeMappingsIs = is) {
					ReplayableInputStream.replayIfPossible(is);
					contentTypeMappings.load(contentTypeMappingsIs);
					Enumeration<Object> keys = contentTypeMappings.keys();
					while (keys.hasMoreElements()) {
						String key = (String) keys.nextElement();
						String value = (String) contentTypeMappings.get(key);
						// the first extension defined for a content type will be the default extension for that content type
						if (!contentTypeMappingsInverse.containsKey(value)) {
							contentTypeMappingsInverse.put(value, key);
						}

					}
					BundleContext bc = componentContext.getBundleContext();
					FileExtensionContentTypeMapper service = new FileExtensionContentTypeMapper() {

						@Override
						public String mapExtensionToContentType(String extension) {
							return contentTypeMappings.getProperty(extension);
						}

						@Override
						public String mapContentTypeToExtension(String contentType) {
							return contentTypeMappingsInverse.getProperty(contentType);
						}
					};
					Dictionary<String, Object> fileExtensionContentTypeMapperProps = new Hashtable<>();
					String dspid = FileExtensionContentTypeMapper.class.getName();
					fileExtensionContentTypeMapperProps.put("service.pid", dspid);
					reg = bc.registerService(FileExtensionContentTypeMapper.class, service, fileExtensionContentTypeMapperProps);
				} catch (IOException ex) {
					LOG.error("could not read content type mappings", ex);
				}
			}
		}
	}

	private void prepareContentTypeMappings() {
		// since properties are using a hashtable, they are never ordered.
		// we want to keep the order to control defaults when mapping from
		// content type to extension.
		contentTypeMappingsInverse = new Properties();
		contentTypeMappings = new Properties() {

			private final HashSet keys = new LinkedHashSet();
			
			@Override
			public synchronized Object put(Object key, Object value) {
				keys.add(key);
				return super.put(key, value);
			}

			@Override
			public synchronized Object remove(Object key) {
				keys.remove(key);
				return super.remove(key);
			}

			@Override
			public Set<Object> keySet() {
				return keys;
			}

			@Override
			public synchronized Enumeration<Object> keys() {
				return new Vector(keySet()).elements();
			}

			@Override
			public synchronized void clear() {
				keys.clear();
				super.clear();
			}

			@Override
			public Set<Map.Entry<Object, Object>> entrySet() {
				Set<Map.Entry<Object, Object>> set = new LinkedHashSet<>();
				for (final Object key : keys) {
					set.add(new Map.Entry<Object, Object>() {

						@Override
						public Object getKey() {
							return key;
						}

						@Override
						public Object getValue() {
							return get(key);
						}

						@Override
						public Object setValue(Object value) {
							return put(key, value);
						}
						
					});
				}
				return set;
			}
			
			
		};
	}
	
	@Deactivate
	public void destroy() {
		if (reg != null) {
			reg.unregister();
			reg = null;
		}
		lock.readLock().lock();
		try {
			for (DataStoreListener dataStoreListener : listeners) {
				dataStoreListener.dataStoreClosed(this);
			}
		} finally {
			lock.readLock().unlock();
		}
		lock.writeLock().lock();
		try {
			listeners.clear();
		} finally {
			lock.writeLock().unlock();
		}
		if (this.watcher != null) {
			this.watcher.stop();
		}
		// we need the if block twice, because the watcher stop might already remove the watcher from this datastore
		if (this.watcher != null) {
			this.watcher.removeListener(this);
		}
		this.watcher = null;
	}

	@Override
	public List<Data> list() {
		List<Data> result = new ArrayList<>();
		File rootFolder = new File(root);
		appendFolderContentToResult(null, rootFolder, result);
		return result;
	}

	private void appendFolderContentToResult(String dataNamePrefix, File currentFolder, List<Data> result) {
		if (!isReady()) {
			return;
		}
		File[] files = currentFolder.listFiles();
		for (File file : files) {
			String newPrefix;
			if (dataNamePrefix == null) {
				newPrefix = file.getName();
			} else {
				newPrefix = dataNamePrefix + pathSeparator + file.getName();
			}
			if (file.isDirectory()) {
				appendFolderContentToResult(newPrefix, file, result);
			} else if (file.isFile()) {
				file.getName();
				Data fileData = mapFileToData(file, newPrefix, false);
				result.add(fileData);
			}
		}
	}

	@Override
	public Data findByName(String name) {
		if (!isReady()) {
			return null;
		}
		File file = new File(buildPath(name));
		if (!file.exists() || file.isDirectory()) {
			return null;
		}
		Data d = mapFileToData(file, name, false);
		return d;
	}

	@Override
	public Data findByNameAndContentType(String name, String contentType) {
		return findByName(name);
	}

	private File prepareJavaFileForData(Data data) {
		String targetLocation = buildTargetLocationForData(data);
		String targetPath = targetLocation;
		String targetFile = targetLocation;
		int i = targetLocation.lastIndexOf(pathSeparator);
		if (i > -1) {
			targetPath = targetLocation.substring(0, i);
			targetFile = targetLocation.substring(i + 1);
		}
		File targetDirectory = assertTargetDirectoryExists(targetPath);
		File file = new File(targetDirectory, targetFile);
		return file;
	}

	private String buildTargetLocationForData(Data data) {
		String targetLocation = data.getName();
		if (root != null) {
			if (root.endsWith(pathSeparator)) {
				targetLocation = root + data.getName();
			} else {
				targetLocation = root + pathSeparator + data.getName();
			}
		}
		return targetLocation;
	}

	@Override
	public Data create(Data data) {
		String targetLocation = buildTargetLocationForData(data);
		File file = prepareJavaFileForData(data);
		if (file.exists()) {
			throw new IllegalStateException("can not create '" + targetLocation + "' because the file already exists.");
		}
		try {
			file.createNewFile();
		} catch (IOException ex) {
			throw new IllegalStateException("can not create '" + targetLocation + "': " + ex.getMessage(), ex);
		}
		FileData fileData = copyDataToFileData(data);
		writeDataToFile(file, fileData);
		return data;
	}

	@Override
	public Data update(Data data) {
		String targetLocation = buildTargetLocationForData(data);
		File file = prepareJavaFileForData(data);
		if (!file.exists()) {
			throw new IllegalStateException("can not update '" + targetLocation + "' because the file does not exist.");
		}
		FileData fileData = copyDataToFileData(data);
		writeDataToFile(file, fileData);
		return data;
	}

	@Override
	public void delete(Data data) {
		File file = prepareJavaFileForData(data);
		if (file.exists()) {
			file.delete();
		}
	}

	private String buildPath(String name) {
		if (root == null) {
			return name;
		} else {
			if (!root.endsWith(pathSeparator)) {
				return root + pathSeparator + name;
			} else {
				return root + name;
			}
		}
	}

	private File assertTargetDirectoryExists(String targetPath) {
		File f = new File(targetPath);
		if (f.exists()) {
			return f;
		} else {
			f.mkdirs();
		}
		return f;
	}

	private void writeDataToFile(File file, FileData data) throws IllegalStateException {
		String targetLocation = buildTargetLocationForData(data);
		FileOutputStream fos;
		try {
			fos = new FileOutputStream(file);
		} catch (FileNotFoundException ex) {
			throw new IllegalStateException("can not create file output stream for '" + targetLocation + "': " + ex.getMessage(), ex);
		}
		if (data.getInputStream() != null) {
			try {
				IOUtils.copy(data.getInputStream(), fos);
			} catch (IOException ex) {
				throw new IllegalStateException("could not write bytes to file '" + targetLocation + "':" + ex.getMessage(), ex);
			}
			try {
				fos.flush();
			} catch (IOException ex) {
				throw new IllegalStateException("could not flush bytes to file '" + targetLocation + "':" + ex.getMessage(), ex);
			}
			try {
				fos.close();
			} catch (IOException e) {
				// silently close
			}
		}
		data.setUpdatedOn(new Date(file.lastModified()));
	}

	public void setRoot(String rootFolder) {
		this.root = rootFolder;
	}

	private Data mapFileToData(final File r, String name, boolean loadBytes) {
		FileData d = new FileData(r, getRootPath().getAbsolutePath());
		d.setUpdatedOn(new Date(r.lastModified()));
		if (loadBytes) {
			d.setInputStream(d.getLazyLoader().getBytes());
		}
		d.setName(name);
		String extension = null;
		int i = name.lastIndexOf(".");
		if (i > -1) {
			extension = name.substring(i + 1);
		}
		if (extension != null) {
			d.setContentType(mapExtensionToContentType(extension));
		}
		return d;
	}

	@Override
	public String getName() {
		return name;
	}

	private File getRootPath() {
		return new File(root);
	}

	@Reference(
			policy = ReferencePolicy.DYNAMIC, 
			cardinality = ReferenceCardinality.MULTIPLE, 
			bind = "addListener", 
			unbind = "removeListener", 
			service = DataStoreListener.class
	)
	@Override
	public void addListener(DataStoreListener listener) {
		if (listener != null) {
			lock.writeLock().lock();
			try {
				this.listeners.add(listener);
				if (isReady()) {
					listener.dataStoreIsReady(this);
				}
			} finally {
				lock.writeLock().unlock();
			}
		}
	}

	@Override
	public void removeListener(DataStoreListener listener) {
		if (listener != null) {
			lock.writeLock().lock();
			try {
				this.listeners.remove(listener);
			} finally {
				lock.writeLock().unlock();
			}
		}
	}

	public void setName(String name) {
		this.name = name;
	}

	private String mapExtensionToContentType(String extension) {
		if (contentTypeMappings != null) {
			return contentTypeMappings.getProperty(extension);
		}
		return null;
	}

	@Override
	public boolean isReady() {
		return ready;
	}

	private FileData copyDataToFileData(Data data) {
		File file = prepareJavaFileForData(data);
		FileData fileData = new FileData(file, pathSeparator);
		fileData.setContentType(data.getContentType());
		fileData.setCreatedOn(data.getCreatedOn());
		fileData.setUpdatedOn(data.getUpdatedOn());
		fileData.setName(data.getName());
		fileData.setInputStream(data.getInputStream());
		return fileData;
	}

	@Override
	public void newData(FolderWatcher folderWatcher, Data data, File file) {
		LOG.debug("new data in file system data store {}", data.getName());
		lock.readLock().lock();
		try {
			for (DataStoreListener listener : listeners) {
				listener.dataCreated(this, data);
			}
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public void updatedData(FolderWatcher folderWatcher, Data data, File file) {
		LOG.debug("updated data in file system data store {}", data.getName());
		lock.readLock().lock();
		try {
			for (DataStoreListener listener : listeners) {
				listener.dataUpdated(this, data);
			}
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public void deletedData(FolderWatcher folderWatcher, Data data, File file) {
		LOG.debug("deleted data in file system data store {}", data.getName());
		lock.readLock().lock();
		try {
			for (DataStoreListener listener : listeners) {
				listener.dataDeleted(this, data);
			}
		} finally {
			lock.readLock().unlock();
		}
	}

	@Override
	public void deletedSubFolder(FolderWatcher folderWatcher, File subFolder) {
	}

	@Override
	public void newSubFolder(FolderWatcher folderWatcher, File subFolder) {
	}

	@Override
	public void shuttingDown(FolderWatcher folderWatcher) {
		folderWatcher.removeListener(this);
		this.watcher = null;
	}

}
