package org.bndly.rest.pdf.controller.impl;

/*-
 * #%L
 * REST PDF Resource
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
import org.bndly.rest.api.ContentType;
import org.bndly.rest.atomlink.api.annotation.AtomLink;
import org.bndly.rest.atomlink.api.annotation.AtomLinks;
import org.bndly.rest.api.Context;
import org.bndly.rest.controller.api.GET;
import org.bndly.rest.controller.api.Meta;
import org.bndly.rest.controller.api.POST;
import org.bndly.rest.controller.api.Path;
import org.bndly.rest.controller.api.PathParam;
import org.bndly.rest.api.ResourceURIBuilder;
import org.bndly.rest.controller.api.Response;
import org.bndly.rest.pdf.beans.PDFDocument;
import org.bndly.rest.pdf.beans.PDFDocuments;
import org.bndly.rest.common.beans.Services;
import org.bndly.rest.controller.api.ControllerResourceRegistry;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(service = PDFResource.class, configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = PDFResource.Configuration.class)
@Path("pdf")
public class PDFResource {
	
	@ObjectClassDefinition(
			name = "PDF Resource",
			description = "The PDF resource is used to generate and provide PDF files based on templates."
	)
	public @interface Configuration {
		@AttributeDefinition(
				name = "Temp folder",
				description = "A folder where temporary PDF data can be stored."
		)
		String pdfTempDirectory();
	}

	private DocumentFactory documentFactory;
	private String pdfTempDirectory;

	@Reference
	private ControllerResourceRegistry controllerResourceRegistry;

	@Activate
	public void activate(Configuration configuration) {
		pdfTempDirectory = configuration.pdfTempDirectory();
		controllerResourceRegistry.deploy(this);
	}
	@Deactivate
	public void deactivate() {
		controllerResourceRegistry.undeploy(this);
	}
	
	@GET
	@AtomLinks({
		@AtomLink(rel = "pdfs", target = Services.class),
		@AtomLink(rel = "list", target = PDFDocument.class),
		@AtomLink(target = PDFDocuments.class)
	})
	public Response listPDFs() {
		PDFDocuments docs = new PDFDocuments();
		if (pdfTempDirectory != null) {
			File folder = new File(pdfTempDirectory);
			File[] files = null;
			if (folder.isDirectory()) {
				files = folder.listFiles(new FileFilter() {
					@Override
					public boolean accept(File pathname) {
						return pathname.getName().endsWith(".pdf");
					}
				});
			}
			if (files != null) {
				for (File file : files) {
					PDFDocument doc = new PDFDocument();
					String n = file.getName();
					doc.setName(n.substring(0, n.length() - ".pdf".length()));
					docs.add(doc);
				}
			}
		}
		return Response.ok(docs);
	}

	@GET
	@Path("{fileName}")
	@AtomLink(target = PDFDocument.class)
	public Response read(@PathParam("fileName") String fileName) {
		java.nio.file.Path path = Paths.get(pdfTempDirectory).resolve(fileName + ".pdf");
		if (!Files.exists(path) || !Files.isRegularFile(path)) {
			return Response.status(404);
		}
		PDFDocument doc = new PDFDocument();
		doc.setName(fileName);
		return Response.ok(doc);
	}

	@GET
	@Path("{fileName}.pdf")
	@AtomLink(rel = "download", target = PDFDocument.class)
	public Response readPDF(@PathParam("fileName") String fileName) throws IOException {
		java.nio.file.Path path = Paths.get(pdfTempDirectory).resolve(fileName + ".pdf");
		if (!Files.exists(path) || !Files.isRegularFile(path)) {
			return Response.status(404);
		}
		return Response.ok(new FileInputStream(path.toFile())).contentType(ContentType.PDF, null);
	}

	@POST
	@AtomLink(target = PDFDocuments.class)
	public Response generatePDF(PDFDocument doc, @Meta Context context) throws IOException {
		java.nio.file.Path path = Paths.get(pdfTempDirectory).resolve(doc.getName() + ".pdf");
		Files.deleteIfExists(path);
		try (OutputStream os = Files.newOutputStream(path, StandardOpenOption.WRITE)) {
			os.flush();
			String[] cssNames = new String[]{doc.getCssName()};
			documentFactory.renderDocument(doc.getDocument(), cssNames, os, true);

			ResourceURIBuilder builder = context.createURIBuilder();
			String outFileName = doc.getName();
			String uri = builder.pathElement("pdf").pathElement(outFileName).build().asString();
			return Response.created(uri);
		}
	}

	public void setDocumentFactory(DocumentFactory documentFactory) {
		this.documentFactory = documentFactory;
	}

	public void setPdfTempDirectory(String pdfTempDirectory) {
		this.pdfTempDirectory = pdfTempDirectory;
	}

}
