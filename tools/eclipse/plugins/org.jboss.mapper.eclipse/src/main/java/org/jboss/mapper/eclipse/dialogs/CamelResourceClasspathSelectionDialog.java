/******************************************************************************* 
 * Copyright (c) 2015 Red Hat, Inc. 
 *  All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
package org.jboss.mapper.eclipse.dialogs;

import java.io.File;
import java.util.Collections;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaModel;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.FilteredResourcesSelectionDialog;
import org.jboss.mapper.camel.CamelConfigBuilder;

/**
 * Allows user to select a Camel resource on the project's classpath.
 * TODO - work in progress - the CamelResourceFilter implementation is VERY resource intensive
 * has to be a better way to introspect the file to see whether it's a valid camel route file or not
 */
public class CamelResourceClasspathSelectionDialog extends FilteredResourcesSelectionDialog {

    private Set<String> _fileExtensions;
    private IJavaModel _fJavaModel;

    /**
     * Create a new ClasspathResourceSelectionDialog.
     * 
     * @param parentShell the parent shell
     * @param container the root container
     */
    public CamelResourceClasspathSelectionDialog(Shell parentShell, IContainer container, String title) {
        this(parentShell, container, Collections.<String> emptySet(), title);
    }

    /**
     * Create a new ClasspathResourceSelectionDialog.
     * 
     * @param parentShell the parent shell
     * @param container the root container
     * @param fileExtension the type of files to display; may be null
     */
    public CamelResourceClasspathSelectionDialog(Shell parentShell, IContainer container, 
            String fileExtension, String title) {
        this(parentShell, container, fileExtension == null ? Collections.<String> emptySet() : Collections
                .singleton(fileExtension), title);
    }

    /**
     * Create a new ClasspathResourceSelectionDialog.
     * 
     * @param parentShell the parent shell
     * @param container the root container
     * @param fileExtensions the types of files to display; may be null
     */
    public CamelResourceClasspathSelectionDialog(Shell parentShell, IContainer container, 
            Set<String> fileExtensions, String title) {
        super(parentShell, false, container, IResource.FILE);
        _fJavaModel = JavaCore.create(ResourcesPlugin.getWorkspace().getRoot());
        _fileExtensions = fileExtensions == null ? Collections.<String> emptySet() : fileExtensions;
        setTitle(title);
    }

    @Override
    protected ItemsFilter createFilter() {
        return new CamelResourceFilter();
    }

    private class CamelResourceFilter extends ResourceFilter {

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.ui.dialogs.FilteredResourcesSelectionDialog.
         * ResourceFilter#matchItem(java.lang.Object)
         */
        @Override
        public boolean matchItem(Object item) {
            IResource resource = (IResource) item;
            return super.matchItem(item)
                    && (_fileExtensions == null || _fileExtensions.isEmpty() || _fileExtensions.contains(resource
                            .getFullPath().getFileExtension())) && select(resource);
        }
        
        private boolean fileIsSupportedCamel(Object item) {
            try {
                IResource resource = (IResource) item;
                File testFile = new File( resource.getLocationURI() );
                System.out.println("Testing " + testFile.toString());
                if (testFile.exists()) {
                    CamelConfigBuilder.loadConfig( testFile );
                    return true;
                }
            } catch ( final Exception e ) {
                // ignore
            }
            return false;
        }

        private boolean isParentOnClassPath(IJavaProject javaProject, IResource resource) {
            boolean flag = false;
            while (!flag && resource.getParent() != null) {
                flag = javaProject.isOnClasspath(resource);
                if (!flag) {
                    resource = resource.getParent();
                } else {
                    return flag;
                }
            }
            return flag;
        }

        /**
         * This is the orignal <code>select</code> method. Since
         * <code>GotoResourceDialog</code> needs to extend
         * <code>FilteredResourcesSelectionDialog</code> result of this method
         * must be combined with the <code>matchItem</code> method from super
         * class (<code>ResourceFilter</code>).
         * 
         * @param resource A resource
         * @return <code>true</code> if item matches against given conditions
         *         <code>false</code> otherwise
         */
        private boolean select(IResource resource) {
            IProject project = resource.getProject();
            IJavaProject javaProject = JavaCore.create(project);
            try {
                boolean isSupported = fileIsSupportedCamel(resource);
                return (javaProject != null && isParentOnClassPath(javaProject, resource) && isSupported)
                        || (project.getNature(JavaCore.NATURE_ID) != null && _fJavaModel.contains(resource) && isSupported);
            } catch (CoreException e) {
                return false;
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see org.eclipse.ui.dialogs.FilteredResourcesSelectionDialog.
         * ResourceFilter
         * #equalsFilter(org.eclipse.ui.dialogs.FilteredItemsSelectionDialog
         * .ItemsFilter)
         */
        @Override
        public boolean equalsFilter(ItemsFilter filter) {
            return filter instanceof CamelResourceFilter && super.equalsFilter(filter);
        }
    }

}
