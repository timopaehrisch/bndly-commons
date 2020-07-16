package org.bndly.rest.swagger.impl;

/*-
 * #%L
 * REST Swagger Integration
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

import org.bndly.rest.controller.api.DocumentationInfo;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class DocumentedController implements Uninstaller {

	private final Object controller;
	private final List<DocumentationInfo> documentationInfos = new ArrayList<>();
	private final List<Uninstaller> uninstallers = new ArrayList<>();

	public DocumentedController(Object controller) {
		if (controller == null) {
			throw new IllegalArgumentException("controller is not allowed to be null");
		}
		this.controller = controller;
	}

	public Object getController() {
		return controller;
	}

	public void add(DocumentationInfo documentationInfo, Uninstaller uninstaller) {
		if (documentationInfo == null || uninstaller == null) {
			throw new IllegalArgumentException("documentation info and uninstaller have to be pro");
		}
		documentationInfos.add(documentationInfo);
		uninstallers.add(uninstaller);
	}

	@Override
	public void uninstall() {
		Iterator<Uninstaller> iter = uninstallers.iterator();
		while (iter.hasNext()) {
			Uninstaller next = iter.next();
			next.uninstall();
		}
		documentationInfos.clear();
		uninstallers.clear();
	}
	
	
}
