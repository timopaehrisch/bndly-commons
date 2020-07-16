package org.bndly.schema.impl;

/*-
 * #%L
 * Schema Impl
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

import org.bndly.schema.api.AttributeMediator;
import org.bndly.schema.api.MediatorRegistry;
import org.bndly.schema.model.Attribute;
import java.util.HashMap;
import java.util.Map;

public class MediatorRegistryImpl implements MediatorRegistry {

	private Map<Class<? extends Attribute>, AttributeMediator<?>> mediators = new HashMap<>();

	@Override
	public <E extends Attribute> AttributeMediator<E> getMediatorForAttribute(E attribute) {
		AttributeMediator<?> m = mediators.get(attribute.getClass());
		if (m == null) {
			throw new IllegalStateException("could not find mediator for attribute: " + attribute.getClass().getSimpleName());
		}
		return (AttributeMediator<E>) m;
	}

	public void setMediators(Map<Class<? extends Attribute>, AttributeMediator<?>> mediators) {
		this.mediators.putAll(mediators);
	}

}
