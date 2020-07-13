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
import org.bndly.common.data.api.FolderWatcher;
import org.bndly.common.data.api.FolderWatcherListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.testng.Assert;
import static org.testng.Assert.*;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class FolderWatcherTest {
	
	@Test
	public void testNIOWatcher() throws IOException, InterruptedException {
		Path p = Paths.get("emptyfolder");
		try {
			deleteFolderContent(p.toFile());
			if(Files.notExists(p)) {
				Files.createDirectories(p);
			}
			final NIOFolderWatcherImpl watcher = new NIOFolderWatcherImpl("test");
			watcher.setFolderLocation("emptyfolder");
			final Data[] d = new Data[2];
			watcher.addListener(new FolderWatcherListener() {
				int pos = 0;
				@Override
				public void newData(FolderWatcher folderWatcher, Data data, File file) {
					d[pos] = data;
					pos++;
					if(pos == d.length) {
						watcher.stop();
					}
				}

				@Override
				public void updatedData(FolderWatcher folderWatcher, Data data, File file) {
				}

				@Override
				public void deletedData(FolderWatcher folderWatcher, Data data, File file) {
				}

				@Override
				public void deletedSubFolder(FolderWatcher folderWatcher, File subFolder) {
				}

				@Override
				public void newSubFolder(FolderWatcher folderWatcher, File subFolder) {
					Path newfileinsubfolder = subFolder.toPath().resolve("newfileinsubfolder");
					try {
						Files.createFile(newfileinsubfolder);
					} catch (IOException ex) {
						Assert.fail("could not create file in subfolder", ex);
					}
				}

				@Override
				public void shuttingDown(FolderWatcher folderWatcher) {
				}
			});
			watcher.start();
			Path nf = p.resolve("newfile");
			Files.createFile(nf);
			Path subfolder = p.resolve("subfolder");
			Files.createDirectory(subfolder);
			watcher.join();
			assertNotNull(d[0]);
			assertEquals(d[0].getName(), "newfile");
			assertNotNull(d[1]);
			assertEquals(d[1].getName(), "subfolder/newfileinsubfolder");
		} finally {
			deleteFolderContent(p.toFile());
		}
	}

	private void deleteFolderContent(File f) {
		if(!f.exists()) {
			return;
		}
		if(f.isDirectory()) {
			File[] files = f.listFiles();
			for (File file : files) {
				deleteFolderContent(file);
			}
			if(!f.delete()) {
				throw new IllegalStateException("could not delete folder after deleting content");
			}
		} else if(f.isFile()) {
			if(!f.delete()) {
				throw new IllegalStateException("could not delete file");
			}
		}
	}
}
