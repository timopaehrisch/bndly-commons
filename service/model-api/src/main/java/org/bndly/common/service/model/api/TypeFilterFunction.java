package org.bndly.common.service.model.api;

/*-
 * #%L
 * Service Model API
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

public class TypeFilterFunction<OBJECT> implements FilterFunction<OBJECT> {

    private final Class<? extends OBJECT> type;

    public TypeFilterFunction(Class<? extends OBJECT> type) {
        this.type = type;
    }
    
    @Override
    public boolean applies(OBJECT o) {
        return type.isInstance(o);
    }
    
}
