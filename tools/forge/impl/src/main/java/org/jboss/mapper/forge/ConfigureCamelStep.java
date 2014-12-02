package org.jboss.mapper.forge;

import static org.jboss.mapper.forge.MapperContext.JSON_TYPE;
import static org.jboss.mapper.forge.MapperContext.XML_TYPE;

import java.io.File;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.context.UINavigationContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.NavigationResult;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.forge.addon.ui.wizard.UIWizardStep;
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

public class ConfigureCamelStep extends AbstractMapperCommand implements UIWizardStep {

    private static String SPRING_NS = "http://www.springframework.org/schema/beans";
    private static String CAMEL_NS = "http://camel.apache.org/schema/spring";
    private static String CAMEL_JAXB_PATH = "org.jboss.mapper.camel.config";
    private static final String CAMEL_CTX_PATH = "META-INF/spring/camel-context.xml";

    @Inject
    @WithAttributes(label = "Context Path",
            required = true,
            defaultValue = CAMEL_CTX_PATH,
            description = "Path to Camel Context configuration.")
    UIInput<String> camelContextPath;

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        builder.add(camelContextPath);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        Project project = getSelectedProject(context);
        MapperContext mapCtx = getMapperContext(project);
        Element camelConfig = loadCamelConfig(project, camelContextPath.getValue());
        Element camelContextEl = (Element)camelConfig.getElementsByTagNameNS(
                CAMEL_NS, "camelContext").item(0);

        JAXBContext jc = JAXBContext.newInstance(CAMEL_JAXB_PATH);
        JAXBElement<CamelContextFactoryBean> ccfb = 
                jc.createUnmarshaller().unmarshal(camelContextEl, CamelContextFactoryBean.class);
        CamelContextFactoryBean camelContext = ccfb.getValue();
                
        
        // All transformations, regardless of type, will use Dozer
        addDozerBean(camelConfig);
        
        // Add data formats
        DataFormatsDefinition df = new DataFormatsDefinition();
        List<DataFormat> dfList = new LinkedList<DataFormat>();
        DataFormat jaxb = null;
        DataFormat json = null;
        
        if (JSON_TYPE.equals(mapCtx.getSourceType())
                || JSON_TYPE.equals(mapCtx.getTargetType())) {
            json = createJsonDataFormat();
            dfList.add(json);
        }
        if (XML_TYPE.equals(mapCtx.getSourceType())
                || XML_TYPE.equals(mapCtx.getTargetType())) {
            jaxb = createJaxbDataFormat(getPackage(mapCtx.getSourceModel().getType()));
            dfList.add(jaxb);
        }
        df.getAvroOrBarcodeOrBase64().addAll(dfList);
        camelContext.setDataFormats(df);
        
        // Create a transformation endpoint
        camelContext.getEndpoint().add(createTransformEndpoint(mapCtx, json, jaxb));
        
        // Replace Camel Context in config DOM
        ObjectFactory of = new ObjectFactory();
        DocumentFragment frag = camelConfig.getOwnerDocument().createDocumentFragment();
        jc.createMarshaller().marshal(of.createCamelContext(camelContext), frag);
        camelConfig.replaceChild(frag.getFirstChild(), camelContextEl);

        saveConfig(project, camelContextPath.getValue(), camelConfig);
        return Results.success();
    }

    @Override
    public NavigationResult next(UINavigationContext context) throws Exception {
        return null;
    }

    private CamelEndpointFactoryBean createTransformEndpoint( 
            MapperContext context, DataFormat marshaller, DataFormat unmarshaller) 
            throws Exception {
        
        CamelEndpointFactoryBean endpoint = new CamelEndpointFactoryBean();
        endpoint.setId(context.getTransformId());
        StringBuffer uriBuf = new StringBuffer("transform:" + context.getTransformId() + "?");
        uriBuf.append("sourceModel=" + context.getSourceModel().getType());
        uriBuf.append("&targetModel=" + context.getTargetModel().getType());
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
    
    private void saveConfig(Project project, String path, Element config) throws Exception {
        Transformer t = TransformerFactory.newInstance().newTransformer();
        t.setOutputProperty(OutputKeys.INDENT, "yes");
        t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        OutputStream out = getFile(project, path).getResourceOutputStream();
        t.transform(new DOMSource(config.getOwnerDocument()), new StreamResult(out));
        out.close();
    }

    private String getPackage(String type) {
        int idx = type.lastIndexOf('.');
        return idx > 0 ? type.substring(0, idx) : type;
    }
    
    private Element loadCamelConfig(Project project, String configPath) throws Exception {
        File camelConfig = getFile(project, configPath).getUnderlyingResourceObject();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setIgnoringElementContentWhitespace(true);
        Document doc = dbf.newDocumentBuilder().parse(camelConfig);
        return doc.getDocumentElement();
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

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

}
