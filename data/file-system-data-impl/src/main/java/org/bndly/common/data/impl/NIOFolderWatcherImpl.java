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

import org.bndly.common.data.api.FolderWatcher;
import org.bndly.common.data.api.FolderWatcherListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class NIOFolderWatcherImpl implements FolderWatcher, Runnable {
	private String name;
	private String folderLocation;
	private WatchService watcher;
	private Path path;
	private final List<FolderWatcherListener> listeners = new ArrayList<>();
	private boolean active;
	private Thread watcherThread;
	private final Map<String, WatchKey> watchKeysByPath = new HashMap<>();
	private final List<WatchKey> watchKeys = new ArrayList<>();

	private static final Logger LOG = LoggerFactory.getLogger(NIOFolderWatcherImpl.class);

	public NIOFolderWatcherImpl(String name) {
		if (name == null) {
			throw new IllegalArgumentException("name is not allowed to be null");
		}
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isActive() {
		return active;
	}

	public final void start() {
		if (isActive()) {
			return;
		}
		path = Paths.get(folderLocation);
		if (Files.notExists(path)) {
			try {
				Files.createDirectories(path);
			} catch (IOException ex) {
				throw new IllegalStateException("could not create directory to watch it: " + ex.getMessage(), ex);
			}
		}
		try {
			watcher = FileSystems.getDefault().newWatchService();
			Files.walkFileTree(path, new FileVisitor<Path>() {

				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					addFolderToWatcher(dir);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
					LOG.error("failed to initially visit all folders of watched folder");
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					return FileVisitResult.CONTINUE;
				}
			});
			active = true;
			watcherThread = new Thread(this, getClass().getSimpleName() + "." + name);
			watcherThread.start();
		} catch (IOException ex) {
			throw new IllegalStateException("failed to start folder watcher: " + ex.getMessage(), ex);
		}
	}

	@Override
	public final void stop() {
		if (!isActive()) {
			return;
		}
		active = false;
		if (watcherThread != null) {
			watcherThread.interrupt();
			watcherThread = null;
		}
		Iterator<WatchKey> keyIter = watchKeysByPath.values().iterator();
		while (keyIter.hasNext()) {
			WatchKey next = keyIter.next();
			try {
				next.cancel();
			} finally {
				keyIter.remove();
			}
		}
		List<FolderWatcherListener> defCopy = new ArrayList<>(listeners);
		for (FolderWatcherListener listener : defCopy) {
			listener.shuttingDown(this);
		}
		if (path != null) {
			path = null;
		}
		if (watcher != null) {
			try {
				watcher.close();
			} catch (IOException ex) {
				throw new IllegalStateException("failed to stop folder watcher: " + ex.getMessage(), ex);
			}
		}
	}

	@Override
	public void addListener(FolderWatcherListener listener) {
		if (listener != null) {
			listeners.add(listener);
		}
	}

	@Override
	public void removeListener(FolderWatcherListener listener) {
		if (listener != null) {
			listeners.remove(listener);
		}
	}

	@Override
	public File getWatchedFolder() {
		if (path == null) {
			return null;
		}
		return path.toFile();
	}

	public void setFolderLocation(String folderLocation) {
		this.folderLocation = folderLocation;
	}

	@Override
	public void run() {
		for (;;) {
			WatchKey key = null;
			try {
				if (!isActive()) {
					break;
				}
				if (LOG.isDebugEnabled()) {
					LOG.debug("{} taking watch key. thread will block now", path.toString());
				}
				key = watcher.take();
				if (LOG.isDebugEnabled()) {
					LOG.debug("{} got a watch key", path.toString());
				}
				List<WatchEvent<?>> events = key.pollEvents();
				if (LOG.isDebugEnabled()) {
					LOG.debug("{} watch key events: {}", path.toString(), events);
				}
				for (WatchEvent<?> event : events) {
					WatchEvent.Kind<?> kind = event.kind();
					if (kind == StandardWatchEventKinds.OVERFLOW) {
						if (LOG.isDebugEnabled()) {
							LOG.debug("{} OVERFLOW", path.toString());
						}
						continue;
					}
					Object context = event.context();
					if (Path.class.isInstance(context)) {
						Path foundFilePath = (Path) context;
						boolean isUpdate = kind == StandardWatchEventKinds.ENTRY_MODIFY;
						boolean isNew = kind == StandardWatchEventKinds.ENTRY_CREATE;
						boolean isDelete = kind == StandardWatchEventKinds.ENTRY_DELETE;
						if (!isUpdate && !isNew && !isDelete) {
							LOG.info("{} UNKNOWN EVENT: {}", path.toString(), foundFilePath.toString());
							continue;
						}
						if (LOG.isInfoEnabled()) {
							String eventName;
							if (isUpdate) {
								eventName = "UPDATE";
							} else if (isNew) {
								eventName = "NEW";
							} else if (isDelete) {
								eventName = "DELETE";
							} else {
								eventName = "OOPS....";
							}
							LOG.info("{} {}: {}", path.toString(), eventName, foundFilePath.toString());
						}
						Path watchedPath = (Path) key.watchable();
						foundFilePath = watchedPath.resolve(foundFilePath);
						Path absPath = foundFilePath.toAbsolutePath();
						boolean isFile = Files.isRegularFile(foundFilePath);
						boolean isFolder = Files.isDirectory(foundFilePath);
						if (isFile || isFolder) {
							File file = absPath.toFile();
							FileData data = null;
							if (isFile) {
								data = new FileData(file, path.toAbsolutePath().toString() + File.separator);
							} else if (isFolder) {
								if (isNew) {
									try {
										addFolderToWatcher(foundFilePath);
									} catch (IOException ex) {
										LOG.error("could not add folder to watcher: " + ex.getMessage(), ex);
										continue;
									}
								} else if (isDelete) {
									removeFolderFromWatcher(foundFilePath);
								}
							}
							for (FolderWatcherListener listener : listeners) {
								try {
									if (isNew) {
										if (isFile) {
											listener.newData(this, data, file);
										} else if (isFolder) {
											listener.newSubFolder(this, file);
										}
									} else if (isUpdate) {
										if (isFile) {
											listener.updatedData(this, data, file);
										} else if (isFolder) {
											// NO-OP
										}
									} else if (isDelete) {
										if (isFile) {
											listener.deletedData(this, data, file);
										} else if (isFolder) {
											listener.deletedSubFolder(this, file);
										}
									}
								} catch (Exception e) {
									LOG.error("listener of folder wather threw an exception", e);
								}
							}
						}
					}
				}
			} catch (ClosedWatchServiceException ex) {
				LOG.warn("watchservice is closed {}", name);
			} catch (InterruptedException ex) {
				LOG.warn("interrupted while waiting for key.");
				break;
			} finally {
				if (key != null) {
					key.reset();
				}
			}
		}
	}
	
	private WatchKey addFolderToWatcher(Path folder) throws IOException {
		String pathAsString = folder.toString();
		WatchKey oldKey = watchKeysByPath.get(pathAsString);
		if (oldKey != null) {
			LOG.warn("watching folder '{}' that was already watched. dropping old watch key now.", pathAsString);
			oldKey.cancel();
		}
		WatchKey localKey = folder.register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_DELETE);
		watchKeysByPath.put(pathAsString, localKey);
		return localKey;
	}
	
	private void removeFolderFromWatcher(Path folder) {
		String pathAsString = folder.toString();
		removeFolderFromWatcher(pathAsString);
	}
	
	private void removeFolderFromWatcher(String pathAsString) {
		WatchKey oldKey = watchKeysByPath.get(pathAsString);
		if (oldKey != null) {
			LOG.info("stopping folder watcher for '{}'.", pathAsString);
			oldKey.cancel();
		}
		watchKeysByPath.remove(pathAsString);
	}
	
	public void join() throws InterruptedException {
		if (watcherThread == null) {
			return;
		}
		watcherThread.join();
	}
	
}
