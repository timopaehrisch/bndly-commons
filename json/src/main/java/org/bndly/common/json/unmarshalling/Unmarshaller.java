package org.bndly.common.json.unmarshalling;

/*-
 * #%L
 * JSON
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

import org.bndly.common.json.api.ConversionContext;
import org.bndly.common.json.api.ConversionContextBuilder;
import org.bndly.common.json.model.JSObject;

/**
 * maps a parsed JSON document to a Java Class
 */
public class Unmarshaller {

	public <E> E unmarshall(JSObject documentRoot, Class<E> targetType) {
		ConversionContext ctx = new ConversionContextBuilder().initDefaults().build();
		E instance = (E) ctx.deserialize(targetType, documentRoot);
		if (instance == null) {
			throw new UnmarshallingException("could not instantiate type " + targetType.getName());
		}
		return instance;
	}
}
