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
import org.bndly.pdf.visualobject.Image;
import org.bndly.pdf.visualobject.Paragraph;
import org.bndly.pdf.visualobject.Text;
import org.bndly.pdf.visualobject.VisualObject;

public abstract class AbstractVerticalLayout extends Layout {
	
	protected AbstractVerticalLayout(PrintingContext printingContext, Container layoutedContainer) {
		super(printingContext, layoutedContainer);
	}
	
	protected void handleHorizontalContainerOverflow(Container container, Double maxWidth) {
		List<VisualObject> items = container.getItems();
		if (items != null) {
			for (VisualObject visualObject : items) {
				Double w = visualObject.getWidth();
				if (w != null && w > maxWidth) {
					if (visualObject.is(Text.class)) {
						handleHorizontalContainerOverflowForText(container, maxWidth, (Text) visualObject);
					} else if (visualObject.is(Image.class)) {
						handleHorizontalContainerOverflowForImage(container, maxWidth, (Image) visualObject);
					}
				}
			}
		}

		performRendering(container); // handle the layout as long until it fits!
	}

	protected void handleHorizontalContainerOverflowForImage(Container container, Double maxWidth, Image visualObject) {
		Double ratio = maxWidth / visualObject.getWidth();
		visualObject.setWidth(maxWidth);
		if (ratio != null) {
			visualObject.setHeight(visualObject.getHeight() * ratio);
		}
	}

	protected void handleHorizontalContainerOverflowForText(Container container, Double maxWidth, Text visualObject) {
		String textValue = visualObject.getValue();
		int i = textValue.length();
		do {
			i = textValue.lastIndexOf(" ", i - 1);
			if (i > -1) {
				visualObject.setValue(textValue.substring(0, i));
			}
		} while (i > -1 && visualObject.getWidth() > maxWidth);

		if (visualObject.getWidth() > maxWidth) {
			i = textValue.length();
			do {
				i--;
				if (i > -1) {
					visualObject.setValue(textValue.substring(0, i));
				}
			} while (i > -1 && visualObject.getWidth() > maxWidth);
		}

		String remainingTextValue = textValue.substring(i);
		
		Text remainingText = getContext().createText(container);
		remainingText.setValue(remainingTextValue);
		container.insertAfter(remainingText, visualObject);
		remainingText.setStyle(visualObject.getStyle());
	}
	
	/**
	 * deals with the "left" or "right" alignment within the container
	 * @param container
	 */
	protected void horizontallyAlignChildren(Container container) {
		Style style = container.getCalculatedStyle();
		boolean rightAligned = style.isRightAligned();
		Double mt = style.get(StyleAttributes.MARGIN_TOP);
		mt = mt == null ? 0 : mt;
		Double mr = style.get(StyleAttributes.MARGIN_RIGHT);
		mr = mr == null ? 0 : mr;
		Double ml = style.get(StyleAttributes.MARGIN_LEFT);
		ml = ml == null ? 0 : ml;
		
		List<VisualObject> items = container.getItems();
		if (items != null) {
			double currentX = 0d;
			double currentY = 0d + mt;
			if (rightAligned) {
				currentX = container.getWidth() - mr;
			} else {
				currentX += ml;
			}
			for (VisualObject visualObject : items) {
				double x = currentX;
				double y = currentY;
				if (visualObject.is(Text.class)) {
					Text text = (Text) visualObject;
					Double fontSize = text.getFontSize();
					if (fontSize == null) {
						fontSize = 1d;
					}
					Double bls = text.getCalculatedStyle().get(StyleAttributes.FONT_BASELINE_SHIFT);
					if (bls == null) {
						throw new IllegalStateException("base line shift for " + text.getClass().getSimpleName() + " " + text.getItemId() + " was null.");
					}
					y -= fontSize * bls;
					Style tStyle = text.getCalculatedStyle();
					if (tStyle.isRightAligned()) {
						x = container.getWidth() - mr - text.getWidth();
					} else {
						x = ml;
					}
				} else if (visualObject.is(Paragraph.class)) {
					Paragraph p = visualObject.as(Paragraph.class);
					Style tStyle = p.getCalculatedStyle();
					if (tStyle.isRightAligned()) {
						x = container.getWidth() - mr - p.getWidth();
					} else {
						x = ml;
					}
				} else {
					if (rightAligned) {
						x = currentX - visualObject.getWidth();
					}
				}

				visualObject.setRelativePosition(new Point2D(x, y));

				currentY += visualObject.getHeight();
			}
		}
	}
}
