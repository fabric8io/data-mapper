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
package org.jboss.mapper.dozer;

import java.io.File;
import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.jboss.mapper.dozer.config.Field;
import org.jboss.mapper.dozer.config.FieldDefinition;
import org.jboss.mapper.dozer.config.Mapping;
import org.jboss.mapper.dozer.config.Mappings;
import org.jboss.mapper.model.Model;
import org.jboss.mapper.model.ModelBuilder;

public class ConfigBuilder {

    private static final String DOZER_SCHEMA_LOC = "http://dozer.sourceforge.net http://dozer.sourceforge.net/schema/beanmapping.xsd";

    // JAXB classes for Dozer config model
    private JAXBContext jaxbCtx;
    private Mappings mapConfig;

    private ConfigBuilder() {
        this(new Mappings());
    }

    private ConfigBuilder(Mappings mapConfig) {
        this.mapConfig = mapConfig;
    }

    private ConfigBuilder(File file) throws Exception {
        mapConfig = (Mappings) getJAXBContext().createUnmarshaller().unmarshal(file);
    }

    public static ConfigBuilder newConfig() {
        return new ConfigBuilder();
    }

    public static ConfigBuilder loadConfig(File file) throws Exception {
        return new ConfigBuilder(file);
    }

    public void saveConfig(OutputStream output) throws Exception {
        Marshaller m = getJAXBContext().createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, DOZER_SCHEMA_LOC);
        m.marshal(mapConfig, output);
    }

    public Mappings getMappings() {
        return mapConfig;
    }

    // Adds a <class-a> and <class-b> mapping definition to the dozer config.
    // If multiple fields within a class are being mapped, this should only
    // be called once.
    public void addClassMapping(String fromClass, String toClass) {
        Mapping map = new Mapping();
        org.jboss.mapper.dozer.config.Class classA = new org.jboss.mapper.dozer.config.Class();
        org.jboss.mapper.dozer.config.Class classB = new org.jboss.mapper.dozer.config.Class();
        classA.setContent(fromClass);
        classB.setContent(toClass);
        map.setClassA(classA);
        map.setClassB(classB);
        mapConfig.getMapping().add(map);
    }

    public void map(Model source, Model target) {
        // Only add a class mapping if one has not been created already
        if (requiresClassMapping(source.getParent(), target.getParent())) {
            String sourceType = source.getParent().isCollection() ? ModelBuilder
                    .getListType(source.getParent().getType()) : source
                    .getParent().getType();
            String targetType = target.getParent().isCollection() ? ModelBuilder
                    .getListType(target.getParent().getType()) : target
                    .getParent().getType();
            addClassMapping(sourceType, targetType);
        }

        // Add field mapping details for the source and target
        addFieldMapping(source, target);
    }

    boolean requiresClassMapping(Model source, Model target) {
        // If a class mapping already exists, then no need to add a new one
        if (getClassMapping(source) != null || getClassMapping(target) != null) {
            return false;
        }

        return true;
    }

    // Return an existing mapping which includes the specified node's parent
    // as a source or target. This basically fetches the mapping definition
    // under which a field mapping can be defined.
    Mapping getClassMapping(Model model) {
        Mapping mapping = null;
        String type = model.isCollection() 
                ? ModelBuilder.getListType(model.getType()) : model.getType();

        for (Mapping m : mapConfig.getMapping()) {
            if ((m.getClassA().getContent().equals(type) 
                    || m.getClassB().getContent().equals(type))) {
                mapping = m;
                break;
            }
        }
        return mapping;
    }

    // Add a field mapping to the dozer config.
    void addFieldMapping(Model source, Model target) {
        boolean sourceClassMapping = getClassMapping(source.getParent()) != null;
        boolean targetClassMapping = getClassMapping(target.getParent()) != null;

        Mapping mapping = sourceClassMapping ? getClassMapping(source
                .getParent()) : getClassMapping(target.getParent());

        Field field = new Field();
        field.setA(createField(source, !sourceClassMapping));
        field.setB(createField(target, !targetClassMapping));
        mapping.getFieldOrFieldExclude().add(field);
    }

    static FieldDefinition createField(Model model, boolean includeParentPrefix) {
        FieldDefinition fd = new FieldDefinition();
        String name = model.getName();
        if (includeParentPrefix) {
            fd.setContent(model.getParent().getName() + "." + name);
        } else {
            fd.setContent(name);
        }
        return fd;
    }

    private synchronized JAXBContext getJAXBContext() {
        if (jaxbCtx == null) {
            try {
                jaxbCtx = JAXBContext
                        .newInstance("org.jboss.mapper.model.dozer.config");
            } catch (JAXBException jaxbEx) {
                throw new RuntimeException(jaxbEx);
            }
        }
        return jaxbCtx;
    }
}
