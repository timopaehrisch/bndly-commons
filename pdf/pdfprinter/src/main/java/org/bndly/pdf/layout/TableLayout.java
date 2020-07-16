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
import org.bndly.pdf.visualobject.Container;
import org.bndly.pdf.visualobject.VisualObject;

public class TableLayout extends ColumnLayout {

	public TableLayout(PrintingContext printingContext, Container ownerContainer) {
		super(printingContext, ownerContainer);
	}
	
	@Override
	protected void performRendering(Container container) {
		Double maxContentHeight = getMaxContentHeight(container);
		
		super.performRendering(container);
		
		Double calculatedContentHeight = alignCellsAsRows(container);

		if (isSignificantlyLower(maxContentHeight, calculatedContentHeight)) {
			handleVerticalContainerOverflow(container, container, maxContentHeight, maxContentHeight);
		}
	}

	protected Double alignCellsAsRows(Container container) {
		Double calculatedContentHeight = 0d;
		List<VisualObject> columns = container.getItems();
		if (columns != null) {
			// handle vertical layouting for the cells
			int rowCount = getRowCount(columns); // while looking in all columns
			int i = 0;
			// each row
			for (i = 0; i < rowCount; i++) {
				double rowHeight = getRowHeightForRow(i, columns);
				calculatedContentHeight += rowHeight;
				// each column
				for (VisualObject visualObject : columns) {
					Container column = (Container) visualObject; //items.get(i);
					List<VisualObject> rows = column.getItems();
					if (rows != null && rows.size() > i) {
						VisualObject cell = rows.get(i);
						Double cellHeight = cell.getHeight();
						if (cellHeight == null) {
							if (cell.is(Container.class)) {
								Container cellContainer = (Container) cell;
								cellContainer.doLayout();
								cellHeight = cellContainer.getHeight();
							}
						}
						if (cellHeight < rowHeight) {
							double delta = rowHeight - cellHeight;
							for (int j = i + 1; j < rows.size(); j++) {
								VisualObject cellToMove = rows.get(j);
								Point2D pos = cellToMove.getRelativePosition();
								pos = pos.moveY(delta);
								cellToMove.setRelativePosition(pos);
							}
							cell.setHeight(rowHeight);
						}
					}
				}
			}

			Double columnMaxHeight = 0d;
			for (VisualObject visualObject : columns) {
				Double colHeight = calculatedContentHeight + getTotalVerticalMargins(visualObject);
				visualObject.setHeight(colHeight);
				if (colHeight > columnMaxHeight) {
					columnMaxHeight = colHeight;
				}
			}

			container.setHeight(columnMaxHeight + getTotalVerticalMargins(container));
			calculatedContentHeight = columnMaxHeight;
		}
		return calculatedContentHeight;
	}

	protected double getColumnHeight(Container column) {
		double result = 0d;
		List<VisualObject> items = column.getItems();
		if (items != null) {
			for (VisualObject visualObject : items) {
				result += visualObject.getHeight();
			}
		}

		return result;
	}
	
	public double getRowHeightForRow(int i, List<VisualObject> items) {
		double max = 0;
		for (VisualObject visualObject : items) {
			Container c = (Container) visualObject;
			List<VisualObject> rows = c.getItems();
			if (rows != null && rows.size() > i) {
				VisualObject cell = rows.get(i);
				Double h = cell.getHeight();
				if (h > max) {
					max = h;
				}
			}
		}
		
		return max;
	}
	
	public int getRowCount(List<VisualObject> items) {
		int max = 0;
		for (VisualObject visualObject : items) {
			Container c = (Container) visualObject;
			List<VisualObject> rows = c.getItems();
			if (rows != null && max < rows.size()) {
				max = rows.size();
			}
		}
		return max;
	}
}
