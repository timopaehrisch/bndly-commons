package org.bndly.rest.bpm.beans;

/*-
 * #%L
 * REST BPM Resource Beans
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
import org.bndly.rest.atomlink.api.annotation.BeanID;
import java.util.Date;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "businessProcessInstance")
@XmlAccessorType(XmlAccessType.NONE)
public class BusinessProcessInstance extends RestBean {

    @XmlElement
    @BeanID
    private String id;
	@XmlElement
    private String engineName;
    @XmlElement
    private String processName;
    @XmlElement
    private Date startTime;
    @XmlElement
    private Date endTime;
    @XmlElement
    private BusinessProcessVariables variables;

	public String getEngineName() {
		return engineName;
	}

	public void setEngineName(String engineName) {
		this.engineName = engineName;
	}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public BusinessProcessVariables getVariables() {
        return variables;
    }

    public void setVariables(BusinessProcessVariables variables) {
        this.variables = variables;
    }

}
