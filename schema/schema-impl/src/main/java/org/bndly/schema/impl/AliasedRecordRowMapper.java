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

import org.bndly.schema.api.AliasBinding;
import org.bndly.schema.api.AttributeMediator;
import org.bndly.schema.api.MappingBinding;
import org.bndly.schema.api.MappingBindingsProvider;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.RecordContext;
import org.bndly.schema.api.services.Accessor;
import org.bndly.schema.api.mapper.RowMapper;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.Type;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class AliasedRecordRowMapper implements RowMapper<Record> {

	private final Accessor accessor;
	private final MappingBindingsProvider mappingBindingsProvider;
	private final MediatorRegistryImpl mediatorRegistry;
	private final RecordContext recordContext;

	public AliasedRecordRowMapper(MappingBindingsProvider mappingBindingsProvider, MediatorRegistryImpl mediatorRegistry, Accessor accessor, RecordContext recordContext) {
		this.mappingBindingsProvider = mappingBindingsProvider;
		this.mediatorRegistry = mediatorRegistry;
		this.accessor = accessor;
		this.recordContext = recordContext;
	}

	@Override
	public Record mapRow(ResultSet rs, int i) throws SQLException {
		Record r = null;
		List<MappingBinding> mappingBindings = mappingBindingsProvider.getMappingBindings();
		if (mappingBindings != null) {
			Iterator<MappingBinding> it = mappingBindings.iterator();
			while (r == null && it.hasNext()) {
				MappingBinding binding = it.next();
				r = evaluateBinding(binding, rs);
			}
		}
		if (RecordImpl.class.isInstance(r) && r != null) {
			((RecordImpl) r).setIsDirty(false);
		}
		return r;
	}

	private Record evaluateBinding(MappingBinding binding, ResultSet rs) throws SQLException {
		AliasBinding pkAlias = binding.getPrimaryKeyAlias();
		Attribute pkAtt = pkAlias.getAttribute();
		AttributeMediator<Attribute> mediator = mediatorRegistry.getMediatorForAttribute(pkAtt);
		Object value = mediator.extractFromResultSet(rs, pkAlias.getAlias(), pkAtt, recordContext);
		if (value == null) {
			return null;
		}

		// id will not be null at this place
		Record record = recordContext.get((Type) binding.getHolder(), (Long) value);
		if (record == null) {
			record = recordContext.create(binding.getHolder().getName(), (Long) value);
		}
		evaluateAliasesOfBinding(binding, rs, record);
		return record;
	}

	private void evaluateAliasesOfBinding(MappingBinding binding, ResultSet rs, Record record) throws SQLException {
		List<AliasBinding> a = binding.getAliases();
		if (a != null) {
			for (AliasBinding aliasBinding : a) {
				AttributeMediator<Attribute> mediator = mediatorRegistry.getMediatorForAttribute(aliasBinding.getAttribute());
				Object value = mediator.extractFromResultSet(rs, aliasBinding.getAlias(), aliasBinding.getAttribute(), recordContext);
				record.setAttributeValue(aliasBinding.getAttribute().getName(), value);
			}
		}
		Map<String, List<MappingBinding>> subBindingsByAttributeName = binding.getSubBindings();
		if (subBindingsByAttributeName != null) {
			for (Map.Entry<String, List<MappingBinding>> entry : subBindingsByAttributeName.entrySet()) {
				String attributeName = entry.getKey();
				List<MappingBinding> list = entry.getValue();
				for (MappingBinding mappingBinding : list) {
					Record r = evaluateBinding(mappingBinding, rs);
					if (r != null) {
						record.setAttributeValue(attributeName, r);
						break;
					}
				}
				if (!record.isAttributePresent(attributeName)) {
					record.setAttributeValue(attributeName, null);
				}
			}
		}
	}

}
