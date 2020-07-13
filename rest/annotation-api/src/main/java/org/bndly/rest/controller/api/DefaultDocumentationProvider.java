package org.bndly.rest.controller.api;

/*-
 * #%L
 * REST Annotation API
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

import org.bndly.rest.api.StatusWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class DefaultDocumentationProvider implements DocumentationProvider {

	protected boolean isLoggingEnabled() {
		return false;
	}
	protected void logWarn(String message) {}
	protected void logError(String message, Exception e) {}
	
	@Override
	public DocumentationInfo getDocumentationInfo(DocumentationProvider.Context context) {
		Documentation documentation = context.getDocumentation();
		Object controller = context.getController();
		
		if (documentation == null) {
			return null;
		}
		Class<?>[] decoratorTypes = documentation.documentationDecorator();
		DocumentationInfo currentDocumentationInfo = createAnnotationBasedDocumentationInfo(context);
		for (int i = decoratorTypes.length - 1; i >= 0; i--) {
			Class<?> decoratorType = decoratorTypes[i];
			if (DocumentationDecorator.class.isAssignableFrom(decoratorType)) {
				Constructor defaultConstructor = getDefaultConstructor(decoratorType);
				DocumentationDecorator providerInstance = null;
				try {
					if (defaultConstructor == null) {
						if (controller != null) {
							Constructor controllerBasedConstructor = getControllerBasedConstructor(decoratorType, controller);
							if (controllerBasedConstructor != null) {
								providerInstance = (DocumentationDecorator) controllerBasedConstructor.newInstance(controller);
							} else {
								if (isLoggingEnabled()) {
									logWarn(
										"could not find default constructor or controller based constructor in documentation provider class: "
												+ decoratorType.getName()
									);
								}
								return null;
							}
						} else {
							if (isLoggingEnabled()) {
								logWarn(
									"could not find default constructor and controller based constructor was not possible, "
									+ "because the controller was not provided: " + decoratorType.getName()
								);
							}
							return null;
						}
					} else {
						providerInstance = (DocumentationDecorator) defaultConstructor.newInstance();
					}
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					if (isLoggingEnabled()) {
						logError("failed to instantiate documentation provider: " + e.getMessage(), e);
					}
				}
				currentDocumentationInfo = providerInstance == null ? currentDocumentationInfo : providerInstance.decorateDocumentationInfo(context, currentDocumentationInfo);
			}			
		}
		return currentDocumentationInfo;
	}

	protected DocumentationInfo createAnnotationBasedDocumentationInfo(final DocumentationProvider.Context context) {
		final Method method = context.getMethod();
		final Documentation documentation = context.getDocumentation();
		
		DocumentationResponse[] r = context.getDocumentation().responses();
		final List<DocumentationInfo.ResponseInfo> tmp = new ArrayList<>();
		for (DocumentationResponse resp : r) {
			tmp.add(createAnnotationBasedResponseInfo(resp));
		}
		
		List<DocumentationInfo.GenericParameterInfo> queryParameterInfos = null;
		List<DocumentationInfo.GenericParameterInfo> pathParameterInfos = null;
		DocumentationInfo.BodyParameterInfo bodyParameterInfo = null;
		
		Type[] parameterTypes = method.getGenericParameterTypes();
		Annotation[][] parameterAnnotations = method.getParameterAnnotations();
		for (int i = 0; i < parameterTypes.length; i++) {
			Type parameterType = parameterTypes[i];
			Annotation[] annotations = parameterAnnotations[i];
			QueryParam queryParam = null;
			PathParam pathParam = null;
			Documentation paramDocumentation = null;
			Meta meta = null;
			for (Annotation annotation : annotations) {
				if (QueryParam.class.isInstance(annotation)) {
					queryParam = (QueryParam) annotation;
				} else if (PathParam.class.isInstance(annotation)) {
					pathParam = (PathParam) annotation;
				} else if (Documentation.class.isInstance(annotation)) {
					paramDocumentation = (Documentation) annotation;
				} else if (Meta.class.isInstance(annotation)) {
					meta = (Meta) annotation;
				}
			}
			if (queryParam != null) {
				DocumentationInfo.GenericParameterInfo pi = createGenericParameterInfo(queryParam, paramDocumentation, parameterType);
				if (queryParameterInfos == null) {
					queryParameterInfos = new ArrayList<>();
				}
				queryParameterInfos.add(pi);

			} else if (pathParam != null) {
				DocumentationInfo.GenericParameterInfo pi = createGenericParameterInfo(pathParam, paramDocumentation, parameterType);
				if (pathParameterInfos == null) {
					pathParameterInfos = new ArrayList<>();
				}
				pathParameterInfos.add(pi);
			} else if (meta == null) {
				// if meta is null, then the type is deserialized from the payload
				bodyParameterInfo = createBodyParameterInfo(paramDocumentation, parameterType);

			}
		}

		
		final List<DocumentationInfo.GenericParameterInfo> q = queryParameterInfos == null ? Collections.EMPTY_LIST : queryParameterInfos;
		final List<DocumentationInfo.GenericParameterInfo> p = pathParameterInfos == null ? Collections.EMPTY_LIST : pathParameterInfos;
		final DocumentationInfo.BodyParameterInfo b = bodyParameterInfo;
		
		return new DocumentationInfo() {
			private final String summary = documentation.summary();
			private final String text = documentation.value();
			private final List<String> authors = Arrays.asList(documentation.authors());
			private final List<String> produces = Arrays.asList(documentation.produces());
			private final List<String> consumes = Arrays.asList(documentation.consumes());
			private final List<String> tags = Arrays.asList(documentation.tags());
			private final List<DocumentationInfo.ResponseInfo> responses = Collections.unmodifiableList(tmp);

			@Override
			public List<DocumentationInfo.GenericParameterInfo> getQueryParameters() {
				return q;
			}

			@Override
			public List<DocumentationInfo.GenericParameterInfo> getPathParameters() {
				return p;
			}

			@Override
			public DocumentationInfo.BodyParameterInfo getBodyParameter() {
				return b;
			}

			@Override
			public String getText() {
				return text;
			}

			@Override
			public String getSummary() {
				return summary;
			}

			@Override
			public List<String> getAuthors() {
				return authors;
			}

			@Override
			public List<String> getProduces() {
				return produces;
			}

			@Override
			public List<String> getConsumes() {
				return consumes;
			}

			@Override
			public List<String> getTags() {
				return tags;
			}

			@Override
			public List<DocumentationInfo.ResponseInfo> getResponses() {
				return responses;
			}

			@Override
			public List<DocumentationExample> getExamples() {
				return context.getExamplesFromProviders();
			}

		};
	}
	
	protected DocumentationInfo.GenericParameterInfo createGenericParameterInfo(final QueryParam queryParam, Documentation documentation, final Type parameterType) {
		final String desc = documentation == null ? null : documentation.value();
		return new DocumentationInfo.GenericParameterInfo() {

			@Override
			public String getName() {
				return queryParam.value();
			}

			@Override
			public String getDescription() {
				return desc;
			}

			@Override
			public Boolean isRequired() {
				return null; // This requires some implementation. Skipping this due to a lack of time. :(
			}

			@Override
			public Type getJavaType() {
				return parameterType;
			}
		};
	}
	
	protected DocumentationInfo.GenericParameterInfo createGenericParameterInfo(final PathParam pathParam, Documentation documentation, final Type parameterType) {
		final String desc = documentation == null ? null : documentation.value();
		return new DocumentationInfo.GenericParameterInfo() {

			@Override
			public String getName() {
				return pathParam.value();
			}

			@Override
			public String getDescription() {
				return desc;
			}

			@Override
			public Boolean isRequired() {
				return null; // This requires some implementation. Skipping this due to a lack of time. :(
			}

			@Override
			public Type getJavaType() {
				return parameterType;
			}
		};
	}
	
	protected DocumentationInfo.BodyParameterInfo createBodyParameterInfo(Documentation documentation, final Type parameterType) {
		final String desc = documentation == null ? null : documentation.value();
		return new DocumentationInfo.BodyParameterInfo() {

			@Override
			public String getSchemaElementName() {
				if (Class.class.isInstance(parameterType)) {
					return ((Class) parameterType).getName();
				}
				return null;
			}

			@Override
			public String getName() {
				return "body";
			}

			@Override
			public String getDescription() {
				return desc;
			}

			@Override
			public Boolean isRequired() {
				// those body parameters are always required.
				return Boolean.TRUE;
			}

			@Override
			public Type getJavaType() {
				return parameterType;
			}
		};
	}

	private DocumentationInfo.ResponseInfo createAnnotationBasedResponseInfo(final DocumentationResponse resp) {
		final Type javaType = resp.messageType().equals(Void.class) ? null : resp.messageType();
		return new DocumentationInfo.ResponseInfo() {

			@Override
			public StatusWriter.Code getCode() {
				return resp.code();
			}

			@Override
			public String getDescription() {
				return resp.description();
			}

			@Override
			public Type getJavaType() {
				return javaType;
			}
		};
	}

	private Constructor getDefaultConstructor(Class<?> prov) {
		try {
			Constructor<?> constructor = prov.getConstructor();
			return constructor;
		} catch (NoSuchMethodException | SecurityException ex) {
			// this is not a big deal. we will look for another constructor later.
			return null;
		}
	}

	private Constructor getControllerBasedConstructor(Class<?> prov, Object controller) {
		try {
			Constructor<?> constructor = prov.getConstructor(controller.getClass());
			return constructor;
		} catch (NoSuchMethodException | SecurityException ex) {
			// this is not a big deal. we will log a warning later.
			return null;
		}
	}

}
