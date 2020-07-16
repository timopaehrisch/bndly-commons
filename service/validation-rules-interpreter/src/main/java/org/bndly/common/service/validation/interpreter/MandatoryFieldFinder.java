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
import org.bndly.common.service.validation.EmptyFunctionRestBean;
import org.bndly.common.service.validation.FunctionReferenceRestBean;
import org.bndly.common.service.validation.FunctionsRestBean;
import org.bndly.common.service.validation.NotFunctionRestBean;
import org.bndly.common.service.validation.ORFunctionRestBean;
import org.bndly.common.service.validation.RuleFunctionRestBean;
import org.bndly.common.service.validation.ValueFunctionRestBean;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class MandatoryFieldFinder {

	/**
	 * extracts all mandatory fields from a RestBean based on the JAXB-annotated fields and the provided rules 
	 * @param rules rules that will be inspected to find mandatory rules
	 * @param type the inspected type on which the rules might be applied 
	 * @return null if no mandatory fields can be found - otherwise a list of java.lang.reflect.Field
	 */
	public <B> List<Field> findMandatoryFields(List<RuleFunctionRestBean> rules, Class<B> type) {
		if (rules != null) {
			List<String> optionalFields = new ArrayList<>();
			List<String> mandatoryFields = new ArrayList<>();
			for (RuleFunctionRestBean ruleFunctionRestBean : rules) {
				String fieldName = ruleFunctionRestBean.getField();
				boolean isMandatory = isMandatoryrule(ruleFunctionRestBean, optionalFields);
				if (isMandatory && !mandatoryFields.contains(fieldName)) {
					mandatoryFields.add(fieldName);
				}
			}

			for (String fieldName : optionalFields) {
				mandatoryFields.remove(fieldName);
			}

			List<FieldBinding> fieldsFromClass = XPathResolver.getAllFieldsFromClass(type);
			List<Field> result = new ArrayList<>();
			for (String fieldName : mandatoryFields) {
				for (FieldBinding fieldBinding : fieldsFromClass) {
					if (fieldBinding.getField().getName().equals(fieldName)) {
						result.add(fieldBinding.getField());
					}
				}
			}

			if (result.isEmpty()) {
				result = null;
			}
			return result;
		}
		return null;
	}

	private boolean isMandatoryrule(RuleFunctionRestBean ruleFunctionRestBean, List<String> optionalFields) {
		boolean result = handleRule(ruleFunctionRestBean, optionalFields);
		return result;
	}

	private boolean handleRule(RuleFunctionRestBean ruleFunctionRestBean, List<String> optionalFields) {
		return handleAndRule(ruleFunctionRestBean, ruleFunctionRestBean, optionalFields);
	}

	private boolean handleOrRule(FunctionReferenceRestBean functionRestBean, RuleFunctionRestBean ruleFunctionRestBean, List<String> optionalFields) {
		boolean result = false;
		List<FunctionReferenceRestBean> parameters = parametersOf(functionRestBean);
		if (parameters != null) {
			boolean containsEmpty = false;
			for (FunctionReferenceRestBean subFn : parameters) {
				if (EmptyFunctionRestBean.class.isAssignableFrom(subFn.getClass())) {
					containsEmpty = true;
					List<FunctionReferenceRestBean> subParameters = parametersOf(subFn);
					if (subParameters != null) {
						for (FunctionReferenceRestBean subSubFn : subParameters) {
							if (ValueFunctionRestBean.class.isAssignableFrom(subSubFn.getClass())) {
								if (((ValueFunctionRestBean) subSubFn).getField().equals(ruleFunctionRestBean.getField())) {
									containsEmpty = true;
									optionalFields.add(ruleFunctionRestBean.getField());
								}
							}
						}
					}
				}
			}

			if (containsEmpty) {
				for (FunctionReferenceRestBean subFn : parameters) {
					if (ANDFunctionRestBean.class.isAssignableFrom(subFn.getClass())) {
						result = result || handleAndRule(subFn, ruleFunctionRestBean, optionalFields);
					}
				}
			}
		}
		return result;
	}

	private boolean handleAndRule(FunctionReferenceRestBean functionRestBean, RuleFunctionRestBean ruleFunctionRestBean, List<String> optionalFields) {
		boolean result = false;
		List<FunctionReferenceRestBean> parameters = parametersOf(functionRestBean);
		if (parameters != null) {
			for (FunctionReferenceRestBean subFn : parameters) {
				Class<? extends FunctionReferenceRestBean> t = subFn.getClass();
				if (ANDFunctionRestBean.class.isAssignableFrom(t)) {
					result = result || handleAndRule(subFn, ruleFunctionRestBean, optionalFields);
				} else if (ORFunctionRestBean.class.isAssignableFrom(t)) {
					result = result || handleOrRule(subFn, ruleFunctionRestBean, optionalFields);
				} else if (NotFunctionRestBean.class.isAssignableFrom(t)) {
					result = result || handleNotRule(subFn, ruleFunctionRestBean, optionalFields);
				}
			}
		}
		return result;
	}

	private boolean handleNotRule(FunctionReferenceRestBean functionRestBean, RuleFunctionRestBean ruleFunctionRestBean, List<String> optionalFields) {
		boolean result = false;
		List<FunctionReferenceRestBean> parameters = parametersOf(functionRestBean);
		if (parameters != null) {
			for (FunctionReferenceRestBean subFn : parameters) {
				Class<? extends FunctionReferenceRestBean> t = subFn.getClass();
				if (EmptyFunctionRestBean.class.isAssignableFrom(t)) {
					result = true;
				}
			}
		}
		return result;
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
}
