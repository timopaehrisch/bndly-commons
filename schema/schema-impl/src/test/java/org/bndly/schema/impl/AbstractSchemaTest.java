package org.bndly.schema.impl;

/*-
 * #%L
 * Schema Impl
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

import org.bndly.schema.api.tx.Template;
import org.bndly.schema.api.tx.TransactionCallback;
import org.bndly.schema.api.tx.TransactionStatus;
import org.bndly.common.crypto.api.Base64Service;
import org.bndly.common.crypto.api.SimpleCryptoService;
import org.bndly.common.crypto.impl.CryptoServiceFactoryImpl;
import org.bndly.schema.api.AttributeMediator;
import org.bndly.schema.api.SchemaBeanProvider;
import org.bndly.schema.beans.SchemaBeanFactory;
import org.bndly.schema.impl.json.RecordJsonConverterImpl;
import org.bndly.schema.api.mapper.LobHandler;
import org.bndly.schema.vendor.mediator.BinaryAttributeMediator;
import org.bndly.schema.vendor.mediator.BooleanAttributeMediator;
import org.bndly.schema.vendor.mediator.CryptoAttributeMediator;
import org.bndly.schema.vendor.mediator.DateAttributeMediator;
import org.bndly.schema.vendor.mediator.DecimalAttributeMediator;
import org.bndly.schema.vendor.mediator.InverseAttributeMediator;
import org.bndly.schema.vendor.mediator.JSONAttributeMediator;
import org.bndly.schema.vendor.mediator.MixinAttributeMediator;
import org.bndly.schema.vendor.mediator.StringAttributeMediator;
import org.bndly.schema.vendor.mediator.TypeAttributeMediator;
import org.bndly.schema.impl.nquery.expression.DelegatingExpressionStatementHandler;
import org.bndly.schema.impl.nquery.expression.EqualExpressionStatementHandler;
import org.bndly.schema.impl.nquery.expression.GreaterEqualExpressionStatementHandler;
import org.bndly.schema.impl.nquery.expression.GreaterExpressionStatementHandler;
import org.bndly.schema.impl.nquery.expression.InRangeExpressionStatementHandler;
import org.bndly.schema.impl.nquery.expression.LowerEqualExpressionStatementHandler;
import org.bndly.schema.impl.nquery.expression.LowerExpressionStatementHandler;
import org.bndly.schema.impl.nquery.expression.TypedExpressionStatementHandler;
import org.bndly.schema.impl.vendor.VendorConfigurations;
import org.bndly.schema.json.beans.JSONSchemaBeanFactory;
import org.bndly.schema.model.Attribute;
import org.bndly.schema.vendor.VendorConfiguration;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.h2.jdbcx.JdbcDataSource;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

/**
 *
 * @author cybercon &lt;bndly@cybercon.de&gt;
 */
public abstract class AbstractSchemaTest {
	protected EngineImpl engine;

	protected SchemaBeanFactory schemaBeanFactory;

	protected JSONSchemaBeanFactory jsonSchemaBeanFactory;
	protected TransactionTemplateImpl transactionTemplate;
	protected DelegatingExpressionStatementHandler delegatingExpressionStatementHandler;
	protected CryptoProviderImpl cryptoProvider;
	protected Map<String, SimpleCryptoService> cryptoServicesByName;
	protected CryptoServiceFactoryImpl cryptoService;
	protected Base64Service base64Service;
	protected SimpleCryptoService simpleCryptoService;
	
	@AfterMethod
	public void destroy() {
		transactionTemplate.setDataSource(null);
	}

	@BeforeMethod
	public void setup() {
		cryptoService = new CryptoServiceFactoryImpl();
		Path path = Paths.get("src","test","resources","demo.jceks");
		cryptoService.setKeystoreLocation(path.toString());
		cryptoService.setKeystorePassword("changeit");
		cryptoService.setKeystoreType("jceks");
		cryptoService.setEncryptionKeyAlias("symkey");
		cryptoService.setEncryptionKeyAlgorithm("AES");
		cryptoService.setEncryptionKeyPassword("changeit");
		cryptoService.initDefaultConfigs();
		base64Service = cryptoService.createBase64Service();
		
		cryptoServicesByName = new HashMap<>();
		simpleCryptoService = cryptoService.createSimpleCryptoService();
		cryptoServicesByName.put(null, simpleCryptoService);
		cryptoProvider = new CryptoProviderImpl(cryptoService, cryptoServicesByName);
		JdbcDataSource ds = new org.h2.jdbcx.JdbcDataSource();
		ds.setURL("jdbc:h2:mem:");
		ds.setUser("sa");
		EngineImpl engineImpl = new EngineImpl();
		transactionTemplate = new SingleConnectionTransactionTemplateImpl(VendorConfigurations.H2, engineImpl);
		transactionTemplate.setDataSource(ds);
		
		transactionTemplate.doInTransaction(new TransactionCallback() {

			@Override
			public Object doInTransaction(TransactionStatus transactionStatus, Template template) {
				template.execute("DROP ALL OBJECTS");
				return null;
			}
		});

		VendorConfiguration vendorConfig = VendorConfigurations.H2;
		engineImpl.setVendorConfiguration(vendorConfig);
		AccessorImpl accessorImpl = new AccessorImpl();
		delegatingExpressionStatementHandler = new DelegatingExpressionStatementHandler();
		accessorImpl.setExpressionStatementHandler(delegatingExpressionStatementHandler);
		
		delegatingExpressionStatementHandler.addExpressionStatementHandler(new EqualExpressionStatementHandler());
		delegatingExpressionStatementHandler.addExpressionStatementHandler(new GreaterExpressionStatementHandler());
		delegatingExpressionStatementHandler.addExpressionStatementHandler(new GreaterEqualExpressionStatementHandler());
		delegatingExpressionStatementHandler.addExpressionStatementHandler(new LowerEqualExpressionStatementHandler());
		delegatingExpressionStatementHandler.addExpressionStatementHandler(new LowerExpressionStatementHandler());
		delegatingExpressionStatementHandler.addExpressionStatementHandler(new InRangeExpressionStatementHandler());
		delegatingExpressionStatementHandler.addExpressionStatementHandler(new TypedExpressionStatementHandler());
		
		engineImpl.setAccessor(accessorImpl);
		TransactionFactoryImpl queryRunnerImpl = new TransactionFactoryImpl();
		engineImpl.setQueryRunner(queryRunnerImpl);
		QueryContextFactoryImpl queryContextFactoryImpl = new QueryContextFactoryImpl();
		queryContextFactoryImpl.setVendorConfiguration(vendorConfig);
		engineImpl.setQueryContextFactory(queryContextFactoryImpl);
		VirtualAttributeAdapterRegistryImpl virtualAttributeAdapterRegistryImpl = new VirtualAttributeAdapterRegistryImpl();
		engineImpl.setVirtualAttributeAdapterRegistry(virtualAttributeAdapterRegistryImpl);
		ConstraintRegistryImpl constraintRegistryImpl = new ConstraintRegistryImpl();
		engineImpl.setConstraintRegistry(constraintRegistryImpl);
		DeployerImpl deployerImpl = new DeployerImpl(engineImpl);
		deployerImpl.setVendorConfiguration(vendorConfig);
		engineImpl.setDeployer(deployerImpl);
		TableRegistryImpl tableRegistryImpl = new TableRegistryImpl();
		tableRegistryImpl.setVendorConfiguration(vendorConfig);
		engineImpl.setTableRegistry(tableRegistryImpl);
		engine = engineImpl;

		MediatorRegistryImpl mediatorRegistry = new MediatorRegistryImpl();
		engineImpl.setMediatorRegistry(mediatorRegistry);
		Map<Class<? extends Attribute>, AttributeMediator<?>> mediatorMap = new HashMap<>();
		mediatorMap.put(org.bndly.schema.model.StringAttribute.class, new StringAttributeMediator());
		mediatorMap.put(org.bndly.schema.model.DecimalAttribute.class, new DecimalAttributeMediator());
		mediatorMap.put(org.bndly.schema.model.BooleanAttribute.class, new BooleanAttributeMediator());
		mediatorMap.put(org.bndly.schema.model.DateAttribute.class, new DateAttributeMediator());
		mediatorMap.put(org.bndly.schema.model.InverseAttribute.class, new InverseAttributeMediator());
		mediatorMap.put(org.bndly.schema.model.TypeAttribute.class, new TypeAttributeMediator(accessorImpl));
		mediatorMap.put(org.bndly.schema.model.MixinAttribute.class, new MixinAttributeMediator(accessorImpl));
		LobHandler lobHandler = new DefaultLobHandler(vendorConfig, engineImpl);
		mediatorMap.put(org.bndly.schema.model.BinaryAttribute.class, new BinaryAttributeMediator(lobHandler));
		mediatorMap.put(org.bndly.schema.model.JSONAttribute.class, new JSONAttributeMediator(lobHandler, new RecordJsonConverterImpl()));
		mediatorMap.put(org.bndly.schema.model.CryptoAttribute.class, new CryptoAttributeMediator(cryptoProvider, lobHandler));
		mediatorRegistry.setMediators(mediatorMap);

		tableRegistryImpl.setMediatorRegistry(mediatorRegistry);
		deployerImpl.setMediatorRegistry(mediatorRegistry);
		deployerImpl.setConstraintRegistry(constraintRegistryImpl);
		deployerImpl.setListeners(Collections.EMPTY_LIST, new ReentrantReadWriteLock());
		deployerImpl.setTableRegistry(tableRegistryImpl);
		deployerImpl.setDatabaseSchemaName(System.getProperty("deployer.schemaName"));
		deployerImpl.setTransactionTemplate(transactionTemplate);

		accessorImpl.setEngine(engineImpl);
		accessorImpl.setMediatorRegistry(mediatorRegistry);

		queryRunnerImpl.setTransactionTemplate(transactionTemplate);
		queryRunnerImpl.setEngineImpl(engineImpl);

		queryContextFactoryImpl.setAccessor(accessorImpl);
		queryContextFactoryImpl.setMediatorRegistry(mediatorRegistry);
		queryContextFactoryImpl.setTableRegistry(tableRegistryImpl);

		final AbstractSchemaTest thisTest = this;
		schemaBeanFactory = new SchemaBeanFactory(new SchemaBeanProvider() {

			@Override
			public String getSchemaName() {
				return thisTest.schemaBeanFactory.getEngine().getDeployer().getDeployedSchema().getName();
			}

			@Override
			public String getSchemaBeanPackage() {
				return thisTest.getSchemaBeanPackage();
			}

			@Override
			public ClassLoader getSchemaBeanClassLoader() {
				return getClass().getClassLoader();
			}
		});
		jsonSchemaBeanFactory = new JSONSchemaBeanFactory();
		schemaBeanFactory.setJsonSchemaBeanFactory(jsonSchemaBeanFactory);
		schemaBeanFactory.setEngine(engine);
		List<Class<?>> javaTypes = new ArrayList<>();
		schemaBeanFactory.setTypeBindings(javaTypes);
	}

	protected String getSchemaBeanPackage() {
		return "org.bndly.schema.impl.test.beans";
	};
}
