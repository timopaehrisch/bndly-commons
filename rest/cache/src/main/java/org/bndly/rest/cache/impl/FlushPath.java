package org.bndly.rest.cache.impl;

/*-
 * #%L
 * REST Cache
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

import org.bndly.rest.cache.api.CacheEventListener;
import static org.bndly.rest.cache.impl.CacheInterceptor.convertPathStringToCachePath;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class FlushPath extends AbstractFlushStrategy {
	private static final Logger LOG = LoggerFactory.getLogger(FlushPath.class);
	
	protected final String pathAsString;

	public FlushPath(String pathAsString, String cacheRoot, List<CacheEventListener> cacheEventListeners) {
		super(cacheRoot, cacheEventListeners);
		this.pathAsString = pathAsString;
	}
	
	@Override
	public void doFlush() {
		Path root = Paths.get(cacheRoot).normalize();
		final Path path = convertPathStringToCachePath(pathAsString, root);

		if (Files.exists(path)) {
			try {
				if (Files.isRegularFile(path)) {
					silentlyDelete(path);
					informListeners();

				} else if (Files.isDirectory(path)) {
					removeDirectoryEntry(path, root);
					informListeners();
				}
			} catch (IOException e) {
				LOG.error("failed to flush path '{}': {}", pathAsString, e.getMessage(), e);
			}
		}
	}

	protected boolean shouldStepIntoSubDirectory() {
		return false;
	}
	
	protected void removeDirectoryEntry(final Path path, final Path root) throws IOException {
		final Path rootAbs = root.toAbsolutePath();
		final Path expectedLinksFolder = path.resolve("links");

		// delete cache entries for this path.
		// there are sperate entries for the different languages and content types
		Files.walkFileTree(path, new FileVisitor<Path>() {
			private boolean isInCacheLinkFolder;
			private Path cacheLinkFolder;
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				if (dir.equals(path)) {
					isInCacheLinkFolder = false;
					return FileVisitResult.CONTINUE;
				} else {
					if (!isInCacheLinkFolder) {
						if (expectedLinksFolder.equals(dir)) {
							isInCacheLinkFolder = true;
							cacheLinkFolder = dir;
							return FileVisitResult.CONTINUE;
						} else {
							if (shouldStepIntoSubDirectory()) {
								return FileVisitResult.CONTINUE;
							} else {
								return FileVisitResult.SKIP_SUBTREE;
							}
						}
					} else {
						return FileVisitResult.CONTINUE;
					}
				}
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (attrs.isSymbolicLink()) {
					// could be a cache link
					LOG.debug("found a cache link. {}", file);
					if (isInCacheLinkFolder) {
						Path rp = file.toRealPath();
						Path linkedCacheEntryPath = rootAbs.relativize(rp);
						if (LOG.isDebugEnabled()) {
							LOG.debug("CACHE ENTRY OF LINK TO REMOVE: {}", linkedCacheEntryPath.toString());
						}
						try {
							silentlyDelete(file);
						} finally {
							flushLinkedItem(linkedCacheEntryPath.toString());
						}
					}
				} else if (attrs.isRegularFile()) {
					silentlyDelete(file);
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				if (isInCacheLinkFolder) {
					if (dir.equals(cacheLinkFolder)) {
						isInCacheLinkFolder = false;
						cacheLinkFolder = null;
					}
					return FileVisitResult.CONTINUE;
				} else {
					if (shouldStepIntoSubDirectory()) {
						if (dir.equals(path)) {
							return FileVisitResult.TERMINATE;
						} else {
							return FileVisitResult.CONTINUE;
						}
					} else {
						return FileVisitResult.TERMINATE;
					}
				}
			}

		});
	}
	
	private void silentlyDelete(Path file) throws IOException {
		try {
			Files.delete(file);
		} catch (java.nio.file.NoSuchFileException e) {
			// we don't care since we would have deleted it anayways
		}
	}
	
	protected void flushLinkedItem(String linkedItemToFlush) {
		new FlushPath(linkedItemToFlush, cacheRoot, cacheEventListeners).doFlush();
	}
	
	protected void informListeners() {
		for (CacheEventListener cacheEventListener : cacheEventListeners) {
			cacheEventListener.onFlush(pathAsString, false);
		}
	}
	
}
