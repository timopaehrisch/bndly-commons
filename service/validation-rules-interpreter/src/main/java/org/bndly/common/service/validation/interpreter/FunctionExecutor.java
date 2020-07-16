package org.bndly.common.service.validation.interpreter;

/*-
 * #%L
 * Validation Rules Interpreter
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

import org.bndly.common.service.validation.ANDFunctionRestBean;
import org.bndly.common.service.validation.BooleanFunctionReferenceRestBean;
import org.bndly.common.service.validation.CharsetFunctionRestBean;
import org.bndly.common.service.validation.EmptyFunctionRestBean;
import org.bndly.common.service.validation.EqualsFunctionRestBean;
import org.bndly.common.service.validation.FunctionReferenceRestBean;
import org.bndly.common.service.validation.FunctionsRestBean;
import org.bndly.common.service.validation.IntervallFunctionRestBean;
import org.bndly.common.service.validation.MaxSizeFunctionRestBean;
import org.bndly.common.service.validation.MultiplyFunctionRestBean;
import org.bndly.common.service.validation.NotFunctionRestBean;
import org.bndly.common.service.validation.NumericFunctionReferenceRestBean;
import org.bndly.common.service.validation.ORFunctionRestBean;
import org.bndly.common.service.validation.PrecisionFunctionRestBean;
import org.bndly.common.service.validation.RuleFunctionRestBean;
import org.bndly.common.service.validation.ValueFunctionRestBean;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

public class FunctionExecutor<B> {

	private final B bean;

	public FunctionExecutor(B bean) {
		this.bean = bean;
	}

	public <F extends FunctionReferenceRestBean, T, C> T evaluate(F function) {
		return evaluate(function, bean);
	}

	@SuppressWarnings("unchecked")
	public <F extends FunctionReferenceRestBean, T, C> T evaluate(F function, C entity) {
		if (entity == null) {
			entity = (C) bean;
		}
		if (function != null) {
			if (ValueFunctionRestBean.class.isAssignableFrom(function.getClass())) {
				return evaluateValueFunction((ValueFunctionRestBean) function, entity);
			} else if (BooleanFunctionReferenceRestBean.class.isAssignableFrom(function.getClass())) {
				return evaluateBooleanFunction((BooleanFunctionReferenceRestBean) function, entity);
			} else if (NumericFunctionReferenceRestBean.class.isAssignableFrom(function.getClass())) {
				return evaluateNumericFunction((NumericFunctionReferenceRestBean) function, entity);
			} else {
				throw new IllegalArgumentException("unsupported function type: " + function.getClass().getSimpleName());
			}
		}

		return null;
	}

	private FunctionReferenceRestBean getFunctionByName(String name, List<FunctionReferenceRestBean> functions) {
		if (name != null && functions != null) {
			for (FunctionReferenceRestBean subFn : functions) {
				if (name.equals(subFn.getName())) {
					return subFn;
				}
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private <T, C> T evaluateValueFunction(ValueFunctionRestBean function, C entity) {
		String fieldName = function.getField();
		BigDecimal numericValue = function.getNumeric();
		String stringValue = function.getString();
		Boolean useDateOfNow = function.getDateOfNow();
		if (fieldName != null) {
			if (entity != null) {
				// the fieldname is an xpath expression. hence the XPathResolver is used to get the fields value
				try {
					XPathResolver resolver = new XPathResolver();
					return resolver.resolve(fieldName, entity, false);
				} catch (IllegalArgumentException e) {
					throw e;
				}
			}
		} else if (stringValue != null) {
			return (T) stringValue;
		} else if (numericValue != null) {
			return (T) numericValue;
		} else if (useDateOfNow != null && useDateOfNow) {
			return (T) new Date();
		}
		throw new IllegalArgumentException("could not evaluate value. fieldName: " + fieldName + " numericValue: " + numericValue + " stringValue: " + stringValue);
	}

	private <T, C> T evaluateBooleanFunction(BooleanFunctionReferenceRestBean fn, C e) {
		if (fn instanceof ANDFunctionRestBean) {
			return evaluateAndFunction((ANDFunctionRestBean) fn, e);
		} else if (fn instanceof RuleFunctionRestBean) {
			return evaluateRuleFunction((RuleFunctionRestBean) fn, e);
		} else if (fn instanceof ORFunctionRestBean) {
			return evaluateOrFunction((ORFunctionRestBean) fn, e);
		} else if (fn instanceof EmptyFunctionRestBean) {
			return evaluateEmptyFunction((EmptyFunctionRestBean) fn, e);
		} else if (fn instanceof IntervallFunctionRestBean) {
			return evaluateIntervallFunction((IntervallFunctionRestBean) fn, e);
		} else if (fn instanceof NotFunctionRestBean) {
			return evaluateNotFunction((NotFunctionRestBean) fn, e);
		} else if (fn instanceof MaxSizeFunctionRestBean) {
			return evaluateMaxSizeFunction((MaxSizeFunctionRestBean) fn, e);
		} else if (fn instanceof CharsetFunctionRestBean) {
			return evaluateCharsetFunction((CharsetFunctionRestBean) fn, e);
		} else if (fn instanceof PrecisionFunctionRestBean) {
			return evaluatePrecisionFunction((PrecisionFunctionRestBean) fn, e);
		} else if (fn instanceof EqualsFunctionRestBean) {
			return evaluateEqualsFunction((EqualsFunctionRestBean) fn, e);
		} else {
			throw new IllegalArgumentException("unsupported function type: " + fn.getClass().getSimpleName());
		}
	}

	private <T, C> T evaluateNumericFunction(NumericFunctionReferenceRestBean fn, C entity) {
		if (fn instanceof MultiplyFunctionRestBean) {
			return evaluateMultiplyFunction((MultiplyFunctionRestBean) fn, entity);
		} else {
			throw new IllegalArgumentException("unsupported function type: " + fn.getClass().getSimpleName());
		}
	}

	@SuppressWarnings("unchecked")
	private <T, C> T evaluateMultiplyFunction(MultiplyFunctionRestBean fn, C entity) {
		BigDecimal returnValue = BigDecimal.ONE;
		List<FunctionReferenceRestBean> parameters = parametersOf(fn);
		if (parameters != null) {
			for (FunctionReferenceRestBean subFn : parameters) {
				Object value = evaluate(subFn, entity);
				Double dv = getDoubleValue(value);
				if (dv != null) {
					returnValue = returnValue.multiply(new BigDecimal(dv));
				}
			}
		}
		return (T) returnValue;
	}

	@SuppressWarnings("unchecked")
	private <T, C> T evaluateAndFunction(ANDFunctionRestBean fn, C e) {
		Boolean returnValue = true;
		List<FunctionReferenceRestBean> parameters = parametersOf(fn);
		if (parameters != null) {
			for (FunctionReferenceRestBean subFn : parameters) {
				Boolean value = evaluate(subFn, e);
				returnValue = returnValue && value;
			}
		}
		return (T) returnValue;
	}

	@SuppressWarnings("unchecked")
	private <T, C> T evaluateRuleFunction(RuleFunctionRestBean fn, C e) {
		Boolean returnValue = true;
		List<FunctionReferenceRestBean> parameters = parametersOf(fn);
		if (parameters != null) {
			for (FunctionReferenceRestBean subFn : parameters) {
				Boolean value = evaluate(subFn, e);
				returnValue = returnValue && value;
			}
		}
		return (T) returnValue;
	}

	@SuppressWarnings("unchecked")
	private <T, C> T evaluateOrFunction(ORFunctionRestBean fn, C e) {
		Boolean returnValue = false;
		List<FunctionReferenceRestBean> parameters = parametersOf(fn);
		if (parameters != null) {
			for (FunctionReferenceRestBean subFn : parameters) {
				Boolean value = evaluate(subFn, e);
				returnValue = returnValue || value;
			}
		}
		return (T) returnValue;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <T, C> T evaluateEmptyFunction(EmptyFunctionRestBean fn, C e) {
		Boolean returnValue = true;
		List<FunctionReferenceRestBean> parameters = parametersOf(fn);
		if (parameters != null) {
			for (FunctionReferenceRestBean subFn : parameters) {
				Object value = evaluate(subFn, e);
				if (value != null) {
					if (List.class.isAssignableFrom(value.getClass())) {
						returnValue = returnValue && ((List) value).size() < 1;
					} else {
						returnValue = returnValue && false;
					}
				} else {
					returnValue = returnValue && true;
				}
			}
		}
		return (T) returnValue;
	}

	@SuppressWarnings("unchecked")
	private <T, C> T evaluateIntervallFunction(IntervallFunctionRestBean fn, C e) {
		Boolean returnValue = true;
		List<FunctionReferenceRestBean> parameters = parametersOf(fn);
		if (parameters != null) {
			FunctionReferenceRestBean valueFunction = getFunctionByName("value", parameters);
			FunctionReferenceRestBean leftFunction = getFunctionByName("left", parameters);
			FunctionReferenceRestBean rightFunction = getFunctionByName("right", parameters);
			if (valueFunction != null) {
				Object value = evaluate(valueFunction, e);
				if (value != null) {
					Object l = null;
					if (leftFunction != null) {
						l = evaluate(leftFunction, e);
					}
					Object r = null;
					if (rightFunction != null) {
						r = evaluate(rightFunction, e);
					}

					boolean result = true;
					Double valued = getDoubleValue(value);
					Date valueDate = Date.class.isAssignableFrom(value.getClass()) ? (Date) value : null;
					if (l != null) {
						if (BigDecimal.class.isAssignableFrom(l.getClass())) {
							double ld = BigDecimal.class.cast(l).doubleValue();
							if (valued != null) {
								result = result && valued >= ld;
							}
						}
						if (Date.class.isAssignableFrom(l.getClass())) {
							long time = Date.class.cast(l).getTime();
							if (valueDate != null) {
								result = result && valueDate.getTime() >= time;
							}
						}
					}
					if (r != null) {
						if (BigDecimal.class.isAssignableFrom(r.getClass())) {
							double rd = BigDecimal.class.cast(r).doubleValue();
							if (valued != null) {
								result = result && valued <= rd;
							}
						}
						if (Date.class.isAssignableFrom(r.getClass())) {
							long time = Date.class.cast(r).getTime();
							if (valueDate != null) {
								result = result && valueDate.getTime() <= time;
							}
						}
					}
					returnValue = result;
				}
			}

		}
		return (T) returnValue;
	}

	private Double getDoubleValue(Object value) {
		if (Double.class.isAssignableFrom(value.getClass())) {
			return (Double) value;
		} else if (Long.class.isAssignableFrom(value.getClass())) {
			return ((Long) value).doubleValue();
		} else if (Integer.class.isAssignableFrom(value.getClass())) {
			return ((Integer) value).doubleValue();
		} else if (Float.class.isAssignableFrom(value.getClass())) {
			return ((Float) value).doubleValue();
		} else if (BigDecimal.class.isAssignableFrom(value.getClass())) {
			return ((BigDecimal) value).doubleValue();
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private <T, C> T evaluateNotFunction(NotFunctionRestBean fn, C e) {
		Boolean returnValue = true;
		List<FunctionReferenceRestBean> parameters = parametersOf(fn);
		if (parameters != null) {
			for (FunctionReferenceRestBean subFn : parameters) {
				Boolean value = evaluate(subFn, e);
				returnValue = returnValue && value;
			}
		}
		if (returnValue) {
			return (T) Boolean.FALSE;
		} else {
			return (T) Boolean.TRUE;
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <T, C> T evaluateMaxSizeFunction(MaxSizeFunctionRestBean fn, C e) {
		Boolean returnValue = true;
		List<FunctionReferenceRestBean> parameters = parametersOf(fn);
		if (parameters != null) {
			FunctionReferenceRestBean valueFunction = getFunctionByName("value", parameters);
			FunctionReferenceRestBean sizeFunction = getFunctionByName("size", parameters);
			if (sizeFunction != null) {
				Object value = evaluate(valueFunction, e);
				if (value != null) {
					BigDecimal size = evaluate(sizeFunction, e);
					if (List.class.isAssignableFrom(value.getClass())) {
						returnValue = ((List) value).size() <= size.intValue();
					} else if (String.class.isAssignableFrom(value.getClass())) {
						returnValue = ((String) value).length() <= size.intValue();
					} else {
						throw new IllegalStateException("unhandled type in maxSizeFunction: " + value.getClass().getSimpleName());
					}
				}
			}

		}
		return (T) returnValue;
	}

	@SuppressWarnings("unchecked")
	private <T, C> T evaluateCharsetFunction(CharsetFunctionRestBean fn, C e) {
		Boolean returnValue = true;
		List<FunctionReferenceRestBean> parameters = parametersOf(fn);
		if (parameters != null) {
			FunctionReferenceRestBean valueFunction = getFunctionByName("value", parameters);
			FunctionReferenceRestBean allowedFunction = getFunctionByName("allowed", parameters);
			String value = evaluate(valueFunction, e);
			String allowed = evaluate(allowedFunction, e);
			char[] allowedChars = new char[allowed.length()];
			for (int i = 0; i < allowed.length(); i++) {
				allowedChars[i] = allowed.charAt(i);
			}
			if (value != null) {
				for (int i = 0; i < value.length(); i++) {
					boolean isAllowed = false;
					for (int j = 0; j < allowedChars.length; j++) {
						isAllowed = isAllowed || allowedChars[j] == value.charAt(i);
					}
					if (!isAllowed) {
						returnValue = false;
						break;
					}
				}
			}
		}
		return (T) returnValue;
	}

	@SuppressWarnings("unchecked")
	private <T, C> T evaluatePrecisionFunction(PrecisionFunctionRestBean fn, C e) {
		Boolean returnValue = true;
		List<FunctionReferenceRestBean> parameters = parametersOf(fn);
		if (parameters != null) {
			FunctionReferenceRestBean valueFunction = getFunctionByName("value", parameters);
			FunctionReferenceRestBean decimalFunction = getFunctionByName("decimal", parameters);
			FunctionReferenceRestBean integerFunction = getFunctionByName("integer", parameters);
			Object value = evaluate(valueFunction, e);
			BigDecimal valueAsBigDecimal = getBigDecimalFromNumericObject(value);
			BigDecimal decimal = evaluate(decimalFunction, e);
			BigDecimal integer = evaluate(integerFunction, e);
			Integer d = null;
			if (decimal != null) {
				d = decimal.intValue();
			}
			Integer i = null;
			if (integer != null) {
				i = integer.intValue();
			}
			if (valueAsBigDecimal != null) {
				valueAsBigDecimal = valueAsBigDecimal.stripTrailingZeros();
				int s = valueAsBigDecimal.scale();
				int p = valueAsBigDecimal.precision() - s;
				if (i != null) {
					returnValue = returnValue && (p <= i);
				}
				if (d != null) {

					returnValue = returnValue && (s <= d);
				}
			}
		}
		return (T) returnValue;
	}

	@SuppressWarnings("unchecked")
	private <T, C> T evaluateEqualsFunction(EqualsFunctionRestBean fn, C e) {
		Boolean returnValue = true;

		Object tmp = null;
		List<FunctionReferenceRestBean> parameters = parametersOf(fn);
		if (parameters != null) {
			for (FunctionReferenceRestBean subFn : parameters) {
				Object value = evaluate(subFn, e);
				if (tmp == null) {
					tmp = value;
				} else {
					returnValue = returnValue && tmp.equals(value);
				}
			}
		}
		return (T) returnValue;
	}

	private List<FunctionReferenceRestBean> parametersOf(FunctionReferenceRestBean fn) {
		if (fn != null) {
			FunctionsRestBean params = fn.getParameters();
			if (params != null) {
				return params.getItems();
			}
		}
		return null;
	}

	private BigDecimal getBigDecimalFromNumericObject(Object value) {
		if (value == null) {
			return null;
		}

		if (Double.class.isAssignableFrom(value.getClass())) {
			double d = (Double) value;
			return new BigDecimal(d);
		} else if (Long.class.isAssignableFrom(value.getClass())) {
			return new BigDecimal((Long) value);
		} else if (Integer.class.isAssignableFrom(value.getClass())) {
			return new BigDecimal((Integer) value);
		} else if (Short.class.isAssignableFrom(value.getClass())) {
			return new BigDecimal((Short) value);
		} else if (Float.class.isAssignableFrom(value.getClass())) {
			return new BigDecimal((Float) value);
		} else if (String.class.isAssignableFrom(value.getClass())) {
			try {
				return new BigDecimal((String) value);
			} catch (Exception e) {
				return null;
			}
		} else if (BigDecimal.class.isAssignableFrom(value.getClass())) {
			return ((BigDecimal) value);
		}
		return null;
	}
}
