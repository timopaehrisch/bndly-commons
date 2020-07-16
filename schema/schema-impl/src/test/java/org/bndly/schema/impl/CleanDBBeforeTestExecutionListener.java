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

import org.bndly.schema.api.services.Engine;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.test.context.TestContext;
//import org.springframework.test.context.TestExecutionListener;

public class CleanDBBeforeTestExecutionListener /*implements TestExecutionListener*/ {

	/*
    @Override
    public void beforeTestClass(TestContext tc) throws Exception {
    }

    @Override
    public void prepareTestInstance(TestContext tc) throws Exception {
    }

    @Override
    public void beforeTestMethod(TestContext tc) throws Exception {
        tc.getApplicationContext().getBean(Engine.class).reset();
        JdbcTemplate template = tc.getApplicationContext().getBean(JdbcTemplate.class);
        try {
            template.execute("DROP ALL OBJECTS");
        } catch(Exception e) {
//            template.execute("DROP DATABASE IF EXISTS schematest");
//            template.execute("CREATE DATABASE schematest");
        }
    }

    @Override
    public void afterTestMethod(TestContext tc) throws Exception {
    }

    @Override
    public void afterTestClass(TestContext tc) throws Exception {
    }
    */
}
