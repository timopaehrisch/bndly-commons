package org.bndly.common.service.validation.interpreter;

/*-
 * #%L
 * Validation Rules Interpreter
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

import org.bndly.rest.common.beans.ListRestBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CollectionUtil {

    public static <E extends Collection> E defensiveCopy(E input) {
        if (input == null) {
            return null;
        }
        if (List.class.isInstance(input)) {
            ArrayList<Object> l = new ArrayList<>();
            l.addAll(input);
            return (E) l;
        }
        if (Set.class.isInstance(input)) {
            HashSet s = new HashSet();
            s.addAll(input);
            return (E) s;
        }
        throw new IllegalArgumentException("unsupported type: " + input.getClass());
    }

    public static <E> List<E> getItemsAs(ListRestBean l, Class<E> type) {
        if (l == null) {
            return new ArrayList<>();
        }
        return getItemsAs(l.getItems(), type);

    }

    public static <E> List<E> getItemsAs(Collection c, Class<E> type) {
        List<E> l = new ArrayList<>();
        if (c != null) {
            for (Object object : c) {
                l.add(type.cast(object));
            }
        }
        return l;
    }
}
