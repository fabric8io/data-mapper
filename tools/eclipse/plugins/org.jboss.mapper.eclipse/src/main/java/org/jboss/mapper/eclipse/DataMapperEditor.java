/**
 * Copyright 2014 Turnip Solutions, LLC.
 * All rights reserved.
 */
package org.jboss.mapper.eclipse;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;

/**
 * 
 */
public class DataMapperEditor extends MultiPageEditorPart {
    
    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.ui.part.MultiPageEditorPart#createPages()
     */
    @Override
    protected void createPages() {
        try {
            addPage( new DataMapperEditorMappingPage(), getEditorInput() );
        } catch ( final PartInitException e ) {
            e.printStackTrace();
        }
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
     * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
     */
    @Override
    public boolean isSaveAsAllowed() {
        return false;
    }
}
