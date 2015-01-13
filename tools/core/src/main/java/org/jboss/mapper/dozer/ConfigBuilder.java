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
import org.jboss.mapper.dozer.config.ObjectFactory;
import org.jboss.mapper.model.Model;
import org.jboss.mapper.model.ModelBuilder;

public class ConfigBuilder {

    public static final String DEFAULT_DOZER_CONFIG = "dozerBeanMapping.xml";
    
    private static final String DOZER_SCHEMA_LOC = 
            "http://dozer.sourceforge.net http://dozer.sourceforge.net/schema/beanmapping.xsd";

    // JAXB classes for Dozer config model
    private JAXBContext jaxbCtx;

    private final Mappings mapConfig;

    private ConfigBuilder() {
        this(new Mappings());
    }

    private ConfigBuilder(final File file) throws Exception {
        mapConfig = (Mappings)getJAXBContext().createUnmarshaller().unmarshal(file);
    }

    private ConfigBuilder(final Mappings mapConfig) {
        this.mapConfig = mapConfig;
    }
    
    public static ConfigBuilder loadConfig(final File file) throws Exception {
        return new ConfigBuilder(file);
    }

    public static ConfigBuilder newConfig() {
        return new ConfigBuilder();
    }

    // Adds a <class-a> and <class-b> mapping definition to the dozer config.
    // If multiple fields within a class are being mapped, this should only
    // be called once.
    public void addClassMapping(final String fromClass, final String toClass) {
        final Mapping map = new Mapping();
        final org.jboss.mapper.dozer.config.Class classA = new org.jboss.mapper.dozer.config.Class();
        final org.jboss.mapper.dozer.config.Class classB = new org.jboss.mapper.dozer.config.Class();
        classA.setContent(fromClass);
        classB.setContent(toClass);
        map.setClassA(classA);
        map.setClassB(classB);
        mapConfig.getMapping().add(map);
    }
    
    static FieldDefinition createField(final Model model, final String rootType) {
        final FieldDefinition fd = new FieldDefinition();
        StringBuilder name = new StringBuilder(model.getName());
        for (Model parent = model.getParent(); parent != null; parent = parent.getParent()) {
            if (parent.getType().equals(rootType)) {
                break;
            }
            name.insert(0, parent.getName() + ".");
        }
        fd.setContent(name.toString());
        return fd;
    }

    
    // Add a field mapping to the dozer config.
    void addFieldMapping(final Model source, final Model target) {
        Mapping mapping;
        if (getClassMapping(source.getParent()) != null) {
            mapping = getClassMapping(source.getParent());
        } else {
            mapping = getClassMapping(target.getParent());
        }
        

        final Field field = new Field();
        field.setA(createField(source, mapping.getClassA().getContent()));
        field.setB(createField(target, mapping.getClassB().getContent()));
        mapping.getFieldOrFieldExclude().add(field);
    }

    // Return an existing mapping which includes the specified node's parent
    // as a source or target. This basically fetches the mapping definition
    // under which a field mapping can be defined.
    Mapping getClassMapping(final Model model) {
        Mapping mapping = null;
        final String type = model.isCollection()
                ? ModelBuilder.getListType(model.getType()) : model.getType();

        for (final Mapping m : mapConfig.getMapping()) {
            if ((m.getClassA().getContent().equals(type)
            || m.getClassB().getContent().equals(type))) {
                mapping = m;
                break;
            }
        }
        return mapping;
    }

    private synchronized JAXBContext getJAXBContext() {
        if (jaxbCtx == null) {
            try {
                jaxbCtx = JAXBContext.newInstance(ObjectFactory.class);
            } catch (final JAXBException jaxbEx) {
                throw new RuntimeException(jaxbEx);
            }
        }
        return jaxbCtx;
    }

    public Mappings getMappings() {
        return mapConfig;
    }

    public void map(final Model source, final Model target) {
        // Only add a class mapping if one has not been created already
        if (requiresClassMapping(source.getParent(), target.getParent())) {
            final String sourceType = source.getParent().isCollection() ? ModelBuilder
                    .getListType(source.getParent().getType()) : source
                    .getParent().getType();
            final String targetType = target.getParent().isCollection() ? ModelBuilder
                    .getListType(target.getParent().getType()) : target
                    .getParent().getType();
            addClassMapping(sourceType, targetType);
        }

        // Add field mapping details for the source and target
        addFieldMapping(source, target);
    }

    boolean requiresClassMapping(final Model source, final Model target) {
        // If a class mapping already exists, then no need to add a new one
        if (getClassMapping(source) != null || getClassMapping(target) != null) {
            return false;
        }

        return true;
    }

    public void saveConfig(final OutputStream output) throws Exception {
        final Marshaller m = getJAXBContext().createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, DOZER_SCHEMA_LOC);
        m.marshal(mapConfig, output);
    }
}
