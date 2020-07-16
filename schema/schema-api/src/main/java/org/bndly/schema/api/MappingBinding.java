package org.bndly.schema.api;

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

import org.bndly.schema.model.Attribute;
import org.bndly.schema.model.NamedAttributeHolder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MappingBinding {

    private final String tableAlias;
    private final NamedAttributeHolder holder;
    private final AliasBinding primaryKeyAlias;
    private List<AliasBinding> aliases;
    private Map<String, List<MappingBinding>> subBindingsByAttributeName;

    public MappingBinding(NamedAttributeHolder holder, AliasBinding primaryKeyAlias, String tableAlias) {
        this.holder = holder;
        this.primaryKeyAlias = primaryKeyAlias;
        this.tableAlias = tableAlias;
    }

    public String getTableAlias() {
        return tableAlias;
    }

    public AliasBinding getPrimaryKeyAlias() {
        return primaryKeyAlias;
    }

    public NamedAttributeHolder getHolder() {
        return holder;
    }

    public List<AliasBinding> getAliases() {
        return aliases;
    }

    public Map<String, List<MappingBinding>> getSubBindings() {
        return subBindingsByAttributeName;
    }

    public void addAlias(AliasBinding a) {
        if (aliases == null) {
            aliases = new ArrayList<>();
        }
        aliases.add(a);
    }

    public void addSubBinding(MappingBinding a, Attribute att) {
        if (subBindingsByAttributeName == null) {
            subBindingsByAttributeName = new HashMap<>();
        }
        List<MappingBinding> sb = subBindingsByAttributeName.get(att.getName());
        if (sb == null) {
            sb = new ArrayList<>();
            subBindingsByAttributeName.put(att.getName(), sb);
        }
        sb.add(a);
    }
}
