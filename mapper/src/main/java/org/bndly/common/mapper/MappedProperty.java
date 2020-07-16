package org.bndly.common.mapper;

/*-
 * #%L
 * Mapper
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

import org.bndly.common.reflection.BeanPropertyAccessor;
import org.bndly.common.reflection.BeanPropertyWriter;

public final class MappedProperty {
    private final String name;
    private final Class<?> type;
    private final BeanPropertyAccessor accessor;
    private final BeanPropertyWriter writer;

    public MappedProperty(String name, Class<?> type, BeanPropertyAccessor accessor, BeanPropertyWriter writer) {
        this.name = name;
        this.type = type;
        this.accessor = accessor;
        this.writer = writer;
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        return type;
    }

    public BeanPropertyAccessor getAccessor() {
        return accessor;
    }

    public BeanPropertyWriter getWriter() {
        return writer;
    }
    
}
