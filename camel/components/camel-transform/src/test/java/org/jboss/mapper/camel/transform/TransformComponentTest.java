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

import org.apache.camel.impl.DefaultCamelContext;
import org.junit.Assert;
import org.junit.Test;

public class TransformComponentTest {
    
    private static final String NAME = "examplename";
    private static final String MARSHAL_ID = "marshal123";
    private static final String UNMARSHAL_ID = "unmarshal456";
    private static final String SOURCE_MODEL = "org.example.A";
    private static final String TARGET_MODEL = "org.example.B";
    private static final String DOZER_CONFIG_PATH = "test/dozerBeanMapping.xml";
    private static final String TRANSFORM_EP_1 =
            "transform:" + NAME 
            + "?marshalId=" + MARSHAL_ID 
            + "&unmarshalId=" + UNMARSHAL_ID 
            + "&sourceModel=" + SOURCE_MODEL 
            + "&targetModel=" + TARGET_MODEL
            + "&dozerConfigPath=" + DOZER_CONFIG_PATH;
    
    @Test
    public void testCreateEndpoint() throws Exception {
        TransformComponent comp = new TransformComponent();
        comp.setCamelContext(new DefaultCamelContext());
        TransformEndpoint ep = (TransformEndpoint)comp.createEndpoint(TRANSFORM_EP_1);
        
        Assert.assertEquals(NAME, ep.getName());
        Assert.assertEquals(MARSHAL_ID, ep.getMarshalId());
        Assert.assertEquals(UNMARSHAL_ID, ep.getUnmarshalId());
        Assert.assertEquals(SOURCE_MODEL, ep.getSourceModel());
        Assert.assertEquals(TARGET_MODEL, ep.getTargetModel());
        Assert.assertEquals(DOZER_CONFIG_PATH, ep.getDozerConfigPath());
    }
}
