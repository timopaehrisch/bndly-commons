package org.bndly.schema.impl.vendor;

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

import org.bndly.schema.impl.vendor.mysql8.MySQL8IndexExistenceAdapter;
import org.bndly.schema.vendor.def.DefaultAttributeMediatorFactory;
import org.bndly.schema.vendor.def.DefaultQueryRenderingAdapter;
import org.bndly.schema.vendor.def.DefaultTableExistenceAdapter;
import org.bndly.schema.vendor.def.UpperCaseIdentifierAdapter;
import org.bndly.schema.impl.vendor.h2.H2ColumnExistenceAdapter;
import org.bndly.schema.impl.vendor.h2.H2ConstraintExistenceAdapter;
import org.bndly.schema.impl.vendor.h2.H2ErrorCodeMapper;
import org.bndly.schema.impl.vendor.h2.H2IndexExistenceAdapter;
import org.bndly.schema.impl.vendor.h2.H2PrimaryKeyAdapter;
import org.bndly.schema.impl.vendor.mariadb.MariaDBIndexExistenceAdapter;
import org.bndly.schema.impl.vendor.mysql.MySQLAttributeMediatorFactory;
import org.bndly.schema.impl.vendor.mysql.MySQLColumnExistenceAdapter;
import org.bndly.schema.impl.vendor.mysql.MySQLConstraintExistenceAdapter;
import org.bndly.schema.impl.vendor.mysql.MySQLErrorCodeMapper;
import org.bndly.schema.impl.vendor.mysql.MySQLIndexExistenceAdapter;
import org.bndly.schema.impl.vendor.mysql.MySQLPrimaryKeyAdapter;
import org.bndly.schema.impl.vendor.postgres.PostgresAttributeMediatorFactory;
import org.bndly.schema.impl.vendor.postgres.PostgresColumnExistenceAdapter;
import org.bndly.schema.impl.vendor.postgres.PostgresConstraintExistenceAdapter;
import org.bndly.schema.impl.vendor.postgres.PostgresErrorCodeMapper;
import org.bndly.schema.impl.vendor.postgres.PostgresIdentifierAdapter;
import org.bndly.schema.impl.vendor.postgres.PostgresIndexExistenceAdapter;
import org.bndly.schema.impl.vendor.postgres.PostgresPrimaryKeyAdapter;
import org.bndly.schema.impl.vendor.postgres.PostgresQueryRenderingAdapter;
import org.bndly.schema.impl.vendor.postgres.PostgresTableExistenceAdapter;
import org.bndly.schema.vendor.AttributeMediatorFactory;
import org.bndly.schema.vendor.ColumnExistenceAdapter;
import org.bndly.schema.vendor.ConstraintExistenceAdapter;
import org.bndly.schema.vendor.ErrorCodeMapper;
import org.bndly.schema.vendor.IdentifierAdapter;
import org.bndly.schema.vendor.IndexExistenceAdapter;
import org.bndly.schema.vendor.PrimaryKeyAdapter;
import org.bndly.schema.vendor.QueryRenderingAdapter;
import org.bndly.schema.vendor.TableExistenceAdapter;
import org.bndly.schema.vendor.VendorConfiguration;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public interface VendorConfigurations {

	public static final VendorConfiguration MARIADB = new VendorConfiguration() {
		private final IndexExistenceAdapter indexExistenceAdapter = new MariaDBIndexExistenceAdapter();
		@Override
		public ColumnExistenceAdapter getColumnExistenceAdapter() {
			return MYSQL.getColumnExistenceAdapter();
		}

		@Override
		public TableExistenceAdapter getTableExistenceAdapter() {
			return MYSQL.getTableExistenceAdapter();
		}

		@Override
		public ConstraintExistenceAdapter getConstraintExistenceAdapter() {
			return MYSQL.getConstraintExistenceAdapter();
		}

		@Override
		public ErrorCodeMapper getErrorCodeMapper() {
			return MYSQL.getErrorCodeMapper();
		}

		@Override
		public PrimaryKeyAdapter getPrimaryKeyAdapter() {
			return MYSQL.getPrimaryKeyAdapter();
		}

		@Override
		public IdentifierAdapter getIdentifierAdapter() {
			return MYSQL.getIdentifierAdapter();
		}

		@Override
		public AttributeMediatorFactory getAttributeMediatorFactory() {
			return MYSQL.getAttributeMediatorFactory();
		}

		@Override
		public QueryRenderingAdapter getQueryRenderingAdapter() {
			return MYSQL.getQueryRenderingAdapter();
		}

		@Override
		public IndexExistenceAdapter getIndexExistenceAdapter() {
			return indexExistenceAdapter;
		}
		
	};
			
	public static final VendorConfiguration MYSQL = new VendorConfiguration() {
		private final ColumnExistenceAdapter columnExistenceAdapter = new MySQLColumnExistenceAdapter();
		private final TableExistenceAdapter tableExistenceAdapter = new DefaultTableExistenceAdapter();
		private final ConstraintExistenceAdapter constraintExistenceAdapter = new MySQLConstraintExistenceAdapter();
		private final ErrorCodeMapper errorCodeMapper = new MySQLErrorCodeMapper();
		private final PrimaryKeyAdapter primaryKeyAdapter = new MySQLPrimaryKeyAdapter();
		private final IndexExistenceAdapter indexExistenceAdapter = new MySQLIndexExistenceAdapter();
		private final IdentifierAdapter identifierAdapter = new UpperCaseIdentifierAdapter() {

			@Override
			public int getIdentifierMaxLength() {
				return 64;
			}
		};
		private final AttributeMediatorFactory attributeMediatorFactory = new MySQLAttributeMediatorFactory();
		private final QueryRenderingAdapter queryRenderingAdapter = new DefaultQueryRenderingAdapter();

		@Override
		public ColumnExistenceAdapter getColumnExistenceAdapter() {
			return columnExistenceAdapter;
		}

		@Override
		public TableExistenceAdapter getTableExistenceAdapter() {
			return tableExistenceAdapter;
		}

		@Override
		public ConstraintExistenceAdapter getConstraintExistenceAdapter() {
			return constraintExistenceAdapter;
		}

		@Override
		public ErrorCodeMapper getErrorCodeMapper() {
			return errorCodeMapper;
		}

		@Override
		public PrimaryKeyAdapter getPrimaryKeyAdapter() {
			return primaryKeyAdapter;
		}

		@Override
		public IdentifierAdapter getIdentifierAdapter() {
			return identifierAdapter;
		}

		@Override
		public AttributeMediatorFactory getAttributeMediatorFactory() {
			return attributeMediatorFactory;
		}

		@Override
		public QueryRenderingAdapter getQueryRenderingAdapter() {
			return queryRenderingAdapter;
		}

		@Override
		public IndexExistenceAdapter getIndexExistenceAdapter() {
			return indexExistenceAdapter;
		}

	};

	public static final VendorConfiguration MYSQL8 = new VendorConfiguration() {
		private final IndexExistenceAdapter indexExistenceAdapter = new MySQL8IndexExistenceAdapter();

		@Override
		public ColumnExistenceAdapter getColumnExistenceAdapter() {
			return MYSQL.getColumnExistenceAdapter();
		}

		@Override
		public TableExistenceAdapter getTableExistenceAdapter() {
			return MYSQL.getTableExistenceAdapter();
		}

		@Override
		public ConstraintExistenceAdapter getConstraintExistenceAdapter() {
			return MYSQL.getConstraintExistenceAdapter();
		}

		@Override
		public ErrorCodeMapper getErrorCodeMapper() {
			return MYSQL.getErrorCodeMapper();
		}

		@Override
		public PrimaryKeyAdapter getPrimaryKeyAdapter() {
			return MYSQL.getPrimaryKeyAdapter();
		}

		@Override
		public IdentifierAdapter getIdentifierAdapter() {
			return MYSQL.getIdentifierAdapter();
		}

		@Override
		public AttributeMediatorFactory getAttributeMediatorFactory() {
			return MYSQL.getAttributeMediatorFactory();
		}

		@Override
		public QueryRenderingAdapter getQueryRenderingAdapter() {
			return MYSQL.getQueryRenderingAdapter();
		}

		@Override
		public IndexExistenceAdapter getIndexExistenceAdapter() {
			return indexExistenceAdapter;
		}

	};
	
	public static final VendorConfiguration H2 = new VendorConfiguration() {
		private final ColumnExistenceAdapter columnExistenceAdapter = new H2ColumnExistenceAdapter();
		private final TableExistenceAdapter tableExistenceAdapter = new DefaultTableExistenceAdapter();
		private final ConstraintExistenceAdapter constraintExistenceAdapter = new H2ConstraintExistenceAdapter();
		private final ErrorCodeMapper errorCodeMapper = new H2ErrorCodeMapper();
		private final PrimaryKeyAdapter primaryKeyAdapter = new H2PrimaryKeyAdapter();
		private final IndexExistenceAdapter indexExistenceAdapter = new H2IndexExistenceAdapter();
		private final IdentifierAdapter identifierAdapter = new UpperCaseIdentifierAdapter() {

			@Override
			public int getIdentifierMaxLength() {
				return 64;
			}
			
		};
		private final AttributeMediatorFactory attributeMediatorFactory = new DefaultAttributeMediatorFactory();
		private final QueryRenderingAdapter queryRenderingAdapter = new DefaultQueryRenderingAdapter();

		@Override
		public ColumnExistenceAdapter getColumnExistenceAdapter() {
			return columnExistenceAdapter;
		}

		@Override
		public TableExistenceAdapter getTableExistenceAdapter() {
			return tableExistenceAdapter;
		}

		@Override
		public ConstraintExistenceAdapter getConstraintExistenceAdapter() {
			return constraintExistenceAdapter;
		}

		@Override
		public ErrorCodeMapper getErrorCodeMapper() {
			return errorCodeMapper;
		}

		@Override
		public PrimaryKeyAdapter getPrimaryKeyAdapter() {
			return primaryKeyAdapter;
		}

		@Override
		public IdentifierAdapter getIdentifierAdapter() {
			return identifierAdapter;
		}

		@Override
		public AttributeMediatorFactory getAttributeMediatorFactory() {
			return attributeMediatorFactory;
		}

		@Override
		public QueryRenderingAdapter getQueryRenderingAdapter() {
			return queryRenderingAdapter;
		}

		@Override
		public IndexExistenceAdapter getIndexExistenceAdapter() {
			return indexExistenceAdapter;
		}

	};
	
	public static final VendorConfiguration POSTGRES = new VendorConfiguration() {
		private final ColumnExistenceAdapter columnExistenceAdapter = new PostgresColumnExistenceAdapter();
		private final TableExistenceAdapter tableExistenceAdapter = new PostgresTableExistenceAdapter();
		private final ConstraintExistenceAdapter constraintExistenceAdapter = new PostgresConstraintExistenceAdapter();
		private final ErrorCodeMapper errorCodeMapper = new PostgresErrorCodeMapper();
		private final PrimaryKeyAdapter primaryKeyAdapter = new PostgresPrimaryKeyAdapter();
		private final IdentifierAdapter identifierAdapter = new PostgresIdentifierAdapter();
		private final AttributeMediatorFactory attributeMediatorFactory = new PostgresAttributeMediatorFactory();
		private final IndexExistenceAdapter indexExistenceAdapter = new PostgresIndexExistenceAdapter();
		private final QueryRenderingAdapter queryRenderingAdapter = new PostgresQueryRenderingAdapter();

		@Override
		public ColumnExistenceAdapter getColumnExistenceAdapter() {
			return columnExistenceAdapter;
		}

		@Override
		public TableExistenceAdapter getTableExistenceAdapter() {
			return tableExistenceAdapter;
		}

		@Override
		public ConstraintExistenceAdapter getConstraintExistenceAdapter() {
			return constraintExistenceAdapter;
		}

		@Override
		public ErrorCodeMapper getErrorCodeMapper() {
			return errorCodeMapper;
		}

		@Override
		public PrimaryKeyAdapter getPrimaryKeyAdapter() {
			return primaryKeyAdapter;
		}

		@Override
		public IdentifierAdapter getIdentifierAdapter() {
			return identifierAdapter;
		}

		@Override
		public AttributeMediatorFactory getAttributeMediatorFactory() {
			return attributeMediatorFactory;
		}

		@Override
		public QueryRenderingAdapter getQueryRenderingAdapter() {
			return queryRenderingAdapter;
		}

		@Override
		public IndexExistenceAdapter getIndexExistenceAdapter() {
			return indexExistenceAdapter;
		}

	};

}
