package org.bndly.schema.impl;

/*-
 * #%L
 * Schema Impl
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

import org.bndly.schema.api.mapper.RowMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class SimpleTypeRowMapper implements RowMapper<Object> {

	private final Class<?> simpleType;

	public SimpleTypeRowMapper(Class<?> simpleType) {
		this.simpleType = simpleType;
	}
	
	@Override
	public Object mapRow(ResultSet rs, int i) throws SQLException {
		int index = 1;
		if (long.class.equals(simpleType) || Long.class.equals(simpleType)) {
			return rs.getLong(index);
		} else if (int.class.equals(simpleType) || Integer.class.equals(simpleType)) {
			return rs.getInt(index);
		} else if (short.class.equals(simpleType) || Short.class.equals(simpleType)) {
			return rs.getShort(index);
		} else if (byte.class.equals(simpleType) || Byte.class.equals(simpleType)) {
			return rs.getByte(index);
		} else if (double.class.equals(simpleType) || Double.class.equals(simpleType)) {
			return rs.getDouble(index);
		} else if (float.class.equals(simpleType) || Float.class.equals(simpleType)) {
			return rs.getFloat(index);
		} else if (Date.class.equals(simpleType)) {
			return rs.getDate(index);
		} else if (String.class.equals(simpleType)) {
			return rs.getString(index);
		}
		throw new IllegalStateException("unsupported simple type: " + simpleType);
	}
	
}
