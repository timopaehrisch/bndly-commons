package org.bndly.rest.mail.resources;

/*-
 * #%L
 * REST Mail Resource
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

import org.bndly.common.mail.api.Attachment;
import org.bndly.common.mail.api.EmailAddress;
import org.bndly.common.mail.api.MailException;
import org.bndly.common.mail.api.Mailer;
import org.bndly.rest.atomlink.api.annotation.AtomLink;
import org.bndly.rest.api.Context;
import org.bndly.rest.api.StatusWriter;
import org.bndly.rest.controller.api.Meta;
import org.bndly.rest.controller.api.POST;
import org.bndly.rest.controller.api.Path;
import org.bndly.rest.controller.api.Response;
import org.bndly.rest.mail.beans.Mail;
import org.bndly.rest.mail.beans.TemplateMailContent;
import org.bndly.rest.common.beans.Services;
import org.bndly.rest.common.beans.error.ErrorRestBean;
import org.bndly.rest.controller.api.ControllerResourceRegistry;
import org.bndly.rest.controller.api.Documentation;
import org.bndly.rest.controller.api.DocumentationResponse;
import org.bndly.rest.mail.beans.MailContent;
import org.bndly.rest.mail.beans.SimpleMailContent;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;

@Component(service = MailResource.class, immediate = true)
@Path("mail")
public class MailResource {

	@Reference
	private Mailer mailer;
	
	@Reference
	private ControllerResourceRegistry controllerResourceRegistry;

	@Activate
	public void activate() {
		controllerResourceRegistry.deploy(this);
	}

	@Deactivate
	public void deactivate() {
		controllerResourceRegistry.undeploy(this);
	}

	@POST
	@AtomLink(rel = "sendMail", target = Services.class)
	@Documentation(
		authors = "bndly@bndly.org",
		consumes = Documentation.ANY_CONTENT_TYPE, 
		responses = {
			@DocumentationResponse(
				code = StatusWriter.Code.NO_CONTENT, 
				description = "The mail could be successfully submitted to the SMTP server. This does not mean, that the mail has reached its recipient(s)."
			),
			@DocumentationResponse(
				code = StatusWriter.Code.INTERNAL_SERVER_ERROR, 
				description = "The mail could not be created or sent. The response message should contain more information on the reasons.", 
				messageType = ErrorRestBean.class
			)
		}, 
		summary = "Send a mail", 
		value = "Sends a mail based on the provided information. The mail can contain simple text or references to templates, that will be rendered with the provided entity."
	)
	public Response sendMail(final Mail mail, @Meta Context context) throws MailException {
		Locale localeTmp = context.getLocale();
		if (mail.getLocale() != null) {
			try {
				localeTmp = Locale.forLanguageTag(mail.getLocale());
			} catch (Exception e) {
				// ignore this exception. then there is no locale.
			}
		}
		final Locale locale = localeTmp;
		final Object entity = mail.getEntity() == null ? null : mail.getEntity().getElement();
		org.bndly.common.mail.api.Mail m = new org.bndly.common.mail.api.Mail() {
			@Override
			public List<org.bndly.common.mail.api.MailContent> getContent() throws MailException {
				final List<org.bndly.common.mail.api.MailContent> content = new ArrayList<>();
				List<MailContent> t = mail.getContent();
				if (t != null) {
					for (MailContent mailContent : t) {
						if (TemplateMailContent.class.isInstance(mailContent)) {
							TemplateMailContent templateMailContent = (TemplateMailContent) mailContent;
							org.bndly.common.mail.api.MailTemplate template = new org.bndly.common.mail.api.MailTemplate();
							template.setMail(this);
							template.setTemplateName(templateMailContent.getTemplateName());
							template.setLocale(locale);
							template.setEntity(entity);
							template.setContentType(mailContent.getContentType());
							content.add(mailer.renderTemplate(template));
						} else if (SimpleMailContent.class.isInstance(mailContent)) {
							final SimpleMailContent simpleMailContent = (SimpleMailContent) mailContent;
							String text = simpleMailContent.getText();
							if (text == null) {
								continue;
							}
							try {
								final byte[] data = text.getBytes("UTF-8");
								content.add(new org.bndly.common.mail.api.MailContent() {
									@Override
									public byte[] getData() {
										return data;
									}

									@Override
									public String getContentType() {
										return simpleMailContent.getContentType();
									}
								});
							} catch (UnsupportedEncodingException ex) {
								throw new MailException("could not convert text to bytes", ex);
							}
						} else {
							throw new MailException("unsupported mail content: " + mailContent.getClass());
						}
					}
				}
				return content;
			}

			@Override
			public List<EmailAddress> getTo() {
				return toEmailAddresses(mail.getTo());
			}

			@Override
			public List<EmailAddress> getBcc() {
				return toEmailAddresses(mail.getBcc());
			}

			@Override
			public EmailAddress getSender() {
				return toEmailAddress(mail.getSenderAddress());
			}

			@Override
			public String getSubject() {
				return mail.getSubject();
			}

			@Override
			public List<Attachment> getAttachments() throws MailException {
				// not supported yet
				return null;
			}
			
		};
		mailer.send(m);
		return Response.NO_CONTENT;
	}

	private List<EmailAddress> toEmailAddresses(List<String> bcc) {
		if (bcc == null) {
			return null;
		}

		List<EmailAddress> result = new ArrayList<>();
		for (String string : bcc) {
			EmailAddress a = toEmailAddress(string);
			if (a != null) {
				result.add(a);
			}
		}
		return result;
	}

	private EmailAddress toEmailAddress(String string) {
		if (string == null) {
			return null;
		}
		return new EmailAddress(string);
	}

	public void setMailer(Mailer mailer) {
		this.mailer = mailer;
	}

}
