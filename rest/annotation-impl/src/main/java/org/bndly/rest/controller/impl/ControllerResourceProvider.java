package org.bndly.rest.controller.impl;

/*-
 * #%L
 * REST Controller Impl
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

import org.bndly.common.converter.api.ConversionException;
import org.bndly.common.converter.api.Converter;
import org.bndly.common.converter.api.ConverterRegistry;
import org.bndly.common.data.io.ReplayableInputStream;
import org.bndly.rest.api.CacheContext;
import org.bndly.rest.api.Context;
import org.bndly.rest.controller.api.ControllerBinding;
import org.bndly.rest.controller.api.ControllerResourceRegistry;
import org.bndly.rest.controller.api.ExceptionMapper;
import org.bndly.rest.controller.api.Meta;
import org.bndly.rest.controller.api.PathParam;
import org.bndly.rest.controller.api.QueryParam;
import org.bndly.rest.api.Resource;
import org.bndly.rest.api.ResourceBuildingException;
import org.bndly.rest.api.ResourceProvider;
import org.bndly.rest.api.ResourceURI;
import org.bndly.rest.controller.api.CacheControl;
import org.bndly.rest.controller.api.EntityParser;
import org.bndly.rest.controller.api.Response;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.xml.bind.annotation.XmlRootElement;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = ResourceProvider.class)
public class ControllerResourceProvider implements ResourceProvider {

	@Reference
	private ControllerResourceRegistry controllerResourceRegistry;
	
	private final List<EntityParser> entityParsers = new ArrayList<>();
	private final ReadWriteLock entityParsersLock = new ReentrantReadWriteLock();

	@Reference
	private ConverterRegistry converterRegistry;
	
	private final DelegatingExceptionMapper exceptionMapper = new DelegatingExceptionMapper();

	@Activate
	public void activate() {
	}
	
	@Deactivate
	public void deactivate() {
		exceptionMapper.clear();
	}
	
	@Reference(
			bind = "addEntityParser",
			unbind = "removeEntityParser",
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = EntityParser.class
	)
	public void addEntityParser(EntityParser entityParser) {
		if (entityParser != null) {
			entityParsersLock.writeLock().lock();
			try {
				entityParsers.add(entityParser);
			} finally {
				entityParsersLock.writeLock().unlock();
			}
		}
	}

	public void removeEntityParser(EntityParser entityParser) {
		if (entityParser != null) {
			entityParsersLock.writeLock().lock();
			try {
				Iterator<EntityParser> iterator = entityParsers.iterator();
				while (iterator.hasNext()) {
					EntityParser next = iterator.next();
					if (next == entityParser) {
						iterator.remove();
					}
				}
			} finally {
				entityParsersLock.writeLock().unlock();
			}
		}
	}
	
	@Reference(
			bind = "addExceptionMapper",
			unbind = "removeExceptionMapper", 
			cardinality = ReferenceCardinality.MULTIPLE,
			policy = ReferencePolicy.DYNAMIC,
			service = ExceptionMapper.class
	)
	public void addExceptionMapper(ExceptionMapper exceptionMapper) {
		if (exceptionMapper != null) {
			this.exceptionMapper.registerMapper(exceptionMapper);
		}
	}

	public void removeExceptionMapper(ExceptionMapper exceptionMapper) {
		if (exceptionMapper != null) {
			this.exceptionMapper.unregisterMapper(exceptionMapper);
		}
	}
	
	@Override
	public boolean supports(Context context) {
		ControllerBinding binding = controllerResourceRegistry.resolveBindingForResourceURI(context.getURI(), context.getMethod());
		return binding != null;
	}

	@Override
	public Resource build(Context context, ResourceProvider provider) throws ResourceBuildingException {
		ControllerBinding binding = controllerResourceRegistry.resolveBindingForResourceURI(context.getURI(), context.getMethod());
		Method method = binding.getMethod();
		// parse the arguments from URI or payload
		Object[] args;
		try {
			args = collectArgumentForInvocationOfBinding(binding, context);
		} catch (IOException e) {
			throw new ResourceBuildingException(context, "could build arguments for controller invocation.");
		}
		try {
			CacheControl cc = method.getAnnotation(CacheControl.class);
			if (cc == null) {
				cc = method.getDeclaringClass().getAnnotation(CacheControl.class);
			}
			if (cc != null) {
				CacheContext cacheContext = context.getCacheContext();
				if (cacheContext != null) {
					if (cc.preventCaching()) {
						cacheContext.preventCache();
					}
					if (cc.maxAge() > -1) {
						cacheContext.setMaxAge(cc.maxAge());
					}
				}
			}
			Object result = method.invoke(binding.getController(), args);
			if (Response.class.isInstance(result)) {
				Object entity = ((Response) result).getEntity();
				if (entity != null) {
					Response resp = (Response) result;
					if (entity.getClass().isAnnotationPresent(XmlRootElement.class)) {
						return new JAXBControllerResource(provider, context.getURI(), binding, resp);
					} else if (InputStream.class.isInstance(entity)) {
						return new StreamWritingResource(provider, context.getURI(), resp);
					} else if (Resource.class.isInstance(entity)) {
						return (Resource) entity;
					}
				}
			} else if (Resource.class.isInstance(result)) {
				return (Resource) result;
			}
			return new ControllerResource(provider, context.getURI(), binding, result);
		} catch (InvocationTargetException ex) {
			Response r = exceptionMapper.toResponse(ex.getTargetException());
			if (r != null) {
				Object entity = r.getEntity();
				if (entity != null && entity.getClass().isAnnotationPresent(XmlRootElement.class)) {
					return new JAXBControllerResource(provider, context.getURI(), binding, r);
				}
			}
			throw new ResourceBuildingException(context, ex);
		} catch (IllegalAccessException | IllegalArgumentException ex) {
			throw new ResourceBuildingException(context, ex);
		}
	}

	public void setControllerResourceRegistry(ControllerResourceRegistry controllerResourceRegistry) {
		this.controllerResourceRegistry = controllerResourceRegistry;
	}

	private Object[] collectArgumentForInvocationOfBinding(ControllerBinding binding, Context context) throws IOException {
		Method method = binding.getMethod();
		Class<?>[] pt = method.getParameterTypes();
		if (pt == null || pt.length == 0) {
			return null;
		}

		Annotation[][] ptAnnotations = method.getParameterAnnotations();
		Object[] args = new Object[pt.length];

		for (int i = 0; i < pt.length; i++) {
			Class<?> parameterType = pt[i];
			parameterType = replacePrimitiveWithAutoboxedType(parameterType);
			Annotation[] parameterAnnotations = ptAnnotations[i];
			boolean found = false;
			for (Annotation annotation : parameterAnnotations) {
				if (PathParam.class.isInstance(annotation)) {
					// convert path parameter
					Converter converter = converterRegistry.getConverter(String.class, parameterType);
					if (converter != null) {

						int indexOfParameterInPath = findIndexOfPathParameter(((PathParam) annotation).value(), binding.getResourceURIPattern());
						// TODO: find parameters in the path
						String pathParameter = null;
						if (indexOfParameterInPath > -1) {
							pathParameter = context.getURI().getPath().getElements().get(indexOfParameterInPath);
						}

						try {
							args[i] = converter.convert(pathParameter);
						} catch (ConversionException ex) {
							throw new IllegalArgumentException("could not convert path parameter", ex);
						}
					}
					found = true;
				} else if (QueryParam.class.isInstance(annotation)) {
					// convert query parameter
					QueryParam queryParamAnnotation = ((QueryParam) annotation);
					String name = queryParamAnnotation.value();
					if (queryParamAnnotation.asSelector()) {
						List<ResourceURI.Selector> selectors = context.getURI().getSelectors();
						if (selectors != null) {
							for (ResourceURI.Selector selector : selectors) {
								if (selector.getName().startsWith(name)) {
									try {
										String raw = selector.getName().substring(name.length());
										found = convertRawParameter(raw, parameterType, args, i);
										if (found) {
											break;
										}
									} catch (ConversionException ex) {
										// lets try the query parameters
									}
								}
							}
						}
					}
					if (!found) {
						try {
							found = getParameterFromRequestParameter(context, name, parameterType, args, i);
						} catch (ConversionException ex2) {
							throw new IllegalArgumentException("could not convert query parameter " + name, ex2);
						}
					}
					found = true;
				} else if (Meta.class.isInstance(annotation)) {
					// provide meta data of execution
					if (Context.class.isAssignableFrom(parameterType)) {
						args[i] = context;
					} else if (ResourceURI.class.isAssignableFrom(parameterType)) {
						args[i] = context.getURI();
					} else if (ResourceProvider.class.isAssignableFrom(parameterType)) {
						args[i] = this;
					}
					found = true;
				}
			}
			if (found) {
				continue;
			}
			
			args[i] = buildParameterFromPayload(context, parameterType);
		}

		return args;
	}
	
	private boolean getParameterFromRequestParameter(Context context, String name, Class parameterType, Object[] args, int i) throws ConversionException {
		ResourceURI.QueryParameter param = context.getURI().getParameter(name);
		if (param != null) {
			String v = param.getValue();
			return convertRawParameter(v, parameterType, args, i);
		}
		return false;
	}

	private boolean convertRawParameter(String raw, Class parameterType, Object[] args, int i) throws ConversionException {
		Converter converter = converterRegistry.getConverter(String.class, parameterType);
		if (converter != null) {
			args[i] = converter.convert(raw);
			return true;
		}
		return false;
	}

	private Object buildParameterFromPayload(Context context, Class<?> parameterType) throws IOException, IllegalArgumentException {
		// if it is no annotated parameter, we assume it is a payload
		ReplayableInputStream is = context.getInputStream();
		if (context.getInputContentType() == null) {
			throw new IllegalArgumentException("can not parse content without content-type header.");
		}
		entityParsersLock.readLock().lock();
		try {
			Iterator<EntityParser> it = entityParsers.iterator();
			while (it.hasNext()) {
				EntityParser parser = it.next();
				String inputCtName = context.getInputContentType().getName();
				if (inputCtName != null) {
					if (inputCtName.startsWith(parser.getSupportedContentType().getName())) {
						return parser.parse(is, parameterType);
					}
				}
			}
		} finally {
			entityParsersLock.readLock().unlock();
		}
		throw new IllegalArgumentException("unsupported content type: " + context.getInputContentType().getName());
	}

	private Class<?> replacePrimitiveWithAutoboxedType(Class<?> parameterType) {
		if (parameterType.isPrimitive()) {
			if (byte.class.equals(parameterType)) {
				parameterType = Byte.class;
			} else if (short.class.equals(parameterType)) {
				parameterType = Short.class;
			} else if (int.class.equals(parameterType)) {
				parameterType = Integer.class;
			} else if (long.class.equals(parameterType)) {
				parameterType = Long.class;
			} else if (float.class.equals(parameterType)) {
				parameterType = Float.class;
			} else if (double.class.equals(parameterType)) {
				parameterType = Double.class;
			} else if (char.class.equals(parameterType)) {
				parameterType = Character.class;
			}
		}
		return parameterType;
	}

	private int findIndexOfPathParameter(String name, ResourceURI resourceURIPattern) {
		ResourceURI.Path p = resourceURIPattern.getPath();
		if (p != null) {
			for (int i = 0; i < p.getElements().size(); i++) {
				String element = p.getElements().get(i);
				if (controllerResourceRegistry.isVariableElementOfName(element, name)) {
					return i;
				}
			}
		}
		return -1;
	}
	
	public void setConverterRegistry(ConverterRegistry converterRegistry) {
		this.converterRegistry = converterRegistry;
	}

}
