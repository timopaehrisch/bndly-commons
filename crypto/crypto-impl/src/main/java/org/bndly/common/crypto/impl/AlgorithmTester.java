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

/**
 * The AlgorithmTester test, if the short name of an algorithm applies to a provided algorithm name.
 * The tester could be used to check if a specific algorithm like 'AES/ECB/PKCS7Padding' is an 'AES' algorithm in general.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class AlgorithmTester {

	private final String name;
	private final String prefix;

	/**
	 * Creates a AlgorithmTester with the short name of the algorithm.
	 * @param name the name of the algorithm. could be something like 'RSA' or 'AES'.
	 */
	public AlgorithmTester(String name) {
		this.name = name;
		prefix = name + "/";
	}

	/**
	 * This method tests, if the provided algorithm name applies for the current AlgorithmTester instance. 
	 * This method will return true, if the provided algorithm name either equals or started with the name of 
	 * the algorithm of this tester instance.
	 * @param alg The name of the algorithm, that should be tested.
	 * @return true, if the provided algorithm either equals or starts with the algorithm name of the current tester instance.
	 */
	public boolean matches(String alg) {
		return alg.equals(name) || alg.startsWith(prefix);
	}
}
