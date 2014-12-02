package org.jboss.mapper.eclipse;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.dialogs.ResourceListSelectionDialog;
import org.jboss.mapper.model.Model;

class DataBrowser extends Composite {
    
    static final Field[] NO_FIELDS = new Field[ 0 ];
    
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
    
    Model model;
    final Listener listener;
    final Button modelButton;
    final TreeViewer viewer;
    
    DataBrowser( final DataMapper mapper,
                 final String modelType,
                 final Model model,
                 final Listener listener ) {
        super( mapper, SWT.NONE );
        this.model = model;
        this.listener = listener;
        setLayout( GridLayoutFactory.fillDefaults().numColumns( 2 ).create() );
        
        final Label label = new Label( this, SWT.NONE );
        label.setText( modelType + ":" );
        modelButton = new Button( this, SWT.NONE );
        modelButton.setText( model == null ? "Add" : model.getName() );
        modelButton.addSelectionListener( new SelectionAdapter() {
            
            @Override
            public void widgetSelected( final SelectionEvent event ) {
                selectModel( mapper.configFile.getProject(), modelType );
            }
        } );
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
    
    void selectModel( final IProject project,
                      final String modelType ) {
        final String name = selectModel( getShell(), project, model, modelType );
        if ( name == null ) return;
        if ( name.equals( modelButton.getText() ) ) return;
        model = listener.modelSelected( name );
        modelButton.setText( model.getName() );
        viewer.setInput( model );
        layout();
    }
    
    static interface Listener {
        
        Model modelSelected( String className );
    }
}
