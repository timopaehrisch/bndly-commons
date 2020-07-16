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
import org.bndly.pdf.visualobject.VisualObject;

public class AbsoluteLayout extends Layout {

	public AbsoluteLayout(PrintingContext printingContext, Container layoutedContainer) {
		super(printingContext, layoutedContainer);
	}
	
	@Override
	protected void performRendering(Container container) {
		Double w = 0d;
		Double h = 0d;
		
		List<VisualObject> items = container.getItems();
		if (items != null) {
			for (VisualObject visualObject : items) {
				if (visualObject.is(Container.class)) {
					Container subContainer = (Container) visualObject;
					subContainer.doLayout();
				}

				Style style = visualObject.getCalculatedStyle();
				Double top = style.get(StyleAttributes.TOP);
				Double left = style.get(StyleAttributes.LEFT);

				if (top == null) {
					top = 0d;
				}
				if (left == null) {
					left = 0d;
				}

				double x = left;
				double y = top;
				visualObject.setRelativePosition(new Point2D(x, y));
				Double vW = visualObject.getWidth();
				Double vH = visualObject.getHeight();
				if (vW == null) {
					visualObject.getWidth();
					throw new IllegalStateException(visualObject.getClass().getSimpleName() + " " + visualObject.getItemId() + " had no width after layouting");
				}
				if (vH == null) {
					throw new IllegalStateException(visualObject.getClass().getSimpleName() + " " + visualObject.getItemId() + " had no height after layouting");
				}
				double farX = x + vW;
				double farY = y + vH;
				if (farX > w) {
					w = farX;
				}
				if (farY > h) {
					h = farY;
				}
			}
		}
		
		Double minWidth = container.getMinWidth();
		Double minHeight = container.getMinHeight();

		if (minWidth != null && minWidth > w) {
			w = minWidth;
		}
		if (minHeight != null && minHeight > h) {
			h = minHeight;
		}

		Double maxWidth = container.getMaxWidth();
		Double maxHeight = container.getMaxHeight();
		if (maxWidth != null && maxWidth < w) {
			w = maxWidth;
		}

		if (maxHeight != null && maxHeight < h) {
			h = maxHeight;
		}

		container.setWidth(w);
		container.setHeight(h);
	}

}
