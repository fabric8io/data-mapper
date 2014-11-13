/**
 * Copyright 2014 Turnip Solutions, LLC.
 * All rights reserved.
 */
package org.jboss.mapper.eclipse;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.jboss.mapper.forge.ConfigBuilder;

/**
 * 
 */
public class DataMapperEditorMappingPage extends EditorPart {
    
    ConfigBuilder configBuilder;
    
    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
     */
    @SuppressWarnings( "unused" )
    @Override
    public void createPartControl( final Composite parent ) {
        new DataMapper( parent, ( ( FileEditorInput ) getEditorInput() ).getFile() );
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
     */
    @Override
    public void doSave( final IProgressMonitor monitor ) {}
    
    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.ui.part.EditorPart#doSaveAs()
     */
    @Override
    public void doSaveAs() {}
    
    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
     */
    @Override
    public void init( final IEditorSite site,
                      final IEditorInput input ) throws PartInitException {
        final IContentType contentType = Platform.getContentTypeManager().getContentType( DozerConfigContentTypeDescriber.ID );
        if ( !contentType.isAssociatedWith( input.getName() ) )
            throw new PartInitException( "The Data Mapping editor can only be opened with a Dozer configuration file." );
        setSite( site );
        setInput( input );
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.ui.part.EditorPart#isDirty()
     */
    @Override
    public boolean isDirty() {
        return false;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
     */
    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
     */
    @Override
    public void setFocus() {}
}
