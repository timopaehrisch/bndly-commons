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

import org.bndly.rest.swagger.model.Document;
import org.bndly.rest.swagger.model.Path;
import org.bndly.rest.swagger.model.Paths;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class EmptyPathUninstaller implements Uninstaller {

	private final String pathKey;
	private final Path path;
	private final Document document;

	public EmptyPathUninstaller(String pathKey, Path path, Document document) {
		if (pathKey == null || path == null || document == null) {
			throw new IllegalArgumentException("pathKey, path and document have to be non-null");
		}
		this.pathKey = pathKey;
		this.path = path;
		this.document = document;
	}
	
	@Override
	public void uninstall() {
		if (
				path.getDelete() == null
				&& path.getGet() == null
				&& path.getHead() == null
				&& path.getOptions() == null
				&& path.getPatch() == null
				&& path.getPost() == null
				&& path.getPut() == null
		) {
			Paths paths = document.getPaths();
			Path p = paths.get(pathKey);
			if (p == path) {
				paths.remove(pathKey);
			}
		}
	}
	
}
