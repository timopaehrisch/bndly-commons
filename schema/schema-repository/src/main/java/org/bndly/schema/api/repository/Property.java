package org.bndly.schema.api.repository;

/*-
 * #%L
 * Schema Repository
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

import org.bndly.schema.api.Record;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Date;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface Property extends BelongsToRepositorySession, IndexedItem {
	public static enum Type {
		STRING,
		DATE,
		DECIMAL,
		LONG,
		DOUBLE,
		BOOLEAN,
		BINARY,
		ENTITY
	}
	Type getType();
	String getName();
	Node getNode();
	boolean isMultiValued();
	String getString();
	String[] getStrings();
	Date getDate();
	Date[] getDates();
	BigDecimal getDecimal();
	BigDecimal[] getDecimals();
	Long getLong();
	Long[] getLongs();
	Double getDouble();
	Double[] getDoubles();
	Boolean getBoolean();
	Boolean[] getBooleans();
	InputStream getBinary();
	InputStream[] getBinaries();
	Record getEntity();
	Record[] getEntities();
	Object getValue();
	Object[] getValues();
	void setValue(Object rawValue) throws RepositoryException;
	void setValue(int index, Object rawValue) throws RepositoryException;
	void setValues(Object... rawValues) throws RepositoryException;
	void addValue(Object rawValue) throws RepositoryException;
	void remove() throws RepositoryException;
}
