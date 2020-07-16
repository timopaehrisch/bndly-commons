package org.bndly.schema.fixtures.impl;

/*-
 * #%L
 * Schema Fixtures
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

import org.bndly.common.json.model.JSBoolean;
import org.bndly.common.json.model.JSMember;
import org.bndly.common.json.model.JSNull;
import org.bndly.common.json.model.JSNumber;
import org.bndly.common.json.model.JSObject;
import org.bndly.common.json.model.JSString;
import org.bndly.common.json.model.JSValue;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.RecordContext;
import org.bndly.schema.api.services.Engine;
import org.bndly.schema.beans.SchemaBeanFactory;
import org.bndly.schema.fixtures.api.FixtureDeploymentException;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.BooleanAttribute;
import org.bndly.schema.model.DateAttribute;
import org.bndly.schema.model.DecimalAttribute;
import org.bndly.schema.model.NamedAttributeHolderAttribute;
import org.bndly.schema.model.StringAttribute;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class NotExistsConditionEvaluator implements ConditionEvaluator {
	
	private static interface VariableAppender {
		void append(String variableName, Object value);
	}

	@Override
	public boolean shouldBePersisted(ConditionContext conditionContext) throws FixtureDeploymentException {
		SchemaBeanFactory schemaBeanFactory = conditionContext.getSchemaBeanFactory();
		Engine engine = schemaBeanFactory.getEngine();
		RecordContext queryRecordContext = engine.getAccessor().buildRecordContext();
		JSValue definition = conditionContext.getDefinition();
		/*
		"_condition": {
			"type": "notExists",
			"values": {
				"flight": {
					"sku": "CY1-2016-07-01"
				},
				"seatNumber": "5_J"
			}
		}
		*/
		JSObject definitionObject;
		JSObject protoValues;
		if (JSObject.class.isInstance(definition)) {
			definitionObject = (JSObject) definition;
			protoValues = definitionObject.getMemberValue("values", JSObject.class);
		} else {
			definitionObject = conditionContext.getEntry();
			protoValues = definitionObject;
		}
		if (protoValues != null) {
			Set<JSMember> valueMembers = protoValues.getMembers();
			if (valueMembers == null || valueMembers.isEmpty()) {
				return true;
			}
			// iterate over the values and put them into the query
			
			final StringBuilder sb = new StringBuilder().append("PICK ").append(conditionContext.getInstanceType()).append(" t IF ");
			final List<Object> parameters = new ArrayList<>();
			VariableAppender va = new VariableAppender() {
				String separator = null;
				@Override
				public void append(String variableName, Object value) {
					if (separator == null) {
						separator = " AND ";
					} else {
						sb.append(separator);
					}
					sb.append(variableName).append("=?");
					parameters.add(value);
				}
			};
			recursiveAppendToQuery("t.", valueMembers, va, queryRecordContext.create(conditionContext.getInstanceType()), conditionContext);
			Iterator<Record> res = engine.getAccessor().query(sb.toString(), queryRecordContext, null, parameters.toArray());
			if (res.hasNext()) {
				conditionContext.onFoundRecord(res.next());
				return false;
			} else {
				return true;
			}
		} else {
			// no values object
			return true;
		}
	}

	private void recursiveAppendToQuery(String prefix, Set<JSMember> valueMembers, VariableAppender variableAppender, Record currentProtoRecord, ConditionContext conditionContext) throws FixtureDeploymentException {
		for (JSMember valueMember : valueMembers) {
			String attributeName = valueMember.getName().getValue();
			if ("_ref".equals(attributeName) && valueMembers.size() == 1) {
				// look up the values via the context
				JSObject entry = conditionContext.getEntry(((JSString)valueMember.getValue()).getValue());
				recursiveAppendToQuery(prefix, entry.getMembers(), variableAppender, currentProtoRecord, conditionContext);
				return;
			}
			if ("_ref".equals(attributeName) || "_type".equals(attributeName) || "_key".equals(attributeName) || "_condition".equals(attributeName)) {
				continue;
			}
			Attribute attributeDefinition = currentProtoRecord.getAttributeDefinition(attributeName);
			if (attributeDefinition.isVirtual()) {
				throw new FixtureDeploymentException("can not use virtual attributes in notExists condition. attribute name: '" + attributeDefinition.getName() + "'");
			}
			JSValue jsAttributeValue = valueMember.getValue();
			if (StringAttribute.class.isInstance(attributeDefinition)) {
				if (JSNull.class.isInstance(jsAttributeValue)) {
					variableAppender.append(prefix + attributeName, null);
				} else if (JSString.class.isInstance(jsAttributeValue)) {
					variableAppender.append(prefix + attributeName, ((JSString) jsAttributeValue).getValue());
				} else {
					throw new FixtureDeploymentException("illegal value for attribute " + attributeName);
				}
			} else if (BooleanAttribute.class.isInstance(attributeDefinition)) {
				if (JSNull.class.isInstance(jsAttributeValue)) {
					variableAppender.append(prefix + attributeName, null);
				} else if (JSBoolean.class.isInstance(jsAttributeValue)) {
					variableAppender.append(prefix + attributeName, ((JSBoolean) jsAttributeValue).isValue());
				} else {
					throw new FixtureDeploymentException("illegal value for attribute " + attributeName);
				}
			} else if (DateAttribute.class.isInstance(attributeDefinition)) {
				if (JSNull.class.isInstance(jsAttributeValue)) {
					variableAppender.append(prefix + attributeName, null);
				} else if (JSNumber.class.isInstance(jsAttributeValue)) {
					variableAppender.append(prefix + attributeName, new Date(((JSNumber) jsAttributeValue).getValue().longValue()));
				} else {
					throw new FixtureDeploymentException("illegal value for attribute " + attributeName);
				}
			} else if (DecimalAttribute.class.isInstance(attributeDefinition)) {
				DecimalAttribute da = (DecimalAttribute) attributeDefinition;
				if (JSNull.class.isInstance(jsAttributeValue)) {
					variableAppender.append(prefix + attributeName, null);
				} else if (JSNumber.class.isInstance(jsAttributeValue)) {
					BigDecimal decimal = ((JSNumber) jsAttributeValue).getValue();
					Integer length = da.getLength();
					Integer dp = da.getDecimalPlaces();
					if (dp == null) {
						dp = 0;
					}
					if (length == null) {
						if (dp == 0) {
							variableAppender.append(prefix + attributeName, decimal.longValue());
						} else {
							variableAppender.append(prefix + attributeName, decimal.doubleValue());
						}
					} else {
						variableAppender.append(prefix + attributeName, decimal);
					}
				} else {
					throw new FixtureDeploymentException("illegal value for attribute " + attributeName);
				}
			} else if (NamedAttributeHolderAttribute.class.isInstance(attributeDefinition)) {
				NamedAttributeHolderAttribute naha = (NamedAttributeHolderAttribute) attributeDefinition;
				// recurse
				if (JSNull.class.isInstance(jsAttributeValue)) {
					variableAppender.append(prefix + attributeName, null);
				} else if (JSObject.class.isInstance(jsAttributeValue)) {
					JSObject nestedValues = (JSObject) jsAttributeValue;
					Set<JSMember> members = nestedValues.getMembers();
					if (members != null && !members.isEmpty()) {
						Record nestedProto = currentProtoRecord.getContext().create(naha.getNamedAttributeHolder().getName());
						recursiveAppendToQuery(prefix + attributeName + ".", members, variableAppender, nestedProto, conditionContext);
					}
				} else {
					throw new FixtureDeploymentException("illegal value for attribute " + attributeName);
				}
			} else {
				throw new FixtureDeploymentException("unsupported type of attribute for notExists condition: " + attributeDefinition.getClass().getName());
			}
		}
	}
	
}
