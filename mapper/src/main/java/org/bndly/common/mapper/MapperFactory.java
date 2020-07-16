package org.bndly.common.mapper;

/*-
 * #%L
 * Mapper
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

import org.bndly.common.reflection.CompiledBeanPropertyAccessorWriter;
import org.bndly.common.reflection.CompiledFieldBeanPropertyAccessorWriter;
import org.bndly.common.reflection.UnresolvablePropertyException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.osgi.service.component.annotations.Component;

@Component(service = MapperFactory.class, immediate = true)
public class MapperFactory {

	public static final String NAME = "mapperFactory";
	private final List<MappingPreInterceptor> preInterceptors = new ArrayList<>();
	private final List<MappingPostInterceptor> postInterceptors = new ArrayList<>();
	private final List<MappingContextCustomizer> customizers = new ArrayList<>();
	private final List<Mapper> mappers = new ArrayList<>();
	private final Map<Class<?>, Map<Class<?>, Mapper>> mappersByType = new HashMap<>();
	private final Map<Class<?>, Map<Class<?>, CollectionMapper>> collectionMappersByType = new HashMap<>();
	private final Map<Class<?>, CollectionTypeAdapter> collectionTypeAdapters = new HashMap<>();
	private TypeInstanceBuilder typeInstanceBuilder = new DefaultTypeInstanceBuilder();
	private final List<MappingContextKeyBuilder> mappingContextKeyBuilders = new ArrayList<>();

	private MissingMapperHandler missingMapperHandler = new DefaultMissingMapperHandler();
	private ComplexTypeDetector complexTypeDetector;
	private MapperAmbiguityResolver ambiguityResolver;

	private MappedProperty buildMappedProperty(String propertyName, CompiledBeanPropertyAccessorWriter accessorWriter) {
		try {
			Class<?> propertyType = accessorWriter.typeOf(propertyName, null);
			return new MappedProperty(propertyName, propertyType, accessorWriter, accessorWriter);
		} catch (UnresolvablePropertyException e) {
			return null;
		}
	}

	public void registerMapper(Mapper mapper) {
		this.mappers.add(mapper);
		if (TypeSpecificMapper.class.isInstance(mapper)) {
			TypeSpecificMapper tsMapper = (TypeSpecificMapper) mapper;
			Map<Class<?>, Mapper> inputMappers = this.mappersByType.get(tsMapper.getSupportedInput());
			if (inputMappers == null) {
				inputMappers = new HashMap<>();
				this.mappersByType.put(tsMapper.getSupportedInput(), inputMappers);
			}
			inputMappers.put(tsMapper.getSupportedOutput(), mapper);
		}
		if (CollectionMapper.class.isInstance(mapper)) {
			CollectionMapper collectionMapper = (CollectionMapper) mapper;
			Class<?> collectionType = collectionMapper.getCollectionType();
			Class<?> elementType = collectionMapper.getElementType();
			Map<Class<?>, CollectionMapper> collectionMapperByElementType = this.collectionMappersByType.get(collectionType);
			if (collectionMapperByElementType == null) {
				collectionMapperByElementType = new HashMap<>();
				this.collectionMappersByType.put(collectionType, collectionMapperByElementType);
			}
			collectionMapperByElementType.put(elementType, collectionMapper);
		}
	}

	public MappingContext buildContext() {
		MappingContextImpl ctx = new MappingContextImpl(missingMapperHandler) {
			@Override
			protected Mapper getMapper(Class<?> inputType, Class<?> desiredOutputType, Object inputObject) {
				return _getMapper(inputType, desiredOutputType, inputObject);
			}
			
			@Override
			protected <T> T buildInstance(Class<T> type, MappingState state) {
				return typeInstanceBuilder.buildInstance(type, state);
			}
			
			@Override
			protected MappingContextKey buildMappingContextKey(Object source) {
				if (mappingContextKeyBuilders.isEmpty()) {
					return null;
				}
				for (MappingContextKeyBuilder mappingContextKeyBuilder : mappingContextKeyBuilders) {
					MappingContextKey key = mappingContextKeyBuilder.buildMappingContextKey(source);
					if (key != null) {
						return key;
					}
				}
				return null;
			}
			
			@Override
			protected CollectionTypeAdapter getCollectionTypeAdapterFor(Class<?> type) {
				return _getCollectionTypeAdapterFor(type);
			}
			
			@Override
			protected boolean isCollectionType(Class<?> type) {
				return _isCollectionType(type);
			}
			
		};
		
		for (MappingContextCustomizer customizer : customizers) {
			customizer.customize(ctx);
		}
		return ctx;
	}

	public void buildCollection(Class<?> collectionType, Class<?> collectionElementType) {
		CollectionMapper mapper = new CollectionMapperImpl(collectionType, collectionElementType) {

			@Override
			protected CollectionTypeAdapter assertCollectionTypeAdapterExists(Object object) {
				return _assertCollectionTypeAdapterExists(object);
			}

			@Override
			protected CollectionTypeAdapter assertCollectionTypeAdapterExists(Class<?> type) {
				return _assertCollectionTypeAdapterExists(type);
			}
		};
		registerMapper(mapper);
	}

	private CollectionTypeAdapter _assertCollectionTypeAdapterExists(Class<?> collectionType) {
		CollectionTypeAdapter adapter = _getCollectionTypeAdapterFor(collectionType);
		if (adapter == null) {
			throw new IllegalStateException("could not find a collection type adapter for collection type " + collectionType.getSimpleName());
		}
		return adapter;
	}

	private CollectionTypeAdapter _assertCollectionTypeAdapterExists(Object source) {
		Class<?> collectionType = source.getClass();
		return _assertCollectionTypeAdapterExists(collectionType);
	}

	private CollectionTypeAdapter _getCollectionTypeAdapterFor(Class<?> collectionType) {
		if (collectionType == null) {
			throw new IllegalArgumentException("can not get collection type adapter because collectionType parameter was null.");
		}
		CollectionTypeAdapter adapter = collectionTypeAdapters.get(collectionType);
		if (adapter == null) {
			Class<?> superCollectionType = collectionType.getSuperclass();
			if (superCollectionType != null) {
				adapter = _getCollectionTypeAdapterFor(superCollectionType);
			}
			if (adapter == null) {
				Class<?>[] interfaces = collectionType.getInterfaces();
				for (Class<?> interfaceType : interfaces) {
					adapter = _getCollectionTypeAdapterFor(interfaceType);
					if (adapter != null) {
						break;
					}
				}
			}
		}
		return adapter;
	}

	public void register(CollectionTypeAdapter adapter) {
		this.collectionTypeAdapters.put(adapter.getSupportedCollectionType(), adapter);
	}

	private boolean _isCollectionType(Class<?> type) {
		return _getCollectionTypeAdapterFor(type) != null;
	}

	public void build(Class<?> restType, Class<?> domainType) {
		Mapping mappingAnnotation = getMappingAnnotation(restType);
		if (mappingAnnotation != null) {
			Mapping.Type mode = mappingAnnotation.mode();
			if (Mapping.Type.AUTO.equals(mode)) {
				buildAutoMappers(restType, domainType);
			} else if (Mapping.Type.MANUAL.equals(mode)) {
				buildManualMappers(restType, domainType);
			} else {
				// unsupported mapping mode
			}
		}
	}

	private Mapping getMappingAnnotation(Class<?> restType) {
		Mapping a = restType.getAnnotation(Mapping.class);
		if (a == null) {
			Class<?> sType = restType.getSuperclass();
			if (sType != null && !Object.class.equals(restType)) {
				return getMappingAnnotation(sType);
			}
		}
		return a;
	}

	public TypeSpecificMapper buildAutoMapper(Class<?> from, Class<?> to) {
		final List<Tuple> tuples = new ArrayList<>();
		lookForAutoMapTuples(tuples, new CompiledBeanPropertyAccessorWriter(from), new CompiledBeanPropertyAccessorWriter(to));
		return buildMapperFromTuples(tuples, false, from, to);
	}

	public void buildAutoMappers(Class<?> restType, Class<?> domainType) {
		final List<Tuple> tuples = new ArrayList<>();
		lookForAutoMapTuples(tuples, new CompiledBeanPropertyAccessorWriter(restType), new CompiledBeanPropertyAccessorWriter(domainType));
		TypeSpecificMapper mapper = buildMapperFromTuples(tuples, true, domainType, restType);
		registerMapper(mapper);
		mappers.add(mapper);

		mapper = buildMapperFromTuples(tuples, false, restType, domainType);
		registerMapper(mapper);
		mappers.add(mapper);
	}

	private void lookForAutoMapTuples(List<Tuple> tuples, CompiledBeanPropertyAccessorWriter restType, CompiledBeanPropertyAccessorWriter domainType) {
		CompiledFieldBeanPropertyAccessorWriter fieldAccessorWriter = restType.getFieldAccessorWriter();
		Set<String> supportedPropertyNames = fieldAccessorWriter.getSupportedPropertyNames();
		for (String supportedPropertyName : supportedPropertyNames) {
			// if the field is not blocked explicitly
			NotMapped propertyMetaData = fieldAccessorWriter.getPropertyMetaData(supportedPropertyName, NotMapped.class);
			if (propertyMetaData != null) {
				return ;
			}
			Class<?> type = fieldAccessorWriter.typeOf(supportedPropertyName, null);
			MappedProperty sourceProperty = new MappedProperty(supportedPropertyName, type, restType, restType);

			Mapped mappedAnnotation = fieldAccessorWriter.getPropertyMetaData(supportedPropertyName, Mapped.class);
			String targetPropertyName = supportedPropertyName;
			if (mappedAnnotation != null) {
				String v = mappedAnnotation.value();
				if (!"".equals(v)) {
					targetPropertyName = v;
				}
			}

			MappedProperty targetProperty = buildMappedProperty(targetPropertyName, domainType);

			if (sourceProperty != null && targetProperty != null) {
				Tuple tuple = new Tuple(sourceProperty, targetProperty);
				tuples.add(tuple);
			}
		}
	}

	private void buildManualMappers(Class<?> restType, Class<?> domainType) {
		final List<Tuple> tuples = new ArrayList<>();
		lookForManualMapTuples(tuples, new CompiledBeanPropertyAccessorWriter(restType), new CompiledBeanPropertyAccessorWriter(domainType));

		TypeSpecificMapper mapper = buildMapperFromTuples(tuples, true, domainType, restType);
		registerMapper(mapper);
		mappers.add(mapper);

		mapper = buildMapperFromTuples(tuples, false, restType, domainType);
		registerMapper(mapper);
		mappers.add(mapper);
	}

	private void lookForManualMapTuples(List<Tuple> tuples, CompiledBeanPropertyAccessorWriter restType, CompiledBeanPropertyAccessorWriter domainType) {
		CompiledFieldBeanPropertyAccessorWriter fieldAccessorWriter = restType.getFieldAccessorWriter();
		for (String supportedPropertyName : fieldAccessorWriter.getSupportedPropertyNames()) {

			// if the field is not blocked explicitly
			Mapped mappedAnnotation = fieldAccessorWriter.getPropertyMetaData(supportedPropertyName, Mapped.class);
			if (mappedAnnotation != null) {
				MappedProperty sourceProperty = new MappedProperty(supportedPropertyName, fieldAccessorWriter.typeOf(supportedPropertyName, null), restType, restType);

				String targetPropertyName = supportedPropertyName;
				String v = mappedAnnotation.value();
				if (!"".equals(v)) {
					targetPropertyName = v;
				}

				MappedProperty targetProperty = buildMappedProperty(targetPropertyName, domainType);

				if (targetProperty != null) {
					Tuple tuple = new Tuple(sourceProperty, targetProperty);
					tuples.add(tuple);
				}
			}
		}
	}

	private TypeSpecificMapper buildMapperFromTuples(List<Tuple> tuples, boolean swapMappingDirection, Class<?> inputType, Class<?> outputType) {
		TypeSpecificMapper mapper = new TypeSpecificMapperImpl(inputType, outputType, tuples, swapMappingDirection) {

			@Override
			protected List<MappingPreInterceptor> getMappingPreInterceptors() {
				return preInterceptors;
			}

			@Override
			protected List<MappingPostInterceptor> getMappingPostInterceptors() {
				return postInterceptors;
			}

			@Override
			protected boolean isComplexType(Class<?> propertyType, Object propertyValue) {
				if (complexTypeDetector == null) {
					return false;
				}
				return complexTypeDetector.isComplexType(propertyType, propertyValue);
			}

			@Override
			protected boolean isCollectionType(Class<?> type) {
				return _isCollectionType(type);
			}

			@Override
			protected CollectionTypeAdapter assertCollectionTypeAdapterExists(Class<?> type) {
				return _assertCollectionTypeAdapterExists(type);
			}

			@Override
			protected CollectionTypeAdapter assertCollectionTypeAdapterExists(Object object) {
				return _assertCollectionTypeAdapterExists(object);
			}
		};
		return mapper;
	}

	private Mapper _getMapper(Class<?> inputType, Class<?> desiredOutputType, Object inputObject) {
		if (Proxy.isProxyClass(inputType)) {
			Class<?>[] interfaces = inputType.getInterfaces();
			for (Class<?> interfaceType : interfaces) {
				Mapper mapper = _getMapper(interfaceType, desiredOutputType, inputObject);
				if (mapper != null) {
					return mapper;
				}
			}
		}
		if (_isCollectionType(inputType) && _isCollectionType(desiredOutputType)) {
			return new CollectionCopyMapperImpl() {

				@Override
				protected CollectionTypeAdapter assertCollectionTypeAdapterExists(Object object) {
					return _assertCollectionTypeAdapterExists(object);
				}

				@Override
				protected CollectionTypeAdapter assertCollectionTypeAdapterExists(Class<?> type) {
					return _assertCollectionTypeAdapterExists(type);
				}
			};
		}
		Mapper mapper = null;
		while (mapper == null && inputType != null && !Object.class.equals(inputType)) {
			Map<Class<?>, Mapper> mappersForInput = this.mappersByType.get(inputType);
			if (mappersForInput != null) {
				if (desiredOutputType == null) {
					if (mappersForInput.size() > 1) {
						// resolve the ambigouity
						if (ambiguityResolver == null) {
							throw new IllegalStateException("can not resolve mapper ambiguity, because a ambiguityResolver is missing");
						}
						mapper = ambiguityResolver.pickMapper(inputType, mappersForInput, inputObject);
					} else {
						for (Map.Entry<Class<?>, Mapper> entry : mappersForInput.entrySet()) {
							Mapper tmp = entry.getValue();
							if (tmp != null) {
								mapper = tmp;
							}
						}
					}
				} else {
					if (mappersForInput.size() > 1) {
						Map<Class<?>, Mapper> filteredMappersForInput = new HashMap<>();
						Mapper tmp = null;
						for (Map.Entry<Class<?>, Mapper> entrySet : mappersForInput.entrySet()) {
							Class<? extends Object> key = entrySet.getKey();
							Mapper value = entrySet.getValue();
							if (desiredOutputType.isAssignableFrom(key)) {
								filteredMappersForInput.put(key, value);
								tmp = value;
							}
						}
						if (filteredMappersForInput.size() == 1) {
							mapper = tmp;
						} else {
							mapper = ambiguityResolver.pickMapper(inputType, mappersForInput, inputObject);
						}
					} else {
						Class<?> curOutputType = desiredOutputType;
						while (mapper == null && curOutputType != null && !Object.class.equals(curOutputType)) {
							mapper = mappersForInput.get(curOutputType);
							curOutputType = curOutputType.getSuperclass();
						}
					}

					// if the desired output type is super type of all mappers,
					// no mapper will be found. hence we will try if there is exactly
					// one mapper that can deal with the desired output type.
					if (mapper == null) {
						Set<Class<?>> supportedClasses = new HashSet<>();
						for (Map.Entry<Class<?>, Mapper> entry : mappersForInput.entrySet()) {
							Class<? extends Object> clazz = entry.getKey();
							if (desiredOutputType.isAssignableFrom(clazz)) {
								supportedClasses.add(clazz);
							}
						}
						if (supportedClasses.size() == 1) {
							for (Class<?> clazz : supportedClasses) {
								mapper = mappersForInput.get(clazz);
							}
						}
					}
				}
			}
			inputType = inputType.getSuperclass();
		}
		return mapper;
	}

	public void setComplexTypeDetector(ComplexTypeDetector complexTypeDetector) {
		this.complexTypeDetector = complexTypeDetector;
	}

	public void addCustomizer(MappingContextCustomizer customizer) {
		customizers.add(customizer);
	}

	public void addPostInterceptor(MappingPostInterceptor interceptor) {
		postInterceptors.add(interceptor);
	}

	public void addPreInterceptor(MappingPreInterceptor interceptor) {
		preInterceptors.add(interceptor);
	}

	public void addMappingContextKeyBuilder(MappingContextKeyBuilder mappingContextKeyBuilder) {
		if (mappingContextKeyBuilder != null) {
			mappingContextKeyBuilders.add(mappingContextKeyBuilder);
		}
	}

	public void removeCustomizer(MappingContextCustomizer customizer) {
		customizers.remove(customizer);
	}

	public void removeMappingContextKeyBuilder(MappingContextKeyBuilder mappingContextKeyBuilder) {
		if (mappingContextKeyBuilder != null) {
			mappingContextKeyBuilders.remove(mappingContextKeyBuilder);
		}
	}

	public void setTypeInstanceBuilder(TypeInstanceBuilder typeInstanceBuilder) {
		this.typeInstanceBuilder = typeInstanceBuilder;
	}

	public void setAmbiguityResolver(MapperAmbiguityResolver ambiguityResolver) {
		this.ambiguityResolver = ambiguityResolver;
	}

	public void setMissingMapperHandler(MissingMapperHandler missingMapperHandler) {
		this.missingMapperHandler = missingMapperHandler;
	}

}
