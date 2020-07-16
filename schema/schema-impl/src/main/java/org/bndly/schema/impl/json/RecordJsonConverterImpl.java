package org.bndly.schema.impl.json;

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

import org.bndly.common.json.model.JSArray;
import org.bndly.common.json.model.JSBoolean;
import org.bndly.common.json.model.JSMember;
import org.bndly.common.json.model.JSNumber;
import org.bndly.common.json.model.JSObject;
import org.bndly.common.json.model.JSString;
import org.bndly.common.json.model.JSValue;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.RecordAttributeIterator;
import org.bndly.schema.api.RecordContext;
import org.bndly.schema.api.RecordList;
import org.bndly.schema.vendor.mediator.DecimalAttributeMediator;
import org.bndly.schema.json.RecordJsonConverter;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.BooleanAttribute;
import org.bndly.schema.model.DateAttribute;
import org.bndly.schema.model.DecimalAttribute;
import org.bndly.schema.model.InverseAttribute;
import org.bndly.schema.model.JSONAttribute;
import org.bndly.schema.model.NamedAttributeHolderAttribute;
import org.bndly.schema.model.SimpleAttribute;
import org.bndly.schema.model.StringAttribute;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Iterator;
import java.util.Stack;
import org.osgi.service.component.annotations.Component;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = RecordJsonConverter.class, immediate = true)
public class RecordJsonConverterImpl implements RecordJsonConverter {
	@Override
	public Record convertJsonToRecord(JSObject sourceJson, final RecordContext recordContext) {
		String typeName = sourceJson.getMemberStringValue("_type");
		if (typeName == null) {
			return null;
		}
		final Record toMap;
		BigDecimal id = sourceJson.getMemberNumberValue("_id");
		if (id != null) {
			Boolean isRef = sourceJson.getMemberBooleanValue("_isReference");
			final Record found = recordContext.get(typeName, id.longValue());
			if (found != null) {
				return found;
			}
			toMap = recordContext.create(typeName, id.longValue());
			if (isRef != null && isRef) {
				toMap.setIsReference(true);
			}
		} else {
			toMap = recordContext.create(typeName);
		}
		
		Iterator<JSMember> memberIterator = sourceJson.getMembers().iterator();
		while (memberIterator.hasNext()) {
			JSMember next = memberIterator.next();
			String attributeName = next.getName().getValue();
			if ("_type".equals(attributeName) || "_id".equals(attributeName) || "_isReference".equals(attributeName)) {
				continue;
			}
			final JSValue jsValue = next.getValue();
			if (toMap.isAttributeDefined(attributeName)) {
				Attribute attribute = toMap.getAttributeDefinition(attributeName);
				if (JSONAttribute.class.isInstance(attribute) || NamedAttributeHolderAttribute.class.isInstance(attribute)) {
					if (JSObject.class.isInstance(jsValue)) {
						Record rec = convertJsonToRecord((JSObject) jsValue, recordContext);
						if (rec != null) {
							toMap.setAttributeValue(attributeName, rec);
						}
					}
				} else if (SimpleAttribute.class.isInstance(attribute)) {
					if (StringAttribute.class.isInstance(attribute)) {
						if (JSString.class.isInstance(jsValue)) {
							toMap.setAttributeValue(attributeName, ((JSString) jsValue).getValue());
						}
					} else if (DecimalAttribute.class.isInstance(attribute)) {
						if (JSNumber.class.isInstance(jsValue)) {
							Class javaNativeType = DecimalAttributeMediator.getJavaNativeType((DecimalAttribute) attribute);
							BigDecimal value = ((JSNumber) jsValue).getValue();
							if (Long.class.equals(javaNativeType)) {
								toMap.setAttributeValue(attributeName, value.longValue());
							} else if (Double.class.equals(javaNativeType)) {
								toMap.setAttributeValue(attributeName, value.doubleValue());
							} else if (BigDecimal.class.equals(javaNativeType)) {
								toMap.setAttributeValue(attributeName, value);
							}
						}
					} else if (DateAttribute.class.isInstance(attribute)) {
						if (JSNumber.class.isInstance(jsValue)) {
							Date date = new Date(((JSNumber) jsValue).getValue().longValue());
							toMap.setAttributeValue(attributeName, date);
						}
					} else if (BooleanAttribute.class.isInstance(attribute)) {
						if (JSBoolean.class.isInstance(jsValue)) {
							toMap.setAttributeValue(attributeName, ((JSBoolean) jsValue).isValue());
						}
					}
				} else if (InverseAttribute.class.isInstance(attribute)) {
					if (JSArray.class.isInstance(jsValue)) {
						RecordList rl = recordContext.createList(new RecordContext.RecordListInitializer() {

							@Override
							public Iterator<Record> initialize() {
								return new Iterator<Record>() {
									private Iterator<JSValue> iter;
									private JSObject current;

									private Iterator<JSValue> getIter() {
										if (iter == null) {
											iter = ((JSArray) jsValue).iterator();
										}
										return iter;
									}

									@Override
									public boolean hasNext() {
										if (!getIter().hasNext()) {
											return false;
										}
										JSValue val = null;
										while (!JSObject.class.isInstance(val) && getIter().hasNext()) {
											val = getIter().next();
										}
										if (val == null) {
											return false;
										}
										current = (JSObject) val;
										return true;
									}

									@Override
									public Record next() {
										Record currentConverted = convertJsonToRecord(current, recordContext);
										return currentConverted;
									}

									@Override
									public void remove() {
										throw new UnsupportedOperationException("Not supported.");
									}

								};
							}
						}, toMap, (InverseAttribute) attribute);
						toMap.setAttributeValue(attributeName, rl);
					}
				}
			}
		}
		
		return toMap;
	}

	@Override
	public JSObject convertRecordToJson(final JSObject targetObject, final Record sourceRecord) {
		return convertRecordToJson(targetObject, sourceRecord, new Stack<Record>());
	}
	
	private JSObject convertRecordToJson(final JSObject targetObject, final Record sourceRecord, final Stack<Record> visitedRecords) {
		if (visitedRecords.contains(sourceRecord)) {
			return null;
		}
		visitedRecords.add(sourceRecord);

		targetObject.createMember("_type").setValue(new JSString(sourceRecord.getType().getName()));
		Long id = sourceRecord.getId();
		if (id != null) {
			targetObject.createMember("_id").setValue(new JSNumber(id));
		}
		if (sourceRecord.isReference()) {
			targetObject.createMember("_isReference").setValue(JSBoolean.TRUE);
		}

		sourceRecord.iteratePresentValues(new RecordAttributeIterator() {

			@Override
			public void handleAttribute(Attribute attribute, Record record) {
				if (JSONAttribute.class.isInstance(attribute)) {
					Object attributeValue = record.getAttributeValue(attribute.getName());
					if (attributeValue != null) {
						if (Record.class.isInstance(attributeValue)) {
							JSObject nested = new JSObject();
							nested = convertRecordToJson(nested, (Record) attributeValue, visitedRecords);
							if (nested != null) {
								targetObject.createMember(attribute.getName()).setValue(nested);
							}
						} else if (JSValue.class.isInstance(attributeValue)) {
							targetObject.createMember(attribute.getName()).setValue((JSValue) attributeValue);
						}
					}
				} else if (SimpleAttribute.class.isInstance(attribute)) {
					Object attributeValue = record.getAttributeValue(attribute.getName());
					if (attributeValue != null) {
						if (String.class.isInstance(attributeValue)) {
							targetObject.createMember(attribute.getName()).setValue(new JSString((String) attributeValue));
						} else if (Long.class.isInstance(attributeValue)) {
							targetObject.createMember(attribute.getName()).setValue(new JSNumber((Long) attributeValue));
						} else if (Double.class.isInstance(attributeValue)) {
							targetObject.createMember(attribute.getName()).setValue(new JSNumber((Double) attributeValue));
						} else if (BigDecimal.class.isInstance(attributeValue)) {
							targetObject.createMember(attribute.getName()).setValue(new JSNumber((BigDecimal) attributeValue));
						} else if (Date.class.isInstance(attributeValue)) {
							targetObject.createMember(attribute.getName()).setValue(new JSNumber((((Date) attributeValue)).getTime()));
						} else if (Boolean.class.isInstance(attributeValue)) {
							targetObject.createMember(attribute.getName()).setValue(new JSBoolean((Boolean) attributeValue));
						}
					}
				} else if (InverseAttribute.class.isInstance(attribute)) {
					RecordList attributeValue = record.getAttributeValue(attribute.getName(), RecordList.class);
					if (attributeValue != null) {
						Iterator<Record> iter = attributeValue.iterator();
						JSArray array = new JSArray();
						targetObject.createMember(attribute.getName()).setValue(array);
						while (iter.hasNext()) {
							Record next = iter.next();
							JSObject converted = convertRecordToJson(new JSObject(), next, visitedRecords);
							if (converted != null) {
								array.add(converted);
							}
						}
					}
				} else if (NamedAttributeHolderAttribute.class.isInstance(attribute)) {
					Object attributeValue = record.getAttributeValue(attribute.getName());
					if (attributeValue != null) {
						if (Record.class.isInstance(attributeValue)) {
							JSObject nested = new JSObject();
							nested = convertRecordToJson(nested, (Record) attributeValue, visitedRecords);
							if (nested != null) {
								targetObject.createMember(attribute.getName()).setValue(nested);
							}
						} else if (Long.class.isInstance(attributeValue)) {
							targetObject.createMember(attribute.getName()).setValue(new JSNumber((Long) attributeValue));
						}
					}
				}
			}
		});
		
		visitedRecords.remove(sourceRecord);
		return targetObject;
	}
}
