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

import org.bndly.common.data.io.IOUtils;
import org.bndly.common.data.io.ReplayableInputStream;
import org.bndly.rest.cache.api.CacheEntry;
import org.bndly.rest.cache.impl.CacheEntryIO;
import org.bndly.rest.api.ResourceURI;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Date;
import java.util.UUID;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class CacheEntryTest {

	@Test
	public void testCacheEntryIO() throws IOException {
		CacheEntryIO io = new CacheEntryIO();
		
		Path f = Paths.get("target","testcacheentryio.tar");
		Files.deleteIfExists(f);
		
		final Date date = new Date();
		final String etag = UUID.randomUUID().toString();
		final String data = "Teststring";
		final String contentType = "text/plain";
		final String contentLanguage = "de";
		final Integer maxAge = 60;
		
		f = Files.createFile(f);
		
		byte[] dataBytes;
		dataBytes = data.getBytes("UTF-8");
		final ReplayableInputStream dataStream = ReplayableInputStream.newInstance(new ByteArrayInputStream(dataBytes));
		
		try(OutputStream os = Files.newOutputStream(f, StandardOpenOption.WRITE)) {
			io.serialize(new CacheEntry() {
				@Override
				public ResourceURI getResourceURI() {
					return null;
				}

				@Override
				public Integer getMaxAge() {
					return maxAge;
				}

				@Override
				public void close() throws Exception {
				}

				@Override
				public String getContentLanguage() {
					return contentLanguage;
				}

				@Override
				public String getContentType() {
					return contentType;
				}

				@Override
				public String getEncoding() {
					return "UTF-8";
				}

				@Override
				public ReplayableInputStream getData() {
					return dataStream;
				}

				@Override
				public Long getContentLength() {
					return null;
				}


				@Override
				public String getETag() {
					return etag;
				}

				@Override
				public Date getLastModified() {
					return date;
				}
			}, os);
		}
		
		try(InputStream is = Files.newInputStream(f, StandardOpenOption.READ)){
			CacheEntry entry = io.deserialize(is);
			Assert.assertNotNull(entry);
			ReplayableInputStream dataIs = entry.getData();
			String dataFromEntry = IOUtils.readToString(dataIs, "UTF-8");
			Assert.assertEquals(dataFromEntry, data);
			Assert.assertEquals(entry.getETag(), etag);
			Assert.assertEquals(entry.getContentType(), contentType);
			Assert.assertEquals(entry.getEncoding(), "UTF-8");
			Assert.assertEquals(entry.getContentLanguage(), contentLanguage);
			Assert.assertEquals(entry.getMaxAge(), maxAge);
			Date dateFromEntry = entry.getLastModified();
			Assert.assertNotNull(dateFromEntry);
			long dif = dateFromEntry.getTime() - date.getTime();
			// we accept 1 second difference
			Assert.assertTrue(dif < 1000, "last modified date is different");
		}
		
		try(InputStream is = Files.newInputStream(f, StandardOpenOption.READ)){
			CacheEntry entry = io.deserialize(is);
			ReplayableInputStream dataIs = entry.getData();
			long toSkip = data.length()/2;
			long skipped = dataIs.skipUnreplayable(toSkip);
			Assert.assertEquals(skipped, toSkip);
			String dataFromEntry = IOUtils.readToString(dataIs, "UTF-8");
			Assert.assertEquals(dataFromEntry, data.substring((int) toSkip));
			long br = dataIs.getBytesRead();
			Assert.assertEquals(data.length()-toSkip, br);
			long l = dataIs.getLength();
			Assert.assertEquals(l, data.length());
		}
	}
}
