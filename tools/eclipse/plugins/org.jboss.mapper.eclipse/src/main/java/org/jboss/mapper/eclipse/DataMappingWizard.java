package org.jboss.mapper.eclipse;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileOutputStream;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.jboss.mapper.MapperConfiguration;
import org.jboss.mapper.TransformType;
import org.jboss.mapper.camel.CamelConfigBuilder;
import org.jboss.mapper.dozer.DozerMapperConfiguration;
import org.jboss.mapper.model.json.JsonModelGenerator;
import org.jboss.mapper.model.xml.XmlModelGenerator;

import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JPackage;

/**
 *
 */
public class DataMappingWizard extends Wizard implements INewWizard {
    
    private static final String MAIN_PATH = "src/main/";
    private static final String JAVA_PATH = MAIN_PATH + "java/";
    private static final String RESOURCES_PATH = MAIN_PATH + "resources/";
    private static final String CAMEL_CONFIG_PATH = RESOURCES_PATH + "META-INF/spring/camel-context.xml";
    private static final String DEFAULT_FILE_PATH = "transformation.xml";
    private static final String OBJECT_FACTORY_NAME = "ObjectFactory";
    
    final Model uiModel = new Model();
    
    /**
     *
     */
    public DataMappingWizard() {
        addPage( new NewTransformationWizardPage( uiModel ) );
    }
    
    private String generateModel( final String filePath,
                                  final ModelType type ) throws Exception {
        // Build class name from file name
        final StringBuilder className = new StringBuilder();
        final StringCharacterIterator iter =
            new StringCharacterIterator( filePath.substring( filePath.lastIndexOf( '/' ) + 1, filePath.lastIndexOf( '.' ) ) );
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
        while ( uiModel.getProject().exists( new Path( JAVA_PATH + pkgName ) ) ) {
            pkgName = className.toString() + sequencer++;
        }
        pkgName = pkgName.toLowerCase();
        // Generate model
        final File targetClassesFolder = new File( uiModel.getProject().getFolder( JAVA_PATH ).getLocationURI() );
        switch ( type ) {
            case CLASS: {
                final IResource resource = uiModel.getProject().findMember( filePath );
                if ( resource != null ) {
                    final IClassFile file = ( IClassFile ) JavaCore.create( uiModel.getProject().findMember( filePath ) );
                    if ( file != null ) return pkgName + "." + file.getType().getFullyQualifiedName();
                }
                return null;
            }
            case JAVA: {
                final IResource resource = uiModel.getProject().findMember( filePath );
                if ( resource != null ) {
                    final ICompilationUnit file = ( ICompilationUnit ) JavaCore.create( uiModel.getProject().findMember( filePath ) );
                    if ( file != null ) {
                        final IType[] types = file.getTypes();
                        if ( types.length > 0 ) return types[ 0 ].getFullyQualifiedName();
                    }
                }
                return null;
            }
            case JSON: {
                final JsonModelGenerator generator = new JsonModelGenerator();
                generator.generateFromInstance( className.toString(),
                                                pkgName,
                                                uiModel.getProject().findMember( filePath ).getLocationURI().toURL(),
                                                targetClassesFolder );
                return pkgName + "." + className;
            }
            case JSON_SCHEMA: {
                final JsonModelGenerator generator = new JsonModelGenerator();
                generator.generateFromSchema( className.toString(),
                                              pkgName,
                                              uiModel.getProject().findMember( filePath ).getLocationURI().toURL(),
                                              targetClassesFolder );
                return pkgName + "." + className;
            }
            case XSD: {
                final XmlModelGenerator generator = new XmlModelGenerator();
                final JCodeModel model = generator.generateFromSchema( new File( uiModel.getProject().findMember( filePath ).getLocationURI() ),
                                                                       pkgName,
                                                                       targetClassesFolder );
                final String modelClass = selectModelClass( model );
                if ( modelClass != null ) { return modelClass; }
                return null;
            }
            case XML: {
                final XmlModelGenerator generator = new XmlModelGenerator();
                final File schemaPath = new File( uiModel.getProject().getFile( filePath + ".xsd" ).getLocationURI() );
                final JCodeModel model = generator.generateFromInstance( new File( uiModel.getProject().findMember( filePath ).getLocationURI() ),
                                                                         schemaPath,
                                                                         pkgName,
                                                                         targetClassesFolder );
                final String modelClass = selectModelClass( model );
                if ( modelClass != null ) { return modelClass; }
                return null;
            }
            default:
                return null;
        }
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.ui.IWorkbenchWizard#init(org.eclipse.ui.IWorkbench, org.eclipse.jface.viewers.IStructuredSelection)
     */
    @Override
    public void init( final IWorkbench workbench,
                      final IStructuredSelection selection ) {
        for ( final Iterator< IProject > iter = uiModel.projects.iterator(); iter.hasNext(); ) {
            if ( iter.next().findMember( CAMEL_CONFIG_PATH ) == null ) iter.remove();
        }
        if ( uiModel.projects.size() == 1 ) uiModel.setProject( uiModel.projects.get( 0 ) );
        else {
            final IStructuredSelection resourceSelection =
                ( IStructuredSelection ) workbench.getActiveWorkbenchWindow().getSelectionService().getSelection( "org.eclipse.ui.navigator.ProjectExplorer" );
            if ( resourceSelection == null || resourceSelection.size() != 1 ) return;
            final IProject project =
                ( ( IResource ) ( ( IAdaptable ) resourceSelection.getFirstElement() ).getAdapter( IResource.class ) ).getProject();
            if ( uiModel.projects.contains( project ) ) uiModel.setProject( project );
        }
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.jface.wizard.Wizard#performFinish()
     */
    @Override
    public boolean performFinish() {
        // Save transformation file
        final IFile file = uiModel.getProject().getFile( RESOURCES_PATH + uiModel.getFilePath() );
        if ( file.exists() &&
             !MessageDialog.openConfirm( getShell(),
                                         "Confirm",
                                         "Overwrite existing transformation file (\"" + file.getFullPath() + "\")?" ) )
            return false;
        final MapperConfiguration dozerConfigBuilder = DozerMapperConfiguration.newConfig();
        final File newFile = new File( file.getLocationURI() );
        if ( !newFile.getParentFile().exists() ) newFile.getParentFile().mkdirs();
        try ( FileOutputStream configStream = new FileOutputStream( newFile ) ) {
            if ( uiModel.getSourceFilePath() != null ) {
                // Generate models
                final String sourceClassName = generateModel( uiModel.getSourceFilePath(), uiModel.getSourceType() );
                final String targetClassName = generateModel( uiModel.getTargetFilePath(), uiModel.getTargetType() );
                // Update Camel config
                final IPath resourcesPath = uiModel.getProject().getFolder( RESOURCES_PATH ).getFullPath();
                uiModel.camelConfigBuilder.addTransformation( uiModel.getId(),
                                                              file.getFullPath().makeRelativeTo( resourcesPath ).toString(),
                                                              uiModel.getSourceType().transformType, sourceClassName,
                                                              uiModel.getTargetType().transformType, targetClassName );
                try ( FileOutputStream camelConfigStream =
                    new FileOutputStream( new File( uiModel.getProject().getFile( CAMEL_CONFIG_PATH ).getLocationURI() ) ) ) {
                    uiModel.camelConfigBuilder.saveConfig( camelConfigStream );
                } catch ( final Exception e ) {
                    Activator.error( getShell(), e );
                    return false;
                }
                dozerConfigBuilder.addClassMapping( sourceClassName, targetClassName );
            }
            dozerConfigBuilder.saveConfig( configStream );
            uiModel.getProject().refreshLocal( IProject.DEPTH_INFINITE, null );
            // Ensure build of Java classes has completed
            Job.getJobManager().join( ResourcesPlugin.FAMILY_AUTO_BUILD, null );
            // Open mapping editor
            final IEditorDescriptor desc =
                PlatformUI.getWorkbench().getEditorRegistry().getEditors( file.getName(),
                                                                          Platform.getContentTypeManager().getContentType( DozerConfigContentTypeDescriber.ID ) )[ 0 ];
            final IEditorPart editor =
                PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor( new FileEditorInput( file ),
                                                                                                 desc.getId() );
            final DataMapperEditor dmEditor = ( DataMapperEditor ) editor;
            final DataMapperEditorMappingPage page = ( DataMapperEditorMappingPage ) dmEditor.getSelectedPage();
            page.mapper.setEndpointID( uiModel.getId() );
        } catch ( final Exception e ) {
            Activator.error( getShell(), e );
            return false;
        }
        return true;
    }
    
    private String selectModelClass( final JCodeModel model ) {
        for ( final Iterator< JPackage > pkgIter = model.packages(); pkgIter.hasNext(); ) {
            final JPackage pkg = pkgIter.next();
            for ( final Iterator< JDefinedClass > classIter = pkg.classes(); classIter.hasNext(); ) {
                // TODO this only works when a single top-level class exists; fix after issue #33 is fixed
                final JDefinedClass definedClass = classIter.next();
                if ( OBJECT_FACTORY_NAME.equals( definedClass.name() ) ) continue;
                return definedClass.fullName();
            }
        }
        return null;
    }
    
    class Model implements PropertyChangeListener {
        
        final List< IProject > projects = new ArrayList<>( Arrays.asList( ResourcesPlugin.getWorkspace().getRoot().getProjects() ) );
        CamelConfigBuilder camelConfigBuilder;
        
        private final PropertyChangeSupport changeSupport = new PropertyChangeSupport( this );
        
        private IProject project;
        private String id;
        private String filePath = DEFAULT_FILE_PATH;
        private String sourceFilePath, targetFilePath;
        private ModelType sourceType, targetType;
        
        /**
         * @param propertyName
         * @param listener
         */
        public void addPropertyChangeListener( final String propertyName,
                                               final PropertyChangeListener listener ) {
            changeSupport.addPropertyChangeListener( propertyName, listener );
        }
        
        /**
         * @return the transformation file path
         */
        public String getFilePath() {
            return filePath;
        }
        
        /**
         * @return the transformation ID that will be seen in the Camel editor
         */
        public String getId() {
            return id;
        }
        
        /**
         * @return the project in which to create the transformation
         */
        public IProject getProject() {
            return project;
        }
        
        /**
         * @return the source file path
         */
        public String getSourceFilePath() {
            return sourceFilePath;
        }
        
        /**
         * @return the source type
         */
        public ModelType getSourceType() {
            return sourceType;
        }
        
        /**
         * @return the target file path
         */
        public String getTargetFilePath() {
            return targetFilePath;
        }
        
        /**
         * @return the target type
         */
        public ModelType getTargetType() {
            return targetType;
        }
        
        /**
         * {@inheritDoc}
         * 
         * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
         */
        @Override
        public void propertyChange( final PropertyChangeEvent event ) {
            changeSupport.firePropertyChange( event.getPropertyName(), event.getOldValue(), event.getNewValue() );
        }
        
        /**
         * @param listener
         */
        public void removePropertyChangeListener( final PropertyChangeListener listener ) {
            changeSupport.removePropertyChangeListener( listener );
        }
        
        /**
         * @param filePath
         */
        public void setFilePath( final String filePath ) {
            changeSupport.firePropertyChange( "filePath", this.filePath, this.filePath = filePath.trim() );
        }
        
        /**
         * @param id
         */
        public void setId( final String id ) {
            changeSupport.firePropertyChange( "id", this.id, this.id = id.trim() );
        }
        
        /**
         * @param project
         */
        public void setProject( final IProject project ) {
            try {
                camelConfigBuilder =
                    CamelConfigBuilder.loadConfig( new File( project.getFile( CAMEL_CONFIG_PATH ).getLocationURI() ) );
                changeSupport.firePropertyChange( "project", this.project, this.project = project );
            } catch ( final Exception e ) {
                Activator.error( getShell(), e );
            }
        }
        
        /**
         * @param sourceFilePath
         */
        public void setSourceFilePath( final String sourceFilePath ) {
            changeSupport.firePropertyChange( "sourceFilePath", this.sourceFilePath, this.sourceFilePath = sourceFilePath.trim() );
        }
        
        /**
         * @param sourceType
         */
        public void setSourceType( final ModelType sourceType ) {
            changeSupport.firePropertyChange( "sourceType", this.sourceType, this.sourceType = sourceType );
        }
        
        /**
         * @param targetFilePath
         */
        public void setTargetFilePath( final String targetFilePath ) {
            changeSupport.firePropertyChange( "targetFilePath", this.targetFilePath, this.targetFilePath = targetFilePath.trim() );
        }
        
        /**
         * @param targetType
         */
        public void setTargetType( final ModelType targetType ) {
            changeSupport.firePropertyChange( "targetType", this.targetType, this.targetType = targetType );
        }
    }
    
    enum ModelType {
        
        CLASS( "Java Class", TransformType.JAVA ),
        JAVA( "Java Source", TransformType.JAVA ),
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
