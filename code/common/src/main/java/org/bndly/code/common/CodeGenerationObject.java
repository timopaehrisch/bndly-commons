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

import java.lang.reflect.Constructor;

public abstract class CodeGenerationObject {
	private CodeGenerationContext context;
	private CodeGenerationObject owner;
	public CodeGenerationContext getContext() {
		if (context == null) {
			if (owner != null) {
				return owner.getContext();
			} else {
				throw new IllegalStateException("CodeGenerationContext could not be retrieved.");
			}
		} else {
			return context;
		}
	}
	protected void setContext(CodeGenerationContext context) {
		this.context = context;
	}
	protected CodeGenerationObject getOwner() {
		return owner;
	}
	public <T extends CodeGenerationObject> T create(Class<T> type) {
		T instance = null;
		try {
			instance = type.newInstance();
			CodeGenerationObject cgo = instance;
			cgo.owner = this;
			return instance;
		} catch (InstantiationException | IllegalAccessException e) {
			throw new IllegalStateException(type.getSimpleName() + " could not be instantiated.", e);
		}
	}
	public <T extends CodeGenerationObject> T create(Class<T> type, Object...constructorParameters) {
		T instance = null;
		int numberOfParameters = 0;
		if (constructorParameters != null) {
			numberOfParameters = constructorParameters.length;
		}
		try {
			Constructor<?>[] constructors = type.getConstructors();
			for (Constructor<?> constructor : constructors) {
				Class<?>[] parameterTypes = constructor.getParameterTypes();
				int tmp = 0;
				if (parameterTypes != null) {
					tmp = parameterTypes.length;
				}
				
				if (numberOfParameters == tmp) {
					instance = type.cast(constructor.newInstance(constructorParameters));
				}

			}
			if (instance == null) {
				instance = type.newInstance();
			}
			CodeGenerationObject cgo = instance;
			cgo.owner = this;
			return instance;
		} catch (Exception e) {
			throw new IllegalStateException(type.getSimpleName() + " could not be instantiated.", e);
		}
	}
	public <T extends CodeGenerationObject> boolean is(Class<T> type) {
		return (type.isAssignableFrom(getClass()));
	}
	public <T extends CodeGenerationObject> T as(Class<T> type) {
		return type.cast(this);
	}
}
