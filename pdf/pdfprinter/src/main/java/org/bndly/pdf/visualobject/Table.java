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

import org.bndly.pdf.style.Overflows;
import org.bndly.pdf.style.Style;
import org.bndly.pdf.style.StyleAttributes;

public class Table extends Container {

	public Table(PrintingContext context, PrintingObject owner) {
		super(context, owner);
		setLayout(getContext().createTableLayout(this));
	}
	
	public int getRowCount() {
		int max = 0;
		List<VisualObject> items = getItems();
		if (items != null) {
			for (VisualObject visualObject : getItems()) {
				Container c = (Container) visualObject;
				List<VisualObject> rows = c.getItems();
				if (rows != null && max < rows.size()) {
					max = rows.size();
				}
			}
		}
		return max;
	}
	
	public double getRowHeightForRow(int i) {
		double max = 0;
		List<VisualObject> items = getItems();
		if (items != null) {
			for (VisualObject visualObject : getItems()) {
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
		}
		
		return max;
	}
	
	@Override
	public void overflowVerticalInto(Container overflowToContainer, Double maxHeight) {
		Style cStyle = getCalculatedStyle();
		if (cStyle != null) {
			String overflowRaw = cStyle.get(StyleAttributes.OVERFLOW);
			Overflows overflow = null;
			if (overflowRaw != null) {
				overflow = Overflows.valueOf(overflowRaw);
			}
			if (overflow != null) {
				if (overflow.equals(Overflows.split)) {
					int rowCount = getRowCount();
					double tmp = 0;
					int i;
					boolean found = false;
					for (i = 0; i < rowCount; i++) {
						tmp += getRowHeightForRow(i);
						if (tmp > maxHeight) {
							i++;
							found = true;
							break;
						}
					}
					if (!found) {
						throw new IllegalStateException("actually the table " + getItemId() + " did not require to be split. what happened here?");
					}
					if (i > 0) {
						int rowsToMove = rowCount - i + 1;

						Table overflowToTable = copyToWithoutChildren(overflowToContainer).as(Table.class);
						overflowToContainer.add(overflowToTable);
						List<TableColumn> columns = getItems(TableColumn.class);
						for (TableColumn rawColumn : columns) {
							TableColumn clonedColumn = rawColumn.copyToWithoutChildren(overflowToTable).as(TableColumn.class);
							overflowToTable.add(clonedColumn);

							List<VisualObject> rows = rawColumn.getItems();
							int firstRowIndexToMove = i - 1;
							if (rows != null && rows.size() > firstRowIndexToMove) {
								for (int j = firstRowIndexToMove; j < rows.size(); j++) {
									clonedColumn.add(rows.get(j));
								}
							}
						}

						setHeight(maxHeight);
						return;
					}
				}
			}
		}
		super.overflowVerticalInto(overflowToContainer, maxHeight);
	}
	
	@Override
	public Table copyToWithoutChildren(VisualObject owner) {
		Table copyTable = getContext().createTable(owner);
		return copyStyleClassesTo(copyTable);
	}
	
	@Override
	public Table copyTo(VisualObject owner) {
		Table copyContainer = getContext().createTable(owner);
		List<VisualObject> subs = getItems();
		if (subs != null) {
			for (VisualObject sub : subs) {
				sub.copyTo(copyContainer);
			}
		}
		return copyContainer;
	}
}
