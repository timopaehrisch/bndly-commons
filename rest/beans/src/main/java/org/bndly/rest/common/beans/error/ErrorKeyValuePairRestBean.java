package org.bndly.rest.common.beans.error;

/*-
 * #%L
 * REST Common Beans
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

import org.bndly.rest.atomlink.api.annotation.ErrorKeyValuePair;
import java.math.BigDecimal;
import java.util.Date;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "errorKeyValuePair")
@XmlAccessorType(XmlAccessType.NONE)
public class ErrorKeyValuePairRestBean implements ErrorKeyValuePair {

	@XmlElement
	private Date dateValue;
	@XmlElement
	private BigDecimal decimalValue;
	@XmlElement
	private String stringValue;
	@XmlElement
	private String key;

	@Override
	public String getKey() {
		return key;
	}

	@Override
	public String getStringValue() {
		return stringValue;
	}

	@Override
	public BigDecimal getDecimalValue() {
		return decimalValue;
	}

	@Override
	public Date getDateValue() {
		return dateValue;
	}

	public void setDateValue(Date dateValue) {
		this.dateValue = dateValue;
	}

	public void setDecimalValue(BigDecimal decimalValue) {
		this.decimalValue = decimalValue;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public void setStringValue(String stringValue) {
		this.stringValue = stringValue;
	}

}
