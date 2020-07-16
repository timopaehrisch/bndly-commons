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

import org.bndly.pdf.PrintingContext;
import java.util.List;

import org.bndly.pdf.visualobject.Container;
import org.bndly.pdf.visualobject.VisualObject;

public class TableColumnLayout extends AbstractVerticalLayout {

	public TableColumnLayout(PrintingContext printingContext, Container ownerContainer) {
		super(printingContext, ownerContainer);
	}
	
	@Override
	protected void performRendering(Container container) {
		// set all properties of the rendered container (width, height, position)
		Double maxContentWidth = getMaxContentWidth(container);
		Double minWidth = container.getMinWidth();
		Double minHeight = container.getMinHeight();
		
		Double calculatedContentWidth = null;
		Double calculatedContentHeight = null;
		
		List<VisualObject> items = container.getItems();
		if (items != null) {
			for (VisualObject visualObject : items) {
				// digg through the object graph and delegate the layout to other sub-layouts, if they are available

				if (visualObject.is(Container.class)) {
					Container subContainer = visualObject.as(Container.class);
					subContainer.doLayout();
				}
				Double width = visualObject.getWidth();
				Double height = visualObject.getHeight();

				// vertical layout gets as wide as the longest children
				if (width != null) {
					if (calculatedContentWidth == null || calculatedContentWidth < width) {
						calculatedContentWidth = width;
					}
				}

				// vertical layout gets as high as all children together
				if (height != null) {
					if (calculatedContentHeight == null) {
						calculatedContentHeight = height;
					} else {
						calculatedContentHeight += height;
					}
				} else {
					throw new IllegalStateException("visual object " + visualObject.getItemId() + " had no height after layouting.");
				}
			}
		}
		if (calculatedContentHeight == null) {
			calculatedContentHeight = 0d;
		}
		if (calculatedContentWidth == null) {
			calculatedContentWidth = 0d;
		}

		// since all children had the chance to add to the height of the container, 
		// the final height will now be determined.
		if (maxContentWidth != null) {
			if (calculatedContentWidth > maxContentWidth) {
				handleHorizontalContainerOverflow(container, maxContentWidth);
				calculatedContentWidth = maxContentWidth;
			}
		} else if (minWidth != null && calculatedContentWidth < minWidth - getTotalHorizontalMargins(container)) {
			calculatedContentWidth = minWidth - getTotalHorizontalMargins(container);
		}
		Double calculatedWidth = calculatedContentWidth + getTotalHorizontalMargins(container);

		if (minHeight != null && calculatedContentHeight < minHeight - getTotalVerticalMargins(container)) {
			calculatedContentHeight = minHeight - getTotalVerticalMargins(container);
		}
		Double calculatedHeight = calculatedContentHeight + getTotalVerticalMargins(container);

		
		// when an overflow container is created, its minHeight and minWidth should be set as the real width and height.
		if (minWidth != null && minWidth > calculatedWidth) {
			container.setWidth(minWidth);
		} else {
			container.setWidth(calculatedWidth);
		}

		Double height = null;
		if (minHeight != null && minHeight > calculatedHeight) {
			height = minHeight;
		} else {
			height = calculatedHeight;
		}
		
		// the if statement to prevent overwriting old height values after returning from the recursion of horizontal overflowing.
		container.setHeight(height);
		
		// now that this container knows its width, its children can be aligned (alignment depends on the container width)
		horizontallyAlignChildren(container);
	}

}
