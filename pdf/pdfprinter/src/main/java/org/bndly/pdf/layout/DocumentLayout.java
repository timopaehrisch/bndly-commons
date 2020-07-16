package org.bndly.pdf.layout;

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

import org.bndly.pdf.Point2D;
import org.bndly.pdf.PrintingContext;
import org.bndly.pdf.style.Style;
import org.bndly.pdf.style.StyleAttributes;
import org.bndly.pdf.visualobject.Container;
import org.bndly.pdf.visualobject.Document;
import org.bndly.pdf.visualobject.VisualObject;

public class DocumentLayout extends Layout {

	public DocumentLayout(PrintingContext printingContext, Document ownerDocument) {
		super(printingContext, ownerDocument);
	}
	
	@Override
	protected void performRendering(Container container) {
		Style docStyle = container.getStyle();
		if (docStyle == null) {
			docStyle = container.createStyle();
		}
		docStyle.set(StyleAttributes.MARGIN_TOP, new Double(0));
		docStyle.set(StyleAttributes.MARGIN_RIGHT, new Double(0));
		docStyle.set(StyleAttributes.MARGIN_BOTTOM, new Double(0));
		docStyle.set(StyleAttributes.MARGIN_LEFT, new Double(0));

		List<VisualObject> items = container.getItems();
		if (items != null) {
			for (VisualObject visualObject : items) {
				if (Container.class.isAssignableFrom(visualObject.getClass())) {
					Container subContainer = (Container) visualObject;
					subContainer.doLayout();
					Double mw = subContainer.getMaxWidth();
					if (mw != null) {
						subContainer.setWidth(mw);
					}
					Double mh = subContainer.getMaxHeight();
					if (mh != null) {
						subContainer.setHeight(mh);
					}
				}
			}

			// reset page positions
			items = container.getItems();
			for (VisualObject visualObject : items) {
				if (Container.class.isAssignableFrom(visualObject.getClass())) {
					Container subContainer = (Container) visualObject;
					subContainer.setRelativePosition(Point2D.ZERO);
				}
			}
		}

	}
	
	/**
	 * this method is overridden, because a document has no width.
	 */
	@Override
	protected Double getMaxContentWidth(Container container) {
		return null;
	}

}
