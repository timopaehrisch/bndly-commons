package org.bndly.common.osgi.config;

/*-
 * #%L
 * OSGI Config
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
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ObfuscationTest {

	@Test
	public void testObfuscation() throws IOException {
		String input = "hello world";
		// negative values should not be used, because there the first bit is indicating a multi byte character. this may lead to problems with the inverse transformation
		for (byte mask = 1; mask <= 127; mask++) {
			ObfuscationPrefixHandler obfuscationPrefixHandler = new ObfuscationPrefixHandler("UTF-8", mask);
			String obfuscated = obfuscationPrefixHandler.set(input);
			Assert.assertNotEquals(obfuscated, input, "failed to obfuscate for mask " + mask);
			String deobfuscated = obfuscationPrefixHandler.get(obfuscated);
			Assert.assertEquals(deobfuscated, input, "failed to deobfuscate for mask " + mask);
			// prevent the overflow
			if (mask == 127) {
				break;
			}
		}
		ObfuscationPrefixHandler obfuscationPrefixHandler = new ObfuscationPrefixHandler("UTF-8", (byte)85);
		String obfuscated = obfuscationPrefixHandler.set("ebx");
		obfuscated = obfuscated;
	}
}
