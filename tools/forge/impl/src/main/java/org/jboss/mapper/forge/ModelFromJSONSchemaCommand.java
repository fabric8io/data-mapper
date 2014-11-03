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
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;

import javax.inject.Inject;

import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.resource.FileResource;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UIInput;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jsonschema2pojo.Annotator;
import org.jsonschema2pojo.DefaultGenerationConfig;
import org.jsonschema2pojo.Jackson2Annotator;
import org.jsonschema2pojo.SchemaGenerator;
import org.jsonschema2pojo.SchemaMapper;
import org.jsonschema2pojo.rules.RuleFactory;

import com.sun.codemodel.JCodeModel;


public class ModelFromJSONSchemaCommand extends AbstractMapperCommand {
	
	public static final String NAME = "model-from-json-schema";
	public static final String DESCRIPTION = "Generate a Java class model from JSON Schema.";
	
	@Inject
	@WithAttributes(label = "Schema Path", required = true, description = "Path to JSON schema in project")
	UIInput<String> schemaPath;
	
	@Inject
	@WithAttributes(label = "Class Name", required = true, description = "Name used for the top-level generated class")
	UIInput<String> className;
	
	@Inject
	@WithAttributes(label = "Package Name", required = true, description = "Package name for generated model classes")
	UIInput<String> packageName;

	@Override
	public void initializeUI(UIBuilder builder) throws Exception {
		builder.add(schemaPath).add(packageName).add(className);
	}

	@Override
	public Result execute(UIExecutionContext context) throws Exception {
		Project project = getSelectedProject(context);
		FileResource<?> schemaFile = getFile(project, schemaPath.getValue());
		
		CustomGenerationConfig config = new CustomGenerationConfig();
		Annotator annotator = new Jackson2Annotator() {
			public boolean isAdditionalPropertiesSupported() { return false; }
		};
        RuleFactory ruleFactory = new RuleFactory();
		ruleFactory.setAnnotator(annotator);
        ruleFactory.setGenerationConfig(config);
		URL jsonSchemaUrl = schemaFile.getUnderlyingResourceObject().toURI().toURL();
        SchemaMapper mapper = new SchemaMapper(ruleFactory, new SchemaGenerator());
        JCodeModel codeModel = new JCodeModel();
        
        mapper.generate(codeModel, className.getValue(), packageName.getValue(), jsonSchemaUrl);
        codeModel.build(new File(project.getRoot().getChild("src/main/java").getFullyQualifiedName()));
		
		return Results.success("Model classes created for " + schemaPath.getValue());
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getDescription() {
		return DESCRIPTION;
	}
	
	private class CustomGenerationConfig extends DefaultGenerationConfig {
		private File source;
		
		@Override
		public Iterator<File> getSource() {
			return Arrays.asList(new File[] {source}).iterator();
		}
		@Override
		public boolean isIncludeHashcodeAndEquals() {
			return false;
		}
		@Override
		public boolean isIncludeToString() {
			return false;
		}
		@Override
		public boolean isUsePrimitives() {
			return true;
		}
	}
}
