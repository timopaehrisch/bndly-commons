package org.bndly.schema.model;

/*-
 * #%L
 * Schema Model
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

import java.util.List;

public class Schema extends Annotatable {
	private final String name;
	private final String namespace;
    private List<Type> types;
    private List<Mixin> mixins;
    private List<UniqueConstraint> uniqueConstraints;

	public Schema(String name, String namespace) {
		this.name = name;
		this.namespace = namespace;
	}

	public String getNamespace() {
		return namespace;
	}

	public String getName() {
		return name;
	}

    public List<Mixin> getMixins() {
        return mixins;
    }

    public void setMixins(List<Mixin> mixins) {
        this.mixins = mixins;
    }

    public List<Type> getTypes() {
        return types;
    }

    public void setTypes(List<Type> types) {
        this.types = types;
    }

    public List<UniqueConstraint> getUniqueConstraints() {
        return uniqueConstraints;
    }

    public void setUniqueConstraints(List<UniqueConstraint> uniqueConstraints) {
        this.uniqueConstraints = uniqueConstraints;
    }
    
}
