package org.bndly.common.mail.impl;

/*-
 * #%L
 * Mail Impl
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

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.pop3.Pop3Server;
import com.icegreen.greenmail.smtp.SmtpServer;
import com.icegreen.greenmail.user.GreenMailUser;
import com.icegreen.greenmail.util.ServerSetup;
import com.sun.mail.pop3.POP3Store;
import org.bndly.common.data.io.IOUtils;
import org.bndly.common.mail.api.Attachment;
import org.bndly.common.mail.api.AttachmentImpl;
import org.bndly.common.mail.api.ConfigurationImpl;
import org.bndly.common.mail.api.EmailAddress;
import org.bndly.common.mail.api.MailException;
import org.bndly.common.mail.api.MailImpl;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.internet.MimeMultipart;
import junit.framework.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public class MailerImplTest {

	private static int getIntSystemProperty(String name) {
		return Integer.valueOf(System.getProperty(name));
	}
	
	public static final ServerSetup SMTP = new ServerSetup(getIntSystemProperty("greenmail.port.smtp"), null, ServerSetup.PROTOCOL_SMTP);
	public static final ServerSetup SMTPS = new ServerSetup(getIntSystemProperty("greenmail.port.smtps"), null, ServerSetup.PROTOCOL_SMTPS);
	public static final ServerSetup POP3 = new ServerSetup(getIntSystemProperty("greenmail.port.pop3"), null, ServerSetup.PROTOCOL_POP3);
	public static final ServerSetup POP3S = new ServerSetup(getIntSystemProperty("greenmail.port.pop3s"), null, ServerSetup.PROTOCOL_POP3S);
	public static final ServerSetup IMAP = new ServerSetup(getIntSystemProperty("greenmail.port.imap"), null, ServerSetup.PROTOCOL_IMAP);
	public static final ServerSetup IMAPS = new ServerSetup(getIntSystemProperty("greenmail.port.imaps"), null, ServerSetup.PROTOCOL_IMAPS);

	@Rule
	public final GreenMailRule greenMail = new GreenMailRule(new ServerSetup[]{MailerImplTest.SMTP, MailerImplTest.POP3});

	@Test
	public void testAttachments() throws MailException, UnsupportedEncodingException, NoSuchProviderException, MessagingException, IOException {
		MailerImpl mailerImpl = new MailerImpl();
		ConfigurationImpl configurationImpl = new ConfigurationImpl();
		String domain = configurationImpl.getHost();
		String email = "bndly@" + domain;
		GreenMailUser user = greenMail.setUser(email, "123");

		SmtpServer smtp = greenMail.getSmtp();
		configurationImpl.setPort(smtp.getPort());
		configurationImpl.setHost(smtp.getBindTo());
		configurationImpl.setDebug(Boolean.TRUE);

		mailerImpl.configure(configurationImpl);
		MailImpl mail = new MailImpl();
		mail.setSender(new EmailAddress(email));
		mail.setTo(Arrays.asList(new EmailAddress(email)));
		mail.setSubject("testAttachments");
		List<Attachment> attachments = new ArrayList<>();
		AttachmentImpl attachment = new AttachmentImpl();
		attachment.setContentType("text/plain");
		attachment.setFileName("readme.txt");
//		String testDataOfAttachment = "hello world";
		StringBuilder stringBuilder = new StringBuilder();
		for (int i = 0; i < 256 * 1024 / 8; i++) {
			stringBuilder.append("01234567");
		}
		String testDataOfAttachment = stringBuilder.toString();
		final InputStream bis = new ByteArrayInputStream(testDataOfAttachment.getBytes("UTF-8"));
		attachment.setData(bis);
		attachments.add(attachment);
		
		byte[] testfileAsBytes = IOUtils.read(getClass().getClassLoader().getResourceAsStream("testfile.pdf"));
		
		AttachmentImpl pdfAttachment = new AttachmentImpl();
		pdfAttachment.setContentType("application/pdf");
		pdfAttachment.setData(new ByteArrayInputStream(testfileAsBytes));
		pdfAttachment.setFileName("testfile.pdf");
		attachments.add(pdfAttachment);
		mail.setAttachments(attachments);
		mailerImpl.send(mail);
		Pop3Server pop = greenMail.getPop3();
		POP3Store store = pop.createStore();
		store.connect(user.getLogin(), user.getPassword());
		try {
			Folder folder = store.getFolder("INBOX");
			folder.open(Folder.READ_ONLY);
			try {
				int messageCount = folder.getMessageCount();
				Assert.assertEquals("expected messages", 1, messageCount);
				Message[] messages = folder.getMessages();
				Message message = messages[0];
				Assert.assertEquals(mail.getSubject(), message.getSubject());
				Object messageContent = message.getContent();
				Assert.assertTrue(MimeMultipart.class.isInstance(messageContent));
				MimeMultipart mm = (MimeMultipart) messageContent;
				BodyPart attachmentPart = null;
				BodyPart pdfAttachmentPart = null;
				for (int i = 0; i < mm.getCount(); i++) {
					BodyPart part = mm.getBodyPart(i);
					if ("readme.txt".equals(part.getFileName())) {
						attachmentPart = part;
					} else  if ("testfile.pdf".equals(part.getFileName())) {
						pdfAttachmentPart = part;
					}
				}
				Assert.assertNotNull("could not find attachment", attachmentPart);
				try (InputStream is = attachmentPart.getInputStream()) {
					String attachmentContent = IOUtils.readToString(is, "UTF-8");
					Assert.assertEquals(testDataOfAttachment, attachmentContent);
				}
				Assert.assertNotNull("could not find odf attachment", pdfAttachmentPart);
				try (InputStream is = pdfAttachmentPart.getInputStream()) {
					byte[] pdfBytesFromMail = IOUtils.read(is);
					Assert.assertEquals("test pdf was not complete", testfileAsBytes.length, pdfBytesFromMail.length);
					for (int i = 0; i < pdfBytesFromMail.length; i++) {
						Assert.assertEquals(testfileAsBytes[i], pdfBytesFromMail[i]);
					}
				}
			} finally {
				folder.close(true);
			}
		} finally {
			store.close();
		}
	}
}
