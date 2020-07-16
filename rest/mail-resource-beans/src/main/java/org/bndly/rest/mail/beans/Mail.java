package org.bndly.rest.mail.beans;

/*-
 * #%L
 * REST Mail Resource Beans
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

import org.bndly.rest.common.beans.RestBean;
import org.bndly.rest.common.beans.AnyBean;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "mail")
@XmlAccessorType(XmlAccessType.NONE)
public class Mail extends RestBean {

	@XmlElement
	private String senderAddress;
	@XmlElement
	private List<String> bcc;
	@XmlElement
	private List<String> to;
	@XmlElement
	private String subject;
	@XmlElements({
		@XmlElement(name = "simple", type = SimpleMailContent.class),
		@XmlElement(name = "template", type = TemplateMailContent.class)
	})
	private List<MailContent> content;
	@XmlElement
	private String locale;
	@XmlElement
	private AnyBean entity;

	public AnyBean getEntity() {
		return entity;
	}

	public void setEntity(AnyBean entity) {
		this.entity = entity;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	public List<MailContent> getContent() {
		return content;
	}

	public void setContent(List<MailContent> content) {
		this.content = content;
	}

	public String getSenderAddress() {
		return senderAddress;
	}

	public void setSenderAddress(String senderAddress) {
		this.senderAddress = senderAddress;
	}

	public List<String> getBcc() {
		return bcc;
	}

	public void setBcc(List<String> bcc) {
		this.bcc = bcc;
	}

	public List<String> getTo() {
		return to;
	}

	public void setTo(List<String> to) {
		this.to = to;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

}
