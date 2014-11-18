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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.ui.context.UIBuilder;
import org.jboss.forge.addon.ui.context.UIExecutionContext;
import org.jboss.forge.addon.ui.input.UISelectOne;
import org.jboss.forge.addon.ui.metadata.WithAttributes;
import org.jboss.forge.addon.ui.result.Result;
import org.jboss.forge.addon.ui.result.Results;
import org.jboss.mapper.dozer.ConfigBuilder;
import org.jboss.mapper.dozer.config.Field;
import org.jboss.mapper.dozer.config.Mapping;
import org.jboss.mapper.dozer.config.Mappings;
import org.jboss.mapper.model.Model;


public class MapFieldCommand extends AbstractMapperCommand  {
	
	public static final String NAME = "map-field";
	public static final String DESCRIPTION = "Create a mapping between two fields.";
	private static final String FIELD_SEPARATOR = ".";

	@Inject
	@WithAttributes(label = "Source Field", required = true, description = "Full path of the source field")
	UISelectOne<String> sourceField;
	
	@Inject
	@WithAttributes(label = "Target Field", required = true, description = "Full path of the target field")
	UISelectOne<String> targetField;

	@Override
	public void initializeUI(UIBuilder builder) throws Exception {
		Project project = getSelectedProject(builder.getUIContext());
		Model sourceModel = getMapperContext(project).getSourceModel();
		if (sourceModel != null) {
			sourceField.setValueChoices(getEligibleFields(
					getMapperContext(project), sourceModel, FieldType.FROM));
		}
		Model targetModel = getMapperContext(project).getTargetModel();
		if (targetModel != null) {
			targetField.setValueChoices(getEligibleFields(
					getMapperContext(project), targetModel, FieldType.TO));
		}
		builder.add(sourceField).add(targetField);
	}

	@Override
	public Result execute(UIExecutionContext context) throws Exception {
		Project project = getSelectedProject(context);
		ConfigBuilder config = getMapperContext(project).getConfig();
		Model source = getModel(
				getMapperContext(project).getSourceModel(), sourceField.getValue());
		Model target = getModel(
				getMapperContext(project).getTargetModel(), targetField.getValue());
		config.map(source, target);

		saveConfig(project);
		
		return Results.success("Created field mapping.");
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public String getDescription() {
		return DESCRIPTION;
	}
	
	Model getModel(Model model, String path) {
		int sepIdx = path.indexOf(FIELD_SEPARATOR);
		if (sepIdx < 0) {
			return model.get(path);
		} else {
			String nextField = path.substring(0, sepIdx);
			return getModel(model.get(nextField), path.substring(sepIdx + 1));
		}
	}
	
	List<String> getEligibleFields(MapperContext context, Model model, FieldType fieldType) {
		List<String> mappedFields = getMappedFields(
				context.getConfig().getMappings(), fieldType);
		List<String> eligibleFields = model.listFields();
		
		for (String field : mappedFields) {
			if (!eligibleFields.remove(field)) {
				// Crappy fallback logic in case the model field name is
				// qualified by a parent which has already been mapped
				for (String fullName : eligibleFields) {
					if (fullName.endsWith("." + field)) {
						eligibleFields.remove(fullName);
						break;
					}
				}
			}
		}
		return eligibleFields;
	}
	
	List<String> getMappedFields(Mappings mappings, FieldType type) {
		List<String> mappedFields = new ArrayList<String>();
		for (Mapping map : mappings.getMapping()) {
			for (Object f :  map.getFieldOrFieldExclude()) {
				if (f instanceof Field) {
					Field field = (Field)f;
					mappedFields.add(type == FieldType.FROM 
							? field.getA().getContent() : field.getB().getContent());
				}
			}
		}
		return mappedFields;
	}
	
	private enum FieldType {TO, FROM};
}
