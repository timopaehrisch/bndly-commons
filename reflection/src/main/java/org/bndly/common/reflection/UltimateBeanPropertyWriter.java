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

public class UltimateBeanPropertyWriter implements BeanPropertyWriter {
	private final FieldBeanPropertyWriter field = new FieldBeanPropertyWriter();
	private final SetterBeanPropertyWriter setter = new SetterBeanPropertyWriter();

	@Override
	public boolean set(String propertyName, Object value, Object target, TypeHint... typeHints) {
		if (!field.set(propertyName, value, target, typeHints)) {
			return setter.set(propertyName, value, target, typeHints);
		}
		return true;
	}
}
