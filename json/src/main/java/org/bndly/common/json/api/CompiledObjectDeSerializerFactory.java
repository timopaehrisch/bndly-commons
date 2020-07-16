package org.bndly.common.json.api;

/*-
 * #%L
 * JSON
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

import org.bndly.common.json.model.JSObject;
import org.bndly.common.json.model.JSValue;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class CompiledObjectDeSerializerFactory {
	
	public static final CompiledObjectDeSerializerFactory INSTANCE = new CompiledObjectDeSerializerFactory();

	private static final Logger LOG = LoggerFactory.getLogger(CompiledObjectDeSerializerFactory.class);
	private static final String PREFIX_OBJECT = "get";
	private static final String PREFIX_BOOLEAN = "is";
	private static final String PREFIX_SETTER = "set";
	
	private static final NamingPolicy DEFAULT_SETTER_NAMING_POLICY = new NamingPolicy() {
		@Override
		public String deriveNameFrom(Method method) {
			String name = method.getName();
			if (name.startsWith(PREFIX_SETTER)) {
				// all other getters
				return lowercaseFirstChar(name.substring(PREFIX_SETTER.length()));
			} else {
				throw new IllegalArgumentException("unsupported method: " + method);
			}
		}

		@Override
		public String deriveNameFrom(Field field) {
			return field.getName();
		}
		
	};
			
	private static final NamingPolicy DEFAULT_GETTER_NAMING_POLICY = new NamingPolicy() {
		@Override
		public String deriveNameFrom(Method method) {
			Class<?> returnType = method.getReturnType();
			String name = method.getName();
			if (boolean.class.equals(returnType) && name.startsWith(PREFIX_BOOLEAN)) {
				// a boolean getter
				return lowercaseFirstChar(name.substring(PREFIX_BOOLEAN.length()));
			} else if (name.startsWith(PREFIX_OBJECT)) {
				// all other getters
				return lowercaseFirstChar(name.substring(PREFIX_OBJECT.length()));
			} else {
				throw new IllegalArgumentException("unsupported method: " + method);
			}
		}

		@Override
		public String deriveNameFrom(Field field) {
			return field.getName();
		}
	};

	public static enum CompilationFlavor {
		GETTERS,
		FIELDS,
		GETTERS_AND_FIELDS
	}
	
	public static interface NamingPolicy {
		String deriveNameFrom(Method method);
		String deriveNameFrom(Field field);
	}
	
	private interface CompiledPropertySerializer {
		void serialize(Object source, JSObject target, ConversionContext conversionContext);
	}
	
	private interface CompiledPropertyDeserializer {
		void deserialize(JSObject source, Object target, ConversionContext conversionContext);
	}
	
	private CompiledObjectDeSerializerFactory() {
	}
	
	public Deserializer compileDeserializer(final Class clazzToInspect) {
		return compileDeserializer(clazzToInspect, DEFAULT_SETTER_NAMING_POLICY, CompilationFlavor.GETTERS);
	}
	
	public Deserializer compileDeserializer(final Class clazzToInspect, NamingPolicy namingPolicy, CompilationFlavor compilationFlavor) {
		if (namingPolicy == null) {
			namingPolicy = DEFAULT_SETTER_NAMING_POLICY;
		}
		if (compilationFlavor == null) {
			compilationFlavor = CompilationFlavor.GETTERS;
		}
		final Map<String, CompiledPropertyDeserializer> propertyDeserializers = new LinkedHashMap<>();
		inspectForDeserializers(clazzToInspect, propertyDeserializers, namingPolicy, compilationFlavor);
		return new Deserializer() {
			@Override
			public boolean canDeserialize(Type targetType, ConversionContext conversionContext, JSValue value) {
				return clazzToInspect.equals(targetType) && JSObject.class.isInstance(value);
			}

			@Override
			public Object deserialize(Type targetType, ConversionContext conversionContext, JSValue value) {
				Object target = conversionContext.newInstance(targetType, value);
				if (target == null) {
					return null;
				}
				for (CompiledPropertyDeserializer deserializer : propertyDeserializers.values()) {
					deserializer.deserialize((JSObject) value, target, conversionContext);
				}
				return target;
			}
		};
	}
	
	public Serializer compileSerializer(final Class clazzToInspect) {
		return compileSerializer(clazzToInspect, DEFAULT_GETTER_NAMING_POLICY, CompilationFlavor.GETTERS);
	}

	public Serializer compileSerializer(final Class clazzToInspect, NamingPolicy namingPolicy, CompilationFlavor compilationFlavor) {
		if (namingPolicy == null) {
			namingPolicy = DEFAULT_GETTER_NAMING_POLICY;
		}
		if (compilationFlavor == null) {
			compilationFlavor = CompilationFlavor.GETTERS;
		}
		// look for fields and for getters
		final Map<String, CompiledPropertySerializer> propertySerializers = new LinkedHashMap<>();
		inspectForSerializers(clazzToInspect, propertySerializers, namingPolicy, compilationFlavor);
		return new Serializer() {
			@Override
			public boolean canSerialize(Type sourceType, ConversionContext conversionContext, Object javaValue) {
				return clazzToInspect.equals(sourceType);
			}

			@Override
			public JSValue serialize(Type sourceType, ConversionContext conversionContext, Object javaValue) {
				JSObject object = new JSObject();
				for (CompiledPropertySerializer propertySerializer : propertySerializers.values()) {
					propertySerializer.serialize(javaValue, object, conversionContext);
				}
				return object;
			}
		};
	}
	
	private void inspectForDeserializers(Class clazzToInspect, Map<String, CompiledPropertyDeserializer> propertyDeserializers, NamingPolicy namingPolicy, CompilationFlavor compilationFlavor) {
		if (clazzToInspect == null || clazzToInspect.equals(Object.class)) {
			return;
		}
		if (compilationFlavor == CompilationFlavor.GETTERS || compilationFlavor == CompilationFlavor.GETTERS_AND_FIELDS) {
			// get the public getters
			Method[] publicMethods = clazzToInspect.getMethods();
			if (publicMethods != null) {
				for (final Method publicMethod : publicMethods) {
					compileSetterDeserializer(publicMethod, propertyDeserializers, namingPolicy);
				}
			}
		}
		if (compilationFlavor == CompilationFlavor.FIELDS || compilationFlavor == CompilationFlavor.GETTERS_AND_FIELDS) {
			Field[] fields = clazzToInspect.getDeclaredFields();
			if (fields != null) {
				for (final Field field : fields) {
					compileFieldDeserializer(field, propertyDeserializers, namingPolicy);
				}
			}
			// only the fields need recursive inspection
			inspectForDeserializers(clazzToInspect.getSuperclass(), propertyDeserializers, namingPolicy, compilationFlavor);
		}
	}
	
	private void inspectForSerializers(Class clazzToInspect, Map<String, CompiledPropertySerializer> propertySerializers, NamingPolicy namingPolicy, CompilationFlavor compilationFlavor) {
		if (clazzToInspect == null || clazzToInspect.equals(Object.class)) {
			return;
		}
		
		if (compilationFlavor == CompilationFlavor.GETTERS || compilationFlavor == CompilationFlavor.GETTERS_AND_FIELDS) {
			// get the public getters
			Method[] publicMethods = clazzToInspect.getMethods();
			if (publicMethods != null) {
				for (final Method publicMethod : publicMethods) {
					compileGetterSerializer(publicMethod, propertySerializers, namingPolicy);
				}
			}
		}
		if (compilationFlavor == CompilationFlavor.FIELDS || compilationFlavor == CompilationFlavor.GETTERS_AND_FIELDS) {
			Field[] fields = clazzToInspect.getDeclaredFields();
			if (fields != null) {
				for (final Field field : fields) {
					compileFieldSerializer(field, propertySerializers, namingPolicy);
				}
			}
			// only the fields need recursive inspection
			inspectForSerializers(clazzToInspect.getSuperclass(), propertySerializers, namingPolicy, compilationFlavor);
		}
	}

	private void compileFieldDeserializer(final Field field, Map<String, CompiledPropertyDeserializer> propertyDeserializers, NamingPolicy namingPolicy) {
		if (Modifier.isStatic(field.getModifiers())) {
			return;
		}
		if (Modifier.isFinal(field.getModifiers())) {
			return;
		}
		final String propertyName;
		JSONProperty jsonProperty = field.getAnnotation(JSONProperty.class);
		if (jsonProperty != null) {
			propertyName = jsonProperty.value();
		} else {
			propertyName = namingPolicy.deriveNameFrom(field);
		}
		if (propertyDeserializers.containsKey(propertyName)) {
			return;
		}
		final Type propertyType = field.getGenericType();
		if (field.getType().isPrimitive()) {
			propertyDeserializers.put(propertyName, new CompiledPropertyDeserializer() {
				@Override
				public void deserialize(JSObject source, Object target, ConversionContext conversionContext) {
					JSValue jsonPropertyValue = source.getMemberValue(propertyName);
					if (jsonPropertyValue != null) {
						boolean ia = field.isAccessible();
						try {
							field.setAccessible(true);
							Object deserialized = conversionContext.deserialize(propertyType, jsonPropertyValue);
							// primitives can not be null, therefore we have the additional check here
							if (deserialized != null) {
								field.set(target, deserialized);
							}
						} catch (IllegalAccessException ex) {
							// reading the field failed.
							LOG.error("could not get property: " + ex.getMessage(), ex);
						} finally {
							field.setAccessible(ia);
						}
					}
				}

			});
		} else {
			propertyDeserializers.put(propertyName, new CompiledPropertyDeserializer() {
				@Override
				public void deserialize(JSObject source, Object target, ConversionContext conversionContext) {
					JSValue jsonPropertyValue = source.getMemberValue(propertyName);
					if (jsonPropertyValue != null) {
						boolean ia = field.isAccessible();
						try {
							field.setAccessible(true);
							Object deserialized = conversionContext.deserialize(propertyType, jsonPropertyValue);
							field.set(target, deserialized);
						} catch (IllegalAccessException ex) {
							// reading the field failed.
							LOG.error("could not get property: " + ex.getMessage(), ex);
						} finally {
							field.setAccessible(ia);
						}
					}
				}

			});
		}
	}
	
	private void compileFieldSerializer(final Field field, Map<String, CompiledPropertySerializer> propertySerializers, NamingPolicy namingPolicy) {
		// ignore static fields
		if (Modifier.isStatic(field.getModifiers())) {
			return;
		}
		final String propertyName;
		JSONProperty jsonProperty = field.getAnnotation(JSONProperty.class);
		if (jsonProperty != null) {
			propertyName = jsonProperty.value();
		} else {
			propertyName = namingPolicy.deriveNameFrom(field);
		}
		if (propertySerializers.containsKey(propertyName)) {
			return;
		}
		final Type propertyType = field.getGenericType();
		propertySerializers.put(propertyName, new CompiledPropertySerializer() {
			@Override
			public void serialize(Object source, JSObject target, ConversionContext conversionContext) {
				boolean ia = field.isAccessible();
				try {
					field.setAccessible(true);
					Object propertyValue = field.get(source);
					JSValue serializedObjectProperty = conversionContext.serialize(propertyType, propertyValue);
					if (serializedObjectProperty != null) {
						target.createMember(propertyName).setValue(serializedObjectProperty);
					}
				} catch (IllegalAccessException ex) {
					// reading the field failed.
					LOG.error("could not get property: " + ex.getMessage(), ex);
				} finally {
					field.setAccessible(ia);
				}
			}
		});
	}

	private void compileSetterDeserializer(final Method publicMethod, Map<String, CompiledPropertyDeserializer> propertyDeserializers, NamingPolicy namingPolicy) {
		if (Object.class.equals(publicMethod.getDeclaringClass())) {
			// skip the reflection methods
			return;
		}
		// ignore static methods
		if (Modifier.isStatic(publicMethod.getModifiers())) {
			return;
		}
		// is it a setter?
		final Type[] pt = publicMethod.getGenericParameterTypes();
		if (pt.length != 1) {
			return;
		}
		final String propertyName;
		JSONProperty jsonProperty = publicMethod.getAnnotation(JSONProperty.class);
		if (jsonProperty != null) {
			propertyName = jsonProperty.value();
		} else {
			propertyName = namingPolicy.deriveNameFrom(publicMethod);
		}
		if (propertyDeserializers.containsKey(propertyName)) {
			return;
		}
		if (publicMethod.getParameterTypes()[0].isPrimitive()) {
			// for primitives we need an additional null check
			propertyDeserializers.put(propertyName, new CompiledPropertyDeserializer() {
				@Override
				public void deserialize(JSObject source, Object target, ConversionContext conversionContext) {
					JSValue propertyJsonValue = source.getMemberValue(propertyName);
					if (propertyJsonValue != null) {
						Object deserializedProperty = conversionContext.deserialize(pt[0], propertyJsonValue);
						// call the setter
						if (deserializedProperty != null) {
							try {
								publicMethod.invoke(target, deserializedProperty);
							} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
								// invoking a public setter failed.
								LOG.error("could not set property: " + ex.getMessage(), ex);
							}
						}
					}
				}
			});
		} else {
			propertyDeserializers.put(propertyName, new CompiledPropertyDeserializer() {
				@Override
				public void deserialize(JSObject source, Object target, ConversionContext conversionContext) {
					JSValue propertyJsonValue = source.getMemberValue(propertyName);
					if (propertyJsonValue != null) {
						Object deserializedProperty = conversionContext.deserialize(pt[0], propertyJsonValue);
						// call the setter
						try {
							publicMethod.invoke(target, deserializedProperty);
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
							// invoking a public setter failed.
							LOG.error("could not set property: " + ex.getMessage(), ex);
						}
					}
				}
			});
		}
	}
	
	private void compileGetterSerializer(final Method publicMethod, Map<String, CompiledPropertySerializer> propertySerializers, NamingPolicy namingPolicy) {
		if (Object.class.equals(publicMethod.getDeclaringClass())) {
			// skip the reflection methods
			return;
		}
		// ignore static methods
		if (Modifier.isStatic(publicMethod.getModifiers())) {
			return;
		}
		// is it a getter?
		if (publicMethod.getParameterTypes().length != 0) {
			return;
		}
		Class<?> returnType = publicMethod.getReturnType();
		if (returnType.equals(Void.class)) {
			return;
		}
		if (returnType.equals(boolean.class) && !publicMethod.getName().startsWith(PREFIX_BOOLEAN)) {
			return;
		}
		if (!publicMethod.getName().startsWith(PREFIX_OBJECT)) {
			return;
		}
		final String propertyName;
		JSONProperty jsonProperty = publicMethod.getAnnotation(JSONProperty.class);
		if (jsonProperty != null) {
			propertyName = jsonProperty.value();
		} else {
			propertyName = namingPolicy.deriveNameFrom(publicMethod);
		}
		if (propertySerializers.containsKey(propertyName)) {
			return;
		}
		propertySerializers.put(propertyName, new CompiledPropertySerializer() {
			@Override
			public void serialize(Object source, JSObject target, ConversionContext conversionContext) {
				try {
					Object propertyValue = publicMethod.invoke(source);
					Type propertyType = publicMethod.getGenericReturnType();
					JSValue serializedObjectProperty = conversionContext.serialize(propertyType, propertyValue);
					if (serializedObjectProperty != null) {
						target.createMember(propertyName).setValue(serializedObjectProperty);
					}
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
					// invoking the public getter failed.
					LOG.error("could not get property: " + ex.getMessage(), ex);
				}
			}
		});
	}

	private static String lowercaseFirstChar(String propertyName) {
		propertyName = propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1);
		return propertyName;
	}
}
