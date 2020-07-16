package org.bndly.rest.repository.resources.json;

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

import org.bndly.common.converter.api.ConversionException;
import org.bndly.common.converter.api.Converter;
import org.bndly.common.json.serializing.JSONWriter;
import org.bndly.schema.api.repository.Property;
import org.bndly.schema.api.repository.RepositoryException;
import java.io.IOException;
import java.util.Date;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class DateMultiJSONPropertyWriter implements JSONPropertyWriter {
	private final Converter<Date, String> dateConverter;

	public DateMultiJSONPropertyWriter(Converter<Date, String> dateConverter) {
		if (dateConverter == null) {
			throw new IllegalArgumentException("dateConverter is not allowed to be null");
		}
		this.dateConverter = dateConverter;
	}
	
	@Override
	public void write(Property property, JSONWriter writer) throws RepositoryException, IOException {
		Date[] dates = property.getDates();
		if (dates != null) {
			boolean firstString = true;
			for (Date date : dates) {
				if (!firstString) {
					writer.writeComma();
				}
				if (date != null) {
					try {
						writer.writeString(dateConverter.convert(date));
					} catch (ConversionException ex) {
						throw new IOException("could not convert date while writing property to json: " + ex.getMessage(), ex);
					}
				} else {
					writer.writeNull();
				}
				firstString = false;
			}
		} else {
			writer.writeNull();
		}
	}
	
}
