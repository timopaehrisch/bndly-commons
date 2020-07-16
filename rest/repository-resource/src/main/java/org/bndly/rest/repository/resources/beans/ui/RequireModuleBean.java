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
import org.bndly.rest.api.PathCoder;
import org.bndly.rest.api.ResourceURI;
import org.bndly.rest.api.ResourceURIBuilder;
import org.bndly.rest.api.ResourceURIParser;
import org.bndly.rest.repository.resources.RepositoryResource;
import org.bndly.rest.repository.resources.beans.api.BeanPojoFactory;
import org.bndly.rest.repository.resources.beans.api.BeanWrapper;
import org.bndly.rest.repository.resources.beans.initializer.BeanPojoFactoryAware;
import org.bndly.rest.repository.resources.beans.initializer.Initializeable;
import org.bndly.rest.repository.resources.beans.initializer.RESTContextAware;
import org.bndly.schema.api.repository.beans.Bean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class RequireModuleBean extends BeanWrapper implements Initializeable, BeanPojoFactoryAware, RESTContextAware, RequireModule {

	public static final String BEAN_TYPE = "cy:requiremodule";

	private String script;
	private String name;
	private List<RequireModule> dependencies;
	private BeanPojoFactory beanPojoFactory;
	private Context context;
	private RequireModule proxy;
	private boolean didLookForProxy;

	public RequireModuleBean(Bean wrapped) {
		super(wrapped);
	}

	@Override
	public void init() {

	}
	
	@Override
	public List<RequireModule> getDependencies() {
		if (dependencies == null) {
			String[] tmp = (String[]) getProperty("dependencies");
			if (tmp != null && tmp.length > 0) {
				for (String pathToDependency : tmp) {
					RequireModuleBean dep = beanPojoFactory.getBean(getBeanResolver().resolve(pathToDependency), RequireModuleBean.class);
					if (dep != null) {
						if (dependencies == null) {
							dependencies = new ArrayList<>();
						}
						dependencies.add(dep);
					}
				}
			}
			if (dependencies == null) {
				dependencies = Collections.EMPTY_LIST;
			}
		}
		return dependencies;
	}
	
	@Override
	public RequireModule getProxyBean() {
		if (!didLookForProxy) {
			didLookForProxy = true;
			if (isProxyModule()) {
				Bean tmp = beanPojoFactory.getBean(getBeanResolver().resolve(getProxy()));
				if (RequireModule.class.isInstance(tmp)) {
					proxy = (RequireModule) tmp;
				}
			}
		}

		return proxy;
	}
	
	public String getProxy() {
		String proxyLocation = (String) getProperty("proxy");
		return proxyLocation != null && !proxyLocation.isEmpty() ? proxyLocation : null;
	}
	
	@Override
	public boolean isProxyModule() {
		return getProxy() != null;
	}

	public String getScript() {
		if (script == null) {
			script = (String) getProperty("script");
			if (script == null) {
				script = "";
			}
		}
		return script;
	}

	@Override
	public String getModuleInitMethod() {
		String tmp = (String) getProperty("initMethod");
		return tmp == null || tmp.isEmpty() ? null : tmp;
	}
	
	@Override
	public String getModuleName() {
		if (name == null) {
			name = (String) getProperty("name");
			if (name == null) {
				name = getName();
			}
		}
		return name;
	}
	
	@Override
	public String getModuleId() {
		String moduleId = (String) getProperty("moduleId");
		return moduleId == null || moduleId.isEmpty() ? null : moduleId;
	}
	
	@Override
	public String getModulePath() {
		if (isProxyModule()) {
			RequireModule proxyBean = getProxyBean();
			if (proxyBean != null) {
				return proxyBean.getModulePath();
			}
			String proxy = getProxy();
			ResourceURI uri = new ResourceURIParser(new PathCoder.UTF8(), proxy).parse().getResourceURI();
			if (uri.hasSchemeHost()) {
				return uri.asString();
			} else {
				ResourceURIBuilder builder = context.createURIBuilder();
				for (String element : uri.getPath().getElements()) {
					builder.pathElement(element);
				}
				List<ResourceURI.Selector> selectors = uri.getSelectors();
				if (selectors != null) {
					for (ResourceURI.Selector selector : selectors) {
						builder.selector(selector.getName());
					}
				}
				ResourceURI.Extension extension = uri.getExtension();
				if (extension != null) {
					builder.extension(extension.getName());
				}
				List<ResourceURI.QueryParameter> parameters = uri.getParameters();
				if (parameters != null) {
					for (ResourceURI.QueryParameter parameter : parameters) {
						builder.parameter(parameter.getName(), parameter.getValue());
					}
				}
				return builder.build().asString();
			}
		} else {
			return RepositoryResource.URL_SEGEMENT + getPath();
		}
	}

	@Override
	public void setBeanPojoFactory(BeanPojoFactory beanPojoFactory) {
		this.beanPojoFactory = beanPojoFactory;
	}

	@Override
	public void setRESTContext(Context context) {
		this.context = context;
	}

	@Override
	public boolean isHavingExport() {
		return getExport() != null;
	}
	
	@Override
	public String getExport() {
		String tmp = (String) getProperty("export");
		if (tmp != null && tmp.isEmpty()) {
			return null;
		}
		return tmp;
	}
}
