package org.jboss.mapper.eclipse;

import java.io.File;
import java.io.FileOutputStream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.IStructuredSelection;
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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.dialogs.ResourceListSelectionDialog;
import org.eclipse.ui.dialogs.ResourceSelectionDialog;
import org.jboss.mapper.forge.ConfigBuilder;

/**
 * 
 */
public class DataMapperWizard extends Wizard implements INewWizard {
    
    IProject project;
    IFile configFile;
    IFile sourceModelFile, targetModelFile;
    
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
                final Text projectText = new Text( page, SWT.BORDER );
                projectText.setEditable( false );
                projectText.setLayoutData( GridDataFactory.swtDefaults().grab( true, false ).align( SWT.FILL, SWT.CENTER ).create() );
                if ( project != null ) projectText.setText( project.getFullPath().makeRelative().toString() );
                final Button projectButton = new Button( page, SWT.NONE );
                projectButton.setText( "..." );
                projectButton.addSelectionListener( new SelectionAdapter() {
                    
                    @Override
                    public void widgetSelected( final SelectionEvent event ) {
                        final ResourceListSelectionDialog dlg =
                            new ResourceListSelectionDialog( getShell(),
                                                             ResourcesPlugin.getWorkspace().getRoot(),
                                                             IResource.PROJECT );
                        if ( dlg.open() == Window.OK ) {
                            final IPath path = ( ( IPath ) dlg.getResult()[ 0 ] ).makeRelative();
                            projectText.setText( path.toString() );
                            project = ResourcesPlugin.getWorkspace().getRoot().getProject( path.toString() );
                        }
                    }
                } );
                label = new Label( page, SWT.NONE );
                label.setText( "Name:" );
                final Text nameText = new Text( page, SWT.BORDER );
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
                            configFile = ResourcesPlugin.getWorkspace().getRoot().getFile( project.getFullPath().append( "src/main/resources/" + path ) );
                            setErrorMessage( configFile.exists() ? "A file with that name already exists." : null );
                        }
                        validatePage();
                    }
                } );
                label = new Label( page, SWT.NONE );
                label.setText( "Source Model:" );
                label.setToolTipText( "The Java class representing the source model." );
                final Text sourceModelText = new Text( page, SWT.BORDER );
                sourceModelText.setLayoutData( GridDataFactory.swtDefaults().grab( true, false ).align( SWT.FILL, SWT.CENTER )
                                                              .create() );
                Button button = new Button( page, SWT.NONE );
                button.setText( "..." );
                button.addSelectionListener( new SelectionAdapter() {
                    
                    @Override
                    public void widgetSelected( final SelectionEvent event ) {
                        final ResourceSelectionDialog dlg =
                            new ResourceSelectionDialog( getShell(),
                                                         ResourcesPlugin.getWorkspace().getRoot().getFolder( project.getFullPath().append( "target/classes" ) ),
                                                         "Select Source Model" );
                        if ( dlg.open() == Window.OK ) {
                            sourceModelFile = ( IFile ) dlg.getResult()[ 0 ];
                            sourceModelText.setText( sourceModelFile.getName() );
                        }
                    }
                } );
                label = new Label( page, SWT.NONE );
                label.setText( "Target Model:" );
                label.setToolTipText( "The Java class representing the target model." );
                final Text targetModelText = new Text( page, SWT.BORDER );
                targetModelText.setLayoutData( GridDataFactory.swtDefaults().grab( true, false ).align( SWT.FILL, SWT.CENTER )
                                                              .create() );
                button = new Button( page, SWT.NONE );
                button.setText( "..." );
                button.addSelectionListener( new SelectionAdapter() {
                    
                    @Override
                    public void widgetSelected( final SelectionEvent event ) {
                        final ResourceSelectionDialog dlg =
                            new ResourceSelectionDialog( getShell(),
                                                         ResourcesPlugin.getWorkspace().getRoot().getFolder( project.getFullPath().append( "target/classes" ) ),
                                                         "Select Target Model" );
                        if ( dlg.open() == Window.OK ) {
                            targetModelFile = ( IFile ) dlg.getResult()[ 0 ];
                            targetModelText.setText( targetModelFile.getName() );
                        }
                    }
                } );
                page.addPaintListener( new PaintListener() {
                    
                    @Override
                    public void paintControl( final PaintEvent event ) {
                        if ( project == null ) projectButton.setFocus();
                        else nameText.setFocus();
                        page.removePaintListener( this );
                    }
                } );
            }
            
            void validatePage() {
                setPageComplete( project != null && configFile != null );
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
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.wizard.Wizard#performFinish()
     */
    @Override
    public boolean performFinish() {
        final ConfigBuilder configBuilder = ConfigBuilder.newConfig();
        configBuilder.addClassMapping( "xml.ABCOrder", "json.XYZOrder" );
        try ( FileOutputStream stream = new FileOutputStream( new File( configFile.getLocationURI() ) ) ) {
            configBuilder.saveConfig( stream );
        } catch ( final Exception e ) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
