package org.bndly.shop.common.csv.marshalling;

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

import org.bndly.shop.common.csv.model.Document;
import org.bndly.shop.common.csv.model.RowImpl;

public class IndexedRow extends RowImpl {
    private IndexedRow parent;
    private boolean mutated;

	public IndexedRow(Document document, long index) {
		super(document, index);
	}
	
	
//    private Long index;
//
//    public Long getIndex() {
//        return index;
//    }
//
//    public void setIndex(Long index) {
//        this.index = index;
//    }

    public boolean isMutated() {
        return mutated;
    }
    
    public IndexedRow createMutation(long index) {
        IndexedRow mutation = new IndexedRow(getDocument(), index);
        mutation.parent = this;
        mutated = true;
        return mutation;
    }

    public IndexedRow getParent() {
        return parent;
    }
    
}
