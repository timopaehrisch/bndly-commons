package org.bndly.schema.vendor.mediator;

/*-
 * #%L
 * Schema Vendor
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

import org.bndly.schema.api.AttributeMediator;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.query.PreparedStatementValueProvider;
import org.bndly.schema.model.Attribute;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class AbstractAttributeMediator<E extends Attribute> implements AttributeMediator<E> {

	@Override
	public boolean requiresColumnMapping(E attribute) {
		return true;
	}

	@Override
	public boolean isAttributePresent(Record record, E attribute) {
		return record.isAttributePresent(attribute.getName());
	}

	@Override
	public Object filterValueForPreparedStatementArgument(Object value, E attribute) {
		return value;
	}

	@Override
	public String columnName(E attribute) {
		return attribute.getName().toUpperCase();
	}

	private PreparedStatementValueProvider createPreparedStatementValueProvider(final E attribute, final Record record) {
		return new PreparedStatementValueProvider() {

			@Override
			public Object get() {
				final Object rawValue = record.getAttributeValue(attribute.getName());
				Object filtered = filterValueForPreparedStatementArgument(rawValue, attribute);
				return filtered;
			}

			@Override
			public void set(int index, PreparedStatement ps) throws SQLException {
				setParameterInPreparedStatement(index, ps, attribute, record);
			}
		};
	}

	@Override
	public PreparedStatementValueProvider createPreparedStatementValueProvider(final E attribute, final Object rawValue) {
		return new PreparedStatementValueProvider() {

			@Override
			public Object get() {
				return rawValue;
			}

			@Override
			public void set(int index, PreparedStatement ps) throws SQLException {
				setRawParameterInPreparedStatement(index, ps, attribute, rawValue);
			}
		};
	}
}
