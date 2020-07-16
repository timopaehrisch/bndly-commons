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
public class FlushEverything extends AbstractFlushStrategy {
	private static final Logger LOG = LoggerFactory.getLogger(FlushEverything.class);

	public FlushEverything(String cacheRoot, List<CacheEventListener> cacheEventListeners) {
		super(cacheRoot, cacheEventListeners);
	}
	
	@Override
	public void doFlush() {
		Path cacheRootPath = Paths.get(cacheRoot);
		recursiveDeletePath(cacheRootPath, true);
		for (CacheEventListener cacheEventListener : cacheEventListeners) {
			cacheEventListener.onFlush();
		}
	}
	
	private void recursiveDeletePath(final Path path, final boolean keepRoot) {
		if (Files.exists(path) && Files.isDirectory(path)) {
			try {
				Files.walkFileTree(path, new FileVisitor<Path>() {
					
					@Override
					public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
						return FileVisitResult.CONTINUE;
					}
					
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						try {
							Files.deleteIfExists(file);
						} catch (IOException ex) {
							LOG.error("failed to delete cache path", ex);
						}
						return FileVisitResult.CONTINUE;
					}
					
					@Override
					public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
						return FileVisitResult.CONTINUE;
					}
					
					@Override
					public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
						try {
							if (keepRoot && dir.equals(path)) {
								return FileVisitResult.TERMINATE;
							}
							Files.deleteIfExists(dir);
						} catch (IOException ex) {
							LOG.error("failed to delete cache path", ex);
						}
						return FileVisitResult.CONTINUE;
					}
				});
				
			} catch (IOException ex) {
				LOG.error("exception while trying to flush a cache path recursively.", ex);
			}
		}
	}
	
}
