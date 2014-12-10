/**
 * Copyright 2014 Turnip Solutions, LLC.
 * All rights reserved.
 */
package org.jboss.mapper.eclipse;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.menus.IMenuService;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.FileEditorInput;
import org.jboss.mapper.dozer.ConfigBuilder;

/**
 * 
 */
public class DataMapperEditorMappingPage extends EditorPart {
    
    private ConfigBuilder configBuilder;
    private DataMapper mapper;
    
    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public void createPartControl( final Composite parent ) {
        mapper = new DataMapper( parent, ( ( FileEditorInput ) getEditorInput() ).getFile() );

        // Create a menu manager and create context menu
        IWorkbench wb = PlatformUI.getWorkbench();
        IWorkbenchWindow win = wb.getActiveWorkbenchWindow();
        IMenuService mSvc = (IMenuService) win.getService(IMenuService.class);
        MenuManager mgr = new MenuManager();
        mSvc.populateContributionManager(mgr, "popup:" + IDataMapperConstants.DM_VIEWER_POPUPMENU);
        mapper.viewer.getTable().setMenu(mgr.createContextMenu(mapper.viewer.getTable()));
        getSite().setSelectionProvider(mapper.viewer);
    }
    
    public DataMapper getMapper() {
        return mapper;
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
