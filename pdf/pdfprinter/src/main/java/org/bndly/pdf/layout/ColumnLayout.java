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

import java.util.ArrayList;
import java.util.List;

import org.bndly.pdf.Point2D;
import org.bndly.pdf.PrintingContext;
import org.bndly.pdf.PrintingObject;
import org.bndly.pdf.style.Style;
import org.bndly.pdf.style.StyleAttributes;
import org.bndly.pdf.visualobject.Container;
import org.bndly.pdf.visualobject.VisualObject;

public class ColumnLayout extends Layout {

	protected ColumnLayout(PrintingContext printingContext, Container layoutedContainer) {
		super(printingContext, layoutedContainer);
	}
	
	@Override
	protected void performRendering(Container container) {
		Double maxContentWidth = getMaxContentWidth(container);
		
		Double calculatedContentWidth = 0d;
		
		wrapChildElementsInContainers(container);

		List<Container> columns = container.getItems(Container.class);
		if (columns != null) {
			Double currentX = 0d;
			for (Container column : columns) {
				column.setMinWidth(getMaxWidthForSubContainer(column));
				column.doLayout();

				Double w = column.getWidth();
				calculatedContentWidth += w;
				column.setRelativePosition(new Point2D(currentX, 0d));
				currentX += w;
			}
		}
		
		
		Double calculatedContentHeight = 0d;
		if (columns != null) {
			for (Container column : columns) {
				Double h = column.getHeight();
				if (h > calculatedContentHeight) {
					calculatedContentHeight = h;
				}
			}
	
			for (Container column : columns) {
				column.setHeight(calculatedContentHeight);
			}
		}
		
		Double calculatedHeight = calculatedContentHeight + getTotalVerticalMargins(container);
		Double calculatedWidth = calculatedContentWidth + getTotalHorizontalMargins(container);
		
		if (!isScalingTable()) {
			container.setWidth(calculatedWidth);
		}
		container.setHeight(calculatedHeight);

		if (isSignificantlyLower(maxContentWidth, calculatedContentWidth)) {
			throw new IllegalStateException("layout did not pay attention to maxWidth for container " + container.getItemId());
		}
	}

	private void handleHorizontalOverflow(Container container, Double calculatedWidth, Double maxWidth) {
		// deal with overflowing tables. don't let columns grow until others can't be shrink anymore 
		List<VisualObject> items = container.getItems();
		Double flex = null;
		Double flexTotal = 0d;
		for (VisualObject visualObject : items) {
			Style vStyle = visualObject.getStyle();
			flex = vStyle.get(StyleAttributes.FLEX);
			if (flex == null) {
				flex = 1d;
				vStyle.set(StyleAttributes.FLEX, flex);
			}
			flexTotal += flex;
		}

		
		double extraSpaceToDistribute = 0;
		List<VisualObject> extraSpaceReceivers = new ArrayList<>();
		for (VisualObject visualObject : items) {
			Style vStyle = visualObject.getStyle();
			flex = vStyle.get(StyleAttributes.FLEX);
			double relativeWidth = flex / flexTotal;
			Double mWidth = relativeWidth * maxWidth;
			Double vMaxWidth = visualObject.getMaxWidth();
			if (vMaxWidth != null && vMaxWidth < mWidth) {
				extraSpaceToDistribute += mWidth - vMaxWidth;
				mWidth = vMaxWidth;
			} else {
				extraSpaceReceivers.add(visualObject);
			}
			visualObject.setMinWidth(mWidth);
			visualObject.setMaxWidth(mWidth);
		}
		
		if (isSignificantlyLower(0, extraSpaceToDistribute)) {
			flexTotal = 0d;
			for (VisualObject visualObject : extraSpaceReceivers) {
				Style vStyle = visualObject.getStyle();
				flex = vStyle.get(StyleAttributes.FLEX);
				flexTotal += flex;
			}
			
			for (VisualObject visualObject : extraSpaceReceivers) {
				Style vStyle = visualObject.getStyle();
				flex = vStyle.get(StyleAttributes.FLEX);
				double relativeWidth = flex / flexTotal;
				Double mWidth = visualObject.getMinWidth();
				mWidth += relativeWidth * extraSpaceToDistribute;
				visualObject.setMinWidth(mWidth);
				visualObject.setMaxWidth(mWidth);
			}
		}
		
		container.setWidth(maxWidth);
		performRendering(container);
	}

	protected void wrapChildElementsInContainers(Container container) {
		List<VisualObject> items = container.getItems();
		if (items != null) {
			for (VisualObject visualObject : items) {
				if (!Container.class.isAssignableFrom(visualObject.getClass())) {
					Container wrapperContainer = container.getContext().createContainer(container);
					wrapperContainer.add(visualObject);
					container.insertAfter(wrapperContainer, visualObject);
				}
			}
		}
		
	}

	public Double getMaxWidthForSubContainer(Container container) {
		Double result = null;
		Container owningContainer = container.getOwnerContainer();
		PrintingObject layoutOwner = getOwner();
		if (owningContainer != layoutOwner) {
			throw new IllegalStateException(
					"layout " + getId() + " is used to calculate the max width for a visual object " + container.getId() + ", that is not a subelement (actual owner: " 
					+ owningContainer.getId() + " " + owningContainer.getItemId() + ") of the container " + layoutOwner.getId() + " this layout is controlling."
			);
		}
		Double ownerMaxWidth = getMaxContentWidth(owningContainer);

		double distributableSpace = 0;
		double distributableFlexTotal = 0;
		List<VisualObject> flexDistributableItems = new ArrayList<>();
		List<VisualObject> subItems = owningContainer.getItems();
		if (subItems != null) {
			// patch the flex attribute for all columnLayout members
			double flexTotal = 0d;
			for (VisualObject visualObject : subItems) {
				Style vStyle = visualObject.getStyle();
				if (vStyle == null) {
					vStyle = visualObject.createStyle();
				}
				Double flex = vStyle.get(StyleAttributes.FLEX);
				if (flex == null) {
					flex = new Double(1);
					vStyle.set(StyleAttributes.FLEX, flex);
				}
				flexTotal += flex;
			}
			
			for (VisualObject visualObject : subItems) {
				Double mw = visualObject.getMaxWidth();

				Style vStyle = visualObject.getStyle();
				Double flex = vStyle.get(StyleAttributes.FLEX);

				double flexedWidth = flex / flexTotal * ownerMaxWidth;
				if (mw != null && flexedWidth > mw) {
					distributableSpace += flexedWidth - mw;
				} else {
					distributableFlexTotal += flex;
					flexDistributableItems.add(visualObject);
				}
			}
		
			boolean didFindContainer = false;
			for (VisualObject visualObject : subItems) {
				if (visualObject == container) {
					Style vStyle = visualObject.getStyle();
					Double flex = vStyle.get(StyleAttributes.FLEX);

					double flexedWidth = flex / flexTotal * ownerMaxWidth;
					if (flexDistributableItems.contains(visualObject)) {
						flexedWidth += flex / distributableFlexTotal * distributableSpace;
					} else if (flexedWidth > visualObject.getMaxWidth()) {
						flexedWidth = visualObject.getMaxWidth();
					}
					result = flexedWidth;
					didFindContainer = true;
				}
			}
			if (!didFindContainer) {
				throw new IllegalStateException(
						"maxwidth could not be determined, because the visual object " + container.getItemId()
						+ " was not contained by its referenced owning container. owner container: " + owningContainer.getItemId()
				);
			}
		} else {
			throw new IllegalStateException("a maxwidth can only be determined for direct subelements. the owning container " + owningContainer.getItemId() + " had no sub elements.");
		}
		
		if (result == null) {
			throw new IllegalStateException(
				"a maxwidth could not be determined for container " + container.getItemId()
				+ ", but is required to perform the layouting process. owner container: " + owningContainer.getItemId()
			);
		}
		return result;
	}
	
}
