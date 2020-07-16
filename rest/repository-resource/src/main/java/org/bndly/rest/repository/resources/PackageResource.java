package org.bndly.rest.repository.resources;

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

import org.bndly.rest.api.ContentType;
import org.bndly.rest.api.Context;
import org.bndly.rest.atomlink.api.annotation.AtomLink;
import org.bndly.rest.common.beans.Services;
import org.bndly.rest.controller.api.ControllerResourceRegistry;
import org.bndly.rest.controller.api.Documentation;
import org.bndly.rest.controller.api.DocumentationResponse;
import org.bndly.rest.controller.api.GET;
import org.bndly.rest.controller.api.Meta;
import org.bndly.rest.controller.api.Path;
import org.bndly.schema.api.repository.PackageExporter;
import org.bndly.schema.api.repository.Repository;
import org.bndly.schema.api.repository.RepositoryException;
import org.bndly.schema.api.repository.RepositorySession;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Dictionary;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Component(service = PackageResource.class, immediate = true)
@Path(PackageResource.URL_SEGEMENT)
public class PackageResource {
	public static final String URL_SEGEMENT = "repo/package.zip";
	private static ContentType ZIP = new ContentType() {
		@Override
		public String getName() {
			return "application/zip";
		}

		@Override
		public String getExtension() {
			return "zip";
		}
	};
	@Reference
	private ControllerResourceRegistry controllerResourceRegistry;

	@Reference
	private Repository repository;
	
	@Reference
	private PackageExporter packageExporter;
	
	@Activate
	public void activate(ComponentContext componentContext) {
		Dictionary<String, Object> props = componentContext.getProperties();
		controllerResourceRegistry.deploy(this);
	}

	@Deactivate
	public void deactivate() {
		controllerResourceRegistry.undeploy(this);
	}
	
	@GET
	@AtomLink(rel = "repositoryPackage", target = Services.class)
	@Documentation(authors = "bndly@bndly.org", produces = "application/zip", value = "Returns a zip with the exported data of the repository.", tags = "repository", responses = {
		@DocumentationResponse(description = "A zip with the exported data")
	})
	public void getRepositoryPackage(@Meta Context context) throws RepositoryException, IOException {
		try (RepositorySession repositorySession = repository.createReadOnlySession()) {
			context.setOutputContentType(ZIP, null);
			try (OutputStream os = context.getOutputStream()) {
				packageExporter.exportRepositoryData(os, repositorySession);
				os.flush();
			}
		}
	}
}
