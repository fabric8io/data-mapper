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
package org.jboss.mapper.model.json;

import java.io.File;
import java.net.URL;

import org.jsonschema2pojo.Jackson2Annotator;
import org.jsonschema2pojo.SchemaGenerator;
import org.jsonschema2pojo.SchemaMapper;
import org.jsonschema2pojo.rules.RuleFactory;

import com.sun.codemodel.JCodeModel;

/**
 * Model generator for JSON type definitions.  This generator supports 
 * model generation from JSON schema and JSON instance data.
 */
public class JsonModelGenerator {
	
	private JsonGenerationConfig config;
	
	/**
	 * Create a new XmlModelGenerator with default configuration.
	 */
	public JsonModelGenerator() {
		this(new JsonGenerationConfig());
	}
	
	/**
	 * Configuration used to control model generation behavior.
	 * @param config
	 */
	public JsonModelGenerator(JsonGenerationConfig config) {
		this.config = config;
	}
	
	/**
	 * Generates Java classes in targetPath directory given a JSON schema.
	 * @param className name of the top-level class used for the generated model
	 * @param packageName package name for generated model classes
	 * @param schemaUrl url for the JSON schema
	 * @param targetPath directory where class source will be generated
	 * @throws Exception failure during model generation
	 */
	public JCodeModel generateFromSchema(
			String className, String packageName, URL schemaUrl, File targetPath) 
			throws Exception {
		SchemaMapper mapper = createSchemaMapper();
		JCodeModel codeModel = new JCodeModel();
		mapper.generate(codeModel, className, packageName, schemaUrl);
        codeModel.build(targetPath);
        
        return codeModel;
	}
	
	private SchemaMapper createSchemaMapper() {
        RuleFactory ruleFactory = new RuleFactory();
		ruleFactory.setAnnotator(new Jackson2Annotator() {
			public boolean isAdditionalPropertiesSupported() { 
				return false; 
			}
		});
        ruleFactory.setGenerationConfig(config);
        return new SchemaMapper(ruleFactory, new SchemaGenerator());
	}
}
