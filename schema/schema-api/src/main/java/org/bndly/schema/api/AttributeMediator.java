package org.bndly.schema.api;

/*-
 * #%L
 * Schema API
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

import org.bndly.schema.api.query.PreparedStatementValueProvider;
import org.bndly.schema.api.query.ValueProvider;
import org.bndly.schema.model.Attribute;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface AttributeMediator<E extends Attribute> extends AttributeToColumnConverter<E> {

	@Override
	int columnSqlType(E attribute);

	PreparedStatementValueProvider createPreparedStatementValueProvider(E attribute, Object rawValue);

	boolean requiresColumnMapping(E attribute);

	String columnName(E attribute);

	Object extractFromResultSet(ResultSet rs, String columnName, E attribute, RecordContext recordContext) throws SQLException;

	boolean isAttributePresent(Record record, E attribute);

	Object getAttributeValue(Record record, E attribute);

	Object filterValueForPreparedStatementArgument(Object value, E attribute);

	void setParameterInPreparedStatement(int index, PreparedStatement ps, E attribute, Record record) throws SQLException;

	void setRawParameterInPreparedStatement(int index, PreparedStatement ps, E attribute, Object rawValue) throws SQLException;

	ValueProvider createValueProviderFor(Record record, E attribute);
}
