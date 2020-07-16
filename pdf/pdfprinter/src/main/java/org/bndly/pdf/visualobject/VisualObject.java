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

import java.util.List;

import org.bndly.pdf.Point2D;
import org.bndly.pdf.PrintingContext;
import org.bndly.pdf.PrintingObject;
import org.bndly.pdf.PrintingObjectImpl;
import org.bndly.pdf.style.CalculatedStyle;
import org.bndly.pdf.style.Style;
import org.bndly.pdf.style.StyleAttributes;

public abstract class VisualObject extends PrintingObjectImpl {
	private Double minWidth;
	private Double width;
	private Double maxWidth;
	private Double minHeight;
	private Double height;
	private Double maxHeight;
	private Point2D relativePosition;
	private Style style;
	protected Container ownerContainer;
	private String styleClasses;

	public VisualObject(PrintingContext context, PrintingObject owner) {
		super(context, owner);
		relativePosition = Point2D.ZERO;
	}
	
	public Container getOwnerContainer() {
		return ownerContainer;
	}
	public Style getStyle() {
		return style;
	}
	public void setStyle(Style style) {
		this.style = style;
	}
	public Style createStyle() {
		style = new Style(getContext(), this);
		return style;
	}
	public Double getMinWidth() {
		if (minWidth != null) {
			return minWidth;
		} else {
			Style s = getCalculatedStyle();
			return s.get(StyleAttributes.WIDTH);
		}
	}
	public void setMinWidth(Double minWidth) {
		this.minWidth = minWidth;
	}
	public Double getWidth() {
		return width;
	}
	public void setWidth(Double width) {
		this.width = width;
	}
	public Double getMaxWidth() {
		if (maxWidth != null) {
			return maxWidth;
		} else {
			Style s = getCalculatedStyle();
			return s.get(StyleAttributes.WIDTH);
		}
	}
	public void setMaxWidth(Double maxWidth) {
		this.maxWidth = maxWidth;
	}
	public Double getMinHeight() {
		if (minHeight != null) {
			return minHeight;
		} else {
			Style s = getCalculatedStyle();
			return s.get(StyleAttributes.HEIGHT);
		}
	}
	public void setMinHeight(Double minHeight) {
		this.minHeight = minHeight;
	}
	public Double getHeight() {
		return height;
	}
	public void setHeight(Double height) {
		this.height = height;
	}
	public Double getMaxHeight() {
		if (maxHeight != null) {
			return maxHeight;
		} else {
			Style s = getCalculatedStyle();
			return s.get(StyleAttributes.HEIGHT);
		}
	}
	public void setMaxHeight(Double maxHeight) {
		this.maxHeight = maxHeight;
	}
	public Point2D getRelativePosition() {
		return relativePosition;
	}
	public void setRelativePosition(Point2D relativePosition) {
		this.relativePosition = relativePosition;
	}
	public Style getCalculatedStyle() {
		return CalculatedStyle.createFrom(this);
	}
	public Double getRatio() {
		Double w = getWidth();
		Double h = getHeight();
		if (w != null && h != null) {
			return w / h;
		}
		return null;
	}
	@Override
	public String toString() {
		return getItemId() + " - w:" + getWidth() + " h:" + getHeight();
	}

	protected String asStringIndented(int indent) {
		StringBuffer sb = new StringBuffer();
		int i;
		for (i = 0; i < indent; i++) {
			sb.append("  ");
		}
		sb.append(asString());
		return sb.toString();
	}
	
	protected String asString() {
		return new StringBuffer()
				.append(this.getClass().getSimpleName())
				.append(" (").append(getItemId())
				.append(", id: ").append(getId())
				.append(", w: ").append(getWidth())
				.append(", h: ").append(getHeight())
				.append(", x: ").append(relativePosition.getX())
				.append(", y: ").append(relativePosition.getY())
				.append(")").toString();
	}
	public Point2D getAbsolutePosition() {
		Point2D abs = getRelativePosition();
		Container o = getOwnerContainer();
		while (o != null) {
			Point2D p = o.getRelativePosition();
			abs = abs.moveX(p.getX()).moveY(p.getY());
			o = o.getOwnerContainer();
		}
		return abs;
	}
	public <T extends VisualObject> T getOwnerContainerAs(Class<T> type) {
		return getOwnerContainer().as(type);
	}
	
	public VisualObject getNextSibling() {
		List<VisualObject> items = ownerContainer.getItems();
		if (items != null) {
			int i = items.indexOf(this);
			if (i > -1) {
				if (i + 1 < items.size()) {
					return items.get(i + 1);
				}
			} else {
				throw new IllegalStateException("visual object is refering to an owner container, but the visualObject is not part of the sub items.");
			}
		} else {
			throw new IllegalStateException("visual object is refering to an owner container, that has no sub items.");
		}
		
		return null;
	}

	public VisualObject getPreviousSibling() {
		List<VisualObject> items = ownerContainer.getItems();
		if (items != null) {
			int i = items.indexOf(this);
			if (i > -1) {
				if (i - 1 >= 0) {
					return items.get(i - 1);
				}
			} else {
				throw new IllegalStateException("visual object is refering to an owner container, but the visualObject is not part of the sub items.");
			}
		} else {
			throw new IllegalStateException("visual object is refering to an owner container, that has no sub items.");
		}
		
		return null;
	}
	
	protected abstract VisualObject copyTo(VisualObject owner);
	
	public final VisualObject copyTo(Container container) {
		VisualObject copy = copyTo((VisualObject)container);
		container.add(copy);
		return copy;
	}
	
	protected final <E extends VisualObject> E copyStyleClassesTo(E copy) {
		copy.setStyleClasses(styleClasses);
		return copy;
	}
	
	protected final VisualObject copyAttributesTo(VisualObject copy) {
		copy.setMaxHeight(maxHeight);
		copy.setMaxWidth(maxWidth);
		copy.setMinHeight(minHeight);
		copy.setMinWidth(minWidth);
		copy.setWidth(width);
		copy.setHeight(height);
		if (getItemId() != null) {
			copy.setItemId(getItemId() + "Clone");
		}
		copy.setStyleClasses(styleClasses);
		
		if (style != null) {
			Style copyStyle = copy.createStyle();
			style.overwriteTo(copyStyle);
		}
		
		return copy;
	}
	
	/**
	 * this method is used to prepare a visual object for a vertical overflow situation.
	 * by default a visualObject deals with a vertical overflow by adding itself to the overflowToContainer
	 * @param overflowToContainer
	 * @param maxHeight the maximum height that was allowed for the visualObject without causing a verticalOverflow
	 */
	public void overflowVerticalInto(Container overflowToContainer, Double maxHeight) {
		overflowToContainer.add(this);
	}
	public String getStyleClasses() {
		return styleClasses;
	}
	public void setStyleClasses(String styleClasses) {
		this.styleClasses = styleClasses;
	}
	
	public void destroy() {
		if (ownerContainer != null) {
			ownerContainer.remove(this);
		}
	}

	public int indexInOwnerCt() {
		if (ownerContainer == null) {
			return -1;
		}
		return ownerContainer.getItems().indexOf(this);
	}

}
