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

import org.bndly.common.lang.StringUtil;
import org.bndly.shop.common.csv.CSVConfig;
import org.bndly.shop.common.csv.model.ValueImpl;
import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.io.Writer;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class CSVWriter implements Closeable, Flushable {

	private final Writer writer;
	private final CSVConfig config;
	private boolean hasValue;

	public CSVWriter(Writer writer, CSVConfig config) {
		this.writer = writer;
		this.config = config;
	}

	public final CSVWriter row() throws IOException {
		hasValue = false;
		writer.write(config.getNewLine());
		return this;
	}

	/**
	 * Writes the provided value to the CSV.
	 * This method will use {@link #valueEscaped(java.lang.String)}, if the provided value requires escaping.
	 * If no escaping is required, then {@link #valuePlain(java.lang.String)} will be used for writing the value.
	 * @param value the value to write
	 * @return the current CSVWriter instance
	 * @throws IOException 
	 */
	public final CSVWriter value(String value) throws IOException {
		if (ValueImpl.requiresQuotes(config, value)) {
			return valueEscaped(value);
		} else {
			return valuePlain(value);
		}
	}
	
	/**
	 * Writes the value to the CSV without wrapping it in {@link CSVConfig#getQuote()}.
	 * @param value the value to write
	 * @return the current CSVWriter instance
	 * @throws IOException 
	 */
	public final CSVWriter valuePlain(String value) throws IOException {
		return valueInternal(value, false);
	}
	
	/**
	 * Writes the value to the CSV by wrapping it in {@link CSVConfig#getQuote()}.
	 * If {@link CSVConfig#getQuote()} appears within the provided value, this sequence will be escaped by applying the quote itself as the escape character sequence.
	 * @param value the value to write
	 * @return the current CSVWriter instance
	 * @throws IOException 
	 */
	public final CSVWriter valueEscaped(String value) throws IOException {
		return valueInternal(value, true);
	}

	private CSVWriter valueInternal(String value, boolean escape) throws IOException {
		if (!hasValue) {
			hasValue = true;
		} else {
			writer.write(config.getSeparator());
		}
		if (value != null) {
			if (escape) {
				String quote = config.getQuote();
				
				writer.write(config.getQuote());
				StringBuilder buffer = null;
				int q = 0;
				for (Integer c : StringUtil.codePoints(value)) {
					if (c == quote.charAt(q)) {
						q++;
						if (q == quote.length()) {
							// the quote string has to be escaped with a preceding quote string
							writer.write(quote);
							writer.write(quote);

							buffer = null;
							q = 0;
						} else {
							// just buffer the data. the quote is not complete yet
							if (buffer == null) {
								buffer = new StringBuilder();
							}
							buffer.append(c);
						}
					} else {
						if (buffer != null) {
							// flush the buffer
							writer.append(buffer);
							buffer = null;
							q = 0;
						}
						// write the char
						writer.write(c);
					}
				}
				writer.write(config.getQuote());
			} else {
				writer.write(value);
			}
		}
		return this;
	}
	
	@Override
	public void flush() throws IOException {
		writer.flush();
	}

	@Override
	public final void close() throws IOException {
		writer.close();
	}

}
