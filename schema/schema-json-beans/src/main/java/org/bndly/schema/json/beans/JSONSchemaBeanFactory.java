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

import org.bndly.common.json.model.JSMember;
import org.bndly.common.json.model.JSObject;
import org.bndly.common.json.model.JSString;
import org.bndly.common.json.parsing.JSONParser;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

public class JSONSchemaBeanFactory {

	private final Map<String, Class<?>> defaultBindings = new HashMap<>();
	private final Map<Class<?>, String> defaultTypeBindings = new HashMap<>();

	public void registerTypeBindings(Class<?>... javaTypes) {
		if (javaTypes != null) {
			for (Class<?> t : javaTypes) {
				registerTypeBinding(t);
			}
		}
	}

	public <E> E convertToStreamingObject(E input) {
		E output = null;
		if (!StreamingObject.class.isInstance(input)) {
			if (Proxy.isProxyClass(input.getClass())) {
				Class<?>[] interfaces = input.getClass().getInterfaces();
				for (Class<?> interfaceType : interfaces) {
					if (isSchemaBeanType(interfaceType)) {
						StreamingObject so = (StreamingObject) getSchemaBean(interfaceType);
						try {
							copyData(input, so, interfaceType);
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
							// ignore this
						}
						output = (E) so;
						break;
					}
				}
			} else {
				// this won't work
			}
		} else {
			output = input;
		}
		return output;
	}

	private void copyData(Object source, StreamingObject target, Class<?> interfaceType) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		// get the values an apply them
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
						Object nestedValue = m.invoke(source);
						setter.invoke(target, nestedValue);
					}
				}
			}
		}
	}

	public JSONSchemaBeanInvocationHandler getInvocationHandler(Object e) {
		return getInvocationHandlerFromProxy(e);
	}

	public void registerTypeBinding(Class<?> javaType) {
		registerTypeBinding(javaType.getSimpleName(), javaType);
	}

	public void registerTypeBinding(String typeName, Class<?> javaType) {
		assertTypeIsInterface(javaType);
		defaultBindings.put(typeName, javaType);
		defaultTypeBindings.put(javaType, typeName);
	}

	public Class<?> getBindingForType(String typeName) {
		Class<?> t = defaultBindings.get(typeName);
		if (t == null) {
			throw new IllegalArgumentException("no type binding registered for schema type " + typeName);
		}
		return t;
	}

	public boolean isSchemaBeanType(Class<?> type) {
		return defaultTypeBindings.containsKey(type);
	}

	private void assertTypeIsInterface(Class<?> type) throws IllegalArgumentException {
		if (!type.isInterface()) {
			throw new IllegalArgumentException("schema beans can only be created on interfaces.");
		}
	}

	public <E> E getSchemaBean(Class<E> type) {
		return getSchemaBean(type, null);
	}

	public Object getSchemaBean(InputStream is, String encoding) {
		return getSchemaBean((JSObject) new JSONParser().parse(is, encoding));
	}

	public Object getSchemaBean(JSObject jSObject) {
		if (jSObject == null) {
			throw new JSONSchemaBeanException("jsObject is null. can not created proxy around json, if no jsObject is present.");
		}
		JSMember typeMember = JSONUtil.getMemberByName("_type", jSObject);
		if (typeMember == null) {
			throw new JSONSchemaBeanException("can not create a proxy around a json object, if the '_type' member is missing.");
		}
		String typeName = ((JSString) typeMember.getValue()).getValue();
		Class<?> binding = getBindingForType(typeName);
		return getSchemaBean(binding, jSObject);
	}

	public <E> E getSchemaBean(Class<E> type, JSObject jSObject) {
		assertTypeIsInterface(type);
		JSONSchemaBeanInvocationHandler handler;
		if (jSObject == null) {
			handler = new JSONSchemaBeanInvocationHandler(this, type);
			String typeName = defaultTypeBindings.get(type);
			if (typeName == null) {
				throw new JSONSchemaBeanException("could not determine type name for schema bean type " + type.getName());
			}
			JSONUtil.createStringMember(handler.getJSObject(), "_type", typeName);
		} else {
			handler = new JSONSchemaBeanInvocationHandler(jSObject, this, type);
		}
		Object proxy = Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type, StreamingObject.class}, handler);
		return type.cast(proxy);
	}

	public JSObject getJSObjectFromSchemaBean(Object schemaBean) {
		JSONSchemaBeanInvocationHandler ih = getInvocationHandlerFromProxy(schemaBean);
		return ih.getJSObject();
	}

	public boolean isSchemaBean(Object schemaBean) {
		if (schemaBean == null) {
			throw new IllegalArgumentException("can not check if null is a schema bean");
		}
		if (Proxy.isProxyClass(schemaBean.getClass())) {
			InvocationHandler ih = Proxy.getInvocationHandler(schemaBean);
			if (JSONSchemaBeanInvocationHandler.class.isInstance(ih)) {
				return true;
			}
		}
		return false;
	}

	private JSONSchemaBeanInvocationHandler getInvocationHandlerFromProxy(Object schemaBean) {
		if (!isSchemaBean(schemaBean)) {
			throw new IllegalArgumentException("provided object is not a schema bean.");
		}
		InvocationHandler ih = Proxy.getInvocationHandler(schemaBean);
		if (!JSONSchemaBeanInvocationHandler.class.isInstance(ih)) {
			throw new IllegalArgumentException("provided object is a proxy but not a schema bean.");
		}
		return JSONSchemaBeanInvocationHandler.class.cast(ih);
	}

}
