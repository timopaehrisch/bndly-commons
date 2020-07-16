package org.bndly.rest.repository.resources.beans.api;

/*-
 * #%L
 * REST Repository Resource
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

import org.bndly.schema.api.repository.beans.Bean;
import org.bndly.schema.api.repository.beans.BeanResolver;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public abstract class BeanWrapper implements Bean {

	private final Bean wrapped;

	public BeanWrapper(Bean wrapped) {
		if (wrapped == null) {
			throw new IllegalArgumentException("wrapped bean can not be null");
		}
		this.wrapped = wrapped;
	}
	
	@Override
	public final BeanResolver getBeanResolver() {
		return wrapped.getBeanResolver();
	}

	@Override
	public final String getName() {
		return wrapped.getName();
	}

	@Override
	public final String getPath() {
		return wrapped.getPath();
	}

	@Override
	public final String getBeanType() {
		return wrapped.getBeanType();
	}

	@Override
	public Bean getParent() {
		return wrapped.getParent();
	}

	@Override
	public final Bean getChild(String name) {
		return wrapped.getChild(name);
	}

	@Override
	public final Iterator<Bean> getChildren() {
		return wrapped.getChildren();
	}

	@Override
	public final Map<String, Object> getProperties() {
		return wrapped.getProperties();
	}

	@Override
	public final Object getProperty(String name) {
		return wrapped.getProperty(name);
	}

	@Override
	public final Object get(String name) {
		return wrapped.get(name);
	}

	@Override
	public <T> T morphTo(Class<T> type) {
		return wrapped.morphTo(type);
	}
	
}
