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
import org.bndly.pdf.PrintingObjectImpl;
import org.bndly.pdf.style.Style;
import org.bndly.pdf.style.StyleAttributes;
import org.bndly.pdf.visualobject.Container;
import org.bndly.pdf.visualobject.VisualObject;


public abstract class Layout extends PrintingObjectImpl {
	private boolean isScalingTable;
	private VerticalOverflowStrategy verticalOverflowStrategy;

	protected Layout(PrintingContext printingContext, Container layoutedContainer) {
		super(printingContext, layoutedContainer);
	}
	
	public void render() {
		Container owner = (Container) getOwner();
		performRendering(owner);
	}
	
	/**
	 * used to prevent infinite recursion
	 */
	protected boolean isScalingTable() {
		return isScalingTable;
	}
	
	protected Double getMaxContentWidth(Container container) {
		Double max = container.getMaxWidth();
		if (max == null) {
			Container owner = container.getOwnerContainer();
			if (owner == null) {
				throw new IllegalStateException(
					"container had no owner, but the owner was required to calculate maxWidth. container: " + container.getClass().getSimpleName() + " " + container.getItemId()
				);
			}
			Layout layout = owner.getLayout();
			if (layout == null) {
				throw new IllegalStateException("container had no layout. " + owner.getClass().getSimpleName() + " " + owner.getItemId());
			}
			if (layout.is(ColumnLayout.class)) {
				// treat column layouts differently
				return owner.getLayout().as(ColumnLayout.class).getMaxWidthForSubContainer(container);
			}
			if (owner != null) {
				max = getMaxContentWidth(owner);
			}
		}
		Style style = container.getCalculatedStyle();
		Double mr = style.get(StyleAttributes.MARGIN_RIGHT);
		Double ml = style.get(StyleAttributes.MARGIN_LEFT);

		if (max == null) {
			throw new IllegalStateException("container has no maxWidth and no parent that defines one: " + container.getItemId());
		}

		if (mr != null) {
			max -= mr;
		}
		if (ml != null) {
			max -= ml;
		}
		return max;
	}
	
	protected Double getMaxContentHeight(Container controlledSubContainer) {
		Double max = controlledSubContainer.getMaxHeight();

		if (max == null) {
			Container ownerCt = controlledSubContainer.getOwnerContainer();
			if (ownerCt != null) {
				Layout ownerLayout = ownerCt.getLayout();
				max = ownerLayout.getMaxContentHeight(ownerCt);
				if (ownerLayout.is(VerticalLayout.class)) {
					VisualObject sibling = controlledSubContainer.getPreviousSibling();
					while (sibling != null) {
						Double h = sibling.getHeight();
						max -= h;
						sibling = sibling.getPreviousSibling();
					}
				}
			}
		}

		return max - getTotalVerticalMargins(controlledSubContainer);
	}
	
	protected boolean isSignificantlyLower(double isLower, double than) {
		return than - isLower > 0.000001;
	}
	
	protected double getTotalHorizontalMargins(VisualObject visualObject) {
		double result = 0;
		Style vStyle = visualObject.getCalculatedStyle();
		if (vStyle != null) {
			Double ml = vStyle.get(StyleAttributes.MARGIN_LEFT);
			Double mr = vStyle.get(StyleAttributes.MARGIN_RIGHT);
			if (ml != null) {
				result += ml;
			}
			if (mr != null) {
				result += mr;
			}
		}
		return result;
	}

	protected double getTotalVerticalMargins(VisualObject visualObject) {
		double result = 0;
		Style vStyle = visualObject.getCalculatedStyle();
		if (vStyle != null) {
			Double mt = vStyle.get(StyleAttributes.MARGIN_TOP);
			Double mb = vStyle.get(StyleAttributes.MARGIN_BOTTOM);
			if (mt != null) {
				result += mt;
			}
			if (mb != null) {
				result += mb;
			}
		}
		return result;
	}

	protected abstract void performRendering(Container container);
	
	protected void handleVerticalContainerOverflow(Container container, VisualObject overflownObject, Double maxHeight, Double overflownObjectMaxHeight) {
		if (overflownObject == null) {
			throw new IllegalStateException("overflow handlers require the overflownObject parameter to not be null.");
		}
		VerticalOverflowStrategy strategy = getVerticalOverflowStrategy(this);
		strategy.handleVerticalOverflowIn(container, overflownObject, maxHeight, overflownObjectMaxHeight);
		performRendering(container); // handle the layout as long until it fits!		
	}

	private VerticalOverflowStrategy getVerticalOverflowStrategy(Layout layout) {
		if (layout.verticalOverflowStrategy != null) {
			return layout.verticalOverflowStrategy;
		} else {
			Container ownerCt = layout.getOwner().as(Container.class).getOwnerContainer();
			if (ownerCt == null) {
				throw new IllegalStateException("ran into a vertical overflow situation and there was no overflow strategy registered.");
			}
			return getVerticalOverflowStrategy(ownerCt.getLayout());
		}
	}
	
	public void setVerticalOverflowStrategy(VerticalOverflowStrategy verticalOverflowStrategy) {
		this.verticalOverflowStrategy = verticalOverflowStrategy;
	}
	
}
