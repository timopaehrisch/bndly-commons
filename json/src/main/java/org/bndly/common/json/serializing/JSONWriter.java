package org.bndly.common.json.serializing;

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

import org.bndly.common.lang.StringUtil;
import java.io.IOException;
import java.io.Writer;
import java.math.BigDecimal;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class JSONWriter implements AutoCloseable {

	private final Writer writer;
	private char[] charbuffer = new char[4];

	public JSONWriter(Writer writer) {
		this.writer = writer;
	}

	public JSONWriter writeNull() throws IOException {
		writer.write("null");
		return this;
	}

	public JSONWriter writeObjectStart() throws IOException {
		writer.write("{");
		return this;
	}

	public JSONWriter writeObjectEnd() throws IOException {
		writer.write("}");
		return this;
	}

	public JSONWriter writeArrayStart() throws IOException {
		writer.write("[");
		return this;
	}

	public JSONWriter writeArrayEnd() throws IOException {
		writer.write("]");
		return this;
	}
	
	public JSONWriter writeTrue() throws IOException {
		writer.write("true");
		return this;
	}
	public JSONWriter writeFalse() throws IOException {
		writer.write("false");
		return this;
	}
	
	public JSONWriter writeBoolean(boolean b) throws IOException {
		return b ? writeTrue() : writeFalse();
	}
	
	public JSONWriter writeComma() throws IOException {
		writer.write(",");
		return this;
	}
	
	public JSONWriter writeColon() throws IOException {
		writer.write(":");
		return this;
	}
	
	public JSONWriter writeDecimal(BigDecimal bigDecimal) throws IOException {
		writer.write(bigDecimal.stripTrailingZeros().toPlainString());
		return this;
	}
	
	public JSONWriter writeLong(long longValue) throws IOException {
		writer.write(Long.toString(longValue));
		return this;
	}
	
	public JSONWriter writeDouble(double doubleValue) throws IOException {
		return writeDecimal(new BigDecimal(doubleValue));
	}
	
	public JSONWriter writeString(String string) throws IOException {
		writer.append('"');
		for (int codePoint : StringUtil.codePoints(string)) {
			if ('\n' == codePoint) {
				writer.append("\\n");
			} else if ('\b' == codePoint) {
				writer.append("\\b");
			} else if ('\t' == codePoint) {
				writer.append("\\t");
			} else if ('\r' == codePoint) {
				writer.append("\\r");
			} else if ('\f' == codePoint) {
				writer.append("\\f");
			} else if ('"' == codePoint) {
				writer.append("\\\"");
			} else if ('\\' == codePoint) {
				writer.append("\\\\");
			} else {
				if (Character.isBmpCodePoint(codePoint)) {
					// we only require a single char
					writer.append((char) codePoint);
				} else {
					int r = Character.toChars(codePoint, charbuffer, 0);
					if (r < 0 || r > 2) {
						throw new IOException("could not convert codepoint to chars");
					}
					for (int i = 0; i < r; i++) {
						writer.append("\\u");
						int charInt = charbuffer[i];
						StringUtil.appendIntAsHex(charInt, writer, false);
					}
				}
			}
		}
		writer.append('"');
		return this;
	}

	public void flush() throws IOException {
		writer.flush();
	}

	@Override
	public void close() throws IOException {
		writer.close();
	}

}
