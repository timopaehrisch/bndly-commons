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

import java.io.IOException;

import org.apache.pdfbox.pdmodel.font.PDFont;

import org.bndly.pdf.layout.TextSizeStrategy;
import org.bndly.pdf.style.Style;
import org.bndly.pdf.style.StyleAttributes;
import org.bndly.pdf.visualobject.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PDFontTextSizeStrategy extends AbstractPDFPrinterStrategy implements TextSizeStrategy {
	
	private static final Logger LOG = LoggerFactory.getLogger(PDFontTextSizeStrategy.class);
	
	@Override
	public double calculateWidthForText(Text text) {
		Style style = text.getCalculatedStyle();
		Double fontSize = style.get(StyleAttributes.FONT_SIZE);
		if (fontSize == null) {
			throw new IllegalStateException("fontsize for text '" + text.getValue() + "' could not be determined.");
		}
		String fontName = style.get(StyleAttributes.FONT);
		if (fontName == null) {
			throw new IllegalStateException("fontName for text '" + text.getValue() + "' could not be determined.");
		}
		FontBinding binding = getPdfPrinter().getFontBinding(fontName, getPdfPrinter().getFontWeight(style), getPdfPrinter().getFontStyle(style));
		if (binding != null) {
			PDFont pdFont = binding.getPdFont();
			try {
				return pdFont.getStringWidth(text.getValue()) / 1000d * fontSize;
			} catch (IOException e) {
				LOG.error("failed to calculate text width for text with font " + fontName + ": " + e.getMessage(), e);
			}
		}
		throw new RuntimeException("could not determine text width, because the font binding for " + fontName + " was not found.");
	}

	@Override
	public double calculateHeightForText(Text text) {
		Style style = text.getCalculatedStyle();
		Double fontsize = style.get(StyleAttributes.FONT_SIZE);
		Double lineHeight = style.get(StyleAttributes.LINE_HEIGHT);
		if (lineHeight != null) {
			fontsize *= lineHeight;
		}
		return fontsize;
	}

}
