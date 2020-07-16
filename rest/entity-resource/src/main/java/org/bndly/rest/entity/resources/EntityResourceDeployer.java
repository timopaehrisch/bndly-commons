package org.bndly.rest.entity.resources;

/*-
 * #%L
 * REST Entity Resource
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

import org.bndly.common.converter.api.ConverterRegistry;
import org.bndly.common.cors.api.CORSRequestDetector;
import org.bndly.common.data.api.FileExtensionContentTypeMapper;
import org.bndly.common.mapper.DomainCollectionAdapter;
import org.bndly.common.mapper.Mapper;
import org.bndly.common.mapper.MapperAmbiguityResolver;
import org.bndly.common.mapper.MapperFactory;
import org.bndly.common.mapper.MappingContext;
import org.bndly.common.mapper.MappingContextCustomizer;
import org.bndly.common.mapper.MappingState;
import org.bndly.common.reflection.BeanPropertyWriter;
import org.bndly.common.reflection.CompiledMethodBeanPropertyAccessorWriter;
import org.bndly.rest.api.DefaultCharacterEncodingProvider;
import org.bndly.rest.atomlink.api.annotation.AtomLinkConstraint;
import org.bndly.rest.cache.api.CacheLinkingService;
import org.bndly.rest.controller.api.ControllerResourceRegistry;
import org.bndly.rest.descriptor.ParameterExtractor;
import org.bndly.rest.entity.resources.impl.ServiceTrackingFileExtensionContentTypeMapper;
import org.bndly.schema.api.DeletionStrategy;
import org.bndly.schema.beans.ActiveRecord;
import org.bndly.schema.beans.SchemaBeanFactory;
import org.bndly.schema.json.beans.StreamingObject;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.BinaryAttribute;
import org.bndly.schema.model.Schema;
import org.bndly.schema.model.SchemaUtil;
import org.bndly.schema.model.Type;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBException;

@Component(service = EntityResourceDeployer.class, immediate = true)
public class EntityResourceDeployer {
	private BundleContext bundleContext;
	private static final Logger LOG = LoggerFactory.getLogger(EntityResourceDeployer.class);
	private ServiceTracker<SchemaAdapter, SchemaAdapter> schemaAdapterTracker;

	private class DeployedSchema {
		private final SchemaAdapter schemaAdapter;
		private final List<Object> registeredResources = new ArrayList<>();
		private MapperFactory mapperFactory;
		private final List<ServiceRegistration> registrations = new ArrayList<>();
		private final DeletionStrategy deletionStrategy = null;
		private final List<AtomLinkConstraint> constraintList = null;
		private final DelegatingMappingPostInterceptor mappingPostInterceptor = new DelegatingMappingPostInterceptor();
		private final SchemaBeanTypeInstanceBuilder instancebuilder = new SchemaBeanTypeInstanceBuilder();

		public DeployedSchema(final SchemaAdapter schemaAdapter) {
			if (schemaAdapter == null) {
				throw new IllegalArgumentException("schemaAdapter is not allowed to be null");
			}
			this.schemaAdapter = schemaAdapter;
			mappingPostInterceptor.addInterceptor(new ActiveRecordIdMappingPostInterceptor() {
				Map<Class, BeanPropertyWriter> cachedWriters = new HashMap<>();
				
				@Override
				protected BeanPropertyWriter getBeanPropertyWriter(MappingState state) {
					Object target = state.getTarget();
					if (target != null) {
						Class<? extends Object> cls = target.getClass();
						if (cls.getClassLoader() == schemaAdapter.getSchemaRestBeanClassLoader()) {
							BeanPropertyWriter writer = cachedWriters.get(cls);
							if (writer == null) {
								writer = new CompiledMethodBeanPropertyAccessorWriter(cls);
								cachedWriters.put(cls, writer);
							}
							return writer;
						}
					}
					return super.getBeanPropertyWriter(state);
				}
				
			});
			mappingPostInterceptor.addInterceptor(new StreamingObjectIdMappingPostInterceptor());
		}

		public SchemaBeanTypeInstanceBuilder getInstancebuilder() {
			return instancebuilder;
		}

		public List<ServiceRegistration> getRegistrations() {
			return registrations;
		}

		public List<Object> getRegisteredResources() {
			return registeredResources;
		}

		public SchemaAdapter getSchemaAdapter() {
			return schemaAdapter;
		}

		public DeletionStrategy getDeletionStrategy() {
			return deletionStrategy;
		}

		public MapperFactory getMapperFactory() {
			return mapperFactory;
		}

		public DelegatingMappingPostInterceptor getMappingPostInterceptor() {
			return mappingPostInterceptor;
		}

		public List<AtomLinkConstraint> getConstraintList() {
			return constraintList;
		}

		private void setMapperFactory(MapperFactory mapperFactory) {
			if (mapperFactory != this.mapperFactory && mapperFactory != null) {
				mapperFactory.addPostInterceptor(mappingPostInterceptor);
				mapperFactory.addCustomizer(new MappingContextCustomizer() {

					@Override
					public void customize(MappingContext mappingContext) {
						mappingContext.addListener(new ParentAsChildWithListMappingPostInterceptor(schemaAdapter.getSchemaBeanFactory()));
					}
				});
			}
			this.mapperFactory = mapperFactory;
		}
		
	}
	
	@Reference
	private CacheLinkingService cacheLinkingService;
	@Reference
	private DefaultCharacterEncodingProvider defaultCharacterEncodingProvider;

	@Reference
	private ControllerResourceRegistry controllerResourceRegistry;
	@Reference
	private ConverterRegistry converterRegistry;

	private final List<DeployedSchema> deployedSchemas = new ArrayList<>();
	private final ReadWriteLock deployedSchemasLock = new ReentrantReadWriteLock();

	private ServiceTrackingFileExtensionContentTypeMapper fileExtensionContentTypeMapper;

	private final List<EntityResourceDeploymentListener> listeners = new ArrayList<>();
	private final ReadWriteLock listenersLock = new ReentrantReadWriteLock();
	
	@Reference
	private ParameterExtractor parameterExtractor;
	@Reference(
		cardinality = ReferenceCardinality.OPTIONAL
	)
	private CORSRequestDetector corsRequestDetector;

	@Reference(
			bind = "addEntityResourceDeploymentListener",
			unbind = "removeEntityResourceDeploymentListener",
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = EntityResourceDeploymentListener.class
	)
	public void addEntityResourceDeploymentListener(EntityResourceDeploymentListener listener) {
		if (listener != null) {
			listenersLock.writeLock().lock();
			try {
				listeners.add(listener);
				if (!deployedSchemas.isEmpty()) {
					for (DeployedSchema deployedSchema : deployedSchemas) {
						for (Object resource : deployedSchema.getRegisteredResources()) {
							if (EntityResource.class.isInstance(resource)) {
								EntityResource entityResource = (EntityResource) resource;
								listener.deployed(deployedSchema.getSchemaAdapter(), entityResource);
							}
						}
					}
				}
			} finally {
				listenersLock.writeLock().unlock();
			}
		}
	}
	public void removeEntityResourceDeploymentListener(EntityResourceDeploymentListener listener) {
		if (listener != null) {
			listenersLock.writeLock().lock();
			try {
				listeners.remove(listener);
			} finally {
				listenersLock.writeLock().unlock();
			}
		}
	}
	
	@Activate
	public void activate(ComponentContext componentContext) {
		bundleContext = componentContext.getBundleContext();
		schemaAdapterTracker = new ServiceTracker<SchemaAdapter, SchemaAdapter>(bundleContext, SchemaAdapter.class, null) {
			@Override
			public SchemaAdapter addingService(ServiceReference<SchemaAdapter> reference) {
				deployedSchemasLock.writeLock().lock();
				try {
					DeployedSchema deployedSchema = new DeployedSchema(super.addingService(reference));
					deployedSchemas.add(deployedSchema);
					activateDeployedSchema(deployedSchema);
					return deployedSchema.getSchemaAdapter();
				} finally {
					deployedSchemasLock.writeLock().unlock();
				}
			}

			@Override
			public void removedService(ServiceReference<SchemaAdapter> reference, SchemaAdapter service) {
				deployedSchemasLock.writeLock().lock();
				try {
					Iterator<DeployedSchema> iterator = deployedSchemas.iterator();
					while (iterator.hasNext()) {
						DeployedSchema next = iterator.next();
						
						if (next.getSchemaAdapter() == service) {
							iterator.remove();
							deactivateDeployedSchema(next);
						}
					}
				} finally {
					deployedSchemasLock.writeLock().unlock();
				}
				super.removedService(reference, service); //To change body of generated methods, choose Tools | Templates.
			}
			
			private void activateDeployedSchema(DeployedSchema deployedSchema) {
				createMapperFactory(deployedSchema);
				buildEntityResourcesForDeployedSchema(deployedSchema);
				registerMapperFactoryInContainer(deployedSchema);

				for (Object registeredResource : deployedSchema.getRegisteredResources()) {
					deployEntityResource(registeredResource, deployedSchema);
				}
			}
			
			private void deactivateDeployedSchema(DeployedSchema deployedSchema) {
				for (Object registeredResource : deployedSchema.getRegisteredResources()) {
					controllerResourceRegistry.undeploy(registeredResource);
					if (EntityResource.class.isInstance(registeredResource)) {
						EntityResource entityResource = (EntityResource) registeredResource;
						listenersLock.readLock().lock();
						try {
							for (EntityResourceDeploymentListener entityResourceDeploymentListener : listeners) {
								entityResourceDeploymentListener.undeployed(deployedSchema.getSchemaAdapter(), entityResource);
							}
						} finally {
							listenersLock.readLock().unlock();
						}
					}
				}
				for (ServiceRegistration registration : deployedSchema.getRegistrations()) {
					registration.unregister();
				}
				deployedSchema.getRegistrations().clear();
			}
			
		};
		schemaAdapterTracker.open();

		fileExtensionContentTypeMapper = new ServiceTrackingFileExtensionContentTypeMapper(bundleContext);
		fileExtensionContentTypeMapper.open();
	}
	
	@Deactivate
	public void deactivate(ComponentContext componentContext) {
		fileExtensionContentTypeMapper.close();
		fileExtensionContentTypeMapper = null;

		schemaAdapterTracker.close();
		schemaAdapterTracker = null;
		bundleContext = null;
	}
	
	private void buildEntityResourcesForDeployedSchema(DeployedSchema deployedSchema) {
		createMapperFactory(deployedSchema);
		Schema schema = deployedSchema.getSchemaAdapter().getSchema();
		if (schema == null) {
				return;
		}
		List<Type> types = schema.getTypes();
		if (types == null) {
			return;
		}
		FileExtensionContentTypeMapper proxyFileExtensionContentTypeMapper = new FileExtensionContentTypeMapper() {

			@Override
			public String mapExtensionToContentType(String extension) {
				if (fileExtensionContentTypeMapper == null) {
					return null;
				}
				return fileExtensionContentTypeMapper.mapExtensionToContentType(extension);
			}

			@Override
			public String mapContentTypeToExtension(String contentType) {
				if (fileExtensionContentTypeMapper == null) {
					return null;
				}
				return fileExtensionContentTypeMapper.mapContentTypeToExtension(contentType);
			}
		};
		for (Type type : types) {
			if (type.isAbstract()) {
				continue;
			}
			if (!type.isVirtual()) {
				EntityResource resource = createResourceForType(type, deployedSchema);
				if (resource != null) {
					List<Attribute> atts = SchemaUtil.collectAttributes(type);
					for (Attribute att : atts) {
						if (BinaryAttribute.class.equals(att.getClass())) {
							EntityBinaryAttributeResource entityBinaryAttributeResource = new EntityBinaryAttributeResource(
									resource, 
									(BinaryAttribute) att, 
									proxyFileExtensionContentTypeMapper
							);
							entityBinaryAttributeResource.setSchemaBeanFactory(deployedSchema.getSchemaAdapter().getSchemaBeanFactory());
							deployedSchema.getRegisteredResources().add(entityBinaryAttributeResource);
						}
					}
					deployedSchema.getRegisteredResources().add(resource);
				}
			} else {
				registerMapperObjects(type, deployedSchema);
			}
		}
	}
	
	private void deployEntityResource(Object resource, DeployedSchema deployedSchema) {
		EntityResource entityResource;
		boolean notifyListeners = false;
		final String baseUrlSuffix;
		if (EntityResource.class.isInstance(resource)) {
			entityResource = (EntityResource) resource;
			notifyListeners = true;
			baseUrlSuffix = "";
		} else if (EntityBinaryAttributeResource.class.isInstance(resource)) {
			EntityBinaryAttributeResource entityBinaryAttributeResource = (EntityBinaryAttributeResource) resource;
			entityResource = entityBinaryAttributeResource.getEntityResource();
			baseUrlSuffix = "/" + entityBinaryAttributeResource.getBinaryAttribute().getName();
		} else {
			LOG.warn("could not deploy resource, because it had an unsupported type: {}", resource.getClass().getName());
			return;
		}
		String baseUrl = entityResource.getType().getSchema().getName() + "/" + entityResource.getType().getName() + baseUrlSuffix;
		controllerResourceRegistry.deploy(resource, baseUrl);
		if (notifyListeners) {
			listenersLock.readLock().lock();
			try {
				for (EntityResourceDeploymentListener entityResourceDeploymentListener : listeners) {
					entityResourceDeploymentListener.deployed(deployedSchema.getSchemaAdapter(), (EntityResource) resource);
				}
			} finally {
				listenersLock.readLock().unlock();
			}
		}
	}

	private void registerMapperObjects(Type type, DeployedSchema deployedSchema) {
		try {
			SchemaAdapter schemaAdapter = deployedSchema.getSchemaAdapter();
			Class<?> listRestBean = schemaAdapter.getSchemaRestBeanClassLoader().loadClass(schemaAdapter.getSchemaRestBeanPackage() + "." + type.getName() + "ListRestBean");
			Class<?> referenceRestBean = schemaAdapter.getSchemaRestBeanClassLoader().loadClass(schemaAdapter.getSchemaRestBeanPackage() + "." + type.getName() + "ReferenceRestBean");
			Class<?> restBean = schemaAdapter.getSchemaRestBeanClassLoader().loadClass(schemaAdapter.getSchemaRestBeanPackage() + "." + type.getName() + "RestBean");
			Class<?> schemaBean = schemaAdapter.getSchemaBeanClassLoader().loadClass(schemaAdapter.getSchemaBeanPackage() + "." + type.getName());
			deployedSchema.getMapperFactory().buildAutoMappers(restBean, schemaBean);
			deployedSchema.getMapperFactory().buildAutoMappers(referenceRestBean, schemaBean);
			deployedSchema.getMapperFactory().buildCollection(listRestBean, restBean);
			SchemaBeanFactory schemaBeanFactory = schemaAdapter.getSchemaBeanFactory();
			IdFromURLMappingPostInterceptor interceptor = new IdFromURLMappingPostInterceptor(schemaBeanFactory, parameterExtractor, restBean, defaultCharacterEncodingProvider);
			deployedSchema.getInstancebuilder().addIdFromURLMappingPostInterceptor(interceptor);
			deployedSchema.getMappingPostInterceptor().addInterceptor(interceptor);
		} catch (ClassNotFoundException ex) {
			LOG.warn("could not find class while configuring mapper: " + ex.getMessage());
		}
	}
	
	private EntityResource createResourceForType(Type type, DeployedSchema deployedSchema) {
		try {
			registerMapperObjects(type, deployedSchema);
			SchemaAdapter schemaAdapter = deployedSchema.getSchemaAdapter();
			Class<?> listRestBean = schemaAdapter.getSchemaRestBeanClassLoader().loadClass(schemaAdapter.getSchemaRestBeanPackage() + "." + type.getName() + "ListRestBean");
			Class<?> referenceRestBean = schemaAdapter.getSchemaRestBeanClassLoader().loadClass(schemaAdapter.getSchemaRestBeanPackage() + "." + type.getName() + "ReferenceRestBean");
			Class<?> restBean = schemaAdapter.getSchemaRestBeanClassLoader().loadClass(schemaAdapter.getSchemaRestBeanPackage() + "." + type.getName() + "RestBean");
			Class<?> schemaBean = schemaAdapter.getSchemaBeanClassLoader().loadClass(schemaAdapter.getSchemaBeanPackage() + "." + type.getName());
			if (!type.isVirtual() && !type.isAbstract()) {
				EntityResource resource = new EntityResource(type, listRestBean, referenceRestBean, restBean, schemaBean);
				resource.setEngine(schemaAdapter.getEngine());
				resource.setCorsRequestDetector(new CORSRequestDetector() {

					@Override
					public boolean isCORSRequest() {
						if (corsRequestDetector == null) {
							return false;
						}
						return corsRequestDetector.isCORSRequest();
					}
				});
				resource.setDeletionStrategy(deployedSchema.getDeletionStrategy() == null ? schemaAdapter.getEngine().getAccessor() : deployedSchema.getDeletionStrategy());
				resource.setSchemaBeanFactory(schemaAdapter.getSchemaBeanFactory());
				resource.setMapperFactory(deployedSchema.getMapperFactory());
				resource.setConverterRegistry(converterRegistry);
				resource.setCacheLinkingService(new CacheLinkingService() {

					@Override
					public void link(String entryPath, String linkTargetPath) throws IOException {
						if (cacheLinkingService != null) {
							cacheLinkingService.link(entryPath, linkTargetPath);
						}
					}

					@Override
					public void iterateLinksOf(String entryPath, CacheLinkingService.Consumer consumer) throws IOException {
						if (cacheLinkingService != null) {
							cacheLinkingService.iterateLinksOf(entryPath, consumer);
						}
					}
				});
				if (deployedSchema.getConstraintList() != null) {
					for (AtomLinkConstraint atomLinkConstraint : deployedSchema.getConstraintList()) {
						if (atomLinkConstraint.supportsRestBeanType(restBean)) {
							resource.setAtomLinkConstraint(atomLinkConstraint);
							break;
						}
					}
				}
				return resource;
			}
		} catch (JAXBException ex) {
			LOG.warn("could not set up jaxb context for resource", ex);
		} catch (ClassNotFoundException ex) {
			LOG.warn("could not find class while creating entity resource: " + ex.getMessage());
		}
		return null;
	}

	private void createMapperFactory(DeployedSchema deployedSchema) {
		SchemaAdapter schemaAdapter = deployedSchema.getSchemaAdapter();
		MapperFactory mapperFactory = new MapperFactory();
		mapperFactory.register(new DomainCollectionAdapter());
		mapperFactory.register(new RestBeanCollectionAdapter());
		ComplexTypeDetectorImpl complexTypeDetector = new ComplexTypeDetectorImpl();
		mapperFactory.setComplexTypeDetector(complexTypeDetector);
		MappingContextKeyBuilderImpl contextkeybuilder = new MappingContextKeyBuilderImpl();
		contextkeybuilder.setComplexTypeDetector(complexTypeDetector);
		mapperFactory.addMappingContextKeyBuilder(contextkeybuilder);
		deployedSchema.getInstancebuilder().setSchemaBeanFactory(schemaAdapter.getSchemaBeanFactory());
		mapperFactory.setTypeInstanceBuilder(deployedSchema.getInstancebuilder());
		mapperFactory.setAmbiguityResolver(new MapperAmbiguityResolver() {

			@Override
			public Mapper pickMapper(Class<?> inputType, Map<Class<?>, Mapper> mappersForInput, Object inputObject) {
				if (ActiveRecord.class.isInstance(inputObject)) {
					ActiveRecord ar = (ActiveRecord) inputObject;
					if (ar.isReference()) {
						for (Map.Entry<Class<?>, Mapper> entry : mappersForInput.entrySet()) {
							Class<? extends Object> targetType = entry.getKey();
							if (targetType.isAnnotationPresent(org.bndly.rest.atomlink.api.annotation.Reference.class)) {
								return entry.getValue();
							}
						}
					} else {
						for (Map.Entry<Class<?>, Mapper> entry : mappersForInput.entrySet()) {
							Class<? extends Object> targetType = entry.getKey();
							if (!targetType.isAnnotationPresent(org.bndly.rest.atomlink.api.annotation.Reference.class)) {
								return entry.getValue();
							}
						}
					}
				} else if (StreamingObject.class.isInstance(inputObject)) {
					for (Map.Entry<Class<?>, Mapper> entry : mappersForInput.entrySet()) {
						Class<? extends Object> targetType = entry.getKey();
						if (!targetType.isAnnotationPresent(org.bndly.rest.atomlink.api.annotation.Reference.class)) {
							return entry.getValue();
						}
					}
				}
				return null;
			}
		});
		deployedSchema.setMapperFactory(mapperFactory);
	}
	
	private void registerMapperFactoryInContainer(DeployedSchema deployedSchema) {
		MapperFactory mapperFactory = deployedSchema.getMapperFactory();
		Dictionary properties = new Hashtable();
		String pid = MapperFactory.class.getName() + "." + deployedSchema.getSchemaAdapter().getName();
		properties.put("service.pid", pid);
		properties.put("schema", deployedSchema.getSchemaAdapter().getName());
		ServiceRegistration reg = bundleContext.registerService(MapperFactory.class, mapperFactory, properties);
		deployedSchema.getRegistrations().add(reg);
	}

	public void setParameterExtractor(ParameterExtractor parameterExtractor) {
		this.parameterExtractor = parameterExtractor;
	}

	public void setCorsRequestDetector(CORSRequestDetector corsRequestDetector) {
		this.corsRequestDetector = corsRequestDetector;
	}

	public void setControllerResourceRegistry(ControllerResourceRegistry controllerResourceRegistry) {
		this.controllerResourceRegistry = controllerResourceRegistry;
	}

}
