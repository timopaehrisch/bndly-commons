package org.bndly.common.data.io;

/*-
 * #%L
 * Data IO
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.Random;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ReplayableInputStreamTest {

	@AfterMethod
	public void afterMethod() {
		System.gc();
	}
	
	@Test
	public void testReplay() throws UnsupportedEncodingException, IOException {
		ByteArrayInputStream bis = new ByteArrayInputStream("Hello world".getBytes("UTF-8")) {

			@Override
			public boolean markSupported() {
				return false;
			}

		};
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ByteArrayOutputStream bos2 = new ByteArrayOutputStream();
		ByteArrayOutputStream bos3 = new ByteArrayOutputStream();
		try (ReplayableInputStream replayable = ReplayableInputStream.newInstance(bis)) {
			IOUtils.copy(replayable, bos);
			bos.flush();
			bos.close();
			try (ReplayableInputStream replayed = replayable.replay()) {
				IOUtils.copy(replayed, bos2);
				bos2.flush();
				bos2.close();
			}
			replayable.replay();
			try (ReplayableInputStream replayed = replayable) {
				IOUtils.copy(replayed, bos3);
				bos3.flush();
				bos3.close();
			}
		}
		String bosString = bos.toString("UTF-8");
		String bos2String = bos2.toString("UTF-8");
		String bos3String = bos3.toString("UTF-8");
		Assert.assertEquals(bosString, "Hello world");
		Assert.assertEquals(bos2String, "Hello world");
		Assert.assertEquals(bos3String, "Hello world");
	}

	@Test
	public void testInstantReplay() throws UnsupportedEncodingException, IOException {
		ByteArrayInputStream bis = new ByteArrayInputStream("Hello world".getBytes("UTF-8"));
		ReplayableInputStream rpis = ReplayableInputStream.newInstance(bis);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		try (InputStream is = rpis.replay()) {
			int i;
			while ((i = is.read()) > -1) {
				// read everything
				bos.write(i);
			}
			bos.flush();
			bos.close();
		}
		String bosString = bos.toString("UTF-8");
		Assert.assertEquals(bosString, "Hello world");
	}

	@Test
	public void testReplayByteBasedStream() throws UnsupportedEncodingException, IOException {
		ReplayableInputStream rpis = ReplayableInputStream.newInstance("Hello world".getBytes("UTF-8"));
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		IOUtils.copy(rpis, bos);
		bos.flush();
		
		String bosString = bos.toString("UTF-8");
		Assert.assertEquals(bosString, "Hello world");
		
		rpis.replay();
		
		ByteArrayOutputStream bos2 = new ByteArrayOutputStream();
		IOUtils.copy(rpis, bos2);
		bos2.flush();
		
		String bos2String = bos2.toString("UTF-8");
		Assert.assertEquals(bos2String, "Hello world");
	}
	
	@Test
	public void testPartialStreamReplay() throws UnsupportedEncodingException, IOException {
		String original = "Hello world";
		ReplayableInputStream rpis = ReplayableInputStream.newInstance(new ByteArrayInputStream(original.getBytes("UTF-8")), 5);
		ReplayableInputStream rpis2 = ReplayableInputStream.newInstance(new ByteArrayInputStream(original.getBytes("UTF-8")));
		ReplayableInputStream rpis3 = ReplayableInputStream.newInstance(new ByteArrayInputStream(original.getBytes("UTF-8")), original.length()+1);
		String hello = IOUtils.readToString(rpis, "UTF-8");
		String helloWorld = IOUtils.readToString(rpis2, "UTF-8");
		String helloWorld3 = IOUtils.readToString(rpis3, "UTF-8");
		Assert.assertEquals(rpis.getBytesRead(), 5);
		Assert.assertEquals(rpis2.getBytesRead(), original.length());
		Assert.assertEquals(rpis3.getBytesRead(), original.length());
		Assert.assertEquals(rpis.getLength(), 5);
		Assert.assertEquals(rpis2.getLength(), original.length());
		Assert.assertEquals(rpis3.getLength(), original.length());
		Assert.assertEquals(hello, "Hello");
		Assert.assertEquals(helloWorld, original);
		Assert.assertEquals(helloWorld3, original);
	}
	
	@Test
	public void testBufferSwappingForLargeData() throws UnsupportedEncodingException, IOException {
		final int limit = ReplayableInputStream.IN_MEMORY_BUF_LIMIT+1;
		InputStream largeStream = new InputStream() {
			int pos = 0;

			@Override
			public int read() throws IOException {
				if(pos >= limit) {
					return -1;
				}
				pos++;
				return 1;
			}
		};
		ReplayableInputStream rpis = ReplayableInputStream.newInstance(largeStream);
		int pos = 0;
		while(rpis.read() > -1) {
			// read everything
			if(pos < ReplayableInputStream.IN_MEMORY_BUF_LIMIT) {
				Assert.assertTrue(rpis.isBufferedInMemory(), "expected that the data is buffered in memory");
			} else {
				Assert.assertTrue(!rpis.isBufferedInMemory(), "expected that the data is buffered in the file system");
			}
			pos++;
		}
		Path bufferPath = rpis.getFileSystemBufferPath();
		Assert.assertTrue(!rpis.isBufferedInMemory(), "expected that the data is buffered in the file system");
	}
	
	@Test
	public void testEquality() throws IOException {
		Random rand = new Random(new Date().getTime());
		byte[] randomData = new byte[2*ReplayableInputStream.IN_MEMORY_BUF_LIMIT];
//		byte[] randomData = new byte[256];
		for (int i = 0; i < randomData.length; i++) {
			randomData[i] = Integer.valueOf(rand.nextInt(256)).byteValue();
		}
		ReplayableInputStream rpis = ReplayableInputStream.newInstance(new ByteArrayInputStream(randomData));
		try(FilterInputStream filterInputStream = new FilterInputStream(rpis){}) {
			byte[] buffer = new byte[2048];
			int i;
			while((i = filterInputStream.read(buffer)) > -1) {
				// read until the end
			}
		}
		Path pathInFs = rpis.getFileSystemBufferPath();
		long fileSize = Files.size(pathInFs);
		Assert.assertEquals(fileSize, randomData.length, "file system buffer did contain more data than the original.");
		ReplayableInputStream replay = rpis.replay();
		Assert.assertEquals(replay.getLength(), randomData.length);
		byte[] replayData = new byte[randomData.length];
		int j = 0;
		int i;
		while((i = replay.read()) > -1) {
			try {
				replayData[j] = (byte) i;
			} catch(ArrayIndexOutOfBoundsException e) {
				Assert.fail("replayed input stream did contain more data than the original.");
			}
			Assert.assertEquals(replayData[j], randomData[j]);
			j++;
		}
		
		ReplayableInputStream rpisSameInstance = rpis.getBufferOutputStream().getBufferedDataAsReplayableStream();
		Assert.assertTrue(rpisSameInstance == rpis, "instances where not the same");
	}
	
	@Test
	public void testNoOp() throws IOException {
		// a no-op replayable stream is in fact not replayable. it won't buffer the data read from it.
		String original = "Hello wolrd";
		ReplayableInputStream rpis = ReplayableInputStream.newInstance(new ByteArrayInputStream(original.getBytes("UTF-8")));
		rpis = rpis.noOp();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		IOUtils.copy(rpis, bos);
		bos.flush();
		String afterFirstRead = bos.toString("UTF-8");
		Assert.assertEquals(afterFirstRead, original);
		rpis = rpis.replay();
		
		ByteArrayOutputStream bos2 = new ByteArrayOutputStream();
		IOUtils.copy(rpis, bos2);
		bos2.flush();
		String afterSecondRead = bos2.toString("UTF-8");
		Assert.assertEquals(afterSecondRead, "");
	}
	
	@Test
	public void testSkip() throws IOException {
		// a no-op replayable stream is in fact not replayable. it won't buffer the data read from it.
		String original = "Hello world";
		ReplayableInputStream rpis = ReplayableInputStream.newInstance(new ByteArrayInputStream(original.getBytes("UTF-8")));
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		bos.write(rpis.read());
		rpis.skip(5);
		IOUtils.copy(rpis, bos);
		bos.flush();
		String afterRead = bos.toString("UTF-8");
		Assert.assertEquals(afterRead, "Hworld");
		rpis = rpis.replay();
		
		ByteArrayOutputStream bos2 = new ByteArrayOutputStream();
		IOUtils.copy(rpis, bos2);
		bos2.flush();
		String afterSecondRead = bos2.toString("UTF-8");
		Assert.assertEquals(afterSecondRead, original);
		
		rpis.replay();
		long l = rpis.getLength();
		Assert.assertEquals(l, original.length());
		long skipped = rpis.skip(l+10);
		Assert.assertEquals(skipped, l);
		
	}
	
	@Test
	public void testSkipUnreplayable() throws IOException {
		// a no-op replayable stream is in fact not replayable. it won't buffer the data read from it.
		String original = "Hello world";
		ReplayableInputStream rpis = ReplayableInputStream.newInstance(new ByteArrayInputStream(original.getBytes("UTF-8")));
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		bos.write(rpis.read());
		rpis.skipUnreplayable(5);
		IOUtils.copy(rpis, bos);
		bos.flush();
		String afterRead = bos.toString("UTF-8");
		Assert.assertEquals(afterRead, "Hworld");
		rpis = rpis.replay();
		
		ByteArrayOutputStream bos2 = new ByteArrayOutputStream();
		IOUtils.copy(rpis, bos2);
		bos2.flush();
		String afterSecondRead = bos2.toString("UTF-8");
		Assert.assertEquals(afterSecondRead, "Hworld");
	}
}
