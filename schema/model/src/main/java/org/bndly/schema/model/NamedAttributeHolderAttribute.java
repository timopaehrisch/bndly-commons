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

public abstract class NamedAttributeHolderAttribute<E> extends Attribute {
	private Boolean cascadeDelete;
	private Boolean nullOnDelete;
	private Boolean deleteOrphans;
	private String toOneAttribute;
	public abstract NamedAttributeHolder getNamedAttributeHolder();
	public abstract void setNamedAttributeHolder(E holder);

	public NamedAttributeHolderAttribute() {
		super();
		// by default attributes that refer to other entities should be indexed
		setIndexed(true);
	}

	public Boolean getCascadeDelete() {
		return cascadeDelete;
	}

	public void setCascadeDelete(Boolean cascadeDelete) {
		this.cascadeDelete = cascadeDelete;
	}

	public Boolean getNullOnDelete() {
		return nullOnDelete;
	}

	public void setNullOnDelete(Boolean nullOnDelete) {
		this.nullOnDelete = nullOnDelete;
	}

	public Boolean getDeleteOrphans() {
		return deleteOrphans;
	}

	public void setDeleteOrphans(Boolean deleteOrphans) {
		this.deleteOrphans = deleteOrphans;
	}

	public final String getToOneAttribute() {
		return toOneAttribute;
	}

	public final void setToOneAttribute(String toOneAttribute) {
		this.toOneAttribute = toOneAttribute;
	}
	
}
