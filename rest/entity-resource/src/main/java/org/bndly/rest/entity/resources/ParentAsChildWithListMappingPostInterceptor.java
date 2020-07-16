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

import org.bndly.common.mapper.MappedProperty;
import org.bndly.common.mapper.MappingContext;
import org.bndly.common.mapper.MappingState;
import org.bndly.common.mapper.NoOpMappingListener;
import org.bndly.schema.api.Record;
import org.bndly.schema.beans.ActiveRecord;
import org.bndly.schema.beans.SchemaBeanFactory;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.InverseAttribute;
import org.bndly.schema.model.Mixin;
import org.bndly.schema.model.NamedAttributeHolder;
import org.bndly.schema.model.NamedAttributeHolderAttribute;
import org.bndly.schema.model.Type;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ParentAsChildWithListMappingPostInterceptor extends NoOpMappingListener implements MappingContext.Listener {

	private final SchemaBeanFactory schemaBeanFactory;

	public ParentAsChildWithListMappingPostInterceptor(SchemaBeanFactory schemaBeanFactory) {
		if (schemaBeanFactory == null) {
			throw new IllegalArgumentException("schemaBeanFactory is not allowed to be null");
		}
		this.schemaBeanFactory = schemaBeanFactory;
	}
	
	@Override
	public void afterMapping(Object source, Object target, Class<?> outputType, MappingState mappingState) {
		if (mappingState.mapsCollection()) {
			// inspect the stack
			// if a child is a parent in the xml tree, then add it to the child list
			Object targetList = mappingState.getTarget();
			MappingState parentState = mappingState.getParent();
			if (parentState == null) {
				return;
			}
			final MappedProperty listTargetProperty = parentState.getTargetProperty();
			if (listTargetProperty == null) {
				return;
			}
			final Object targetBean = parentState.getTarget();
			if (!ActiveRecord.class.isInstance(targetBean)) {
				return;
			}
			final Record listParentRecord = schemaBeanFactory.getRecordFromSchemaBean(targetBean);
			if (listParentRecord == null) {
				return;
			}
			if (listParentRecord.isAttributeDefined(listTargetProperty.getName())) {
				Attribute att = listParentRecord.getAttributeDefinition(listTargetProperty.getName());
				if (InverseAttribute.class.isInstance(att)) {
					InverseAttribute inverseAttribute = (InverseAttribute) att;
					// currently mapping to a collection of an inverse attribute
					MappingState tmp = parentState.getParent();
					if (tmp != null) {
						tmp = tmp.getParent();
					}
					if (tmp != null) {
						tmp = tmp.getParent();
					}
					if (tmp != null) {
						final Object childAsParent = tmp.getTarget();
						final MappedProperty childTargetProperty = tmp.getTargetProperty();
						if (childTargetProperty != null) {
							if (ActiveRecord.class.isInstance(childAsParent)) {
								final Record childRecord = schemaBeanFactory.getRecordFromSchemaBean(childAsParent);
								if (childRecord != null) {
									if (childRecord.isAttributeDefined(childTargetProperty.getName())) {
										Attribute childAtt = childRecord.getAttributeDefinition(childTargetProperty.getName());
										if (NamedAttributeHolderAttribute.class.isInstance(childAtt)) {
											NamedAttributeHolderAttribute attributeHolderAttribute = (NamedAttributeHolderAttribute) childAtt;
											NamedAttributeHolder referencedAttributeHolder = inverseAttribute.getReferencedAttributeHolder();
											if (isNamedAttributeHolder(childRecord, referencedAttributeHolder)) {
												if (attributeHolderAttribute.getName().equals(inverseAttribute.getReferencedAttributeName())) {
													// add childRecord to list
													if (Collection.class.isInstance(targetList)) {
														((Collection) targetList).add(childRecord);
													}
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}
	
	private boolean isNamedAttributeHolder(Record record, NamedAttributeHolder namedAttributeHolder) {
		if (Type.class.isInstance(namedAttributeHolder)) {
			return isNamedAttributeHolder(record, (Type) namedAttributeHolder);
		} else if (Mixin.class.isInstance(namedAttributeHolder)) {
			return isNamedAttributeHolder(record, (Mixin) namedAttributeHolder);
		} else {
			throw new IllegalStateException("unsupported attribute holder");
		}
	}
	
	private boolean isNamedAttributeHolder(Record record, Type type) {
		Type ct = null;
		do {		
			if (ct == null) {
				ct = record.getType();
			} else {
				ct = ct.getSuperType();
			}
			if (ct == type) {
				return true;
			}
		} while (ct != null && ct != type);
		return false;
	}
	
	private boolean isNamedAttributeHolder(Record record, Mixin mixin) {
		Type ct = null;
		do {
			if (ct == null) {
				ct = record.getType();
			} else {
				ct = ct.getSuperType();
			}
			if (ct != null) {
				List<Mixin> mixins = ct.getMixins();
				if (mixins != null) {
					for (Mixin m : mixins) {
						if (m == mixin) {
							return true;
						}
					}
				}
				return true;
			}
		} while (ct != null);
		return false;
	}

}
