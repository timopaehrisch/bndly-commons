package org.bndly.rest.descriptor.model;

/*-
 * #%L
 * REST API Descriptor
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

public class TypeDescriptor {
	private final Class<?> javaType;
	private String name;
	private TypeDescriptor parent;
	private List<TypeDescriptor> subs;
	private List<TypeMember> members;
	private List<LinkDescriptor> links;
	private String rootElement;
	private boolean referenceType;

	public TypeDescriptor(Class<?> javaType) {
		if (javaType == null) {
			throw new IllegalArgumentException("javatype is required");
		}
		this.javaType = javaType;
	}

	public Class<?> getJavaType() {
		return javaType;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public TypeDescriptor getParent() {
		return parent;
	}

	public void setParent(TypeDescriptor parent) {
		this.parent = parent;
	}

	public List<TypeDescriptor> getSubs() {
		return subs;
	}

	public void setSubs(List<TypeDescriptor> subs) {
		this.subs = subs;
	}

	public List<TypeMember> getMembers() {
		return members;
	}

	public void setMembers(List<TypeMember> members) {
		this.members = members;
	}

	public List<LinkDescriptor> getLinks() {
		return links;
	}

	public void setLinks(List<LinkDescriptor> links) {
		this.links = links;
	}

	public String getRootElement() {
		return rootElement;
	}

	public void setRootElement(String rootElement) {
		this.rootElement = rootElement;
	}

	public boolean isReferenceType() {
		return referenceType;
	}

	public void setReferenceType(boolean referenceType) {
		this.referenceType = referenceType;
	}

}
