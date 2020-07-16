package org.bndly.shop.common.csv.model;

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

import org.bndly.shop.common.csv.CSVConfig;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class DocumentBuilder {
	
	private final CSVConfig config;
	private final DocumentImpl documentImpl = new DocumentImpl();
	private long currentRowIndex = -1;
	private long currentColumnIndex = -1;
	private RowImpl currentRow;

	public static DocumentBuilder newInstance() {
		return new DocumentBuilder(CSVConfig.DEFAULT);
	}
	
	public static DocumentBuilder newInstance(CSVConfig config) {
		return new DocumentBuilder(config);
	}

	public DocumentBuilder(CSVConfig config) {
		if (config == null) {
			throw new IllegalArgumentException("config is not allowed to be null");
		}
		this.config = config;
	}
	
	public DocumentBuilder row() {
		currentRowIndex++;
		currentRow = new RowImpl(documentImpl, currentRowIndex);
		currentColumnIndex = -1;
		documentImpl.addRow(currentRow);
		return this;
	}
	
	public DocumentBuilder value(String value) {
		if (currentRow == null) {
			row();
		}
		currentColumnIndex++;
		ValueImpl val = new ValueImpl(value, currentRow, currentColumnIndex, ValueImpl.requiresQuotes(config, value));
		currentRow.addValue(val);
		return this;
	}
	
	public Document build() {
		return documentImpl;
	}
}
