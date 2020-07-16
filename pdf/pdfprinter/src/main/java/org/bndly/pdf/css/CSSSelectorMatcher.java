package org.bndly.pdf.css;

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
import java.util.List;

import org.bndly.css.CSSStyle;
import org.bndly.css.selector.CSSChildSelector;
import org.bndly.css.selector.CSSSelector;
import org.bndly.css.selector.CSSSelectorParser;
import org.bndly.css.selector.CSSTypeSelector;
import org.bndly.pdf.visualobject.Container;
import org.bndly.pdf.visualobject.VisualObject;

public class CSSSelectorMatcher {
	
	private VisualObject currentVisualObject;
	
	public boolean styleAppliesOn(CSSStyle style, VisualObject v) {
		currentVisualObject = v;
		String selectorString = style.getSelector();
		// parse the string into CSSSelectors
		CSSSelectorParser parser = new CSSSelectorParser();
		List<CSSSelector> selectors = parser.parseSelectorString(selectorString);
		
		boolean allSelectorsApply = true;
		// while all selectors apply
		for (int i = selectors.size() - 1; i >= 0 && allSelectorsApply; i--) {
			CSSSelector selector = selectors.get(i);
			if (selector.is(CSSTypeSelector.class)) {
				allSelectorsApply = allSelectorsApply && typeSelectorAppliesOnCurrentVisualObject(selector.as(CSSTypeSelector.class));
			} else if (selector.is(CSSChildSelector.class)) {
				CSSTypeSelector parentTypeSelector = selectors.get(i - 1).as(CSSTypeSelector.class);
				allSelectorsApply = allSelectorsApply && typeChildSelectorAppliesOnCurrentVisualObject(parentTypeSelector);
				i--;
			}
		}
		
		return allSelectorsApply;
	}

	private boolean typeChildSelectorAppliesOnCurrentVisualObject(CSSTypeSelector parentTypeSelector) {
		Container owner = currentVisualObject.getOwnerContainer();
		while (owner != null) {
			currentVisualObject = owner;
			if (typeSelectorAppliesOnCurrentVisualObject(parentTypeSelector)) {
				return true;
			}

			owner = owner.getOwnerContainer();
		}
		// iterate through the parents, until one is found that applies onto the current type
		return false;
	}

	private boolean typeSelectorAppliesOnCurrentVisualObject(CSSTypeSelector as) {
		String n = as.getTypeName();
		if (n.startsWith(".")) {
			String styleClasses = currentVisualObject.getStyleClasses();
			if (styleClasses != null) {
				String[] styleClassesArray = styleClasses.split(" ");
				if (styleClassesArray != null) {
					for (String styleName : styleClassesArray) {
						if (("." + styleName).equals(n)) {
							return true;
						}
					}
				}
			}
		} else {
			Class<? extends VisualObject> clazz = currentVisualObject.getClass();
			String baseClass = clazz.getSimpleName();
			baseClass = StringUtil.lowerCaseFirstLetter(baseClass);
			if (n.equals(baseClass)) {
				return true;
			}
		}
		return false;
	}

}
