package org.jboss.mapper.eclipse;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.dialogs.ResourceListSelectionDialog;
import org.jboss.mapper.forge.Model;

class DataBrowser extends Composite {
    
    static final Field[] NO_FIELDS = new Field[ 0 ];
    
    Model model;
    final TreeViewer viewer;
    Button addButton;
    
    DataBrowser( final DataMapper mapper,
                 final String modelType,
                 final Model model,
                 final Listener listener ) {
        super( mapper, SWT.NONE );
        this.model = model;
        setLayout( GridLayoutFactory.fillDefaults().numColumns( 2 ).create() );
        
        final Label label = new Label( this, SWT.NONE );
        if ( model == null ) {
            label.setText( modelType + ":" );
            addButton = new Button( this, SWT.NONE );
            addButton.setText( "Add" );
            addButton.addSelectionListener( new SelectionAdapter() {
                
                private void findClasses( final IFolder folder,
                                          final List< IResource > classes ) throws CoreException {
                    for ( final IResource resource : folder.members() ) {
                        if ( resource instanceof IFolder ) findClasses( ( IFolder ) resource, classes );
                        else if ( resource.getName().endsWith( ".class" ) ) classes.add( resource );
                    }
                }
                
                @Override
                public void widgetSelected( final SelectionEvent event ) {
                    final IFolder classesFolder = mapper.configFile.getProject().getFolder( "target/classes" );
                    final List< IResource > classes = new ArrayList<>();
                    try {
                        findClasses( classesFolder, classes );
                        final ResourceListSelectionDialog dlg =
                            new ResourceListSelectionDialog( getShell(), classes.toArray( new IResource[ classes.size() ] ) ) {
                                
                                @Override
                                protected Control createDialogArea( final Composite parent ) {
                                    final Composite dlgArea = ( Composite ) super.createDialogArea( parent );
                                    for ( final Control child : dlgArea.getChildren() ) {
                                        if ( child instanceof Text ) {
                                            ( ( Text ) child ).setText( "*" );
                                            break;
                                        }
                                    }
                                    return dlgArea;
                                }
                            };
                        dlg.setTitle( "Select " + modelType );
                        if ( dlg.open() == Window.OK ) {
                            final IFile file = ( IFile ) dlg.getResult()[ 0 ];
                            final String name =
                                file.getFullPath().makeRelativeTo( classesFolder.getFullPath() ).toString().replace( '/', '.' );
                            DataBrowser.this.model =
                                listener.modelSelected( name.substring( 0, name.length() - ".class".length() ) );
                            addButton.removeSelectionListener( this );
                            addButton.dispose();
                            label.setText( modelType + ": " + DataBrowser.this.model.getName() );
                            viewer.setInput( DataBrowser.this.model );
                            layout();
                        }
                    } catch ( final Exception e ) {
                        Activator.error( e );
                    }
                }
            } );
        } else label.setText( modelType + ": " + model.getName() );
        viewer = new TreeViewer( this );
        final TreeViewerColumn column = new TreeViewerColumn( viewer, SWT.NONE );
        final Tree tree = viewer.getTree();
        tree.setLayoutData( GridDataFactory.fillDefaults().span( 2, 1 ).grab( true, true ).create() );
        column.setLabelProvider( new ColumnLabelProvider() {
            
            @Override
            public String getText( final Object element ) {
                final Model model = ( Model ) element;
                return model.getName() + ": " + model.getType();
            }
        } );
        viewer.setContentProvider( new ITreeContentProvider() {
            
            @Override
            public void dispose() {}
            
            @Override
            public Object[] getChildren( final Object parentElement ) {
                final Model model = ( Model ) parentElement;
                final List< Model > fieldModels = new ArrayList<>();
                for ( final String name : model.listFields() ) {
                    final Model fieldModel = model.get( name );
                    if ( fieldModel != null ) fieldModels.add( fieldModel );
                }
                return fieldModels.toArray( new Model[ fieldModels.size() ] );
            }
            
            @Override
            public Object[] getElements( final Object inputElement ) {
                return getChildren( inputElement );
            }
            
            @Override
            public Object getParent( final Object element ) {
                return ( ( Model ) element ).getParent();
            }
            
            @Override
            public boolean hasChildren( final Object element ) {
                return getChildren( element ).length > 0;
            }
            
            @Override
            public void inputChanged( final Viewer viewer,
                                      final Object oldInput,
                                      final Object newInput ) {}
        } );
        
        tree.addControlListener( new ControlAdapter() {
            
            @Override
            public void controlResized( final ControlEvent event ) {
                column.getColumn().setWidth( viewer.getTree().getSize().x - 2 );
            }
        } );
        viewer.setInput( model );
    }
    
    static interface Listener {
        
        Model modelSelected( String className );
    }
}
