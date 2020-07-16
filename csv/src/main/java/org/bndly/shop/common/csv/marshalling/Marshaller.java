package org.bndly.shop.common.csv.marshalling;

/*-
 * #%L
 * CSV
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

public class Marshaller {

	/*
	private DocumentImpl d;
	private Stack<String> propertyStack;
	private Map<String, Column> allColumns;
	private List<ColumnRowBinding> values;
	private Map<Column, List<ColumnRowBinding>> valuesByColumn;
	private static final Class<?>[] simpleClasses = new Class<?>[]{String.class, byte.class, Byte.class, short.class, Short.class, int.class, Integer.class, long.class, Long.class, float.class, Float.class, double.class, Double.class, char.class, Character.class, Date.class, BigDecimal.class};
	private static final Class<?>[] ignoredClasses = new Class<?>[]{Class.class};
	private Set<Class<?>> simpleTypeSet;
	private Set<Class<?>> ignoredTypeSet;
	private Stack<IndexedRow> rowStack;
	private List<IndexedRow> rows;
	private IndexedRow columnLabels;

	public DocumentImpl marshall(Object javaObject) throws CSVMarshallingException {
		allColumns = new HashMap<>();
		values = new ArrayList<>();
		rows = new ArrayList<>();
		valuesByColumn = new HashMap<>();
		simpleTypeSet = new HashSet<>();
		ignoredTypeSet = new HashSet<>();
		simpleTypeSet.addAll(Arrays.asList(simpleClasses));
		ignoredTypeSet.addAll(Arrays.asList(ignoredClasses));
		propertyStack = new Stack<>();
		rowStack = new Stack<>();
		
		d = new DocumentImpl();
		columnLabels = new IndexedRow(d, d.getRows().size());
		d.addRow(columnLabels);
		
		IndexedRow r = new IndexedRow(d, d.getRows().size());
		rowStack.add(r);
		rows.add(r);
		handleObject(javaObject);
		long rowIndex = 0;
		for (Map.Entry<String, Column> entry : allColumns.entrySet()) {
			Column col = entry.getValue();
			addColumn(new ValueImpl(col.getName()));
			for (IndexedRow row : rows) {
				if (!row.isMutated()) {
					ValueImpl value = getValueForColumnInRow(col, row);
					if (value == null) {
						value = new ValueImpl("");
					}
					row.addValue(value);
					if (row.getIndex() == null) {
						row.setIndex(rowIndex++);
						d.addRow(row);
					}
				}
			}
		}
		return d;
	}

	private ValueImpl getValueForColumnInRow(Column col, IndexedRow row) {
		List<ColumnRowBinding> rowCandits = valuesByColumn.get(col);
		for (ColumnRowBinding columnRowBinding : rowCandits) {
			if (columnRowBinding.getRow() == row) {
				return columnRowBinding.getValue();
			}
		}
		if (row.getParent() != null) {
			return getValueForColumnInRow(col, row.getParent());
		}
		return null;
	}

	private void handleObject(Object javaObject) throws CSVMarshallingException {
		if (javaObject != null) {
			Class<?> javaObjectType = javaObject.getClass();
			if (!isIgnoredType(javaObjectType)) {
				if (Collection.class.isAssignableFrom(javaObjectType)) {
					handleCollection((Collection) javaObject);
				} else if (isSimpleValueType(javaObjectType)) {
					String colLabel = convertPropertyStackToLabel();

					Column col = allColumns.get(colLabel);
					if (col == null) {
						col = new Column(colLabel);
						allColumns.put(colLabel, col);
					}
					handleValue(javaObject, col);
				} else {
					handleComplexObject(javaObject);
				}
			}
		}
	}

	private void handleComplexObject(Object complexObject) throws CSVMarshallingException {
		Class<?> javaObjectType = complexObject.getClass();
		Map<String, Method> getters = new HashMap<>();
		new PropertyAccessor().addGettersAsProperties(javaObjectType, getters);
		for (Map.Entry<String, Method> entry : getters.entrySet()) {
			String propertyName = entry.getKey();
			Method getter = entry.getValue();
			try {
				Object propertyJavaValue = getter.invoke(complexObject);
				propertyStack.add(propertyName);
				handleObject(propertyJavaValue);
				propertyStack.pop();
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
				throw new CSVMarshallingException("exception while retrieving value for marshalling via CSV: "+ex.getMessage(), ex);
			}
		}
	}

	private void handleValue(Object value, Column column) {
		IndexedRow currentRow = rowStack.peek();
		ValueImpl v = new ValueImpl();
		if (value == null) {
			v.setRaw("");
		} else {
			v.setRaw(value.toString());
		}
		ColumnRowBinding binding = new ColumnRowBinding(column, currentRow);
		binding.setValue(v);
		values.add(binding);
		List<ColumnRowBinding> l = valuesByColumn.get(column);
		if (l == null) {
			l = new ArrayList<>();
			l.add(binding);
			valuesByColumn.put(column, l);
		}
		l.add(binding);
	}

	private void handleCollection(Collection collection) throws CSVMarshallingException {
		IndexedRow baseRow = rowStack.peek();
		for (Object object : collection) {
			IndexedRow mutation = baseRow.createMutation();
			rows.add(mutation);
			rowStack.add(mutation);
			handleObject(object);
			rowStack.pop();
		}
	}

	private boolean isSimpleValueType(Class<?> javaObjectType) {
		if (simpleTypeSet.contains(javaObjectType)) {
			return true;
		}
		return false;
	}

	private boolean isIgnoredType(Class<?> javaObjectType) {
		if (ignoredTypeSet.contains(javaObjectType)) {
			return true;
		}
		return false;
	}

	private String convertPropertyStackToLabel() {
		List<String> tmp = new ArrayList<>(propertyStack);
		StringBuffer sb = new StringBuffer();
		String separator = "";
		for (String string : tmp) {
			sb.append(separator);
			sb.append(string);
			separator = ".";
		}
		return sb.toString();
	}

	private void addColumn(ValueImpl label) {
		if (columnLabels == null) {
			columnLabels = new IndexedRow();
		}
		columnLabels.addValue(label);
	}
	*/
}
