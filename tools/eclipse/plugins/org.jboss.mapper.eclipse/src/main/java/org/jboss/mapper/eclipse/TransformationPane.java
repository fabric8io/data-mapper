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
import java.io.IOException;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabFolder2Adapter;
import org.eclipse.swt.custom.CTabFolderEvent;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ResourceListSelectionDialog;
import org.jboss.mapper.Literal;
import org.jboss.mapper.dozer.ConfigBuilder;
import org.jboss.mapper.dozer.config.Mapping;
import org.jboss.mapper.eclipse.util.JavaUtil;
import org.jboss.mapper.model.Model;
import org.jboss.mapper.model.ModelBuilder;

class TransformationPane extends Composite {

    private static void findClasses( final IFolder folder,
                                     final List< IResource > classes ) throws CoreException {
        for ( final IResource resource : folder.members() ) {
            if ( resource instanceof IFolder ) findClasses( ( IFolder ) resource, classes );
            else if ( resource.getName().endsWith( ".class" ) ) classes.add( resource );
        }
    }

    static String selectModel( final Shell shell,
                               final IProject project,
                               final Model existingModel,
                               final String modelType ) {
        final IFolder classesFolder = project.getFolder( "target/classes" );
        final List< IResource > classes = new ArrayList<>();
        try {
            findClasses( classesFolder, classes );
            final ResourceListSelectionDialog dlg =
                new ResourceListSelectionDialog( shell, classes.toArray( new IResource[ classes.size() ] ) ) {

                    @Override
                    protected Control createDialogArea( final Composite parent ) {
                        final Composite dlgArea = ( Composite ) super.createDialogArea( parent );
                        for ( final Control child : dlgArea.getChildren() ) {
                            if ( child instanceof Text ) {
                                ( ( Text ) child ).setText( existingModel == null ? "*" : existingModel.getName() );
                                break;
                            }
                        }
                        return dlgArea;
                    }
                };
            dlg.setTitle( "Select " + modelType );
            if ( dlg.open() == Window.OK ) {
                final IFile file = ( IFile ) dlg.getResult()[ 0 ];
                String name = file.getFullPath().makeRelativeTo( classesFolder.getFullPath() ).toString().replace( '/', '.' );
                name = name.substring( 0, name.length() - ".class".length() );
                return name;
            }
        } catch ( final Exception e ) {
            Activator.error( shell, e );
        }
        return null;
    }

    final IFile configFile;
    ConfigBuilder configBuilder;
    URLClassLoader loader;
    Model sourceModel, targetModel;
    TransformationViewer xformViewer;
    Text helpText;

    TransformationPane( final Composite parent,
                final IFile configFile ) {
        super( parent, SWT.NONE );
        this.configFile = configFile;

        try {
            configBuilder = ConfigBuilder.loadConfig( new File( configFile.getLocationURI() ) );
            final IJavaProject javaProject = JavaCore.create( configFile.getProject() );
            loader = ( URLClassLoader ) JavaUtil.getProjectClassLoader( javaProject, getClass().getClassLoader() );

            final List< Mapping > mappings = configBuilder.getDozerConfig().getMapping();
            if ( !mappings.isEmpty() ) {
                final Mapping mainMapping = mappings.get( 0 );
                sourceModel = ModelBuilder.fromJavaClass( loader.loadClass( mainMapping.getClassA().getContent() ) );
                targetModel = ModelBuilder.fromJavaClass( loader.loadClass( mainMapping.getClassB().getContent() ) );
            }

            setLayout( new FillLayout() );
            SashForm splitter = new SashForm( this, SWT.VERTICAL );

            // Create transformation viewer
            xformViewer = new TransformationViewer( splitter, mappings, configFile, configBuilder );

            final Composite mapper = new Composite( splitter, SWT.NONE );
            mapper.setBackground( getBackground() );
            mapper.setLayout( GridLayoutFactory.swtDefaults().margins( 0, 5 ).numColumns( 3 ).create() );

            // Create help text
            helpText = new Text( mapper, SWT.MULTI | SWT.WRAP );
            helpText.setLayoutData( GridDataFactory.fillDefaults().grab( true, false ).span( 3, 1 ).create() );
            helpText.setForeground( getDisplay().getSystemColor( SWT.COLOR_BLUE ) );
            helpText.setBackground( getBackground() );
            updateBrowserText();

            // Create source viewer
            final CTabFolder sourceTabFolder = createTabFolder( mapper, new Handler() {

                @Override
                public void configureDragAndDrop( final ModelViewer viewer ) {
                    viewer.treeViewer.addDragSupport( DND.DROP_MOVE,
                                                      new Transfer[] { LocalSelectionTransfer.getTransfer() },
                                                      new DragSourceAdapter() {

                        @Override
                        public void dragSetData( final DragSourceEvent event ) {
                            if ( LocalSelectionTransfer.getTransfer().isSupportedType( event.dataType ) )
                                LocalSelectionTransfer.getTransfer().setSelection( viewer.treeViewer.getSelection() );
                        }
                    } );
                }

                @Override
                public Model model() {
                    return sourceModel;
                }

                @Override
                public void setModel( Model model ) {
                    sourceModel = model;
                }

                @Override
                public String type() {
                    return "Source";
                }
            } );

            // Create literals tab
            final CTabItem literalsTab = new CTabItem( sourceTabFolder, SWT.NONE );
            literalsTab.setText( "Literals" );
            final LiteralsViewer literalsViewer = new LiteralsViewer( sourceTabFolder, configBuilder );
            literalsTab.setControl( literalsViewer );
            literalsTab.setImage( Activator.imageDescriptor( "literal16.gif" ).createImage() );

            final Label label = new Label( mapper, SWT.NONE );
            label.setText( "=>" );

            // Create target viewer
            createTabFolder( mapper, new Handler() {

                @Override
                public void configureDragAndDrop( final ModelViewer viewer ) {
                    viewer.treeViewer.addDropSupport( DND.DROP_MOVE,
                                                      new Transfer[] { LocalSelectionTransfer.getTransfer() },
                                                      new ViewerDropAdapter( viewer.treeViewer ) {

                        @Override
                        public boolean performDrop( final Object data ) {
                            Object source =
                                ( ( IStructuredSelection ) LocalSelectionTransfer.getTransfer().getSelection() ).getFirstElement();
                            if (source instanceof Model) xformViewer.map( (Model) source, ( Model ) getCurrentTarget() );
                            else xformViewer.map( new Literal( source.toString() ), ( Model ) getCurrentTarget() );
                            return true;
                        }

                        @Override
                        public boolean validateDrop( final Object target,
                                                     final int operation,
                                                     final TransferData transferType ) {
                            return true;
                        }
                    } );
                }

                @Override
                public Model model() {
                    return targetModel;
                }

                @Override
                public void setModel( Model model ) {
                    targetModel = model;
                }

                @Override
                public String type() {
                    return "Target";
                }
            } );

            splitter.setBackground( getDisplay().getSystemColor( SWT.COLOR_DARK_GRAY ) );
            splitter.SASH_WIDTH = 5;
            splitter.setWeights( new int[] { 25, 75 } );
        } catch ( final Exception e ) {
            Activator.error( getShell(), e );
        }
    }

    void createTab( CTabFolder tabFolder,
                    Handler handler ) {
        final CTabItem tab = new CTabItem( tabFolder, SWT.NONE, 0 );
        tab.setText( handler.type() + ": " + handler.model().getName() );
        tab.setShowClose( true );
        final ModelViewer viewer = new ModelViewer( tabFolder, handler.model() );
        tab.setControl( viewer );
        viewer.setLayoutData( GridDataFactory.fillDefaults().grab( true, true ).create() );
        handler.configureDragAndDrop( viewer );
        viewer.setInput( handler.model() );
        viewer.layout();
        tabFolder.setSelection( tab );
    }

    private CTabFolder createTabFolder( Composite mapper, final Handler handler ) {
        final CTabFolder tabFolder = new CTabFolder( mapper, SWT.BORDER );
        tabFolder.setLayoutData( GridDataFactory.fillDefaults().grab( true, true ).create() );
        final ToolBar toolBar = new ToolBar( tabFolder, SWT.RIGHT );
        tabFolder.setTopRight( toolBar );
        final ToolItem addSourceButton = new ToolItem( toolBar, SWT.NONE );
        addSourceButton.setImage( PlatformUI.getWorkbench().getSharedImages().getImage( ISharedImages.IMG_OBJ_ADD ) );
        addSourceButton.setToolTipText( "Set transformation " + handler.type().toLowerCase() );
        addSourceButton.addSelectionListener( new SelectionAdapter() {

            @Override
            public void widgetSelected( SelectionEvent event ) {
                final String name = selectModel( getShell(), configFile.getProject(), handler.model(), handler.type() );
                if ( name == null ) return;
                try {
                    handler.setModel( ModelBuilder.fromJavaClass( loader.loadClass( name ) ) );
                    updateMappings();
                    createTab( tabFolder, handler );
                    toolBar.setVisible( false );
                    updateBrowserText();
                } catch ( final ClassNotFoundException e ) {
                    Activator.error( getShell(), e );
                }
            }
        } );
        tabFolder.addCTabFolder2Listener( new CTabFolder2Adapter() {

            @Override
            public void close( CTabFolderEvent event ) {
                toolBar.setVisible( true );
                handler.setModel( null );
                updateBrowserText();
            }
        } );
        if ( handler.model() != null ) {
            toolBar.setVisible( false );
            createTab( tabFolder, handler );
        }
        tabFolder.setBackground( getDisplay().getSystemColor( SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT ) );
        return tabFolder;
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
            Activator.error( getShell(), e );
        }
    }

    void updateBrowserText() {
        if ( sourceModel == null && targetModel == null ) helpText.setText( "Select the source and target models below." );
        else if ( sourceModel == null ) helpText.setText( "Select the source model below." );
        else if ( targetModel == null ) helpText.setText( "Select the target model below." );
        else helpText.setText( "Create a new mapping in the list of operations above by dragging an item below from source " +
                               sourceModel.getName() + " to target " + targetModel.getName() );
    }

    void updateMappings() {
        if ( sourceModel == null || targetModel == null ) return;
        configBuilder.clearMappings();
        configBuilder.addClassMapping( sourceModel.getType(), targetModel.getType() );
        xformViewer.save();
    }

    interface Handler {

        void configureDragAndDrop( ModelViewer viewer );

        Model model();

        void setModel( Model model );

        String type();
    }
}
