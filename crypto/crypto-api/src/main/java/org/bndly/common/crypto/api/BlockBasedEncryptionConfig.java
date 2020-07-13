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

/**
 * A BlockBasedEncryptionConfig can be used to define configuration parameters, that only apply to algorithms, which have fixed block sizes.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface BlockBasedEncryptionConfig extends EncryptionConfig {
	/**
	 * Gets the fixed block size of an data block with encrypted data.
	 * @return -1 If the block size can not be determined. Otherwise the block size in bytes.
	 */
	int getBlockSize();
	
	/**
	 * Gets the fixed maximum size of data, that can be stored in a single encryption block.
	 * @return -1 If the maximum size of data per block can not be determined. Otherwise the size of data in bytes per encryption block.
	 */
	int getMaxDataBlockSize();
}
