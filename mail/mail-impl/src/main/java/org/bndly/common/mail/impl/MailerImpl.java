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

import org.bndly.common.mail.api.Attachment;
import org.bndly.common.mail.api.Configuration;
import org.bndly.common.mail.api.EmailAddress;
import org.bndly.common.mail.api.Mail;
import org.bndly.common.mail.api.MailContent;
import org.bndly.common.mail.api.MailException;
import org.bndly.common.mail.api.MailTemplate;
import org.bndly.common.mail.api.MailWrapper;
import org.bndly.common.mail.api.Mailer;
import org.bndly.common.mail.api.MailerListener;
import org.bndly.common.osgi.util.DictionaryAdapter;
import org.bndly.common.osgi.util.ServiceRegistrationBuilder;
import org.bndly.common.velocity.api.Renderer;
import org.bndly.common.velocity.api.ContextData;
import org.bndly.common.velocity.api.VelocityTemplate;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.activation.CommandMap;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.MailcapCommandMap;
import javax.mail.Address;
import javax.mail.AuthenticationFailedException;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.util.tracker.BundleTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
	configurationPolicy = ConfigurationPolicy.REQUIRE
)
@Designate(ocd = MailerImpl.OSGIConfiguration.class)
public class MailerImpl implements Mailer {

	private static final Logger LOG = LoggerFactory.getLogger(MailerImpl.class);


	@ObjectClassDefinition(
			name = "Mailer",
			description = "The mailer is used to integrate javax.mail and a templating mechanism into the OSGI container."
	)
	public @interface OSGIConfiguration {

		@AttributeDefinition(
				name = "Debug",
				description = "Set this property to true, to enable debug messages."
		)
		boolean debug() default false;

		@AttributeDefinition(
				name = "Host",
				description = "The mail server hostname"
		)
		String host();

		@AttributeDefinition(
				name = "Port",
				description = "The mail server port"
		)
		int port();

		@AttributeDefinition(
				name = "User",
				description = "The username to use for the mail server login"
		)
		String user();

		@AttributeDefinition(
				name = "Password",
				description = "The password to use for the mail server login",
				type = AttributeType.PASSWORD
		)
		String password();

		@AttributeDefinition(
				name = "Sender address",
				description = "The email address to use as the mail sender"
		)
		String senderAddress();

		@AttributeDefinition(
				name = "Connection timeout",
				description = "The timeout in milliseconds for establishing a connection. Defaults to infinity."
		)
		int connectionTimeout();

		@AttributeDefinition(
				name = "Read timeout",
				description = "The timeout in milliseconds for reading from a connection. Defaults to infinity."
		)
		int readTimeout();

		@AttributeDefinition(
				name = "Write timeout",
				description = "The timeout in milliseconds for writing to a connection. Defaults to infinity."
		)
		int writeTimeout();

		@AttributeDefinition(
				name = "From",
				description = "Set the mail envelope return address. Defaults to message.getFrom()."
		)
		String from();

		@AttributeDefinition(
				name = "Localhost",
				description = "Override the name of the local host for the HELO/EHLO command."
		)
		String localhost();

		@AttributeDefinition(
				name = "Local address",
				description = "The address to which a local SMTP socket should be bound. Defaults to the value picked from the Socket class."
		)
		String localAddress();

		@AttributeDefinition(
				name = "Local port",
				description = "The port to which a local SMTP socket should be bound. Defaults to the value picked from the Socket class."
		)
		int localPort();

		@AttributeDefinition(
				name = "Sign on with EHLO",
				description = "Disable to sign in with HELO command."
		)
		boolean ehlo() default true;

		@AttributeDefinition(
				name = "Authenticate with AUTH",
				description = "If this property is true, authentication with be done with the AUTH command."
		)
		boolean auth() default false;

		@AttributeDefinition(
				name = "Submitter",
				description = "The submitter to use in the AUTH tag in the MAIL FROM command. Typically used by a mail relay to pass along information about the original submitter of the message."
		)
		String submitter();

		@AttributeDefinition(
				name = "NOTIFY option",
				description = "The NOTIFY option to the RCPT command. Either NEVER, or some combination of SUCCESS, FAILURE, and DELAY (separated by commas)."
		)
		String dsnNotify();

		@AttributeDefinition(
				name = "RET option",
				description = "The RET option to the MAIL command. Either FULL or HDRS."
		)
		String dsnRet();

		@AttributeDefinition(
				name = "Allow 8Bit Mime",
				description = "If set to true, 'quoted-printalbe' and 'base64' encodings will be converted to '8bit'. Please note, that this has to be supported by the server."
		)
		boolean allow8BitMime();

		@AttributeDefinition(
				name = "Send mails partially",
				description = "If not all targeted mail addresses are valid, the mail would still be sent to the others."
		)
		boolean sendPartial() default false;

		@AttributeDefinition(
				name = "Use SASL for login",
				description = "Use javax.security.sasl for the login authentication. Defaults to false"
		)
		boolean saslEnable();

		@AttributeDefinition(
				name = "SASL mechanism names to use",
				description = "A comma separated list of SASL mechanism names that should be tried out during authentication."
		)
		String saslMechanisms();

		@AttributeDefinition(
				name = "SASL authorization ID",
				description = "An authorization ID to use in SASL. If not set the 'User' value would be used."
		)
		String saslAuthorizationId();

		@AttributeDefinition(
				name = "SASL Realm",
				description = "Realm for Digest-MD5 authentication"
		)
		String saslRealm();

		@AttributeDefinition(
				name = "SASL use canonical hostname for connection",
				description = "If this property is true, InetAddress.getCanonicalHostName() will be used for SASL authentication rather than the 'Host' property."
		)
		String saslUseCanonicalHostName();

		@AttributeDefinition(
				name = "Wait for quit",
				description = "Wait for a response of the QUIT command. If set to false, the connection will be closed immediately after QUIT command."
		)
		boolean quitWait() default true;

		@AttributeDefinition(
				name = "Report succes with exception",
				description = "Throw an exception for every successfully sent mail addressee."
		)
		boolean reportSuccess() default false;

		@AttributeDefinition(
				name = "Socket factory class name",
				description = "Class name of a custom javax.net.SocketFactory implementation."
		)
		String socketFactoryClass();

		@AttributeDefinition(
				name = "Socket factory fallback enabled",
				description = "If the defined socket factory can not create a socket, a fallback will be used."
		)
		boolean socketFactoryFallback() default true;

		@AttributeDefinition(
				name = "Socket factory port",
				description = "Specific connection target port, when the provided socket factory class is used."
		)
		int socketFactoryPort();

		@AttributeDefinition(
				name = "Enable SSL",
				description = "If true, SSL will be used for connections"
		)
		boolean sslEnable() default false;

		@AttributeDefinition(
				name = "Check SSL server identity",
				description = "If true, the identity of the target server will be checked."
		)
		boolean sslCheckServerIdentity() default false;

		@AttributeDefinition(
				name = "Trust",
				description = "If '*' is set, all hostnames are trusted. Provide a space separated list of hostnames to mark those as trusted. If no value is provided the trust will depend on the server certificates."
		)
		String sslTrust();

		@AttributeDefinition(
				name = "SSL Socket factory class name",
				description = "Class name of a custom javax.net.ssl.SSLSocketFactory implementation"
		)
		String sslSocketFactoryClass();

		@AttributeDefinition(
				name = "SSL Socket factory port",
				description = "Specific connection target port, when the provided SSL socket factory class is used."
		)
		int sslSocketFactoryPort();

		@AttributeDefinition(
				name = "Allowed SSL protocols",
				description = "Space separated list of enabled SSL protocols"
		)
		String sslProtocols();

		@AttributeDefinition(
				name = "Allowed SSL cipher suites",
				description = "Space separated list of enabled SSL cipher suites"
		)
		String sslCipherSuites();

		@AttributeDefinition(
				name = "Enable switching to TLS",
				description = "Set to true, to allow switching to TLS"
		)
		boolean startTlsEnable() default false;

		@AttributeDefinition(
				name = "Require switching to TLS from server",
				description = "Require switching to TLS during establishment of a connection"
		)
		boolean startTlsRequired() default false;

		@AttributeDefinition(
				name = "SOCKS5 proxy host",
				description = "The host name of a SOCKS5 proxy to use for connection creation."
		)
		String socksHost();

		@AttributeDefinition(
				name = "SOCKS5 proxy port",
				description = "The port of a SOCKS5 proxy to use for connection creation."
		)
		String socksPort();

		@AttributeDefinition(
				name = "Extension for MAIL command",
				description = "An extension string to append to the MAIL command"
		)
		String mailExtension();

		@AttributeDefinition(
				name = "Test connection with RSET",
				description = "If set to true, the connection status will be tested with RSET instead of NOOP command."
		)
		boolean userSet() default false;

		@AttributeDefinition(
				name = "Require 250 status for NOOP",
				description = "Require a status reponse to the NOOP command."
		)
		boolean noOpStrict() default true;

		@AttributeDefinition(
				name = "Mail Impl Bundle",
				description = "The symbolic name of the OSGI bundle, that implements the Mail API."
		)
		String mailImplBundle() default "com.sun.mail.javax.mail";
	}
	
	private final Properties props = new Properties();
	private Configuration configuration;
	@Reference
	private Renderer renderer;
	private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
	private final List<MailerListener> listeners = new ArrayList<>();
	private Authenticator authenticator;
	private BundleTracker<ClassLoader> mailImplBundleTracker;
	private ClassLoader mailImplClassLoader;
	private ServiceRegistration<Mailer> mailerReg;

	@Activate()
	public void activate(ComponentContext componentContext) {
		DictionaryAdapter dictionaryAdapter = new DictionaryAdapter(componentContext.getProperties());
		Configuration c = new DictionaryAdapterBasedConfigurationImpl(dictionaryAdapter);
		configure(c);
		final String mailImplBundleSymbolicName = dictionaryAdapter.getString("mailImplBundle", "com.sun.mail.javax.mail");
		// track the "com.sun.mail.javax.mail" bundle...
		// if it exists and is active, we can send mails within its classloaders scope.
		mailImplBundleTracker = new BundleTracker<ClassLoader>(componentContext.getBundleContext(), Bundle.ACTIVE, null) {
			@Override
			public ClassLoader addingBundle(Bundle bundle, BundleEvent event) {
				ClassLoader cl = bundle.adapt(BundleWiring.class).getClassLoader();
				if (mailImplBundleSymbolicName.equals(bundle.getSymbolicName())) {
					mailImplClassLoader = cl;
					if (mailerReg == null) {
						mailerReg = ServiceRegistrationBuilder.newInstance(Mailer.class, MailerImpl.this).register(context);
					}

				}
				return cl;
			}

			@Override
			public void removedBundle(Bundle bundle, BundleEvent event, ClassLoader cl) {
				if (cl == mailImplClassLoader) {
					if (mailerReg != null) {
						mailerReg.unregister();
						mailerReg = null;
					}
					mailImplClassLoader = null;
				}
				super.removedBundle(bundle, event, cl);
			}
		};
		mailImplBundleTracker.open();
	}

	@Deactivate
	public void deactivate() {
		if (mailerReg != null) {
			mailerReg.unregister();
			mailerReg = null;
		}
		mailImplBundleTracker.close();
	}

	@Override
	public void addMailerListener(MailerListener listener) {
		if (listener != null) {
			readWriteLock.writeLock().lock();
			try {
				listeners.add(listener);
			} finally {
				readWriteLock.writeLock().unlock();
			}
		}
	}

	@Override
	public void removeMailerListener(MailerListener listener) {
		if (listener != null) {
			readWriteLock.writeLock().lock();
			try {
				Iterator<MailerListener> iterator = listeners.iterator();
				while (iterator.hasNext()) {
					if (iterator.next() == listener) {
						iterator.remove();
					}
				}
			} finally {
				readWriteLock.writeLock().unlock();
			}
		}
	}

	@Override
	public MailContent renderTemplate(final MailTemplate template) throws MailException {
		final ByteArrayOutputStream bos = new ByteArrayOutputStream();
		VelocityTemplate vt = new VelocityTemplate();
		vt.setEntity(template.getEntity());
		vt.setLocale(template.getLocale());
		vt.setTemplateName(template.getTemplateName());
		if (template.getContextData() != null) {
			List<ContextData> cd = new ArrayList<>();
			vt.setContextData(cd);
			for (Map.Entry<String, Object> entry : template.getContextData().entrySet()) {
				String string = entry.getKey();
				Object object = entry.getValue();
				ContextData data = new ContextData();
				data.setKey(string);
				data.setValue(object);
				cd.add(data);
			}
		}

		OutputStreamWriter writer;
		try {
			writer = new OutputStreamWriter(bos, "UTF-8");
		} catch (UnsupportedEncodingException ex) {
			throw new IllegalStateException(ex);
		}
		renderer.render(vt, writer);
		try {
			writer.flush();
		} catch (IOException ex) {
			throw new MailException("could not render mail template: " + ex.getMessage(), ex);
		}
		return new MailContent() {
			@Override
			public byte[] getData() {
				return bos.toByteArray();
			}

			@Override
			public String getContentType() {
				return template.getContentType();
			}
		};
	}

	@Override
	public void send(Mail mail) throws MailException {
		final Mail mailToUse;
		if (mail.getSender() == null) {
			// set default sender
			final EmailAddress sa = configuration.getSenderAdress();
			if (sa == null) {
				throw new MailException("mail did not define a sender and the configuration of the mailer did not contain a default sender address");
			}
			mailToUse = new MailWrapper(mail) {
				@Override
				public EmailAddress getSender() throws MailException {
					return sa;
				}
				
			};
		} else {
			mailToUse = mail;
		}
		MimeMessage message = buildMimeMessageFromMail(mailToUse);
		try {
			ClassLoader tmp = mailImplClassLoader;
			if (tmp != null) {
				ClassLoader ccl = Thread.currentThread().getContextClassLoader();
				try {
					Thread.currentThread().setContextClassLoader(tmp);
					Transport.send(message);
				} finally {
					Thread.currentThread().setContextClassLoader(ccl);
				}
			} else {
				Transport.send(message);
			}
			readWriteLock.readLock().lock();
			try {
				for (MailerListener listener : listeners) {
					listener.onMailSent(mailToUse);
				}
			} finally {
				readWriteLock.readLock().unlock();
			}
		} catch (AuthenticationFailedException ex) {
			readWriteLock.readLock().lock();
			try {
				for (MailerListener listener : listeners) {
					listener.onAuthenticationFailure(mailToUse);
				}
			} finally {
				readWriteLock.readLock().unlock();
			}
			throw new MailException("failed to authenticate at the mail server", ex);
		} catch (MessagingException ex) {
			readWriteLock.readLock().lock();
			try {
				for (MailerListener listener : listeners) {
					listener.onGenericFailure(mailToUse);
					listener.onGenericFailure(mailToUse, ex);
				}
			} finally {
				readWriteLock.readLock().unlock();
			}
			throw new MailException("failed to send email: " + ex.getMessage(), ex);
		}
	}

	@Override
	public void configure(Configuration configuration) {
		if (configuration != null) {
			this.configuration = configuration;
			reconfigure();
		}
	}

	private MimeMessage buildMimeMessageFromMail(Mail mail) throws MailException {
		Authenticator a = authenticator;
		Session session;
		if (a != null) {
			session = Session.getInstance(props, a);
		} else {
			session = Session.getInstance(props);
		}
		MimeMessage message = new MimeMessage(session);
		if (mail.getSubject() != null) {
			try {
				message.setSubject(mail.getSubject());
			} catch (MessagingException ex) {
				LOG.warn("could not set subject: " + ex.getMessage(), ex);
			}
		}

		Address from = internetAddress(mail.getSender());
		try {
			message.setFrom(from);
		} catch (MessagingException ex) {
			throw new MailException("could not set 'from': " + ex.getMessage(), ex);
		}

		InternetAddress[] replyTo = new InternetAddress[]{};
		try {
			message.setReplyTo(replyTo);
		} catch (MessagingException ex) {
			throw new MailException("could not set 'replyTo': " + ex.getMessage(), ex);
		}

		addRecipients(message, mail.getTo(), Message.RecipientType.TO);
		addRecipients(message, mail.getBcc(), Message.RecipientType.BCC);
		try {
			message.setSentDate(new Date());
		} catch (MessagingException ex) {
			throw new MailException("could not set 'sentDate' " + replyTo.toString() + ": " + ex.getMessage(), ex);
		}

		MimeMultipart multipartContent = buildMimeMultipartContent(mail);
		List<Attachment> attachments = mail.getAttachments();
		if (attachments != null) {
			try {
				for (Attachment attachment : attachments) {
					// create attachment
					MimeBodyPart mimeBodyPart = new MimeBodyPart();
					DataSource source = new AttachmentDataSource(attachment);
					mimeBodyPart.setDataHandler(new DataHandler(source));
					mimeBodyPart.setFileName(attachment.getFileName());
					multipartContent.addBodyPart(mimeBodyPart);
				}
			} catch (MessagingException ex) {
				throw new MailException("could not add attachment: " + ex.getMessage(), ex);
			}
		}
		try {
			message.setContent(multipartContent);
		} catch (MessagingException ex) {
			throw new MailException("could not set 'content': " + ex.getMessage(), ex);
		}
		return message;
	}

	private InternetAddress internetAddress(EmailAddress ea) {
		try {
			return new InternetAddress(ea.getValue());
		} catch (AddressException ex) {
			throw new IllegalStateException("could not create InternetAddress from EmailAddress-Value: " + ex.getMessage(), ex);
		}
	}

	private void addRecipients(MimeMessage message, List<EmailAddress> addresses, Message.RecipientType type) {
		if (addresses != null) {
			for (EmailAddress emailAddress : addresses) {
				InternetAddress add = internetAddress(emailAddress);
				try {
					message.addRecipient(type, add);
				} catch (Exception ex) {
					throw new IllegalStateException("could not add 'recipient': " + ex.getMessage(), ex);
				}
			}
		}
	}

	private MimeMultipart buildMimeMultipartContent(Mail mail) throws MailException {
		MimeMultipart body = new MimeMultipart("mixed");

		List<MailContent> content = mail.getContent();
		if (content != null) {
			MimeBodyPart wrap = new MimeBodyPart();
			MimeMultipart bdy = new MimeMultipart("alternative");
			for (MailContent mailContent : content) {
				byte[] data = mailContent.getData();
				InternetHeaders headers = new InternetHeaders();
				String contentType = mailContent.getContentType();
				if (contentType == null) {
					throw new MailException("missing content type in mail content");
				}
				headers.addHeader("Content-Type", contentType + ";charset=UTF-8;");
				MimeBodyPart part;
				if (contentType.startsWith(MIME_PREFIX_TEXT)) {
					String mimeSubType = mailContent.getContentType().substring(MIME_PREFIX_TEXT.length());
					try {
						part = new MimeBodyPart();
						part.setText(new String(data, "UTF-8"), "UTF-8", mimeSubType);
					} catch (UnsupportedEncodingException | MessagingException ex) {
						throw new MailException("could not create MimeBodyPart: " + ex.getMessage(), ex);
					}
				} else {
					try {
						part = new MimeBodyPart(headers, data);
					} catch (MessagingException ex) {
						throw new MailException("could not create MimeBodyPart: " + ex.getMessage(), ex);
					}
				}
				try {
					bdy.addBodyPart(part);
				} catch (MessagingException ex) {
					throw new MailException("could not add MimeBodyPart: " + ex.getMessage(), ex);
				}
			}
			try {
				wrap.setContent(bdy);
			} catch (MessagingException ex) {
				throw new MailException("could not set content of body wrapper: " + ex.getMessage(), ex);
			}
			try {
				body.addBodyPart(wrap);
			} catch (MessagingException ex) {
				throw new MailException("could not add body wrapper to mixed body: " + ex.getMessage(), ex);
			}
		}
		return body;
	}
	private static final String MIME_PREFIX_TEXT = "text/";

	private boolean isEmpty(String string) {
		return string == null || string.isEmpty();
	}
	
	private void reconfigure() {
		boolean doAuth;
		if (isEmpty(configuration.getUser()) && isEmpty(configuration.getPassword())) {
			this.authenticator = null;
			doAuth = false;
			LOG.info("configured mailer to not use an authenticator");
		} else {
			this.authenticator = new Authenticator() {

				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(configuration.getUser(), configuration.getPassword());
				}

			};
			doAuth = true;
			LOG.info("configured mailer to use an authenticator with a user and password");
		}

		// see https://javamail.java.net/nonav/docs/api/com/sun/mail/smtp/package-summary.html
		if (configuration != null && props != null) {
			Boolean secured = configuration.getSSLEnable();
			if (secured == null) {
				secured = false;
			}
			String mailPrefix = "mail.";
			String protocolPrefix = mailPrefix + "smtp.";
			if (secured) {
				props.setProperty(mailPrefix + "transport.protocol", "smtps");
			} else {
				props.setProperty(mailPrefix + "transport.protocol", "smtp");
			}
			props.setProperty(protocolPrefix + "ssl.enable", Boolean.toString(secured));
			
			
			Boolean debug = configuration.getDebug();
			if (debug != null) {
				props.setProperty(mailPrefix + "debug", debug.toString());
			}
			String host = configuration.getHost();
			if (host != null) {
				props.setProperty(protocolPrefix + "host", host);
			}
			Integer port = configuration.getPort();
			if (port == null) {
				port = 25;
			}
			props.setProperty(protocolPrefix + "port", port.toString());
			if (doAuth) {
				props.setProperty(protocolPrefix + "auth", "true");
			} else {
				props.setProperty(protocolPrefix + "auth", "false");
			}
			Integer connectionTimeout = configuration.getConnectionTimeout();
			if (connectionTimeout != null) {
				props.setProperty(protocolPrefix + "connectiontimeout", connectionTimeout.toString());
			}
			Integer readTimeout = configuration.getReadTimeout();
			if (readTimeout != null) {
				props.setProperty(protocolPrefix + "timeout", readTimeout.toString());
			}
			Integer writeTimeout = configuration.getWriteTimeout();
			if (writeTimeout != null) {
				props.setProperty(protocolPrefix + "writetimeout", writeTimeout.toString());
			}
			EmailAddress from = configuration.getFrom();
			if (from != null) {
				props.setProperty(mailPrefix + "from", from.getValue());
			}
			String localhost = configuration.getLocalhost();
			if (localhost != null) {
				props.setProperty(protocolPrefix + "localhost", localhost);
			}
			String localAddress = configuration.getLocalAddress();
			if (localAddress != null) {
				props.setProperty(protocolPrefix + "localaddress", localAddress);
			}
			Integer localPort = configuration.getLocalPort();
			if (localPort != null) {
				props.setProperty(protocolPrefix + "localport", localPort.toString());
			}
			Boolean ehlo = configuration.getEhlo();
			if (ehlo != null) {
				props.setProperty(protocolPrefix + "ehlo", ehlo.toString());
			}
			String submitter = configuration.getSubmitter();
			if (submitter != null) {
				props.setProperty(protocolPrefix + "submitter", submitter);
			}
			String dsnNotify = configuration.getDSNNotify();
			if (dsnNotify != null) {
				props.setProperty(protocolPrefix + "dsn.notify", dsnNotify);
			}
			String dsnRet = configuration.getDSNRet();
			if (dsnRet != null) {
				props.setProperty(protocolPrefix + "dsn.ret", dsnRet);
			}
			Boolean allow8BitMime = configuration.getAllow8BitMime();
			if (allow8BitMime != null) {
				props.setProperty(protocolPrefix + "allow8bitmime", allow8BitMime.toString());
			}
			Boolean sendPartial = configuration.getSendPartial();
			if (sendPartial != null) {
				props.setProperty(protocolPrefix + "sendpartial", sendPartial.toString());
			}
			Boolean saslEnable = configuration.getSASLEnable();
			if (saslEnable != null) {
				props.setProperty(protocolPrefix + "sasl.enable", saslEnable.toString());
			}
			String saslMechanisms = configuration.getSASLMechanisms();
			if (saslMechanisms != null) {
				props.setProperty(protocolPrefix + "sasl.mechanisms", saslMechanisms);
			}
			String saslAuthorizationId = configuration.getSASLAuthorizationId();
			if (saslAuthorizationId != null) {
				props.setProperty(protocolPrefix + "sasl.authorizationid", saslAuthorizationId);
			}
			String saslRealm = configuration.getSASLRealm();
			if (saslRealm != null) {
				props.setProperty(protocolPrefix + "sasl.realm", saslRealm);
			}
			Boolean saslUseCanonicalHostName = configuration.getSASLUseCanonicalHostName();
			if (saslUseCanonicalHostName != null) {
				props.setProperty(protocolPrefix + "sasl.usecanonicalhostname", saslUseCanonicalHostName.toString());
			}
			Boolean quitWait = configuration.getQuitWait();
			if (quitWait != null) {
				props.setProperty(protocolPrefix + "quitwait", quitWait.toString());
			}
			Boolean reportSuccess = configuration.getReportSuccess();
			if (reportSuccess != null) {
				props.setProperty(protocolPrefix + "reportsuccess", reportSuccess.toString());
			}
			String socketFactoryClass = configuration.getSocketFactoryClass();
			if (socketFactoryClass != null) {
				props.setProperty(protocolPrefix + "socketFactory.class", socketFactoryClass);
			}
			Boolean socketFactoryFallback = configuration.getSocketFactoryFallback();
			if (socketFactoryFallback != null) {
				props.setProperty(protocolPrefix + "socketFactory.fallback", socketFactoryFallback.toString());
			}
			Integer socketFactoryPort = configuration.getSocketFactoryPort();
			if (socketFactoryPort != null) {
				props.setProperty(protocolPrefix + "socketFactory.port", socketFactoryPort.toString());
			}
			Boolean sslCheckServerIdentity = configuration.getSSLCheckServerIdentity();
			if (sslCheckServerIdentity != null) {
				props.setProperty(protocolPrefix + "ssl.checkserveridentity", sslCheckServerIdentity.toString());
			}
			String sslTrust = configuration.getSSLTrust();
			if (sslTrust != null) {
				props.setProperty(protocolPrefix + "ssl.trust", sslTrust);
			}
			String sslSocketFactoryClass = configuration.getSSLSocketFactoryClass();
			if (sslSocketFactoryClass != null) {
				props.setProperty(protocolPrefix + "ssl.socketFactory.class", sslSocketFactoryClass);
			}
			Integer sslSocketFactoryPort = configuration.getSSLSocketFactoryPort();
			if (sslSocketFactoryPort != null) {
				props.setProperty(protocolPrefix + "ssl.socketFactory.port", sslSocketFactoryPort.toString());
			}
			String sslProtocols = configuration.getSSLProtocols();
			if (sslProtocols != null) {
				props.setProperty(protocolPrefix + "ssl.protocols", sslProtocols);
			}
			String sslCipherSuites = configuration.getSSLCipherSuites();
			if (sslCipherSuites != null) {
				props.setProperty(protocolPrefix + "ssl.ciphersuites", sslCipherSuites);
			}
			Boolean startTLSEnable = configuration.getStartTLSEnable();
			if (startTLSEnable != null) {
				props.setProperty(protocolPrefix + "starttls.enable", startTLSEnable.toString());
			}
			Boolean startTLSRequired = configuration.getStartTLSRequired();
			if (startTLSRequired != null) {
				props.setProperty(protocolPrefix + "starttls.required", startTLSRequired.toString());
			}
			String socksHost = configuration.getSocksHost();
			if (socksHost != null) {
				props.setProperty(protocolPrefix + "socks.host", socksHost);
			}
			String socksPort = configuration.getSocksPort();
			if (socksPort != null) {
				props.setProperty(protocolPrefix + "socks.port", socksPort);
			}
			String mailExtension = configuration.getMailExtension();
			if (mailExtension != null) {
				props.setProperty(protocolPrefix + "mailextension", mailExtension);
			}
			Boolean userSet = configuration.getUserSet();
			if (userSet != null) {
				props.setProperty(protocolPrefix + "userset", userSet.toString());
			}
			Boolean noOpStrict = configuration.getNoOpStrict();
			if (noOpStrict != null) {
				props.setProperty(protocolPrefix + "noop.strict", noOpStrict.toString());
			}
			
			Enumeration<?> enumerator = props.propertyNames();
			while (enumerator.hasMoreElements()) {
				String prop = (String) enumerator.nextElement();
				LOG.info("{} = {}", prop, props.getProperty(prop));

			}
		}
	}

	public void setConfiguration(DictionaryAdapterBasedConfigurationImpl configuration) {
		this.configuration = configuration;
		reconfigure();
	}

	public void setRenderer(Renderer renderer) {
		this.renderer = renderer;
	}

}
