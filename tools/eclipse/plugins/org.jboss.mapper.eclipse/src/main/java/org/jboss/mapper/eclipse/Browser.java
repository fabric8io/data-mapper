package org.jboss.mapper.eclipse;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
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

class Browser extends Composite {
    
    TreeViewer viewer = new TreeViewer( this );
    TreeViewerColumn column = new TreeViewerColumn( viewer, SWT.NONE );
    
    Browser( final Composite parent ) {
        super( parent, SWT.NONE );
        setLayout( GridLayoutFactory.fillDefaults().create() );
        final Tree tree = viewer.getTree();
        tree.setLayoutData( GridDataFactory.fillDefaults().grab( true, true ).create() );
        tree.setHeaderVisible( true );
        column.setLabelProvider( new ColumnLabelProvider() {
            
            @Override
            public String getText( final Object element ) {
                final Member member = ( Member ) element;
                return member.getName()
                       + ": "
                       + ( member instanceof Field ? ( ( Field ) member ).getType() : ( ( Method ) member ).getReturnType() ).getSimpleName();
            }
        } );
        viewer.setContentProvider( new ITreeContentProvider() {
            
            @Override
            public void dispose() {}
            
            @Override
            public Object[] getChildren( final Object parentElement ) {
                final Member member = ( Member ) parentElement;
                Class< ? > type = member instanceof Field ? ( ( Field ) member ).getType()
                                : ( ( Method ) member ).getReturnType();
                if ( type.isArray() ) type = type.getComponentType();
                if ( type.isInterface() ) {
                    final Method[] methods = type.getDeclaredMethods();
                    final List< Method > readMethods = new ArrayList<>();
                    for ( final Method method : methods ) {
                        readMethods.add( method );
                    }
                    return readMethods.toArray();
                }
                return ( ( Class< ? > ) type ).getDeclaredFields();
            }
            
            @Override
            public Object[] getElements( final Object inputElement ) {
                return ( ( Class< ? > ) inputElement ).getDeclaredFields();
            }
            
            @Override
            public Object getParent( final Object element ) {
                final Field field = ( Field ) element;
                System.out.println( "getParent: " + field );
                return field.getDeclaringClass();
            }
            
            @Override
            public boolean hasChildren( final Object element ) {
                final Member member = ( Member ) element;
                final Class< ? > type = member instanceof Field ? ( ( Field ) member ).getType()
                                : ( ( Method ) member ).getReturnType();
                if ( type.isArray() ) return true;
                if ( type.isPrimitive() || type == String.class ) return false;
                if ( type.isInterface() ) {
                    final Method[] methods = type.getDeclaredMethods();
                    final List< Method > readMethods = new ArrayList<>();
                    for ( final Method method : methods ) {
                        readMethods.add( method );
                    }
                    return readMethods.size() > 0;
                }
                final Field[] fields = ( ( Class< ? > ) type ).getDeclaredFields();
                return fields.length > 0;
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
                   final Class< ? > input ) {
        column.getColumn().setText( titlePrefix + ": " + input.getSimpleName() );
        viewer.setInput( input );
    }
}
