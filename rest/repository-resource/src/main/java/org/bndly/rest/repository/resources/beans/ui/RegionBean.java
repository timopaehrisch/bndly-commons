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
public class RegionBean extends BeanWrapper implements Initializeable, BeanPojoFactoryAware {

	public static final String BEAN_TYPE = "cy:region";
	private BeanPojoFactory beanPojoFactory;
	private String name;
	private List<Bean> items;

	public RegionBean(Bean wrapped) {
		super(wrapped);
	}

	@Override
	public void init() {
		name = (String) getProperty("name");
	}

	public String getRegionName() {
		return name;
	}
	
	public List<Bean> getRegionItems() {
		if (items == null) {
			Iterator<Bean> children = getChildren();
			while (children.hasNext()) {
				Bean child = children.next();
				child = beanPojoFactory.getBean(child);
				if (child != null) {
					if (items == null) {
						items = new ArrayList<>();
					}
					items.add(child);
				}
			}
			if (items == null) {
				items = Collections.EMPTY_LIST;
			}
		}
		return items;
	}
	
	@Override
	public void setBeanPojoFactory(BeanPojoFactory beanPojoFactory) {
		this.beanPojoFactory = beanPojoFactory;
	}
	
}
