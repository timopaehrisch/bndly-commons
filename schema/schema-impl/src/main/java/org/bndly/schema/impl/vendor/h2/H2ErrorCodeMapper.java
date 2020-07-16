package org.bndly.schema.impl.vendor.h2;

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
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class H2ErrorCodeMapper extends DefaultErrorCodeMapper {

	@Override
	protected SchemaException map(String defaultMessage, SQLException ex, int errorCode, Engine engine) {
		if (defaultMessage == null) {
			defaultMessage = "";
		}
		Class<? extends SchemaException> cls = SchemaException.class;
		String message;
		switch (errorCode) {
			case 90134:
				message = "Acces denied to class";
				break;
			case 90040:
				message = "Admin rights required";
				break;
			case 90132:
				message = "Aggregate not found";
				break;
			case 90059:
				message = "Ambiguous column name";
				break;
			case 90133:
				message = "Can not change setting when open";
				break;
			case 90107:
				message = "Can not drop";
				break;
			case 90019:
				message = "Can not drop current user";
				break;
			case 90084:
				message = "Can not drop last column";
				break;
			case 90118:
				message = "Can not drop table";
				break;
			case 90123:
				message = "Can not mix indexed and not indexed parameters";
				break;
			case 90106:
				message = "Can not truncate";
				break;
			case 90137:
				message = "Can only assign to variable";
				break;
			case 23514:
				message = "Check constraint invalid";
				break;
			case 23513:
				message = "Check constraint violated";
				break;
			case 90086:
				message = "Class not found";
				break;
			case 90093:
				message = "Cluster error: database runs alone";
				break;
			case 90094:
				message = "Cluster error: database runs clustered";
				break;
			case 90089:
				message = "Collation change with data table";
				break;
			case 90081:
				message = "Column contains null values";
				break;
			case 21002:
				message = "Column count does not match";
				break;
			case 90075:
				message = "Column is part of index";
				break;
			case 90083:
				message = "Column is referenced";
				break;
			case 90023:
				message = "Column must not be nullable";
				break;
			case 42122:
				message = "Column not found";
				break;
			case 90058:
				message = "Commit rollback not allowed";
				break;
			case 90104:
				message = "Compression error";
				break;
			case 90131:
				message = "Concurrent update";
				break;
			case 90067:
				message = "Connection broken";
				break;
			case 90114:
				message = "Constant already exists";
				break;
			case 90115:
				message = "Constant not found";
				break;
			case 90045:
				message = "Constraint already exists";
				break;
			case 90057:
				message = "Constraint not found";
				break;
			case 90020:
				message = "Database already open";
				break;
			case 90121:
				message = "Database called at shutdown";
				break;
			case 90098:
				message = "Database is closed";
				break;
			case 90135:
				message = "Database is in exclusive mode";
				break;
			case 90126:
				message = "Database is not persistent";
				break;
			case 90097:
				message = "Database is read only";
				break;
			case 90013:
				message = "Database not found";
				break;
			case 22018:
				message = "Data conversion error";
				break;
			case 40001:
				message = "Deadlock";
				break;
			case 90027:
				message = "Deserialization failed";
				break;
			case 22012:
				message = "Division by zero";
				break;
			case 90047:
				message = "Driver version error";
				break;
			case 42121:
				message = "Duplicate column name";
				break;
			case 23505:
				message = "Duplicate key";
				cls = ConstraintViolationException.class;
				if (ex != null) {
					String msg = ex.getMessage();
					if (engine != null) {
						Deployer deployer = engine.getDeployer();
						List<String> constraintNames = deployer.getUniqueConstraintNames();
						UniqueConstraint uq = null;
						for (String constraintName : constraintNames) {
							if (msg.contains(constraintName)) {
								uq = deployer.getUniqueConstraintByName(constraintName);
								break;
							}
						}
						if (uq != null) {
							return new ConstraintViolationException(uq, message, ex);
						}
					}
				}
				break;
			case 90066:
				message = "Duplicate property";
				break;
			case 90111:
				message = "Error accessing linked table";
				break;
			case 90043:
				message = "Error creating trigger object";
				break;
			case 90044:
				message = "Error executing trigger";
				break;
			case 8000:
				message = "Error opening database";
				break;
			case 90099:
				message = "Error setting database event listener";
				break;
			case 90105:
				message = "Exception in function";
				break;
			case 90061:
				message = "Exception opening port";
				break;
			case 50100:
				message = "Feature not supported";
				break;
			case 90030:
				message = "File corrupted";
				break;
			case 90062:
				message = "File creation failed";
				break;
			case 90025:
				message = "File delete failed";
				break;
			case 90049:
				message = "File encryption error";
				break;
			case 90124:
				message = "File not found";
				break;
			case 90024:
				message = "File rename failed";
				break;
			case 90048:
				message = "File version error";
				break;
			case 90076:
				message = "Function alias already exists";
				break;
			case 90077:
				message = "Function alias not found";
				break;
			case 90000:
				message = "Function must return result set";
				break;
			case 90022:
				message = "Function not found";
				break;
			case 50000:
				message = "General error";
				break;
			case 90003:
				message = "Hex string odd";
				break;
			case 90004:
				message = "Hex string wrong";
				break;
			case 42111:
				message = "Index already exists";
				break;
			case 90085:
				message = "Index belongs to constraint";
				break;
			case 42112:
				message = "Index not found";
				break;
			case 90125:
				message = "Invalid class";
				break;
			case 90138:
				message = "Invalid database name";
				break;
			case 22007:
				message = "Invalid datetime constant";
				break;
			case 7001:
				message = "Invalid parameter count";
				break;
			case 90010:
				message = "Invalid to char format";
				break;
			case 90054:
				message = "Invalid use of aggregate function";
				break;
			case 90008:
				message = "Invalid value";
				break;
			case 90028:
				message = "IO Exception";
				break;
			case 90031:
				message = "IO Exception";
				break;
			case 90141:
				message = "Java object serializer change with data table";
				break;
			case 22025:
				message = "Like escape error";
				break;
			case 90116:
				message = "Literals are not allowed";
				break;
			case 50200:
				message = "Lock timeout";
				break;
			case 90073:
				message = "Methods must have different parameter counts";
				break;
			case 90130:
				message = "Method not allowed for prepared statement";
				break;
			case 90001:
				message = "Method not allowed for query";
				break;
			case 90087:
				message = "Method not found";
				break;
			case 90002:
				message = "Method only allowed for query";
				break;
			case 90016:
				message = "Must group by column";
				break;
			case 90096:
				message = "Not enough rights for";
				break;
			case 90029:
				message = "Not on updateable row";
				break;
			case 2000:
				message = "No data available";
				break;
			case 23507:
				message = "No default set";
				break;
			case 90100:
				message = "No disk space available";
				break;
			case 23502:
				message = "Null not allowed";
				cls = ConstraintViolationException.class;
				break;
			case 22003:
				message = "Numeric value out of range";
				break;
			case 90007:
				message = "Object closed";
				break;
			case 90068:
				message = "Order by not in result";
				break;
			case 90108:
				message = "Out of memory";
				break;
			case 90012:
				message = "Parameter not set";
				break;
			case 90014:
				message = "Parse error";
				break;
			case 90139:
				message = "Public static java method not found";
				break;
			case 23503:
				cls = IntegrityException.class;
				message = "Referential integrity violated: child exists";
				break;
			case 23506:
				cls = IntegrityException.class;
				message = "Referential integrity violated: parent missing";
				break;
			case 90117:
				message = "Remote connection not allowed";
				break;
			case 90128:
				message = "Result set not scrollable";
				break;
			case 90127:
				message = "Result set not updateable";
				break;
			case 90140:
				message = "Result set read only";
				break;
			case 90072:
				message = "Roles and right can not be mixed";
				break;
			case 90069:
				message = "Role already exists";
				break;
			case 90074:
				message = "Role already granted";
				break;
			case 90091:
				message = "Role can not be dropped";
				break;
			case 90070:
				message = "Role not found";
				break;
			case 90112:
				message = "Role not found when deleting";
				break;
			case 90063:
				message = "Savepoint is invalid";
				break;
			case 90065:
				message = "Savepoint is named";
				break;
			case 90064:
				message = "Savepoint is unnamed";
				break;
			case 90053:
				message = "Scalar subquery contains more than one row";
				break;
			case 90078:
				message = "Schema already exists";
				break;
			case 90090:
				message = "Schema can not be dropped";
				break;
			case 90080:
				message = "Schema name must match";
				break;
			case 90079:
				message = "Schema not found";
				break;
			case 90017:
				message = "Second primary key";
				break;
			case 90035:
				message = "Sequence already exists";
				break;
			case 90009:
				message = "Sequence attributes invalid";
				break;
			case 90082:
				message = "Sequence belongs to a table";
				break;
			case 90006:
				message = "Sequence exhausted";
				break;
			case 90036:
				message = "Sequence not found";
				break;
			case 90026:
				message = "Serialization failed";
				break;
			case 57014:
				message = "Statement was cancelled";
				break;
			case 90095:
				message = "String format error";
				break;
			case 90052:
				message = "Subquery is not single column";
				break;
			case 90015:
				message = "Sum or avg on wrong datatype";
				break;
			case 42000:
				message = "Syntax error";
				break;
			case 42001:
				message = "Syntax error";
				break;
			case 42101:
				message = "Table or view already exists";
				break;
			case 42102:
				message = "Table or view not found";
				break;
			case 90018:
				message = "Trace connection not closed";
				break;
			case 90034:
				message = "Trace file error";
				break;
			case 90129:
				message = "Transaction not found";
				break;
			case 90041:
				message = "Trigger already exists";
				break;
			case 90042:
				message = "Trigger not found";
				break;
			case 90005:
				message = "Trigger select and row based not supported";
				break;
			case 50004:
				message = "Unknown data type";
				break;
			case 90088:
				message = "Unknown mode";
				break;
			case 90055:
				message = "Unsupported cipher";
				break;
			case 90103:
				message = "Unsupported compression algorithm";
				break;
			case 90102:
				message = "Unsupported compression options";
				break;
			case 90092:
				message = "Unsupported java version";
				break;
			case 90060:
				message = "Unsupported lock method";
				break;
			case 90136:
				message = "Unsupported outer join condition";
				break;
			case 90113:
				message = "Unsupported setting";
				break;
			case 90046:
				message = "URL format error";
				break;
			case 90033:
				message = "User already exists";
				break;
			case 90119:
				message = "User data type already exists";
				break;
			case 90120:
				message = "User data type not found";
				break;
			case 90032:
				message = "User not found";
				break;
			case 90071:
				message = "User or role not found";
				break;
			case 22001:
				message = "Value too long";
				break;
			case 90038:
				message = "View already exists";
				break;
			case 90109:
				message = "View is invalid";
				break;
			case 90037:
				message = "View not found";
				break;
			case 90050:
				message = "Wrong password format";
				break;
			case 28000:
				message = "Wrong user or password";
				break;
			case 90101:
				message = "Wrong xid format";
				break;
			default:
				message = "unknown error code: " + errorCode;

		}

		return createSchemaException(cls, ex, defaultMessage, message);
	}

}
