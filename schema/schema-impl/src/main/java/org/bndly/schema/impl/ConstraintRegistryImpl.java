package org.bndly.schema.impl;

/*-
 * #%L
 * Schema Impl
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

import org.bndly.schema.api.db.AttributeColumn;
import org.bndly.schema.api.services.ConstraintRegistry;
import org.bndly.schema.model.Type;
import org.bndly.schema.model.UniqueConstraint;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConstraintRegistryImpl implements ConstraintRegistry, Resetable {

	private final Map<String, List<UniqueConstraintOnType>> uniqueConstraintByTypeName = new HashMap<>();
	private final Map<String, List<UniqueConstraintOnType>> uniqueConstraintByTableName = new HashMap<>();

	@Override
	public List<UniqueConstraint> getUniqueConstraintsForType(Type type) {
		List<UniqueConstraintOnType> l = uniqueConstraintByTypeName.get(type.getName());
		if (l == null) {
			return null;
		}
		List<UniqueConstraint> r = new ArrayList<>();
		for (UniqueConstraintOnType uniqueConstraintOnType : l) {
			r.add(uniqueConstraintOnType.getUniqueConstraint());
		}
		return r;
	}

	@Override
	public void addUniqueConstraintsForType(UniqueConstraint uniqueConstraint, Type type, AttributeColumn columnInUniqueConstraintTable) {
		List<UniqueConstraintOnType> l = uniqueConstraintByTypeName.get(type.getName());
		if (l == null) {
			l = new ArrayList<>();
			uniqueConstraintByTypeName.put(type.getName(), l);
		}
		l.add(new UniqueConstraintOnType(uniqueConstraint, type, columnInUniqueConstraintTable));
	}

	@Override
	public AttributeColumn getJoinColumnForTypeInUniqueConstraintTable(UniqueConstraint constraint, Type type) {
		List<UniqueConstraintOnType> l = uniqueConstraintByTypeName.get(type.getName());
		if (l != null) {
			for (UniqueConstraintOnType uniqueConstraint : l) {
				if (uniqueConstraint.getUniqueConstraint() == constraint) {
					return uniqueConstraint.getColumnInUniqueConstraintTable();
				}
			}
		}
		throw new IllegalStateException("could not find join column for unique constraint");
	}

	@Override
	public void reset() {
		uniqueConstraintByTypeName.clear();
		uniqueConstraintByTableName.clear();
	}

}
