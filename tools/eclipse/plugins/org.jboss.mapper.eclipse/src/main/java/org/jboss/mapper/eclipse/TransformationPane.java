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
import java.util.Stack;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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
import org.eclipse.swt.graphics.Image;
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
import org.jboss.mapper.MapperConfiguration;
import org.jboss.mapper.dozer.DozerMapperConfiguration;
import org.jboss.mapper.eclipse.internal.Handler;
import org.jboss.mapper.eclipse.util.JavaUtil;
import org.jboss.mapper.eclipse.util.PanelUIUtil;
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
    
    // TODO change to select source or class; Util.selectFile
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
    MapperConfiguration mapperConfig;
    URLClassLoader loader;
    Model sourceModel, targetModel;
    TransformationViewer xformViewer;
    Text helpText;
    ModelPane sourceModelPane;
    ModelPane targetModelPane;
    private ToolItem addSourceButton;
    private ToolItem addTargetButton;
    private final Image changeToolItemImage;
    private final Image addToolItemImage;
    private final Image literalsItemImage;
    
    TransformationPane( final Composite parent,
                        final IFile configFile ) {
        super( parent, SWT.NONE );
        this.configFile = configFile;
        changeToolItemImage = Activator.imageDescriptor( "change16.gif" ).createImage();
        addToolItemImage = PlatformUI.getWorkbench().getSharedImages().getImage( ISharedImages.IMG_OBJ_ADD );
        literalsItemImage = Activator.imageDescriptor( "literal16.gif" ).createImage();
        
        try {
            final IJavaProject javaProject = JavaCore.create( configFile.getProject() );
            loader = ( URLClassLoader ) JavaUtil.getProjectClassLoader( javaProject, getClass().getClassLoader() );
            mapperConfig = DozerMapperConfiguration.loadConfig( new File( configFile.getLocationURI() ), loader );
            
            sourceModel = mapperConfig.getSourceModel();
            targetModel = mapperConfig.getTargetModel();
            
            setLayout( new FillLayout() );
            final SashForm splitter = new SashForm( this, SWT.VERTICAL );
            
            // Create transformation viewer
            xformViewer = new TransformationViewer( splitter, configFile, mapperConfig );
            
            final Composite mapper = new Composite( splitter, SWT.NONE );
            mapper.setBackground( getBackground() );
            mapper.setLayout( GridLayoutFactory.swtDefaults().margins( 0, 5 ).numColumns( 3 ).create() );
            
            // Create help text
            helpText = new Text( mapper, SWT.MULTI | SWT.WRAP );
            helpText.setLayoutData( GridDataFactory.fillDefaults().grab( true, false ).span( 3, 1 ).create() );
            helpText.setForeground( getDisplay().getSystemColor( SWT.COLOR_BLUE ) );
            helpText.setBackground( getBackground() );
            sourceModelPane = new ModelPane();
            createSourceModelPane( sourceModelPane, mapper );
            
            // Create literals tab
            final CTabItem literalsTab = new CTabItem( sourceModelPane.modeltabFolder, SWT.NONE );
            literalsTab.setText( "Literals" );
            final LiteralsViewer literalsViewer = new LiteralsViewer( sourceModelPane.modeltabFolder, mapperConfig );
            literalsTab.setControl( literalsViewer );
            literalsTab.setImage( literalsItemImage );
            
            final Label label = new Label( mapper, SWT.NONE );
            label.setText( "=>" );
            
            targetModelPane = new ModelPane();
            createTargetModelPane( targetModelPane, mapper );
            
            splitter.setBackground( getDisplay().getSystemColor( SWT.COLOR_DARK_GRAY ) );
            splitter.SASH_WIDTH = 5;
            splitter.setWeights( new int[] { 25, 75 } );
            updateBrowserText();
        } catch ( final Exception e ) {
            Activator.error( getShell(), e );
        }
    }
    
    void createSourceModelPane( final ModelPane pane,
                                final Composite parent ) {
        pane.modeltabFolder = PanelUIUtil.createTabFolder( parent );
        pane.toolBar = PanelUIUtil.createTabFolderToolbar( pane.modeltabFolder );
        pane.handler = new Handler() {
            
            Stack< Model > history = new Stack<>();
            
            @Override
            public void configureDragAndDrop( final ModelViewer viewer ) {
                viewer.treeViewer.addDragSupport( DND.DROP_MOVE,
                                                  new Transfer[] { LocalSelectionTransfer.getTransfer() },
                                                  new DragSourceAdapter() {
                                                      
                                                      @Override
                                                      public void dragStart( final DragSourceEvent event ) {
                                                          LocalSelectionTransfer.getTransfer().setSelection( viewer.treeViewer.getSelection() );
                                                      }
                                                  } );
            }
            
            @Override
            public Stack< Model > getModelHistory() {
                return history;
            }
            
            @Override
            public Model model() {
                return sourceModel;
            }
            
            @Override
            public void setModel( final Model model ) {
                sourceModel = model;
                history.push( model );
            }
            
            @Override
            public String type() {
                return "Source";
            }
        };
        pane.tab = PanelUIUtil.createTab( pane.modeltabFolder, pane.handler );
        pane.modelViewer = PanelUIUtil.createModelViewer( pane.modeltabFolder, pane.tab, pane.handler );
        pane.handler.getModelHistory().push( sourceModel );
        
        addSourceButton = new ToolItem( pane.toolBar, SWT.NONE );
        addSourceButton.setImage( addToolItemImage );
        addSourceButton.setToolTipText( "Set transformation " + pane.handler.type().toLowerCase() );
        addSourceButton.addSelectionListener( new SelectionAdapter() {
            
            @Override
            public void widgetSelected( final SelectionEvent event ) {
                final String name = selectModel( getShell(), configFile.getProject(), pane.handler.model(), pane.handler.type() );
                if ( name == null ) return;
                try {
                    pane.handler.setModel( ModelBuilder.fromJavaClass( loader.loadClass( name ) ) );
                    xformViewer.sourcehistory = pane.handler.getModelHistory();
                    updateMappings();
                    pane.modelViewer = PanelUIUtil.createModelViewer( pane.modeltabFolder, pane.tab, pane.handler );
                    pane.modelViewer.setMapperConfiguration( mapperConfig );
                    pane.modelViewer.setModelType( pane.handler.type() );
                    updateBrowserText();
                } catch ( final ClassNotFoundException e ) {
                    Activator.error( getShell(), e );
                }
            }
        } );
        pane.modeltabFolder.addCTabFolder2Listener( new CTabFolder2Adapter() {
            
            @Override
            public void close( final CTabFolderEvent event ) {
                pane.toolBar.setVisible( true );
                pane.handler.setModel( null );
                updateBrowserText();
            }
        } );
        pane.toolBar.setVisible( true );
        if ( pane.handler.model() != null ) {
            pane.modelViewer = PanelUIUtil.createModelViewer( pane.modeltabFolder, pane.tab, pane.handler );
            pane.modelViewer.setMapperConfiguration( mapperConfig );
            pane.modelViewer.setModelType( pane.handler.type() );
            xformViewer.sourcehistory = pane.handler.getModelHistory();
        }
    }
    
    void createTargetModelPane( final ModelPane pane,
                                final Composite parent ) {
        pane.modeltabFolder = PanelUIUtil.createTabFolder( parent );
        pane.toolBar = PanelUIUtil.createTabFolderToolbar( pane.modeltabFolder );
        pane.handler = new Handler() {
            
            Stack< Model > history = new Stack<>();
            
            @Override
            public void configureDragAndDrop( final ModelViewer viewer ) {
                viewer.treeViewer.addDragSupport( DND.DROP_MOVE,
                                                  new Transfer[] { LocalSelectionTransfer.getTransfer() },
                                                  new DragSourceAdapter() {
                                                      
                                                      @Override
                                                      public void dragStart( final DragSourceEvent event ) {
                                                          LocalSelectionTransfer.getTransfer().setSelection( viewer.treeViewer.getSelection() );
                                                      }
                                                  } );
                viewer.treeViewer.addDropSupport( DND.DROP_MOVE,
                                                  new Transfer[] { LocalSelectionTransfer.getTransfer() },
                                                  new ViewerDropAdapter( viewer.treeViewer ) {
                                                      
                                                      @Override
                                                      public boolean performDrop( final Object data ) {
                                                          final Object source =
                                                              ( ( IStructuredSelection ) LocalSelectionTransfer.getTransfer().getSelection() ).getFirstElement();
                                                          if ( source instanceof Model ) xformViewer.map( ( Model ) source, ( Model ) getCurrentTarget() );
                                                          else xformViewer.map( new Literal( source.toString() ), ( Model ) getCurrentTarget() );
                                                          return true;
                                                      }
                                                      
                                                      @Override
                                                      public boolean validateDrop( final Object target,
                                                                                   final int operation,
                                                                                   final TransferData transferType ) {
                                                          return getCurrentLocation() == ViewerDropAdapter.LOCATION_ON;
                                                      }
                                                      
                                                  } );
            }
            
            @Override
            public Stack< Model > getModelHistory() {
                return history;
            }
            
            @Override
            public Model model() {
                return targetModel;
            }
            
            @Override
            public void setModel( final Model model ) {
                targetModel = model;
                history.push( model );
            }
            
            @Override
            public String type() {
                return "Target";
            }
        };
        pane.tab = PanelUIUtil.createTab( pane.modeltabFolder, pane.handler );
        pane.modelViewer = PanelUIUtil.createModelViewer( pane.modeltabFolder, pane.tab, pane.handler );
        pane.handler.getModelHistory().push( targetModel );
        
        addTargetButton = new ToolItem( pane.toolBar, SWT.NONE );
        addTargetButton.setImage( addToolItemImage );
        addTargetButton.setToolTipText( "Set transformation " + pane.handler.type().toLowerCase() );
        addTargetButton.addSelectionListener( new SelectionAdapter() {
            
            @Override
            public void widgetSelected( final SelectionEvent event ) {
                final String name = selectModel( getShell(), configFile.getProject(), pane.handler.model(), pane.handler.type() );
                if ( name == null ) return;
                try {
                    pane.handler.setModel( ModelBuilder.fromJavaClass( loader.loadClass( name ) ) );
                    xformViewer.targethistory = pane.handler.getModelHistory();
                    updateMappings();
                    pane.modelViewer = PanelUIUtil.createModelViewer( pane.modeltabFolder, pane.tab, pane.handler );
                    pane.modelViewer.setMapperConfiguration( mapperConfig );
                    pane.modelViewer.setModelType( pane.handler.type() );
                    updateBrowserText();
                } catch ( final ClassNotFoundException e ) {
                    Activator.error( getShell(), e );
                }
            }
        } );
        pane.modeltabFolder.addCTabFolder2Listener( new CTabFolder2Adapter() {
            
            @Override
            public void close( final CTabFolderEvent event ) {
                pane.toolBar.setVisible( true );
                pane.handler.setModel( null );
                updateBrowserText();
            }
        } );
        pane.toolBar.setVisible( true );
        if ( pane.handler.model() != null ) {
            pane.modelViewer = PanelUIUtil.createModelViewer( pane.modeltabFolder, pane.tab, pane.handler );
            pane.modelViewer.setMapperConfiguration( mapperConfig );
            pane.modelViewer.setModelType( pane.handler.type() );
            xformViewer.targethistory = pane.handler.getModelHistory();
        }
        xformViewer.addChangeListener( new ChangeListener() {
            
            @Override
            public void stateChanged( final ChangeEvent e ) {
                // the config changed - update the model labels in case
                // something changed in the mapped fields (for decorators)
                sourceModelPane.modelViewer.refresh();
                targetModelPane.modelViewer.refresh();
                xformViewer.xformsPane.layout(true);
            }
        } );
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.swt.widgets.Widget#dispose()
     */
    @Override
    public void dispose() {
        super.dispose();
        disposeImage( changeToolItemImage );
        disposeImage( addToolItemImage );
        disposeImage( literalsItemImage );
        if ( loader != null ) try {
            loader.close();
        } catch ( final IOException e ) {
            Activator.error( getShell(), e );
        }
    }
    
    private void disposeImage( final Image toDispose ) {
        if ( toDispose != null && !toDispose.isDisposed() ) {
            toDispose.dispose();
        }
    }
    
    public void setEndpointID( final String id ) {
        this.xformViewer.endpointID = id;
    }
    
    void updateBrowserText() {
        if ( sourceModel == null && targetModel == null ) helpText.setText( "Select the source and target models below." );
        else if ( sourceModel == null ) helpText.setText( "Select the source model below." );
        else if ( targetModel == null ) helpText.setText( "Select the target model below." );
        else helpText.setText( "Create a new mapping in the list of operations above by dragging an item below from source " +
                               sourceModel.getName() + " to target " + targetModel.getName() );
        
        if ( addSourceButton != null && !addSourceButton.isDisposed() ) {
            if ( sourceModel == null ) {
                addSourceButton.setText( "Add Source" );
                addSourceButton.setImage( addToolItemImage );
            } else {
                addSourceButton.setText( "Change Source" );
                addSourceButton.setImage( changeToolItemImage );
            }
        }
        if ( addTargetButton != null && !addTargetButton.isDisposed() ) {
            if ( targetModel == null ) {
                addTargetButton.setText( "Add Target" );
                addTargetButton.setImage( addToolItemImage );
            } else {
                addTargetButton.setText( "Change Target" );
                addTargetButton.setImage( changeToolItemImage );
            }
        }
    }
    
    void updateMappings() {
        if ( sourceModel == null || targetModel == null ) return;
        mapperConfig.removeAllMappings();
        mapperConfig.addClassMapping( sourceModel.getType(), targetModel.getType() );
        xformViewer.save();
        xformViewer.xformsPane.layout(true);
    }
    
    class ModelPane {
        
        public CTabFolder modeltabFolder;
        public ToolBar toolBar;
        public ModelViewer modelViewer;
        public Handler handler;
        public CTabItem tab;
    }

    public void setCamelPath(String path) {
        this.xformViewer.camelFilePath = path;
    }
}
