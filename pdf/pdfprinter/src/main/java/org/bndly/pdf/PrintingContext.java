package org.bndly.pdf;

/*-
 * #%L
 * PDF Document Printer
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

import org.bndly.css.CSSItem;
import org.bndly.css.CSSParsingException;
import org.bndly.css.CSSReader;
import java.util.List;

import org.bndly.css.CSSStyle;
import org.bndly.pdf.layout.AbsoluteLayout;
import org.bndly.pdf.layout.AddPageOverflowStrategy;
import org.bndly.pdf.layout.DocumentLayout;
import org.bndly.pdf.layout.LayoutFactory;
import org.bndly.pdf.layout.OverflowStrategyFactory;
import org.bndly.pdf.layout.TableColumnLayout;
import org.bndly.pdf.layout.TableLayout;
import org.bndly.pdf.layout.TextSizeStrategy;
import org.bndly.pdf.layout.ThrowAwayOverflowStrategy;
import org.bndly.pdf.layout.VerticalLayout;
import org.bndly.pdf.output.InputStreamResolver;
import org.bndly.pdf.output.PDFPrinterImpl;
import org.bndly.pdf.visualobject.Container;
import org.bndly.pdf.visualobject.Document;
import org.bndly.pdf.visualobject.Image;
import org.bndly.pdf.visualobject.OverflowPage;
import org.bndly.pdf.visualobject.Page;
import org.bndly.pdf.visualobject.PageTemplate;
import org.bndly.pdf.visualobject.Paragraph;
import org.bndly.pdf.visualobject.SystemText;
import org.bndly.pdf.visualobject.Table;
import org.bndly.pdf.visualobject.TableCell;
import org.bndly.pdf.visualobject.TableColumn;
import org.bndly.pdf.visualobject.Text;
import org.bndly.pdf.visualobject.VisualObjectFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PrintingContext implements OverflowStrategyFactory, LayoutFactory, VisualObjectFactory, PrintingObject {
	private static final Logger LOG = LoggerFactory.getLogger(PrintingContext.class);
	
	private TextSizeStrategy textSizeStrategy;
	private InputStreamResolver inputStreamResolver;
	private final long id;
	private long idCounter = 0;
	private final List<CSSStyle> styles = new ArrayList<>();

	public PrintingContext() {
		idCounter = 0;
		this.id = idCounter;
	}
	
	public TextSizeStrategy getTextSizeStrategy() {
		return textSizeStrategy;
	}

	public void setTextSizeStrategy(TextSizeStrategy textSizeStrategy) {
		this.textSizeStrategy = textSizeStrategy;
	}

	public InputStreamResolver getInputStreamResolver() {
		return inputStreamResolver;
	}

	public void setInputStreamResolver(InputStreamResolver inputStreamResolver) {
		this.inputStreamResolver = inputStreamResolver;
	}

	public long plusplus() {
		return idCounter++;		
	}

	public PDFPrinterImpl createPDFPrinter() {
		PDFPrinterImpl printer = new PDFPrinterImpl(this, this);
		PrintingContext ctx = printer.getContext();
		if (ctx == null) {
			throw new IllegalStateException("the created PDF printer has no context, even though it should.");
		}
		return printer;
	}
	
	public CSSStyle createStyle(String selector) {
		if (selector == null || selector.isEmpty()) {
			throw new IllegalArgumentException("css selector is null or empty");
		}
		CSSStyle cssStyle = new CSSStyle();
		cssStyle.setSelector(selector);
		getStyles().add(cssStyle);
		return cssStyle;
	}
	
	public List<CSSStyle> getStyles() {
		return styles;
	}
	
	public void loadStyleSheetFromLocation(String location) {
		InputStreamResolver isr = getInputStreamResolver();
		if (isr == null) {
			// TODO: log an error
			LOG.error("could not load CSS from location {}, because no input stream resolver was available", location);
			return;
		}
		try {
			InputStream is = isr.resolve(location);
			if (is != null) {
				try (InputStream tmp = is) {
					CSSReader cssReader = new CSSReader();
					List<CSSItem> items = cssReader.read(tmp);
					for (CSSItem item : items) {
						if (CSSStyle.class.isInstance(item)) {
							styles.add((CSSStyle)item);
						}
					}
				} catch (IOException | CSSParsingException ex) {
					LOG.error("could not load CSS from location " + location + ": " + ex.getMessage(), ex);
				}
			}
		} catch (IOException ex) {
			LOG.error("could not resolve CSS from location " + location + ": " + ex.getMessage(), ex);
		}
	}

	@Override
	public ThrowAwayOverflowStrategy createThrowAwayOverflowStrategy() {
		return new ThrowAwayOverflowStrategy(this, this);
	}

	@Override
	public AddPageOverflowStrategy createAddPageOverflowStrategy() {
		return new AddPageOverflowStrategy(this, this);
	}

	@Override
	public TableColumnLayout createTableColumnLayout(TableColumn ownerContainer) {
		return new TableColumnLayout(this, ownerContainer);
	}

	@Override
	public TableLayout createTableLayout(Table ownerContainer) {
		return new TableLayout(this, ownerContainer);
	}

	@Override
	public VerticalLayout createVerticalLayout(Container ownerContainer) {
		return new VerticalLayout(this, ownerContainer);
	}

	@Override
	public AbsoluteLayout createAbsoluteLayout(Container ownerContainer) {
		return new AbsoluteLayout(this, ownerContainer);
	}

	@Override
	public DocumentLayout createDocumentLayout(Document ownerContainer) {
		return new DocumentLayout(this, ownerContainer);
	}

	@Override
	public Container createContainer(PrintingObject owner) {
		return new Container(this, owner);
	}

	@Override
	public Document createDocument(PrintingObject owner) {
		return new Document(this, owner);
	}

	@Override
	public Image createImage(PrintingObject owner) {
		return new Image(this, owner);
	}

	@Override
	public OverflowPage createOverflowPage(PrintingObject owner) {
		return new OverflowPage(this, owner);
	}

	@Override
	public Page createPage(PrintingObject owner) {
		return new Page(this, owner);
	}

	@Override
	public PageTemplate createPageTemplate(PrintingObject owner) {
		return new PageTemplate(this, owner);
	}

	@Override
	public Paragraph createParagraph(PrintingObject owner) {
		return new Paragraph(this, owner);
	}

	@Override
	public SystemText createSystemText(PrintingObject owner) {
		return new SystemText(this, owner);
	}

	@Override
	public Table createTable(PrintingObject owner) {
		return new Table(this, owner);
	}

	@Override
	public TableCell createTableCell(PrintingObject owner) {
		return new TableCell(this, owner);
	}

	@Override
	public TableColumn createTableColumn(PrintingObject owner) {
		return new TableColumn(this, owner);
	}

	@Override
	public Text createText(PrintingObject owner) {
		return new Text(this, owner);
	}

	@Override
	public final String getItemId() {
		return "PRINTINGCONTEXT";
	}

	@Override
	public PrintingContext getContext() {
		return this;
	}

	@Override
	public long getId() {
		return id;
	}

	@Override
	public <T extends PrintingObject> T as(Class<T> type) {
		return type.cast(this);
	}

	@Override
	public boolean is(Class<? extends PrintingObject> clazz) {
		return clazz.isInstance(this);
	}
	
}
