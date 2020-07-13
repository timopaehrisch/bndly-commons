package org.bndly.rest.api;

/*-
 * #%L
 * REST API
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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.CharBuffer;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class XWWWFormEncodedDataParser {
	public static int DEFAULT_NAME_BUFFER_SIZE = 128;
	public static int DEFAULT_VALUE_BUFFER_SIZE = 1024;
	
	public static enum IterationResult {
		CONTINUE,
		TERMINATE
	}
	
	public static interface Listener {
		IterationResult onVariable(String variable);
		IterationResult onVariableValue(String value);
		void onEnd();
	}

	private final PathCoder pathCoder;

	public XWWWFormEncodedDataParser(PathCoder pathCoder) {
		if (pathCoder == null) {
			throw new IllegalArgumentException("pathCoder is not allowed to be null");
		}
		this.pathCoder = pathCoder;
	}
	
	public void parse(String inputString, Listener listener) throws IOException {
		parse(new StringReader(inputString), listener);
	}
	
	public void parse(Reader reader, Listener listener) throws IOException {
		parseInternal(reader, listener);
	}
	
	private void parseInternal(Reader reader, final Listener originalListener) throws IOException {
		Listener listener = new Listener() {
			
			@Override
			public IterationResult onVariable(String variable) {
				String decoded = pathCoder.decodeString(variable);
				return originalListener.onVariable(decoded);
			}

			@Override
			public IterationResult onVariableValue(String value) {
				String decoded = pathCoder.decodeString(value);
				return originalListener.onVariableValue(decoded);
			}

			@Override
			public void onEnd() {
				originalListener.onEnd();
			}
		};
		boolean parsingVariable = true;
		boolean parsingValue = false;
		IterationResult result = IterationResult.CONTINUE;
		int i;
		CharBuffer nameBuffer = CharBuffer.allocate(DEFAULT_NAME_BUFFER_SIZE);
		StringBuffer nameOverFlowBuffer = null;
		CharBuffer valueBuffer = CharBuffer.allocate(DEFAULT_VALUE_BUFFER_SIZE);
		StringBuffer valueOverFlowBuffer = null;
		while ((i = reader.read()) > -1) {
			char c = (char) i;
			if (parsingVariable) {
				if (c == '=') {
					parsingVariable = false;
					parsingValue = true;
					valueOverFlowBuffer = null;
					valueBuffer.clear();
					if (nameOverFlowBuffer != null) {
						result = listener.onVariable(nameOverFlowBuffer.toString());
						nameOverFlowBuffer = null;
					} else {
						nameBuffer.flip();
						result = listener.onVariable(new String(nameBuffer.array(), nameBuffer.position(), nameBuffer.limit()));
					}
					nameBuffer.clear();
				} else if (c == '&') {
					parsingVariable = true;
					parsingValue = false;
					valueOverFlowBuffer = null;
					valueBuffer.clear();
					if (nameOverFlowBuffer != null) {
						result = listener.onVariable(nameOverFlowBuffer.toString());
						nameOverFlowBuffer = null;
					} else {
						nameBuffer.flip();
						result = listener.onVariable(new String(nameBuffer.array(), nameBuffer.position(), nameBuffer.limit()));
					}
					nameBuffer.clear();
				} else {
					parsingVariable = true;
					parsingValue = false;
					if (nameBuffer.remaining() == 0) {
						if (nameOverFlowBuffer == null) {
							nameOverFlowBuffer = new StringBuffer();
							nameBuffer.position(0);
							nameOverFlowBuffer.append(nameBuffer);
						}
						nameOverFlowBuffer.append(c);
					} else {
						nameBuffer.put(c);
					}
				}
			} else if (parsingValue) {
				if (c == '&') {
					parsingVariable = true;
					parsingValue = false;
					nameOverFlowBuffer = null;
					nameBuffer.clear();
					if (valueOverFlowBuffer != null) {
						result = listener.onVariableValue(valueOverFlowBuffer.toString());
						valueOverFlowBuffer = null;
					} else {
						valueBuffer.flip();
						result = listener.onVariableValue(new String(valueBuffer.array(), valueBuffer.position(), valueBuffer.limit()));
					}
					valueBuffer.clear();
				} else {
					parsingVariable = false;
					parsingValue = true;
					if (valueBuffer.remaining() == 0) {
						if (valueOverFlowBuffer == null) {
							valueOverFlowBuffer = new StringBuffer();
							valueBuffer.position(0);
							valueOverFlowBuffer.append(valueBuffer);
						}
						valueOverFlowBuffer.append(c);
					} else {
						valueBuffer.put(c);
					}
				}
			} else {
				throw new IOException("failed to parse input");
			}
			if (result == IterationResult.TERMINATE) {
				break;
			}
		}
		if (result != IterationResult.TERMINATE) {
			if (nameOverFlowBuffer != null) {
				listener.onVariable(nameOverFlowBuffer.toString());
			} else if (nameBuffer.position() > 0) {
				nameBuffer.flip();
				listener.onVariable(new String(nameBuffer.array(), nameBuffer.position(), nameBuffer.limit()));
			}
			if (valueOverFlowBuffer != null) {
				listener.onVariableValue(valueOverFlowBuffer.toString());
			} else if (valueBuffer.position() > 0) {
				valueBuffer.flip();
				listener.onVariableValue(new String(valueBuffer.array(), valueBuffer.position(), valueBuffer.limit()));
			}
		}
		listener.onEnd();
	}
}
