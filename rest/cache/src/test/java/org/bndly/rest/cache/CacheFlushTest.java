package org.bndly.rest.cache;

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

import org.bndly.rest.cache.api.CacheTransaction;
import org.bndly.rest.cache.impl.CacheInterceptor;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class CacheFlushTest {
	private List<Path> shouldExist;
	private Path p;
	private Path aTar;
	private Path aJsonTar;
	private Path aSelectorJsonTar;
	private Path aFolder;
	private Path a1Tar;
	private Path a1JsonTar;
	private Path a1SelectorJsonTar;
	private CacheInterceptor interceptor;
	private Path bTar;
	private Path bPath;
	private Path b1Tar;
	private Path b1Path;
	private Path a1Path;
	private Path cPath;
	private Path c1Path;
	private Path c1Tar;
	private Path a1LinkToCPath;
	private Path a1LinkToC1;

	@BeforeMethod
	public void before() throws IOException {
		interceptor = new CacheInterceptor();
		p = Paths.get(".", "target", "restcache");
		if (Files.exists(p)) {
			Files.walkFileTree(p, new FileVisitor<Path>() {

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
		}
		Files.createDirectories(p);
		interceptor.setCacheRoot(p.toString());
		cPath = p.resolve("c");
		Files.createDirectory(cPath);
		c1Path = cPath.resolve("1");
		Files.createDirectory(c1Path);
		c1Tar = c1Path.resolve("tar");
		Files.createFile(c1Tar);
		
		bPath = p.resolve("b");
		Files.createDirectory(bPath);
		bTar = bPath.resolve("tar");
		Files.createFile(bTar);
		b1Path = bPath.resolve("1");
		Files.createDirectory(b1Path);
		b1Tar = b1Path.resolve("tar");
		Files.createFile(b1Tar);
		
		aFolder = p.resolve("a");
		Files.createDirectory(aFolder);
		aTar = aFolder.resolve("tar");
		Files.createFile(aTar);
		aJsonTar = aFolder.resolve("json.tar");
		Files.createFile(aJsonTar);
		aSelectorJsonTar = aFolder.resolve("selector.json.tar");
		Files.createFile(aSelectorJsonTar);
		a1Path = aFolder.resolve("1");
		Files.createDirectory(a1Path);
		a1Tar = a1Path.resolve("tar");
		Files.createFile(a1Tar);
		a1JsonTar = a1Path.resolve("json.tar");
		Files.createFile(a1JsonTar);
		a1SelectorJsonTar = a1Path.resolve("selector.json.tar");
		Files.createFile(a1SelectorJsonTar);
		
		a1LinkToCPath = a1Path.resolve("links/c");
		Files.createDirectories(a1LinkToCPath);
		a1LinkToC1 = a1LinkToCPath.resolve("1");
		Path linkTargetPath = a1LinkToC1.getParent().relativize(c1Path);
		Assert.assertEquals(linkTargetPath.toString(), "../../../../c/1");
		Files.createSymbolicLink(a1LinkToC1, linkTargetPath);
		
		shouldExist = new ArrayList<>();
		shouldExist.add(aTar);
		shouldExist.add(aJsonTar);
		shouldExist.add(aSelectorJsonTar);
		shouldExist.add(a1Tar);
		shouldExist.add(a1JsonTar);
		shouldExist.add(a1SelectorJsonTar);
		shouldExist.add(bTar);
		shouldExist.add(b1Tar);
		for (Path se : shouldExist) {
			if(!Files.isRegularFile(se)) {
				Assert.fail("file did not exist.");
			}
		}
		if(!Files.isSymbolicLink(a1LinkToC1)) {
			Assert.fail("symbolic link did not exist.");
		}
	}
	
	@Test
	public void testFlushSingleFile() throws IOException {
		try(CacheTransaction cacheTransaction = interceptor.createCacheTransaction()){
			cacheTransaction.flush("a/1/json.tar");
		}
		Assert.assertTrue(!Files.exists(a1JsonTar), "file should have been removed");
		for (Path se : shouldExist) {
			if(se != a1JsonTar) {
				Assert.assertTrue(Files.exists(se), "file "+se+" should still exist");
			}
		}
	}
	
	@Test
	public void testFlushFilePattern() {
		try(CacheTransaction cacheTransaction = interceptor.createCacheTransaction()){
			cacheTransaction.flush("a/1");
		}
		Assert.assertTrue(!Files.exists(a1Tar), "file should have been removed");
		Assert.assertTrue(!Files.exists(a1JsonTar), "file should have been removed");
		Assert.assertTrue(!Files.exists(a1SelectorJsonTar), "file should have been removed");
		Assert.assertTrue(!Files.exists(a1LinkToC1), "symbolic link should have been removed");
		Assert.assertTrue(!Files.exists(c1Tar), "file behind the symbolic link should have been removed");
		for (Path se : shouldExist) {
			if(se != a1JsonTar && se != a1Tar && se != a1SelectorJsonTar) {
				Assert.assertTrue(Files.exists(se), "file should still exist");
			}
		}
	}
	@Test
	public void testFlushRootFilePattern() {
		try(CacheTransaction cacheTransaction = interceptor.createCacheTransaction()){
			cacheTransaction.flush("a");
		}
		Assert.assertTrue(!Files.exists(aTar), "file should have been removed");
		Assert.assertTrue(!Files.exists(aJsonTar), "file should have been removed");
		Assert.assertTrue(!Files.exists(aSelectorJsonTar), "file should have been removed");
		for (Path se : shouldExist) {
			if(se != aTar && se != aJsonTar && se != aSelectorJsonTar) {
				Assert.assertTrue(Files.exists(se), "file should still exist");
			}
		}
	}
	@Test
	public void testFlushRecursive() {
		try(CacheTransaction cacheTransaction = interceptor.createCacheTransaction()){
			cacheTransaction.flushRecursive("a");
		}
		for (Path se : shouldExist) {
			if(se == b1Tar || se == bTar) {
				Assert.assertTrue(Files.exists(se), "file should still exist");
			} else {
				Assert.assertTrue(!Files.exists(se), "file should have been removed");
			}
		}
	}
}
