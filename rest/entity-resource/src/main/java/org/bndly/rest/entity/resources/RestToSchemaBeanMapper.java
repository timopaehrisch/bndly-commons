package org.bndly.rest.entity.resources;

/*-
 * #%L
 * REST Entity Resource
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

import org.bndly.common.mapper.MappingContext;
import org.bndly.common.mapper.MappingState;
import org.bndly.common.mapper.TypeSpecificMapper;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.SchemaUtil;
import org.bndly.schema.model.Type;
import java.util.List;

public class RestToSchemaBeanMapper implements TypeSpecificMapper {
    private final Class<?> restBeanType;
    private final Class<?> referenceRestBeanType;
    private final Class<?> schemaBeanType;
    private final Type type;
    private final List<Attribute> attributes;

    public RestToSchemaBeanMapper(Class<?> restBeanType, Class<?> referenceRestBeanType, Class<?> schemaBeanType, Type type) {
        this.restBeanType = restBeanType;
        this.referenceRestBeanType = referenceRestBeanType;
        this.schemaBeanType = schemaBeanType;
        this.type = type;
        this.attributes = SchemaUtil.collectAttributes(type);
    }
    
    @Override
    public Class<?> getSupportedInput() {
        return referenceRestBeanType;
    }

    @Override
    public Class<?> getSupportedOutput() {
        return schemaBeanType;
    }

    @Override
    public void map(Object source, Object target, MappingContext context, MappingState state) {
        for (Attribute attribute : attributes) {
            
        }
    }
}
