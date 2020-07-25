/*
 * Copyright (c) 2012, cyber:con GmbH, Bonn.
 *
 * All rights reserved. This source file is provided to you for
 * documentation purposes only. No part of this file may be
 * reproduced or copied in any form without the written
 * permission of cyber:con GmbH. No liability can be accepted
 * for errors in the program or in the documentation or for damages
 * which arise through using the program. If an error is discovered,
 * cyber:con GmbH will endeavour to correct it as quickly as possible.
 * The use of the program occurs exclusively under the conditions
 * of the licence contract with cyber:con GmbH.
 */
package org.bndly.rest.beans.testfixture;

/*-
 * #%L
 * Test Fixture Resource Beans
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
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Rest Bean to pass around embedded XML DataSets to serve as Test Fixtures.
 */
@XmlRootElement(name = "testFixture")
@XmlAccessorType(XmlAccessType.NONE)
public class TestFixtureRestBean extends RestBean {

    @XmlElement
    private String schemaName;
    @XmlElement
    private String name;
    @XmlElement
    private String purpose;
    @XmlElement
    private String origin;
    @XmlElement
    private String dataSetContent;

    public String getDataSetContent() {
        return dataSetContent;
    }

	public String getSchemaName() {
		return schemaName;
	}

	public void setSchemaName(String schemaName) {
		this.schemaName = schemaName;
	}

//    @XmlTransient
    public void setDataSetContent(String content) {
        this.dataSetContent = content;
    }

    public String getName() {
        return name;
    }

//    @XmlElement(name = "name")
    public void setName(String name) {
        this.name = name;
    }

    public String getPurpose() {
        return purpose;
    }

//    @XmlElement(name = "purpose")
    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getOrigin() {
        return origin;
    }

//    @XmlElement(name = "origin")
    public void setOrigin(String origin) {
        this.origin = origin;
    }

//    public Object getDataSetContentNode() {
//        if (dataSetContent == null) {
//            return null;
//        }
//        return W3CHelper.parseDocument(dataSetContent.trim()).getDocumentElement();
//    }

//    @XmlElement(name = "dataset")
//    public void setDataSetContentNode(Object contentObj) {
//        Node node = (Node) contentObj;
//        dataSetContent = W3CHelper.nodeToString(node).trim();
//    }
}
