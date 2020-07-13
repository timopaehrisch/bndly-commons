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

import org.bndly.common.converter.api.ConversionException;
import org.osgi.service.component.annotations.Component;

@Component(service = org.bndly.common.converter.api.Converter.class, immediate = true)
public class IntegerStringConverter extends AbstractPrimitiveStringConverter<Integer> {

	public IntegerStringConverter() {
		super(Integer.class);
	}

	@Override
	public String convert(Integer source) throws ConversionException {
		if (source == null) {
			return null;
		}
		return Integer.toString(source);
	}

	@Override
	public Integer reverseConvert(String source) throws ConversionException {
		if (source == null) {
			return null;
		}
		try {
			return Integer.valueOf(source);
		} catch (NumberFormatException e) {
			throw new ConversionException(sourceType, source, "could not convert string to " + sourceType.getName());
		}
	}

}
