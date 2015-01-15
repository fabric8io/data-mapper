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
import java.util.LinkedList;
import java.util.List;

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
import org.w3c.dom.NodeList;

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
    private static String TRANSFORM_SCHEME = "transform";
    
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
     * Returns the root element in the Spring application context which contains
     * bean definitions as well as the Camel Context configuration.
     * @return the <beans> element from the application context
     */
    public Element getConfiguration() {
        return camelConfig;
    }

    /**
     * Add a transformation to the Camel configuration.  This method adds all 
     * required data formats, Dozer configuration, and the camel-transform
     * endpoint definition to the Camel config.
     * @param transformId id for the transformation
     * @param dozerConfigPath path to Dozer config for transformation
     * @param source type of the source data
     * @param sourceClass name of the source model class
     * @param target type of the target data
     * @param targetClass name of the target model class
     * @throws Exception failed to create transformation
     */
    public void addTransformation(String transformId, String dozerConfigPath,
            TransformType source, String sourceClass, 
            TransformType target, String targetClass) throws Exception {
        
        // Add data formats
        DataFormat unmarshaller = createDataFormat(source, sourceClass);
        DataFormat marshaller = createDataFormat(target, targetClass);
        
        // Create a transformation endpoint
        camelContext.getEndpoint().add(createTransformEndpoint(dozerConfigPath,
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
    
    public List<String> getTransformEndpointIds() {
        List<String> endpointIds = new LinkedList<String>();
        for (CamelEndpointFactoryBean ep : camelContext.getEndpoint()) {
            if (ep.getUri().startsWith(TRANSFORM_SCHEME)) {
                endpointIds.add(ep.getId());
            }
        }
        return endpointIds;
    }

    private DataFormat createDataFormat(TransformType type, String className) throws Exception {
        DataFormat dataFormat;
        
        switch (type) {
        case JSON :
            dataFormat = createJsonDataFormat();
            break;
        case XML :
            dataFormat = createJaxbDataFormat(getPackage(className));
            break;
        case JAVA :
            dataFormat = null;
            break;
        default :
            throw new Exception("Unsupported data format type: " + type);
        }
        
        return dataFormat;
    }

    private CamelEndpointFactoryBean createTransformEndpoint(String dozerConfigPath, 
            String transformId, String sourceClass, String targetClass, 
            DataFormat unmarshaller, DataFormat marshaller) 
            throws Exception {
        
        CamelEndpointFactoryBean endpoint = new CamelEndpointFactoryBean();
        endpoint.setId(transformId);
        StringBuffer uriBuf = new StringBuffer(TRANSFORM_SCHEME + ":" + transformId + "?");
        uriBuf.append("sourceModel=" + sourceClass);
        uriBuf.append("&targetModel=" + targetClass);
        if (marshaller != null) {
            uriBuf.append("&marshalId=" + marshaller.getId());
        }
        if (unmarshaller != null) {
            uriBuf.append("&unmarshalId=" + unmarshaller.getId());
        }
        if (dozerConfigPath != null) {
            uriBuf.append("&dozerConfigPath=" + dozerConfigPath);
        }
        
        endpoint.setUri(uriBuf.toString());
        return endpoint;
    }

    private DataFormat createJsonDataFormat() throws Exception {
        final String id = "transform-json";
        
        DataFormat dataFormat = getDataFormat(id);
        if (dataFormat == null) {
            // Looks like we need to create a new one
            JsonDataFormat jdf = new JsonDataFormat();
            jdf.setLibrary(JsonLibrary.JACKSON);
            jdf.setId(id);
            getDataFormats().add(jdf);
            dataFormat = jdf;
        }
        return dataFormat;
    }
    
    private List<DataFormat> getDataFormats() {
        DataFormatsDefinition dfd = camelContext.getDataFormats();
        if (dfd == null) {
            dfd = new DataFormatsDefinition();
            camelContext.setDataFormats(dfd);
        }
        return dfd.getAvroOrBarcodeOrBase64();
    }
    
    private DataFormat getDataFormat(String id) {
        DataFormat dataFormat = null;
        for (DataFormat df : getDataFormats()) {
            if (id.equals(df.getId())) {
                dataFormat = df;
                break;
            }
        }
        return dataFormat;
    }
    
    private DataFormat createJaxbDataFormat(String contextPath) throws Exception {
        final String id = contextPath.replaceAll("\\.", "");
        DataFormat dataFormat = getDataFormat(id);
        
        if (dataFormat == null) {
            JaxbDataFormat df = new JaxbDataFormat();
            df.setContextPath(contextPath);
            df.setId(id);
            getDataFormats().add(df);
            dataFormat = df;
        }
        return dataFormat;
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
    
    // Returns the first instance of a child element that matches the specified name
    private Element getChildElement(Element parent, String childNS, String childName) {
        Element child = null;
        NodeList children = parent.getElementsByTagNameNS(childNS, childName);
        if (children.getLength() > 0) {
            child = (Element)children.item(0);
        }
        return child;
    }
    
    private Element getCamelContextElement() {
        return getChildElement(camelConfig, CAMEL_NS, "camelContext");
    }

}
