package org.jboss.mapper.eclipse;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.jboss.mapper.forge.Model;

class DataBrowser extends Composite {
    
    static final Field[] NO_FIELDS = new Field[ 0 ];
    
    TreeViewer viewer = new TreeViewer( this );
    TreeViewerColumn column = new TreeViewerColumn( viewer, SWT.NONE );
    
    DataBrowser( final Composite parent ) {
        super( parent, SWT.NONE );
        setLayout( GridLayoutFactory.fillDefaults().create() );
        final Tree tree = viewer.getTree();
        tree.setLayoutData( GridDataFactory.fillDefaults().grab( true, true ).create() );
        tree.setHeaderVisible( true );
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
    }
    
    void setInput( final String titlePrefix,
                   final Model model ) {
        column.getColumn().setText( titlePrefix + ": " + model.getName() );
        viewer.setInput( model );
    }
}
