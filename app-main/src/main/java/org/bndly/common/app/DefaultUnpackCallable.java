package org.bndly.common.app;

/*-
 * #%L
 * App Main
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.concurrent.Callable;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class DefaultUnpackCallable implements Callable<Void> {

	private final Environment environment;
	private final Logger log;
	protected final byte[] buffer = new byte[1024];

	public DefaultUnpackCallable(Environment environment, Logger log) {
		this.environment = environment;
		this.log = log;
	}

	public static interface Unpackable {
		boolean isDirectory();
		String getName();
		InputStream openInputStream() throws IOException;
		long getLastModifiedMillis() throws IOException;
	}

	protected AutoCloseableIterable<Unpackable> getUnpackables() {
		return new AutoCloseableIterable<Unpackable>() {
			@Override
			public AutoCloseableIterator<Unpackable> iterator() {
				final JarFile jarFile;
				try {
					jarFile = new JarFile(environment.getJarPath().toFile());
				} catch (IOException ex) {
					throw new IllegalStateException("could not create iterator", ex);
				}
				final Enumeration<JarEntry> entries = jarFile.entries();
				return new AutoCloseableIterator<Unpackable>() {
					int s = -1;
					JarEntry entry;
					
					@Override
					public boolean hasNext() {
						if (s == -1) {
							s = entries.hasMoreElements() ? 1 : 0;
							if (s == 1) {
								entry = entries.nextElement();
								if (!entry.getName().startsWith(SharedConstants.APP_JAR_RESOURCES)) {
									s = -1;
									entry = null;
									return hasNext();
								}
							} else {
								entry = null;
							}
						}
						return s == 1;
					}

					@Override
					public Unpackable next() {
						if (!hasNext()) {
							throw new NoSuchElementException();
						}
						s = -1;
						final JarEntry fEntry = entry;
						return new Unpackable() {
							@Override
							public boolean isDirectory() {
								return fEntry.isDirectory();
							}

							@Override
							public String getName() {
								String name = fEntry.getName();
								name = name.substring(SharedConstants.APP_JAR_RESOURCES.length());
								if (name.startsWith(File.separator)) {
									name = name.substring(File.separator.length());
								} else if (name.startsWith(SharedConstants.JAR_ENTRY_PATH_SEPARATOR)) {
									name = name.substring(SharedConstants.JAR_ENTRY_PATH_SEPARATOR.length());
								}
								return name;
							}

							@Override
							public InputStream openInputStream() throws IOException {
								return jarFile.getInputStream(fEntry);
							}

							@Override
							public long getLastModifiedMillis() {
								return fEntry.getTime();
							}
						};
					}

					@Override
					public void remove() {
					}

					@Override
					public void close() throws Exception {
						jarFile.close();
					}

				};
			}
		};
	}

	@Override
	public Void call() throws Exception {
		// unpack the jar resources but do not overwrite newer files
		Path destDir = environment.getHomeFolder();
		try (AutoCloseableIterator<Unpackable> iter = getUnpackables().iterator()) {
			while (iter.hasNext()) {
				Unpackable entry = iter.next();
				
				final Path t = destDir.resolve(Paths.get(entry.getName()));
				if (entry.isDirectory()) {
					if (Files.notExists(t)) {
						log.debug("creating folder " + t);
						Files.createDirectories(t);
					}
					continue;
				}
				try (InputStream is = entry.openInputStream()) {
					long entryLastModifiedTime = entry.getLastModifiedMillis();
					boolean notExists = Files.notExists(t);
					if (notExists) {
						createAndWriteFile(t, is, buffer, entryLastModifiedTime);
						continue;
					}
					boolean entryHasNoModificationDate = entryLastModifiedTime == -1;
					if (entryHasNoModificationDate) {
						createAndOverWriteFile(t, is, buffer, entryLastModifiedTime);
						continue;
					}
					boolean entryIsNewerThanExistingFile = entryLastModifiedTime > Files.getLastModifiedTime(t).toMillis();
					if (entryIsNewerThanExistingFile) {
						createAndOverWriteFile(t, is, buffer, entryLastModifiedTime);
						continue;
					}
				}
			}
		}
		return null;
	}

	protected final void createAndOverWriteFile(Path t, InputStream entryInputStream, byte[] buffer, long lastModifiedMillis) throws IOException {
		if (Files.exists(t)) {
			Files.delete(t);
		}
		createAndWriteFile(t, entryInputStream, buffer, lastModifiedMillis);
	}

	protected final void createAndWriteFile(Path t, InputStream entryInputStream, byte[] buffer, long lastModifiedMillis) throws IOException {
		Path parentFolderPath = t.getParent();
		if (parentFolderPath != null && Files.notExists(parentFolderPath)) {
			log.debug("creating folder " + parentFolderPath);
			Files.createDirectories(parentFolderPath);
		}
		log.debug("creating file " + t);
		Files.createFile(t);
		try (OutputStream os = Files.newOutputStream(t, StandardOpenOption.WRITE)) {
			int i = entryInputStream.read(buffer);
			while (i >= 0) {
				os.write(buffer, 0, i);
				i = entryInputStream.read(buffer);
			}
		}
		if (lastModifiedMillis > -1) {
			Files.setLastModifiedTime(t, FileTime.fromMillis(lastModifiedMillis));
		}
	}

}
