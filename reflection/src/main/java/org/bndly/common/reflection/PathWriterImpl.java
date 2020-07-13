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

import java.util.ArrayList;

public class PathWriterImpl implements PathWriter {

	private BeanPropertyAccessor accessor = new UltimateBeanPropertyAccessor();
	private BeanPropertyWriter writer = new UltimateBeanPropertyWriter();
	private InstanceFactory instanceFactory = new InstanceFactory() {
		@Override
		public <E> E instantiateType(Class<E> type) {
			return InstantiationUtil.instantiateType(type);
		}
	};
	private static final TypeHint[] EMPTY = new TypeHint[0];

	private TypeHint[] filterTypeHints(String currentPathSegment, TypeHint... typeHints) {
		ArrayList<TypeHint> out = new ArrayList<>();
		if (AbstractBeanPropertyAccessorWriter.propertyNameRefersToElementInCollection(currentPathSegment)) {
			int i = currentPathSegment.indexOf("[");
			currentPathSegment = currentPathSegment.substring(0, i) + "[]";
		}
		String nestedItemPrefix = currentPathSegment + ".";
		String currentItemPrefix = currentPathSegment;
		for (final TypeHint typeHint : typeHints) {
			if (typeHint.getPath().startsWith(currentItemPrefix)) {
				final String newName;
				if (typeHint.getPath().length() > currentItemPrefix.length() && typeHint.getPath().charAt(currentItemPrefix.length()) == '.') {
					newName = typeHint.getPath().substring(nestedItemPrefix.length());
				} else {
					newName = "";
				}
				out.add(new TypeHint() {

					@Override
					public String getPath() {
						return newName;
					}

					@Override
					public boolean isCollection() {
						return typeHint.isCollection();
					}

					@Override
					public Class<?> getType() {
						return typeHint.getType();
					}
				});
			}
		}
		if (out.isEmpty()) {
			return EMPTY;
		}
		return out.toArray(new TypeHint[out.size()]);
	}

	@Override
	public boolean write(String path, Object value, Object root, TypeHint... typeHints) {
		int i = path.indexOf(".");
		if (i > 0) {
			String currentPathSegment = path.substring(0, i);
			TypeHint[] filteredHints = filterTypeHints(currentPathSegment, typeHints);
			Object subRoot = accessor.get(currentPathSegment, root, filteredHints);
			if (subRoot == null) {
				Class<?> type = accessor.typeOf(currentPathSegment, root, filteredHints);
				subRoot = instanceFactory.instantiateType(type);
				writer.set(currentPathSegment, subRoot, root, filteredHints);
			}
			if (subRoot != null) {
				String subPath = path.substring(i + 1);
				return write(subPath, value, subRoot, filteredHints);
			} else {
				throw new IllegalStateException("could not write to path " + path + " because " + currentPathSegment + " could not be found or instantiated.");
			}
		} else {
			return writer.set(path, value, root, typeHints);
		}
	}

	@Override
	public Class<?> typeOf(String path, Object root, TypeHint... typeHints) {
		int i = path.indexOf(".");
		if (i > 0) {
			String currentPathSegment = path.substring(0, i);
			TypeHint[] filteredHints = filterTypeHints(currentPathSegment, typeHints);
			Object subRoot = accessor.get(currentPathSegment, root, filteredHints);
			if (subRoot == null) {
				Class<?> type = accessor.typeOf(currentPathSegment, root, filteredHints);
				subRoot = instanceFactory.instantiateType(type);
			}
			if (subRoot != null) {
				String subPath = path.substring(i + 1);
				return typeOf(subPath, subRoot, filteredHints);
			} else {
				throw new IllegalStateException("could not resolve type of path " + path + " because " + currentPathSegment + " could not be found or instantiated.");
			}
		} else {
			return accessor.typeOf(path, root, typeHints);
		}
	}

	public void setAccessor(BeanPropertyAccessor accessor) {
		this.accessor = accessor;
	}

	public void setInstanceFactory(InstanceFactory instanceFactory) {
		this.instanceFactory = instanceFactory;
	}

	public void setWriter(BeanPropertyWriter writer) {
		this.writer = writer;
	}
	
}
