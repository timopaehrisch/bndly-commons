package org.bndly.rest.entity.resources;

/*-
 * #%L
 * REST Entity Resource
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

import org.bndly.common.mapper.DefaultTypeInstanceBuilder;
import org.bndly.common.mapper.MappingState;
import org.bndly.common.mapper.TypeInstanceBuilder;
import org.bndly.common.reflection.GetterBeanPropertyAccessor;
import org.bndly.rest.atomlink.api.annotation.Reference;
import org.bndly.rest.common.beans.AtomLink;
import org.bndly.rest.common.beans.RestBean;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.RecordContext;
import org.bndly.schema.api.services.Accessor;
import org.bndly.schema.beans.ActiveRecord;
import org.bndly.schema.json.beans.JSONSchemaBeanFactory;
import org.bndly.schema.beans.SchemaBeanFactory;
import org.bndly.schema.json.beans.StreamingObject;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.JSONAttribute;
import java.util.HashMap;
import java.util.Map;

public class SchemaBeanTypeInstanceBuilder implements TypeInstanceBuilder {

	private SchemaBeanFactory schemaBeanFactory;
	private JSONSchemaBeanFactory jsonSchemaBeanFactory;
	private final DefaultTypeInstanceBuilder defaultTypeInstanceBuilder = new DefaultTypeInstanceBuilder();
	private final Map<Class<?>, IdFromURLMappingPostInterceptor> idFromURLMappingPostInterceptorsByType = new HashMap<>();

	public void addIdFromURLMappingPostInterceptor(IdFromURLMappingPostInterceptor idFromURLMappingPostInterceptor) {
		idFromURLMappingPostInterceptorsByType.put(idFromURLMappingPostInterceptor.getRestBeanReferenceType(), idFromURLMappingPostInterceptor);
		idFromURLMappingPostInterceptorsByType.put(idFromURLMappingPostInterceptor.getRestBeanType(), idFromURLMappingPostInterceptor);
	}

	private boolean isSchemaBeanType(Class<?> type) {
		return schemaBeanFactory.isSchemaBeanType(type) || jsonSchemaBeanFactory.isSchemaBeanType(type);
	}

	@Override
	public <T> T buildInstance(Class<T> type, MappingState mappingState) {
		if (!isSchemaBeanType(type)) {
			return defaultTypeInstanceBuilder.buildInstance(type, mappingState);
		}
		Record targetRecord = null;
		if (mappingState.getSource() != null) {
			MappingState parentState = mappingState.getParent();
			if (parentState != null) {
				if (parentState.mapsCollection()) {
					parentState = parentState.getParent();
				}
				if (parentState.getTargetProperty() != null && parentState.getTarget() != null) {
					if (ActiveRecord.class.isInstance(parentState.getTarget())) {
						targetRecord = schemaBeanFactory.getRecordFromSchemaBean(parentState.getTarget());
						Attribute schemaAttribute = targetRecord.getAttributeDefinition(parentState.getTargetProperty().getName());
						if (JSONAttribute.class.isInstance(schemaAttribute)) {
							return jsonSchemaBeanFactory.getSchemaBean(type);
						}
					} else if (StreamingObject.class.isInstance(parentState.getTarget())) {
						return jsonSchemaBeanFactory.getSchemaBean(type);
					}
				}
			}
		}
		RecordContext recordContext = targetRecord == null ? null : targetRecord.getContext();

		Accessor accessor = schemaBeanFactory.getEngine().getAccessor();
		Object s = mappingState.getSource();
		boolean isReference = s.getClass().isAnnotationPresent(Reference.class);
		Long id = getIdFromMappingState(mappingState);
		if (id == null) {
			T instance;
			if (recordContext != null) {
				Record record = recordContext.create(type.getSimpleName());
				instance = schemaBeanFactory.getSchemaBean(type, record);
			} else {
				recordContext = accessor.buildRecordContext();
				Record record = recordContext.create(type.getSimpleName());
				instance = schemaBeanFactory.getSchemaBean(type, record);
				if (isReference) {
					record.setIsReference(true);
				}
			}
			return instance;
		} else {
			if (recordContext == null) {
				recordContext = accessor.buildRecordContext();
			}
			final Record entryInContext = recordContext.get(type.getSimpleName(), id);
			Record r;
			if (isReference) {
				if (entryInContext == null) {
					r = recordContext.create(type.getSimpleName(), id);
					r.setIsReference(true);
				} else {
					r = entryInContext;
				}
			} else {
				if (entryInContext == null) {
					r = recordContext.create(type.getSimpleName(), id);
				} else {
					r = entryInContext;
				}
			}
			return schemaBeanFactory.getSchemaBean(type, r);
		}
	}

	public void setSchemaBeanFactory(SchemaBeanFactory schemaBeanFactory) {
		this.schemaBeanFactory = schemaBeanFactory;
		if (schemaBeanFactory != null && jsonSchemaBeanFactory == null) {
			setJsonSchemaBeanFactory(schemaBeanFactory.getJsonSchemaBeanFactory());
		}
	}

	public void setJsonSchemaBeanFactory(JSONSchemaBeanFactory jsonSchemaBeanFactory) {
		this.jsonSchemaBeanFactory = jsonSchemaBeanFactory;
	}

	private Long getIdFromMappingState(MappingState mappingState) {
		Object s = mappingState.getSource();
		if (ActiveRecord.class.isInstance(s)) {
			return ((ActiveRecord) s).getId();
		}
		Long id = (Long) new GetterBeanPropertyAccessor().get("id", s);
		if (id == null) {
			if (RestBean.class.isInstance(s)) {
				AtomLink selfLink = ((RestBean) s).follow("self");
				if (selfLink != null) {
					IdFromURLMappingPostInterceptor idFromURLMappingPostInterceptor = idFromURLMappingPostInterceptorsByType.get(s.getClass());
					if (idFromURLMappingPostInterceptor != null) {
						id = idFromURLMappingPostInterceptor.getIdFromSelfLink(selfLink);
					}
				}
			}
		}
		return id;
	}

}
