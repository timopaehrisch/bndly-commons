package org.bndly.rest.repository.resources.beans.ui;

/*-
 * #%L
 * REST Repository Resource
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

import org.bndly.rest.api.Context;
import org.bndly.rest.atomlink.api.AtomLinkBean;
import org.bndly.rest.atomlink.api.AtomLinkInjector;
import org.bndly.rest.repository.resources.beans.initializer.Initializeable;
import org.bndly.rest.repository.resources.beans.api.BeanWrapper;
import org.bndly.rest.repository.resources.beans.initializer.OSGIServiceAware;
import org.bndly.rest.repository.resources.beans.initializer.RESTContextAware;
import org.bndly.schema.api.repository.beans.Bean;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class EntityBrowserBean extends BeanWrapper implements Initializeable, RESTContextAware, OSGIServiceAware {

	public static final String BEAN_TYPE = "cy:entitybrowser";
	private Context context;
	private ServiceResolver serviceResolver;
	
	public EntityBrowserBean(Bean wrapped) {
		super(wrapped);
	}

	@Override
	public void init() {
	}
	
	public String getUrl() {
		if (context == null) {
			return null;
		}
		if (serviceResolver == null) {
			return null;
		}
		AtomLinkInjector service = serviceResolver.getService(AtomLinkInjector.class);
		if (service != null) {
			AtomLinkBean link = service.getLinkByName("self", getListRestBean());
			if (link != null) {
				String href = link.getHref();
				return context.createURIBuilder()
						.replace(context.parseURI(href))
						.clearParameters()
						.build()
						.asString();
			}
		}
		// check if the entity exists in a schema
		// if it is available in a schema
		// return the url as it would be cr
		// otherwise
		return context.createURIBuilder().pathElement(getSchemaName()).pathElement(getEntity()).build().asString();
	}
	
	public String getReferenceRestBean() {
		String tmp = (String) getProperty("referenceRestBean");
		if (tmp == null || tmp.isEmpty()) {
			tmp = getEntity() + "ReferenceRestBean";
		}
		return tmp;
	}
	
	public String getRestBean() {
		String tmp = (String) getProperty("restBean");
		if (tmp == null || tmp.isEmpty()) {
			tmp = getEntity() + "RestBean";
		}
		return tmp;
	}
	
	public String getListRestBean() {
		String tmp = (String) getProperty("listRestBean");
		if (tmp == null || tmp.isEmpty()) {
			tmp = getEntity() + "ListRestBean";
		}
		return tmp;
	}
	
	public String getSchemaName() {
		String t = (String) getProperty("schemaName");
		// the 'ebx' is here just as a fallback
		if (t == null || t.isEmpty()) {
			return "ebx";
		}
		return t;
	}
	
	public String getEntity() {
		return (String) getProperty("entity");
	}
	
	public String getNewItemTitle() {
		return (String) getProperty("newItemTitle");
	}
	
	public String getExistingItemTitle() {
		return (String) getProperty("existingItemTitle");
	}

	@Override
	public void setRESTContext(Context context) {
		this.context = context;
	}

	@Override
	public void setServiceResolver(ServiceResolver serviceResolver) {
		this.serviceResolver = serviceResolver;
	}
}
