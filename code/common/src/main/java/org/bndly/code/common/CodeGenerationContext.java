package org.bndly.code.common;

/*-
 * #%L
 * Code Common
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

import java.util.ArrayList;
import java.util.List;

import org.bndly.code.renderer.ImportResolver;

public final class CodeGenerationContext extends CodeGenerationObject {

	private ImportResolver importResolver;
	private List<CodeGenerationObject> contextObjects;
	private String basePackage;
	
	public CodeGenerationContext() {
		contextObjects = new ArrayList<>();
		setContext(this);
		
	}
	
	@Override
	public <T extends CodeGenerationObject> T create(Class<T> type) {
		T instance = super.create(type);
		contextObjects.add(instance);
		return instance;
	}
	
	public <T extends CodeGenerationObject> T get(Class<T> type) {
		for (CodeGenerationObject obj : contextObjects) {
			if (obj.is(type)) {
				return type.cast(obj);
			}
		}
		return null;
	}

	public <T> T getImpl(Class<T> type) {
		for (CodeGenerationObject obj : contextObjects) {
			if (type.isAssignableFrom(obj.getClass())) {
				return type.cast(obj);
			}
		}
		return null;
	}

	public ImportResolver getImportResolver() {
		return importResolver;
	}

	public void setImportResolver(ImportResolver importResolver) {
		this.importResolver = importResolver;
	}

	public List<CodeGenerationObject> getCreatedObjects() {
		return contextObjects;
	}

	public void setCreatedObjects(List<CodeGenerationObject> createdObjects) {
		this.contextObjects = createdObjects;
	}

	public String getBasePackage() {
		return basePackage;
	}
	public void setBasePackage(String basePackage) {
		this.basePackage = basePackage;
	}
}
