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

import org.bndly.rest.repository.resources.beans.api.BeanWrapper;
import org.bndly.rest.repository.resources.beans.initializer.Initializeable;
import org.bndly.schema.api.repository.beans.Bean;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class RequireViewBean extends BeanWrapper implements Initializeable {
	
	public static final String BEAN_TYPE = "cy:requireview";
	private String viewImpl;
	
	public RequireViewBean(Bean wrapped) {
		super(wrapped);
	}

	@Override
	public void init() {
		viewImpl = (String) getProperty("viewImpl");
	}

	public String getViewImpl() {
		return viewImpl;
	}

}
