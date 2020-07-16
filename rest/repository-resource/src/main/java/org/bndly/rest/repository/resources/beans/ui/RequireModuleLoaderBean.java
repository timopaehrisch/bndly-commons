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

import org.bndly.rest.repository.resources.RepositoryResource;
import org.bndly.rest.repository.resources.beans.api.BeanPojoFactory;
import org.bndly.rest.repository.resources.beans.api.BeanWrapper;
import org.bndly.rest.repository.resources.beans.initializer.BeanPojoFactoryAware;
import org.bndly.schema.api.repository.beans.Bean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class RequireModuleLoaderBean extends BeanWrapper implements BeanPojoFactoryAware, RequireModule {
	
	public static final String BEAN_TYPE = "cy:requiremoduleloader";
	
	private List<RequireModule> dependencies;
	private BeanPojoFactory beanPojoFactory;
	
	public RequireModuleLoaderBean(Bean wrapped) {
		super(wrapped);
	}
	
	@Override
	public List<RequireModule> getDependencies() {
		if (dependencies == null) {
			Iterator<Bean> children = getChildren();
			if (children != null) {
				while (children.hasNext()) {
					Bean next = children.next();
					Bean dep = beanPojoFactory.getBean(next);
					if (RequireModule.class.isInstance(dep)) {
						if (dependencies == null) {
							dependencies = new ArrayList<>();
						}
						dependencies.add((RequireModule) dep);
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
	public final boolean isHavingExport() {
		return false;
	}

	@Override
	public final String getExport() {
		return null;
	}

	@Override
	public final String getModulePath() {
		return RepositoryResource.URL_SEGEMENT + getPath();
	}

	@Override
	public final String getModuleId() {
		return null;
	}

	@Override
	public String getModuleName() {
		return getName();
	}

	@Override
	public final boolean isProxyModule() {
		return false;
	}

	@Override
	public final RequireModule getProxyBean() {
		return null;
	}

	@Override
	public final String getModuleInitMethod() {
		return "init";
	}

	@Override
	public void setBeanPojoFactory(BeanPojoFactory beanPojoFactory) {
		this.beanPojoFactory = beanPojoFactory;
	}
	
}
