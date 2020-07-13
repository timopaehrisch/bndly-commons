package org.bndly.common.crypto.api;

/*-
 * #%L
 * Crypto API
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

import java.security.Key;

/**
 * A ProvidedKeyEncryptionConfig provides the encryption/decryption key via a specific method.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface ProvidedKeyEncryptionConfig extends EncryptionConfig {
	/**
	 * Gets the key, that should be used for encryption/decryption.
	 * @return a key instance. never null.
	 */
	Key getKey();
}
