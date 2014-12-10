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

import java.io.File;

import javax.inject.Inject;

import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.mapper.camel.CamelConfigBuilder;
import org.jboss.mapper.test.TestGenerator;

public class NewTransformationTest extends AbstractMapperCommand {

    public static final String NAME = "new-test";
    public static final String DESCRIPTION = "Create a unit test for a transformation endpoint";

    @Inject
    @WithAttributes(label = "Transform Endpoint ID", 
        required = true, 
        description = "Id of transform endpoint")
    UISelectOne<String> transformId;

    @Inject
    @WithAttributes(label = "Class Name", 
        defaultValue = "TransformTest",
        required = true, 
        description = "Name used for the top-level generated class")
    UIInput<String> className;

    @Inject
    @WithAttributes(label = "Package Name", 
        defaultValue = "org.example.test",
        required = true, 
        description = "Package name for generated model classes")
    UIInput<String> packageName;

    @Override
    public void initializeUI(UIBuilder builder) throws Exception {
        Project project = getSelectedProject(builder.getUIContext());
        CamelConfigBuilder camelConfig = CamelConfigBuilder.loadConfig(
                getFile(project, CAMEL_CTX_PATH));
        transformId.setValueChoices(camelConfig.getTransformEndpointIds());
        builder.add(transformId).add(packageName).add(className);
    }

    @Override
    public Result execute(UIExecutionContext context) throws Exception {
        Project project = getSelectedProject(context);
        File targetPath = new File(project.getRoot()
                .getChild("src/test/java").getFullyQualifiedName());
        TestGenerator.createTransformTest(transformId.getValue(),
                packageName.getValue(), 
                className.getValue(),
                targetPath);
        
        return Results.success("Unit test created for " + transformId.getValue());
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
