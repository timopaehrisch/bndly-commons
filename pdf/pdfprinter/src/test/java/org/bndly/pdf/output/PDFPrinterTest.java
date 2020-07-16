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

import org.bndly.css.CSSAttribute;
import org.bndly.css.CSSStyle;
import org.bndly.pdf.PrintingContext;
import org.bndly.pdf.style.StyleAttributes;
import org.bndly.pdf.visualobject.Document;
import org.bndly.pdf.visualobject.Page;
import org.bndly.pdf.visualobject.Text;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.testng.annotations.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class PDFPrinterTest {
	
	@Test
	public void testTTF() throws IOException {
		PDDocument doc = new PDDocument();

		PDPage page = new PDPage();
//		doc.addPage(page);
//		try (InputStream is = Files.newInputStream(Paths.get("src","main","resources","Aller_Rg.ttf"), StandardOpenOption.READ)) {
//			PDFont font = PDTrueTypeFont.loadTTF(doc, is);
//			CMap cmap = font.getToUnicodeCMap();
//			PDPageContentStream contentStream = new PDPageContentStream(doc, page);
//			contentStream.beginText();
//			contentStream.setFont(font, 12);
//			contentStream.moveTextPositionByAmount(100, 700);
//			
//			TextToPDF textToPDF = new TextToPDF();
//			textToPDF.setFont((PDSimpleFont) font);
//			textToPDF.setFontSize(14);
//			
//			// PDFBox drawString does not deal well with unicode strings. Hence the euro symbol will be broken.
//			contentStream.drawString("Hello euro €");
//			contentStream.endText();
//			contentStream.close();
//			doc.save("target/testttf.pdf");
//		}
	}
	
	@Test
	public void testEuroSymbol() throws IOException {
		PrintingContext printingContext = new PrintingContext();
		PDFPrinterImpl printer = printingContext.createPDFPrinter();
		Document document = printingContext.createDocument(printer);
		CSSStyle cssStyle = printingContext.createStyle("text");
		CSSAttribute attribute = new CSSAttribute();
		attribute.setName(StyleAttributes.FONT_SIZE);
		attribute.setValue("12pt");
		cssStyle.addAttribute(attribute);
		attribute = new CSSAttribute();
		attribute.setName(StyleAttributes.FONT);
		attribute.setValue("helvetica");
		cssStyle.addAttribute(attribute);
		
		document.setMaxWidth(300D);
		document.setMaxHeight(300D);
		Page page = printingContext.createPage(document);
		document.add(page);
		page.setMaxHeight(300D);
		page.setMaxWidth(300D);
		Text text = printingContext.createText(page);
		text.setValue("hallo €");
		page.add(text);
		Path get = Paths.get("target", "eurosymbol.pdf");
		Files.deleteIfExists(get);
		Files.createFile(get);
		try (OutputStream os = Files.newOutputStream(get, StandardOpenOption.WRITE)) {
			printer.setOutputStream(os);
			printer.print(document);
			os.flush();
		}
	}
}
