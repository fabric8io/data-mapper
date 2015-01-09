/*******************************************************************************
 * Copyright (c) 2014 Red Hat, Inc.
 *  All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Red Hat, Inc. - initial API and implementation
 ******************************************************************************/

package org.jboss.mapper.eclipse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.jboss.mapper.dozer.ConfigBuilder;
import org.jboss.mapper.dozer.config.Field;
import org.jboss.mapper.dozer.config.Mapping;
import org.jboss.mapper.eclipse.DataBrowser.Listener;
import org.jboss.mapper.eclipse.util.JavaUtil;
import org.jboss.mapper.model.Model;
import org.jboss.mapper.model.ModelBuilder;

/**
 *
 */
public class DataMapper extends Composite {

    final IFile configFile;
    ConfigBuilder configBuilder;
    URLClassLoader loader;
    Model sourceModel = null;
    Model targetModel = null;
    TableViewer viewer;

    DataMapper( final Composite parent,
                final IFile configFile ) {
        super( parent, SWT.NONE );
        this.configFile = configFile;
        final File file = new File( configFile.getLocationURI() );

        try {
            configBuilder = ConfigBuilder.loadConfig( file );
            IJavaProject javaProject = JavaCore.create(configFile.getProject());
            loader = (URLClassLoader) JavaUtil.getProjectClassLoader(javaProject, getClass().getClassLoader());

            final List< Mapping > mappings = configBuilder.getMappings().getMapping();
            if ( !mappings.isEmpty() ) {
                final Mapping mainMapping = mappings.get( 0 );
                sourceModel = ModelBuilder.fromJavaClass( loader.loadClass( mainMapping.getClassA().getContent() ) );
                targetModel = ModelBuilder.fromJavaClass( loader.loadClass( mainMapping.getClassB().getContent() ) );
            }

            this.setLayout(new FillLayout());
            SashForm form = new SashForm(this, SWT.VERTICAL);
            
            // Change the color used to paint the sashes
            form.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
            form.SASH_WIDTH = 5;
            
            final ScrolledComposite sc = new ScrolledComposite(form, SWT.V_SCROLL);
            sc.setExpandHorizontal(true);
            sc.setExpandVertical(true);
            
            final Composite child1 = new Composite(sc, SWT.NONE);
            sc.setContent(child1);
            child1.setLayout( GridLayoutFactory.swtDefaults().spacing( 0, 5 ).numColumns( 3 ).create() );
            
            viewer = new TableViewer( child1 );
            final Table table = viewer.getTable();
            table.setLayoutData( GridDataFactory.fillDefaults().span( 3, 1 ).grab( true, true ).create() );
            table.setHeaderVisible( true );
            
            TableLayout layout=new TableLayout();
            table.setLayout(layout);
            
            final TableViewerColumn sourceColumn = new TableViewerColumn( viewer, SWT.NONE );
            sourceColumn.getColumn().setText( "Source Item" );
            sourceColumn.getColumn().setAlignment( SWT.RIGHT );
            sourceColumn.setLabelProvider( new ColumnLabelProvider() {

                @Override
                public String getText( final Object element ) {
                    return super.getText( ( ( Field ) element ).getA().getContent() );
                }
            } );
            final TableViewerColumn operationColumn = new TableViewerColumn( viewer, SWT.NONE );
            operationColumn.getColumn().setText( "Operation" );
            operationColumn.getColumn().setAlignment( SWT.CENTER );
            operationColumn.setLabelProvider( new ColumnLabelProvider() {

                @Override
                public String getText( final Object element ) {
                    return "=>";
                }
            } );
            final TableViewerColumn targetColumn = new TableViewerColumn( viewer, SWT.NONE );
            targetColumn.getColumn().setText( "Target Item" );
            targetColumn.getColumn().setAlignment( SWT.LEFT );
            targetColumn.setLabelProvider( new ColumnLabelProvider() {

                @Override
                public String getText( final Object element ) {
                    return super.getText( ( ( Field ) element ).getB().getContent() );
                }
            } );
            viewer.setContentProvider( new IStructuredContentProvider() {

                @Override
                public void dispose() {}

                @Override
                public Object[] getElements( final Object inputElement ) {
                    final List< Object > fields = new ArrayList<>();
                    for ( final Mapping mapping : mappings ) {
                        for ( final Object field : mapping.getFieldOrFieldExclude() ) {
                            fields.add( field );
                            viewer.setData(field.toString(), mapping);
                        }
                    }
                    return fields.toArray();
                }

                @Override
                public void inputChanged( final Viewer viewer,
                                          final Object oldInput,
                                          final Object newInput ) {}
            } );
            viewer.setInput( mappings );

            operationColumn.getColumn().pack();

            table.addControlListener( new ControlAdapter() {

                @Override
                public void controlResized( final ControlEvent event ) {
                    final int width = ( table.getSize().x - operationColumn.getColumn().getWidth() ) / 2 - 1;
                    sourceColumn.getColumn().setWidth( width );
                    targetColumn.getColumn().setWidth( width );
                }
            } );

            final ScrolledComposite sc2 = new ScrolledComposite(form, SWT.V_SCROLL | SWT.H_SCROLL);
            sc2.setExpandHorizontal(true);
            sc2.setExpandVertical(true);

            final Composite child2 = new Composite(sc2, SWT.NONE);
            child2.setLayout( GridLayoutFactory.swtDefaults().spacing( 0, 5 ).numColumns( 3 ).create() );
            sc2.setContent(child2);

            final Text text = new Text( child2, SWT.MULTI | SWT.WRAP );
            text.setLayoutData( GridDataFactory.fillDefaults().grab( true, false ).span( 3, 1 ).create() );
            text.setForeground( getDisplay().getSystemColor( SWT.COLOR_BLUE ) );
            updateBrowserText( text );
            text.setBackground( getBackground() );
            text.pack();

            final DataBrowser sourceBrowser = new DataBrowser( child2, this, "Source", sourceModel, new Listener() {

                @Override
                public Model modelSelected( final String className ) {
                    try {
                        sourceModel = ModelBuilder.fromJavaClass( loader.loadClass( className ) );
                        updateMappings();
                        updateBrowserText( text );
                    } catch ( final ClassNotFoundException e ) {
                        Activator.error( getShell(), e );
                    }
                    return sourceModel;
                }
            } );
            sourceBrowser.setLayoutData( GridDataFactory.fillDefaults().grab( true, true ).create() );
            final Transfer[] xfers = new Transfer[] { LocalSelectionTransfer.getTransfer() };
            sourceBrowser.viewer.addDragSupport( DND.DROP_MOVE, xfers, new DragSourceAdapter() {

                @Override
                public void dragSetData( final DragSourceEvent event ) {
                    if ( LocalSelectionTransfer.getTransfer().isSupportedType( event.dataType ) ) {
                        LocalSelectionTransfer.getTransfer().setSelection( sourceBrowser.viewer.getSelection() );
                    }
                }
            } );

            final Label label = new Label( child2, SWT.NONE );
            label.setText( "=>" );

            final DataBrowser targetBrowser = new DataBrowser( child2, this, "Target", targetModel, new Listener() {

                @Override
                public Model modelSelected( final String className ) {
                    try {
                        targetModel = ModelBuilder.fromJavaClass( loader.loadClass( className ) );
                        updateMappings();
                        updateBrowserText( text );
                    } catch ( final ClassNotFoundException e ) {
                        Activator.error( getShell(), e );
                    }
                    return targetModel;
                }
            } );
            targetBrowser.setLayoutData( GridDataFactory.fillDefaults().grab( true, true ).create() );
            targetBrowser.viewer.addDropSupport( DND.DROP_MOVE, xfers, new ViewerDropAdapter( targetBrowser.viewer ) {

                @Override
                public boolean performDrop( final Object data ) {
                    final Model sourceModel = ( Model ) ( ( IStructuredSelection ) LocalSelectionTransfer.getTransfer().getSelection() ).getFirstElement();
                    final Model targetModel = ( Model ) getCurrentTarget();
                    configBuilder.map( sourceModel, targetModel );
                    try ( FileOutputStream stream = new FileOutputStream( file ) ) {
                        configBuilder.saveConfig( stream );
                        configFile.getProject().refreshLocal( IResource.DEPTH_INFINITE, null );
                        viewer.refresh();
                    } catch ( final Exception e ) {
                        Activator.error( getShell(), e );
                    }
                    return true;
                }

                @Override
                public boolean validateDrop( final Object target,
                                             final int operation,
                                             final TransferData transferType ) {
                    return true;
                }
            } );

            form.setWeights(new int[] {25,75});
            
            sc.setMinSize(child1.computeSize(SWT.DEFAULT, SWT.DEFAULT));
            sc2.setMinSize(child2.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        } catch ( final Exception e ) {
            Activator.error( getShell(), e );
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see org.eclipse.swt.widgets.Widget#dispose()
     */
    @Override
    public void dispose() {
        super.dispose();
        if ( loader != null ) try {
            loader.close();
        } catch ( final IOException e ) {
            MessageDialog.openError( getShell(), "Error", e.getMessage() );
            Activator.plugin().getLog().log( new Status( Status.ERROR,
                                                         Activator.plugin().getBundle().getSymbolicName(),
                                                         e.getMessage() ) );
        }
    }

    void updateBrowserText( final Text text ) {
        if ( sourceModel == null && targetModel == null ) text.setText( "Select the source and target models below." );
        else if ( sourceModel == null ) text.setText( "Select the source model below." );
        else if ( targetModel == null ) text.setText( "Select the target model below." );
        else text.setText( "Create a new mapping in the list of operations above by dragging an item below from source " +
                           sourceModel.getName() + " to target " + targetModel.getName() );
    }

    void updateMappings() {
        if ( sourceModel == null || targetModel == null ) return;
        final List< Mapping > mappings = configBuilder.getMappings().getMapping();
        mappings.clear();
        configBuilder.addClassMapping( sourceModel.getType(), targetModel.getType() );
        try {
            configBuilder.saveConfig( new FileOutputStream( new File( configFile.getLocationURI() ) ) );
            configFile.getProject().refreshLocal( IResource.DEPTH_INFINITE, null );
            viewer.refresh();
        } catch ( final Exception e ) {
            Activator.error( getShell(), e );
        }
    }

    /**
     * @param field
     * @return <code>true</code> if the supplied field was successfully removed
     */
    public boolean deleteFieldMapping(Field field) {
        Mapping mapping = (Mapping) viewer.getData(field.toString());
        boolean removed = mapping.getFieldOrFieldExclude().remove(field);
        if (removed) {
            try {
                configBuilder.saveConfig(
                        new FileOutputStream(
                                new File( configFile.getLocationURI())));
                configFile.getProject()
                    .refreshLocal( IResource.DEPTH_INFINITE, null );
                viewer.refresh();
                return true;
            } catch ( final Exception e ) {
                Activator.error( getShell(), e );
            }
        }
        return false;
    }
}
