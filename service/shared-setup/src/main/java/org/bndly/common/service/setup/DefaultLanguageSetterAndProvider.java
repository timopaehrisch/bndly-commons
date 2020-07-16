package org.bndly.common.service.setup;

/*-
 * #%L
 * Service Shared Client Setup
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

import org.bndly.rest.client.api.LanguageSetterAndProvider;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class DefaultLanguageSetterAndProvider implements LanguageSetterAndProvider {

	private String defaultLanguage;
	
	private final ThreadLocal<String> languageLocal = new ThreadLocal<>();

	@Override
	public String getCurrentLanguage() {
		String lang = languageLocal.get();
		if (lang == null) {
			lang = defaultLanguage;
		}
		return lang;
	}

	@Override
	public void setCurrentLanguage(String currentLanguage) {
		languageLocal.set(currentLanguage);
	}

	public void setDefaultLanguage(String defaultLanguage) {
		this.defaultLanguage = defaultLanguage;
	}

}
