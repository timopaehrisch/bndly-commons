package org.bndly.rest.descriptor.util;

/*-
 * #%L
 * REST API Descriptor
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

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public final class TypeUtil {

    private TypeUtil() {
    }
    
    public static boolean isCollection(Class<?> type) {
        return Collection.class.isAssignableFrom(type);
    }
    
    public static boolean isSimpleType(Class<?> type) {
        return isNumericType(type) || isStringType(type) || isDateType(type) || isBooleanType(type);
    }
    
    public static boolean isBooleanType(Class<?> type) {
        return Boolean.class.equals(type) || boolean.class.equals(type);
    }
    
    public static boolean isNumericType(Class<?> type) {
        Set<Class<?>> s = new HashSet();
        s.add(byte.class);
        s.add(Byte.class);
        s.add(short.class);
        s.add(Short.class);
        s.add(int.class);
        s.add(Integer.class);
        s.add(long.class);
        s.add(Long.class);
        s.add(float.class);
        s.add(Float.class);
        s.add(double.class);
        s.add(Double.class);
        s.add(BigDecimal.class);
        return s.contains(type);
    }
    
    public static boolean isStringType(Class<?> type) {
        return String.class.equals(type);
    }
    
    public static boolean isDateType(Class<?> type) {
        return Date.class.isAssignableFrom(type);
    }
    
}
