package org.bndly.pdf.output;

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

import org.bndly.pdf.HasPrintingContext;
import org.bndly.pdf.style.FontStyles;
import org.bndly.pdf.style.FontWeights;
import org.bndly.pdf.style.Style;
import org.bndly.pdf.visualobject.Document;
import java.io.OutputStream;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface PDFPrinter extends HasPrintingContext {
	FontBinding getFontBinding(String fontName, FontWeights fontWeight, FontStyles fontStyle);
	FontWeights getFontWeight(Style style);
	FontStyles getFontStyle(Style style);
	
	void setOutputStream(OutputStream outputStream);
	void setOutputFileName(String outputFileName);
	void print(Document d);
	
	void print(Document d, OutputStream outputStream);
}
