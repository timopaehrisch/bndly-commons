package org.bndly.pdf.templating;

/*-
 * #%L
 * PDF templating
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

import org.bndly.common.data.io.ReplayableInputStream;
import org.bndly.css.CSSItem;
import org.bndly.css.CSSParsingException;
import org.bndly.css.CSSReader;
import org.bndly.css.CSSStyle;
import org.bndly.document.reader.DocumentReader;
import org.bndly.document.xml.XDocument;
import org.bndly.pdf.PrintingContext;
import org.bndly.pdf.mapper.DocumentMapper;
import org.bndly.pdf.output.InputStreamResolver;
import org.bndly.pdf.output.PDFPrinter;
import org.bndly.pdf.visualobject.Document;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.testng.annotations.Test;

public class TableSplitTest {

	private PrintingContext ctx;
	private static final String CSS = "tablesplittest.css";
	private static final String XML = "tablesplittest.xml";

	@Test
	public void setup() {
		ctx = new PrintingContext();
		PDFPrinter printer = ctx.createPDFPrinter();
		printer.setOutputFileName("target/tablesplittest.pdf");
		final ClassLoader cl = getClass().getClassLoader();
		InputStreamResolver resolver = new InputStreamResolver() {
			@Override
			public ReplayableInputStream resolve(String fileName) throws IOException {
				return ReplayableInputStream.newInstance(cl.getResource(fileName).openStream());
			}
		};
		ctx.setInputStreamResolver(resolver);

		List<CSSStyle> styles = new ArrayList<>();
		try {
			CSSReader reader = new CSSReader();
			List<CSSItem> items = reader.read(resolver.resolve(CSS));
			for (CSSItem item : items) {
				if (CSSStyle.class.isInstance(item)) {
					styles.add((CSSStyle) item);
				}
			}
		} catch (IOException | CSSParsingException e) {
			throw new IllegalStateException("CSS file could not be read for file " + CSS, e);
		}

		ctx.getStyles().addAll(styles);

		DocumentMapper mapper = new DocumentMapper();
		DocumentReader reader = new DocumentReader();
		XDocument xDocument;
		try {
			xDocument = reader.read(resolver.resolve(XML));
			Document pdfdocument = mapper.toDocument(xDocument, printer);
			printer.print(pdfdocument);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
