/*
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,  
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.mapper.forge;

import java.util.LinkedList;
import java.util.List;

import org.jboss.mapper.dozer.ConfigBuilder;
import org.jboss.mapper.model.Model;

public class MapperContext {

    private Model sourceModel;
    private Model targetModel;
    private ConfigBuilder config;
    private List<String> generatedTypes = new LinkedList<String>();
    private String sourceType;
    private String targetType;
    private String transformId;
    private String dozerPath;

    public Model getSourceModel() {
        return sourceModel;
    }

    public void setSourceModel(Model sourceModel) {
        this.sourceModel = sourceModel;
    }

    public Model getTargetModel() {
        return targetModel;
    }

    public void setTargetModel(Model targetModel) {
        this.targetModel = targetModel;
    }

    public ConfigBuilder getConfig() {
        return config;
    }

    public void setConfig(ConfigBuilder config) {
        this.config = config;
    }

    public List<String> getGeneratedTypes() {
        return generatedTypes;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getTransformId() {
        return transformId;
    }

    public void setTransformId(String transformId) {
        this.transformId = transformId;
    }

    public String getDozerPath() {
        return dozerPath;
    }

    public void setDozerPath(String dozerPath) {
        this.dozerPath = dozerPath;
    }
}
