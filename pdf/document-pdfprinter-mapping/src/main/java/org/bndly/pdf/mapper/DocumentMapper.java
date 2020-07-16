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

import java.util.List;

import org.bndly.pdf.output.PDFPrinter;
import org.bndly.pdf.style.Style;
import org.bndly.pdf.visualobject.Container;
import org.bndly.pdf.visualobject.Document;
import org.bndly.pdf.visualobject.Image;
import org.bndly.pdf.visualobject.Text;
import org.bndly.pdf.visualobject.VisualObject;
import org.bndly.document.xml.XContainer;
import org.bndly.document.xml.XDocument;
import org.bndly.document.xml.XImage;
import org.bndly.document.xml.XOverflowPage;
import org.bndly.document.xml.XPage;
import org.bndly.document.xml.XPageTemplate;
import org.bndly.document.xml.XParagraph;
import org.bndly.document.xml.XSystemText;
import org.bndly.document.xml.XTable;
import org.bndly.document.xml.XTableCell;
import org.bndly.document.xml.XTableColumn;
import org.bndly.document.xml.XText;
import org.bndly.document.xml.XVisualObject;
import org.bndly.pdf.PrintingContext;
import org.bndly.pdf.PrintingObject;

public class DocumentMapper {

	public Document toDocument(XDocument xDocument, PDFPrinter printer) {
		Document document = printer.getContext().createDocument(printer.getContext());
		handleXVisualObject(xDocument, document);
		return document;
	}

	private void handleXVisualObject(XVisualObject xVisualObject, VisualObject visualObject) {
		visualObject.setItemId(xVisualObject.getItemId());
		if (visualObject.is(Text.class)) {
			String text = ((XText) xVisualObject).getValue();
			visualObject.as(Text.class).setValue(text);
		}
		if (visualObject.is(Image.class)) {
			visualObject.as(Image.class).setSource(((XImage) xVisualObject).getSource());
		}

		visualObject.setStyleClasses(xVisualObject.getStyleClasses());

		// CSS style classes are applied within the pdf printer.
		// this can't be done in the document mapper, because the document might change its structure due to the layout process
		if (visualObject.is(Image.class)) {
			Image image = visualObject.as(Image.class);
			Style cs = image.getCalculatedStyle();
			if (image.getWidth() == null) {
				throw new IllegalStateException("image needs a width! image: " + image.getItemId() + " " + image.getSource());
			}
			if (image.getHeight() == null) {
				throw new IllegalStateException("image needs a height! image: " + image.getItemId() + " " + image.getSource());
			}
		}
		
				if (visualObject.is(Container.class)) {
			XContainer xContainer = ((XContainer) xVisualObject);
			List<XVisualObject> items = xContainer.getItems();
			if (items != null) {
				for (XVisualObject xItem : items) {
					VisualObject child = createVisualObjectFromXMLObject(xItem, visualObject);
					visualObject.as(Container.class).add(child);
					handleXVisualObject(xItem, child);
				}
			}
		}
	}
	
	/**
	 * allow special conversion, if necessary
	 * @param name attribute name
	 * @return the mapped attribute name
	 */
	private String toPDFPrinterStyleName(String name) {
		return name;
	}

//	private Class<? extends VisualObject> getPDFPrinterClassFor(XVisualObject xVisualObject, PrintingObject owner) {
	private VisualObject createVisualObjectFromXMLObject(XVisualObject xVisualObject, PrintingObject owner) {
		PrintingContext context = owner.getContext();
		Class<? extends XVisualObject> xClazz = xVisualObject.getClass();
		if (XDocument.class.isAssignableFrom(xClazz)) {
			return context.createDocument(owner);
		}
		if (XOverflowPage.class.isAssignableFrom(xClazz)) {
			return context.createOverflowPage(owner);
		}
		if (XPage.class.isAssignableFrom(xClazz)) {
			return context.createPage(owner);
		}
		if (XPageTemplate.class.isAssignableFrom(xClazz)) {
			return context.createPageTemplate(owner);
		}
		if (XParagraph.class.isAssignableFrom(xClazz)) {
			return context.createParagraph(owner);
		}
		if (XTable.class.isAssignableFrom(xClazz)) {
			return context.createTable(owner);
		}
		if (XTableColumn.class.isAssignableFrom(xClazz)) {
			return context.createTableColumn(owner);
		}
		if (XTableCell.class.isAssignableFrom(xClazz)) {
			return context.createTableCell(owner);
		}
		if (XImage.class.isAssignableFrom(xClazz)) {
			return context.createImage(owner);
		}
		if (XSystemText.class.isAssignableFrom(xClazz)) {
			return context.createSystemText(owner);
		}
		if (XText.class.isAssignableFrom(xClazz)) {
			return context.createText(owner);
		}
		if (XContainer.class.isAssignableFrom(xClazz)) {
			return context.createContainer(owner);
		}

		throw new IllegalStateException("unhandled xVisualObject: " + xClazz.getName());
	}
}
