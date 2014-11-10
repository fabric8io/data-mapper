/**
 * Copyright 2014 Turnip Solutions, LLC.
 * All rights reserved.
 */
package org.jboss.mapper.eclipse;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;

/**
 * 
 */
public class DataMapperMappingPage extends EditorPart {
    
    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createPartControl( final Composite parent ) {
        new Mapper( parent );
        // jpav: remove
        // try {
        // final Class< ? > abcOrderClass = ( ( FileEditorInput ) getEditorInput()
        // ).getFile().getClass().getClassLoader().loadClass( "org.example.order.abc.ABCOrder" );
        // // jpav: remove
        // System.out.println( abcOrderClass );
        // } catch ( final ClassNotFoundException e ) {
        // e.printStackTrace();
        // }
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
