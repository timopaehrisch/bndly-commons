package org.bndly.common.json.parsing;

/*-
 * #%L
 * JSON
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

import org.bndly.common.json.model.JSNumber;
import java.math.BigDecimal;

public abstract class NumberParsingState extends ParsingState {
	
	private static final String NUMERIC = "0123456789";

	private final StringBuffer integerPart = new StringBuffer();
	private final StringBuffer floatPart = new StringBuffer();
	private StringBuffer exponentDigits;
	private boolean minusPossible = true;
	private boolean exponentPossible = true;
	private boolean hasExponent = false;
	private boolean hasFloat = false;
	private boolean firstIntegerPartStarted = false;
	private boolean firstIntegerPartComplete = false;
	private boolean readingExponentDigits = false;
	private int sign = 1;
	private int exponentSign = 1;

	@Override
	public void handleChar(char c, Stack<ParsingState> stateStack) {
		if ('-' == c && minusPossible) {
			minusPossible = false;
			if (hasExponent && !readingExponentDigits) {
				readingExponentDigits = true;
				exponentSign = -1;
			} else {
				sign = -1;
			}
		} else if ('.' == c && !firstIntegerPartComplete) {
			if (!hasFloat) {
				firstIntegerPartComplete = true;
				hasFloat = true;
			} else {
				throw new ParsingException("can not start another float");
			}
			minusPossible = false;
			firstIntegerPartComplete = true;
		} else if (('e' == c || 'E' == c) && exponentPossible) {
			firstIntegerPartComplete = true;
			minusPossible = true;
			exponentPossible = false;
			hasExponent = true;
			exponentDigits = new StringBuffer();
		} else if ('+' == c && hasExponent && !readingExponentDigits) {
			readingExponentDigits = true;
			minusPossible = false;
		} else if (NUMERIC.indexOf(c) > -1) {
			minusPossible = false;
			if (!firstIntegerPartComplete) {
				firstIntegerPartStarted = true;
				integerPart.append(c);
			} else {
				if (readingExponentDigits || hasExponent) {
					readingExponentDigits = true;
					exponentDigits.append(c);
				} else if (hasFloat) {
					floatPart.append(c);
				} else {
					throw new ParsingException("could not append digit");
				}
			}
		} else {
			if (!firstIntegerPartStarted) {
				throw new ParsingException("illegal character while parsing number");
			} else {
				// build the number
				try {
					BigDecimal integerPart = new BigDecimal(this.integerPart.toString());
					if (this.hasFloat) {
						BigDecimal floatPart = new BigDecimal("0." + this.floatPart.toString());
						integerPart = integerPart.add(floatPart);
					}
					if (sign < 0) {
						integerPart = integerPart.multiply(new BigDecimal("-1"));
					}
					if (hasExponent) {
						BigDecimal exponent = new BigDecimal(exponentDigits.toString());
						if (exponentSign < 0) {
							exponent = exponent.multiply(new BigDecimal("-1"));
						}
						double result = Math.pow(10, exponent.doubleValue());
						integerPart = new BigDecimal(result).multiply(integerPart);
					}
					stateStack.pop();
					onNumberParsed(new JSNumber(integerPart), c);
				} catch (NumberFormatException e) {
					throw new ParsingException("could not parse numer", e);
				}
			}
		}
	}

	public abstract void onNumberParsed(JSNumber number, Character stopChar);
}
