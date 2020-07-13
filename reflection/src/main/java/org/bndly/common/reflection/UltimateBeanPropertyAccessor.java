package org.bndly.common.reflection;

/*-
 * #%L
 * Reflection
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

public class UltimateBeanPropertyAccessor implements BeanPropertyAccessor {
	private final GetterBeanPropertyAccessor getter;
	private final FieldBeanPropertyAccessor field;

	public UltimateBeanPropertyAccessor() {
		getter = new GetterBeanPropertyAccessor();
		field = new FieldBeanPropertyAccessor();
	}

	@Override
	public Object get(String propertyName, Object target, TypeHint... typeHints) {
		Object o = getter.get(propertyName, target, typeHints);
		if (o == null) {
			o = field.get(propertyName, target, typeHints);
		}
		return o;
	}

	@Override
	public Class<?> typeOf(String propertyName, Object target, TypeHint... typeHints) {
		Class<?> type = getter.typeOf(propertyName, target, typeHints);
		if (type == null) {
			type = field.typeOf(propertyName, target, typeHints);
		}
		return type;
	}

}
