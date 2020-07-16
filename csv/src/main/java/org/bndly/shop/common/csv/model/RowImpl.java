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

public class RowImpl implements Row {
	
	private final Document document;
	private final long index;
    private final List<ValueImpl> values = new ArrayList<>();

	public RowImpl(Document document, long index) {
		this.document = document;
		this.index = index;
	}

    /**
     * returns a defensive copy of the values in this row
     * @return 
     */
	@Override
    public List<Value> getValues() {
        List defensiveCopy = new ArrayList(values);
        return defensiveCopy;
    }

    public void addValue(ValueImpl value) {
        values.add(value);
    }

	@Override
	public Document getDocument() {
		return document;
	}

	@Override
	public long getIndex() {
		return index;
	}


}
