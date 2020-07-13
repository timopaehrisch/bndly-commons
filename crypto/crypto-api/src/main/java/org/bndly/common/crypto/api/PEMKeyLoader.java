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

import java.io.InputStream;
import java.nio.file.Path;
import java.security.PublicKey;

/**
 * A PEMKeyLoader loads PEM key information and returns a {@link PublicKey} instance.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface PEMKeyLoader {
	/**
	 * Loads a {@link PublicKey} from a PEM file of the provided path.
	 * @param filePath The path to the PEM file
	 * @return The loaded {@link PublicKey}. Never null.
	 * @throws CryptoException if the {@link PublicKey} can not be loaded from the PEM file.
	 */
	public PublicKey loadRSAPublicKeyFromFile(Path filePath) throws CryptoException;
	
	/**
	 * Loads a {@link PublicKey} from a provided input stream of PEM data.
	 * @param is The stream, that holds the raw PEM data.
	 * @return The loaded {@link PublicKey}. Never null.
	 * @throws CryptoException if the {@link PublicKey} can not be loaded from the input stream.
	 */
	public PublicKey loadRSAPublicKeyFromStream(InputStream is) throws CryptoException;
	
	/**
	 * Loads a {@link PublicKey} from a provided byte array of PEM data.
	 * @param bytes The bytes of raw PEM data.
	 * @return The loaded {@link PublicKey}. Never null.
	 * @throws CryptoException if the {@link PublicKey} can not be loaded from the byte array.
	 */
	public PublicKey loadRSAPublicKeyFromBytes(byte[] bytes) throws CryptoException;
}
