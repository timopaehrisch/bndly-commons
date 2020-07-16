package org.bndly.de.rest.jetty.bridge;

/*-
 * #%L
 * REST Jetty Bridge
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

import org.testng.annotations.Test;
import org.eclipse.jetty.util.security.Password;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class DecodeDefaultPasswordTest {

	@Test
	public void decodeDefaultPassword() {
		String keyStorePassword = Password.deobfuscate("OBF:1vny1zlo1x8e1vnw1vn61x8g1zlu1vn4");
		String keyManagerPassword = Password.deobfuscate("OBF:1u2u1wml1z7s1z7a1wnl1u2g");
		String trustStorePassword = Password.deobfuscate("OBF:1vny1zlo1x8e1vnw1vn61x8g1zlu1vn4");
		System.out.println("KeyStorePassword: "+keyStorePassword);
		System.out.println("KeyManagerPassword: "+keyManagerPassword);
		System.out.println("TrustStorePassword: "+trustStorePassword);
	}
}
