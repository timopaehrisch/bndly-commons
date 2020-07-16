package org.bndly.common.documentation.impl;

/*-
 * #%L
 * Documentation
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

import org.bndly.common.documentation.BundleDocumentation;
import org.bndly.rest.api.Context;
import org.bndly.rest.api.ContextProvider;
import org.bndly.rest.api.StatusWriter;
import org.bndly.rest.controller.api.ControllerResourceRegistry;
import org.bndly.rest.controller.api.GET;
import org.bndly.rest.controller.api.Meta;
import org.bndly.rest.controller.api.Path;
import org.bndly.rest.controller.api.PathParam;
import org.bndly.rest.controller.api.Response;
import java.io.IOException;
import org.osgi.framework.Bundle;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Path("bundledoc")
public class ImageResource {

	private static final String PREFIX = "/bundledoc/";
	
	private final DocumentedBundleTracker documentedBundleTracker;
	private final ControllerResourceRegistry controllerResourceRegistry;
	private final ContextProvider contextProvider;

	public ImageResource(DocumentedBundleTracker documentedBundleTracker, ControllerResourceRegistry controllerResourceRegistry, ContextProvider contextProvider) {
		this.documentedBundleTracker = documentedBundleTracker;
		this.controllerResourceRegistry = controllerResourceRegistry;
		this.contextProvider = contextProvider;
	}

	@GET
	@Path("{id}/{file}")
	public Response download(@PathParam("id") long id, @Meta Context context) throws IOException {
		BundleDocumentation bundleDocumentation = documentedBundleTracker.getBundleDocumentationById(id);
		if (bundleDocumentation == null) {
			return Response.status(StatusWriter.Code.NOT_FOUND.getHttpCode());
		}
		String uri = context.getURI().asString();
		String imagePath = uri.substring(PREFIX.length());
		int i = imagePath.indexOf("/");
		if (i < 0) {
			return Response.status(StatusWriter.Code.NOT_FOUND.getHttpCode());
		}
		imagePath = imagePath.substring(i + 1);
		BundleDocumentation.Entry imageEntry = bundleDocumentation.getImageEntry(imagePath);
		if (imageEntry == null) {
			return Response.status(StatusWriter.Code.NOT_FOUND.getHttpCode());
		}
		return Response.ok(imageEntry.getContent());
	}
	
	public String getUrlOfImage(String imageEntry, Bundle bundle) {
		Context currentContext = contextProvider.getCurrentContext();
		if (currentContext == null) {
			return null;
		}
		return currentContext.createURIBuilder().pathElement("bundledoc").pathElement(Long.toString(bundle.getBundleId())).pathElement(imageEntry).build().asString();
	}
}
