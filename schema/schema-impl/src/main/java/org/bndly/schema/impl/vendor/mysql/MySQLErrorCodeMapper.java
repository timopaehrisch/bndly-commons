package org.bndly.schema.impl.vendor.mysql;

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
import org.bndly.schema.impl.BadGrammarException;
import org.bndly.schema.impl.DefaultErrorCodeMapper;
import org.bndly.schema.model.UniqueConstraint;
import java.sql.SQLException;
import java.util.List;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class MySQLErrorCodeMapper extends DefaultErrorCodeMapper {

	@Override
	protected SchemaException map(String defaultMessage, SQLException ex, int errorCode, Engine engine) {
		if (defaultMessage == null) {
			defaultMessage = "";
		}
		Class<? extends SchemaException> cls = SchemaException.class;
		String message;
		switch (errorCode) {
			case 1451:
				message = "Foreign key failed during delete or update";
				cls = IntegrityException.class;
				break;
			case 1452:
				message = "Foreign key failed during add or update";
				cls = IntegrityException.class;
				break;
			case 1064:
				message = "Parse Error";
				cls = BadGrammarException.class;
				break;
			case 1062:
				message = "Duplicate entry";
				if (ex != null) {
					String msg = ex.getMessage();
					if (engine != null) {
						Deployer deployer = engine.getDeployer();
						List<String> constraintNames = deployer.getUniqueConstraintNames();
						UniqueConstraint uq = null;
						for (String constraintName : constraintNames) {
							if (msg.contains("'" + constraintName + "'")) {
								uq = deployer.getUniqueConstraintByName(constraintName);
								break;
							}
						}
						if (uq != null) {
							return new ConstraintViolationException(uq, message, ex);
						}
					}
				}
				cls = ConstraintViolationException.class;
				break;
			default:
				message = "unknown error code: " + errorCode;

		}

		return createSchemaException(cls, ex, defaultMessage, message);
	}
	
}
