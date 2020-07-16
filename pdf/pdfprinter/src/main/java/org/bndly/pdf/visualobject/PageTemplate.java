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
import java.util.List;

public class PageTemplate extends Container {

	public PageTemplate(PrintingContext context, PrintingObject owner) {
		super(context, owner);
		setLayout(getContext().createAbsoluteLayout(this));
	}
	
	@Override
	public PageTemplate copyToWithoutChildren(VisualObject owner) {
		PageTemplate copyContainer = getContext().createPageTemplate(owner);
		return copyStyleClassesTo(copyContainer);
	}
	
	@Override
	public PageTemplate copyTo(VisualObject owner) {
		PageTemplate copyContainer = getContext().createPageTemplate(owner);
		List<VisualObject> subs = getItems();
		if (subs != null) {
			for (VisualObject sub : subs) {
				sub.copyTo(copyContainer);
			}
		}
		return copyContainer;
	}
}
