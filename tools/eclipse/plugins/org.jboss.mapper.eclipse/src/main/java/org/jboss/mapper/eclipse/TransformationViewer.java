package org.jboss.mapper.eclipse;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.jboss.mapper.FieldMapping;
import org.jboss.mapper.Literal;
import org.jboss.mapper.LiteralMapping;
import org.jboss.mapper.MapperConfiguration;
import org.jboss.mapper.MappingOperation;
import org.jboss.mapper.MappingType;
import org.jboss.mapper.model.Model;

class TransformationViewer extends Composite {

    final IFile configFile;
    final MapperConfiguration mapperConfig;
    final TableViewer viewer;

    TransformationViewer( final Composite parent,
                          final IFile configFile,
                          final MapperConfiguration mapperConfig ) {
        super( parent, SWT.NONE );

        this.configFile = configFile;
        this.mapperConfig = mapperConfig;

        setLayout( GridLayoutFactory.fillDefaults().create() );
        setBackground( parent.getBackground() );

        // Create tool bar
        final ToolBar toolBar = new ToolBar( this, SWT.NONE );
        final ToolItem deleteButton = new ToolItem( toolBar, SWT.PUSH );
        deleteButton.setImage( PlatformUI.getWorkbench().getSharedImages().getImage( ISharedImages.IMG_ETOOL_DELETE ) );
        deleteButton.setToolTipText( "Delete the selected operation(s)" );
        deleteButton.setEnabled( false );

        viewer = new TableViewer( this );
        final Table table = viewer.getTable();
        table.setLayoutData( GridDataFactory.fillDefaults().grab( true, true ).create() );
        table.setHeaderVisible( true );
        final TableViewerColumn sourceColumn = new TableViewerColumn( viewer, SWT.LEFT );
        sourceColumn.getColumn().setText( "Source Item" );
        sourceColumn.setLabelProvider( new ColumnLabelProvider() {

            @Override
            public String getText( final Object element ) {
                MappingOperation< ?, ? > mapping = ( MappingOperation< ?, ? > ) element;
                if ( MappingType.LITERAL == mapping.getType() )
                    return "\"" + ( ( LiteralMapping ) mapping ).getSource().getValue() + "\"";
                return super.getText( ( ( FieldMapping ) mapping ).getSource().getName() );
            }
        } );
        final TableViewerColumn operationColumn = new TableViewerColumn( viewer, SWT.CENTER );
        operationColumn.getColumn().setText( "Operation" );
        operationColumn.setLabelProvider( new ColumnLabelProvider() {

            @Override
            public String getText( final Object element ) {
                return "=>";
            }
        } );
        final TableViewerColumn targetColumn = new TableViewerColumn( viewer, SWT.LEFT );
        targetColumn.getColumn().setText( "Target Item" );
        targetColumn.setLabelProvider( new ColumnLabelProvider() {

            @Override
            public String getText( final Object element ) {
                Model target = ( Model )( ( MappingOperation<?,?> ) element ).getTarget();
                return super.getText( target.getName() );
            }
        } );
        viewer.setContentProvider( new IStructuredContentProvider() {

            @Override
            public void dispose() {}

            @Override
            public Object[] getElements( final Object inputElement ) {
                final List< Object > fieldMappings = new ArrayList<>();
                for ( final MappingOperation<?,?> mapping : mapperConfig.getMappings() ) {
                    fieldMappings.add( mapping );
                    table.setData( mapping.toString(), mapping );
                }
                return fieldMappings.toArray();
            }

            @Override
            public void inputChanged( final Viewer viewer,
                                      final Object oldInput,
                                      final Object newInput ) {}
        } );
        viewer.addSelectionChangedListener( new ISelectionChangedListener() {

            @Override
            public void selectionChanged( SelectionChangedEvent event ) {
                deleteButton.setEnabled( !event.getSelection().isEmpty() );
            }
        } );
        deleteButton.addSelectionListener( new SelectionAdapter() {

            @Override
            public void widgetSelected( SelectionEvent event ) {
                for ( final Iterator< ? > iter = ( ( IStructuredSelection ) viewer.getSelection() ).iterator(); iter.hasNext(); ) {
                    MappingOperation<?,?> mapping = ( MappingOperation<?,?> ) iter.next();
                    mapperConfig.removeMapping(mapping);
                    try {
                        mapperConfig.saveConfig( new FileOutputStream( new File( configFile.getLocationURI() ) ) );
                        configFile.getProject().refreshLocal( IResource.DEPTH_INFINITE, null );
                        viewer.remove( mapping );
                        viewer.refresh();
                    } catch ( final Exception e) {
                        Activator.error( getShell(), e );
                    }
                }
            }
        } );
        viewer.setInput( mapperConfig.getMappings() );
        operationColumn.getColumn().pack();
        table.addControlListener( new ControlAdapter() {

            @Override
            public void controlResized( final ControlEvent event ) {
                final int width = ( table.getSize().x - operationColumn.getColumn().getWidth() ) / 2 - 1;
                sourceColumn.getColumn().setWidth( width );
                targetColumn.getColumn().setWidth( width );
            }
        } );
    }

    void map( Literal literal,
              Model targetModel ) {
        mapperConfig.map( literal, targetModel );
        save();
    }

    void map( Model sourceModel,
              Model targetModel ) {
        mapperConfig.map( sourceModel, targetModel );
        save();
    }

    void save() {
        try ( FileOutputStream stream = new FileOutputStream( new File( configFile.getLocationURI() ) ) ) {
            mapperConfig.saveConfig( stream );
            configFile.getProject().refreshLocal( IResource.DEPTH_INFINITE, null );
            viewer.refresh();
        } catch ( final Exception e ) {
            Activator.error( getShell(), e );
        }
    }
}
