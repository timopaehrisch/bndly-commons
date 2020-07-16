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

import org.bndly.rest.repository.resources.beans.api.BeanFactoryRegistrar;
import org.bndly.rest.repository.resources.beans.api.BeanPojoRegistry;
import org.bndly.schema.api.repository.beans.Bean;
import org.osgi.service.component.annotations.Component;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = BeanFactoryRegistrar.class)
public class UIBeanFactoryRegistrar implements BeanFactoryRegistrar {

	private final BeanPojoRegistry.Factory applicationBeanFactory = new BeanPojoRegistry.Factory() {
		@Override
		public Bean newInstance(Bean original) {
			return new ApplicationBean(original);
		}
	};
	private final BeanPojoRegistry.Factory uiBeanFactory = new BeanPojoRegistry.Factory() {
		@Override
		public Bean newInstance(Bean original) {
			return new UIBean(original);
		}
	};
	private final BeanPojoRegistry.Factory titleTextBeanFactory = new BeanPojoRegistry.Factory() {
		@Override
		public Bean newInstance(Bean original) {
			return new TitleTextBean(original);
		}
	};
	private final BeanPojoRegistry.Factory regionBeanFactory = new BeanPojoRegistry.Factory() {
		@Override
		public Bean newInstance(Bean original) {
			return new RegionBean(original);
		}
	};
	private final BeanPojoRegistry.Factory entityBrowserBeanFactory = new BeanPojoRegistry.Factory() {
		@Override
		public Bean newInstance(Bean original) {
			return new EntityBrowserBean(original);
		}
	};
	private final BeanPojoRegistry.Factory scriptBeanFactory = new BeanPojoRegistry.Factory() {
		@Override
		public Bean newInstance(Bean original) {
			return new ScriptBean(original);
		}
	};
	private final BeanPojoRegistry.Factory requireModuleBeanFactory = new BeanPojoRegistry.Factory() {
		@Override
		public Bean newInstance(Bean original) {
			return new RequireModuleBean(original);
		}
	};
	private final BeanPojoRegistry.Factory requireViewBeanFactory = new BeanPojoRegistry.Factory() {
		@Override
		public Bean newInstance(Bean original) {
			return new RequireViewBean(original);
		}
	};
	private final BeanPojoRegistry.Factory requireModuleLoaderBeanFactory = new BeanPojoRegistry.Factory() {
		@Override
		public Bean newInstance(Bean original) {
			return new RequireModuleLoaderBean(original);
		}
	};
	
	@Override
	public void register(BeanPojoRegistry beanPojoRegistry) {
		beanPojoRegistry.register(ApplicationBean.BEAN_TYPE, applicationBeanFactory);
		beanPojoRegistry.register(UIBean.BEAN_TYPE, uiBeanFactory);
		beanPojoRegistry.register(TitleTextBean.BEAN_TYPE, titleTextBeanFactory);
		beanPojoRegistry.register(RegionBean.BEAN_TYPE, regionBeanFactory);
		beanPojoRegistry.register(EntityBrowserBean.BEAN_TYPE, entityBrowserBeanFactory);
		beanPojoRegistry.register(ScriptBean.BEAN_TYPE, scriptBeanFactory);
		beanPojoRegistry.register(RequireModuleBean.BEAN_TYPE, requireModuleBeanFactory);
		beanPojoRegistry.register(RequireViewBean.BEAN_TYPE, requireViewBeanFactory);
		beanPojoRegistry.register(RequireModuleLoaderBean.BEAN_TYPE, requireModuleLoaderBeanFactory);
	}

	@Override
	public void unregister(BeanPojoRegistry beanPojoRegistry) {
		beanPojoRegistry.unregister(ApplicationBean.BEAN_TYPE, applicationBeanFactory);
		beanPojoRegistry.unregister(UIBean.BEAN_TYPE, uiBeanFactory);
		beanPojoRegistry.unregister(TitleTextBean.BEAN_TYPE, titleTextBeanFactory);
		beanPojoRegistry.unregister(RegionBean.BEAN_TYPE, regionBeanFactory);
		beanPojoRegistry.unregister(EntityBrowserBean.BEAN_TYPE, entityBrowserBeanFactory);
		beanPojoRegistry.unregister(ScriptBean.BEAN_TYPE, scriptBeanFactory);
		beanPojoRegistry.unregister(RequireModuleBean.BEAN_TYPE, requireModuleBeanFactory);
		beanPojoRegistry.unregister(RequireViewBean.BEAN_TYPE, requireViewBeanFactory);
		beanPojoRegistry.unregister(RequireModuleLoaderBean.BEAN_TYPE, requireModuleLoaderBeanFactory);
	}
	
}
