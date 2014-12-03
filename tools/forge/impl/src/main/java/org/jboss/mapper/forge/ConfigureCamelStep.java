package org.jboss.mapper.forge;

import javax.inject.Inject;

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
import org.jboss.mapper.TransformType;
import org.jboss.mapper.camel.CamelConfigBuilder;

public class ConfigureCamelStep extends AbstractMapperCommand implements UIWizardStep {

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
        CamelConfigBuilder camelConfig = CamelConfigBuilder.loadConfig(
                getFile(project, CAMEL_CTX_PATH).getUnderlyingResourceObject());
        
        camelConfig.addTransformation(mapCtx.getTransformId(), 
                TransformType.valueOf(mapCtx.getSourceType()), 
                mapCtx.getSourceModel().getType(),
                TransformType.valueOf(mapCtx.getTargetType()),
                mapCtx.getTargetModel().getType());
        
        camelConfig.saveConfig(getFile(project, CAMEL_CTX_PATH).getResourceOutputStream());
        return Results.success();
    }

    @Override
    public NavigationResult next(UINavigationContext context) throws Exception {
        return null;
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
