package org.bndly.schema.beans;

/*-
 * #%L
 * Schema Beans
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

import org.bndly.schema.json.beans.StreamingObject;
import org.bndly.schema.json.beans.JSONSchemaBeanFactory;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.RecordContext;
import org.bndly.schema.api.SchemaBeanProvider;
import org.bndly.schema.api.db.TypeTable;
import org.bndly.schema.api.services.Engine;
import org.bndly.schema.beans.impl.Invoker;
import org.bndly.schema.json.beans.JSONSchemaBeanInvocationHandler;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.Mixin;
import org.bndly.schema.model.NamedAttributeHolder;
import org.bndly.schema.model.Schema;
import org.bndly.schema.model.SimpleAttribute;
import org.bndly.schema.model.Type;
import org.bndly.schema.model.UniqueConstraint;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class SchemaBeanFactory {

	private JSONSchemaBeanFactory jsonSchemaBeanFactory;
	private Engine engine;
	private final Map<String, Class<?>> defaultTypeBindings = new HashMap<>();
	private final Map<Class<?>, String> defaultSchemaBeanTypeBindings = new HashMap<>();
	private final SchemaBeanProvider schemaBeanProvider;
	private final Map<Method, Invoker> invokerMap = new HashMap<>();

	public SchemaBeanFactory(SchemaBeanProvider schemaBeanProvider) {
		if (schemaBeanProvider == null) {
			throw new IllegalArgumentException("schemaBeanProvider is not allowed to be null");
		}
		this.schemaBeanProvider = schemaBeanProvider;
	}

	public Map<Method, Invoker> getInvokerMap() {
		return invokerMap;
	}

	public <E> E convertToActiveRecord(E input) {
		E output = null;
		if (StreamingObject.class.isInstance(input)) {
			StreamingObject so = (StreamingObject) input;
			Long id = so.getId();
			if (id != null) {
				output = (E) getSchemaBean(so.getSchemaBeanInterface(), id);
			} else {
				String typeName = so.getSchemaBeanTypeName();
				Schema ds = engine.getDeployer().getDeployedSchema();
				if (ds != null) {
					JSONSchemaBeanInvocationHandler invocationHandler = jsonSchemaBeanFactory.getInvocationHandler(input);
					for (UniqueConstraint uniqueConstraint : ds.getUniqueConstraints()) {
						if (uniqueConstraintAppliesToType(uniqueConstraint, typeName)) {
							// try query by example
							List<Attribute> atts = uniqueConstraint.getAttributes();
							StringBuffer sb = null;
							ArrayList<Object> args = new ArrayList<>();
							for (Attribute att : atts) {
								if (SimpleAttribute.class.isInstance(att)) {
									Object v = invocationHandler.getAttributeValue(att.getName());
									if (v != null) {
										if (sb == null) {
											sb = new StringBuffer();
										} else {
											sb.append(" AND ");
										}
										sb.append(att.getName()).append("=?");
										args.add(v);
									}
								}
							}
							if (sb != null) {
								args.add(1);
								String query = new StringBuffer("PICK ").append(typeName).append(" IF ").append(sb).append(" LIMIT ?").toString();
								Iterator<Record> res = engine.getAccessor().query(query, args.toArray());
								if (res.hasNext()) {
									Record rec = res.next();
									output = (E) getSchemaBean(rec);
									break;
								}
							}
						}
					}
				}
			}
		} else if (ActiveRecord.class.isInstance(input)) {
			output = input;
		}
		return output;
	}

	private boolean uniqueConstraintAppliesToType(UniqueConstraint uc, NamedAttributeHolder namedAttributeHolder) {
		if (uc.getHolder() == namedAttributeHolder) {
			return true;
		}
		if (Type.class.isInstance(namedAttributeHolder)) {
			Type type = (Type) namedAttributeHolder;
			List<Mixin> mixins = type.getMixins();
			if (mixins != null) {
				for (Mixin mixin : mixins) {
					if (uniqueConstraintAppliesToType(uc, mixin)) {
						return true;
					}
				}
			}
			Type superType = type.getSuperType();
			if (superType != null && uniqueConstraintAppliesToType(uc, superType)) {
				return true;
			}
		}
		return false;
	}

	private boolean uniqueConstraintAppliesToType(UniqueConstraint uc, String typeName) {
		TypeTable tt = engine.getTableRegistry().getTypeTableByType(typeName);
		if (tt == null) {
			return false;
		}
		Type type = tt.getType();
		return uniqueConstraintAppliesToType(uc, type);
	}

	public <E> E getEclipseChain(Class<E> type, E... entries) {
		return getEclipseChain(false, type, entries);
	}

	public <E> E getEclipseChainWithNullSkipping(Class<E> type, E... entries) {
		return getEclipseChain(true, type, entries);
	}

	private <E> E getEclipseChain(boolean skipNull, Class<E> type, E... entries) {
		SchemaBeanInvocationHandler[] records = new SchemaBeanInvocationHandler[entries.length];
		for (int i = 0; i < entries.length; i++) {
			E e = entries[i];
			records[i] = getInvocationHandlerFromProxy(e);
		}
		return (E) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, new EclipsedSchemaBeanInvocationHandler(skipNull, records));
	}

	public <E> E getEclipseChain(Class<E> type, Record... records) {
		return getEclipseChain(false, type, records);
	}

	public <E> E getEclipseChainWithNullSkipping(Class<E> type, Record... records) {
		return getEclipseChain(true, type, records);
	}

	private <E> E getEclipseChain(boolean skipNull, Class<E> type, Record... records) {
		ArrayList<SchemaBeanInvocationHandler> entries = new ArrayList<>(records.length);
		for (int i = 0; i < records.length; i++) {
			if (records[i] != null) {
				E sb = getSchemaBean(type, records[i]);
				entries.add(getInvocationHandlerFromProxy(sb));
			}
		}
		SchemaBeanInvocationHandler[] tmp = entries.toArray(new SchemaBeanInvocationHandler[entries.size()]);
		return (E) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type}, new EclipsedSchemaBeanInvocationHandler(skipNull, tmp));
	}

	@Deprecated
	private <E> E getEmptySchemaBeanAsReference(Class<E> type) {
		return getEmptySchemaBean(type, true);
	}

	@Deprecated
	private Object getEmptySchemaBean(String typeName) {
		Class<?> type = getTypeBindingForType(typeName);
		if (type == null) {
			throw new IllegalArgumentException("could not find type binding for typeName " + typeName);
		}
		return getEmptySchemaBean(type, false);
	}

	private <E> E getEmptySchemaBean(Class<E> type) {
		return getEmptySchemaBean(type, false);
	}

	private <E> E getEmptySchemaBean(Class<E> type, boolean asReference) {
		RecordContext ctx = engine.getAccessor().buildRecordContext();
		Record r = ctx.create(type.getSimpleName());
		r.setIsReference(asReference);
		return getSchemaBean(type, r);
	}

	private <E> E getEmptySchemaBeanAsReference(Class<E> type, long id) {
		return _getEmptySchemaBean(type, id, true);
	}

	private <E> E getEmptySchemaBean(Class<E> type, long id) {
		return _getEmptySchemaBean(type, id, false);
	}

	private <E> E _getEmptySchemaBean(Class<E> type, long id, boolean asReference) {
		RecordContext ctx = engine.getAccessor().buildRecordContext();
		Record r = ctx.create(type.getSimpleName(), id);
		r.setIsReference(asReference);
		return getSchemaBean(type, r);
	}

	public Iterable<Object> getSchemaBeans(final Iterable<Record> records) {
		return new Iterable<Object>() {
			@Override
			public Iterator<Object> iterator() {
				final Iterator<Record> iter = records.iterator();
				return new Iterator<Object>() {
					@Override
					public boolean hasNext() {
						return iter.hasNext();
					}

					@Override
					public Object next() {
						return getSchemaBean(iter.next());
					}

					@Override
					public void remove() {
						iter.remove();
					}
				};
			}
		};
	}
	
	public <E> Iterable<E> getSchemaBeans(final Iterable<Record> records, final Class<E> type) {
		return new Iterable<E>() {
			@Override
			public Iterator<E> iterator() {
				final Iterator<Record> iter = records.iterator();
				return new Iterator<E>() {
					@Override
					public boolean hasNext() {
						return iter.hasNext();
					}

					@Override
					public E next() {
						return getSchemaBean(type, iter.next());
					}

					@Override
					public void remove() {
						iter.remove();
					}
				};
			}
		};
	}
	
	public <E> List<E> getSchemaBeans(Collection<Record> records, Class<E> type) {
		List<E> l = new ArrayList<>();
		for (Record record : records) {
			l.add(getSchemaBean(type, record));
		}
		return l;
	}

	public List<Object> getSchemaBeans(Collection<Record> records) {
		List<Object> l = new ArrayList<>();
		for (Record record : records) {
			l.add(getSchemaBean(record));
		}
		return l;
	}

	public Class<?> getTypeBindingForType(Record r) {
		return getTypeBindingForType(r.getType());
	}

	public Class<?> getTypeBindingForType(String typeName) {
		Class<?> t = defaultTypeBindings.get(typeName);
		if (t == null) {
			throw new MissingTypeBindingException(typeName, "no type binding registered for schema type " + typeName);
		}
		return t;
	}

	public Class<?> getTypeBindingForType(Type type) {
		return getTypeBindingForType(type.getName());
	}

	private Object getSchemaBean(String typeName, Long id) {
		Class<?> beanType = getTypeBindingForType(typeName);
		if (id != null) {
			return getSchemaBean(beanType, id);
		} else {
			return getEmptySchemaBean(beanType);
		}
	}
	
	public <E> E newInstance(Class<E> type, RecordContext recordContext) {
		Record record = recordContext.create(type.getSimpleName());
		return getSchemaBean(type, record);
	}
	
	public Object newInstance(String typeName, RecordContext recordContext) {
		Record record = recordContext.create(typeName);
		return getSchemaBean(record);
	}

	public Object getSchemaBean(Record record) {
		return getSchemaBean(getTypeBindingForType(record.getType()), record);
	}
	
	public <E> E getSchemaBean(Class<E> type, Record record) {
		assertTypeIsInterface(type);
		Class<?> boundType = getTypeBindingForType(record);
		// switch the provided type from the parameters to the real bound type, if this can be done
		if (type.isAssignableFrom(boundType)) {
			type = (Class) boundType;
		}
		Object proxy = Proxy.newProxyInstance(
				type.getClassLoader(),
				new Class[]{type, ActiveRecord.class},
				new SchemaBeanInvocationHandler(record, this, jsonSchemaBeanFactory, engine, invokerMap)
		);
		return type.cast(proxy);
	}

	private <E> E getSchemaBean(Class<E> type, long id) {
		assertTypeIsInterface(type);
		Record record = engine.getAccessor().readById(type.getSimpleName(), id, engine.getAccessor().buildRecordContext());
		return getSchemaBean(type, record);
	}

	private void assertTypeIsInterface(Class<?> type) throws IllegalArgumentException {
		if (!type.isInterface()) {
			throw new InvalidSchemaBeanTypeException(type, "schema beans can only be created on interfaces.");
		}
	}

	public boolean isSchemaBeanType(Class<?> schemaBeanType) {
		return defaultSchemaBeanTypeBindings.containsKey(schemaBeanType);
	}

	public boolean isSchemaBean(Object schemaBean) {
		if (schemaBean == null) {
			throw new IllegalArgumentException("can not check if null is a schema bean");
		}
		if (Proxy.isProxyClass(schemaBean.getClass())) {
			InvocationHandler ih = Proxy.getInvocationHandler(schemaBean);
			if (SchemaBeanInvocationHandler.class.isInstance(ih)) {
				return true;
			}
		}
		return false;
	}

	public Record getRecordFromSchemaBean(Object schemaBean) {
		SchemaBeanInvocationHandler ih = getInvocationHandlerFromProxy(schemaBean);
		return ih.getRecord();
	}

	private SchemaBeanInvocationHandler getInvocationHandlerFromProxy(Object schemaBean) {
		if (!isSchemaBean(schemaBean)) {
			throw new InvalidSchemaBeanTypeException(schemaBean.getClass(), "provided object is not a schema bean.");
		}
		InvocationHandler ih = Proxy.getInvocationHandler(schemaBean);
		if (!SchemaBeanInvocationHandler.class.isInstance(ih)) {
			throw new InvalidSchemaBeanTypeException(schemaBean.getClass(), "provided object is a proxy but not a schema bean.");
		}
		return SchemaBeanInvocationHandler.class.cast(ih);
	}

	public void setTypeBindings(List<Class<?>> javaTypes) {
		defaultTypeBindings.clear();
		if (javaTypes != null) {
			for (Class<?> t : javaTypes) {
				registerTypeBinding(t);
			}
		}
	}

	public void registerTypeBindings(Class<?>... javaTypes) {
		if (javaTypes != null) {
			for (Class<?> t : javaTypes) {
				registerTypeBinding(t);
			}
		}
	}

	public void registerTypeBinding(Class<?> javaType) {
		registerTypeBinding(javaType.getSimpleName(), javaType);
	}

	public void registerTypeBinding(String typeName, Class<?> javaType) {
		assertTypeIsInterface(javaType);
		defaultTypeBindings.put(typeName, javaType);
		defaultSchemaBeanTypeBindings.put(javaType, typeName);
	}

	public void setEngine(Engine engine) {
		this.engine = engine;
	}

	public JSONSchemaBeanFactory getJsonSchemaBeanFactory() {
		return jsonSchemaBeanFactory;
	}

	public Engine getEngine() {
		return engine;
	}

	public void setJsonSchemaBeanFactory(JSONSchemaBeanFactory jsonSchemaBeanFactory) {
		this.jsonSchemaBeanFactory = jsonSchemaBeanFactory;
	}

	public SchemaBeanProvider getSchemaBeanProvider() {
		return schemaBeanProvider;
	}

}
