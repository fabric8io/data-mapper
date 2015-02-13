package org.jboss.mapper.eclipse;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.jboss.mapper.CustomMapping;
import org.jboss.mapper.FieldMapping;
import org.jboss.mapper.Literal;
import org.jboss.mapper.LiteralMapping;
import org.jboss.mapper.MapperConfiguration;
import org.jboss.mapper.MappingOperation;
import org.jboss.mapper.MappingType;
import org.jboss.mapper.camel.CamelConfigBuilder;
import org.jboss.mapper.camel.EndpointHelper;
import org.jboss.mapper.camel.config.CamelEndpointFactoryBean;
import org.jboss.mapper.eclipse.util.Util;
import org.jboss.mapper.eclipse.dialogs.CamelEndpointSelectionDialog;
import org.jboss.mapper.model.Model;

class TransformationViewer extends Composite {

    private static final String IMAGE_PROPERTY = "org.jboss.mapper.eclipse.image";
    private static final String MAPPING_PROPERTY = "org.jboss.mapper.eclipse.mapping";
    private static final String OP_START_PROPERTY = "org.jboss.mapper.eclipse.opStart";
    private static final String OP_END_PROPERTY = "org.jboss.mapper.eclipse.opEnd";

    static final Image ADD_IMAGE = PlatformUI.getWorkbench().getSharedImages().getImage( ISharedImages.IMG_OBJ_ADD );
    static final Image DELETE_IMAGE = PlatformUI.getWorkbench().getSharedImages().getImage( ISharedImages.IMG_ETOOL_DELETE );

    // static final String ADD_CUSTOM_OPERATION_TOOL_TIP =
    // "Add a custom operation that uses the source element that follows as its first parameter";
    static final String ADD_CUSTOM_OPERATION_TOOL_TIP = "Add a custom operation to the source element that follows";
    static final String DELETE_CUSTOM_OPERATION_TOOL_TIP = "Delete the custom operation that follows";

    final IFile configFile;
    final MapperConfiguration mapperConfig;
    final ScrolledComposite scroller;
    final Composite xformsPane, sourcePane, mapsToPane, targetPane;
    final Point size;
    Color textBackground;
    Text prevTargetText;
    TraversalListener prevTraversalListener;

    Stack< Model > sourcehistory = null;
    Stack< Model > targethistory = null;
    String endpointID = null;
    private final ListenerList _changeListeners;
    String camelFilePath = null;

    TransformationViewer( final Composite parent,
                          final IFile configFile,
                          final MapperConfiguration mapperConfig ) {
        super( parent, SWT.NONE );

        this.configFile = configFile;
        this.mapperConfig = mapperConfig;
        _changeListeners = new ListenerList();

        setLayout( GridLayoutFactory.fillDefaults().create() );
        setBackground( parent.getBackground() );

        // Create tool bar
        // final ToolBar toolBar = new ToolBar( this, SWT.NONE );
        // final ToolItem addButton = new ToolItem( toolBar, SWT.PUSH );
        // addButton.setImage( PlatformUI.getWorkbench().getSharedImages().getImage( ISharedImages.IMG_OBJ_ADD ) );
        // addButton.setToolTipText( "Add new transformation" );

        // Create xforms pane
        scroller = new ScrolledComposite( this, SWT.V_SCROLL | SWT.BORDER );
        scroller.setLayoutData( GridDataFactory.fillDefaults().grab( true, true ).create() );
        scroller.setExpandHorizontal( true );
        scroller.setExpandVertical( true );
        scroller.setBackground( getBackground() );
        xformsPane = new Composite( scroller, SWT.NONE );
        scroller.setContent( xformsPane );
        xformsPane.setLayout( GridLayoutFactory.fillDefaults().numColumns( 3 ).spacing( 0, 0 ).create() );
        xformsPane.setBackground( getDisplay().getSystemColor( SWT.COLOR_BLUE ) );
        xformsPane.setBackground( getBackground() );
        sourcePane = new Composite( xformsPane, SWT.NONE );
        sourcePane.setLayoutData( GridDataFactory.fillDefaults().grab( true, true ).create() );
        sourcePane.setLayout( GridLayoutFactory.fillDefaults().create() );
        sourcePane.setBackground( getBackground() );
        mapsToPane = new Composite( xformsPane, SWT.NONE );
        mapsToPane.setLayoutData( GridDataFactory.fillDefaults().grab( false, true ).create() );
        mapsToPane.setLayout( GridLayoutFactory.fillDefaults().create() );
        mapsToPane.setBackground( getBackground() );
        targetPane = new Composite( xformsPane, SWT.NONE );
        targetPane.setLayoutData( GridDataFactory.fillDefaults().grab( true, false ).create() );
        targetPane.setLayout( GridLayoutFactory.fillDefaults().create() );
        targetPane.setBackground( getBackground() );

        final Label label = new Label( this, SWT.NONE );
        label.setImage( ADD_IMAGE );
        size = label.computeSize( SWT.DEFAULT, SWT.DEFAULT );
        label.dispose();

        for ( final MappingOperation< ?, ? > mapping : mapperConfig.getMappings() ) {
            createMapping( mapping );
        }
    }

    /**
     * Add a change listener.
     *
     * @param listener
     *            new listener
     */
    public void addChangeListener( final ChangeListener listener ) {
        this._changeListeners.add( listener );
    }

    private void createMapping( final MappingOperation< ?, ? > mapping ) {
        final Composite sourceEntryPane = new Composite( sourcePane, SWT.NONE );

        // Save mapping in pane so we can replace it if the mapping is changed
        sourceEntryPane.setData( MAPPING_PROPERTY, mapping );

        final boolean customMapping = mapping instanceof CustomMapping;
        final boolean fieldMapping = mapping instanceof FieldMapping;

        sourceEntryPane.setLayoutData( GridDataFactory.fillDefaults().grab( true, false ).create() );
        sourceEntryPane.setLayout( GridLayoutFactory.fillDefaults().spacing( 0, 0 ).numColumns( customMapping ? 5 : 3 ).create() );
        sourceEntryPane.setBackground( getBackground() );

        // Add spacer to right-justify source components
        final Label spacer = new Label( sourceEntryPane, SWT.NONE );
        spacer.setLayoutData( GridDataFactory.fillDefaults().grab( true, false ).create() );

        // Add add/delete op "button" for source component pane
        final Label addDeleteLabel = new Label( sourceEntryPane, SWT.NONE );
        addDeleteLabel.setLayoutData( GridDataFactory.swtDefaults().hint( size ).create() );
        addDeleteLabel.setToolTipText( customMapping ? DELETE_CUSTOM_OPERATION_TOOL_TIP
                        : fieldMapping ? ADD_CUSTOM_OPERATION_TOOL_TIP : null );
        addDeleteLabel.setData( IMAGE_PROPERTY, customMapping ? DELETE_IMAGE : fieldMapping ? ADD_IMAGE : null );
        // Configure op button to appear on mouse-over
        final MouseTrackListener mouseOverListener = new MouseTrackAdapter() {

            @Override
            public void mouseEnter( final MouseEvent event ) {
                addDeleteLabel.setImage( ( Image ) addDeleteLabel.getData( IMAGE_PROPERTY ) );
            }

            @Override
            public void mouseExit( final MouseEvent event ) {
                addDeleteLabel.setImage( null );
            }
        };
        addDeleteLabel.addMouseTrackListener( mouseOverListener );

        if ( customMapping ) newOpStartLabel( sourceEntryPane, ( ( CustomMapping ) mapping ).getMappingOperation(), mouseOverListener );

        final Text sourceText = new Text( sourceEntryPane, SWT.BORDER );
        if ( MappingType.LITERAL == mapping.getType() ) sourceText.setText( "\"" + ( ( LiteralMapping ) mapping ).getSource().getValue() + "\"" );
        else sourceText.setText( ( ( FieldMapping ) mapping ).getSource().getName() );
        sourceText.setEditable( false );
        // Configure op button to appear on mouse-over of source text
        sourceText.addMouseTrackListener( mouseOverListener );

        if ( customMapping ) newOpEndLabel( sourceEntryPane );

        final Label mapsToLabel = new Label( mapsToPane, SWT.NONE );
        mapsToLabel.setLayoutData( GridDataFactory.swtDefaults().create() );
        mapsToLabel.setText( "=>" );
        // Configure maps-to label to appear as delete "button" on mouse-over
        mapsToLabel.addMouseTrackListener( new MouseTrackAdapter() {

            @Override
            public void mouseEnter( final MouseEvent event ) {
                mapsToLabel.setImage( DELETE_IMAGE );
            }

            @Override
            public void mouseExit( final MouseEvent event ) {
                mapsToLabel.setText( "=>" );
            }
        } );

        final Composite targetEntryPane = new Composite( targetPane, SWT.NONE );
        targetEntryPane.setLayoutData( GridDataFactory.fillDefaults().grab( true, false ).create() );
        targetEntryPane.setLayout( GridLayoutFactory.fillDefaults().spacing( 0, 0 ).create() );
        targetEntryPane.setBackground( getBackground() );
        final Text targetText = new Text( targetEntryPane, SWT.BORDER );
        targetText.setText( ( ( Model ) mapping.getTarget() ).getName() );
        targetText.setEditable( false );

        // Make sourceEntryPane, mapsToLabel, & targetEntryPane the same height
        int height = sourceEntryPane.computeSize( SWT.DEFAULT, SWT.DEFAULT ).y;
        height = Math.max( height, mapsToLabel.computeSize( SWT.DEFAULT, SWT.DEFAULT ).y );
        height = Math.max( height, targetEntryPane.computeSize( SWT.DEFAULT, SWT.DEFAULT ).y );
        ( ( GridData ) sourceEntryPane.getLayoutData() ).heightHint = height;
        ( ( GridData ) mapsToLabel.getLayoutData() ).heightHint = height;
        ( ( GridData ) targetEntryPane.getLayoutData() ).heightHint = height;

        // Configure maps-to label (as delete button) to delete the transformation entry
        mapsToLabel.addMouseListener( new MouseAdapter() {

            @Override
            public void mouseUp( final MouseEvent event ) {
                mapperConfig.removeMapping( ( MappingOperation< ?, ? > ) sourceEntryPane.getData( MAPPING_PROPERTY ) );
                if ( save() ) {
                    sourceEntryPane.dispose();
                    mapsToLabel.dispose();
                    targetEntryPane.dispose();
                    xformsPane.layout();
                    scroller.setMinSize( xformsPane.computeSize( SWT.DEFAULT, SWT.DEFAULT ) );
                }
            }
        } );

        // Configure traversal of source and target text to ignore immediate containers
        final TraversalListener sourceTraversalListener = new TraversalListener( prevTargetText, targetText );
        sourceText.addTraverseListener( sourceTraversalListener );
        final TraversalListener targetTraversalListener = new TraversalListener( sourceText, null );
        targetText.addTraverseListener( targetTraversalListener );
        if ( prevTraversalListener != null ) prevTraversalListener.nextText = sourceText;

        // Save text background to restore after leaving mouse-over during DnD
        if ( textBackground == null ) textBackground = sourceText.getBackground();

        addDeleteLabel.addMouseListener( new MouseAdapter() {

            // private Text source2Text;
            // private TraversalListener source2TraversalListener;

            @Override
            public void mouseUp( final MouseEvent event ) {
                final Object img = addDeleteLabel.getData( IMAGE_PROPERTY );
                if ( img == ADD_IMAGE ) {
                    final AddCustomOperationDialog dlg = new AddCustomOperationDialog( getShell() );
                    if ( dlg.open() != Window.OK ) return;
                    newOpStartLabel( sourceEntryPane, dlg.method.getElementName(), mouseOverListener ).moveAbove( sourceText );
                    // final Label comma = new Label( sourceEntryPane, SWT.NONE );
                    // comma.moveBelow( sourceText );
                    // comma.setText( "," );
                    // source2Text = new Text( sourceEntryPane, SWT.BORDER );
                    // source2Text.setLayoutData( GridDataFactory.swtDefaults().hint( source2Text.computeSize( 100, SWT.DEFAULT
                    // ) ).create() );
                    // source2Text.moveBelow( comma );
                    // sourceTraversalListener.nextText = source2Text;
                    // source2TraversalListener = new TraversalListener( null, targetText );
                    // source2Text.addTraverseListener( source2TraversalListener );
                    //
                    // final DropTarget source2DropTarget = new DropTarget( source2Text, DND.DROP_MOVE );
                    // source2DropTarget.setTransfer( new Transfer[] { LocalSelectionTransfer.getTransfer() } );
                    // source2DropTarget.addDropListener( new DropTargetAdapter() {
                    //
                    // @Override
                    // public void dragEnter( final DropTargetEvent event ) {
                    // final Object dragSource =
                    // ( ( IStructuredSelection ) LocalSelectionTransfer.getTransfer().getSelection() ).getFirstElement();
                    // if ( dragSource instanceof Literal ||
                    // ( dragSource instanceof Model && root( ( Model ) dragSource ).equals( mapperConfig.getSourceModel() ) ) )
                    // source2Text.setBackground( getDisplay().getSystemColor( SWT.COLOR_BLUE ) );
                    // }
                    //
                    // @Override
                    // public void dragLeave( final DropTargetEvent event ) {
                    // source2Text.setBackground( background );
                    // }
                    //
                    // @Override
                    // public void drop( final DropTargetEvent event ) {
                    // final Object dragSource =
                    // ( ( IStructuredSelection ) LocalSelectionTransfer.getTransfer().getSelection() ).getFirstElement();
                    // MappingOperation< ?, ? > op = ( MappingOperation< ?, ? > ) sourceEntryPane.getData( OP_PROPERTY );
                    // mapperConfig.removeMapping( op );
                    // if ( dragSource instanceof Literal ) {
                    // final Literal literal = ( Literal ) dragSource;
                    // // op = mapperConfig.map( literal, ( Model ) op.getTarget() );
                    // source2Text.setText( "\"" + literal.getValue() + "\"" );
                    // } else {
                    // final Model model = ( Model ) dragSource;
                    // // op = mapperConfig.map( model, ( Model ) op.getTarget() );
                    // source2Text.setText( model.getName() );
                    // }
                    // // save();
                    // sourceEntryPane.setData( OP_PROPERTY, op );
                    // }
                    //
                    // private Model root( final Model model ) {
                    // return model.getParent() == null ? model : root( model.getParent() );
                    // }
                    // } );
                    //
                    // targetTraversalListener.prevText = source2Text;
                    newOpEndLabel( sourceEntryPane ).moveBelow( sourceText ); // moveBelow( source2Text );
                    addDeleteLabel.setData( IMAGE_PROPERTY, DELETE_IMAGE );
                    addDeleteLabel.setToolTipText( DELETE_CUSTOM_OPERATION_TOOL_TIP );
                    // ( ( GridLayout ) sourceEntryPane.getLayout() ).numColumns += 4;
                    ( ( GridLayout ) sourceEntryPane.getLayout() ).numColumns += 2;
                    sourceEntryPane.layout();
                    // source2Text.setFocus();
                    final FieldMapping mapping = ( FieldMapping ) sourceEntryPane.getData( MAPPING_PROPERTY );
                    mapperConfig.removeMapping( mapping );
                    sourceEntryPane.setData( MAPPING_PROPERTY,
                                             mapperConfig.customizeMapping( mapping,
                                                                            dlg.type.getFullyQualifiedName(),
                                                                            dlg.method.getElementName() ) );
                    save();
                } else if ( img == DELETE_IMAGE ) {
                    final CustomMapping mapping = ( CustomMapping ) sourceEntryPane.getData( MAPPING_PROPERTY );
                    mapperConfig.removeMapping( mapping );
                    sourceEntryPane.setData( MAPPING_PROPERTY, mapperConfig.map( mapping.getSource(), mapping.getTarget() ) );
                    save();
                    // source2Text.removeTraverseListener( source2TraversalListener );
                    // sourceTraversalListener.nextText = targetText;
                    // targetTraversalListener.prevText = sourceText;
                    ( ( Label ) sourceEntryPane.getData( OP_START_PROPERTY ) ).dispose();
                    ( ( Label ) sourceEntryPane.getData( OP_END_PROPERTY ) ).dispose();
                    sourceEntryPane.setData( OP_START_PROPERTY, null );
                    sourceEntryPane.setData( OP_END_PROPERTY, null );
                    addDeleteLabel.setData( IMAGE_PROPERTY, ADD_IMAGE );
                    addDeleteLabel.setToolTipText( ADD_CUSTOM_OPERATION_TOOL_TIP );
                    // ( ( GridLayout ) sourceEntryPane.getLayout() ).numColumns -= 4;
                    ( ( GridLayout ) sourceEntryPane.getLayout() ).numColumns -= 2;
                    sourceEntryPane.layout();
                    // sourceText.setFocus();
                }
            }
        } );

        final DropTarget sourceDropTarget = new DropTarget( sourceText, DND.DROP_MOVE );
        sourceDropTarget.setTransfer( new Transfer[] { LocalSelectionTransfer.getTransfer() } );
        sourceDropTarget.addDropListener( new DropListener( sourceText, mapperConfig.getSourceModel(), sourceEntryPane ) {

            @Override
            MappingOperation< ?, ? > drop( final Object dragSource,
                                           final MappingOperation< ?, ? > mapping ) {
                if ( dragSource instanceof Literal ) {
                    final Literal literal = ( Literal ) dragSource;
                    sourceText.setText( "\"" + literal.getValue() + "\"" );
                    return mapperConfig.map( literal, ( Model ) mapping.getTarget() );
                }
                final Model model = ( Model ) dragSource;
                sourceText.setText( model.getName() );
                final FieldMapping fieldMapping = mapperConfig.map( model, ( Model ) mapping.getTarget() );
                if ( mapping instanceof CustomMapping ) {
                    final CustomMapping customMapping = ( CustomMapping ) mapping;
                    return mapperConfig.customizeMapping( fieldMapping,
                                                          customMapping.getMappingClass(), customMapping.getMappingOperation() );
                }
                return fieldMapping;
            }

            @Override
            boolean valid( final Object dragSource ) {
                return super.valid( dragSource ) || dragSource instanceof Literal;
            }
        } );

        final DropTarget targetDropTarget = new DropTarget( targetText, DND.DROP_MOVE );
        targetDropTarget.setTransfer( new Transfer[] { LocalSelectionTransfer.getTransfer() } );
        targetDropTarget.addDropListener( new DropListener( targetText, mapperConfig.getTargetModel(), sourceEntryPane ) {

            @Override
            MappingOperation< ?, ? > drop( final Object dragSource,
                                           final MappingOperation< ?, ? > mapping ) {
                final Model dragModel = ( Model ) dragSource;
                targetText.setText( dragModel.getName() );
                if ( mapping.getSource() instanceof Literal ) return mapperConfig.map( ( Literal ) mapping.getSource(), dragModel );
                return mapperConfig.map( ( Model ) mapping.getSource(), dragModel );
            }
        } );

        prevTargetText = targetText;
        prevTraversalListener = targetTraversalListener;

        xformsPane.layout();
        scroller.setMinSize( xformsPane.computeSize( SWT.DEFAULT, SWT.DEFAULT ) );
    }

    /**
     * If we changed, fire a changed event.
     *
     * @param source
     */
    protected void fireChangedEvent( final Object source ) {
        final ChangeEvent e = new ChangeEvent( source );
        // inform any listeners of the resize event
        final Object[] listeners = this._changeListeners.getListeners();
        for ( int i = 0; i < listeners.length; ++i ) {
            ( ( ChangeListener ) listeners[ i ] ).stateChanged( e );
        }
    }

    void map( final Literal literal,
              final Model targetModel ) {
        createMapping( mapperConfig.map( literal, targetModel ) );
        save();
    }

    void map( final Model sourceModel,
              final Model targetModel ) {
        createMapping( mapperConfig.map( sourceModel, targetModel ) );
        save();
    }

    private boolean modelsEqual( final Model left,
                                 final Model right ) {
        if ( left == null && right == null ) { return true; }
        if ( left == null || right == null ) { return false; }
        if ( left.getName().equals( right.getName() ) ) {
            if ( left.getType().equals( right.getType() ) ) { return true; }
        }
        return false;
    }

    Label newOpEndLabel( final Composite sourceEntryPane ) {
        final Label label = new Label( sourceEntryPane, SWT.NONE );
        label.setText( ")" );
        sourceEntryPane.setData( OP_END_PROPERTY, label );
        return label;
    }

    Label newOpStartLabel( final Composite sourceEntryPane,
                           final String name,
                           final MouseTrackListener mouseOverListener ) {
        final Label label = new Label( sourceEntryPane, SWT.NONE );
        label.setText( name + "(" );
        // Configure op button to appear on mouse-over of op
        label.addMouseTrackListener( mouseOverListener );
        sourceEntryPane.setData( OP_START_PROPERTY, label );
        return label;
    }

    /**
     * Remove a change listener.
     *
     * @param listener
     *            old listener
     */
    public void removeChangeListener( final ChangeListener listener ) {
        this._changeListeners.remove( listener );
    }

    boolean save() {
        try ( FileOutputStream stream = new FileOutputStream( new File( configFile.getLocationURI() ) ) ) {
            mapperConfig.saveConfig( stream );
            configFile.getProject().refreshLocal( IResource.DEPTH_INFINITE, null );
            if ( sourcehistory != null && sourcehistory.size() > 0 || targethistory != null
                 && targethistory.size() > 0 ) {
                Model initialSource = null;
                Model lastSource = null;
                if ( sourcehistory != null && sourcehistory.size() > 0 ) {
                    initialSource = sourcehistory.firstElement();
                    lastSource = sourcehistory.lastElement();
                }
                Model initialTarget = null;
                Model lastTarget = null;
                if ( targethistory != null && targethistory.size() > 0 ) {
                    initialTarget = targethistory.firstElement();
                    lastTarget = targethistory.lastElement();
                }

                boolean needCamelUpdate = false;
                if ( !modelsEqual( initialSource, lastSource ) ) {
                    // update the sourceModel
                    System.out.println( "Updated endpoint uri sourceModel to " + lastSource.getName() );
                    needCamelUpdate = true;
                }
                if ( !modelsEqual( initialTarget, lastTarget ) ) {
                    // update the targetModel
                    System.out.println( "Updated endpoint uri targetModel to " + lastTarget.getName() );
                    needCamelUpdate = true;
                }

                if ((endpointID == null || camelFilePath == null) && needCamelUpdate) {
                    CamelEndpointSelectionDialog dialog = 
                            new CamelEndpointSelectionDialog(getShell(), configFile.getProject(), camelFilePath);
                    if (dialog.open() == Window.OK) {
                        endpointID = dialog.getEndpointID();
                        camelFilePath = dialog.getCamelFilePath();
                    }
                }

                if (endpointID != null && camelFilePath != null && needCamelUpdate) {

                    IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
                    IFile file = root.getFileForLocation(configFile.getLocation());
                    IProject project = file.getProject();
                    File camelFile = new File(project.getFile(camelFilePath).getLocationURI());
                    CamelConfigBuilder builder = CamelConfigBuilder.loadConfig(camelFile);
                    CamelEndpointFactoryBean theEndpoint = builder.getEndpoint(endpointID);

                    if ( theEndpoint != null ) {
                        System.out.println( "Found endpoint: " + endpointID );
                        // update the endpoint's URI with the source and target
                        if ( !modelsEqual( initialSource, lastSource ) ) {
                            // update the sourceModel
                            EndpointHelper.setSourceModel( theEndpoint, lastSource.getType() );
                            System.out.println( "Updated endpoint uri sourceModel to " + lastSource.getType() );
                        }
                        if ( !modelsEqual( initialTarget, lastTarget ) ) {
                            // update the targetModel
                            EndpointHelper.setTargetModel( theEndpoint, lastTarget.getType() );
                            System.out.println( "Updated endpoint uri targetModel to " + lastTarget.getType() );
                        }

                        System.out.println( "Now saving the camel file..." );
                        try ( FileOutputStream camelConfigStream = new FileOutputStream( camelFile ) ) {
                            builder.saveConfig( camelConfigStream );
                            project.refreshLocal( IResource.DEPTH_INFINITE, null );
                        } catch ( final Exception e ) {
                            Activator.error( getShell(), e );
                        }
                    } else {
                        MessageDialog.openError( getShell(),
                                                 "Camel Endpoint Not Found",
                                                 "No endpoint named '" + endpointID + "' was found in the Camel context." );
                    }
                }

                // now reset the history
                if ( !modelsEqual( initialSource, lastSource ) ) {
                    System.out.println( "Resetting source history" );
                    sourcehistory.clear();
                    sourcehistory.push( lastSource );
                }
                if ( !modelsEqual( initialTarget, lastTarget ) ) {
                    System.out.println( "Resetting target history" );
                    targethistory.clear();
                    targethistory.push( lastSource );
                }
            }
            fireChangedEvent( this );
            return true;
        } catch ( final Exception e ) {
            Activator.error( getShell(), e );
            return false;
        }
    }

    private final class AddCustomOperationDialog extends TitleAreaDialog {

        IType type;
        IMethod method;

        AddCustomOperationDialog( final Shell shell ) {
            super( shell );
        }

        @Override
        public void create() {
            super.create();
            getButton( IDialogConstants.OK_ID ).setEnabled( false );
        }

        @Override
        protected Control createDialogArea( final Composite parent ) {
            setTitle( "Add Custom Operation" );
            setMessage( "Select the Java class and method that implements the custom operation" );
            setHelpAvailable( false );
            final Composite area = new Composite( parent, SWT.NONE );
            area.setLayout( GridLayoutFactory.swtDefaults().numColumns( 2 ).create() );
            area.setLayoutData( GridDataFactory.fillDefaults().grab( true, true ).create() );
            Label label = new Label( area, SWT.NONE );
            label.setText( "Class:" );
            final Button classButton = new Button( area, SWT.NONE );
            classButton.setLayoutData( GridDataFactory.fillDefaults().grab( true, false ).create() );
            classButton.setAlignment( SWT.LEFT );
            classButton.setText( "< Click to select class >" );
            label = new Label( area, SWT.NONE );
            label.setText( "Method:" );
            final ComboViewer methodComboViewer = new ComboViewer( area, SWT.READ_ONLY );
            methodComboViewer.getCombo().setLayoutData( GridDataFactory.fillDefaults().grab( true, false ).create() );
            methodComboViewer.setContentProvider( new ArrayContentProvider() );
            methodComboViewer.setLabelProvider( new LabelProvider() {

                @Override
                public String getText( final Object element ) {
                    final IMethod method = ( IMethod ) element;
                    try {
                        final StringBuilder builder = new StringBuilder();
                        builder.append( Signature.getSignatureSimpleName( method.getReturnType() ) );
                        builder.append( " " );
                        builder.append( method.getElementName() );
                        builder.append( "(" );
                        final String[] types = method.getParameterTypes();
                        final String[] names = method.getParameterNames();
                        boolean hasPrm = false;
                        for ( int ndx = 0; ndx < types.length; ndx++ ) {
                            if ( hasPrm ) builder.append( ", " );
                            else {
                                builder.append( " " );
                                hasPrm = true;
                            }
                            builder.append( Signature.getSignatureSimpleName( types[ ndx ] ) );
                            builder.append( " " );
                            builder.append( names[ ndx ] );
                        }
                        if ( hasPrm ) builder.append( " " );
                        builder.append( ")" );
                        return builder.toString();
                    } catch ( final JavaModelException e ) {
                        return "";
                    }
                }
            } );
            methodComboViewer.setComparator( new ViewerComparator() {

                @Override
                public int compare( final Viewer viewer,
                                    final Object object1,
                                    final Object object2 ) {
                    final IMethod method1 = ( IMethod ) object1;
                    final IMethod method2 = ( IMethod ) object2;
                    int comparison = method1.getElementName().compareTo( method2.getElementName() );
                    if ( comparison != 0 ) return comparison;
                    final String[] types1 = method1.getParameterTypes();
                    final String[] types2 = method2.getParameterTypes();
                    comparison = types1.length - types2.length;
                    if ( comparison != 0 ) return comparison;
                    for ( int ndx = 0; ndx < types1.length; ndx++ ) {
                        comparison =
                            Signature.getSignatureSimpleName( types1[ ndx ] ).compareTo( Signature.getSignatureSimpleName( types2[ ndx ] ) );
                        if ( comparison != 0 ) return comparison;
                    }
                    return 0;
                }
            } );
            methodComboViewer.addSelectionChangedListener( new ISelectionChangedListener() {

                @Override
                public void selectionChanged( final SelectionChangedEvent event ) {
                    methodSelected( methodComboViewer );
                }
            } );
            classButton.addSelectionListener( new SelectionAdapter() {

                @Override
                public void widgetSelected( final SelectionEvent event ) {
                    selectClass( classButton, methodComboViewer );
                }
            } );
            return area;
        }

        @Override
        protected int getShellStyle() {
            return super.getShellStyle() | SWT.RESIZE;
        }

        void methodSelected( final ComboViewer methodComboViewer ) {
            method = ( IMethod ) ( ( IStructuredSelection ) methodComboViewer.getSelection() ).getFirstElement();
        }

        void selectClass( final Button classButton,
                          final ComboViewer methodComboViewer ) {
            final Util.Filter filter = new Util.Filter() {

                @Override
                public boolean accept( final IType type ) {
                    try {
                        for ( final IMethod method : type.getMethods() ) {
                            if ( valid( method ) ) return true;
                        }
                    } catch ( final JavaModelException ignored ) {}
                    return false;
                }
            };
            final IType type = Util.selectClass( getShell(), configFile.getProject(), filter );
            if ( type == null ) return;
            try {
                classButton.setText( type.getFullyQualifiedName() );
                final List< IMethod > methods = new ArrayList<>( Arrays.asList( type.getMethods() ) );
                for ( final Iterator< IMethod > iter = methods.iterator(); iter.hasNext(); ) {
                    if ( !valid( iter.next() ) ) iter.remove();
                }
                methodComboViewer.setInput( methods.toArray() );
                if ( !methods.isEmpty() ) methodComboViewer.setSelection( new StructuredSelection( methods.get( 0 ) ) );
                this.type = type;
                getButton( IDialogConstants.OK_ID ).setEnabled( true );
            } catch ( final JavaModelException e ) {
                Activator.error( getShell(), e );
            }
        }

        boolean valid( final IMethod method ) {
            try {
                return !Signature.getSignatureSimpleName( method.getReturnType() ).equals( "void" )
                       && method.getParameters().length == 1;
            } catch ( final JavaModelException e ) {
                return false;
            }
        }
    }

    private abstract class DropListener extends DropTargetAdapter {

        private final Text dropText;
        private final Model dragRootModel;
        private final Composite sourceEntryPane;

        DropListener( final Text dropText,
                      final Model dragRootModel,
                      final Composite sourceEntryPane ) {
            this.dropText = dropText;
            this.dragRootModel = dragRootModel;
            this.sourceEntryPane = sourceEntryPane;
        }

        @Override
        public final void dragEnter( final DropTargetEvent event ) {
            if ( valid( dragSource() ) ) dropText.setBackground( getDisplay().getSystemColor( SWT.COLOR_BLUE ) );
        }

        @Override
        public final void dragLeave( final DropTargetEvent event ) {
            dropText.setBackground( textBackground );
        }

        private Object dragSource() {
            return ( ( IStructuredSelection ) LocalSelectionTransfer.getTransfer().getSelection() ).getFirstElement();
        }

        @Override
        public final void drop( final DropTargetEvent event ) {
            MappingOperation< ?, ? > mapping = ( MappingOperation< ?, ? > ) sourceEntryPane.getData( MAPPING_PROPERTY );
            mapperConfig.removeMapping( mapping );
            mapping = drop( dragSource(), mapping );
            save();
            sourceEntryPane.setData( MAPPING_PROPERTY, mapping );
            sourceEntryPane.layout();
        }

        abstract MappingOperation< ?, ? > drop( final Object dragSource,
                                                MappingOperation< ?, ? > mapping );

        private Model root( final Model model ) {
            return model.getParent() == null ? model : root( model.getParent() );
        }

        boolean valid( final Object dragSource ) {
            return dragSource instanceof Model && root( ( Model ) dragSource ).equals( dragRootModel );
        }
    }

    private final class TraversalListener implements TraverseListener {

        Text prevText;
        Text nextText;

        TraversalListener( final Text prevText,
                           final Text nextText ) {
            this.prevText = prevText;
            this.nextText = nextText;
        }

        @Override
        public void keyTraversed( final TraverseEvent event ) {
            if ( event.detail == SWT.TRAVERSE_TAB_NEXT ) {
                if ( nextText != null ) {
                    event.detail = SWT.TRAVERSE_NONE;
                    event.doit = false;
                    nextText.setFocus();
                }
            } else if ( event.detail == SWT.TRAVERSE_TAB_PREVIOUS ) {
                if ( prevText != null ) {
                    event.detail = SWT.TRAVERSE_NONE;
                    event.doit = false;
                    prevText.setFocus();
                }
            }
        }
    }
}
