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

import org.bndly.rest.repository.resources.beans.api.BeanPojoFactory;
import org.bndly.rest.repository.resources.beans.initializer.Initializeable;
import org.bndly.rest.repository.resources.beans.api.BeanWrapper;
import org.bndly.rest.repository.resources.beans.initializer.BeanPojoFactoryAware;
import org.bndly.schema.api.repository.beans.Bean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class ApplicationBean extends BeanWrapper implements Initializeable, BeanPojoFactoryAware {

	public static final String BEAN_TYPE = "cy:application";
	
	private String name;
	private String version;
	private BeanPojoFactory beanPojoFactory;
	private List<ScriptBean> scripts;
	private List<RequireModule> requireModuleBeans;
	private Map<String, RequireModule> requireModuleBeansByPath;
	
	public ApplicationBean(Bean wrapped) {
		super(wrapped);
	}

	@Override
	public void init() {
		name = (String) getProperty("name");
		version = (String) getProperty("version");
	}

	private void initRequireModules() {
		String[] requireModules = (String[]) getProperty("requiremodules");
		if (requireModules != null) {
			for (String requireModulePath : requireModules) {
				Bean bean = getBeanResolver().resolve(requireModulePath);
				if(bean != null) {
					RequireModule requireModule = beanPojoFactory.getBean(bean, RequireModuleBean.class);
					if(requireModule == null) {
						requireModule = beanPojoFactory.getBean(bean, RequireModuleLoaderBean.class);
					}
					if (requireModule != null) {
						if (requireModuleBeans == null) {
							requireModuleBeans = new ArrayList<>();
							requireModuleBeansByPath = new HashMap<>();
						}
						requireModuleBeans.add(requireModule);
						requireModuleBeansByPath.put(requireModulePath, requireModule);
					}
				}
			}
		}
		if (requireModuleBeans == null) {
			requireModuleBeans = Collections.EMPTY_LIST;
			requireModuleBeansByPath = Collections.EMPTY_MAP;
		}
	}

	public List<RequireModule> getRequireModuleBeans() {
		if (requireModuleBeans == null) {
			initRequireModules();
		}
		return requireModuleBeans;
	}
	
	public List<ScriptBean> getScripts() {
		if (scripts == null) {
			String[] tmp = (String[]) getProperty("scripts");
			if (tmp != null) {
				for (String scriptPath : tmp) {
					ScriptBean scriptBean = beanPojoFactory.getBean(getBeanResolver().resolve(scriptPath), ScriptBean.class);
					if (scriptBean != null) {
						if (scripts == null) {
							scripts = new ArrayList<>();
						}
						scripts.add(scriptBean);
					}
				}
			}
			if (scripts == null) {
				scripts = Collections.EMPTY_LIST;
			}
		}
		return scripts;
	}

	public String getApplicationName() {
		return name;
	}

	public String getApplicationVersion() {
		return version;
	}
	
	public boolean isApplicationModule(RequireModule requireModule) {
		getRequireModuleBeans();
		return requireModuleBeansByPath.get(requireModule.getPath()) != null;
	}

	@Override
	public void setBeanPojoFactory(BeanPojoFactory beanPojoFactory) {
		this.beanPojoFactory = beanPojoFactory;
	}

}
