package org.bndly.pdf.style;

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

import java.util.List;

import org.bndly.css.CSSAttribute;
import org.bndly.css.CSSStyle;
import org.bndly.pdf.css.CSSSelectorMatcher;
import org.bndly.pdf.PrintingContext;
import org.bndly.pdf.PrintingObject;
import org.bndly.pdf.visualobject.VisualObject;

public class CalculatedStyle extends Style {

	public CalculatedStyle(PrintingContext context, PrintingObject owner) {
		super(context, owner);
	}
	
	public static Style createFrom(VisualObject object) {
		CalculatedStyle style = new CalculatedStyle(object.getContext(), object);
		mergeStyleFromTo(object, style);
		return style;
	}

	private static void mergeStyleFromTo(VisualObject object, CalculatedStyle style) {
		PrintingContext ctx = style.getContext();
		List<CSSStyle> allStyles = ctx.getStyles();
		CSSSelectorMatcher matcher = new CSSSelectorMatcher();
		if (allStyles != null) {
			for (CSSStyle cssStyle : allStyles) {
				if (matcher.styleAppliesOn(cssStyle, object)) {
					// set the style attributes in the resulting style
					List<CSSAttribute> attributes = cssStyle.getAttributes();
					if (attributes != null) {
						for (CSSAttribute cssAttribute : attributes) {
							String attributeValue = cssAttribute.getValue();
							Object filteredValue = filterAttributeValue(attributeValue);
							String attributeName = cssAttribute.getName();
							style.superSet(attributeName, filteredValue);
						}
					}
				}
			}
		}
	}

	private static Object filterAttributeValue(String rawValue) {
		Object value = rawValue;
		if (rawValue.charAt(0) == '"' && rawValue.charAt(rawValue.length() - 1) == '"') {
			value = rawValue.substring(1, rawValue.length() - 1);
		} else {
			String rawDoubleValue = rawValue;
			if (rawValue.endsWith("pt")) {
				rawDoubleValue = rawValue.replace("pt", "");
			}
			try {
				Double d = new Double(rawDoubleValue);
				value = d;
			} catch (Exception e) {
				// ignore invalid doubles
			}
		}
		return value;
	}

	private void superSet(String attributeName, Object attributeValue) {
		super.set(attributeName, attributeValue);
	}

	@Override
	public void set(String attributeName, Object attributeValue) {
		throw new IllegalAccessError("you are working on a calculated style. this means you are not allowed to add or change any style attributes.");
	}
}
