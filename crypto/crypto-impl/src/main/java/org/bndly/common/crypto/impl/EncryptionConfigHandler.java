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

import org.bndly.common.crypto.api.EncryptionConfig;
import org.bndly.common.crypto.api.KeystoreEncryptionConfig;
import org.bndly.common.crypto.api.ProvidedKeyEncryptionConfig;
import org.bndly.common.crypto.api.ProvidedKeyPairEncryptionConfig;

/**
 * A EncryptionConfigHandler is a switch around the different sub types of {@link EncryptionConfig}.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface EncryptionConfigHandler<E> {
	/**
	 * This method will be called, if a {@link KeystoreEncryptionConfig} should be handled.
	 * @param keystoreEncryptionConfig the config do handle
	 * @return what ever the handlers purpose is
	 */
	E doWith(KeystoreEncryptionConfig keystoreEncryptionConfig);
	/**
	 * This method will be called, if a {@link ProvidedKeyEncryptionConfig} should be handled.
	 * @param providedKeyEncryptionConfig the config do handle
	 * @return what ever the handlers purpose is
	 */
	E doWith(ProvidedKeyEncryptionConfig providedKeyEncryptionConfig);
	/**
	 * This method will be called, if a {@link ProvidedKeyPairEncryptionConfig} should be handled.
	 * @param providedKeyPairEncryptionConfig the config do handle
	 * @return what ever the handlers purpose is
	 */
	E doWith(ProvidedKeyPairEncryptionConfig providedKeyPairEncryptionConfig);
}
