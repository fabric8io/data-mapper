/*************************************************************************************
 * Copyright (c) 2014 Red Hat, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     JBoss by Red Hat - Initial implementation.
 ************************************************************************************/
package org.jboss.mapper.eclipse.util;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

public final class JavaUtil {
    
    /**
     * Creates a ClassLoader using the project's build path.
     * 
     * @param javaProject the Java project.
     * @param parentClassLoader the parent class loader, may be null.
     * 
     * @return a new ClassLoader based on the project's build path.
     * 
     * @throws Exception if something goes wrong.
     */
    public static ClassLoader getProjectClassLoader(IJavaProject javaProject, ClassLoader parentClassLoader)
            throws Exception {
        IProject project = javaProject.getProject();
        IWorkspaceRoot root = project.getWorkspace().getRoot();
        List<URL> urls = new ArrayList<URL>();
        urls.add(new File(project.getLocation() + "/" + javaProject.getOutputLocation().removeFirstSegments(1) + "/") //$NON-NLS-1$ //$NON-NLS-2$
                .toURI().toURL());
        for (IClasspathEntry classpathEntry : javaProject.getResolvedClasspath(true)) {
            if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_PROJECT) {
                IPath projectPath = classpathEntry.getPath();
                IProject otherProject = root.getProject(projectPath.segment(0));
                IJavaProject otherJavaProject = JavaCore.create(otherProject);
                urls.add(new File(otherProject.getLocation() + "/" //$NON-NLS-1$
                        + otherJavaProject.getOutputLocation().removeFirstSegments(1) + "/").toURI().toURL()); //$NON-NLS-1$
            } else if (classpathEntry.getEntryKind() == IClasspathEntry.CPE_LIBRARY) {
                urls.add(new File(classpathEntry.getPath().toOSString()).toURI().toURL());
            }
        }
        if (parentClassLoader == null) {
            return new URLClassLoader(urls.toArray(new URL[urls.size()]));
        } else {
            return new URLClassLoader(urls.toArray(new URL[urls.size()]), parentClassLoader);
        }
    }

    /**
     * Create a new JavaUtil.
     */
    private JavaUtil() {
    }

}
