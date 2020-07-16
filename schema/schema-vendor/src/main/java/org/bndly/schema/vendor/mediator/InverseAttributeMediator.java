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

import org.bndly.schema.api.Record;
import org.bndly.schema.api.RecordContext;
import org.bndly.schema.api.query.ValueProvider;
import org.bndly.schema.model.InverseAttribute;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class InverseAttributeMediator extends AbstractAttributeMediator<InverseAttribute> {

    @Override
    public boolean requiresColumnMapping(InverseAttribute attribute) {
        return false;
    }
    
    @Override
    public String columnType(InverseAttribute attribute) {
        throw new IllegalStateException("inverse attributes are not mapped to columns.");
    }

    @Override
    public int columnSqlType(InverseAttribute attribute) {
        throw new IllegalStateException("inverse attributes are not mapped to columns.");
    }

    @Override
    public Object extractFromResultSet(ResultSet rs, String columnName, InverseAttribute attribute, RecordContext recordContext) throws SQLException {
        throw new IllegalStateException("inverse attributes are not mapped to columns.");
    }

    @Override
    public Object getAttributeValue(Record record, InverseAttribute attribute) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void setParameterInPreparedStatement(int index, PreparedStatement ps, InverseAttribute attribute, Record record) throws SQLException {
        throw new IllegalStateException("inverse attributes are not mapped to columns.");
    }

	@Override
	public void setRawParameterInPreparedStatement(int index, PreparedStatement ps, InverseAttribute attribute, Object rawValue) throws SQLException {
        throw new IllegalStateException("inverse attributes are not mapped to columns.");
	}

	@Override
	public ValueProvider createValueProviderFor(Record record, InverseAttribute attribute) {
		throw new IllegalStateException("inverse attributes are not mapped to columns.");
	}
    
}
