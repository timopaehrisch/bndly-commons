package org.bndly.schema.impl.repository;

/*-
 * #%L
 * Schema Impl
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

import org.bndly.common.data.io.IOUtils;
import org.bndly.schema.api.repository.PackageExporter;
import org.bndly.schema.api.repository.PackageImporter;
import org.bndly.schema.api.repository.RepositoryException;
import org.bndly.schema.api.repository.RepositoryExporter;
import org.bndly.schema.api.repository.RepositoryImporter;
import org.bndly.schema.api.repository.RepositorySession;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = {PackageImporter.class, PackageExporter.class})
@Designate(ocd = PackageImporterImpl.Configuration.class)
public class PackageImporterImpl implements PackageImporter, PackageExporter {

	@ObjectClassDefinition(
			name = "Package Importer",
			description = "This importer imports node packages into a repository"
	)
	public @interface Configuration {

	}
	
	@Reference
	private RepositoryImporter repositoryImporter;
	
	@Reference
	private RepositoryExporter repositoryExporter;

	@Override
	public void importRepositoryData(InputStream packageData, RepositorySession repositorySession) throws RepositoryException, IOException {
		String uuid = UUID.randomUUID().toString();
		Path tempDirectory = Files.createTempDirectory(uuid);
		byte[] buffer = new byte[1024];
		try {
			try (ZipInputStream zipInputStream = new ZipInputStream(packageData)) {
				ZipEntry currentEntry;
				while ((currentEntry = zipInputStream.getNextEntry()) != null) {
					String entryName = currentEntry.getName();
					boolean isDirectory = currentEntry.isDirectory();
					Path entryPath = tempDirectory.resolve(entryName);
					if (isDirectory) {
						Files.createDirectory(entryPath);
					} else {
						Files.createFile(entryPath);
						long entrySize = currentEntry.getSize();
						// write the data to the file
						try (OutputStream os = Files.newOutputStream(entryPath, StandardOpenOption.WRITE)) {
							if (entrySize > -1) {
								IOUtils.copy(zipInputStream, os, entrySize, buffer);
							} else {
								IOUtils.copy(zipInputStream, os, buffer);
							}
							os.flush();
						}
					}
				}
			}
			repositoryImporter.importRepositoryData(tempDirectory.resolve("root.json"), repositorySession);
		} finally {
			deleteDirectory(tempDirectory);
		}
	}

	@Override
	public void exportRepositoryData(OutputStream target, RepositorySession repositorySession) throws RepositoryException, IOException {
		String uuid = UUID.randomUUID().toString();
		final byte[] buffer = new byte[1024];
		final Path tempDirectory = Files.createTempDirectory(uuid);
		try {
			repositoryExporter.exportRepositoryData(tempDirectory, repositorySession);
			try (final ZipOutputStream zos = new ZipOutputStream(target)) {
				Files.walkFileTree(tempDirectory, new FileVisitor<Path>() {
					@Override
					public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
						Path rel = tempDirectory.relativize(dir);
						if (rel.toString().isEmpty()) {
							return FileVisitResult.CONTINUE;
						} else {
							ZipEntry e = new ZipEntry(rel.toString() + "/");
							zos.putNextEntry(e);
							zos.closeEntry();
						}
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						Path rel = tempDirectory.relativize(file);
						ZipEntry e = new ZipEntry(rel.toString());
						long size = file.toFile().length();
						if (size > 0) {
							e.setSize(size);
						}
						zos.putNextEntry(e);
						try (InputStream is = Files.newInputStream(file, StandardOpenOption.READ)) {
							if (size > 0) {
								IOUtils.copy(is, zos, size, buffer);
							} else {
								IOUtils.copy(is, zos, buffer);
							}
						}
						zos.closeEntry();
						Files.delete(file);
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
						Files.deleteIfExists(file);
						return FileVisitResult.TERMINATE;
					}

					@Override
					public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
						Files.delete(dir);
						return FileVisitResult.CONTINUE;
					}
				});
				zos.finish();
			}
		} finally {
			deleteDirectory(tempDirectory);
		}
	}

	private void deleteDirectory(Path toDelete) throws IOException {
		Files.walkFileTree(toDelete, new FileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
		Files.deleteIfExists(toDelete);
	}

	public void setRepositoryImporter(RepositoryImporter repositoryImporter) {
		this.repositoryImporter = repositoryImporter;
	}

	public void setRepositoryExporter(RepositoryExporter repositoryExporter) {
		this.repositoryExporter = repositoryExporter;
	}

}
