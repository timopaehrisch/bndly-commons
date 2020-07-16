package org.bndly.rest.search.resources;

/*-
 * #%L
 * REST Search Resource
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

import org.bndly.rest.search.beans.SearchDocument;
import org.bndly.rest.search.beans.SearchKeyValuePair;
import org.bndly.rest.search.beans.SearchKeyValuePairs;
import org.bndly.search.api.DocumentMapper;
import java.util.Collection;

public class DefaultDocumentMapper implements DocumentMapper<SearchDocument> {

	@Override
	public SearchDocument getInstance() {
		return new SearchDocument();
	}

	@Override
	public void setValue(SearchDocument instance, String fieldName, Object value) {
		if (!Collection.class.isInstance(value)) {
			SearchKeyValuePairs kv = instance.getKeyValuePairs();
			if (kv == null) {
				kv = new SearchKeyValuePairs();
				instance.setKeyValuePairs(kv);
			}
			SearchKeyValuePair item = new SearchKeyValuePair();
			item.setKey(fieldName);
			item.setValue(value);
			kv.add(item);
		}

	}

}
