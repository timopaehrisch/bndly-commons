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
import org.bndly.common.json.parsing.JSONParser;
import org.bndly.common.json.serializing.JSONSerializer;
import org.bndly.rest.api.Context;
import org.bndly.rest.api.ResourceURI;
import org.bndly.rest.atomlink.api.annotation.AtomLink;
import org.bndly.rest.atomlink.api.annotation.AtomLinkDescription;
import org.bndly.rest.atomlink.api.annotation.Parameter;
import org.bndly.rest.controller.api.DELETE;
import org.bndly.rest.controller.api.GET;
import org.bndly.rest.controller.api.Meta;
import org.bndly.rest.controller.api.PUT;
import org.bndly.rest.controller.api.Path;
import org.bndly.rest.controller.api.PathParam;
import org.bndly.rest.controller.api.Response;
import org.bndly.rest.descriptor.DefaultAtomLinkDescriptor;
import org.bndly.rest.descriptor.DelegatingAtomLinkDescription;
import org.bndly.rest.schema.beans.NamedAttributeHolderBean;
import org.bndly.rest.schema.beans.SchemaBean;
import org.bndly.schema.api.Pagination;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.RecordContext;
import org.bndly.schema.api.Transaction;
import org.bndly.schema.api.services.Engine;
import org.bndly.schema.api.services.QueryByExample;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.SchemaUtil;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SchemaBoundResource {

	private static final Logger LOG = LoggerFactory.getLogger(SchemaBoundResource.class);
	private final Engine engine;
	private final ConversionContext conversionContext;

	public SchemaBoundResource(Engine engine, ConversionContext conversionContext) {
		this.engine = engine;
		this.conversionContext = conversionContext;
	}

	public Engine getEngine() {
		return engine;
	}
	
	public class SchemaBeanDescriptor extends DefaultAtomLinkDescriptor {

		@Override
		public AtomLinkDescription getAtomLinkDescription(Object controller, Method method, AtomLink atomLink) {
			AtomLinkDescription atomLinkDescription = super.getAtomLinkDescription(controller, method, atomLink);
			final String constraint = "${this.name eq '" + engine.getDeployer().getDeployedSchema().getName() + "'}";
			return new DelegatingAtomLinkDescription(atomLinkDescription) {
				@Override
				public String getConstraint() {
					return constraint;
				}

			};
		}
		
	}
	
	@GET
	@Path("deployment.sql")
	@AtomLink(rel = "deployment", target = SchemaBean.class, descriptor = SchemaBeanDescriptor.class)
	public Response getDeploymentSQL(@PathParam("schemaName") String schemaName) {
		String sql = engine.getDeployer().getDeploymentSQL();
		return Response.ok(sql);
	}

	@GET
	@Path("{holder}")
	@AtomLink(rel = "list", target = NamedAttributeHolderBean.class, parameters = {
		@Parameter(name = "holder", expression = "${this.name}")
	}, allowSubclasses = true, constraint = "${controller.engine.deployer.deployedSchema.name eq this.schemaName}")
	public Response list(@PathParam("schemaName") String schemaName, @PathParam("holder") String holderName, @Meta Context context) throws IOException {
		final ResourceURI uri = context.getURI();
		RecordContext ctx = engine.getAccessor().buildRecordContext();
		QueryByExample qbe = engine.getAccessor().queryByExample(holderName, ctx);
		List<ResourceURI.QueryParameter> params = uri.getParameters();
		if (params != null) {
			for (ResourceURI.QueryParameter param : params) {
				String parameterName = param.getName();
				if (!"pageSize".equals(parameterName) && !"pageStart".equals(parameterName) && !"eager".equals(parameterName)) {
					String value = param.getValue();
					// there might be some type conversion required
					appendRequestParameterToQueryByExample(qbe, parameterName, value, ctx);
				}
			}
		}
		ResourceURI.QueryParameter eagerParameter = uri.getParameter("eager");
		String eager = null;
		if (eagerParameter != null) {
			eager = eagerParameter.getValue();
		}
		if ("true".equals(eager) || eager == null) {
			qbe.eager();
		}
		qbe.pagination(new Pagination() {
			@Override
			public Long getOffset() {
				return getNumericParameter("pageStart", 0L);
			}

			@Override
			public Long getSize() {
				return getNumericParameter("pageSize", 10L);
			}

			private Long getNumericParameter(String parameterName, Long defaultValue) {
				ResourceURI.QueryParameter p = uri.getParameter(parameterName);
				if (p == null) {
					return defaultValue;
				}
				String ps = p.getValue();
				if (ps == null) {
					return defaultValue;
				}
				try {
					return new Long(ps);
				} catch (NumberFormatException e) {
					return defaultValue;
				}
			}
		});

		List<Record> result = qbe.all();
		final JSArray jsonResult = mapRecordsToJSArray(result, new HashSet<MarshalledRecord>());
		return buildJSONResponse(jsonResult);
	}

	/*
	 * there exist three ways to add a nested query attribute
	 * 1. refer to the id of the referred object directly
	 * http://localhost:8080/schema/CartItem?itemToPurchase=3
	 * 
	 * 2. refer to the id of the referred object directly including a type name, 
	 * if the referred attribute is declared to accept mixins or types with 
	 * sub-types.
	 * http://localhost:8080/schema/CartItem?itemToPurchase=Bundle_2
	 * 
	 * 3. concatenate the attribute path through the object graph. all nested 
	 * attributes need to be prefixed with their holders type name!
	 * http://localhost:8080/schema/CartItem?itemToPurchase_Bundle_id=2
	 */
	private void appendRequestParameterToQueryByExample(QueryByExample qbe, String parameterName, Object rawValue, RecordContext ctx) {
		String attributeName = parameterName;
		String nestedObjectTypeName = null;
		Record nestedRecord = null;
		int i = attributeName.indexOf('_');
		if (i > 0) {
			attributeName = attributeName.substring(0, i);
			nestedObjectTypeName = parameterName.substring(i + 1);
		}
		if (nestedObjectTypeName != null) {
			int j = nestedObjectTypeName.indexOf('_');
			if (j > 0) {
				String fieldInNestedObject = nestedObjectTypeName.substring(j + 1);
				nestedObjectTypeName = nestedObjectTypeName.substring(0, j);
				if ("id".equals(fieldInNestedObject)) {
					nestedRecord = ctx.create(nestedObjectTypeName, new Long((String) rawValue));
				} else {
					nestedRecord = ctx.create(nestedObjectTypeName);
					nestedRecord.setAttributeValue(fieldInNestedObject, rawValue);
				}
			} else {
				nestedObjectTypeName = null;
			}
		} else {
			if (String.class.isInstance(rawValue)) {
				String rawString = (String) rawValue;
				int j = rawString.indexOf('_');
				if (j > 0) {
					nestedObjectTypeName = rawString.substring(0, j);
					Long idOfNestedObject = new Long(rawString.substring(j + 1));
					nestedRecord = ctx.create(nestedObjectTypeName, idOfNestedObject);
				}
			}
		}
		if (nestedRecord != null) {
			qbe.attribute(attributeName, nestedRecord);
		} else {
			qbe.attribute(attributeName, rawValue);
		}
	}

	@GET
	@Path("{holder}/{id}")
	public Response read(@PathParam("schemaName") String schemaName, @PathParam("holder") String holderName, @PathParam("id") long id) throws IOException {
		Record r = engine.getAccessor().readById(holderName, id, engine.getAccessor().buildRecordContext());
		JSObject jsObject = mapRecordToJSObject(r, new HashSet<MarshalledRecord>());
		return buildJSONResponse(jsObject);
	}

	@DELETE
	@Path("{holder}/{id}")
	public Response delete(@PathParam("schemaName") String schemaName, @PathParam("holder") String holderName, @PathParam("id") long id) {
		RecordContext ctx = engine.getAccessor().buildRecordContext();
		Record r = ctx.create(holderName, id);
		Transaction tx = engine.getQueryRunner().createTransaction();
		engine.getAccessor().delete(r, tx);
		tx.commit();
		return Response.NO_CONTENT;
	}

	@PUT
	@Path("{holder}/{id}")
	public Response update(@PathParam("schemaName") String schemaName, @PathParam("holder") String holderName, @PathParam("id") long id, @Meta Context context) throws IOException {
		InputStream is = context.getInputStream();
		String encoding = context.getInputEncoding();
		if (encoding == null) {
			encoding = "UTF-8";
		}
		JSObject json = (JSObject) new JSONParser().parse(is, encoding);
		RecordContext ctx = engine.getAccessor().buildRecordContext();
		Record r = ctx.create(holderName, id);
		mapJSONToRecord(json, r, ctx);
		engine.getAccessor().update(r);
		return Response.NO_CONTENT;
	}

	private JSArray mapRecordsToJSArray(List<Record> result, Set<MarshalledRecord> marshallingStack) {
		if (result == null) {
			return null;
		}
		JSArray a = new JSArray();
		for (Record record : result) {
			JSObject o = mapRecordToJSObject(record, marshallingStack);
			if (a.getItems() == null) {
				a.setItems(new ArrayList<JSValue>());
			}
			a.getItems().add(o);
		}
		return a;
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

	private Response buildJSONResponse(final JSValue value) throws IOException {
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		OutputStreamWriter out = new OutputStreamWriter(os, "UTF-8");
		new JSONSerializer().serialize(value, out);
		out.flush();
		return Response.ok(new ByteArrayInputStream(os.toByteArray())).header("Content-Type", "application/json");
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
