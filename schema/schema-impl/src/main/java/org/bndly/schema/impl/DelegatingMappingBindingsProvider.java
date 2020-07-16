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

import org.bndly.schema.api.MappingBinding;
import org.bndly.schema.api.MappingBindingsProvider;
import java.util.List;

public abstract class DelegatingMappingBindingsProvider implements MappingBindingsProvider {

	private MappingBindingsProvider parent;

	@Override
	public List<MappingBinding> getMappingBindings() {
		if (parent != null) {
			List<MappingBinding> bindings = parent.getMappingBindings();
			if (bindings != null) {
				return bindings;
			}
		}
		return localMappingBindings();
	}

	@Override
	public MappingBinding getRootMappingBinding() {
		if (parent != null) {
			MappingBinding binding = parent.getRootMappingBinding();
			if (binding != null) {
				return binding;
			}
		}
		return localRootMappingBindings();
	}

	protected abstract List<MappingBinding> localMappingBindings();

	protected abstract MappingBinding localRootMappingBindings();

	public void setParent(MappingBindingsProvider parent) {
		this.parent = parent;
	}

}
