package org.bndly.rest.pdf.renderer;

/*-
 * #%L
 * REST PDF Renderer
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

import org.bndly.pdf.templating.DocumentFactory;
import org.bndly.pdf.templating.PDFTemplate;
import org.bndly.rest.api.ContentType;
import org.bndly.rest.api.Context;
import org.bndly.rest.api.ContextProvider;
import org.bndly.rest.api.Resource;
import org.bndly.rest.api.ResourceRenderer;
import org.bndly.rest.api.ResourceURI;
import org.bndly.rest.controller.api.EntityRenderer;
import java.io.IOException;
import java.io.OutputStream;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(service = { ResourceRenderer.class, EntityRenderer.class })
@Designate(ocd = PDFProvider.Configuration.class)
public class PDFProvider implements ResourceRenderer, EntityRenderer {

	@ObjectClassDefinition(
			name = "PDF Resource Renderer"
	)
	public @interface Configuration {

		@AttributeDefinition(
				name = "Default CSS File",
				description = "The default CSS to pick, when a PDF is rendered without specifying any CSS."
		)
		String defaultCSS() default "defaultPDF.css";

		@AttributeDefinition(
				name = "Default Template",
				description = "The default template to use, when no entity is provided while rendering a PDF."
		)
		String defaultTemplate() default "noModelTempalte.vm";
	}
	
	@Reference
	private DocumentFactory documentFactory;

	@Reference
	private ContextProvider contextProvider;
	private String defaultCSS;
	private String defaultTemplate;

	@Activate
	public void activate(Configuration configuration) {
		defaultCSS = configuration.defaultCSS();
		defaultTemplate = configuration.defaultTemplate();
	}
	
	@Override
	public boolean supports(Resource resource, Context context) {
		ResourceURI.Extension ext = resource.getURI().getExtension();
		if (ext != null && ext.getName().equalsIgnoreCase(ContentType.PDF.getExtension())) {
			return isPDFResource(resource);
		}
		ContentType ct = context.getDesiredContentType();
		if (ct != null && ct.getName().startsWith(ContentType.PDF.getName())) {
			return isPDFResource(resource);
		}
		return false;
	}

	private boolean isPDFResource(Resource r) {
		return PDFResource.class.isInstance(r);
	}
	
	@Override
	public void render(Resource resource, Context context) throws IOException {
		PDFResource pr = (PDFResource) resource;
		PDFTemplate doc = new PDFTemplate();
		doc.setEntity(pr.getEntity());
		doc.setCssNames(defaultCSS, pr.getCssName());
		doc.setLocale(context.getLocale());
		doc.setTemplateName(pr.getTemplateName());
		if (!documentFactory.isTemplateAvailable(pr.getTemplateName())) {
			throw new IOException("template " + pr.getTemplateName() + " is not available");
		}
		documentFactory.renderTemplateToDocument(doc, context.getOutputStream());
	}

	public void setDocumentFactory(DocumentFactory documentFactory) {
		this.documentFactory = documentFactory;
	}

	@Override
	public ContentType getSupportedContentType() {
		return ContentType.PDF;
	}

	@Override
	public String getSupportedEncoding() {
		return null;
	}

	@Override
	public void render(Object entity, OutputStream os) throws IOException {
		PDFTemplate doc = new PDFTemplate();
		String templateName = entity == null ? defaultTemplate : entity.getClass().getSimpleName() + ".print.vm";
		String cssName = entity == null ? defaultCSS : entity.getClass().getSimpleName() + ".css";
		doc.setEntity(entity);
		doc.setCssNames(defaultCSS, cssName);
		doc.setLocale(contextProvider.getCurrentContext().getLocale());
		doc.setTemplateName(templateName);
		if (!documentFactory.isTemplateAvailable(templateName)) {
			throw new IOException("template " + templateName + " is not available");
		}
		documentFactory.renderTemplateToDocument(doc, os);
	}
}
