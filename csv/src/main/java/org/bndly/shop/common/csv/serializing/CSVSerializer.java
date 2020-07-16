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

public class CSVSerializer {

	public void serialize(Document document, OutputStream os, String encoding) throws IOException, CSVException {
		serialize(document, os, encoding, CSVConfig.DEFAULT);
	}

	public void serialize(Document document, OutputStream os, String encoding, CSVConfig config) throws IOException, CSVException {
		CSVOutputStreamIterationListener listener = new CSVOutputStreamIterationListener(os, encoding, config) {
			@Override
			public void afterDocument(Document document) throws CSVException {
				try {
					writer.flush();
				} catch (IOException ex) {
					throw new CSVException("could not flush csv writer", ex);
				}
			}
			
		};
		listener.beforeDocument(document);
		for (Row row : document.getRows()) {
			listener.beforeRow(row);
			for (Value value : row.getValues()) {
				listener.onColumn(value);
			}
			listener.afterRow(row);
		}
		listener.afterDocument(document);
	}

//    private void serializeDocument(DocumentImpl document, OutputStream os, String encoding, CSVConfig config) throws IOException {
//        List<RowImpl> rows = document.getRows();
//        if(rows != null) {
//            boolean isFirst = true;
//            for (RowImpl row : rows) {
//                if(isFirst) {
//                    isFirst = false;
//                } else {
//                    os.write(config.getNewLine().getBytes(encoding));
//                }
//                serializeRow(row, os, encoding, config);
//            }
//        }
//    }
//
//    private void serializeRow(RowImpl row, OutputStream os, String encoding, CSVConfig config) throws IOException {
//        List<ValueImpl> values = row.getValues();
//        if(values != null) {
//            String separator = null;
//            for (ValueImpl value : values) {
//                if(separator != null) {
//                    os.write(separator.getBytes(encoding));
//                }
//                serializeValue(value, os, encoding, config);
//                separator = config.getSeparator();
//            }
//        }
//    }
//
//    private void serializeValue(ValueImpl value, OutputStream os, String encoding, CSVConfig config) throws IOException {
//        String raw = value.getRaw();
//        if(raw != null) {
//            boolean requiresQuote = false;
//            boolean containsQuote = false;
//            for (int i = 0; i < raw.length(); i++) {
//                char c = raw.charAt(i);
//                if(c == ',' || c == '\n' || c == '\r') {
//                    requiresQuote = true;
//                    break;
//                } else if(c == '\"') {
//                    containsQuote = true;
//                }
//            }
//            
//            if(containsQuote) {
//                raw = raw.replaceAll(config.getQuote(), config.getQuote()+config.getQuote());
//            }
//            
//            if(requiresQuote) {
//                os.write(config.getQuote().getBytes(encoding));
//            }
//            os.write(raw.getBytes(encoding));
//            if(requiresQuote) {
//                os.write(config.getQuote().getBytes(encoding));
//            }
//        }
//    }
}
