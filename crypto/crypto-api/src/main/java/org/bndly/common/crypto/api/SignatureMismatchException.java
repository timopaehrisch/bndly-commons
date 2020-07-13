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
 * This exception should be thrown, if a signature validation fails.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class SignatureMismatchException extends CryptoException {

	/**
	 * Use this constructor, if there is no root cause for throwing an exception.
	 * @param message A message, that describes the exception.
	 */
	public SignatureMismatchException(String message) {
		super(message);
	}

	/**
	 * Use this constructor, if there is a root cause for throwing an exception.
	 * @param message A message, that describes the exception.
	 * @param cause A root cause, that should be wrapped.
	 */
	public SignatureMismatchException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
