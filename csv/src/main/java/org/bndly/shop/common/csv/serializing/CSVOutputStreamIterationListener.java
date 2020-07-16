package org.bndly.shop.common.csv.serializing;

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
import org.bndly.shop.common.csv.CSVException;
import org.bndly.shop.common.csv.model.Document;
import org.bndly.shop.common.csv.model.Row;
import org.bndly.shop.common.csv.model.Value;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class CSVOutputStreamIterationListener implements CSVDocumentIterationListener {

	protected final CSVWriter writer;

	private boolean firstRow;

	public CSVOutputStreamIterationListener(OutputStream outputStream, String encoding) throws UnsupportedEncodingException {
		this(outputStream, encoding, CSVConfig.DEFAULT);
	}

	public CSVOutputStreamIterationListener(OutputStream outputStream, String encoding, CSVConfig config) throws UnsupportedEncodingException {
		this(new OutputStreamWriter(outputStream, encoding), config);
	}
	
	public CSVOutputStreamIterationListener(Writer writer, CSVConfig config) {
		this.writer = new CSVWriter(writer, config);
	}

	@Override
	public void beforeDocument(Document document) throws CSVException {
		// no-op
		firstRow = true;
	}

	@Override
	public void beforeRow(Row row) throws CSVException {
		if (firstRow) {
			firstRow = false;
		} else {
			try {
				writer.row();
			} catch (IOException ex) {
				throw new CSVException("could not create new row", ex);
			}
		}
	}

	@Override
	public void onColumn(Value value) throws CSVException {
		try {
			if (value.requiresQuotes()) {
				writer.valueEscaped(value.getRaw());
			} else {
				writer.valuePlain(value.getRaw());
			}
		} catch (IOException e) {
			throw new CSVException("could not write value", e);
		}
	}

	@Override
	public void afterRow(Row row) throws CSVException {
	}

	@Override
	public void afterDocument(Document document) throws CSVException {
	}

}
