package org.bndly.common.app.provisioning.mojo;

/*-
 * #%L
 * App Provisioning
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import javax.net.ssl.SSLContext;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
@Mojo(name = "install", defaultPhase = LifecyclePhase.INSTALL)
public class BundleInstallMojo extends AbstractMojo {

	private static final String BUNDLE_HEADER_BUNDLE_SYMBOLIC_NAME = "Bundle-SymbolicName";
	private static final ContentType CT_JAR_ARCHIVE = ContentType.create("application/java-archive");
	
	private static final String REQ_HEADER_REFERER = "Referer";
	
	private static final String REQ_PARAM_ACTION = "action";
	private static final String REQ_PARAMVALUE_ACTION_INSTALL = "install";
	
	private static final String REQ_PARAM_NO_REDIRECT = "_noredir_";
	private static final String REQ_PARAMVALUE_NO_REDIRECT = "_noredir_";
	
	private static final String REQ_PARAM_BUNDLE_START = "bundlestart";
	private static final String REQ_PARAMVALUE_BUNDLE_START = "start";
	
	private static final String REQ_PARAM_REFRESH_PACKAGES = "refreshPackages";
	private static final String REQ_PARAMVALUE_REFRESH_PACKAGES = "refresh";
	
	private static final String REQ_PARAM_BUNDLE_START_LEVEL = "bundlestartlevel";
	private static final String REQ_PARAMVALUE_BUNDLE_START_LEVEL = "1";
	
	private static final String REQ_PARAM_FILE = "bundlefile";
	
	@Parameter(property = "cyber.install.skip", defaultValue = "false", required = true)
	protected boolean skipInstallation;

	// see implementation of org.apache.felix.webconsole.internal.core.BundlesServlet
	// you need to have /install at the end or otherwise the response will never be a status 200
	@Parameter(property = "cyber.webConsoleUrl", defaultValue = "http://localhost:8082/system/console/bundles/install", required = true)
	protected String webConsoleUrl;

	@Parameter(property = "cyber.user", required = false)
	protected String basicAuthUser;

	@Parameter(property = "cyber.password", required = false)
	protected String basicAuthPassword;

	@Parameter(property = "cyber.ssl.hostnameverifier", required = false)
	protected String hostnameverifier;

	@Parameter(property = "cyber.ssl.truststore", required = false)
	protected String truststore;

	@Parameter(property = "cyber.ssl.truststore.password", required = false)
	protected String truststorePassword;

	@Parameter(property = "cyber.ssl.protocols", required = false, alias = "sslProtocols")
	protected String[] sslProtocols;

	@Parameter(property = "cyber.ssl.ciphersuites", required = false, alias = "sslCipherSuites")
	protected String[] sslCipherSuites;

	@Parameter(property = "cyber.ssl.trustSelfSignedCerts", defaultValue = "false", required = true)
	protected boolean trustSelfSignedCerts;

	@Parameter(property = "cyber.bundle.file", defaultValue = "${project.build.directory}/${project.build.finalName}.jar", required = true)
	protected String bundleFileName;

	@Parameter(property = "cyber.bundle.startlevel", defaultValue = "1", required = true)
	protected String bundleStartLevel;

	@Parameter(property = "cyber.bundle.start", defaultValue = "true", required = true)
	protected boolean bundleStart;

	@Parameter(property = "cyber.refreshPackages", defaultValue = "true", required = true)
	protected boolean refreshPackages;

	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	protected MavenProject project;
	
	private static enum HOSTNAME_VERIFIER {
		all,
		browser,
		strict
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skipInstallation) {
			getLog().debug("bundle won't be installed, because installation is configured to be skipped");
			return;
		}

		Path bundleFilePath = Paths.get(bundleFileName);
		if (!Files.exists(bundleFilePath) || !Files.isRegularFile(bundleFilePath)) {
			throw new MojoExecutionException("can not install bundle, because " + bundleFileName + " is not a file");
		}

		try {
			if (!isOsgiBundle(bundleFilePath)) {
				throw new MojoExecutionException("can not install file, because " + bundleFileName + " is not an OSGI bundle");
			}
		} catch (IOException e) {
			throw new MojoExecutionException("could not verify, that " + bundleFileName + " is an OSGI bundle");
		}
		
		installBundleViaWebConsole(bundleFilePath);
	}

	private boolean isOsgiBundle(Path bundleFilePath) throws IOException {
		try (JarFile jarFile = new JarFile(bundleFilePath.toFile())) {
			Manifest manifest = jarFile.getManifest();
			if (manifest == null) {
				getLog().error("no manifest in " + bundleFilePath);
				return false;
			}
			String symbolicName = manifest.getMainAttributes().getValue(BUNDLE_HEADER_BUNDLE_SYMBOLIC_NAME);
			if (symbolicName == null) {
				getLog().error("no " + BUNDLE_HEADER_BUNDLE_SYMBOLIC_NAME + " header in the bundle " + bundleFilePath);
				return false;
			}
			return true;
		}
	}

	private void installBundleViaWebConsole(Path bundleFilePath) throws MojoExecutionException {
		HttpPost httpPost = new HttpPost(webConsoleUrl);
		httpPost.addHeader(REQ_HEADER_REFERER, "about:blank");
		MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create()
				.addTextBody(REQ_PARAM_ACTION, REQ_PARAMVALUE_ACTION_INSTALL)
				.addTextBody(REQ_PARAM_NO_REDIRECT, REQ_PARAMVALUE_NO_REDIRECT)
				.addBinaryBody(REQ_PARAM_FILE, bundleFilePath.toFile(), CT_JAR_ARCHIVE, bundleFilePath.getFileName().toString())
				.addTextBody(REQ_PARAM_BUNDLE_START_LEVEL, getStartLevel());
		if (bundleStart) {
			entityBuilder.addTextBody(REQ_PARAM_BUNDLE_START, REQ_PARAMVALUE_BUNDLE_START);
		}
		if (refreshPackages) {
			entityBuilder.addTextBody(REQ_PARAM_REFRESH_PACKAGES, REQ_PARAMVALUE_REFRESH_PACKAGES);
		}
		httpPost.setEntity(entityBuilder.build());
		try (CloseableHttpClient httpClient = createHttpClient()) {
			HttpContext ctx = new BasicHttpContext();
			if (webConsoleUrl.endsWith("/install")) {
				String authUrl = webConsoleUrl.substring(0, webConsoleUrl.length() - "/install".length());
				HttpGet authenticationPreflight = new HttpGet(authUrl);
				Integer statusCode = httpClient.execute(authenticationPreflight, new ResponseHandler<Integer>() {
					@Override
					public Integer handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
						return response.getStatusLine().getStatusCode();
					}
				}, ctx);
				getLog().info("auth preflight got: " + statusCode);
			}

			Integer statusCode = httpClient.execute(httpPost, new ResponseHandler<Integer>() {
				@Override
				public Integer handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
					return response.getStatusLine().getStatusCode();
				}
			}, ctx);
			if (statusCode == HttpStatus.SC_OK) {
				getLog().info("installed bundle");
			} else {
				throw new MojoExecutionException("could not install bundle. got status code " + statusCode);
			}
		} catch (IOException e) {
			throw new MojoExecutionException("could not install bundle", e);
		}
	}

	private String getStartLevel() {
		return bundleStartLevel == null ? REQ_PARAMVALUE_BUNDLE_START_LEVEL : bundleStartLevel;
	}
	
	private CloseableHttpClient createHttpClient() throws MojoExecutionException {
		HttpClientBuilder clientBuilder = HttpClientBuilder.create();
		if (basicAuthUser != null) {
			BasicCredentialsProvider basicCredentialsProvider = new BasicCredentialsProvider();
			basicCredentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(basicAuthUser, basicAuthPassword));
			clientBuilder.setDefaultCredentialsProvider(basicCredentialsProvider);
		}
		X509HostnameVerifier hnv;
		if (hostnameverifier != null) {
			HOSTNAME_VERIFIER hostnameVerifierValue = HOSTNAME_VERIFIER.valueOf(hostnameverifier);
			if (null != hostnameVerifierValue) {
				switch (hostnameVerifierValue) {
					case all:
						hnv = SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
						clientBuilder.setHostnameVerifier(hnv);
						break;
					case browser:
						hnv = SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER;
						clientBuilder.setHostnameVerifier(hnv);
						break;
					case strict:
						hnv = SSLConnectionSocketFactory.STRICT_HOSTNAME_VERIFIER;
						clientBuilder.setHostnameVerifier(hnv);
						break;
					default:
						throw new MojoExecutionException("unsupported hostname verifier " + hostnameVerifierValue);
				}
			} else {
				hnv = SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER;
				clientBuilder.setHostnameVerifier(hnv);
			}
		} else {
			hnv = SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER;
			clientBuilder.setHostnameVerifier(hnv);
		}

		boolean isCustomSslContextRequired = trustSelfSignedCerts || truststore != null;
		
		if (isCustomSslContextRequired) {
			// Trust own CA and all self-signed certs
			if (truststore != null) {
				SSLContext sslcontext;
				SSLContextBuilder sslContextBuilder = SSLContexts.custom();
				// load truststore
				try (InputStream is = Files.newInputStream(Paths.get(truststore), StandardOpenOption.READ)) {
					KeyStore keyStore = KeyStore.getInstance("JKS");
					keyStore.load(is, truststorePassword == null ? null : truststorePassword.toCharArray());
					if (trustSelfSignedCerts) {
						sslContextBuilder.loadTrustMaterial(keyStore, new TrustSelfSignedStrategy());
					} else {
						sslContextBuilder.loadTrustMaterial(keyStore);
					}
				} catch (IOException | NoSuchAlgorithmException | CertificateException | KeyStoreException ex) {
					throw new MojoExecutionException("could not set up custom truststore", ex);
				}
				try {
					sslcontext = sslContextBuilder.build();
				} catch (NoSuchAlgorithmException | KeyManagementException ex) {
					throw new MojoExecutionException("could not setup SSL context", ex);
				}
				
				// Allow TLSv1 protocol only
				SSLConnectionSocketFactory sslConnectionSocketFactory = new SSLConnectionSocketFactory(
						sslcontext,
						nonEmptyArray(sslProtocols),
						nonEmptyArray(sslCipherSuites),
						hnv);
				clientBuilder.setSSLSocketFactory(sslConnectionSocketFactory);
			}
		}
		return clientBuilder.build();
	}
	
	
	private String[] nonEmptyArray(String[] input) {
		if (input == null || input.length == 0) {
			return null;
		}
		return input;
	}
}
