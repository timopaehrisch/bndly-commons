package org.bndly.testfixture.test.impl;

/*-
 * #%L
 * Test Fixture Test
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

import org.bndly.common.data.io.IOUtils;
import org.bndly.common.json.model.JSArray;
import org.bndly.common.json.model.JSMember;
import org.bndly.common.json.model.JSObject;
import org.bndly.common.json.model.JSValue;
import org.bndly.common.json.parsing.JSONParser;
import org.bndly.rest.beans.testfixture.TestFixtureRestBean;
import org.bndly.testfixture.client.TestFixtureClient;
import org.bndly.testfixture.test.api.CleanFixture;
import org.bndly.testfixture.test.api.Fixture;
import org.bndly.testfixture.test.api.FixtureSetupException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.testng.Assert;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.TestException;

public class TestFixtureExecutionListener implements ITestListener {

	public static final TestFixtureClient testFixtureClient = new TestFixtureClient();
	boolean firstTestInClass = true;

	/**
	 * Query Remote Server for currently active Test Data / Schema
	 *
	 * @return testFixture
	 */
	public static TestFixtureRestBean collectActiveTestFixtureFromServer(TestFixtureClient testFixtureResourceProxy, String schemaName) {
		try {
			TestFixtureRestBean currentlyActiveTestFixtureRestBean = testFixtureResourceProxy.get(schemaName);
			return currentlyActiveTestFixtureRestBean;
		} catch (Exception e) {
			throw new TestException(e);
		}
	}

	private static JSObject getDataSetFromTestFixture(TestFixtureClient testFixtureResourceProxy, TestFixtureRestBean testFixtureRestBean) {
		Assert.assertNotNull(testFixtureRestBean);
		Assert.assertNotNull(testFixtureRestBean.getDataSetContent());

		try {
			return (JSObject) new JSONParser().parse(new StringReader(testFixtureRestBean.getDataSetContent()));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void assertEmptyTables(TestFixtureClient testFixtureResourceProxy, String schemaName, String... typesToEmpty) {
		Assert.assertNotNull(schemaName);

		TestFixtureRestBean relevantTestFixtureSlice = collectActiveTestFixtureFromServer(testFixtureResourceProxy, schemaName);
		JSObject dataSet = getDataSetFromTestFixture(testFixtureResourceProxy, relevantTestFixtureSlice);

		if (dataSet.getMembers() != null) {
			for (JSMember jSMember : dataSet.getMembers()) {
				if ("items".equals(jSMember.getName().getValue())) {
					JSArray itemsArray = (JSArray) jSMember.getValue();
					if (itemsArray != null) {
						for (JSValue jSValue : itemsArray) {
							JSObject typeElement = (JSObject) jSValue;
							if (typeElement.getMembers() != null) {
								String type = typeElement.getMemberStringValue("type");
								boolean shouldBeCounted = false;
								if (typesToEmpty == null || typesToEmpty.length == 0) {
									shouldBeCounted = true;
								} else {
									for (String typesToEmpty1 : typesToEmpty) {
										if (typesToEmpty1.equals(type)) {
											shouldBeCounted = true;
											break;
										}
									}
								}

								if (shouldBeCounted) {
									for (JSMember jSMember1 : typeElement.getMembers()) {
										if ("entries".equals(jSMember1.getName().getValue())) {
											JSArray entries = (JSArray) jSMember1.getValue();
											if (entries.getItems() != null && !entries.getItems().isEmpty()) {
												Assert.fail("tables were not empty");
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	@Override
	public void onTestStart(ITestResult itr) {
		ITestNGMethod testMethod = itr.getMethod();
		try {
			Class testclass = testMethod.getRealClass();
			Method javaMethod = testclass.getMethod(testMethod.getMethodName());
			CleanFixture cf = (CleanFixture) testclass.getAnnotation(CleanFixture.class);
			if (cf != null) {
				deployFixture(cf.value(), cf.schema(), "clean", testclass, testMethod);
			}
			Fixture annotation = javaMethod.getAnnotation(Fixture.class);
			if (annotation != null) {
				deployFixture(annotation.value(), annotation.schema(), annotation.description(), testclass, testMethod);
			}
		} catch (FixtureSetupException ex) {
			throw new IllegalStateException("failed to install a fixture", ex);
		} catch (NoSuchMethodException | SecurityException ex) {
			throw new IllegalStateException("could not find test method in testclass");
		}
	}

	private void deployFixture(String dataLocation, String schema, String purpose, Class testclass, ITestNGMethod testMethod) throws FixtureSetupException {
		TestFixtureRestBean tf = new TestFixtureRestBean();
		tf.setSchemaName(schema);
		tf.setPurpose(purpose);
		tf.setName(dataLocation);
		tf.setOrigin(testclass.getSimpleName() + "." + testMethod.getMethodName());
		Path p = Paths.get(dataLocation);
		if (Files.isRegularFile(p)) {
			try (InputStream is = Files.newInputStream(p, StandardOpenOption.READ)) {
				String content = IOUtils.readToString(is, "UTF-8");
				tf.setDataSetContent(content);
				testFixtureClient.update(tf);
			} catch (IOException e) {
				throw new FixtureSetupException("read data from fixture file " + dataLocation);
			}
		} else {
			InputStream is = getClass().getClassLoader().getResourceAsStream(dataLocation);
			if (is == null) {
				throw new FixtureSetupException("could not find test fixture data at " + dataLocation);
			}
			try {
				String content = IOUtils.readToString(is, "UTF-8");
				tf.setDataSetContent(content);
				testFixtureClient.update(tf);
			} catch (IOException e) {
				throw new FixtureSetupException("read data from fixture file " + dataLocation);
			} finally {
				try {
					is.close();
				} catch (IOException ex) {
					// silently close
				}
			}

		}
	}
	
	@Override
	public void onTestSuccess(ITestResult itr) {
	}

	@Override
	public void onTestFailure(ITestResult itr) {
	}

	@Override
	public void onTestSkipped(ITestResult itr) {
	}

	@Override
	public void onTestFailedButWithinSuccessPercentage(ITestResult itr) {
	}

	@Override
	public void onStart(ITestContext itc) {
		ITestNGMethod[] tm = itc.getAllTestMethods();
		for (ITestNGMethod tm1 : tm) {
			if (AbstractTestFixtureSupport.class.isInstance(tm1.getInstance())) {
				((AbstractTestFixtureSupport) tm1.getInstance()).setTestFixtureClient(testFixtureClient);
			}
		}
	}

	@Override
	public void onFinish(ITestContext itc) {
	}
}
