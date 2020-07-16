package org.bndly.common.json.impl;

/*-
 * #%L
 * JSON
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

import org.bndly.common.json.api.ConversionContext;
import org.bndly.common.json.api.Instanciator;
import org.bndly.common.json.model.JSValue;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class DefaultInstanciator implements Instanciator {

	@Override
	public boolean canInstantiate(Type desiredType, ConversionContext conversionContext, JSValue value) {
		if (!Class.class.isInstance(desiredType)) {
			return false;
		}
		Class cls = (Class) desiredType;
		if (cls.isInterface()) {
			// use the interface instance builder for this
			return false;
		}
		if (cls.isArray()) {
			return false;
		}
		try {
			Constructor defaultConstructor = cls.getConstructor();
		} catch (NoSuchMethodException | SecurityException ex) {
			return false;
		}
		return true;
	}

	@Override
	public Object instantiate(Type desiredType, ConversionContext conversionContext, JSValue value) {
		Class cls = (Class) desiredType;
		try {
			Constructor defaultConstructor = cls.getConstructor();
			return defaultConstructor.newInstance();
		} catch (NoSuchMethodException | SecurityException ex) {
			throw new IllegalStateException(ex);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
			throw new IllegalStateException(ex);
		}
	}
	
}
