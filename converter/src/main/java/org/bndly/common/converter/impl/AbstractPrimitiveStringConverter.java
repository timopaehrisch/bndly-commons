package org.bndly.common.converter.impl;

/*-
 * #%L
 * Converter
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

import org.bndly.common.converter.api.Converter;
import org.bndly.common.converter.api.TwoWayConverter;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public abstract class AbstractPrimitiveStringConverter<P> implements TwoWayConverter<P, String> {

	protected final Class<P> sourceType;

	public AbstractPrimitiveStringConverter(Class<P> sourceType) {
		if (sourceType == null) {
			throw new IllegalArgumentException("source type is not allowed to be null");
		}
		this.sourceType = sourceType;
	}

	@Override
	public Converter<P, String> canConvertFromTo(Class<? extends P> sourceType, Class<? extends String> targetType) {
		if (targetType.equals(String.class) && this.sourceType.isAssignableFrom(sourceType)) {
			return this;
		}
		return null;
	}

}
