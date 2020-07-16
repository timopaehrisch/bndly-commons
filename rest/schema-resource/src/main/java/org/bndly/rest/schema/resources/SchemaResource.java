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
import org.bndly.common.json.model.JSArray;
import org.bndly.common.json.model.JSBoolean;
import org.bndly.common.json.model.JSMember;
import org.bndly.common.json.model.JSNull;
import org.bndly.common.json.model.JSNumber;
import org.bndly.common.json.model.JSObject;
import org.bndly.common.json.model.JSString;
import org.bndly.common.json.model.JSValue;
import org.bndly.rest.atomlink.api.annotation.AtomLink;
import org.bndly.rest.atomlink.api.annotation.AtomLinks;
import org.bndly.rest.common.beans.Services;
import org.bndly.rest.controller.api.GET;
import org.bndly.rest.controller.api.Path;
import org.bndly.rest.controller.api.Response;
import org.bndly.rest.schema.beans.AttributeBean;
import org.bndly.rest.schema.beans.AttributesList;
import org.bndly.rest.schema.beans.BinaryAttributeBean;
import org.bndly.rest.schema.beans.BooleanAttributeBean;
import org.bndly.rest.schema.beans.CryptoAttributeBean;
import org.bndly.rest.schema.beans.DateAttributeBean;
import org.bndly.rest.schema.beans.DecimalAttributeBean;
import org.bndly.rest.schema.beans.InverseAttributeBean;
import org.bndly.rest.schema.beans.JSONAttributeBean;
import org.bndly.rest.schema.beans.MixinAttributeBean;
import org.bndly.rest.schema.beans.MixinBean;
import org.bndly.rest.schema.beans.NamedAttributeHolderBean;
import org.bndly.rest.schema.beans.SchemaBean;
import org.bndly.rest.schema.beans.SchemaList;
import org.bndly.rest.schema.beans.StringAttributeBean;
import org.bndly.rest.schema.beans.TypeAttributeBean;
import org.bndly.rest.schema.beans.TypeBean;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.RecordContext;
import org.bndly.schema.api.services.Engine;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.BinaryAttribute;
import org.bndly.schema.model.BooleanAttribute;
import org.bndly.schema.model.CryptoAttribute;
import org.bndly.schema.model.DateAttribute;
import org.bndly.schema.model.DecimalAttribute;
import org.bndly.schema.model.InverseAttribute;
import org.bndly.schema.model.JSONAttribute;
import org.bndly.schema.model.Mixin;
import org.bndly.schema.model.MixinAttribute;
import org.bndly.schema.model.NamedAttributeHolder;
import org.bndly.schema.model.Schema;
import org.bndly.schema.model.SchemaUtil;
import org.bndly.schema.model.StringAttribute;
import org.bndly.schema.model.Type;
import org.bndly.schema.model.TypeAttribute;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("schema")
public class SchemaResource {

	private static final Logger LOG = LoggerFactory.getLogger(SchemaResource.class);
	
	private final ConversionContext conversionContext;
	private final List<SchemaBoundResource> resources;
	private final ReadWriteLock resourcesLock;

	public SchemaResource(ConversionContext conversionContext, List<SchemaBoundResource> resources, ReadWriteLock resourcesLock) {
		this.conversionContext = conversionContext;
		this.resources = resources;
		this.resourcesLock = resourcesLock;
	}
	
	@GET
	@AtomLinks({
		@AtomLink(rel = "schema", target = Services.class),
		@AtomLink(target = SchemaList.class)
	})
	public Response getSchemas() {
		SchemaList schemaList = new SchemaList();
		resourcesLock.readLock().lock();
		try {
			for (SchemaBoundResource schemaBoundResource : resources) {
				Engine engine = schemaBoundResource.getEngine();
				Schema schema = engine.getDeployer().getDeployedSchema();
				SchemaBean schemaBean = new SchemaBean();
				if (schema != null) {
					schemaBean.setName(schema.getName());
					schemaBean.setNamespace(schema.getNamespace());
					schemaBean.setMixins(new ArrayList<MixinBean>());
					if (schema.getMixins() != null) {
						for (Mixin mixin : schema.getMixins()) {
							schemaBean.getMixins().add(mapMixin(mixin));
						}
					}
					schemaBean.setTypes(new ArrayList<TypeBean>());
					if (schema.getTypes() != null) {
						for (Type type : schema.getTypes()) {
							schemaBean.getTypes().add(mapType(type));
						}
					}
					schemaList.add(schemaBean);
				}
			}
		} finally {
			resourcesLock.readLock().unlock();
		}
		return Response.ok(schemaList);
	}
	
	private AttributesList mapAttributes(List<Attribute> attributes) {
		AttributesList l = new AttributesList();
		if (attributes != null) {
			for (Attribute attribute : attributes) {
				l.add(mapAttribute(attribute));
			}
		}
		return l;
	}

	private AttributeBean mapAttribute(Attribute a) {
		AttributeBean bean;
		if (StringAttribute.class.isInstance(a)) {
			StringAttribute att = (StringAttribute) a;
			StringAttributeBean tmp = new StringAttributeBean();
			tmp.setLength(att.getLength());
			bean = tmp;
		} else if (DecimalAttribute.class.isInstance(a)) {
			DecimalAttribute att = (DecimalAttribute) a;
			DecimalAttributeBean tmp = new DecimalAttributeBean();
			tmp.setLength(att.getLength());
			tmp.setDecimalPlaces(att.getDecimalPlaces());
			bean = tmp;
		} else if (BooleanAttribute.class.isInstance(a)) {
			BooleanAttribute att = (BooleanAttribute) a;
			BooleanAttributeBean tmp = new BooleanAttributeBean();
			bean = tmp;
		} else if (CryptoAttribute.class.isInstance(a)) {
			CryptoAttribute att = (CryptoAttribute) a;
			CryptoAttributeBean tmp = new CryptoAttributeBean();
			bean = tmp;
		} else if (DateAttribute.class.isInstance(a)) {
			DateAttribute att = (DateAttribute) a;
			DateAttributeBean tmp = new DateAttributeBean();
			bean = tmp;
		} else if (TypeAttribute.class.isInstance(a)) {
			TypeAttribute att = (TypeAttribute) a;
			TypeAttributeBean tmp = new TypeAttributeBean();
			TypeBean t = new TypeBean();
			t.setName(att.getNamedAttributeHolder().getName());
			tmp.setNamedAttributeHolderBean(t);
			bean = tmp;
		} else if (MixinAttribute.class.isInstance(a)) {
			MixinAttribute att = (MixinAttribute) a;
			MixinAttributeBean tmp = new MixinAttributeBean();
			MixinBean m = new MixinBean();
			m.setName(att.getNamedAttributeHolder().getName());
			tmp.setNamedAttributeHolderBean(m);
			bean = tmp;
		} else if (JSONAttribute.class.isInstance(a)) {
			JSONAttribute att = (JSONAttribute) a;
			JSONAttributeBean tmp = new JSONAttributeBean();
			tmp.setNamedAttributeHolder(simpleMapNamedAttributeHolder(att.getNamedAttributeHolder()));
			tmp.setAsByteArray(att.getAsByteArray());
			bean = tmp;
		} else if (BinaryAttribute.class.isInstance(a)) {
			BinaryAttribute att = (BinaryAttribute) a;
			BinaryAttributeBean tmp = new BinaryAttributeBean();
			tmp.setAsByteArray(att.getAsByteArray());
			bean = tmp;
		} else if (InverseAttribute.class.isInstance(a)) {
			InverseAttribute att = (InverseAttribute) a;
			InverseAttributeBean tmp = new InverseAttributeBean();
			NamedAttributeHolder holder = att.getReferencedAttributeHolder();
			NamedAttributeHolderBean m = simpleMapNamedAttributeHolder(holder);
			tmp.setReferencedNamedAttributeHolder(m);
			tmp.setReferencedAttributeName(att.getReferencedAttributeName());
			bean = tmp;
		} else {
			throw new IllegalStateException("unsupported attribute type: " + a.getClass().getName());
		}
		bean.setName(a.getName());
		return bean;
	}

	private NamedAttributeHolderBean simpleMapNamedAttributeHolder(NamedAttributeHolder holder) {
		NamedAttributeHolderBean m;
		if (Mixin.class.isInstance(holder)) {
			m = new MixinBean();
		} else if (Type.class.isInstance(holder)) {
			m = new TypeBean();
		} else {
			throw new IllegalStateException("unsupported named attribute holder");
		}
		m.setName(holder.getName());
		return m;
	}

	private TypeBean mapType(Type type) {
		TypeBean t = new TypeBean();
		t.setSchemaName(type.getSchema().getName());
		t.setName(type.getName());
		t.setAttributes(mapAttributes(type.getAttributes()));
		t.setIsAbstract(type.isAbstract());
		if (type.getSuperType() != null) {
			t.setParentTypeName(type.getSuperType().getName());
		}
		List<Mixin> m = type.getMixins();
		if (m != null && !m.isEmpty()) {
			t.setMixins(new ArrayList<String>());
			for (Mixin mixin : m) {
				t.getMixins().add(mixin.getName());
			}
		}
		return t;
	}

	private MixinBean mapMixin(Mixin mixin) {
		MixinBean t = new MixinBean();
		t.setSchemaName(mixin.getSchema().getName());
		t.setName(mixin.getName());
		t.setAttributes(mapAttributes(mixin.getAttributes()));
		return t;
	}

	private JSObject mapRecordToJSObject(Record record, Set<MarshalledRecord> marshallingStack) {
		if (record == null) {
			return null;
		}
		JSObject o = new JSObject();
		MarshalledRecord mr = new MarshalledRecord(record, o);
		if (!marshallingStack.contains(mr)) {
			marshallingStack.add(mr);
			List<Attribute> atts = SchemaUtil.collectAttributes(record.getType());
			for (Attribute attribute : atts) {
				JSMember m = createMemberWithNameAndValue(attribute.getName(), record.getAttributeValue(attribute.getName()), marshallingStack);
				addMemberToJSObject(o, m);
			}
			marshallingStack.remove(mr);
		} else {
			addMemberToJSObject(o, createMemberWithNameAndValue("__isReference", Boolean.TRUE, marshallingStack));
		}
		addMemberToJSObject(o, createMemberWithNameAndValue("id", record.getId(), marshallingStack));
		addMemberToJSObject(o, createMemberWithNameAndValue("__type", record.getType().getName(), marshallingStack));
		return o;
	}

	private JSValue mapValueToJSValue(Object attributeValue, Set<MarshalledRecord> marshallingStack) {
		if (attributeValue == null) {
			return new JSNull();
		}
		JSValue v = conversionContext.serialize(attributeValue.getClass(), attributeValue);
		if (v == null) {
			if (Record.class.isInstance(attributeValue)) {
				return mapRecordToJSObject((Record) attributeValue, marshallingStack);
			} else if (Collection.class.isInstance(attributeValue)) {
				Collection c = Collection.class.cast(attributeValue);
				JSArray array = new JSArray();
				for (Object object : c) {
					List<JSValue> items = array.getItems();
					if (items == null) {
						items = new ArrayList<>();
						array.setItems(items);
					}
					JSValue oJSValue = mapValueToJSValue(object, marshallingStack);
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

	private void addMemberToJSObject(JSObject o, JSMember m) {
		if (m != null) {
			if (o.getMembers() == null) {
				o.setMembers(new LinkedHashSet<JSMember>());
			}
			o.getMembers().add(m);
		}
	}

	private JSMember createMemberWithNameAndValue(String name, Object value, Set<MarshalledRecord> marshallingStack) {
		JSMember m = new JSMember();
		m.setName(new JSString(name));
		JSValue mv = mapValueToJSValue(value, marshallingStack);
		if (mv == null) {
			return null;
		}
		m.setValue(mv);
		return m;
	}

	private void mapJSONToRecord(JSObject json, Record r, RecordContext ctx) {
		Set<JSMember> m = json.getMembers();
		if (m != null) {
			for (JSMember jSMember : m) {
				mapJSMemberToRecord(jSMember, r, ctx);
			}
		}
	}

	private void mapJSMemberToRecord(JSMember jSMember, Record r, RecordContext ctx) {
		String key = jSMember.getName().getValue();
		if ("id".equals(key) || "__type".equals(key)) {
			return;
		}
		Object value = mapJSValueToValue(jSMember.getValue(), ctx);
		if ("__isReference".equals(key)) {
			if (value != null && Boolean.class.isInstance(value)) {
				r.setIsReference((boolean) value);
			}
			return;
		}
		r.setAttributeValue(key, value);
	}

	private Object mapJSValueToValue(JSValue value, RecordContext ctx) {
		if (JSNull.class.isInstance(value)) {
			return null;
		} else if (JSNumber.class.isInstance(value)) {
			return JSNumber.class.cast(value).getValue();
		} else if (JSBoolean.class.isInstance(value)) {
			return JSBoolean.class.cast(value).isValue();
		} else if (JSString.class.isInstance(value)) {
			return JSString.class.cast(value).getValue();
		} else if (JSArray.class.isInstance(value)) {
			JSArray a = JSArray.class.cast(value);
			List<Object> l = new ArrayList<>();
			List<JSValue> i = a.getItems();
			if (i != null) {
				for (JSValue jSValue : i) {
					l.add(mapJSValueToValue(jSValue, ctx));
				}
			}
			return l;
		} else if (JSObject.class.isInstance(value)) {
			return mapJSObjectToRecord(JSObject.class.cast(value), ctx);
		} else {
			throw new IllegalStateException("unsupported value type: " + value.getClass().getSimpleName());
		}
	}

	private Record mapJSObjectToRecord(JSObject object, RecordContext ctx) {
		Set<JSMember> m = object.getMembers();
		if (m != null) {
			JSMember idM = null;
			JSMember typeM = null;
			for (JSMember jSMember : m) {
				if ("id".equals(jSMember.getName().getValue())) {
					idM = jSMember;
				} else if ("__type".equals(jSMember.getName().getValue())) {
					typeM = jSMember;
				}
			}
			if (idM != null && typeM != null) {
				BigDecimal id = (BigDecimal) mapJSValueToValue(idM.getValue(), ctx);
				String typeName = (String) mapJSValueToValue(typeM.getValue(), ctx);
				Record r = ctx.create(typeName, id.longValue());
				mapJSONToRecord(object, r, ctx);
				return r;
			}
		}
		return null;
	}

}
