package org.jboss.mapper.eclipse;

import java.lang.reflect.Field;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
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
import org.example.order.abc.ABCOrder;
import org.example.order.xyz.XYZOrder;

class Mapper extends Composite {
    
    Mapper( final Composite parent ) {
        super( parent, SWT.NONE );
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
                return super.getText( ( ( Operation ) element ).source.getName() );
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
                return super.getText( ( ( Operation ) element ).target.getName() );
            }
        } );
        viewer.setContentProvider( ArrayContentProvider.getInstance() );
        
        final Transformation xform = new Transformation();
        viewer.setInput( xform.operations );
        
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
        text.setText( "Create a new mapping in the list of operations above by dragging an item from source ABCOrder below to target XYZOrder" );
        text.setBackground( getBackground() );
        
        final Browser sourceBrowser = new Browser( this );
        sourceBrowser.setLayoutData( GridDataFactory.fillDefaults().grab( true, true ).create() );
        sourceBrowser.setInput( "Source", ABCOrder.class );
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
        
        final Browser targetBrowser = new Browser( this );
        targetBrowser.setLayoutData( GridDataFactory.fillDefaults().grab( true, true ).create() );
        targetBrowser.setInput( "Target", XYZOrder.class );
        targetBrowser.viewer.addDropSupport( DND.DROP_MOVE, xfers, new ViewerDropAdapter( targetBrowser.viewer ) {
            
            @Override
            public boolean performDrop( final Object data ) {
                final Operation operation = new Operation();
                operation.source = ( Field ) ( ( IStructuredSelection ) LocalSelectionTransfer.getTransfer().getSelection() ).getFirstElement();
                operation.target = ( Field ) getCurrentTarget();
                xform.operations.add( operation );
                viewer.refresh();
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
}
