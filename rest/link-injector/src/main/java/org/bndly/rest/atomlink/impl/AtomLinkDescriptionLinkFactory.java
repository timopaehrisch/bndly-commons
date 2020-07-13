package org.bndly.rest.atomlink.impl;

/*-
 * #%L
 * REST Link Injector
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
import org.bndly.common.converter.api.ConverterRegistry;
import org.bndly.common.reflection.ReflectionUtil;
import org.bndly.rest.api.ContentType;
import org.bndly.rest.api.Context;
import org.bndly.rest.api.ContextProvider;
import org.bndly.rest.api.PathCoder;
import org.bndly.rest.api.ResourceURI;
import org.bndly.rest.api.ResourceURIBuilder;
import org.bndly.rest.atomlink.api.AtomLinkBean;
import org.bndly.rest.atomlink.api.Fragment;
import org.bndly.rest.atomlink.api.LinkFactory;
import org.bndly.rest.atomlink.api.VariableFragment;
import org.bndly.rest.atomlink.api.annotation.AtomLink;
import org.bndly.rest.atomlink.api.annotation.AtomLinkDescription;
import org.bndly.rest.atomlink.api.annotation.BeanID;
import org.bndly.rest.atomlink.api.annotation.CompiledAtomLinkDescription;
import org.bndly.rest.atomlink.api.annotation.Parameter;
import org.bndly.rest.atomlink.api.annotation.PathParameter;
import org.bndly.rest.atomlink.api.annotation.QueryParameter;
import org.bndly.rest.controller.api.Path;
import org.bndly.rest.descriptor.QueryParameterImpl;
import de.odysseus.el.ExpressionFactoryImpl;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.PropertyNotFoundException;
import javax.el.ValueExpression;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class AtomLinkDescriptionLinkFactory implements LinkFactory {
	private static final ExpressionFactory EXPRESSION_FACTORY = new ExpressionFactoryImpl();

	private final CompiledAtomLinkDescription atomLinkDescription;
	private final ContextProvider contextProvider;
	private final ConverterRegistry converterRegistry;
	private final BeanIdGetter beanIdGetter;

	public AtomLinkDescriptionLinkFactory(CompiledAtomLinkDescription atomLinkDescription, ContextProvider contextProvider, ConverterRegistry converterRegistry) {
		this.atomLinkDescription = atomLinkDescription;
		if (atomLinkDescription == null) {
			throw new IllegalArgumentException("atomLinkDescription is not allowed to be null");
		}
		this.contextProvider = contextProvider;
		if (contextProvider == null) {
			throw new IllegalArgumentException("contextProvider is not allowed to be null");
		}
		this.converterRegistry = converterRegistry;
		if (converterRegistry == null) {
			throw new IllegalArgumentException("converterRegistry is not allowed to be null");
		}
		beanIdGetter = createBeanIdGetter(atomLinkDescription.getLinkedInClass());
	}

	public final CompiledAtomLinkDescription getAtomLinkDescription() {
		return atomLinkDescription;
	}

	@Override
	public Class getTargetType() {
		return atomLinkDescription.getLinkedInClass();
	}

	@Override
	public boolean isSupportingSubTypes() {
		return atomLinkDescription.allowSubclasses();
	}

	@Override
	public Iterator<AtomLinkBean> buildLinks(Object targetBean, boolean isMessageRoot) {
		Context ctx = contextProvider.getCurrentContext();
		if (ctx != null) {
			List<AtomLinkBean> links = new ArrayList<>();
			ELContext elContext;
			if (targetBean != null) {
				elContext = ELUtil.createELContext(targetBean, targetBean.getClass(), EXPRESSION_FACTORY);
			} else {
				elContext = ELUtil.createELContext(EXPRESSION_FACTORY);
			}
			ValueExpression atomLinkDescriptionExpression = EXPRESSION_FACTORY.createValueExpression(atomLinkDescription, AtomLinkDescription.class);
			elContext.getVariableMapper().setVariable("linkDescription", atomLinkDescriptionExpression);
			Object controller = atomLinkDescription.getController();
			if (controller != null) {
				ValueExpression controllerExpression = EXPRESSION_FACTORY.createValueExpression(controller, controller.getClass());
				elContext.getVariableMapper().setVariable("controller", controllerExpression);
			}
			AtomLinkBean link = processLinkResource(atomLinkDescription, targetBean, ctx, isMessageRoot, false, elContext);
			if (link != null) {
				links.add(link);
			}
			return links.iterator();
		}
		return Collections.EMPTY_LIST.iterator();
	}
	
	@Override
	public Iterator buildLinks() {
		return buildLinks(null, true);
	}

	private AtomLinkBean processLinkResource(CompiledAtomLinkDescription binding, Object entity, Context context, boolean isRoot, boolean failOnServiceDiscoveryException, ELContext elContext) {
		boolean c = checkConstraint(binding, elContext);
		if (c) {
			return addInstanceService(binding, entity, context, isRoot, failOnServiceDiscoveryException, elContext);
		}
		return null;
	}

	private boolean checkConstraint(AtomLinkDescription binding, ELContext elContext) {
		String constraint = binding.getConstraint();
		Method m = binding.getControllerMethod();
		if (constraint == null || constraint.length() == 0) {
			return true;
		}
		Boolean ret = ELUtil.evaluateELBoolean(m, elContext, constraint, EXPRESSION_FACTORY);
		return ret != null && ret;
	}

	private AtomLinkBean addInstanceService(
			CompiledAtomLinkDescription binding, 
			Object entity, 
			Context context, 
			boolean isRoot, 
			boolean failOnServiceDiscoveryException, 
			ELContext elContext
	) {
		Method m = binding.getControllerMethod();
		ResourceURIBuilder uriBuilder = context.createURIBuilder();
		try {
			try {
				String uri = buildURI(uriBuilder, context, binding, entity, m, isRoot, elContext);
				if (uri != null) {
					return createAtomLinkBean(uri);
				}
			} catch (ConversionException e) {
				throw new ServiceDiscoveryException(m, e.getMessage(), e);
			}
		} catch (ServiceDiscoveryException e) {
			if (failOnServiceDiscoveryException) {
				throw e;
			} else {
				return null;
			}
		} catch (PropertyNotFoundException e) {
			// i don't care in this case. probably a property of a specialized type was required.
		}
		return null;
	}

	private String buildURI(
			ResourceURIBuilder uriBuilder, 
			Context context, 
			CompiledAtomLinkDescription binding, 
			Object entity, 
			Method m, 
			boolean isRoot, 
			ELContext elContext
	) throws ConversionException {
		String uriTemplate = binding.getUriTemplate();

		// collect all BeanID values of the entity in "parent-to-child"-order
		List<Object> beanIds = collectBeanIdsFromEntity(entity);

		StringBuffer sb = new StringBuffer();
		String segString = binding.getSegment();
		if (segString != null && !"".equals(segString)) {
			if (!segString.startsWith("/")) {
				sb.append('/');
			}
			sb.append(segString);
		}
		Class<?> resource = binding.getControllerMethod().getDeclaringClass();
		if (resource.isAnnotationPresent(Path.class)) {
			Path p = resource.getAnnotation(Path.class);
			appendPathAnnotationToPathElements(p, sb);
		}
		Method method = binding.getControllerMethod();
		if (method.isAnnotationPresent(Path.class)) {
			appendPathAnnotationToPathElements(method.getAnnotation(Path.class), sb);
		}

		ResourceURI templateURI = context.parseURI(sb.toString());
		List<QueryParameter> queryParams = binding.getQueryParams();
		Map<String, QueryParameterAppender> queryParameterApppenders = new LinkedHashMap<>();
		reappendUsedQueryParameters(binding, isRoot, context, queryParameterApppenders);
		resolveQueryParameterValues(queryParams, binding, queryParameterApppenders, m, elContext);
		resolveParameterAnnotationValues(binding, m, elContext, queryParameterApppenders);

		ResourceURI.Extension ext = templateURI.getExtension();
		ContentType desiredContentType = binding.isContextExtensionEnabled() ? context.getDesiredContentType() : null;
		String extension = null;
		extension = desiredContentType == null ? extension : desiredContentType.getExtension();
		extension = ext == null ? extension : ext.getName();
		if (extension != null) {
			uriBuilder.extension(extension);
		}

		for (Entry<String, QueryParameterAppender> entrySet : queryParameterApppenders.entrySet()) {
			QueryParameterAppender value = entrySet.getValue();
			value.append(uriBuilder, extension != null);
		}

		List<Fragment> fragments = binding.getUriFragments();
		int indexInBeanIds = 0;

		// TODO:
		// number index of fragments is not equal to index of path elements.
		// this needs some re-work
		StringBuffer fragmentBuf = new StringBuffer();
		for (Fragment fragment : fragments) {
			if (VariableFragment.class.isInstance(fragment)) {
				VariableFragment varFragment = VariableFragment.class.cast(fragment);
				String varName = varFragment.getVariableName();
				if (isPathParameter(varName, binding)) {
					Object value = null;
					Parameter parameter = getParameterForVariable(varName, binding.getAtomLink());
					if (parameter != null) {
						String expression = parameter.expression();
						value = ELUtil.evaluateEL(m, elContext, expression, EXPRESSION_FACTORY);
						if (value == null) {
							throw new ServiceDiscoveryException(
									binding.getControllerMethod(), 
									"could not retrieve a value for the variable " + varName + " in the URITemplate "
									+ uriTemplate + " when using the expression " + expression + " on the given "
									+ (entity == null ? "NULL_ENTITY_PROVIDED" : entity.getClass().getSimpleName()) + " instance."
							);
						} else {
							fragmentBuf.append(PathCoder.encode(convertToString(value)));
						}
					} else {
						if (indexInBeanIds < beanIds.size()) {
							value = beanIds.get(indexInBeanIds);
							if (value != null) {
								fragmentBuf.append(PathCoder.encode(convertToString(value)));
							}
							indexInBeanIds++;
						}
					}
				}
			} else {
				fragmentBuf.append(fragment.getValue());
			}
		}

		String filledTemplate = fragmentBuf.toString();
		if (!filledTemplate.startsWith("/")) {
			filledTemplate = "/" + filledTemplate;
		}

		ResourceURI filledUri = context.parseURI(filledTemplate);
		if (filledUri.getPath() != null) {
			for (String string : filledUri.getPath().getElements()) {
				uriBuilder.pathElement(string);
			}
		} else {
			if (extension != null) {
				uriBuilder.emptyPathElement();
			}
		}
		ResourceURI.Extension filledExtension = filledUri.getExtension();
		if (filledExtension != null) {
			uriBuilder.extension(filledExtension.getName());
		}

		if (templateURI.getSelectors() != null) {
			for (ResourceURI.Selector selector : templateURI.getSelectors()) {
				uriBuilder.selector(selector.getName());
			}
		}
		ResourceURI uri = uriBuilder.build();
		String uriBase = uri.asString();
		return uriBase;
	}

	private String convertToString(Object o) throws ConversionException {
		return (String) converterRegistry.getConverter(o.getClass(), String.class).convert(o);
	}

	private void resolveQueryParameterValues(
			List<QueryParameter> queryParams, 
			CompiledAtomLinkDescription binding, 
			Map<String, QueryParameterAppender> queryParameterApppenders, 
			Method m, 
			ELContext elContext
	) throws ServiceDiscoveryException {
		for (final QueryParameter queryParam : queryParams) {
			String varName = queryParam.getName();
			Object value = null;
			Parameter parameterAnnotation = getParameterForVariable(varName, binding.getAtomLink());
			if (parameterAnnotation == null) {
				String expression = buildElExpressionForVariable(varName, binding.getAtomLink());
				try {
					value = ELUtil.evaluateEL(m, elContext, expression, EXPRESSION_FACTORY);
				} catch (PropertyNotFoundException e) {
					// this is not too bad. 
				}
			} else {
				String expression = parameterAnnotation.expression();
				try {
					value = ELUtil.evaluateEL(m, elContext, expression, EXPRESSION_FACTORY);
				} catch (PropertyNotFoundException e) {
					// this is not too bad. 
				}
			}
			if (value != null) {
				final String _val = value.toString();
				queryParameterApppenders.put(varName, new QueryParameterAppender() {

					@Override
					public void append(ResourceURIBuilder uriBuilder, boolean hasExtension) {
						if (hasExtension && queryParam.isAllowedAsSelector()) {
							uriBuilder.selector(queryParam.getName() + _val);
						} else {
							uriBuilder.parameter(queryParam.getName(), _val);
						}
					}
				});
			}
		}
	}

	private void resolveParameterAnnotationValues(CompiledAtomLinkDescription binding, Method m, ELContext elContext, Map<String, QueryParameterAppender> queryParameterApppenders) {
		Parameter[] parameters = binding.getAtomLink().parameters();
		for (Parameter parameter : parameters) {
			String parameterName = parameter.name();
			if (!queryParameterApppenders.containsKey(parameterName) && !isPathParameter(parameterName, binding)) {
				String expression = parameter.expression();
				if ("".equals(expression)) {
					expression = "${" + parameterName + "}";
				}
				final Object parameterValue = ELUtil.evaluateEL(m, elContext, expression, EXPRESSION_FACTORY);
				if (parameterValue != null) {

					QueryParameter qp = binding.getQueryParamsByName().get(parameterName);
					if (qp == null) {
						qp = new QueryParameterImpl(parameterName, false, null);
					}
					final QueryParameter _qp = qp;
					queryParameterApppenders.put(qp.getName(), new QueryParameterAppender() {

						@Override
						public void append(ResourceURIBuilder uriBuilder, boolean hasExtension) {
							if (hasExtension && _qp.isAllowedAsSelector()) {
								uriBuilder.selector(_qp.getName() + parameterValue.toString());
							} else {
								uriBuilder.parameter(_qp.getName(), parameterValue.toString());
							}
						}
					});
				}
			}
		}
	}

	private void reappendUsedQueryParameters(CompiledAtomLinkDescription binding, boolean isRoot, Context context, Map<String, QueryParameterAppender> queryParameterAppenders) {
		// take care about reuesedQueryParameters (for example when querying paginated list resources)
		boolean shouldReuseQueryParameters = binding.getAtomLink().reuseQueryParameters();
		if (shouldReuseQueryParameters && isRoot) {
			ResourceURI uri = context.getURI();
			List<ResourceURI.Selector> selectors = uri.getSelectors();
			if (selectors != null) {
				for (final ResourceURI.Selector selector : selectors) {
					for (Entry<String, QueryParameter> entrySet : binding.getQueryParamsByName().entrySet()) {
						QueryParameter value = entrySet.getValue();
						if (value.isAllowedAsSelector()) {
							final String selectorPrefix = entrySet.getKey();
							if (selector.getName().startsWith(selectorPrefix)) {
								queryParameterAppenders.put(selectorPrefix, new QueryParameterAppender() {

									@Override
									public void append(ResourceURIBuilder uriBuilder, boolean hasExtension) {
										if (hasExtension) {
											uriBuilder.selector(selector.getName());
										} else {
											uriBuilder.parameter(selectorPrefix, selector.getName().substring(selectorPrefix.length()));
										}
									}
								});
							}
						}
					}
				}
			}

			List<ResourceURI.QueryParameter> params = uri.getParameters();
			if (params != null) {
				for (final ResourceURI.QueryParameter queryParameter : params) {
					final QueryParameter meta = binding.getQueryParamsByName().get(queryParameter.getName());
					if (!queryParameterAppenders.containsKey(queryParameter.getName())) {
						queryParameterAppenders.put(queryParameter.getName(), new QueryParameterAppender() {

							@Override
							public void append(ResourceURIBuilder uriBuilder, boolean hasExtension) {
								if (meta != null && meta.isAllowedAsSelector()) {
									String v = queryParameter.getValue();
									if (v == null) {
										v = "";
									}
									uriBuilder.selector(queryParameter.getName() + v);
								} else {
									uriBuilder.parameter(queryParameter.getName(), queryParameter.getValue());
								}
							}
						});
					}
				}
			}
		}
	}

	private void appendPathAnnotationToPathElements(Path p, StringBuffer pathBuffer) {
		String v = p.value();
		if ("".equals(v)) {
			return;
		}
		if (!v.startsWith("/")) {
			pathBuffer.append('/');
		}
		pathBuffer.append(v);
	}

	private boolean isPathParameter(String fragment, AtomLinkDescription binding) {
		List<PathParameter> params = binding.getPathParams();
		if (params != null) {
			for (PathParameter pathParam : params) {
				if (pathParam.getName().equals(fragment)) {
					return true;
				}
			}
		}
		return false;
	}

	private List<Object> collectBeanIdsFromEntity(Object entity) {
		if (entity == null) {
			return Collections.EMPTY_LIST;
		}
		List<Object> list = new ArrayList<>();
		collectBeanIdsFromEntityRecursive(entity, list);
		return list;
	}

	private BeanIdGetter createBeanIdGetter(Class<?> cls) {
		final List<Field> beanIdFields = ReflectionUtil.getFieldsWithAnnotation(BeanID.class, cls);
		BeanIdGetter getter;
		if ((beanIdFields == null || beanIdFields.isEmpty())) {
			getter = null;
		} else {
			getter = new BeanIdGetter() {

				@Override
				public boolean hasBeanId() {
					return (beanIdFields != null && beanIdFields.size() == 1);
				}

				@Override
				public Object getBeanId(Object source) {
					return ReflectionUtil.getFieldValue(beanIdFields.get(0), source);
				}
			};
		}
		return getter;
	}

	private void collectBeanIdsFromEntityRecursive(Object entity, List<Object> beanIds) {
		if (entity != null) {
			if (beanIdGetter != null) {
				if (beanIdGetter.hasBeanId()) {
					Object id = beanIdGetter.getBeanId(entity);
					beanIds.add(id);
				}
			}
		}
	}

	private Parameter getParameterForVariable(String variableNameFromFragment, AtomLink atomLink) {
		Parameter[] parameters = atomLink.parameters();
		for (Parameter parameter : parameters) {
			if (parameter.name().equals(variableNameFromFragment)) {
				return parameter;
			}
		}

		return null;
	}

	private String buildElExpressionForVariable(String variableNameFromFragment, AtomLink atomLink) {
		Parameter parameter = getParameterForVariable(variableNameFromFragment, atomLink);
		if (parameter != null) {
			return parameter.expression();
		}

		return "${" + variableNameFromFragment + "}";
	}

	private AtomLinkBean createAtomLinkBean(final String uri) {
		return new AtomLinkBean() {

			@Override
			public String getRel() {
				return atomLinkDescription.getRel();
			}

			@Override
			public void setRel(String rel) {
			}

			@Override
			public String getHref() {
				return uri;
			}

			@Override
			public void setHref(String href) {
			}

			@Override
			public String getMethod() {
				return atomLinkDescription.getHttpMethod();
			}

			@Override
			public void setMethod(String method) {
			}
		};
	}

}
