package org.jboss.mapper.eclipse;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.jboss.mapper.model.Model;

class DataBrowser extends Composite {

    Model model;
    final TreeViewer viewer;

    DataBrowser( final Composite parent,
                 final Model model ) {
        super( parent, SWT.NONE );
        this.model = model;
        setLayout( GridLayoutFactory.fillDefaults().numColumns( 2 ).create() );
        viewer = new TreeViewer( this );
        viewer.setComparator( new ViewerComparator() {

            @Override
            public int compare( Viewer viewer,
                                Object model1,
                                Object model2 ) {
                return ( ( Model ) model1 ).getName().compareTo( ( ( Model ) model2 ).getName() );
            }
        });
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
}
