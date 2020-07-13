package org.bndly.rest.common.beans.error;

/*-
 * #%L
 * REST Common Beans
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

import org.bndly.rest.atomlink.api.annotation.ErrorBean;
import org.bndly.rest.common.beans.RestBean;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "error")
@XmlAccessorType(XmlAccessType.NONE)
public class ErrorRestBean extends RestBean implements ErrorBean {

    @XmlElement
    private String name;
    @XmlElement
    private String message;
    @XmlElement(type = StackTraceElementRestBean.class)
    private List<StackTraceElementRestBean> stackTraceElements;
    @XmlElement(type = ErrorKeyValuePairRestBean.class)
    private List<ErrorKeyValuePairRestBean> description;
    @XmlElement(type = ErrorRestBean.class)
    private ErrorRestBean cause;

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<StackTraceElementRestBean> getStackTraceElements() {
        return stackTraceElements;
    }

    public void setStackTraceElements(List<StackTraceElementRestBean> stackTraceElements) {
        this.stackTraceElements = stackTraceElements;
    }

    public List<ErrorKeyValuePairRestBean> getDescription() {
        return description;
    }

    public void setDescription(List<ErrorKeyValuePairRestBean> description) {
        this.description = description;
    }

    @Override
    public ErrorRestBean getCause() {
        return cause;
    }

    public void setCause(ErrorRestBean cause) {
        this.cause = cause;
    }

}
