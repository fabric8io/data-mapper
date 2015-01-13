package org.jboss.mapper.eclipse;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.jboss.mapper.dozer.ConfigBuilder;

/**
 * 
 */
public class DozerMappingWizard extends Wizard implements INewWizard {
    
    static final String DEFAULT_DOZER_CONFIG_FILE_NAME = "dozerBeanMapping.xml";
    
    IProject project;
    IFile dozerConfigFile;
    Text sourceFileText, targetFileText;
    Button sourceFileButton, targetFileButton;
    
    /**
     * 
     */
    public DozerMappingWizard() {
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
                                "Optionally, select the source and target models to be mapped." );
                final Composite page = new Composite( parent, SWT.NONE );
                setControl( page );
                page.setLayout( GridLayoutFactory.swtDefaults().spacing( 0, 5 ).numColumns( 3 ).create() );
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
                label = new Label( page, SWT.NONE );
                label.setText( "Source Model:" );
                label.setToolTipText( "The Java class representing the source model." );
                sourceFileText = new Text( page, SWT.BORDER );
                sourceFileText.setLayoutData( GridDataFactory.swtDefaults().grab( true, false ).align( SWT.FILL, SWT.CENTER )
                                                             .create() );
                sourceFileButton = new Button( page, SWT.NONE );
                sourceFileButton.setText( "..." );
                sourceFileButton.addSelectionListener( new SelectionAdapter() {
                    
                    @Override
                    public void widgetSelected( final SelectionEvent event ) {
                        final String name = DataBrowser.selectModel( getShell(), project, null, "Source" );
                        if ( name != null ) sourceFileText.setText( name );
                    }
                } );
                label = new Label( page, SWT.NONE );
                label.setText( "Target Model:" );
                label.setToolTipText( "The Java class representing the target model." );
                targetFileText = new Text( page, SWT.BORDER );
                targetFileText.setLayoutData( GridDataFactory.swtDefaults().grab( true, false ).align( SWT.FILL, SWT.CENTER )
                                                             .create() );
                targetFileButton = new Button( page, SWT.NONE );
                targetFileButton.setText( "..." );
                targetFileButton.addSelectionListener( new SelectionAdapter() {
                    
                    @Override
                    public void widgetSelected( final SelectionEvent event ) {
                        final String name = DataBrowser.selectModel( getShell(), project, null, "Target" );
                        if ( name != null ) targetFileText.setText( name );
                    }
                } );
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
            
            void validatePage() {
                if ( project != null ) {
                    sourceFileButton.setEnabled( true );
                    targetFileButton.setEnabled( true );
                    String path = nameText.getText();
                    if ( path.isEmpty() ) dozerConfigFile = null;
                    else {
                        if ( !path.toLowerCase().endsWith( ".xml" ) ) path = path + ".xml";
                        dozerConfigFile = project.getFile( "src/main/resources/" + path );
                        final String sourceFileName = sourceFileText.getText().trim();
                        final String targetFileName = targetFileText.getText().trim();
                        if ( ( sourceFileName.isEmpty() && targetFileName.isEmpty() ) ||
                             ( !sourceFileName.isEmpty() && !targetFileName.isEmpty() ) ) {
                            setPageComplete( true );
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
        if ( project != null ) dozerConfigFile = project.getFile( "src/main/resources/" + DEFAULT_DOZER_CONFIG_FILE_NAME );
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.wizard.Wizard#performFinish()
     */
    @Override
    public boolean performFinish() {
        if ( dozerConfigFile.exists() && !MessageDialog.openConfirm( getShell(), "Confirm", "Overwrite existing file?" ) ) return false;
        final ConfigBuilder configBuilder = ConfigBuilder.newConfig();
        if ( !sourceFileText.getText().trim().isEmpty() && !targetFileText.getText().trim().isEmpty() )
            configBuilder.addClassMapping( sourceFileText.getText().trim(), targetFileText.getText().trim() );
        File newFile = new File(dozerConfigFile.getLocationURI());
        if (!newFile.getParentFile().exists()) {
            newFile.getParentFile().mkdirs();
        }
        try ( FileOutputStream stream = new FileOutputStream( newFile) ) {
            configBuilder.saveConfig( stream );
            project.refreshLocal( IProject.DEPTH_INFINITE, null );
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
