package org.jboss.mapper.eclipse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DragSourceAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.jboss.mapper.dozer.config.Field;
import org.jboss.mapper.dozer.config.Mapping;
import org.jboss.mapper.forge.ConfigBuilder;
import org.jboss.mapper.forge.Model;
import org.jboss.mapper.forge.ModelBuilder;

class DataMapper extends Composite {
    
    URLClassLoader loader;
    ConfigBuilder configBuilder;
    
    DataMapper( final Composite parent,
            final File configFile ) {
        super( parent, SWT.NONE );
        
        try {
            configBuilder = ConfigBuilder.loadConfig( configFile );
            final List< Mapping > mappings = configBuilder.getMappings().getMapping();
            final Mapping mainMapping = mappings.get( 0 );
            loader = new URLClassLoader( new URL[] {
                new File( configFile.getParentFile().getParentFile().getParentFile().getParentFile(), "target/classes" ).toURI().toURL()
            } );
            final Model sourceModel = ModelBuilder.fromJavaClass( loader.loadClass( mainMapping.getClassA().getContent() ) );
            final Model targetModel = ModelBuilder.fromJavaClass( loader.loadClass( mainMapping.getClassB().getContent() ) );
            
            setLayout( GridLayoutFactory.swtDefaults().spacing( 0, 5 ).numColumns( 3 ).create() );
            
            final TableViewer viewer = new TableViewer( this );
            final Table table = viewer.getTable();
            table.setLayoutData( GridDataFactory.fillDefaults().span( 3, 1 ).grab( true, true ).create() );
            table.setHeaderVisible( true );
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
            
            final Text text = new Text( this, SWT.MULTI | SWT.WRAP );
            text.setLayoutData( GridDataFactory.fillDefaults().grab( true, false ).span( 3, 1 ).create() );
            text.setText( "Create a new mapping in the list of operations above by dragging an item below from source " +
                          sourceModel.getName() + " to target " + targetModel.getName() );
            text.setBackground( getBackground() );
            
            final DataBrowser sourceBrowser = new DataBrowser( this );
            sourceBrowser.setLayoutData( GridDataFactory.fillDefaults().grab( true, true ).create() );
            sourceBrowser.setInput( "Source", sourceModel );
            final Transfer[] xfers = new Transfer[] { LocalSelectionTransfer.getTransfer() };
            sourceBrowser.viewer.addDragSupport( DND.DROP_MOVE, xfers, new DragSourceAdapter() {
                
                @Override
                public void dragSetData( final DragSourceEvent event ) {
                    if ( LocalSelectionTransfer.getTransfer().isSupportedType( event.dataType ) ) {
                        LocalSelectionTransfer.getTransfer().setSelection( sourceBrowser.viewer.getSelection() );
                    }
                }
            } );
            
            final Label label = new Label( this, SWT.NONE );
            label.setText( "=>" );
            
            final DataBrowser targetBrowser = new DataBrowser( this );
            targetBrowser.setLayoutData( GridDataFactory.fillDefaults().grab( true, true ).create() );
            targetBrowser.setInput( "Target", targetModel );
            targetBrowser.viewer.addDropSupport( DND.DROP_MOVE, xfers, new ViewerDropAdapter( targetBrowser.viewer ) {
                
                @Override
                public boolean performDrop( final Object data ) {
                    final Model sourceModel = ( Model ) ( ( IStructuredSelection ) LocalSelectionTransfer.getTransfer().getSelection() ).getFirstElement();
                    final Model targetModel = ( Model ) getCurrentTarget();
                    configBuilder.map( sourceModel, targetModel );
                    try ( FileOutputStream stream = new FileOutputStream( configFile ) ) {
                        configBuilder.saveConfig( stream );
                        viewer.refresh();
                    } catch ( final Exception e ) {
                        e.printStackTrace();
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
        } catch ( final Exception e ) {
            e.printStackTrace();
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
            e.printStackTrace();
        }
    }
}
