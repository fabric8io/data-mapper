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

import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class XmlToJsonTest {
    
    @EndpointInject(uri = "mock:result")
    private MockEndpoint resultEndpoint;
    
    @Produce(uri = "direct:start")
    private ProducerTemplate startEndpoint;
    
    @Autowired
    private CamelContext camelContext;
    
    @After
    public void tearDown() {
        resultEndpoint.reset();
    }
    
    @Test
    public void testXmlToJson() throws Exception {
        resultEndpoint.expectedMessageCount(1);
        startEndpoint.sendBody(getResourceAsString("abc-order.xml"));
        // check results
        resultEndpoint.assertIsSatisfied();
        String result = resultEndpoint.getExchanges().get(0).getIn().getBody(String.class);
        Assert.assertEquals(getResourceAsString("xyz-order.json"), result);
    }
    
    @Test
    public void testMultipleSends() throws Exception {
        resultEndpoint.expectedMessageCount(2);
        startEndpoint.sendBody(getResourceAsString("abc-order.xml"));
        startEndpoint.sendBody(getResourceAsString("abc-order.xml"));
        // check results
        resultEndpoint.assertIsSatisfied();
        String result1 = resultEndpoint.getExchanges().get(0).getIn().getBody(String.class);
        String result2 = resultEndpoint.getExchanges().get(1).getIn().getBody(String.class);
        Assert.assertEquals(getResourceAsString("xyz-order.json"), result1);
        Assert.assertEquals(getResourceAsString("xyz-order.json"), result2);
    }
    
    private String getResourceAsString(String resourcePath) {
        InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
        return camelContext.getTypeConverter().convertTo(String.class, is);
    }
}
