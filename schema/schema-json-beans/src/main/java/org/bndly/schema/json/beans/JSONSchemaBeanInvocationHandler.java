package org.bndly.schema.json.beans;

/*-
 * #%L
 * Schema JSON Beans
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
import org.bndly.common.json.model.JSNull;
import org.bndly.common.json.model.JSNumber;
import org.bndly.common.json.model.JSObject;
import org.bndly.common.json.model.JSString;
import org.bndly.common.json.model.JSValue;
import org.bndly.common.json.parsing.JSONParser;
import org.bndly.common.json.serializing.JSONSerializer;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JSONSchemaBeanInvocationHandler implements InvocationHandler, StreamingObject {

	private JSObject object;
	private final JSONSchemaBeanFactory jsonSchemaBeanFactory;
	private final Class<?> schemaBeanInterface;

	public JSONSchemaBeanInvocationHandler(JSONSchemaBeanFactory jSONSchemaBeanFactory, Class<?> schemaBeanInterface) {
		this(new JSObject(), jSONSchemaBeanFactory, schemaBeanInterface);
	}

	public JSONSchemaBeanInvocationHandler(JSObject object, JSONSchemaBeanFactory jSONSchemaBeanFactory, Class<?> schemaBeanInterface) {
		this.object = object;
		this.jsonSchemaBeanFactory = jSONSchemaBeanFactory;
		this.schemaBeanInterface = schemaBeanInterface;
	}

	@Override
	public JSObject getJSObject() {
		return object;
	}

	private JSMember getMemberByName(String name) {
		return JSONUtil.getMemberByName(name, object);
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		if (method.getDeclaringClass().equals(Object.class)) {
			try {
				return method.invoke(this, args);
			} catch (InvocationTargetException e) {
				throw e.getTargetException();
			}
		}

		if (method.getDeclaringClass().equals(StreamingObject.class)) {
			try {
				return method.invoke(this, args);
			} catch (InvocationTargetException e) {
				throw e.getTargetException();
			}
		}

		String attributeName = null;
		if (method.getName().startsWith("get")) {
			attributeName = getAttributeNameFromMethod(attributeName, method, "get");
			Class<?> returnType = method.getReturnType();
			return getAttributeValue(attributeName, returnType);
		} else if (method.getName().startsWith("set")) {
			attributeName = getAttributeNameFromMethod(attributeName, method, "set");
			Class<?> parameterType = method.getParameterTypes()[0];
			if (List.class.isAssignableFrom(parameterType)) {
				// list of complex objects
				if (args[0] == null) {
					JSONUtil.createNullMember(object, attributeName);
				} else {
					List l = (List) args[0];
					List<JSValue> objects = new ArrayList<>();
					for (Object nestedObject : l) {
						JSObject nestedJSObject = jsonSchemaBeanFactory.getJSObjectFromSchemaBean(nestedObject);
						objects.add(nestedJSObject);
					}
					JSONUtil.createJSArrayMember(object, attributeName, objects);
				}
			} else if (isSimpleType(parameterType)) {
				if (String.class.equals(parameterType)) {
					JSONUtil.createStringMember(object, attributeName, (String) args[0]);
				} else if (BigDecimal.class.equals(parameterType)) {
					JSONUtil.createNumberMember(object, attributeName, (BigDecimal) args[0]);
				} else if (Long.class.equals(parameterType)) {
					JSONUtil.createNumberMember(object, attributeName, (Long) args[0]);
				} else if (Double.class.equals(parameterType)) {
					JSONUtil.createNumberMember(object, attributeName, (Double) args[0]);
				} else if (Date.class.equals(parameterType)) {
					JSONUtil.createDateMember(object, attributeName, (Date) args[0]);
				} else if (Boolean.class.equals(parameterType)) {
					JSONUtil.createBooleanMember(object, attributeName, (Boolean) args[0]);
				} else {
					throw new IllegalStateException("unsupported parameterType: " + parameterType);
				}
			} else if (parameterType.isInterface()) {
				// complex object
				if (args[0] == null) {
					JSONUtil.createNullMember(object, attributeName);
				} else {
					if (!jsonSchemaBeanFactory.isSchemaBean(args[0])) {
						Class<?>[] interfaces = args[0].getClass().getInterfaces();
						for (Class<?> interfaceType : interfaces) {
							if (jsonSchemaBeanFactory.isSchemaBeanType(interfaceType)) {
								// get the values an apply them
								Object newInstance = jsonSchemaBeanFactory.getSchemaBean(interfaceType);
								Method[] methods = interfaceType.getMethods();
								Map<String, Method> methodsByName = new HashMap<>();
								for (Method m : methods) {
									methodsByName.put(m.getName(), m);
								}
								for (Map.Entry<String, Method> entry : methodsByName.entrySet()) {
									String methodName = entry.getKey();
									Method m = entry.getValue();
									if (methodName.startsWith("get")) {
										if (m.getParameterTypes().length == 0) {
											String propertyName = methodName.substring("get".length());
											String setterName = "set" + propertyName;
											Method setter = methodsByName.get(setterName);
											if (setter != null && setter.getParameterTypes().length == 1) {
												Object nestedValue = m.invoke(args[0]);
												setter.invoke(newInstance, nestedValue);
											}
										}
									}
								}
								try {
									Method idGetter = args[0].getClass().getMethod("getId");
									if (idGetter != null) {
										Object id = idGetter.invoke(args[0]);
										if (id != null && Long.class.isInstance(id)) {
											jsonSchemaBeanFactory.getJSObjectFromSchemaBean(newInstance)
													.createMember("id").setValue(new JSNumber((Long) id));
										}
									}
								} catch (Exception e) {
									// bad
								}
								invoke(proxy, method, new Object[]{newInstance});
								break;
							}
						}
					} else {
						JSObject nestedObject = jsonSchemaBeanFactory.getJSObjectFromSchemaBean(args[0]);
						JSONUtil.createJSObjectMember(object, attributeName, nestedObject);
					}
				}
			} else {
				throw new IllegalStateException("unsupported parameterType:" + parameterType.getSimpleName());
			}
			return null;
		}
		throw new IllegalStateException("can not handle methods that do not start with 'get' or 'set'");
	}

	public Object getAttributeValue(String attributeName) {
		String getterMethodName = "get" + attributeName.substring(0, 1).toUpperCase() + attributeName.substring(1);
		try {
			Method method = schemaBeanInterface.getMethod(getterMethodName);
			return getAttributeValue(attributeName, method.getReturnType());
		} catch (NoSuchMethodException | SecurityException ex) {
			return null; // i don't care.
		}
	}

	public <E> E getAttributeValue(String attributeName, Class<E> returnType) {
		JSMember member = getMemberByName(attributeName);
		if (member == null || JSNull.class.isInstance(member.getValue())) {
			return null;
		}

		if (List.class.isAssignableFrom(returnType)) {
			// list of complex objects
			JSArray array = (JSArray) member.getValue();
			List result = new ArrayList();
			for (JSValue jSValue : array) {
				Object nestedProxy = jsonSchemaBeanFactory.getSchemaBean((JSObject) jSValue);
				result.add(nestedProxy);
			}
			return (E) result;
		} else if (isSimpleType(returnType)) {
			if (String.class.equals(returnType)) {
				return (E) ((JSString) member.getValue()).getValue();
			} else if (BigDecimal.class.equals(returnType)) {
				return (E) ((JSNumber) member.getValue()).getValue();
			} else if (Long.class.equals(returnType)) {
				BigDecimal bd = ((JSNumber) member.getValue()).getValue();
				return (E) Long.valueOf(bd.longValue());
			} else if (Double.class.equals(returnType)) {
				BigDecimal bd = ((JSNumber) member.getValue()).getValue();
				return (E) Double.valueOf(bd.doubleValue());
			} else if (Date.class.equals(returnType)) {
				long time = ((JSNumber) member.getValue()).getValue().longValue();
				return (E) new Date(time);
			} else if (Boolean.class.equals(returnType)) {
				return (E) Boolean.valueOf(((JSBoolean) member.getValue()).isValue());
			} else {
				throw new IllegalStateException("unsupported returntype: " + returnType);
			}
		} else if (returnType.isInterface()) {
			// complex object
			JSObject nestedJSObject = (JSObject) member.getValue();
			Object nestedProxy = jsonSchemaBeanFactory.getSchemaBean(nestedJSObject);
			if (returnType.isInstance(nestedProxy)) {
				return (E) nestedProxy;
			} else {
				return jsonSchemaBeanFactory.getSchemaBean(returnType, nestedJSObject);
			}
		} else {
			throw new IllegalStateException("unsupported returnType:" + returnType.getSimpleName());
		}
	}

	protected static String getAttributeNameFromMethod(String attributeName, Method method, String prefix) {
		if (attributeName == null) {
			attributeName = method.getName().substring(prefix.length());
			if (attributeName.length() > 1) {
				attributeName = attributeName.substring(0, 1).toLowerCase() + attributeName.substring(1);
			} else {
				attributeName = attributeName.toLowerCase();
			}
		}
		return attributeName;
	}

	private boolean isSimpleType(Class<?> returnType) {
		return String.class.equals(returnType)
				|| BigDecimal.class.equals(returnType)
				|| Long.class.equals(returnType)
				|| Double.class.equals(returnType)
				|| Date.class.equals(returnType)
				|| Boolean.class.equals(returnType);
	}

	@Override
	public void writeTo(Writer writer) throws IOException {
		new JSONSerializer().serialize(object, writer);
		writer.flush();
	}

	@Override
	public void writeTo(OutputStream os, String encoding) throws IOException {
		new JSONSerializer().serialize(object, os, encoding, false);
		os.flush();
	}

	@Override
	public void loadFrom(Reader reader) throws IOException {
		object = (JSObject) new JSONParser().parse(reader);
	}

	@Override
	public void loadFrom(InputStream is, String encoding) throws IOException {
		object = (JSObject) new JSONParser().parse(is, encoding);
	}

	@Override
	public InputStream asInputStream(final String encoding) {
		return _asInputStream(encoding, false);
	}

	@Override
	public InputStream asPipedInputStream(final String encoding) {
		return _asInputStream(encoding, true);
	}

	private InputStream _asInputStream(final String encoding, boolean piped) {
		InputStream is;
		final OutputStream os;
		if (piped) {
			os = new PipedOutputStream();
		} else {
			os = new ByteArrayOutputStream();
		}
		Runnable task = new Runnable() {
			@Override
			public void run() {
				try {
					new JSONSerializer().serialize(object, os, encoding, true);
				} catch (Exception ex) {
					throw new IllegalStateException("could not serialize json: " + ex.getMessage(), ex);
				}
			}
		};
		if (piped) {
			PipedInputStream s = new PipedInputStream();
			is = s;
			final PipedOutputStream o = (PipedOutputStream) os;
			try {
				o.connect(s);
			} catch (IOException ex) {
				throw new IllegalStateException("could not set up pipe: " + ex.getMessage(), ex);
			}

			Thread t = new Thread(task);
			t.setDaemon(true);
			t.start();
			return s;
		} else {
			task.run();
			is = new ByteArrayInputStream(((ByteArrayOutputStream) os).toByteArray());
		}
		return is;
	}

	@Override
	public void setId(Long id) {
		JSMember member = JSONUtil.getMemberByName("id", object);
		if (member == null) {
			JSONUtil.createNumberMember(object, "id", id);
		} else {
			JSValue idValue;
			if (id == null) {
				idValue = new JSNull();
			} else {
				idValue = new JSNumber();
				((JSNumber) idValue).setValue(new BigDecimal(id));
			}
			member.setValue(idValue);
		}
	}

	@Override
	public Long getId() {
		JSMember member = JSONUtil.getMemberByName("id", object);
		if (member == null) {
			return null;
		}
		if (JSNull.class.isInstance(member.getValue())) {
			return null;
		}
		return ((JSNumber) member.getValue()).getValue().longValue();
	}

	@Override
	public String getSchemaBeanTypeName() {
		return getSchemaBeanInterface().getSimpleName();
	}

	@Override
	public Class<?> getSchemaBeanInterface() {
		return schemaBeanInterface;
	}

}
