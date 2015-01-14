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

import java.util.Map;

import org.apache.camel.Endpoint;
import org.apache.camel.impl.DefaultComponent;

/**
 * Represents the component that manages {@link TransformEndpoint}.
 */
public class TransformComponent extends DefaultComponent {

    private static final String DEFAULT_DOZER_PATH = "dozerBeanMapping.xml";

    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        TransformEndpoint endpoint = new TransformEndpoint(uri, this, remaining);
        setProperties(endpoint, parameters);
        if (endpoint.getDozerConfigPath() == null) {
            endpoint.setDozerConfigPath(DEFAULT_DOZER_PATH);
        }
        return endpoint;
    }
}
