package org.bndly.rest.schema.beans;

/*-
 * #%L
 * REST Schema Resource Beans
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

import org.bndly.rest.common.beans.ListRestBean;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "attributes")
@XmlAccessorType(XmlAccessType.NONE)
public class AttributesList extends ListRestBean<AttributeBean> {

    @XmlElements({
        @XmlElement(name = "dateAttribute", type = DateAttributeBean.class),
        @XmlElement(name = "booleanAttribute", type = BooleanAttributeBean.class),
        @XmlElement(name = "cryptoAttribute", type = CryptoAttributeBean.class),
        @XmlElement(name = "decimalAttribute", type = DecimalAttributeBean.class),
        @XmlElement(name = "mixinAttribute", type = MixinAttributeBean.class),
        @XmlElement(name = "typeAttribute", type = TypeAttributeBean.class),
        @XmlElement(name = "inverseAttribute", type = InverseAttributeBean.class),
        @XmlElement(name = "binaryAttribute", type = BinaryAttributeBean.class),
        @XmlElement(name = "stringAttribute", type = StringAttributeBean.class)
    })
    private List<AttributeBean> items;

    @Override
    public void setItems(List<AttributeBean> items) {
        this.items = items;
    }

    @Override
    public List<AttributeBean> getItems() {
        return items;
    }
}
