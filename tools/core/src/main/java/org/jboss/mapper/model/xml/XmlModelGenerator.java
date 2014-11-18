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
package org.jboss.mapper.model.xml;

import java.io.File;
import java.io.FileInputStream;

import org.xml.sax.InputSource;

import com.sun.codemodel.JCodeModel;
import com.sun.tools.xjc.api.S2JJAXBModel;
import com.sun.tools.xjc.api.SchemaCompiler;
import com.sun.tools.xjc.api.XJC;

/**
 * Model generator for XML type definitions.  This generator supports 
 * model generation from XML schema and XML instance data.
 */
public class XmlModelGenerator {
	
	
	/**
	 * Generates Java classes in targetPath directory given an XML schema.
	 * @param schemaFile file reference to the XML schema
	 * @param packageName package name for generated model classes
	 * @param targetPath directory where class source will be generated
	 * @throws Exception failure during model generation
	 */
	public JCodeModel generateFromSchema(File schemaFile, String packageName, File targetPath) 
			throws Exception {
		
		SchemaCompiler sc = XJC.createSchemaCompiler();
		FileInputStream schemaStream = new FileInputStream(schemaFile);
		InputSource is = new InputSource(schemaStream);
		is.setSystemId(schemaFile.getAbsolutePath());
		
		sc.parseSchema(is);
		sc.forcePackageName(packageName);
		
		S2JJAXBModel s2 = sc.bind();
		JCodeModel jcm = s2.generateCode(null, null);
		jcm.build(targetPath);
		
		return jcm;
	}
}
