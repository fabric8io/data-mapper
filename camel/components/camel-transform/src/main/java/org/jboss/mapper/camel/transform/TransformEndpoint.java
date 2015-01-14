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
package org.jboss.mapper.camel.transform;

import java.io.InputStream;

import org.apache.camel.Component;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.impl.DefaultEndpoint;
import org.apache.camel.spi.UriParam;
import org.dozer.DozerBeanMapper;

/**
 * Represents a Transform endpoint.
 */
public class TransformEndpoint extends DefaultEndpoint {

    @UriParam
    private String name;
    @UriParam
    private String marshalId;
    @UriParam
    private String unmarshalId;
    @UriParam
    private String sourceModel;
    @UriParam
    private String targetModel;
    @UriParam
    private String dozerConfigPath;

    private DozerBeanMapper mapper;

    /**
     * Create a new TransformEndpoint.
     * @param endpointUri The uri of the Camel endpoint.
     * @param component The {@link TransformComponent}.
     * @param name Name for this transform endpoint
     */
    public TransformEndpoint(String endpointUri, Component component, String name) throws Exception {
        super(endpointUri, component);
        this.name = name;
    }

    @Override
    public Producer createProducer() throws Exception {
        return new TransformProducer(this);
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        throw new UnsupportedOperationException(
                "Consumer not supported for Transform endpoints");
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public String getMarshalId() {
        return marshalId;
    }

    public void setMarshalId(String marshalId) {
        this.marshalId = marshalId;
    }

    public String getUnmarshalId() {
        return unmarshalId;
    }

    public void setUnmarshalId(String unmarshalId) {
        this.unmarshalId = unmarshalId;
    }

    public String getSourceModel() {
        return sourceModel;
    }

    public void setSourceModel(String sourceModel) {
        this.sourceModel = sourceModel;
    }

    public String getTargetModel() {
        return targetModel;
    }

    public void setTargetModel(String targetModel) {
        this.targetModel = targetModel;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public String getDozerConfigPath() {
        return dozerConfigPath;
    }

    public void setDozerConfigPath(String dozerConfigPath) {
        this.dozerConfigPath = dozerConfigPath;
    }
    
    public synchronized DozerBeanMapper getMapper() throws Exception {
        if (mapper == null) {
            initDozer();
        }
        return mapper;
    }

    private void initDozer() throws Exception {
        mapper = new DozerBeanMapper();
        InputStream mapStream = null;
        try {
            mapStream = getCamelContext().getClassResolver().loadResourceAsStream(dozerConfigPath);
            if (mapStream == null) {
                throw new Exception("Unable to resolve Dozer config: " + dozerConfigPath);
            }
            mapper.addMapping(mapStream);
        } finally {
            if (mapStream != null) {
                mapStream.close();
            }
        }
    }
}
