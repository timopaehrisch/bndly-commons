package org.bndly.schema.impl.vendor.postgres;

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

import org.bndly.schema.api.exception.ConstraintViolationException;
import org.bndly.schema.api.exception.IntegrityException;
import org.bndly.schema.api.exception.SchemaException;
import org.bndly.schema.api.services.Deployer;
import org.bndly.schema.api.services.Engine;
import org.bndly.schema.impl.DefaultErrorCodeMapper;
import org.bndly.schema.model.UniqueConstraint;
import java.sql.SQLException;
import java.util.List;

/**
 * For a list of error codes and sql states: http://www.postgresql.org/docs/9.4/static/errcodes-appendix.html
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class PostgresErrorCodeMapper extends DefaultErrorCodeMapper {

	@Override
	protected SchemaException map(String message, SQLException exception, int errorCode, Engine engine) {
		if (exception != null) {
			String sqlState = exception.getSQLState();
			if (sqlState != null) {
				String msg = message;
				Class<? extends SchemaException> exceptionClass = SchemaException.class;
				// Class 23 — Integrity Constraint Violation
				if (sqlState.startsWith("23")) {
					exceptionClass = IntegrityException.class;
					if ("23000".equals(sqlState)) {
						msg = "integrity_constraint_violation";
					} else if ("23001".equals(sqlState)) {
						msg = "restrict_violation";
					} else if ("23502".equals(sqlState)) {
						msg = "not_null_violation";
					} else if ("23503".equals(sqlState)) {
						msg = "foreign_key_violation";
					} else if ("23505".equals(sqlState)) {
						msg = "unique_violation";
						exceptionClass = ConstraintViolationException.class;
						String exceptionMessage = exception.getMessage();
						if (engine != null) {
							Deployer deployer = engine.getDeployer();
							List<String> constraintNames = deployer.getUniqueConstraintNames();
							UniqueConstraint uq = null;
							for (String constraintName : constraintNames) {
								if (exceptionMessage.contains("\"" + constraintName + "\"")) {
									uq = deployer.getUniqueConstraintByName(constraintName);
									break;
								}
							}
							if (uq != null) {
								return new ConstraintViolationException(uq, msg, exception);
							}
						}
					} else if ("23514".equals(sqlState)) {
						msg = "check_violation";
					} else if ("23P01".equals(sqlState)) {
						msg = "exclusion_violation";
					}
				}
				return createSchemaException(exceptionClass, exception, message, msg);
			}
		}
		return super.map(message, exception, errorCode, engine);
	}
	
}
