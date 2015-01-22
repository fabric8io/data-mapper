package org.jboss.mapper.eclipse;

import java.io.IOException;
import java.io.InputStream;
import java.text.StringCharacterIterator;
import java.util.Arrays;

import org.eclipse.core.databinding.Binding;
import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.BeanProperties;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.core.databinding.property.Properties;
import org.eclipse.core.databinding.validation.IValidator;
import org.eclipse.core.databinding.validation.ValidationStatus;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationSupport;
import org.eclipse.jface.databinding.fieldassist.ControlDecorationUpdater;
import org.eclipse.jface.databinding.swt.WidgetProperties;
import org.eclipse.jface.databinding.viewers.ObservableListContentProvider;
import org.eclipse.jface.databinding.viewers.ViewerProperties;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.jboss.mapper.camel.config.CamelEndpointFactoryBean;
import org.jboss.mapper.eclipse.DataMappingWizard.Model;
import org.jboss.mapper.eclipse.DataMappingWizard.ModelType;
import org.jboss.mapper.eclipse.util.Util;

// TODO chain source and target path validators together
class NewTransformationWizardPage extends WizardPage {

    final Model model;

    NewTransformationWizardPage( Model model ) {
        super( "New Data Mapping", "New Data Mapping", Activator.imageDescriptor( "transform.png" ) );
        this.model = model;
    }

    /**
     * {@inheritDoc}
     *
     * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createControl( final Composite parent ) {
        setDescription( "Supply the ID, project, and path for the the new transformation.\n" +
                        "Optionally, supply the source and target files for the transformation." );
        final Composite page = new Composite( parent, SWT.NONE );
        setControl( page );
        page.setLayout( GridLayoutFactory.swtDefaults().spacing( 0, 5 ).numColumns( 3 ).create() );

        final DataBindingContext context = new DataBindingContext();

        // Create project widgets
        Label label = new Label( page, SWT.NONE );
        label.setText( "Project:" );
        label.setToolTipText( "The project that will contain the mapping file." );
        final ComboViewer projectViewer = new ComboViewer( new Combo( page, SWT.READ_ONLY ) );
        projectViewer.getCombo().setLayoutData( GridDataFactory.swtDefaults()
                                                               .grab( true, false )
                                                               .span( 2, 1 )
                                                               .align( SWT.FILL, SWT.CENTER )
                                                               .create() );
        projectViewer.getCombo().setToolTipText( label.getToolTipText() );
        projectViewer.setLabelProvider( new LabelProvider() {

            @Override
            public String getText( final Object element ) {
                return ( ( IProject ) element ).getName();
            }
        } );

        // Create ID widgets
        label = new Label( page, SWT.NONE );
        label.setText( "ID:" );
        label.setToolTipText( "The transformation ID that will be shown in the Fuse editor" );
        final Text idText = new Text( page, SWT.BORDER );
        idText.setLayoutData( GridDataFactory.swtDefaults().span( 2, 1 ).grab( true, false )
                                             .align( SWT.FILL, SWT.CENTER ).create() );
        idText.setToolTipText( label.getToolTipText() );

        // Create file path widgets
        label = new Label( page, SWT.NONE );
        label.setText( "File path:" );
        final Text pathText = new Text( page, SWT.BORDER );
        pathText.setLayoutData( GridDataFactory.swtDefaults().span( 2, 1 ).grab( true, false )
                                               .align( SWT.FILL, SWT.CENTER ).create() );
        pathText.setToolTipText( label.getToolTipText() );

        // Create source widgets
        Group group = new Group( page, SWT.SHADOW_ETCHED_IN );
        Label fileLabel = new Label( group, SWT.NONE );
        final Text sourcePathText = new Text( group, SWT.BORDER );
        final Button sourcePathButton = new Button( group, SWT.NONE );
        Label typeLabel = new Label( group, SWT.NONE );
        final ComboViewer sourceTypeViewer = new ComboViewer( new Combo( group, SWT.READ_ONLY ) );
        createFileControls( group, fileLabel, "Source", sourcePathText, sourcePathButton, typeLabel, sourceTypeViewer );

        // Create target widgets
        group = new Group( page, SWT.SHADOW_ETCHED_IN );
        fileLabel = new Label( group, SWT.NONE );
        final Text targetPathText = new Text( group, SWT.BORDER );
        final Button targetPathButton = new Button( group, SWT.NONE );
        typeLabel = new Label( group, SWT.NONE );
        final ComboViewer targetTypeViewer = new ComboViewer( new Combo( group, SWT.READ_ONLY ) );
        createFileControls( group, fileLabel, "Target", targetPathText, targetPathButton, typeLabel, targetTypeViewer );

        // Bind project widget to UI model
        projectViewer.setContentProvider( new ObservableListContentProvider() );
        IObservableValue widgetValue = ViewerProperties.singleSelection().observe( projectViewer );
        IObservableValue modelValue = BeanProperties.value( Model.class, "project" ).observe( model );
        UpdateValueStrategy strategy = new UpdateValueStrategy();
        strategy.setBeforeSetValidator( new IValidator() {

            @Override
            public IStatus validate( Object value ) {
                if ( value == null ) {
                    sourcePathButton.setEnabled( false );
                    targetPathButton.setEnabled( false );
                    return ValidationStatus.error( "A project must be selected" );
                }
                sourcePathButton.setEnabled( true );
                targetPathButton.setEnabled( true );
                return ValidationStatus.ok();
            }
        } );
        ControlDecorationSupport.create( context.bindValue( widgetValue, modelValue, strategy, null ), SWT.LEFT );
        projectViewer.setInput( Properties.selfList( IProject.class ).observe( model.projects ) );

        // Bind transformation ID widget to UI model
        widgetValue = WidgetProperties.text( SWT.Modify ).observe( idText );
        modelValue = BeanProperties.value( Model.class, "id" ).observe( model );
        strategy = new UpdateValueStrategy();
        strategy.setBeforeSetValidator( new IValidator() {

            @Override
            public IStatus validate( Object value ) {
                if ( value == null || value.toString().trim().isEmpty() )
                    return ValidationStatus.error( "A transformation ID must be supplied" );
                final String id = value.toString().trim();
                final StringCharacterIterator iter = new StringCharacterIterator( id );
                for ( char chr = iter.first(); chr != StringCharacterIterator.DONE; chr = iter.next() ) {
                    if ( !Character.isJavaIdentifierPart( chr ) )
                        return ValidationStatus.error( "The transformation ID may only contain letters, digits, currency symbols, or underscores" );
                }
                for ( final CamelEndpointFactoryBean bean : model.camelConfigBuilder.getCamelContext().getEndpoint() ) {
                    if ( id.equalsIgnoreCase( bean.getId() ) )
                        return ValidationStatus.error( "A transformation with the supplied ID already exists" );
                }
                return ValidationStatus.ok();
            }
        } );
        ControlDecorationSupport.create( context.bindValue( widgetValue, modelValue, strategy, null ), SWT.LEFT );

        // Bind file path widget to UI model
        widgetValue = WidgetProperties.text( SWT.Modify ).observe( pathText );
        modelValue = BeanProperties.value( Model.class, "filePath" ).observe( model );
        strategy = new UpdateValueStrategy();
        strategy.setBeforeSetValidator( new IValidator() {

            @Override
            public IStatus validate( Object value ) {
                return value == null || value.toString().trim().isEmpty()
                                ? ValidationStatus.error( "The transformation file path must be supplied" )
                                : ValidationStatus.ok();
            }
        } );
        ControlDecorationSupport.create( context.bindValue( widgetValue, modelValue, strategy, null ), SWT.LEFT );

        final ControlDecorationUpdater sourceUpdator = new ControlDecorationUpdater();
        final ControlDecorationUpdater targetUpdator = new ControlDecorationUpdater();

        // Bind source file path widget to UI model
        widgetValue = WidgetProperties.text( SWT.Modify ).observe( sourcePathText );
        modelValue = BeanProperties.value( Model.class, "sourceFilePath" ).observe( model );
        strategy = new UpdateValueStrategy();
        strategy.setBeforeSetValidator( new IValidator() {

            @Override
            public IStatus validate( Object value ) {
                if ( model.getTargetFilePath() != null ) {
                    final String path = value == null ? null : value.toString().trim();
                    if ( path == null || path.isEmpty() )
                        return ValidationStatus.error( "A source file path must be supplied for the supplied target file path" );
                    if ( model.getProject().findMember( path ) == null )
                        return ValidationStatus.error( "Unable to find a source file with the supplied path" );
                }
                return ValidationStatus.ok();
            }
        } );
        ControlDecorationSupport.create( context.bindValue( widgetValue, modelValue, strategy, null ), SWT.LEFT, null, sourceUpdator );

        // Bind target file path widget to UI model
        widgetValue = WidgetProperties.text( SWT.Modify ).observe( targetPathText );
        modelValue = BeanProperties.value( Model.class, "targetFilePath" ).observe( model );
        strategy = new UpdateValueStrategy();
        strategy.setBeforeSetValidator( new IValidator() {

            @Override
            public IStatus validate( Object value ) {
                if ( model.getSourceFilePath() != null ) {
                    final String path = value == null ? null : value.toString().trim();
                    if ( path == null || path.isEmpty() )
                        return ValidationStatus.error( "A target file path must be supplied for the supplied source file path" );
                    if ( model.getProject().findMember( path ) == null )
                        return ValidationStatus.error( "Unable to find a target file with the supplied path" );
                }
                return ValidationStatus.ok();
            }
        } );
        ControlDecorationSupport.create( context.bindValue( widgetValue, modelValue, strategy, null ), SWT.LEFT, null, targetUpdator );

        // Bind source type widget to UI model
        sourceTypeViewer.setContentProvider( new ObservableListContentProvider() );
        widgetValue = ViewerProperties.singleSelection().observe( sourceTypeViewer );
        modelValue = BeanProperties.value( Model.class, "sourceType" ).observe( model );
        context.bindValue( widgetValue, modelValue );
        sourceTypeViewer.setInput( Properties.selfList( ModelType.class ).observe( Arrays.asList( ModelType.values() ) ) );

        // Bind target type widget to UI model
        targetTypeViewer.setContentProvider( new ObservableListContentProvider() );
        widgetValue = ViewerProperties.singleSelection().observe( targetTypeViewer );
        modelValue = BeanProperties.value( Model.class, "targetType" ).observe( model );
        context.bindValue( widgetValue, modelValue );
        targetTypeViewer.setInput( Properties.selfList( ModelType.class ).observe( Arrays.asList( ModelType.values() ) ) );

        // Set focus to appropriate control
        page.addPaintListener( new PaintListener() {

            @Override
            public void paintControl( final PaintEvent event ) {
                if ( model.getProject() == null ) projectViewer.getCombo().setFocus();
                else idText.setFocus();
                page.removePaintListener( this );
            }
        } );

        for ( Object observable : context.getValidationStatusProviders() ) {
            ( ( Binding ) observable ).getTarget().addChangeListener( new IChangeListener() {

                @Override
                public void handleChange( ChangeEvent event ) {
                    validatePage();
                }
            } );
        }

        if ( model.getProject() == null ) validatePage();
        else projectViewer.setSelection( new StructuredSelection( model.getProject() ) );
    }

    void createFileControls( final Group group,
                             final Label pathLabel,
                             final String schemaType,
                             final Text pathText,
                             final Button pathButton,
                             final Label typeLabel,
                             final ComboViewer typeComboViewer ) {
        group.setLayoutData( GridDataFactory.swtDefaults()
                                            .grab( true, false )
                                            .span( 3, 1 )
                                            .align( SWT.FILL, SWT.CENTER )
                                            .create() );
        group.setLayout( GridLayoutFactory.swtDefaults().spacing( 0, 5 ).numColumns( 3 ).create() );
        group.setText( schemaType + " File" );
        pathLabel.setText( "File path:" );
        pathText.setLayoutData( GridDataFactory.swtDefaults().grab( true, false ).align( SWT.FILL, SWT.CENTER ).create() );
        pathButton.setText( "..." );
        typeLabel.setText( "Type:" );
        typeComboViewer.getCombo().setLayoutData( GridDataFactory.swtDefaults().span( 2, 1 ).grab( true, false ).create() );
        pathButton.addSelectionListener( new SelectionAdapter() {

            @Override
            public void widgetSelected( final SelectionEvent event ) {
                final String path = Util.selectFile( getShell(), model.getProject(), schemaType, pathText );
                if ( path != null ) {
                    pathText.setText( path );
                    if ( typeComboViewer.getSelection().isEmpty() ) {
                        final String ext = path.substring( path.lastIndexOf( '.' ) + 1 ).toLowerCase();
                        switch ( ext ) {
                            case "class":
                                typeComboViewer.setSelection( new StructuredSelection( ModelType.CLASS ) );
                                break;
                            case "java":
                                typeComboViewer.setSelection( new StructuredSelection( ModelType.JAVA ) );
                                break;
                            case "json":
                                try ( InputStream stream = model.getProject().getFile( path ).getContents() ) {
                                    char quote = '\0';
                                    final StringBuilder builder = new StringBuilder();
                                    ModelType type = ModelType.JSON;
                                    for ( char chr = ( char ) stream.read(); chr != -1; chr = ( char ) stream.read() ) {
                                        // Find quote
                                        if ( quote == '\0' ) {
                                            if ( chr == '"' || chr == '\'' ) quote = chr;
                                        } else if ( chr == quote ) {
                                            final String keyword = builder.toString();
                                            switch ( keyword ) {
                                                case "$schema":
                                                case "title":
                                                case "type":
                                                case "id":
                                                    type = ModelType.JSON_SCHEMA;
                                            }
                                            break;
                                        }
                                        else builder.append( chr );
                                    }
                                    typeComboViewer.setSelection( new StructuredSelection( type ) );
                                } catch ( IOException | CoreException e ) {
                                    Activator.error( getShell(), e );
                                    typeComboViewer.setSelection( new StructuredSelection( ModelType.JSON ) );
                                }
                                break;
                            case "xml":
                                typeComboViewer.setSelection( new StructuredSelection( ModelType.XML ) );
                                break;
                            case "xsd":
                                typeComboViewer.setSelection( new StructuredSelection( ModelType.XSD ) );
                                break;
                            default:
                                break;
                        }
                    }
                }
            }
        } );
    }

    void validatePage() {
        setPageComplete( model.getProject() != null && model.getId() != null && model.getFilePath() != null &&
                         ( ( model.getSourceFilePath() == null && model.getTargetFilePath() == null ) ||
                         ( model.getSourceFilePath() != null && model.getTargetFilePath() != null ) ) );
    }
}
