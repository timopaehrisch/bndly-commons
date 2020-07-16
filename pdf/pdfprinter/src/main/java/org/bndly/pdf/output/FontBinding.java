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

import org.apache.pdfbox.pdmodel.font.PDFont;

import org.bndly.pdf.style.FontStyles;
import org.bndly.pdf.style.FontWeights;

public class FontBinding {

	private String name;
	private PDFont pdFont;
	private FontWeights weight;
	private FontStyles style;

	public void setPDFont(PDFont pdFont) {
		this.pdFont = pdFont;
	}

	public void setName(String name) {
		this.name = name;		
	}
	public String getName() {
		return name;
	}
	public PDFont getPdFont() {
		return pdFont;
	}
	public void setWeight(FontWeights fontWeight) {
		this.weight = fontWeight; 
	}
	public void setStyle(FontStyles fontStyle) {
		this.style = fontStyle;
	}
	public FontStyles getStyle() {
		return style;
	}
	public FontWeights getWeight() {
		return weight;
	}
}
