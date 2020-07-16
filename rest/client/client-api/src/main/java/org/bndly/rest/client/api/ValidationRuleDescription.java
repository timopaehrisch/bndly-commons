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

public class ValidationRuleDescription {

    private String ruleName;
    private String affectedPropertyName;
    private Object originalRule;

    public String getRuleName() {
	return ruleName;
    }

    public void setRuleName(String ruleName) {
	this.ruleName = ruleName;
    }

    public String getAffectedPropertyName() {
	return affectedPropertyName;
    }

    public void setAffectedPropertyName(String affectedPropertyName) {
	this.affectedPropertyName = affectedPropertyName;
    }

    public Object getOriginalRule() {
	return originalRule;
    }

    public void setOriginalRule(Object originalRule) {
	this.originalRule = originalRule;
    }

    @Override
    public String toString() {
	return "ValidationRuleDescription{" + "ruleName=" + ruleName + ", affectedPropertyName=" + affectedPropertyName + '}';
    }
}
