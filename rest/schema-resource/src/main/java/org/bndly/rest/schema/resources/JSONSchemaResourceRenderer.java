package org.bndly.rest.schema.resources;

/*-
 * #%L
 * REST Schema Resource
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

import org.bndly.common.json.api.ConversionContext;
import org.bndly.common.json.api.ConversionContextBuilder;
import org.bndly.common.json.model.JSArray;
import org.bndly.common.json.model.JSMember;
import org.bndly.common.json.model.JSNull;
import org.bndly.common.json.model.JSObject;
import org.bndly.common.json.model.JSString;
import org.bndly.common.json.model.JSValue;
import org.bndly.common.json.serializing.JSONSerializer;
import org.bndly.rest.api.Context;
import org.bndly.rest.api.Resource;
import org.bndly.rest.api.ResourceRenderer;
import org.bndly.rest.api.ResourceURI;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.RecordAttributeIterator;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.BinaryAttribute;
import org.bndly.schema.model.JSONAttribute;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class JSONSchemaResourceRenderer implements ResourceRenderer {

	private final ConversionContext conversionContext = new ConversionContextBuilder().initDefaults().build();

	@Override
	public boolean supports(Resource resource, Context context) {
		ResourceURI.Extension ext = resource.getURI().getExtension();
		return SchemaResourceInstance.class.isInstance(resource) && ext != null && "json".equals(ext.getName());
	}

	@Override
	public void render(Resource resource, Context context) throws IOException {
		SchemaResourceInstance sri = SchemaResourceInstance.class.cast(resource);
		if (sri.getRecord() != null) {
			JSObject jsObject = mapRecordToJSObject(sri.getRecord(), new HashSet<MarshalledRecord>(), context);
			new JSONSerializer().serialize(jsObject, context.getOutputStream(), "UTF-8");
		}
	}

	private JSObject mapRecordToJSObject(Record record, final Set<MarshalledRecord> marshallingStack, final Context context) {
		if (record == null) {
			return null;
		}
		final JSObject o = new JSObject();
		MarshalledRecord mr = new MarshalledRecord(record, o);
		boolean isUnvisitedRecord = !marshallingStack.contains(mr);
		if (isUnvisitedRecord) {
			marshallingStack.add(mr);
			record.iteratePresentValues(new RecordAttributeIterator() {

				@Override
				public void handleAttribute(Attribute attribute, Record record) {
					JSMember m = createMemberWithNameAndValue(record, attribute, attribute.getName(), record.getAttributeValue(attribute.getName()), marshallingStack, context);
					addMemberToJSObject(o, m);
				}
			});
			marshallingStack.remove(mr);
		}
		if (!isUnvisitedRecord || record.isReference()) {
			addMemberToJSObject(o, createMemberWithNameAndValue(record, null, "__isReference", Boolean.TRUE, marshallingStack, context));
		}
		addMemberToJSObject(o, createMemberWithNameAndValue(record, null, "id", record.getId(), marshallingStack, context));
		addMemberToJSObject(o, createMemberWithNameAndValue(record, null, "__type", record.getType().getName(), marshallingStack, context));
		return o;
	}

	private JSMember createMemberWithNameAndValue(Record r, Attribute attribute, String name, Object value, Set<MarshalledRecord> marshallingStack, Context context) {
		JSMember m = new JSMember();
		m.setName(new JSString(name));
		JSValue mv = mapValueToJSValue(r, attribute, value, marshallingStack, context);
		if (mv == null) {
			return null;
		}
		m.setValue(mv);
		return m;
	}

	private void addMemberToJSObject(JSObject o, JSMember m) {
		if (m != null) {
			if (o.getMembers() == null) {
				o.setMembers(new LinkedHashSet<JSMember>());
			}
			o.getMembers().add(m);
		}
	}

	private JSValue mapValueToJSValue(Record r, Attribute attribute, Object attributeValue, Set<MarshalledRecord> marshallingStack, Context context) {
		if (attributeValue == null) {
			return new JSNull();
		}
		boolean isBinary = BinaryAttribute.class.isInstance(attribute);
		boolean isJSON = JSONAttribute.class.isInstance(attribute);
		if (isBinary && !isJSON && r != null && r.getId() != null) {
			String downloadUriAsString = context.createURIBuilder()
					.pathElement("schema")
					.pathElement(r.getType().getName())
					.pathElement(r.getId().toString())
					.pathElement(attribute.getName())
					.build().asString();
			return new JSString(downloadUriAsString);
		}
		JSValue v = conversionContext.serialize(attributeValue.getClass(), attributeValue);
		if (v == null) {
			if (Record.class.isInstance(attributeValue)) {
				return mapRecordToJSObject((Record) attributeValue, marshallingStack, context);
			} else if (Collection.class.isInstance(attributeValue)) {
				Collection c = Collection.class.cast(attributeValue);
				JSArray array = new JSArray();
				for (Object object : c) {
					List<JSValue> items = array.getItems();
					if (items == null) {
						items = new ArrayList<>();
						array.setItems(items);
					}
					JSValue oJSValue = mapValueToJSValue(r, attribute, object, marshallingStack, context);
					items.add(oJSValue);
				}
				return array;
			} else {
				if (InputStream.class.isInstance(attributeValue) || byte[].class.isInstance(attributeValue)) {
					return null;
				}
				throw new IllegalStateException("unsupported java object while marhsalling: " + attributeValue.getClass().getSimpleName());
			}
		} else {
			return v;
		}
	}
}
