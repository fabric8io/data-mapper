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
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import org.jboss.forge.addon.projects.Project;
import org.jboss.forge.addon.projects.ProjectFactory;
import org.jboss.forge.addon.projects.facets.ResourcesFacet;
import org.jboss.forge.addon.projects.ui.AbstractProjectCommand;
import org.jboss.forge.addon.resource.DirectoryResource;
import org.jboss.forge.addon.ui.context.UIContext;
import org.jboss.forge.addon.ui.input.InputComponent;
import org.jboss.forge.addon.ui.input.UICompleter;
import org.jboss.forge.addon.ui.util.Categories;
import org.jboss.forge.addon.ui.util.Metadata;
import org.jboss.mapper.dozer.ConfigBuilder;
import org.jboss.mapper.model.Model;
import org.jboss.mapper.model.ModelBuilder;

import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JPackage;

public abstract class AbstractMapperCommand extends AbstractProjectCommand {

    public static final String CAMEL_CTX_PATH = "META-INF/spring/camel-context.xml";
    private static final String MAPPER_CATEGORY = "Data Mapper";
    private static final String MAP_CONTEXT_ATTR = "mapper.context";

    @Inject
    protected ProjectFactory projectFactory;

    @Override
    protected boolean isProjectRequired() {
        return true;
    }

    @Override
    protected ProjectFactory getProjectFactory() {
        return projectFactory;
    }

    @Override
    public Metadata getMetadata(UIContext context) {
        return Metadata.forCommand(getClass())
                .description(getDescription())
                .name(getName())
                .category(Categories.create(MAPPER_CATEGORY));
    }

    protected MapperContext getMapperContext(Project project) {
        MapperContext mc = (MapperContext)project.getAttribute(MAP_CONTEXT_ATTR);
        if (mc == null) {
            mc = new MapperContext();
            project.setAttribute(MAP_CONTEXT_ATTR, mc);
        }
        return mc;
    }

    protected ConfigBuilder loadConfig(Project project) throws Exception {
        String configPath = getMapperContext(project).getDozerPath();
        ConfigBuilder config;
        if (getFile(project, configPath).exists()) {
            config = ConfigBuilder.loadConfig(getFile(project, configPath));
        } else {
            config = ConfigBuilder.newConfig();
        }
        getMapperContext(project).setConfig(config);
        return config;
    }

    protected void saveConfig(Project project) throws Exception {
        FileOutputStream fos = new FileOutputStream(
                getFile(project, getMapperContext(project).getDozerPath()));
        getMapperContext(project).getConfig().saveConfig(fos);
        fos.close();
    }

    protected File getFile(Project project, String path) {
        DirectoryResource root = project.getFacet(ResourcesFacet.class).getResourceDirectory();
        File rootDir = root.getUnderlyingResourceObject();
        // look in src/main/resources first
        File resourceFile = new File(rootDir, path);
        if (!resourceFile.exists()) {
            // try src/data
            File dataFile = new File(rootDir, "../../data/" + path);
            if (dataFile.exists()) {
                resourceFile = dataFile;
            }
        }
        return resourceFile;
    }

    protected Model loadModel(Project project, String className) throws Exception {
        URL[] urls = new URL[] {
                new File(project.getRootDirectory().getFullyQualifiedName() + "/target/classes").toURL() };

        URLClassLoader cl = null;
        Model model = null;
        try {
            cl = new URLClassLoader(urls);
            Class<?> clazz = cl.loadClass(className);
            model = ModelBuilder.fromJavaClass(clazz);
        } finally {
            if (cl != null) {
                cl.close();
            }
        }

        return model;

    }

    public abstract String getName();

    public abstract String getDescription();

    protected void addGeneratedTypes(Project project, JCodeModel codeModel) {
        List<String> types = getMapperContext(project).getGeneratedTypes();
        Iterator<JPackage> ip = codeModel.packages();
        while (ip.hasNext()) {
            Iterator<JDefinedClass> ic = ip.next().classes();
            while (ic.hasNext()) {
                types.add(ic.next().fullName());
            }
        }
    }

    protected UICompleter<String> getModelCompleter(Project project) {
        final List<String> options = new LinkedList<String>();
        options.addAll(getMapperContext(project).getGeneratedTypes());
        return new UICompleter<String>() {
            @Override
            public Iterable<String> getCompletionProposals(UIContext context,
                    InputComponent<?, String> input, String value) {
                return options;
            }
        };
    }
}
