package org.bndly.common.service.setup;

/*-
 * #%L
 * Service Shared Client Setup
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

import org.bndly.common.mapper.Mapper;
import org.bndly.common.mapper.MapperAmbiguityResolver;
import org.bndly.rest.atomlink.api.annotation.Reference;
import org.bndly.common.service.model.api.ReferableResource;
import java.util.Map;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class DefaultMapperAmbiguityResolver implements MapperAmbiguityResolver {
	
	@Override
	public Mapper pickMapper(Class<?> inputType, Map<Class<?>, Mapper> mappersForInput, Object inputObject) {
		if (ReferableResource.class.isInstance(inputObject)) {
			boolean isRef = ((ReferableResource) inputObject).isResourceReference();
			boolean isSmartRef = ((ReferableResource) inputObject).isSmartReference();
			for (Map.Entry<Class<?>, Mapper> entrySet : mappersForInput.entrySet()) {
				Class<? extends Object> key = entrySet.getKey();
				// simple reference by id
				if (key.isAnnotationPresent(Reference.class) && isRef && !isSmartRef) {
					return entrySet.getValue();
					// smart reference
				} else if (!key.isAnnotationPresent(Reference.class) && isRef && isSmartRef) {
					return entrySet.getValue();
				// full object
				} else if (!isRef && !key.isAnnotationPresent(Reference.class)) {
					return entrySet.getValue();
				}
			}
		}
		return null;
	}
}
