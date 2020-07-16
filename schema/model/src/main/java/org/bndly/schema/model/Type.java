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

public class Type extends NamedAttributeHolder {

	private boolean isAbstract;
	private Type superType;
	private List<Type> subTypes;
	private List<Mixin> mixins;

	public Type(Schema schema) {
		super(schema);
	}

	public boolean isAbstract() {
		return isAbstract;
	}

	public void setAbstract(boolean isAbstract) {
		this.isAbstract = isAbstract;
	}

	public Type getSuperType() {
		return superType;
	}

	public void setSuperType(Type superType) {
		this.superType = superType;
	}

	public List<Type> getSubTypes() {
		return subTypes;
	}

	public void setSubTypes(List<Type> subTypes) {
		this.subTypes = subTypes;
	}

	public List<Mixin> getMixins() {
		return mixins;
	}

	public void setMixins(List<Mixin> mixins) {
		this.mixins = mixins;
	}

}
