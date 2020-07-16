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

import org.bndly.schema.api.db.Table;
import org.bndly.schema.api.db.JoinTable;
import org.bndly.schema.api.db.TypeTable;
import java.util.List;

public final class TableIterator {

	private TableIterator() {
	}

	public static interface Listener {

		public void join(JoinTable leftTable, TypeTable rightTable, String joinColumnInLeftTable);

		public void nestedJoinPush(JoinTable leftTable, JoinTable rightTable, String joinColumnInLeftTable);

		public void nestedJoinPop(JoinTable leftTable, JoinTable rightTable, String joinColumnInLeftTable);
	}

	public static void iterate(JoinTable table, Listener listener) {
		List<Table> joinedTables = table.getJoinedTables();
		for (Table joinedTable : joinedTables) {
			String localJoinedColumn;
			if (TypeTable.class.isInstance(joinedTable)) {
				TypeTable tt = TypeTable.class.cast(joinedTable);
				localJoinedColumn = tt.getType().getName() + "_id";
				listener.join(table, tt, localJoinedColumn);
			} else if (JoinTable.class.isInstance(joinedTable)) {
				JoinTable jt = JoinTable.class.cast(joinedTable);
				localJoinedColumn = jt.getNamedAttributeHolder().getName() + "_id";
				listener.nestedJoinPush(table, jt, localJoinedColumn);
				iterate(jt, listener);
				listener.nestedJoinPop(table, jt, localJoinedColumn);
			} else {
				// crap
				throw new IllegalStateException("unsupported joined table.");
			}
		}
	}

}
