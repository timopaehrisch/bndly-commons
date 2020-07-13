package org.bndly.common.crypto.impl;

/*-
 * #%L
 * Crypto Impl
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
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class BlockReaderTest {
	@Test
	public void testBlockReading() throws UnsupportedEncodingException, IOException {
		String input = "hello";
		byte[] buf = input.getBytes("UTF-8");
		InputStream bis = new ByteArrayInputStream(buf);
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		new BlockReader(buf.length) {
			@Override
			protected void doWithBlock(byte[] blockBuffer, int length) throws IOException {
				bos.write(blockBuffer, 0, length);
			}
		}.read(bis);
		bos.flush();
		Assert.assertEquals(new String(bos.toByteArray(), "UTF-8"), input);
		bis.reset();
		final ByteArrayOutputStream bos2 = new ByteArrayOutputStream();
		new BlockReader(buf.length / 2) {
			@Override
			protected void doWithBlock(byte[] blockBuffer, int length) throws IOException {
				bos2.write(blockBuffer, 0, length);
			}
		}.read(bis);
		bos2.flush();
		Assert.assertEquals(new String(bos2.toByteArray(), "UTF-8"), input);
	}
	
	@Test
	public void testBlockReadingArray() throws UnsupportedEncodingException, IOException {
		String input = "hello";
		byte[] buf = input.getBytes("UTF-8");
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		new BlockReader(buf.length) {
			@Override
			protected void doWithBlock(byte[] blockBuffer, int length) throws IOException {
				bos.write(blockBuffer, 0, length);
			}
		}.read(buf);
		bos.flush();
		Assert.assertEquals(new String(bos.toByteArray(), "UTF-8"), input);
		final ByteArrayOutputStream bos2 = new ByteArrayOutputStream();
		new BlockReader(buf.length / 2) {
			@Override
			protected void doWithBlock(byte[] blockBuffer, int length) throws IOException {
				bos2.write(blockBuffer, 0, length);
			}
		}.read(buf);
		bos2.flush();
		Assert.assertEquals(new String(bos2.toByteArray(), "UTF-8"), input);
	}
}
