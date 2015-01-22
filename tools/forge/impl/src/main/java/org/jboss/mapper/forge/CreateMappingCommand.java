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
package org.jboss.mapper.forge;

import javax.inject.Inject;

import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UICompleter;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.mapper.MapperConfiguration;
import org.jboss.mapper.dozer.DozerMapperConfiguration;

public class CreateMappingCommand extends AbstractMapperCommand {

    public static final String NAME = "create-mapping";
    public static final String DESCRIPTION = "Create a new mapping definition";

    @Inject
    @WithAttributes(label = "Source Model", required = true, description = "Name of the source model type")
    UIInput<String> sourceModel;

    @Inject
    @WithAttributes(label = "Target Model", required = true, description = "Name of the target model type")
    UIInput<String> targetModel;
    
    @Inject
    @WithAttributes(label = "Dozer Config",
        defaultValue = DozerMapperConfiguration.DEFAULT_DOZER_CONFIG,
        required = true, 
        description = "Path to the Dozer configuration file")
    UIInput<String> dozerConfig;

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        UICompleter<String> modelTypes = getModelCompleter(
                getSelectedProject(builder.getUIContext()));
        sourceModel.setCompleter(modelTypes);
        targetModel.setCompleter(modelTypes);
        builder.add(sourceModel).add(targetModel);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        Project project = getSelectedProject(context);
        // Create configuration
        getMapperContext(project).setDozerPath(dozerConfig.getValue());
        MapperConfiguration config = loadConfig(project);
        config.addClassMapping(sourceModel.getValue(), targetModel.getValue());

        // Load models into mapper context
        getMapperContext(project).setSourceModel(loadModel(project, sourceModel.getValue()));
        getMapperContext(project).setTargetModel(loadModel(project, targetModel.getValue()));

        // Save on our way out
        saveConfig(project);
        return Results.success("Created mapping configuration.");
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
}
