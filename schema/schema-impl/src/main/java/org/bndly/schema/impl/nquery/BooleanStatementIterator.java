package org.bndly.schema.impl.nquery;

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

import org.bndly.schema.api.nquery.BooleanOperator;
import org.bndly.schema.api.nquery.BooleanStatement;
import org.bndly.schema.api.nquery.WrapperBooleanStatement;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public final class BooleanStatementIterator {

	private BooleanStatementIterator() {
	}
	
	public static interface Callback {

		public void onLastBooleanStatement(BooleanStatement booleanStatement);
		public void onBooleanStatement(BooleanStatement booleanStatement, BooleanOperator operator);
		public void onWrapperOpened(WrapperBooleanStatement wrapper);
		public void onWrapperClosed(WrapperBooleanStatement wrapper);
		
	}
	
	public static class NoOpCallback implements Callback {

		@Override
		public void onLastBooleanStatement(BooleanStatement booleanStatement) {
		}

		@Override
		public void onBooleanStatement(BooleanStatement booleanStatement, BooleanOperator operator) {
		}

		@Override
		public void onWrapperOpened(WrapperBooleanStatement wrapper) {
		}

		@Override
		public void onWrapperClosed(WrapperBooleanStatement wrapper) {
		}
		
	}
	
	public static void iterate(BooleanStatement booleanStatement, Callback callback) {
		BooleanOperator operator;
		while (booleanStatement != null) {
			operator = booleanStatement.getNextOperator();

			if (operator == null) {
				callback.onLastBooleanStatement(booleanStatement);
			} else {
				callback.onBooleanStatement(booleanStatement, operator);
			}

			if (WrapperBooleanStatement.class.isInstance(booleanStatement)) {
				WrapperBooleanStatement wrapper = (WrapperBooleanStatement) booleanStatement;
				callback.onWrapperOpened(wrapper);
				iterate(wrapper.getWrapped(), callback);
				callback.onWrapperClosed(wrapper);
			}
			
			booleanStatement = booleanStatement.getNext();
		}
	}
}
