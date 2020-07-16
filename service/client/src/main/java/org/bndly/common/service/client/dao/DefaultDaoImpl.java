package org.bndly.common.service.client.dao;

/*-
 * #%L
 * Service Client
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

import org.bndly.common.service.validation.RuleFunctionReferenceRestBean;
import org.bndly.common.service.validation.RuleFunctionRestBean;
import org.bndly.common.service.validation.RuleSetRestBean;
import org.bndly.common.service.validation.RulesRestBean;
import org.bndly.common.service.validation.interpreter.FunctionExecutor;
import org.bndly.common.service.validation.interpreter.MandatoryFieldFinder;
import org.bndly.rest.client.api.HATEOASClient;
import org.bndly.rest.client.api.RestBeanValidationException;
import org.bndly.rest.client.api.ServiceFactory;
import org.bndly.rest.client.api.ValidationResult;
import org.bndly.rest.client.api.ValidationRuleDescription;
import org.bndly.rest.client.exception.MissingLinkClientException;
import org.bndly.rest.client.exception.UnknownResourceClientException;
import org.bndly.rest.client.exception.UnmanagedClientException;
import org.bndly.rest.common.beans.ListRestBean;
import org.bndly.rest.common.beans.PaginationRestBean;
import org.bndly.rest.common.beans.RestBean;
import org.bndly.common.service.validation.interpreter.FunctionExecutionException;
import org.bndly.rest.client.exception.ClientException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultDaoImpl implements DefaultDao {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultDaoImpl.class);
	private ServiceFactory serviceFactory;
	private Object primaryResource;
	private final Class<?> listType;
	private final Class<?> restBeanType;
	private final Class<?> restBeanReferenceType;
	private Initializer initializer;

	private RulesRestBean cachedRules;
	private boolean didLookForRules;

	public DefaultDaoImpl(Class<?> listType, Class<?> restBeanType, Class<?> restBeanReferenceType) {
		this.listType = listType;
		this.restBeanType = restBeanType;
		this.restBeanReferenceType = restBeanReferenceType;
	}

	public void setInitializer(Initializer initializer) {
		this.initializer = initializer;
	}

	@Override
	public HATEOASClient getPrimaryResourceClient() throws ClientException {
		if (initializer == null) {
			throw new IllegalStateException("missing initializer");
		}
		if (!initializer.isInitialized()) {
			initializer.init();
		}
		return createClient(primaryResource);
	}

	private HATEOASClient createClient(Object object) {
		return serviceFactory.createClient(object);
	}

	@Override
	public Class getListType() {
		return listType;
	}

	@Override
	public Object listAll() throws ClientException {
		Object tmp = getPrimaryResourceClient().read().execute();
		HATEOASClient tmpClient = createClient(tmp);
		try {
			while (true) {
				Object nextPage = tmpClient.next().execute();
				// add page to tmp;
				if (ListRestBean.class.isInstance(tmp) && ListRestBean.class.isInstance(nextPage)) {
					ListRestBean t = (ListRestBean) tmp;
					ListRestBean n = (ListRestBean) nextPage;
					List i = t.getItems();
					if (i == null) {
						i = new ArrayList();
						t.setItems(i);
					}
					if (n.getItems() != null) {
						i.addAll(n.getItems());
					}
				}
				// go to next page
				tmpClient = createClient(nextPage);
			}
		} catch (MissingLinkClientException e) {
			// no more pages
		}
		return tmp;
	}

	@Override
	public Collection listAllAsCollection() throws ClientException {
		Object tmp = listAll();
		return getItemsOfListRestBean(tmp);
	}

	@Override
	public Object findAllLike(Object prototype) throws ClientException {
		return getPrimaryResourceClient().follow("findAll").execute(prototype);
	}

	@Override
	public Object readNextPage(Object currentPage) throws ClientException {
		return createClient(currentPage).next().execute();
	}

	@Override
	public Object readPreviousPage(Object currentPage) throws ClientException {
		return createClient(currentPage).previous().execute();
	}

	@Override
	public long getTotalCount() throws ClientException {
		Object tmp = getPrimaryResourceClient().read().execute();
		if (ListRestBean.class.isInstance(tmp)) {
			PaginationRestBean p = ((ListRestBean) tmp).getPage();
			if (p != null) {
				if (p.getTotalRecords() != null) {
					return p.getTotalRecords();
				}
			}
		}
		return -1;
	}

	@Override
	public Object findLocation(Object entity) throws ClientException {
		String location = (String) getPrimaryResourceClient().follow("find").preventRedirect().execute(entity, String.class);
		if (location == null) {
			return null;
		}
		setSelfLinkInRestBean(entity, location);
		return entity;
	}

	@Override
	public Object find(Object entity) throws ClientException {
		return getPrimaryResourceClient().follow("find").execute(entity, entity.getClass());
	}

	@Override
	public Object findWithLocale(Object entity, String locale) throws ClientException {
		return getPrimaryResourceClient().acceptLanguage(locale).follow("find").execute(entity, entity.getClass());
	}

	@Override
	public Collection findAllLikeAsCollection(Object entity) throws ClientException {
		Object tmp = findAllLike(entity);
		return getItemsOfListRestBean(tmp);
	}

	@Override
	public void print(Object entity, OutputStream os) throws ClientException {
		createClient(entity).accept("application/pdf").follow("print").execute(os);
	}

	@Override
	public Object create(Object entity) throws ClientException {
		ValidationResult validationResult = validate(entity);
		if (validationResult.didFail()) {
			throw new RestBeanValidationException("validation failed. " + validationResult.getFailedRules());
		}
		return getPrimaryResourceClient().create().execute(entity);
	}

	@Override
	public Object createLocation(Object entity) throws ClientException {
		ValidationResult validationResult = validate(entity);
		if (validationResult.didFail()) {
			throw new RestBeanValidationException("validation failed. " + validationResult.getFailedRules());
		}
		String location = (String) getPrimaryResourceClient().create().preventRedirect().execute(entity, String.class);
		if (location == null) {
			return null;
		}
		setSelfLinkInRestBean(entity, location);
		return entity;
	}

	@Override
	public Object readFromUrl(String url) throws ClientException {
		try {
			Object bean = restBeanType.newInstance();
			setSelfLinkInRestBean(bean, url);
			return read(bean);
		} catch (InstantiationException | IllegalAccessException ex) {
			throw new ClientException("could not instantiate type " + restBeanType.getSimpleName() + " to read it from url " + url, ex);
		}
	}

	@Override
	public Object read(Object entity) throws ClientException {
		return createClient(entity).read().execute();
	}

	@Override
	public Object update(Object entity) throws ClientException {
		ValidationResult validationResult = validate(entity);
		if (validationResult.didFail()) {
			throw new RestBeanValidationException("validation failed. " + validationResult.getFailedRules());
		}
		createClient(entity).update().execute(entity);
		return createClient(entity).read().execute();
	}

	@Override
	public Object updateLocation(Object entity) throws ClientException {
		ValidationResult validationResult = validate(entity);
		if (validationResult.didFail()) {
			throw new RestBeanValidationException("validation failed. " + validationResult.getFailedRules());
		}
		createClient(entity).update().execute(entity);
		return entity;
	}

	@Override
	public boolean delete(Object entity) throws ClientException {
		Object result = createClient(entity).delete().execute();
		return result == null;
	}

	@Override
	public ValidationResult validate(Object bean) throws ClientException {
		ValidationResult result = new ValidationResult();
		result.setValidatedBean(bean);

		if (bean != null) {
			try {
				if (cachedRules == null && !didLookForRules) {
					didLookForRules = true;
					try {
						RuleSetRestBean r = (RuleSetRestBean) getPrimaryResourceClient().follow("rules").execute(RuleSetRestBean.class);
						if (r != null) {
							cachedRules = r.getRules();
						}
					} catch (UnmanagedClientException e) {
						// ignore this
					}
				}
				if (cachedRules != null) {
					for (RuleFunctionReferenceRestBean ruleFunctionReferenceRestBean : cachedRules) {
						if (!RuleFunctionRestBean.class.isInstance(ruleFunctionReferenceRestBean)) {
							continue;
						}
						RuleFunctionRestBean ruleFunctionRestBean = (RuleFunctionRestBean) ruleFunctionReferenceRestBean;
						FunctionExecutor executor = new FunctionExecutor(bean);
						try {
							Boolean ruleSucceeded = (Boolean) executor.evaluate(ruleFunctionRestBean);
							ValidationRuleDescription description = new ValidationRuleDescription();
							description.setAffectedPropertyName(ruleFunctionRestBean.getField());
							description.setOriginalRule(ruleFunctionRestBean);
							description.setRuleName(ruleFunctionRestBean.getName());

							if (ruleSucceeded) {
								result.addSucceededRule(description);
							} else {
								result.addFailedRule(description);
							}
						} catch (Exception e) {
							throw new FunctionExecutionException(
								"could not execute rule function " + ruleFunctionRestBean.getName() + " for field " + ruleFunctionRestBean.getField(), e
							);
						}
					}
				}
			} catch (UnknownResourceClientException e) {
				// if the rules have been dropped while the client still had the rules link in memory
				LOG.info(e.getMessage());
			} catch (MissingLinkClientException e) {
				// if there are no rules, the validation result remains as it has been initialized
				LOG.info(e.getMessage());
			} finally {
				didLookForRules = true;
			}
		}

		return result;
	}

	@Override
	public List listMandatoryFields() throws ClientException {
		try {
			RulesRestBean r = (RulesRestBean) getPrimaryResourceClient().follow("rules").execute(RulesRestBean.class);
			List<RuleFunctionRestBean> rules = new ArrayList<>();
			for (RuleFunctionReferenceRestBean ruleFunctionReferenceRestBean : r) {
				rules.add((RuleFunctionRestBean) ruleFunctionReferenceRestBean);
			}
			MandatoryFieldFinder finder = new MandatoryFieldFinder();
			return finder.findMandatoryFields(rules, restBeanType);
		} catch (MissingLinkClientException e) {
			// if there are no rules, there are no known mandatory fields
			return null;
		}
	}

	@Override
	public Class getBeanType() {
		return restBeanType;
	}

	public void setServiceFactory(ServiceFactory serviceFactory) {
		this.serviceFactory = serviceFactory;
	}

	public void setPrimaryResource(Object primaryResource) {
		this.primaryResource = primaryResource;
	}

	private Collection getItemsOfListRestBean(Object tmp) {
		if (ListRestBean.class.isInstance(tmp)) {
			return ((ListRestBean) tmp).getItems();
		} else {
			return null;
		}
	}

	private void setSelfLinkInRestBean(Object entity, String location) {
		if (RestBean.class.isInstance(entity)) {
			((RestBean) entity).setLink("self", location, "GET");
		}
	}

	@Override
	public Object search(Object searchDescription) throws ClientException {
		return getPrimaryResourceClient().follow("search").execute(searchDescription, listType);
	}

	@Override
	public Object getSearchParameters() {
		return null;
	}

}
