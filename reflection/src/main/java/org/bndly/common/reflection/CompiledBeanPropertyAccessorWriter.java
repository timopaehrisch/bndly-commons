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

import static org.bndly.common.reflection.AbstractBeanPropertyAccessorWriter.collectionPropertyDescriptor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class CompiledBeanPropertyAccessorWriter implements BeanPropertyAccessor, BeanPropertyWriter {

	private final Class inspectedType;
	private final CompiledFieldBeanPropertyAccessorWriter fieldAccessorWriter;
	private final CompiledMethodBeanPropertyAccessorWriter methodAccessorWriter;
	private final Map<String, BeanPropertyAccessor> accessorsByPropertyName;
	private final Map<String, BeanPropertyWriter> writersByPropertyName;

	public CompiledBeanPropertyAccessorWriter(Class inspectedType) {
		this.inspectedType = inspectedType;
		fieldAccessorWriter = new CompiledFieldBeanPropertyAccessorWriter(inspectedType);
		methodAccessorWriter = new CompiledMethodBeanPropertyAccessorWriter(inspectedType);
		Map<String, BeanPropertyAccessor> accessorsByPropertyNameTmp = new HashMap<>();
		Map<String, BeanPropertyWriter> writersByPropertyNameTmp = new HashMap<>();
		for (String supportedPropertyName : methodAccessorWriter.getSupportedReadablePropertyNames()) {
			accessorsByPropertyNameTmp.put(supportedPropertyName, methodAccessorWriter);
		}
		for (String supportedPropertyName : methodAccessorWriter.getSupportedWritablePropertyNames()) {
			writersByPropertyNameTmp.put(supportedPropertyName, methodAccessorWriter);
		}
		for (String supportedPropertyName : fieldAccessorWriter.getSupportedPropertyNames()) {
			accessorsByPropertyNameTmp.put(supportedPropertyName, fieldAccessorWriter);
			writersByPropertyNameTmp.put(supportedPropertyName, fieldAccessorWriter);
		}
		
		accessorsByPropertyName = Collections.unmodifiableMap(accessorsByPropertyNameTmp);
		writersByPropertyName = Collections.unmodifiableMap(writersByPropertyNameTmp);
	}

	public Class getInspectedType() {
		return inspectedType;
	}

	public CompiledFieldBeanPropertyAccessorWriter getFieldAccessorWriter() {
		return fieldAccessorWriter;
	}

	public CompiledMethodBeanPropertyAccessorWriter getMethodAccessorWriter() {
		return methodAccessorWriter;
	}
	
	@Override
	public Object get(final String propertyName, Object target, TypeHint... typeHints) {
		String tmp = propertyName;
		CollectionProperty collectionDescriptor = collectionPropertyDescriptor(propertyName);
		if (collectionDescriptor != null) {
			tmp = collectionDescriptor.getCollectionPropertyName();
		}
		BeanPropertyAccessor accessor = accessorsByPropertyName.get(tmp);
		if (accessor == null) {
			return null;
		}
		return accessor.get(propertyName, target, typeHints);
	}

	@Override
	public boolean set(final String propertyName, Object value, Object target, TypeHint... typeHints) {
		String tmp = propertyName;
		CollectionProperty collectionDescriptor = collectionPropertyDescriptor(propertyName);
		if (collectionDescriptor != null) {
			tmp = collectionDescriptor.getCollectionPropertyName();
		}
		BeanPropertyWriter writer = writersByPropertyName.get(tmp);
		if (writer == null) {
			return false;
		}
		return writer.set(propertyName, value, target, typeHints);
	}

	@Override
	public Class<?> typeOf(final String propertyName, Object target, TypeHint... typeHints) {
		String tmp = propertyName;
		CollectionProperty collectionDescriptor = collectionPropertyDescriptor(propertyName);
		if (collectionDescriptor != null) {
			tmp = collectionDescriptor.getCollectionPropertyName();
		}
		BeanPropertyAccessor accessor = accessorsByPropertyName.get(tmp);
		if (accessor == null) {
			throw new UnresolvablePropertyException(propertyName, inspectedType, "could not resolve property " + tmp + " in type " + inspectedType.getSimpleName());
		}
		return accessor.typeOf(propertyName, target, typeHints);
	}
	
}
