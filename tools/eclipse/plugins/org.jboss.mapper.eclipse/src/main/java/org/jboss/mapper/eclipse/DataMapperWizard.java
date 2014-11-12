package org.jboss.mapper.eclipse;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Status;
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
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.ResourceListSelectionDialog;
import org.eclipse.ui.part.FileEditorInput;
import org.jboss.mapper.forge.ConfigBuilder;

/**
 * 
 */
public class DataMapperWizard extends Wizard implements INewWizard {
    
    static final String DEFAULT_DOZER_CONFIG_FILE_NAME = "dozerBeanMapping.xml";
    
    IProject project;
    IFile configFile;
    IWorkspaceRoot workspace = ResourcesPlugin.getWorkspace().getRoot();
    Text sourceModelText, targetModelText;
    
    /**
     * 
     */
    public DataMapperWizard() {
        addPage( constructMainPage() );
    }
    
    private IWizardPage constructMainPage() {
        return new WizardPage( "New Data Mapping", "New Data Mapping", null ) {
            
            /**
             * {@inheritDoc}
             * 
             * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
             */
            @Override
            public void createControl( final Composite parent ) {
                setDescription( "Select the project and name for the the new mapping file\n" +
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
                projectViewer.add( workspace.getProjects() );
                if ( project != null ) projectViewer.setSelection( new StructuredSelection( project ) );
                projectViewer.getCombo().addSelectionListener( new SelectionAdapter() {
                    
                    @Override
                    public void widgetSelected( final SelectionEvent event ) {
                        project = ( IProject ) ( ( IStructuredSelection ) projectViewer.getSelection() ).getFirstElement();
                    }
                } );
                label = new Label( page, SWT.NONE );
                label.setText( "Name:" );
                final Text nameText = new Text( page, SWT.BORDER );
                nameText.setText( DEFAULT_DOZER_CONFIG_FILE_NAME );
                nameText.setLayoutData( GridDataFactory.swtDefaults().span( 2, 1 ).grab( true, false )
                                                       .align( SWT.FILL, SWT.CENTER ).create() );
                nameText.addKeyListener( new KeyAdapter() {
                    
                    @Override
                    public void keyReleased( final KeyEvent event ) {
                        String path = nameText.getText();
                        if ( project == null || path.isEmpty() ) {
                            configFile = null;
                        } else {
                            if ( !path.endsWith( ".xml" ) ) path = path + ".xml";
                            configFile = workspace.getFile( project.getFullPath().append( "src/main/resources/" + path ) );
                            setErrorMessage( configFile.exists() ? "A file with that name already exists." : null );
                        }
                        setPageComplete( project != null && configFile != null );
                    }
                } );
                label = new Label( page, SWT.NONE );
                label.setText( "Source Model:" );
                label.setToolTipText( "The Java class representing the source model." );
                sourceModelText = new Text( page, SWT.BORDER );
                sourceModelText.setLayoutData( GridDataFactory.swtDefaults().grab( true, false ).align( SWT.FILL, SWT.CENTER )
                                                              .create() );
                Button button = new Button( page, SWT.NONE );
                button.setText( "..." );
                button.addSelectionListener( new SelectionAdapter() {
                    
                    @Override
                    public void widgetSelected( final SelectionEvent event ) {
                        selectModel( "Source", sourceModelText );
                    }
                } );
                label = new Label( page, SWT.NONE );
                label.setText( "Target Model:" );
                label.setToolTipText( "The Java class representing the target model." );
                targetModelText = new Text( page, SWT.BORDER );
                targetModelText.setLayoutData( GridDataFactory.swtDefaults().grab( true, false ).align( SWT.FILL, SWT.CENTER )
                                                              .create() );
                button = new Button( page, SWT.NONE );
                button.setText( "..." );
                button.addSelectionListener( new SelectionAdapter() {
                    
                    @Override
                    public void widgetSelected( final SelectionEvent event ) {
                        selectModel( "Target", targetModelText );
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
            }
            
            private void findClasses( final IFolder folder,
                                      final List< IResource > classes ) throws CoreException {
                for ( final IResource resource : folder.members() ) {
                    if ( resource instanceof IFolder ) findClasses( ( IFolder ) resource, classes );
                    else if ( resource.getName().endsWith( ".class" ) ) classes.add( resource );
                }
            }
            
            void selectModel( final String modelType,
                              final Text text ) {
                final IFolder classesFolder = project.getFolder( "target/classes" );
                final List< IResource > classes = new ArrayList<>();
                try {
                    findClasses( classesFolder, classes );
                    final ResourceListSelectionDialog dlg =
                        new ResourceListSelectionDialog( getShell(), classes.toArray( new IResource[ classes.size() ] ) ) {
                            
                            @Override
                            protected Control createDialogArea( final Composite parent ) {
                                final Composite dlgArea = ( Composite ) super.createDialogArea( parent );
                                for ( final Control child : dlgArea.getChildren() ) {
                                    if ( child instanceof Text ) {
                                        ( ( Text ) child ).setText( "*" );
                                        break;
                                    }
                                }
                                return dlgArea;
                            }
                        };
                    dlg.setTitle( "Select " + modelType + " Model" );
                    if ( dlg.open() == Window.OK ) {
                        final IFile file = ( IFile ) dlg.getResult()[ 0 ];
                        final String name =
                            file.getFullPath().makeRelativeTo( classesFolder.getFullPath() ).toString().replace( '/', '.' );
                        text.setText( name.substring( 0, name.length() - ".class".length() ) );
                    }
                } catch ( final CoreException e ) {
                    MessageDialog.openError( getShell(), "Error", e.getMessage() );
                    Activator.plugin().getLog().log( new Status( Status.ERROR,
                                                                 Activator.plugin().getBundle().getSymbolicName(),
                                                                 e.getMessage() ) );
                }
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
            final IProject[] projects = workspace.getProjects();
            if ( projects.length == 1 ) project = projects[ 0 ];
            return;
        }
        project = ( ( IResource ) ( ( IAdaptable ) selection.getFirstElement() ).getAdapter( IResource.class ) ).getProject();
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.wizard.Wizard#performFinish()
     */
    @Override
    public boolean performFinish() {
        if ( configFile.exists() && !MessageDialog.openConfirm( getShell(), "Confirm", "Overwrite existing file?" ) ) return false;
        final ConfigBuilder configBuilder = ConfigBuilder.newConfig();
        configBuilder.addClassMapping( sourceModelText.getText(), targetModelText.getText() );
        try ( FileOutputStream stream = new FileOutputStream( new File( configFile.getLocationURI() ) ) ) {
            configBuilder.saveConfig( stream );
            project.refreshLocal( IProject.DEPTH_INFINITE, null );
            final IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor( configFile.getName() );
            PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor( new FileEditorInput( configFile ),
                                                                                             desc.getId() );
        } catch ( final Exception e ) {
            MessageDialog.openError( getShell(), "Error", e.getMessage() );
            Activator.plugin().getLog().log( new Status( Status.ERROR,
                                                         Activator.plugin().getBundle().getSymbolicName(),
                                                         e.getMessage() ) );
            return false;
        }
        return true;
    }
}
