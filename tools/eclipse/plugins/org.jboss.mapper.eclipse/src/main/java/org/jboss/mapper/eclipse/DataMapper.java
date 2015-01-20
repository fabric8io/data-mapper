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
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.jface.viewers.ViewerSorter;
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
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ResourceListSelectionDialog;
import org.jboss.mapper.Literal;
import org.jboss.mapper.dozer.ConfigBuilder;
import org.jboss.mapper.dozer.config.Field;
import org.jboss.mapper.dozer.config.Mapping;
import org.jboss.mapper.eclipse.util.JavaUtil;
import org.jboss.mapper.model.Model;
import org.jboss.mapper.model.ModelBuilder;

/**
 *
 */
public class DataMapper extends Composite {

    static final Transfer[] XFERS = new Transfer[] { LocalSelectionTransfer.getTransfer() };
    static final ISharedImages IMAGES = PlatformUI.getWorkbench().getSharedImages();

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
    TableViewer opViewer;
    Text helpText;

    DataMapper( final Composite parent,
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

            // Change the color used to paint the sashes
            splitter.setBackground( getDisplay().getSystemColor( SWT.COLOR_DARK_GRAY ) );
            splitter.SASH_WIDTH = 5;

            // Create mapped operations viewer
            final Composite opPane = new Composite( splitter, SWT.NONE );
            opPane.setLayout( GridLayoutFactory.fillDefaults().create() );
            opPane.setBackground( getBackground() );
            // Create tool bar for mapped operations
            final ToolBar opToolBar = new ToolBar( opPane, SWT.NONE );
            final ToolItem deleteOp = new ToolItem( opToolBar, SWT.PUSH );
            deleteOp.setImage( IMAGES.getImage( ISharedImages.IMG_ETOOL_DELETE ) );
            deleteOp.setToolTipText( "Delete the selected operation(s)" );
            deleteOp.setEnabled( false );
            opViewer = new TableViewer( opPane );
            final Table table = opViewer.getTable();
            table.setLayoutData( GridDataFactory.fillDefaults().grab( true, true ).create() );
            table.setHeaderVisible( true );
            final TableViewerColumn sourceColumn = new TableViewerColumn( opViewer, SWT.LEFT );
            sourceColumn.getColumn().setText( "Source Item" );
            sourceColumn.setLabelProvider( new ColumnLabelProvider() {

                @Override
                public String getText( final Object element ) {
                    Field field = ( Field ) element;
                    String literal = field.getCustomConverterParam();
                    return literal == null ? super.getText( field.getA().getContent() ) : "\"" + literal + "\"";
                }
            } );
            final TableViewerColumn operationColumn = new TableViewerColumn( opViewer, SWT.CENTER );
            operationColumn.getColumn().setText( "Operation" );
            operationColumn.setLabelProvider( new ColumnLabelProvider() {

                @Override
                public String getText( final Object element ) {
                    return "=>";
                }
            } );
            final TableViewerColumn targetColumn = new TableViewerColumn( opViewer, SWT.LEFT );
            targetColumn.getColumn().setText( "Target Item" );
            targetColumn.setLabelProvider( new ColumnLabelProvider() {

                @Override
                public String getText( final Object element ) {
                    return super.getText( ( ( Field ) element ).getB().getContent() );
                }
            } );
            opViewer.setContentProvider( new IStructuredContentProvider() {

                @Override
                public void dispose() {}

                @Override
                public Object[] getElements( final Object inputElement ) {
                    final List< Object > fields = new ArrayList<>();
                    for ( final Mapping mapping : mappings ) {
                        for ( final Object field : mapping.getFieldOrFieldExclude() ) {
                            fields.add( field );
                            opViewer.setData( field.toString(), mapping );
                        }
                    }
                    return fields.toArray();
                }

                @Override
                public void inputChanged( final Viewer viewer,
                                          final Object oldInput,
                                          final Object newInput ) {}
            } );
            opViewer.setInput( mappings );
            opViewer.addSelectionChangedListener( new ISelectionChangedListener() {

                @Override
                public void selectionChanged( SelectionChangedEvent event ) {
                    deleteOp.setEnabled( !event.getSelection().isEmpty() );
                }
            } );
            deleteOp.addSelectionListener( new SelectionAdapter() {

                @Override
                public void widgetSelected( SelectionEvent event ) {
                    for ( final Iterator< ? > iter = ( ( IStructuredSelection ) opViewer.getSelection() ).iterator(); iter.hasNext(); ) {
                        Field field = ( Field ) iter.next();
                        deleteFieldMapping( field );
                        opViewer.remove( field );
                    }
                }
            } );
            operationColumn.getColumn().pack();
            table.addControlListener( new ControlAdapter() {

                @Override
                public void controlResized( final ControlEvent event ) {
                    final int width = ( table.getSize().x - operationColumn.getColumn().getWidth() ) / 2 - 1;
                    sourceColumn.getColumn().setWidth( width );
                    targetColumn.getColumn().setWidth( width );
                }
            } );

            final Composite mapper = new Composite( splitter, SWT.NONE );
            mapper.setBackground( getBackground() );
            mapper.setLayout( GridLayoutFactory.swtDefaults().margins( 0, 5 ).numColumns( 3 ).create() );

            // Create help text
            helpText = new Text( mapper, SWT.MULTI | SWT.WRAP );
            helpText.setLayoutData( GridDataFactory.fillDefaults().grab( true, false ).span( 3, 1 ).create() );
            helpText.setForeground( getDisplay().getSystemColor( SWT.COLOR_BLUE ) );
            helpText.setBackground( getBackground() );
            updateBrowserText();

            // Create source browser
            final CTabFolder sourceTabFolder = createTabFolder( mapper, new Handler() {

                @Override
                public void configureDragAndDrop( final TreeViewer viewer ) {
                    viewer.addDragSupport( DND.DROP_MOVE, XFERS, new DragSourceAdapter() {

                        @Override
                        public void dragSetData( final DragSourceEvent event ) {
                            if ( LocalSelectionTransfer.getTransfer().isSupportedType( event.dataType ) )
                                LocalSelectionTransfer.getTransfer().setSelection( viewer.getSelection() );
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

            // Create constants tab
            final CTabItem constantsTab = new CTabItem( sourceTabFolder, SWT.NONE );
            constantsTab.setText( "Constants" );
            final Composite constantsPane = new Composite( sourceTabFolder, SWT.NONE );
            constantsPane.setLayout( GridLayoutFactory.fillDefaults().create() );
            constantsPane.setBackground( getBackground() );
            final ToolBar constantsToolBar = new ToolBar( constantsPane, SWT.NONE );
            final ToolItem addConstant = new ToolItem( constantsToolBar, SWT.PUSH );
            addConstant.setImage( IMAGES.getImage( ISharedImages.IMG_OBJ_ADD ) );
            addConstant.setToolTipText( "Add a new constant" );
            final ToolItem deleteConstant = new ToolItem( constantsToolBar, SWT.PUSH );
            deleteConstant.setImage( IMAGES.getImage( ISharedImages.IMG_ETOOL_DELETE ) );
            deleteConstant.setToolTipText( "Delete the selected constant(s)" );
            deleteConstant.setEnabled( false );
            final ListViewer constantsViewer = new ListViewer( constantsPane );
            constantsViewer.getList().setLayoutData( GridDataFactory.fillDefaults().grab( true, true ).create() );
            constantsViewer.addDragSupport( DND.DROP_MOVE, XFERS, new DragSourceAdapter() {

                @Override
                public void dragSetData( final DragSourceEvent event ) {
                    if ( LocalSelectionTransfer.getTransfer().isSupportedType( event.dataType ) )
                        LocalSelectionTransfer.getTransfer().setSelection( constantsViewer.getSelection() );
                }
            } );
            constantsViewer.setSorter( new ViewerSorter() );
            constantsTab.setControl( constantsPane );
            addConstant.addSelectionListener( new SelectionAdapter() {

                @Override
                public void widgetSelected( SelectionEvent event ) {
                    final InputDialog dlg = new InputDialog( getShell(),
                                                             "Add Constant",
                                                             "Enter a new constant value",
                                                             null,
                                                             new IInputValidator() {

                                                                 @Override
                                                                 public String isValid( String text ) {
                                                                     return constantsViewer.getList().indexOf( text ) < 0 ? null : "Value already exists";
                                                                 }
                                                             } );
                    if ( dlg.open() == Window.OK ) constantsViewer.add( dlg.getValue() );
                }
            } );
            constantsViewer.addSelectionChangedListener( new ISelectionChangedListener() {

                @Override
                public void selectionChanged( SelectionChangedEvent event ) {
                    deleteConstant.setEnabled( !event.getSelection().isEmpty() );
                }
            } );
            deleteConstant.addSelectionListener( new SelectionAdapter() {

                @Override
                public void widgetSelected( SelectionEvent event ) {
                    for ( final Iterator< ? > iter = ( ( IStructuredSelection ) constantsViewer.getSelection() ).iterator(); iter.hasNext(); ) {
                        constantsViewer.remove( iter.next() );
                    }
                }
            } );
            // Initialize constants from config
            for ( final Mapping mapping : mappings ) {
                for ( final Object object : mapping.getFieldOrFieldExclude() ) {
                    Field field = (Field) object;
                    String literal = field.getCustomConverterParam();
                    if ( literal != null ) constantsViewer.add( literal );
                }
            }

            final Label label = new Label( mapper, SWT.NONE );
            label.setText( "=>" );

            // Create target browser
            createTabFolder( mapper, new Handler() {

                @Override
                public void configureDragAndDrop( final TreeViewer viewer ) {
                    viewer.addDropSupport( DND.DROP_MOVE, XFERS, new ViewerDropAdapter( viewer ) {

                        @Override
                        public boolean performDrop( final Object data ) {
                            Object source =
                                ( ( IStructuredSelection ) LocalSelectionTransfer.getTransfer().getSelection() ).getFirstElement();
                            if (source instanceof Model) configBuilder.map( (Model) source, ( Model ) getCurrentTarget() );
                            else configBuilder.map( new Literal( source.toString() ), ( Model ) getCurrentTarget() );
                            try ( FileOutputStream stream = new FileOutputStream( new File( configFile.getLocationURI() ) ) ) {
                                configBuilder.saveConfig( stream );
                                configFile.getProject().refreshLocal( IResource.DEPTH_INFINITE, null );
                                opViewer.refresh();
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
        Composite pane = new Composite( tabFolder, SWT.NONE );
        pane.setBackground( getBackground() );
        tab.setControl( pane );
        pane.setLayout( GridLayoutFactory.fillDefaults().create() );
        ToolBar toolBar = new ToolBar( pane, SWT.NONE );
        final DataBrowser browser = new DataBrowser( pane, handler.model() );
        browser.setLayoutData( GridDataFactory.fillDefaults().grab( true, true ).create() );
        handler.configureDragAndDrop( browser.viewer );
        browser.viewer.setInput( handler.model() );
        browser.layout();
        tabFolder.setSelection( tab );
    }

    private CTabFolder createTabFolder( Composite mapper, final Handler handler ) {
        final CTabFolder tabFolder = new CTabFolder( mapper, SWT.BORDER );
        tabFolder.setLayoutData( GridDataFactory.fillDefaults().grab( true, true ).create() );
        tabFolder.setBackground( getDisplay().getSystemColor( SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT ) );
        final ToolBar toolBar = new ToolBar( tabFolder, SWT.RIGHT );
        tabFolder.setTopRight( toolBar );
        final ToolItem addSourceButton = new ToolItem( toolBar, SWT.NONE );
        addSourceButton.setImage( IMAGES.getImage( ISharedImages.IMG_OBJ_ADD ) );
        addSourceButton.setToolTipText( "Set transformation " + handler.type().toLowerCase() );
        addSourceButton.addSelectionListener( new SelectionAdapter() {

            @Override
            public void widgetSelected( SelectionEvent event ) {
                final String name = selectModel( getShell(), configFile.getProject(), handler.model(), handler.type() );
                if ( name == null ) return;
                try {
                    handler.setModel( ModelBuilder.fromJavaClass( loader.loadClass( name ) ) );
                    updateMappings();
                    updateBrowserText();
                    createTab( tabFolder, handler );
                    toolBar.setVisible( false );
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
        if ( handler.model() == null ) tabFolder.setTopRight( toolBar );
        else createTab( tabFolder, handler );
        return tabFolder;
    }

    /**
     * @param field
     * @return <code>true</code> if the supplied field was successfully removed
     */
    public boolean deleteFieldMapping( Field field ) {
        final Mapping mapping = ( Mapping ) opViewer.getData( field.toString() );
        final boolean removed = mapping.getFieldOrFieldExclude().remove( field );
        if ( removed ) {
            try {
                configBuilder.saveConfig( new FileOutputStream( new File( configFile.getLocationURI() ) ) );
                configFile.getProject().refreshLocal( IResource.DEPTH_INFINITE, null );
                opViewer.refresh();
                return true;
            } catch ( final Exception e ) {
                Activator.error( getShell(), e );
            }
        }
        return false;
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
        try {
            configBuilder.saveConfig( new FileOutputStream( new File( configFile.getLocationURI() ) ) );
            configFile.getProject().refreshLocal( IResource.DEPTH_INFINITE, null );
            opViewer.refresh();
        } catch ( final Exception e ) {
            Activator.error( getShell(), e );
        }
    }

    interface Handler {

        void configureDragAndDrop( TreeViewer viewer );

        Model model();

        void setModel( Model model );

        String type();
    }
}
