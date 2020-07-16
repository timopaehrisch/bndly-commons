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

import org.bndly.rest.repository.resources.beans.initializer.Initializeable;
import org.bndly.rest.repository.resources.beans.initializer.TemplateContextAware;
import org.bndly.rest.repository.resources.beans.initializer.BeanPojoFactoryAware;
import org.bndly.rest.repository.resources.beans.api.BeanWrapper;
import org.bndly.rest.repository.resources.beans.api.BeanPojoFactory;
import org.bndly.schema.api.repository.beans.Bean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class UIBean extends BeanWrapper implements Initializeable, BeanPojoFactoryAware, TemplateContextAware {

	public static final String BEAN_TYPE = "cy:ui";
	
	private boolean isRoot;
	private UIBean rootUi;
	private ApplicationBean application;
	private TitleTextBean hero;
	private List<NavigationItem> mainNavigation;
	private NavigationItem activeMainNavigation;
	private BeanPojoFactory beanPojoFactory;
	private TemplateContext templateContext;
	private List<RegionBean> regions;
	private List<RequireModule> requireModuleBeans;
	private String title;

	public UIBean(Bean wrapped) {
		super(wrapped);
	}

	private NavigationItem createNavigationItem(final UIBean uiBean, final List<NavigationItem> items) {
		return new NavigationItem() {
			@Override
			public UIBean getUi() {
				return uiBean;
			}

			@Override
			public boolean isActive() {
				return uiBean.isActive();
			}

			@Override
			public List<NavigationItem> getSubItems() {
				return items;
			}

			@Override
			public String getTitle() {
				return uiBean.getTitle();
			}
			
		};
	}
	
	public static interface NavigationItem {
		public UIBean getUi();
		public boolean isActive();
		public List<NavigationItem> getSubItems();
		public String getTitle();
	}
	
	@Override
	public void init() {
		initRootUi();
		if (isRoot()) {
			Bean appBean = getBeanResolver().resolve((String) getProperty("app"));
			if (appBean != null) {
				application = beanPojoFactory.getBean(appBean, ApplicationBean.class);
			}
			initHero();
			mainNavigation = initNavigation();
			initRequireModules();
		}
	}
	
	private String getTitle() {
		if (title == null) {
			title = (String) getProperty("title");
			if (title == null) {
				title = getName();
			}
		}
		return title;
	}

	private void initRootUi() {
		Bean current = this;
		while (current != null) {
			Bean tmp = current.getParent();
			if (tmp != null && tmp.getBeanType().equals(BEAN_TYPE)) {
				current = tmp;
			} else {
				break;
			}
		}
		if (current != this) {
			rootUi = beanPojoFactory.getBean(current, UIBean.class);
			isRoot = false;
		} else {
			rootUi = this;
			isRoot = true;
		}
	}
	
	private void initHero() {
		Bean heroBean = getBeanResolver().resolve((String) getProperty("hero"));
		if (heroBean != null) {
			hero = beanPojoFactory.getBean(heroBean, TitleTextBean.class);
		}
	}
	
	private void initRequireModules() {
		String[] requireModules = (String[]) getProperty("requiremodules");
		if (requireModules != null) {
			for (String requireModulePath : requireModules) {
				Bean tmp = beanPojoFactory.getBean(getBeanResolver().resolve(requireModulePath));
				if (!RequireModule.class.isInstance(tmp)) {
					continue;
				}
				RequireModule requireModule = (RequireModule) tmp;
				if (requireModuleBeans == null) {
					requireModuleBeans = new ArrayList<>();
				}
				requireModuleBeans.add(requireModule);
			}
		}
		if (requireModuleBeans == null) {
			requireModuleBeans = Collections.EMPTY_LIST;
		}
	}

	private List<NavigationItem> initNavigation() {
		UIBean root = getRootUi();
		List<NavigationItem> mainNavigationItemList = new ArrayList<>();
		Iterator<Bean> children = root.getChildren();
		while (children.hasNext()) {
			Bean next = children.next();
			if (next.getBeanType().equals(BEAN_TYPE)) {
				UIBean uiBean = beanPojoFactory.getBean(next, UIBean.class);
				if (uiBean != null) {
					NavigationItem mainNavigationItem = createNavigationItem(uiBean, new ArrayList<NavigationItem>());
					mainNavigationItemList.add(mainNavigationItem);
					if (mainNavigationItem.isActive()) {
						activeMainNavigation = mainNavigationItem;
					}
						Iterator<Bean> subNavHeaderIter = uiBean.getChildren();
						while (subNavHeaderIter.hasNext()) {
							Bean subNavHeader = subNavHeaderIter.next();
							if (subNavHeader.getBeanType().equals(BEAN_TYPE)) {
								UIBean subNavHeaderBean = beanPojoFactory.getBean(subNavHeader, UIBean.class);
								if (subNavHeaderBean != null) {
									NavigationItem subNavigationHeaderItem = createNavigationItem(subNavHeaderBean, new ArrayList<NavigationItem>());
									mainNavigationItem.getSubItems().add(subNavigationHeaderItem);
									Iterator<Bean> subNavigationItemIter = subNavHeaderBean.getChildren();
									while (subNavigationItemIter.hasNext()) {
										Bean subNavigationItem = subNavigationItemIter.next();
										if (subNavigationItem.getBeanType().equals(BEAN_TYPE)) {
											UIBean subNavigationItemBean = beanPojoFactory.getBean(subNavigationItem, UIBean.class);
											if (subNavigationItemBean != null) {
												subNavigationHeaderItem.getSubItems().add(
													createNavigationItem(subNavigationItemBean, Collections.EMPTY_LIST)
												);
											}
										}
									}
								}
							}
						}
					
				}
			}
			
		}
		return mainNavigationItemList;
	}

	public List<RequireModule> getRequireModuleBeans() {
		return isRoot() ? requireModuleBeans : getRootUi().getRequireModuleBeans();
	}
	
	public TitleTextBean getHero() {
		return hero;
	}
	
	public boolean isRoot() {
		return isRoot;
	}

	public UIBean getRootUi() {
		return rootUi;
	}

	public NavigationItem getActiveMainNavigation() {
		return isRoot() ? activeMainNavigation : getRootUi().getActiveMainNavigation();
	}

	public boolean isActive() {
		if (isRoot()) {
			return true;
		}
		if (templateContext == null) {
			return false;
		}
		String currentPath = (String) templateContext.get("currentPath");
		return currentPath.startsWith(getPath());
	}
	
	public List<RegionBean> getRegions() {
		if (regions == null) {
			Iterator<Bean> children = getChildren();
			while (children.hasNext()) {
				RegionBean bean = beanPojoFactory.getBean(children.next(), RegionBean.class);
				if (bean != null) {
					if (regions == null) {
						regions = new ArrayList<>();
					}
					regions.add(bean);
				}

			}
			if (regions == null) {
				regions = Collections.EMPTY_LIST;
			}
		}
		return regions;
	}
	
	public List<NavigationItem> getMainNavigation() {
		return isRoot() ? mainNavigation : getRootUi().getMainNavigation();
	}
	
	public ApplicationBean getApplication() {
		return application != null ? application : getRootUi().getApplication();
	}

	@Override
	public void setBeanPojoFactory(BeanPojoFactory beanPojoFactory) {
		this.beanPojoFactory = beanPojoFactory;
	}

	@Override
	public void setTemplateContext(TemplateContext templateContext) {
		this.templateContext = templateContext;
	}

	
}
