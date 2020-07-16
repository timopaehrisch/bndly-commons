package org.bndly.rest.client.api;

/*-
 * #%L
 * REST Client API
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

public interface ReadInterceptor<E> {
	/**
	 * this method will be invoked by the DAO when a read operation is performed. 
	 * the implementing interceptor can make changes to the bean it receives.
	 * the interceptor has to return the bean instance, that will be returned by the read operation
	 * @param bean the original bean received from the service
	 * @return the bean that should be returned by the read operation
	 */
	E process(E bean);
}
