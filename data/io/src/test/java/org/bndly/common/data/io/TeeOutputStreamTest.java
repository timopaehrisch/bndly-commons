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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class TeeOutputStreamTest {
	
	private static class FailOutputStream extends OutputStream {

		@Override
		public void write(int b) throws IOException {
			throw new IOException("this call always fails at writing");
		}
		
	}
	
	@Test
	public void testSimple() throws IOException {
		ByteArrayOutputStream left = new ByteArrayOutputStream();
		ByteArrayOutputStream right = new ByteArrayOutputStream();
		TeeOutputStream tee = new TeeOutputStream(left, right);
		String original = "Hello Tee World";
		tee.write(original.getBytes("UTF-8"));
		tee.flush();
		Assert.assertEquals(left.toString("UTF-8"), original);
		Assert.assertEquals(right.toString("UTF-8"), original);
	}
	
	@Test
	public void testIgnoreOneSideFail() throws IOException {
		FailOutputStream left = new FailOutputStream();
		ByteArrayOutputStream right = new ByteArrayOutputStream();
		TeeOutputStream tee = new TeeOutputStream(left, right);
		String original = "Hello Tee World";
		tee.write(original.getBytes("UTF-8"));
		tee.flush();
		Assert.assertEquals(right.toString("UTF-8"), original);
	}
	
	@Test
	public void testOneSideFail() throws IOException {
		FailOutputStream left = new FailOutputStream();
		ByteArrayOutputStream right = new ByteArrayOutputStream();
		TeeOutputStream tee = new TeeOutputStream(left, right).failOnSingleFailure();
		String original = "Hello Tee World";
		try {
			tee.write(original.getBytes("UTF-8"));
			Assert.fail("write should have failed");
		} catch(IOException e) {
			Assert.assertEquals(e.getMessage(), "this call always fails at writing");
		}
		
		tee = new TeeOutputStream(right, left).failOnSingleFailure();
		try {
			tee.write(original.getBytes("UTF-8"));
			Assert.fail("write should have failed");
		} catch(IOException e) {
			Assert.assertEquals(e.getMessage(), "this call always fails at writing");
		}
	}
	
	@Test
	public void testFailWhenAllSidesFail() throws IOException {
		FailOutputStream left = new FailOutputStream();
		FailOutputStream right = new FailOutputStream();
		TeeOutputStream tee = new TeeOutputStream(left, right);
		String original = "Hello Tee World";
		try {
			tee.write(original.getBytes("UTF-8"));
			Assert.fail("write should have failed");
		} catch(IOException e) {
			Assert.assertEquals(e.getMessage(), "this call always fails at writing");
		}
	}
}
