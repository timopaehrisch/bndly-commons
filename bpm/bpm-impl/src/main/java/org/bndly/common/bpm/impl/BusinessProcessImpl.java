package org.bndly.common.bpm.impl;

/*-
 * #%L
 * BPM Impl
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

import org.bndly.common.bpm.api.BusinessProcess;
import org.bndly.common.bpm.api.BusinessProcessData;

public class BusinessProcessImpl implements BusinessProcess {

    private String name;
    private String category;
    private Integer version;
    private String resourceName;
    private String diagramResourceName;
    private String description;
    private String deploymentId;
    private String key;
    private String id;
    private BusinessProcessData data;
    private BusinessProcessData imageData;

	@Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

	@Override
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

	@Override
    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

	@Override
    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

	@Override
    public String getDiagramResourceName() {
        return diagramResourceName;
    }

    public void setDiagramResourceName(String diagramResourceName) {
        this.diagramResourceName = diagramResourceName;
    }

	@Override
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

	@Override
    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

	@Override
    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

	@Override
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

	@Override
    public BusinessProcessData getData() {
        return data;
    }

    public void setData(BusinessProcessData data) {
        this.data = data;
    }

	@Override
    public BusinessProcessData getImageData() {
        return imageData;
    }

    public void setImageData(BusinessProcessData imageData) {
        this.imageData = imageData;
    }
}
