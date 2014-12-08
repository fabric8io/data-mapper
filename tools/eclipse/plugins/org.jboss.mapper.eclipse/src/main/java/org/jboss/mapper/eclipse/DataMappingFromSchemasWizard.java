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
import org.jboss.mapper.dozer.ConfigBuilder;
import org.jboss.mapper.model.json.JsonModelGenerator;
import org.jboss.mapper.model.xml.XmlModelGenerator;

import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JPackage;

/**
 * 
 */
public class DataMappingFromSchemasWizard extends Wizard implements INewWizard {
    
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
    IFile dozerConfigFile;
    Text sourceFileText, targetFileText;
    Button sourceFileButton, targetFileButton;
    ComboViewer sourceTypeComboViewer, targetTypeComboViewer;
    
    /**
     * 
     */
    public DataMappingFromSchemasWizard() {
        addPage( constructMainPage() );
    }
    
    private IWizardPage constructMainPage() {
        return new WizardPage( "New Data Mapping", "New Data Mapping", Activator.imageDescriptor( "transform.png" ) ) {
            
            Text nameText;
            
            /**
             * {@inheritDoc}
             * 
             * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
             */
            @Override
            public void createControl( final Composite parent ) {
                setDescription( "Select the project and name for the the new mapping file.\n" +
                                "Optionally, select the source and target schemas to be mapped." );
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
                // Create Dozer config controls
                label = new Label( page, SWT.NONE );
                label.setText( "Name:" );
                nameText = new Text( page, SWT.BORDER );
                nameText.setText( DEFAULT_DOZER_CONFIG_FILE_NAME );
                nameText.setLayoutData( GridDataFactory.swtDefaults().span( 2, 1 ).grab( true, false )
                                                       .align( SWT.FILL, SWT.CENTER ).create() );
                nameText.addKeyListener( new KeyAdapter() {
                    
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
                createSchemaControls( group, fileLabel, "Source", sourceFileText, sourceFileButton, typeLabel, sourceTypeComboViewer );
                // Create target controls
                group = new Group( page, SWT.SHADOW_ETCHED_IN );
                fileLabel = new Label( group, SWT.NONE );
                targetFileText = new Text( group, SWT.BORDER );
                targetFileButton = new Button( group, SWT.NONE );
                typeLabel = new Label( group, SWT.NONE );
                targetTypeComboViewer = new ComboViewer( new Combo( group, SWT.READ_ONLY ) );
                createSchemaControls( group, fileLabel, "Target", targetFileText, targetFileButton, typeLabel, targetTypeComboViewer );
                
                page.addPaintListener( new PaintListener() {
                    
                    @Override
                    public void paintControl( final PaintEvent event ) {
                        if ( project == null ) projectViewer.getCombo().setFocus();
                        else nameText.setFocus();
                        page.removePaintListener( this );
                    }
                } );
                validatePage();
            }
            
            void createSchemaControls( final Group group,
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
                group.setText( schemaType + " Schema" );
                fileLabel.setText( "File:" );
                fileText.setLayoutData( GridDataFactory.swtDefaults().grab( true, false ).align( SWT.FILL, SWT.CENTER ).create() );
                fileButton.setText( "..." );
                typeLabel.setText( "Type:" );
                typeComboViewer.getCombo().setLayoutData( GridDataFactory.swtDefaults().span( 2, 1 ).grab( true, false ).create() );
                typeComboViewer.add( TransformType.values() );
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
                            final String ext = name.substring( name.lastIndexOf( '.' ) + 1 ).toLowerCase();
                            switch ( ext ) {
                                case "java":
                                    typeComboViewer.setSelection( new StructuredSelection( TransformType.JAVA ) );
                                    break;
                                case "json":
                                    typeComboViewer.setSelection( new StructuredSelection( TransformType.JSON ) );
                                    break;
                                case "xml":
                                case "xsd":
                                    typeComboViewer.setSelection( new StructuredSelection( TransformType.XML ) );
                                    break;
                                default:
                                    break;
                            }
                        }
                        validatePage();
                    }
                } );
            }
            
            void validatePage() {
                if ( project != null ) {
                    sourceFileButton.setEnabled( true );
                    targetFileButton.setEnabled( true );
                    String path = nameText.getText();
                    if ( path.isEmpty() ) dozerConfigFile = null;
                    else {
                        if ( !path.toLowerCase().endsWith( ".xml" ) ) path = path + ".xml";
                        dozerConfigFile = project.getFile( RESOURCES_PATH + path );
                        final String sourceFileName = sourceFileText.getText().trim();
                        final String targetFileName = targetFileText.getText().trim();
                        if ( sourceFileName.isEmpty() && targetFileName.isEmpty() ) {
                            setPageComplete( true );
                            return;
                        }
                        if ( !sourceFileName.isEmpty() && !targetFileName.isEmpty() ) {
                            setPageComplete( !sourceTypeComboViewer.getSelection().isEmpty()
                                             && !targetTypeComboViewer.getSelection().isEmpty() );
                            return;
                        }
                    }
                } else {
                    sourceFileButton.setEnabled( false );
                    targetFileButton.setEnabled( false );
                }
                setPageComplete( false );
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
                                  final TransformType type ) throws Exception {
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
        switch ( type ) {
            case JSON: {
                final JsonModelGenerator generator = new JsonModelGenerator();
                generator.generateFromSchema( className.toString(),
                                              pkgName,
                                              project.findMember( fileName ).getLocationURI().toURL(),
                                              new File( project.getFolder( JAVA_PATH ).getLocationURI() ) );
                break;
            }
            case XML: {
                final XmlModelGenerator generator = new XmlModelGenerator();
                final JCodeModel model = generator.generateFromSchema( new File( project.findMember( fileName ).getLocationURI() ),
                                                                       pkgName,
                                                                       new File( project.getFolder( JAVA_PATH ).getLocationURI() ) );
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
        return pkgName + "." + className;
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
                final TransformType sourceType =
                    ( TransformType ) ( ( IStructuredSelection ) sourceTypeComboViewer.getSelection() ).getFirstElement();
                final String sourceClassName = generateModel( sourceFileName, sourceType );
                final TransformType targetType =
                    ( TransformType ) ( ( IStructuredSelection ) targetTypeComboViewer.getSelection() ).getFirstElement();
                final String targetClassName = generateModel( targetFileName, targetType );
                // Update Camel config
                final File camelConfigFile = new File( project.getFile( CAMEL_CONFIG_PATH ).getLocationURI() );
                final CamelConfigBuilder camelConfigBuilder = CamelConfigBuilder.loadConfig( camelConfigFile );
                camelConfigBuilder.addTransformation( null, sourceType, sourceClassName, targetType, targetClassName );
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
}
