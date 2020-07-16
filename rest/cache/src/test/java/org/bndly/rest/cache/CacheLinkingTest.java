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

import org.bndly.rest.cache.api.CacheLink;
import org.bndly.rest.cache.api.CacheLinkingService;
import org.bndly.rest.cache.impl.CacheInterceptor;
import org.bndly.rest.cache.impl.FlushPath;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class CacheLinkingTest {
	
	private class CollectingConsumer implements CacheLinkingService.Consumer {
		private final List<CacheLink> links = new ArrayList<>();

		@Override
		public void accept(CacheLink link) throws IOException {
			this.links.add(link);
		}

		public List<CacheLink> getLinks() {
			return links;
		}
		
	}
	
	@Test
	public void testLinkCreationAndInvalidation() throws IOException {
		CacheInterceptor interceptor = new CacheInterceptor();
		CacheLinkingService cacheLinkingService = interceptor;
		
		Path cacheRoot = Paths.get("target/testdata/cacheroot");
		deleteRecursive(cacheRoot);
		String linkTargetPathString = "test/data/en.txt";
		interceptor.setCacheRoot(cacheRoot.toString());
		Path linkTarget = cacheRoot.resolve("test").resolve("data").resolve("en.txt");
		Path parent = linkTarget.getParent();
		if (Files.notExists(parent)) {
			Files.createDirectories(parent);
		}
		Files.createFile(linkTarget);
		try (OutputStream os = Files.newOutputStream(linkTarget)) {
			os.write("Hello World".getBytes("UTF-8"));
			os.flush();
		}
		cacheLinkingService.link("has/a/link", linkTargetPathString);
		
		CollectingConsumer collectingConsumer = new CollectingConsumer(){

			@Override
			public void accept(CacheLink link) throws IOException {
				super.accept(link);
				link.invalidate();
			}
			
		};
		cacheLinkingService.iterateLinksOf("has/a/link", collectingConsumer);
		Assert.assertEquals(collectingConsumer.getLinks().size(), 1);
		
		collectingConsumer.getLinks().clear();
		
		cacheLinkingService.iterateLinksOf("has/a/link", collectingConsumer);
		Assert.assertEquals(collectingConsumer.getLinks().size(), 0);
		
		Assert.assertTrue(Files.notExists(linkTarget), "link target should not exist anymore");
	}
	
	@Test
	public void testLinkCreationAndInvalidation2() throws IOException {
		CacheInterceptor interceptor = new CacheInterceptor();
		CacheLinkingService cacheLinkingService = interceptor;
		
		Path cacheRoot = Paths.get("target/testdata/testLinkCreationAndInvalidation2");
		deleteRecursive(cacheRoot);
		String linkTargetPathString = "test/data";
		interceptor.setCacheRoot(cacheRoot.toString());
		Path linkTarget = cacheRoot.resolve("test").resolve("data").resolve("en.txt");
		Path parent = linkTarget.getParent();
		if (Files.notExists(parent)) {
			Files.createDirectories(parent);
		}
		Files.createFile(linkTarget);
		cacheLinkingService.link("has/a/link", linkTargetPathString);

		new FlushPath("has/a/link", cacheRoot.toString(), Collections.EMPTY_LIST).doFlush();
		Assert.assertTrue(Files.notExists(linkTarget));
	}
	
	
	private void deleteRecursive(Path path) throws IOException {
		if (Files.isDirectory(path)) {
			Files.walkFileTree(path, new FileVisitor<Path>() {
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
					return FileVisitResult.TERMINATE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}
			});
		}
	}
	
}
