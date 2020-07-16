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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Callable;

public class WebappEnvironment implements Environment {

	protected Path getResourcesPath() {
		return getWebAppRoot().resolve(SharedConstants.APP_JAR_RESOURCES);
	}
	
	@Override
	public Path getAutoDeployPath() {
		return getResourcesPath().resolve(SharedConstants.AUTO_DEPLOY_PATH);
	}

	@Override
	public Path getConfigPropertiesPath() {
		return getResourcesPath().resolve("conf").resolve("config.properties");
	}

	@Override
	public Path getApplicationConfigPath() {
		// in a webapp, we will not unpack/copy config files. therefore, we will not point to framework/conf/app but my-webapp/resources/conf/app
		return getResourcesPath().resolve(SharedConstants.SYSTEM_PROPERTY_VALUE_SECURED_CONFIG_LOCATION);

	}

	@Override
	public Path getHomeFolder() {
		return getJarPath().getParent().resolve(SharedConstants.APP_JAR_EXPLODED_FRAMEWORK);
	}

	@Override
	public Path getJettyHome() {
		return getHomeFolder().resolve(SharedConstants.SYSTEM_PROPERTY_VALUE_JETTY_HOME);
	}

	@Override
	public Path getEmbeddedSolrHome() {
		return getHomeFolder().resolve(SharedConstants.SYSTEM_PROPERTY_VALUE_SOLR_HOME);
	}

	@Override
	public Path getTempFolder() {
		return getHomeFolder().resolve(SharedConstants.SYSTEM_PROPERTY_VALUE_JAVA_IO_TEMP);
	}

	@Override
	public Path getLogbackConfigFilePath() {
		return getHomeFolder().resolve(SharedConstants.SYSTEM_PROPERTY_VALUE_LOGBACK_CONFIGURATION_FILE);
	}

	/**
	 * FelixMain can be launched from within a WebApp.
	 * When it is launched from within a standalone Tomcat, then this method will return a path pointing to <code>WEB-INF/classes</code> in the deployed WebApp.
	 * When it is launched from within a Maven Tomcat Plugin via <code>mvn tomcat7:run</code>, then this method will return a path pointing to <code>current-maven-webapp/target/classes</code>.
	 *
	 * In some Tomcat environments, the location is not <code>WEB-INF/classes</code> but <code>WEB-INF/classes/org.bndly/common/app/FelixMain.class</code>.
	 * Therefore we apply a patch to return the expected <code>WEB-INF/classes</code>, if we have the filename <code>FelixMain.class</code>
	 * @return
	 */
	@Override
	public Path getJarPath() {
		URL location = FelixMain.class.getProtectionDomain().getCodeSource().getLocation();
		try {
			Path path = Paths.get(location.toURI());
			if ((FelixMain.class.getSimpleName() + ".class").equals(path.getFileName().toString())) {
				Path current = path;
				Path parent = current.getParent();
				while (current != null && parent != null) {
					if (current.getFileName().toString().equals("classes") && parent.getFileName().toString().equals("WEB-INF")) {
						return current;
					} else {
						current = parent;
						parent = current.getParent();
					}
				}
				throw new IllegalStateException("could not derive jar location from " + path);
			}
			return path;
		} catch (URISyntaxException ex) {
			throw new IllegalStateException("could not determine jar path", ex);
		}
		/**
		*/
	}

	@Override
	public Boolean needsUnpack() {
		return true;
	}

	private Path getWebAppRoot() {
		return getHomeFolder().getParent().getParent();
	}

	@Override
	public Callable<Void> createUnpackCallable(Environment environment, Logger log) {
		return new DefaultUnpackCallable(environment, log) {
			@Override
			protected AutoCloseableIterable<DefaultUnpackCallable.Unpackable> getUnpackables() {
				// we will look in the resources folder of the extracted webapp and use the file-info.properties, to only copy the files, that are not bunbles.
				final Path resourcesPath = getResourcesPath();
				Path fileInfoPropertiesPath = getWebAppRoot().resolve(SharedConstants.FINAL_FILE_INFO_FILE);
				// TODO read the properties and walk the file tree and track the unpackables
				final Properties fileInfo = new Properties();
				if (Files.isRegularFile(fileInfoPropertiesPath)) {
					try (Reader reader = Files.newBufferedReader(fileInfoPropertiesPath, Charset.forName("UTF-8"))) {
						fileInfo.load(reader);
					} catch (IOException e) {
						throw new IllegalStateException("could not read file info properties", e);
					}
				}
				final List<Path> resourceFilePaths = new ArrayList<>();
				try {
					Files.walkFileTree(resourcesPath, new FileVisitor<Path>() {
						@Override
						public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
							Path relativeFilePath = resourcesPath.toAbsolutePath().relativize(file.toAbsolutePath());
							String type = fileInfo.getProperty(relativeFilePath.toString());

							if (type == null || type.equals("r")) {
								resourceFilePaths.add(file);
							}
							return FileVisitResult.CONTINUE;
						}

						@Override
						public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
							return FileVisitResult.TERMINATE;
						}

						@Override
						public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
							return FileVisitResult.CONTINUE;
						}
					});
				} catch (IOException e) {
					throw new IllegalStateException("could not iterate over the resources in " + resourcesPath, e);
				}
				return new AutoCloseableIterable<DefaultUnpackCallable.Unpackable>() {
					@Override
					public AutoCloseableIterator<DefaultUnpackCallable.Unpackable> iterator() {
						final Iterator<Path> wrappedIter = resourceFilePaths.iterator();
						return new AutoCloseableIterator<DefaultUnpackCallable.Unpackable>() {
							@Override
							public boolean hasNext() {
								return wrappedIter.hasNext();
							}

							@Override
							public DefaultUnpackCallable.Unpackable next() {
								final Path path = wrappedIter.next();
								return new Unpackable() {
									@Override
									public boolean isDirectory() {
										return Files.isDirectory(path);
									}

									@Override
									public String getName() {
										return resourcesPath.relativize(path).toString();
									}

									@Override
									public InputStream openInputStream() throws IOException {
										return Files.newInputStream(path, StandardOpenOption.READ);
									}

									@Override
									public long getLastModifiedMillis() throws IOException {
										FileTime ft = Files.getLastModifiedTime(path);
										if (ft == null) {
											return -1;
										}
										return ft.toMillis();
									}
								};
							}

							@Override
							public void remove() {
							}

							@Override
							public void close() {
							}
						};
					}
				};
			}
		};
	}

}
