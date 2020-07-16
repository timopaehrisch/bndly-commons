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

import org.bndly.pdf.PrintingContext;
import org.bndly.pdf.PrintingObject;
import org.bndly.pdf.style.Style;
import org.bndly.pdf.style.StyleAttributes;

public class Image extends VisualObject {
	private String source;

	public Image(PrintingContext context, PrintingObject owner) {
		super(context, owner);
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}
	
	@Override
	public Double getWidth() {
		Double w = super.getWidth();
		if (w == null) {
			Style style = getCalculatedStyle();
			w = style.get(StyleAttributes.WIDTH);
		}
		return w;
	}
	
	@Override
	public Double getHeight() {
		Double h = super.getHeight();
		if (h == null) {
			Style style = getCalculatedStyle();
			h = style.get(StyleAttributes.HEIGHT);
		}
		return h;
	}
	
	@Override
	public VisualObject copyTo(VisualObject owner) {
		Image copy = getContext().createImage(owner);
		copy.source = source;
		return copy;
	}
}
