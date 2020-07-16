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

import java.util.ArrayList;
import java.util.List;

public class DocumentImpl implements Document {
    private final List<RowImpl> rows = new ArrayList<>();

	@Override
    public List<Row> getRows() {
		List<Row> copy = new ArrayList<>(rows.size());
		copy.addAll(rows);
        return copy;
    }

    public void addRow(RowImpl row) {
        rows.add(row);
    }

	@Override
	public int getNumberOfColumns() {
		return rows.isEmpty() ? 0 : rows.get(0).getValues().size();
	}
    
}
