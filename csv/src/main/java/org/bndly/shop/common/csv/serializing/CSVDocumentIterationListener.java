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

import org.bndly.shop.common.csv.CSVException;
import org.bndly.shop.common.csv.model.Document;
import org.bndly.shop.common.csv.model.Row;
import org.bndly.shop.common.csv.model.Value;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface CSVDocumentIterationListener {
	void beforeDocument(Document document) throws CSVException;
	void beforeRow(Row row) throws CSVException;
	void onColumn(Value value) throws CSVException;
	void afterRow(Row row) throws CSVException;
	void afterDocument(Document document) throws CSVException;
}
