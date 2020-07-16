package org.bndly.common.velocity.api;

/*-
 * #%L
 * Velocity API
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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VelocityTemplate {

	private Locale locale;
	private Object entity;
	private String templateName;
	private List<ContextData> contextData;
	private String dateFormatString;

	public String getDateFormatString() {
		return dateFormatString;
	}

	public VelocityTemplate setDateFormatString(String dateFormatString) {
		this.dateFormatString = dateFormatString;
		return this;
	}

	public Locale getLocale() {
		return locale;
	}

	public VelocityTemplate setLocale(Locale locale) {
		this.locale = locale;
		return this;
	}

	public Object getEntity() {
		return entity;
	}

	public VelocityTemplate setEntity(Object entity) {
		this.entity = entity;
		return this;
	}

	public String getTemplateName() {
		return templateName;
	}

	public VelocityTemplate setTemplateName(String templateName) {
		this.templateName = templateName;
		return this;
	}

	public List<ContextData> getContextData() {
		return contextData;
	}

	public VelocityTemplate setContextData(List<ContextData> contextData) {
		this.contextData = contextData;
		return this;
	}

	public VelocityTemplate addContextData(ContextData contextData) {
		if (contextData != null) {
			if (getContextData() == null) {
				setContextData(new ArrayList<ContextData>());
			}
			getContextData().add(contextData);
		}
		return this;
	}
}
