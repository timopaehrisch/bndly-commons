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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class CryptoPropertyTest {

	Pattern pattern = Pattern.compile(SecuredConfigArtifactInstaller.SECURED_VALUE_REGEX);
	
	@Test
	public void testEncryptedPropertyValues() {
		String[] valid = new String[]{
			"@{CRYPT:a}",
			"@{CRYPT:}",
			"@{CRYPT: }",
			"@{CRYPT:}}",
			"@{CRYPT:\\n}",
			"@{CRYPT:a\\nb}"
		};
		String[] values = new String[]{
			"a",
			"",
			" ",
			"}",
			"\\n",
			"a\\nb"
		};
		for (int i = 0; i < values.length; i++) {
			String value = values[i];
			String property = valid[i];
			Matcher matcher = pattern.matcher(property);
			boolean matches = matcher.matches();
			Assert.assertTrue(matches, property + " did not match regex pattern: " + pattern.pattern());
			Assert.assertEquals(matcher.group(1), "CRYPT");
			Assert.assertEquals(matcher.group(2), value);
		}
	}
	
	@Test
	public void testNonEncryptedPropertyValues() {
		String[] invalid = new String[]{
			"@{:a}",
			"@{a:a}",
			"@{A:",
			"{A:}",
			"@A:}",
			" @{A:}",
			"@{A:} ",
			"@{1:}",
			"@{1:}",
			"A:}",
			"@{CRYPT:a} ",
			" @{CRYPT:a}"
		};
		int i = 0;
		for (String property : invalid) {
			Matcher matcher = pattern.matcher(property);
			boolean matches = matcher.matches();
			Assert.assertFalse(matches, property + " did match regex pattern: " + pattern.pattern());
			i++;
		}
	}
	
}
