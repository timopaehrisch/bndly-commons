package org.bndly.common.service.validation;

/*-
 * #%L
 * Validation Rules Beans
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

import java.math.BigDecimal;
import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "value")
@XmlAccessorType(XmlAccessType.NONE)
public class ValueFunctionRestBean extends ValueFunctionReferenceRestBean implements FieldRelated {

	@XmlElement
	private String field;
	@XmlElement
	private String string;
	@XmlElement
	private Date date;
	@XmlElement
	private Boolean dateOfNow;
	@XmlElement
	private BigDecimal numeric;

	public Boolean getDateOfNow() {
		return dateOfNow;
	}

	public void setDateOfNow(Boolean dateOfNow) {
		this.dateOfNow = dateOfNow;
	}

	@Override
	public String getField() {
		return field;
	}

	@Override
	public void setField(String field) {
		this.field = field;
	}

	public BigDecimal getNumeric() {
		return numeric;
	}

	public void setNumeric(BigDecimal numeric) {
		this.numeric = numeric;
	}

	public String getString() {
		return string;
	}

	public void setString(String string) {
		this.string = string;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public void setNumeric(int minValue) {
		setNumeric(new BigDecimal(minValue));
	}
}
