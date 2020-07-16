package org.bndly.pdf.mapper;

/*-
 * #%L
 * PDF XML Document Mapper
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

import org.bndly.document.reader.DocumentReader;
import org.bndly.document.xml.XDocument;
import org.bndly.pdf.PrintingContext;
import org.bndly.pdf.output.InputStreamResolver;
import org.bndly.pdf.output.PDFPrinter;
import org.bndly.pdf.visualobject.Document;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.junit.Test;


public class DocumentMappingTest {

	private static final String css = "src/test/resources/order.css";
	private static final String xml = "src/test/resources/order.xml";
	
	@Test
	public void printOrderPDF() throws IOException {
		DocumentReader documentReader = new DocumentReader();

		PrintingContext ctx = new PrintingContext();
		ctx.setInputStreamResolver(new InputStreamResolver() {
			@Override
			public InputStream resolve(String fileName) throws IOException {
				return Files.newInputStream(Paths.get(fileName), StandardOpenOption.READ);
			}
		});
		PDFPrinter printer = ctx.createPDFPrinter();
		ctx.loadStyleSheetFromLocation(css);
		printer.setOutputFileName("foo.pdf");
		
		XDocument xDocument = documentReader.read(xml);
		Document doc = new DocumentMapper().toDocument(xDocument, printer);
		printer.print(doc);
	}
}
