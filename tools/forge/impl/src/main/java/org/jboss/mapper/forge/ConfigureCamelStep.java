package org.jboss.mapper.forge;

import static org.jboss.mapper.forge.MapperContext.JSON_TYPE;
import static org.jboss.mapper.forge.MapperContext.XML_TYPE;

import java.io.File;
import java.io.OutputStream;
import java.util.Arrays;

import javax.inject.Inject;
import javax.xml.bind.JAXBContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.camel.model.DataFormatDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.dataformat.DataFormatsDefinition;
import org.apache.camel.model.dataformat.JsonDataFormat;
import org.apache.camel.model.dataformat.JsonLibrary;
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
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;

public class ConfigureCamelStep extends AbstractMapperCommand implements UIWizardStep {

    private static String SPRING_NS = "http://www.springframework.org/schema/beans";
    private static String CAMEL_NS = "http://camel.apache.org/schema/spring";
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

        // All transformations, regardless of type, will use Dozer
        addDozerBean(camelConfig);
        // Create a transformation route to execute the mapping and any required
        // encoding/decoding of source and target data
        addTransformRoute(camelConfig, mapCtx);
        // If JSON is part of this transformation, register an appropriate data
        // format
        if (JSON_TYPE.equals(mapCtx.getSourceType())
                || JSON_TYPE.equals(mapCtx.getTargetType())) {
            addJsonDataFormat(camelConfig);
        }

        saveConfig(project, camelContextPath.getValue(), camelConfig);
        return Results.success();
    }

    @Override
    public NavigationResult next(UINavigationContext context) throws Exception {
        return null;
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

    private void addTransformRoute(Element config, MapperContext context) throws Exception {
        Element camelContextEl = (Element)config.getElementsByTagNameNS(
                CAMEL_NS, "camelContext").item(0);
        RouteDefinition route = new RouteDefinition();
        route.id(context.getTransformId()).from("direct:" + context.getTransformId());
        addDecoder(route, context);
        route.convertBodyTo(context.getTargetModel().getModelClass());
        addEncoder(route, context);
        JAXBContext jc = JAXBContext.newInstance("org.apache.camel.model");
        jc.createMarshaller().marshal(route, camelContextEl);
    }

    private void addEncoder(RouteDefinition route, MapperContext context) {
        switch (context.getTargetType()) {
        case JSON_TYPE:
            route.marshal("json");
            break;
        case XML_TYPE:
            route.marshal().jaxb(getPackage(context.getTargetModel().getType()));
            break;
        }
    }

    private void addDecoder(RouteDefinition route, MapperContext context) {
        switch (context.getSourceType()) {
        case JSON_TYPE:
            route.unmarshal("json");
            break;
        case XML_TYPE:
            route.unmarshal().jaxb(getPackage(context.getSourceModel().getType()));
            break;
        }
    }

    private void addJsonDataFormat(Element config) throws Exception {
        Element camelContextEl = (Element)config.getElementsByTagNameNS(
                CAMEL_NS, "camelContext").item(0);
        DataFormatsDefinition df = new DataFormatsDefinition();
        JsonDataFormat jdf = new JsonDataFormat();
        jdf.setLibrary(JsonLibrary.Jackson);
        jdf.setId("json");
        df.setDataFormats(Arrays.asList(new DataFormatDefinition[] { jdf }));

        JAXBContext jc = JAXBContext.newInstance("org.apache.camel.model.dataformat");
        DocumentFragment frag = camelContextEl.getOwnerDocument().createDocumentFragment();
        jc.createMarshaller().marshal(df, frag);
        camelContextEl.insertBefore(frag.getFirstChild(), camelContextEl.getFirstChild());
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

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

}
