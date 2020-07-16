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

import org.bndly.common.lang.StringUtil;
import org.bndly.pdf.PrintingContext;
import org.bndly.pdf.PrintingObject;
import org.bndly.pdf.style.Style;
import org.bndly.pdf.style.StyleAttributes;

public class Text extends VisualObject {
	private String value;

	public Text(PrintingContext context, PrintingObject owner) {
		super(context, owner);
	}
	
	@Override
	public Double getWidth() {
		return getContext().getTextSizeStrategy().calculateWidthForText(this);
	}
	
	@Override
	public Double getHeight() {
		return getContext().getTextSizeStrategy().calculateHeightForText(this);
	}

	public String getValue() {
		return value;
	}
	

	public void setValue(String value) {
		this.value = value;
	}
	@Override
	protected String asString() {
		return super.asString() + " : " + value;
	}

	/**
	 * convenience method
	 * @return
	 */
	public Double getFontSize() {
		Style style = getCalculatedStyle();
		return style.get(StyleAttributes.FONT_SIZE);
	}
	
	@Override
	public Text copyTo(VisualObject owner) {
		Text copy = getContext().createText(owner);
		copy.value = value;
		return copy;
	}

}
