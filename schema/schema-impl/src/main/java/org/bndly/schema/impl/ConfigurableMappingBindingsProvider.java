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

import org.bndly.schema.api.AliasBinding;
import org.bndly.schema.api.MappingBinding;
import org.bndly.schema.model.NamedAttributeHolder;
import org.bndly.schema.model.Type;
import java.util.ArrayList;
import java.util.List;

public class ConfigurableMappingBindingsProvider extends DelegatingMappingBindingsProvider {

	private List<MappingBinding> bindings;
	private MappingBinding rootBinding;

	@Override
	protected List<MappingBinding> localMappingBindings() {
		return bindings;
	}

	@Override
	protected MappingBinding localRootMappingBindings() {
		return rootBinding;
	}

	public MappingBinding getMappingBindingFor(Type type) {
		if (bindings != null) {
			for (MappingBinding mappingBinding : bindings) {
				if (mappingBinding.getHolder() == type) {
					return mappingBinding;
				}
			}
		}
		return null;
	}

	public MappingBinding createMappingBinding(Type type, AliasBinding pkAlias, String tableAlias) {
		MappingBinding b = new MappingBinding(type, pkAlias, tableAlias);
		if (bindings == null) {
			bindings = new ArrayList<>();
		}
		bindings.add(b);
		return b;
	}

	public MappingBinding createRootMappingBinding(NamedAttributeHolder nah, AliasBinding pkAlias, String tableAlias) {
		MappingBinding b = new MappingBinding(nah, pkAlias, tableAlias);
		rootBinding = b;
		return rootBinding;
	}
}
