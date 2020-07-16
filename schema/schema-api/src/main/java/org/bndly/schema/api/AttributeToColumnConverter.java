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

import org.bndly.schema.model.Attribute;

/**
 * This is a convenience interface to define a strategy, that derives a SQL column type from an attribute definition.
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface AttributeToColumnConverter<E extends Attribute> {
	/**
	 * Gets the SQL column type name like 'VARCHAR' or 'BIGINT' for the provided attribute
	 * @param attribute The attribute that shall be mapped to a column
	 * @return the SQL column type name
	 */
	String columnType(E attribute);
	
	/**
	 * Gets the SQL type for the provided attribute. The SQL type should come from {@link java.sql.Types}.
	 * @param attribute The attribute that shall be mapped to a column
	 * @return the SQL column type
	 */
	int columnSqlType(E attribute);
}
