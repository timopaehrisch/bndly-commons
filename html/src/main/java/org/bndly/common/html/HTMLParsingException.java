package org.bndly.common.html;

/*-
 * #%L
 * HTML
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

/**
 *
 * A special exception to allow specific catch statements when using the HTML
 * parser.
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class HTMLParsingException extends Exception {

	private final long row;
	private final long column;

	public HTMLParsingException(long row, long column) {
		this.row = row;
		this.column = column;
	}
	
	public HTMLParsingException(long row, long column, String message) {
		super(message);
		this.row = row;
		this.column = column;
	}

	public HTMLParsingException(long row, long column, Throwable cause) {
		super(cause);
		this.row = row;
		this.column = column;
	}

	public HTMLParsingException(long row, long column, String message, Throwable cause) {
		super(message, cause);
		this.row = row;
		this.column = column;
	}

	public long getRow() {
		return row;
	}

	public long getColumn() {
		return column;
	}

}
