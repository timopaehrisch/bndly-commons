package org.bndly.schema.impl.nquery.states;

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

import org.bndly.schema.api.nquery.Parser;
import org.bndly.schema.api.nquery.QueryParsingException;
import org.bndly.schema.api.nquery.ReservedKeywords;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public abstract class ExpressionState extends AbstractParsingState {

	private boolean operatorHasBeenRead = false;
	private boolean negate = false;
	private String leftOfOperator;
	private String operator;
	private String rightOfOperator;
	private StringBuffer buf;

	@Override
	public void handleChar(char character, Parser parser) throws QueryParsingException {
		String cs = Character.toString(character);
		String buffered = getBuffered() + cs;
		if (ReservedKeywords.NEGATION.equals(cs)) {
			if (operatorHasBeenRead) {
				throw new QueryParsingException("negation after operator is not allowed");
			}
			negate = true;
		} else if (buffered.endsWith(ReservedKeywords.EQUAL)) {
			onOperatorRead(ReservedKeywords.EQUAL, buffered);
		} else if (buffered.endsWith(ReservedKeywords.INRANGE)) {
			onOperatorRead(ReservedKeywords.INRANGE, buffered);
		} else if (ReservedKeywords.PARAM_WILDCARD.equals(cs)) {
			if (!operatorHasBeenRead) {
				throw new QueryParsingException("parameter wild cards are only expected right to the operator");
			}
			appendToBuffer(character);
		} else {
			appendToBuffer(character);
		}
	}
	
	private void onOperatorRead(String op, String buffered) throws QueryParsingException {
		if (operatorHasBeenRead) {
			throw new QueryParsingException("operator is already parsed");
		}
		operatorHasBeenRead = true;
		if (buf == null || buf.toString().equals(ReservedKeywords.PARAM_WILDCARD)) {
			throw new QueryParsingException("equal operator requires an attribute reference left to the equal.");
		}
		operator = op;
		leftOfOperator = buffered.substring(0, buffered.length() - op.length());
		buf = null;
	}
	
	private String getBuffered() {
		return buf == null ? "" : buf.toString();
	}
	
	private void appendToBuffer(char character) {
		if (buf == null) {
			buf = new StringBuffer();
		}
		buf.append(character);
	}

	@Override
	public void onEnd(Parser parser) throws QueryParsingException {
		if (operatorHasBeenRead && buf != null) {
			rightOfOperator = buf.toString();
		}
		parser.pop();
	}

	public boolean isOperatorRead() {
		return operatorHasBeenRead;
	}

	public boolean isNegated() {
		return negate;
	}

	public String getLeftOfOperator() {
		return leftOfOperator;
	}

	public String getOperator() {
		return operator;
	}

	public String getRightOfOperator() {
		return rightOfOperator;
	}

}
