package org.bndly.common.json.parsing;

/*-
 * #%L
 * JSON
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

import org.bndly.common.json.model.JSValue;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

public class JSONParser {

	private Stack<ParsingState> states;
	private StringBuffer whatHasBeenRead;

	public JSValue parse(InputStream in, String charset) {
		try {
			InputStreamReader reader = new InputStreamReader(in, charset);
			return parse(reader);
		} catch (UnsupportedEncodingException ex) {
			throw new ParsingException("could not parse JSON, because the charset " + charset + " was unknown: " + ex.getMessage(), ex);
		}
	}

	public JSValue parse(Reader reader) {
		final JSValue[] ref = new JSValue[1];
		states = new Stack<>();
		states.add(new EatAllWhiteSpace());
		states.add(new ValueParsingState() {

			@Override
			public void didParseValue(JSValue value, Character stopChar) {
				ref[0] = value;
			}

		});

		final boolean debug = isDebugEnabled();
		if (debug) {
			whatHasBeenRead = new StringBuffer();
		}
		try {
			int charInt = reader.read();
			while (charInt > -1) {
				char character = (char) charInt;
				if (debug) {
					whatHasBeenRead.append(character);
				}
				states.top().handleChar(character, states);
				charInt = reader.read();
			}
		} catch (IOException ex) {
			throw new ParsingException("could not parse JSON, because the input could not be read : " + ex.getMessage(), ex);
		} finally {
			try {
				reader.close();
			} catch (IOException ex) {
				// we can not compensate this error.
			}
		}
		if (states.size() > 1) {
			throw new ParsingException("parsing was incomplete");
		}
		return ref[0];
	}

	private boolean isDebugEnabled() {
		String prop = System.getProperty("org.bndly.common.json.parsing.JSONParser.debug");
		return "true".equals(prop);
	}
}
