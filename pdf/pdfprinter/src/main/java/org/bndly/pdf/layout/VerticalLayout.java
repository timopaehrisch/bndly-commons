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
import org.bndly.pdf.visualobject.Text;
import org.bndly.pdf.visualobject.VisualObject;

public class VerticalLayout extends AbstractVerticalLayout {
	
	public VerticalLayout(PrintingContext printingContext, Container ownerContainer) {
		super(printingContext, ownerContainer);
	}
	
	@Override
	protected void performRendering(Container container) {
		// set all properties of the rendered container (width, height, position)
		Double maxContentWidth = getMaxContentWidth(container);
		Double maxContentHeight = getMaxContentHeight(container);
		Double minWidth = container.getMinWidth();
		Double minHeight = container.getMinHeight();
		
		
		// if the container has any specific boundaries, now we know them
		Double calculatedContentWidth = null;
		Double calculatedContentHeight = null;
		
		VisualObject overflowObject = null;
		Double overflownObjectMaxHeight = null;
		
		List<VisualObject> items = container.getItems();
		if (items != null) {
			// first of all: split newline-characters into multiple text objects
			for (VisualObject visualObject : items) {
				if (visualObject.is(Text.class)) {
					String textValue = visualObject.as(Text.class).getValue();
					String[] textElements = textValue.split("\n");
					if (textElements.length > 1) {
						visualObject.as(Text.class).setValue(textElements[0]);
						VisualObject predecessor = visualObject;
						for (int i = 1; i < textElements.length; i++) {
							String element = textElements[i];
							Text newLineText = getContext().createText(container);
							newLineText.setValue(element);
							container.insertAfter(newLineText, predecessor);
							predecessor = newLineText;
						}
					}
				}
			}

			items = container.getItems();
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
				
				double tmpHeight = calculatedContentHeight;
				
				if (maxContentHeight != null && isSignificantlyLower(maxContentHeight, tmpHeight) && overflowObject == null) {
					overflowObject = visualObject;
					overflownObjectMaxHeight = tmpHeight - maxContentHeight;
					break; // only layout as much items, as will fit into the container. the overflown items should be handled afterwards
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


		if (maxContentHeight != null) {
			if (calculatedContentHeight > maxContentHeight) {
				handleVerticalContainerOverflow(container, overflowObject, maxContentHeight, overflownObjectMaxHeight);
				calculatedContentHeight = maxContentHeight;
			}
		} else if (minHeight != null && calculatedContentHeight < minHeight - getTotalVerticalMargins(container)) {
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
		if (container.getHeight() == null) {
			container.setHeight(height);
		}

		// now that this container knows its width, its children can be aligned (alignment depends on the container width)
		horizontallyAlignChildren(container);
	}
	
}
