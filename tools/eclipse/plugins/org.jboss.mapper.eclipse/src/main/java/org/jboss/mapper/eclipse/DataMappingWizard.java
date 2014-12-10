package org.jboss.mapper.eclipse;

import java.io.File;
import java.io.FileOutputStream;
import java.text.StringCharacterIterator;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ResourceSelectionDialog;
import org.eclipse.ui.part.FileEditorInput;
import org.jboss.mapper.TransformType;
import org.jboss.mapper.camel.CamelConfigBuilder;
import org.jboss.mapper.camel.config.CamelEndpointFactoryBean;
import org.jboss.mapper.dozer.ConfigBuilder;
import org.jboss.mapper.model.json.JsonModelGenerator;
import org.jboss.mapper.model.xml.XmlModelGenerator;

import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JPackage;

/**
 * 
 */
public class DataMappingWizard extends Wizard implements INewWizard {
    
    static final String MAIN_PATH = "src/main/";
    static final String JAVA_PATH = MAIN_PATH + "java/";
    static final String RESOURCES_PATH = MAIN_PATH + "resources/";
    static final String CAMEL_CONFIG_PATH = RESOURCES_PATH + "META-INF/spring/camel-context.xml";
    
    static final String DEFAULT_DOZER_CONFIG_FILE_NAME = "dozerBeanMapping.xml";
    
    static String selectSchema( final Shell shell,
                                final IProject project,
                                final String schemaType ) {
        final IFolder resourcesFolder = project.getFolder( MAIN_PATH );
        try {
            final ResourceSelectionDialog dlg = new ResourceSelectionDialog( shell, resourcesFolder, "Select " + schemaType );
            if ( dlg.open() == Window.OK )
                return ( ( IFile ) dlg.getResult()[ 0 ] ).getFullPath().makeRelativeTo( project.getFullPath() ).toString();
        } catch ( final Exception e ) {
            Activator.error( shell, e );
        }
        return null;
    }
    
    IProject project;
    Text idText;
    IFile dozerConfigFile;
    Text sourceFileText, targetFileText;
    Button sourceFileButton, targetFileButton;
    ComboViewer sourceTypeComboViewer, targetTypeComboViewer;
    File camelConfigFile;
    CamelConfigBuilder camelConfigBuilder;
    
    /**
     * 
     */
    public DataMappingWizard() {
        addPage( constructMainPage() );
    }
    
    private IWizardPage constructMainPage() {
        return new WizardPage( "New Data Mapping", "New Data Mapping", Activator.imageDescriptor( "transform.png" ) ) {
            
            Text dozerConfigFileText;
            
            /**
             * {@inheritDoc}
             * 
             * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
             */
            @Override
            public void createControl( final Composite parent ) {
                setDescription( "Supply the ID, project, and name for the the new mapping.\n" +
                                "Optionally, supply the source and target files to be mapped." );
                final Composite page = new Composite( parent, SWT.NONE );
                setControl( page );
                page.setLayout( GridLayoutFactory.swtDefaults().spacing( 0, 5 ).numColumns( 3 ).create() );
                // Create project controls
                Label label = new Label( page, SWT.NONE );
                label.setText( "Project:" );
                label.setToolTipText( "The project that will contain the mapping file." );
                final ComboViewer projectViewer = new ComboViewer( new Combo( page, SWT.READ_ONLY ) );
                projectViewer.getCombo().setLayoutData( GridDataFactory.swtDefaults()
                                                                       .grab( true, false )
                                                                       .span( 2, 1 )
                                                                       .align( SWT.FILL, SWT.CENTER )
                                                                       .create() );
                projectViewer.setLabelProvider( new LabelProvider() {
                    
                    @Override
                    public String getText( final Object element ) {
                        return ( ( IProject ) element ).getName();
                    }
                } );
                projectViewer.add( ResourcesPlugin.getWorkspace().getRoot().getProjects() );
                if ( project != null ) projectViewer.setSelection( new StructuredSelection( project ) );
                projectViewer.getCombo().addSelectionListener( new SelectionAdapter() {
                    
                    @Override
                    public void widgetSelected( final SelectionEvent event ) {
                        project = ( IProject ) ( ( IStructuredSelection ) projectViewer.getSelection() ).getFirstElement();
                        validatePage();
                    }
                } );
                // Create ID controls
                label = new Label( page, SWT.NONE );
                label.setText( "ID:" );
                label.setToolTipText( "The transform ID that will be shown in the Fuse editor" );
                idText = new Text( page, SWT.BORDER );
                idText.setLayoutData( GridDataFactory.swtDefaults().span( 2, 1 ).grab( true, false )
                                                     .align( SWT.FILL, SWT.CENTER ).create() );
                idText.addKeyListener( new KeyAdapter() {
                    
                    @Override
                    public void keyReleased( final KeyEvent event ) {
                        validatePage();
                    }
                } );
                // Create Dozer config controls
                label = new Label( page, SWT.NONE );
                label.setText( "File name:" );
                dozerConfigFileText = new Text( page, SWT.BORDER );
                dozerConfigFileText.setText( DEFAULT_DOZER_CONFIG_FILE_NAME );
                dozerConfigFileText.setLayoutData( GridDataFactory.swtDefaults().span( 2, 1 ).grab( true, false )
                                                                  .align( SWT.FILL, SWT.CENTER ).create() );
                dozerConfigFileText.addKeyListener( new KeyAdapter() {
                    
                    @Override
                    public void keyReleased( final KeyEvent event ) {
                        validatePage();
                    }
                } );
                // Create source controls
                Group group = new Group( page, SWT.SHADOW_ETCHED_IN );
                Label fileLabel = new Label( group, SWT.NONE );
                sourceFileText = new Text( group, SWT.BORDER );
                sourceFileButton = new Button( group, SWT.NONE );
                Label typeLabel = new Label( group, SWT.NONE );
                sourceTypeComboViewer = new ComboViewer( new Combo( group, SWT.READ_ONLY ) );
                createFileControls( group, fileLabel, "Source", sourceFileText, sourceFileButton, typeLabel, sourceTypeComboViewer );
                // Create target controls
                group = new Group( page, SWT.SHADOW_ETCHED_IN );
                fileLabel = new Label( group, SWT.NONE );
                targetFileText = new Text( group, SWT.BORDER );
                targetFileButton = new Button( group, SWT.NONE );
                typeLabel = new Label( group, SWT.NONE );
                targetTypeComboViewer = new ComboViewer( new Combo( group, SWT.READ_ONLY ) );
                createFileControls( group, fileLabel, "Target", targetFileText, targetFileButton, typeLabel, targetTypeComboViewer );
                // Set focus to appropriate control
                page.addPaintListener( new PaintListener() {
                    
                    @Override
                    public void paintControl( final PaintEvent event ) {
                        if ( project == null ) projectViewer.getCombo().setFocus();
                        else idText.setFocus();
                        page.removePaintListener( this );
                    }
                } );
                
                sourceFileButton.setEnabled( false );
                targetFileButton.setEnabled( false );
                setPageComplete( false );
            }
            
            void createFileControls( final Group group,
                                     final Label fileLabel,
                                     final String schemaType,
                                     final Text fileText,
                                     final Button fileButton,
                                     final Label typeLabel,
                                     final ComboViewer typeComboViewer ) {
                group.setLayoutData( GridDataFactory.swtDefaults()
                                                    .grab( true, false )
                                                    .span( 3, 1 )
                                                    .align( SWT.FILL, SWT.CENTER )
                                                    .create() );
                group.setLayout( GridLayoutFactory.swtDefaults().spacing( 0, 5 ).numColumns( 3 ).create() );
                group.setText( schemaType + " File" );
                fileLabel.setText( "Name:" );
                fileText.setLayoutData( GridDataFactory.swtDefaults().grab( true, false ).align( SWT.FILL, SWT.CENTER ).create() );
                fileText.addKeyListener( new KeyAdapter() {
                    
                    @Override
                    public void keyReleased( final KeyEvent event ) {
                        validatePage();
                    }
                } );
                fileButton.setText( "..." );
                typeLabel.setText( "Type:" );
                typeComboViewer.getCombo().setLayoutData( GridDataFactory.swtDefaults().span( 2, 1 ).grab( true, false ).create() );
                typeComboViewer.add( ModelType.values() );
                typeComboViewer.getCombo().addSelectionListener( new SelectionAdapter() {
                    
                    @Override
                    public void widgetSelected( final SelectionEvent event ) {
                        validatePage();
                    }
                } );
                fileButton.addSelectionListener( new SelectionAdapter() {
                    
                    @Override
                    public void widgetSelected( final SelectionEvent event ) {
                        final String name = selectSchema( getShell(), project, schemaType );
                        if ( name != null ) {
                            fileText.setText( name );
                            if ( typeComboViewer.getSelection().isEmpty() ) {
                                final String ext = name.substring( name.lastIndexOf( '.' ) + 1 ).toLowerCase();
                                switch ( ext ) {
                                    case "java":
                                        typeComboViewer.setSelection( new StructuredSelection( ModelType.JAVA ) );
                                        break;
                                    case "json":
                                        typeComboViewer.setSelection( new StructuredSelection( ModelType.JSON ) );
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
                        validatePage();
                    }
                } );
            }
            
            void validatePage() {
                setErrorMessage( null );
                setPageComplete( false );
                if ( project == null ) {
                    sourceFileButton.setEnabled( false );
                    targetFileButton.setEnabled( false );
                    setErrorMessage( "A project must be selected" );
                    return;
                }
                sourceFileButton.setEnabled( true );
                targetFileButton.setEnabled( true );
                final String id = idText.getText().trim();
                if ( id.isEmpty() ) {
                    setErrorMessage( "A mapping ID must be supplied" );
                    return;
                }
                final StringCharacterIterator iter = new StringCharacterIterator( id );
                for ( char chr = iter.first(); chr != StringCharacterIterator.DONE; chr = iter.next() ) {
                    if ( !Character.isUnicodeIdentifierPart( chr ) ) {
                        setErrorMessage( "The mapping ID contains an illegal character" );
                        return;
                    }
                }
                camelConfigFile = new File( project.getFile( CAMEL_CONFIG_PATH ).getLocationURI() );
                try {
                    camelConfigBuilder = CamelConfigBuilder.loadConfig( camelConfigFile );
                    for ( final CamelEndpointFactoryBean bean : camelConfigBuilder.getCamelContext().getEndpoint() ) {
                        if ( id.equalsIgnoreCase( bean.getId() ) ) {
                            setErrorMessage( "A mapping with the supplied ID already exists" );
                            return;
                        }
                    }
                } catch ( final Exception e ) {
                    Activator.error( getShell(), e );
                }
                String path = dozerConfigFileText.getText();
                if ( path.isEmpty() ) {
                    dozerConfigFile = null;
                    setErrorMessage( "The name of the mapping file must be supplied" );
                    return;
                }
                if ( !path.toLowerCase().endsWith( ".xml" ) ) path = path + ".xml";
                dozerConfigFile = project.getFile( RESOURCES_PATH + path );
                final String sourceFileName = sourceFileText.getText().trim();
                final String targetFileName = targetFileText.getText().trim();
                if ( sourceFileName.isEmpty() && targetFileName.isEmpty() ) {
                    setPageComplete( true );
                    return;
                }
                if ( !sourceFileName.isEmpty() && !targetFileName.isEmpty() ) {
                    if ( sourceTypeComboViewer.getSelection().isEmpty()
                         || targetTypeComboViewer.getSelection().isEmpty() ) {
                        setErrorMessage( "The types for both source and target files must be selected" );
                        return;
                    }
                    setPageComplete( true );
                    return;
                }
                setErrorMessage( "Source and target files must be selected in conjunction with each other" );
            }
        };
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
     */
    @Override
    public void init( final IWorkbench workbench,
                      final IStructuredSelection selection ) {
        if ( selection.size() != 1 ) {
            final IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
            if ( projects.length == 1 ) project = projects[ 0 ];
            return;
        }
        project = ( ( IResource ) ( ( IAdaptable ) selection.getFirstElement() ).getAdapter( IResource.class ) ).getProject();
        if ( project != null ) dozerConfigFile = project.getFile( RESOURCES_PATH + DEFAULT_DOZER_CONFIG_FILE_NAME );
    }
    
    private String generateModel( final String fileName,
                                  final ModelType type ) throws Exception {
        // Build class name from file name
        final StringBuilder className = new StringBuilder();
        final StringCharacterIterator iter =
            new StringCharacterIterator( fileName.substring( fileName.lastIndexOf( '/' ) + 1, fileName.lastIndexOf( '.' ) ) );
        boolean wordStart = true;
        for ( char chr = iter.first(); chr != StringCharacterIterator.DONE; chr = iter.next() ) {
            if ( className.length() == 0 ) {
                if ( Character.isJavaIdentifierStart( chr ) ) {
                    className.append( wordStart ? Character.toUpperCase( chr ) : chr );
                    wordStart = false;
                }
            } else if ( Character.isJavaIdentifierPart( chr ) ) {
                className.append( wordStart ? Character.toUpperCase( chr ) : chr );
                wordStart = false;
            } else wordStart = true;
        }
        // Build package name from class name
        int sequencer = 1;
        String pkgName = className.toString();
        while ( project.exists( new Path( JAVA_PATH + pkgName ) ) ) {
            pkgName = className.toString() + sequencer++;
        }
        // Generate model
        final File targetClassesFolder = new File( project.getFolder( JAVA_PATH ).getLocationURI() );
        switch ( type ) {
            case JSON: {
                final JsonModelGenerator generator = new JsonModelGenerator();
                generator.generateFromInstance( className.toString(),
                                                pkgName,
                                                project.findMember( fileName ).getLocationURI().toURL(),
                                                targetClassesFolder );
                return pkgName + "." + className;
            }
            case JSON_SCHEMA: {
                final JsonModelGenerator generator = new JsonModelGenerator();
                generator.generateFromSchema( className.toString(),
                                              pkgName,
                                              project.findMember( fileName ).getLocationURI().toURL(),
                                              targetClassesFolder );
                return pkgName + "." + className;
            }
            case XSD: {
                final XmlModelGenerator generator = new XmlModelGenerator();
                final JCodeModel model = generator.generateFromSchema( new File( project.findMember( fileName ).getLocationURI() ),
                                                                       pkgName,
                                                                       targetClassesFolder );
                for ( final Iterator< JPackage > pkgIter = model.packages(); pkgIter.hasNext(); ) {
                    final JPackage pkg = pkgIter.next();
                    for ( final Iterator< JDefinedClass > classIter = pkg.classes(); classIter.hasNext(); ) {
                        // TODO this only works when a single top-level class exists
                        final JDefinedClass definedClass = classIter.next();
                        return definedClass.fullName();
                    }
                }
                break;
            }
            default:
                break;
        }
        return null;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.wizard.Wizard#performFinish()
     */
    @Override
    public boolean performFinish() {
        // Save Dozer config
        if ( dozerConfigFile.exists() && !MessageDialog.openConfirm( getShell(), "Confirm", "Overwrite existing file?" ) )
            return false;
        final ConfigBuilder dozerConfigBuilder = ConfigBuilder.newConfig();
        try ( FileOutputStream dozerConfigStream = new FileOutputStream( new File( dozerConfigFile.getLocationURI() ) ) ) {
            final String sourceFileName = sourceFileText.getText().trim();
            final String targetFileName = targetFileText.getText().trim();
            if ( !sourceFileName.isEmpty() && !targetFileName.isEmpty() ) {
                // Generate models
                final ModelType sourceType =
                    ( ModelType ) ( ( IStructuredSelection ) sourceTypeComboViewer.getSelection() ).getFirstElement();
                final String sourceClassName = generateModel( sourceFileName, sourceType );
                final ModelType targetType =
                    ( ModelType ) ( ( IStructuredSelection ) targetTypeComboViewer.getSelection() ).getFirstElement();
                final String targetClassName = generateModel( targetFileName, targetType );
                // Update Camel config
                camelConfigBuilder.addTransformation( idText.getText(),
                                                      sourceType.transformType, sourceClassName,
                                                      targetType.transformType, targetClassName );
                try ( FileOutputStream camelConfigStream = new FileOutputStream( camelConfigFile ) ) {
                    camelConfigBuilder.saveConfig( camelConfigStream );
                } catch ( final Exception e ) {
                    Activator.error( getShell(), e );
                    return false;
                }
                dozerConfigBuilder.addClassMapping( sourceClassName, targetClassName );
            }
            dozerConfigBuilder.saveConfig( dozerConfigStream );
            project.refreshLocal( IProject.DEPTH_INFINITE, null );
            // Open mapping editor
            final IEditorDescriptor desc =
                PlatformUI.getWorkbench().getEditorRegistry().getEditors( dozerConfigFile.getName(),
                                                                          Platform.getContentTypeManager().getContentType( DozerConfigContentTypeDescriber.ID ) )[ 0 ];
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor( new FileEditorInput( dozerConfigFile ),
                                                                                             desc.getId() );
        } catch ( final Exception e ) {
            Activator.error( getShell(), e );
            return false;
        }
        return true;
    }
    
    enum ModelType {
        
        JAVA( "Java", TransformType.JAVA ),
        JSON( "JSON", TransformType.JSON ),
        JSON_SCHEMA( "JSON Schema", TransformType.JSON ),
        XML( "XML", TransformType.XML ),
        XSD( "XSD", TransformType.XML );
        
        final String text;
        final TransformType transformType;
        
        private ModelType( final String text,
                           final TransformType transformType ) {
            this.text = text;
            this.transformType = transformType;
        }
        
        @Override
        public String toString() {
            return text;
        }
    }
}
