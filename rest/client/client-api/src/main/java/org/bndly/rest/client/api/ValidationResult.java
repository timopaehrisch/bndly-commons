package org.bndly.rest.client.api;

/*-
 * #%L
 * REST Client API
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

import java.util.ArrayList;
import java.util.List;

public class ValidationResult {
	private Object validatedBean;
	private List<ValidationRuleDescription> succeededRules;
	private List<ValidationRuleDescription> failedRules;
	
	public Object getValidatedBean() {
		return validatedBean;
	}

	public void setValidatedBean(Object validatedBean) {
		this.validatedBean = validatedBean;
	}
	
	public void addSucceededRule(ValidationRuleDescription rule) {
		if (succeededRules == null) {
			succeededRules = new ArrayList<>();
		}
		succeededRules.add(rule);
	}

	public void addFailedRule(ValidationRuleDescription rule) {
		if (failedRules == null) {
			failedRules = new ArrayList<>();
		}
		failedRules.add(rule);
	}
	
	/**
	 * returns a defensive list of succeeded rules.
	 * @return
	 */
	public List<ValidationRuleDescription> getSucceededRules() {
		if (succeededRules != null) {
			return new ArrayList<>(succeededRules);
		}
		return succeededRules;
	}

	/**
	 * returns a defensive list of failed rules.
	 * @return
	 */
	public List<ValidationRuleDescription> getFailedRules() {
		if (failedRules != null) {
			return new ArrayList<>(failedRules);
		}
		return failedRules;
	}
	
	/**
	 * checks if rules have failed
	 * @return true if no failed rules are known
	 */
	public boolean isValid() {
		return failedRules == null || failedRules.isEmpty(); 
	}

	public boolean didFail() {
		List<ValidationRuleDescription> f = getFailedRules();
		return f != null && f.size() > 0;
	}

	public boolean didSucceed() {
		return !didFail();
	}
}
