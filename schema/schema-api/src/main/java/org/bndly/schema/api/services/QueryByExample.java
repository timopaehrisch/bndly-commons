package org.bndly.schema.api.services;

/*-
 * #%L
 * Schema API
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

import org.bndly.schema.api.Pagination;
import org.bndly.schema.api.Record;
import org.bndly.schema.api.query.QueryAttribute;
import java.util.List;

public interface QueryByExample {
    public QueryByExample attribute(String attributeName, Object value);
    public QueryByExample pagination(Pagination p);
    public QueryByExample eager();
    public QueryByExample lazy();
    public QueryByExample orderBy(String attributeName);
    public QueryByExample asc();
    public QueryByExample desc();
    public Record single();
    public List<Record> all();
    public long count();
    public List<QueryAttribute> getQueryAttributes();
}
