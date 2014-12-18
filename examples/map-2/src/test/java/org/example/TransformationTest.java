package org.example;

import java.io.FileInputStream;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.spring.CamelSpringTestSupport;
import org.junit.Test;
import org.springframework.context.support.AbstractXmlApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class TransformationTest extends CamelSpringTestSupport {
    
    @EndpointInject(uri = "mock:xml2json-test-output")
    private MockEndpoint resultEndpoint;
    
    @Produce(uri = "direct:xml2json-test-input")
    private ProducerTemplate startEndpoint;
    
    @Test
    public void transform() throws Exception {
        startEndpoint.sendBody(readFile("src/data/abc-order.xml"));
    }
    
    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() throws Exception {
                from("direct:xml2json-test-input")
                    .log("Before transformation:\n ${body}")
                    .to("ref:xml2json")
                    .log("After transformation:\n ${body}")
                    .to("mock:xml2json-test-output");
            }
        };
    }
    
    @Override
    protected AbstractXmlApplicationContext createApplicationContext() {
        return new ClassPathXmlApplicationContext("META-INF/spring/camel-context.xml");
    }
    
    private String readFile(String filePath) throws Exception {
        String content;
        FileInputStream fis = new FileInputStream(filePath);
        try {
             content = createCamelContext().getTypeConverter().convertTo(String.class, fis);
        } finally {
            fis.close();
        }
        return content;
    }
}
