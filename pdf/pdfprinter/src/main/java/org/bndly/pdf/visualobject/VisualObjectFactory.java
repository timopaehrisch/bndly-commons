package org.bndly.pdf.visualobject;

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

import org.bndly.pdf.PrintingObject;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface VisualObjectFactory {
	Container createContainer(PrintingObject owner);
	Document createDocument(PrintingObject owner);
	Image createImage(PrintingObject owner);
	OverflowPage createOverflowPage(PrintingObject owner);
	Page createPage(PrintingObject owner);
	PageTemplate createPageTemplate(PrintingObject owner);
	Paragraph createParagraph(PrintingObject owner);
	SystemText createSystemText(PrintingObject owner);
	Table createTable(PrintingObject owner);
	TableCell createTableCell(PrintingObject owner);
	TableColumn createTableColumn(PrintingObject owner);
	Text createText(PrintingObject owner);
}
