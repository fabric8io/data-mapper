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
package org.jboss.mapper.camel;

import java.io.File;
import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.jboss.mapper.TransformType;
import org.jboss.mapper.camel.config.CamelContextFactoryBean;
import org.jboss.mapper.camel.config.CamelEndpointFactoryBean;
import org.jboss.mapper.camel.config.DataFormat;
import org.jboss.mapper.camel.config.DataFormatsDefinition;
import org.jboss.mapper.camel.config.JaxbDataFormat;
import org.jboss.mapper.camel.config.JsonDataFormat;
import org.jboss.mapper.camel.config.JsonLibrary;
import org.jboss.mapper.camel.config.ObjectFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

/**
 * CamelConfigBuilder provides read/write access to Camel configuration used 
 * in a data transformation project.  This class assumes that all Camel configuration
 * is stored in a Spring application context.  Any changes to Camel configuration
 * through direct methods on this class or the underlying CamelContextFactoryBean
 * config model are in-memory only and not persisted until saveConfig() is called.
 */
public class CamelConfigBuilder {

    private static String SPRING_NS = "http://www.springframework.org/schema/beans";
    private static String CAMEL_NS = "http://camel.apache.org/schema/spring";
    private static String CAMEL_JAXB_PATH = "org.jboss.mapper.camel.config";
    
    // JAXB classes for Camel config model
    private JAXBContext jaxbCtx;

    private final CamelContextFactoryBean camelContext;
    private final Element camelConfig;

    private CamelConfigBuilder(File camelConfigFile) throws Exception {
        camelConfig = loadCamelConfig(camelConfigFile);
        JAXBElement<CamelContextFactoryBean> ccfb = getJAXBContext()
                .createUnmarshaller().unmarshal(
                        getCamelContextElement(), CamelContextFactoryBean.class);
        camelContext = ccfb.getValue();
    }
    
    /**
     * Load a Spring application context containing Camel configuration from 
     * the specified file.
     * @param file reference to a file containing Camel configuration
     * @return a config builder loaded with camel configuration
     * @throws Exception failed to read/parse configuration file
     */
    public static CamelConfigBuilder loadConfig(final File file) throws Exception {
        return new CamelConfigBuilder(file);
    }
    
    /**
     * Returns the top-level object model for Camel configuration.
     * @return Camel Context configuration
     */
    public CamelContextFactoryBean getCamelContext() {
        return camelContext;
    }

    /**
     * Add a transformation to the Camel configuration.  This method adds all 
     * required data formats, Dozer configuration, and the camel-transform
     * endpoint definition to the Camel config.
     * @param transformId id for the transformation
     * @param source type of the source data
     * @param sourceClass name of the source model class
     * @param target type of the target data
     * @param targetClass name of the target model class
     * @throws Exception failed to create transformation
     */
    public void addTransformation(String transformId,
            TransformType source, String sourceClass, 
            TransformType target, String targetClass) throws Exception {
        
        // All transformations, regardless of type, will use Dozer
        addDozerBean(camelConfig);
        
        // Add data formats
        DataFormatsDefinition df = new DataFormatsDefinition();
        DataFormat unmarshaller = createDataFormat(source, sourceClass);
        DataFormat marshaller = createDataFormat(target, targetClass);
        
        if (unmarshaller != null) {
            df.getAvroOrBarcodeOrBase64().add(unmarshaller);
        }
        if (marshaller != null) {
            df.getAvroOrBarcodeOrBase64().add(marshaller);
        }
        camelContext.setDataFormats(df);
        
        // Create a transformation endpoint
        camelContext.getEndpoint().add(createTransformEndpoint(
                transformId, sourceClass, targetClass, unmarshaller, marshaller));
        
        // Replace Camel Context in config DOM
        ObjectFactory of = new ObjectFactory();
        DocumentFragment frag = camelConfig.getOwnerDocument().createDocumentFragment();
        getJAXBContext().createMarshaller().marshal(of.createCamelContext(camelContext), frag);
        camelConfig.replaceChild(frag.getFirstChild(), getCamelContextElement());
    }
    
    /**
     * Persists the in-memory state of Camel configuration to the specified 
     * output stream.
     * @param output stream to write config to
     * @throws Exception failed to save configuration
     */
    public void saveConfig(final OutputStream output) throws Exception {
        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        t.transform(new DOMSource(camelConfig.getOwnerDocument()), new StreamResult(output));
    }

    private DataFormat createDataFormat(TransformType type, String className) throws Exception {
        DataFormat dataFormat = null;
        
        switch (type) {
        case JSON :
            dataFormat = createJsonDataFormat();
            break;
        case XML :
            dataFormat = createJaxbDataFormat(getPackage(className));
            break;
        }
        
        return dataFormat;
    }

    private CamelEndpointFactoryBean createTransformEndpoint(String transformId,
            String sourceClass, String targetClass, DataFormat unmarshaller, DataFormat marshaller) 
            throws Exception {
        
        CamelEndpointFactoryBean endpoint = new CamelEndpointFactoryBean();
        endpoint.setId(transformId);
        StringBuffer uriBuf = new StringBuffer("transform:" + transformId + "?");
        uriBuf.append("sourceModel=" + sourceClass);
        uriBuf.append("&targetModel=" + targetClass);
        if (marshaller != null) {
            uriBuf.append("&marshalId=" + marshaller.getId());
        }
        if (unmarshaller != null) {
            uriBuf.append("&unmarshalId=" + unmarshaller.getId());
        }
        
        endpoint.setUri(uriBuf.toString());
        return endpoint;
    }

    private DataFormat createJsonDataFormat() throws Exception {
        final String id = "json";
        JsonDataFormat jdf = new JsonDataFormat();
        jdf.setLibrary(JsonLibrary.JACKSON);
        jdf.setId(id);
        return jdf;
    }
    
    private DataFormat createJaxbDataFormat(String contextPath) throws Exception {
        final String id = "jaxb";
        JaxbDataFormat df = new JaxbDataFormat();
        df.setContextPath(contextPath);
        df.setId(id);
        return df;
    }

    private String getPackage(String type) {
        int idx = type.lastIndexOf('.');
        return idx > 0 ? type.substring(0, idx) : type;
    }
    
    private Element loadCamelConfig(File configFile) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setIgnoringElementContentWhitespace(true);
        Document doc = dbf.newDocumentBuilder().parse(configFile);
        return doc.getDocumentElement();
    }
    
    private synchronized JAXBContext getJAXBContext() {
        if (jaxbCtx == null) {
            try {
                jaxbCtx = JAXBContext.newInstance(ObjectFactory.class);
            } catch (final JAXBException jaxbEx) {
                throw new RuntimeException(jaxbEx);
            }
        }
        return jaxbCtx;
    }

    private void addDozerBean(Element parent) {
        Element dozerLoader = parent.getOwnerDocument().createElementNS(SPRING_NS, "bean");
        dozerLoader.setAttribute("id", "dozerConverterLoader");
        dozerLoader.setAttribute("class", "org.apache.camel.converter.dozer.DozerTypeConverterLoader");

        Element dozerMapper = parent.getOwnerDocument().createElementNS(SPRING_NS, "bean");
        dozerMapper.setAttribute("id", "mapper");
        dozerMapper.setAttribute("class", "org.dozer.DozerBeanMapper");
        Element property = parent.getOwnerDocument().createElementNS(SPRING_NS, "property");
        property.setAttribute("name", "mappingFiles");
        dozerMapper.appendChild(property);
        Element list = parent.getOwnerDocument().createElementNS(SPRING_NS, "list");
        property.appendChild(list);
        Element value = parent.getOwnerDocument().createElementNS(SPRING_NS, "value");
        value.appendChild(parent.getOwnerDocument().createTextNode("dozerBeanMapping.xml"));
        list.appendChild(value);

        parent.appendChild(dozerLoader);
        parent.appendChild(dozerMapper);
    }
    
    private Element getCamelContextElement() {
        return (Element)camelConfig.getElementsByTagNameNS(CAMEL_NS, "camelContext").item(0);
    }

}
