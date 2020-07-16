package org.bndly.common.bpm.api;

/*-
 * #%L
 * BPM API
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
 * The ProcessInvokerFactory is used to create java object, that defines the contract of a business process in java language constructs. Invocations of methods on the java object will be delegated to
 * an invocation of a business process. Resolving return values and providing parameters is done by using annotations in the java objects interface.
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface ProcessInvokerFactory {

	/**
	 * Create a java object (process invoker), that can be called to invoke business processes via a simple java API. Please note, that created objects need to be destroyed manually by calling
	 * {@link #destroy(java.lang.Object)}.
	 *
	 * @param <T> Any java interface
	 * @param type The java interface with the annotations, that define return values and process parameters.
	 * @param engineName The business process instance name, that shall be used for invocations on the java interface.
	 * @return A java object, that acts as a proxy around business process invocations
	 */
	<T> T create(Class<T> type, String engineName);

	/**
	 * Destroys a java business process proxy object (process invoker). If the provided process invoker is not a valid process invoker or null, this method will act as a no-op.
	 *
	 * @param processInvoker The process invoker instance to destroy.
	 */
	void destroy(Object processInvoker);
}
