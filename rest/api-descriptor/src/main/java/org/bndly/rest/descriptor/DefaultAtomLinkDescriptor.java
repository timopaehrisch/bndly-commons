package org.bndly.rest.descriptor;

/*-
 * #%L
 * REST API Descriptor
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

import org.bndly.rest.descriptor.util.JAXRSUtil;
import org.bndly.rest.descriptor.util.TypeUtil;
import org.bndly.rest.atomlink.api.annotation.AtomLink;
import org.bndly.rest.atomlink.api.annotation.AtomLinkDescription;
import org.bndly.rest.atomlink.api.annotation.AtomLinkDescriptor;
import org.bndly.rest.atomlink.api.annotation.PathParameter;
import org.bndly.rest.atomlink.api.annotation.QueryParameter;
import org.bndly.rest.controller.api.DELETE;
import org.bndly.rest.controller.api.Documentation;
import org.bndly.rest.controller.api.GET;
import org.bndly.rest.controller.api.POST;
import org.bndly.rest.controller.api.PUT;
import org.bndly.rest.controller.api.PathParam;
import org.bndly.rest.controller.api.QueryParam;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;

public class DefaultAtomLinkDescriptor implements AtomLinkDescriptor {

	@Override
	public AtomLinkDescription getAtomLinkDescription(final Object controller, final Method method, final AtomLink atomLink) {
		Class<?> tmpLinkedClass = atomLink.target();
		if (Void.class.equals(tmpLinkedClass)) {
			String className = atomLink.targetName();
			if (!"".equals(className)) {
				try {
					tmpLinkedClass = getClass().getClassLoader().loadClass(className);
				} catch (ClassNotFoundException ex) {
					// if this is the case, we don't care. than consider this link to be non-existent
					tmpLinkedClass = null;
				}
			}
		}
		Class<?> tmpReturnType = atomLink.returns();
		if (Void.class.equals(tmpReturnType)) {
			tmpReturnType = JAXRSUtil.extractImplicitReturnType(method);
		}
		final boolean isContextExtensionEnabled = atomLink.isContextExtensionEnabled();
		final boolean allowSubclasses = atomLink.allowSubclasses();
		final Class<?> returnType = tmpReturnType;
		final Class<?> linkedClass = tmpLinkedClass;
		if (linkedClass != null) {
			final String constraint = atomLink.constraint();
			Class<?> tmpConsumedType = null;
			Class<?>[] parameterTypes = method.getParameterTypes();
			if (parameterTypes != null) {
				for (Class<?> pType : parameterTypes) {
					if (!TypeUtil.isSimpleType(pType) && pType.isAnnotationPresent(XmlRootElement.class)) {
						tmpConsumedType = pType;
					}
				}
			}
			Documentation doc = method.getAnnotation(Documentation.class);
			final String desc = doc == null ? null : doc.value();
			final String[] authors = doc == null ? null : doc.authors();
			final Class<?> consumedType = tmpConsumedType;

			final List<QueryParameter> queryParameters = new ArrayList<>();
			final List<PathParameter> pathParameters = new ArrayList<>();
			JAXRSUtil.iterateAnnotationsFromMethod(method, new JAXRSUtil.JavaMethodParameterIteratorCallback() {

				@Override
				public void onParameter(JAXRSUtil.JavaMethodParameterIteratorCallback.Context context) {
					final Documentation parameterDocumentation = context.getAnnotation(Documentation.class);
					final QueryParam qp = context.getAnnotation(QueryParam.class);
					if (qp != null) {
						queryParameters.add(new QueryParameterImpl(qp, parameterDocumentation != null ? parameterDocumentation.value() : null));
					} else {
						final PathParam pp = context.getAnnotation(PathParam.class);
						if (pp != null) {
							pathParameters.add(new PathParameter() {

								@Override
								public String getName() {
									return pp.value();
								}

								@Override
								public String getHumanReadableDescription() {
									return parameterDocumentation != null ? parameterDocumentation.value() : null;
								}
							});
						}
					}
				}
			});

			final String seg = atomLink.segment().isEmpty() ? null : atomLink.segment();
			return new AtomLinkDescription() {

				@Override
				public Class<?> getLinkedInClass() {
					return linkedClass;
				}

				@Override
				public String getHumanReadableDescription() {
					return desc;
				}

				@Override
				public String[] getAuthors() {
					return authors;
				}

				@Override
				public Class<?> getReturnType() {
					return returnType;
				}

				@Override
				public Class<?> getConsumesType() {
					return consumedType;
				}

				@Override
				public String getConstraint() {
					return constraint;
				}

				@Override
				public String getSegment() {
					return seg;
				}

				@Override
				public String getRel() {
					String rel = atomLink.rel();
					if ("".equals(rel)) {
						if (method.getAnnotation(GET.class) != null) {
							rel = "self";
						} else if (method.getAnnotation(POST.class) != null) {
							rel = "add";
						} else if (method.getAnnotation(PUT.class) != null) {
							rel = "update";
						} else if (method.getAnnotation(DELETE.class) != null) {
							rel = "remove";
						}
					}
					if ("".equals(rel)) {
						throw new IllegalStateException("FOUND LINK WITHOUT REL: " + atomLink.toString() + " DECLARED IN " + method.getDeclaringClass().getSimpleName());
					}
					return rel;
				}

				@Override
				public Method getControllerMethod() {
					return method;
				}

				@Override
				public Object getController() {
					return controller;
				}

				@Override
				public AtomLink getAtomLink() {
					return atomLink;
				}

				@Override
				public String getHttpMethod() {
					String httpMethod = null;
					if (method.getAnnotation(GET.class) != null) {
						httpMethod = "GET";
					} else if (method.getAnnotation(POST.class) != null) {
						httpMethod = "POST";
					} else if (method.getAnnotation(PUT.class) != null) {
						httpMethod = "PUT";
					} else if (method.getAnnotation(DELETE.class) != null) {
						httpMethod = "DELETE";
					}
					return httpMethod;
				}

				@Override
				public List<QueryParameter> getQueryParams() {
					return queryParameters;
				}

				@Override
				public List<PathParameter> getPathParams() {
					return pathParameters;
				}

				@Override
				public boolean isContextExtensionEnabled() {
					return isContextExtensionEnabled;
				}

				@Override
				public boolean allowSubclasses() { return allowSubclasses; }

			};
		}
		return null;
	}

	@Override
	public AtomLinkDescription getAtomLinkDescription(final Method method, final AtomLink atomLink) {
		return getAtomLinkDescription(null, method, atomLink);
	}
}
