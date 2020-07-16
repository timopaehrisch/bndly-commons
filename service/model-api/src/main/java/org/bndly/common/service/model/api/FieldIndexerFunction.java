package org.bndly.common.service.model.api;

/*-
 * #%L
 * Service Model API
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

import org.bndly.common.reflection.PathResolver;
import org.bndly.common.reflection.PathResolverImpl;

public class FieldIndexerFunction<I, O> implements IndexerFunction<I, O> {

	private final String fieldName;
	private final PathResolver accessor = new PathResolverImpl();

	public FieldIndexerFunction(String fieldName) {
		this.fieldName = fieldName;
	}

	@Override
	public I index(O o) {
		return (I) accessor.resolve(fieldName, o, Object.class);
	}

}
