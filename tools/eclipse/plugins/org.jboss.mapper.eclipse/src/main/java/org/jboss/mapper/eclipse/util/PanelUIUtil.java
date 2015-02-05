package org.jboss.mapper.eclipse.util;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ToolBar;
import org.jboss.mapper.eclipse.ModelViewer;
import org.jboss.mapper.eclipse.internal.Handler;

public final class PanelUIUtil {

    /**
     * Create a new JavaUtil.
     */
    private PanelUIUtil() {
    }

    public static CTabFolder createTabFolder(Composite parent) {
        CTabFolder tabFolder = new CTabFolder(parent, SWT.BORDER);
        tabFolder.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
        tabFolder.setBackground( parent.getDisplay().getSystemColor( SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT ) );
        return tabFolder;
    }

    public static ToolBar createTabFolderToolbar(CTabFolder parent) {
        final ToolBar toolBar = new ToolBar(parent, SWT.RIGHT);
        parent.setTopRight(toolBar);
        return toolBar;
    }

    public static CTabItem createTab(CTabFolder tabFolder, Handler handler) {
        final CTabItem tab = new CTabItem(tabFolder, SWT.NONE, 0);
        if (handler.model() != null) {
            tab.setText(handler.type() + ": " + handler.model().getName());
        } else {
            tab.setText(handler.type());
        }
        tab.setShowClose(true);
        return tab;
    }

    public static ModelViewer createModelViewer(CTabFolder tabFolder, CTabItem tab, Handler handler) {
        if (tab == null || tab.isDisposed()) {
            tab = createTab(tabFolder, handler);
        }
        if (handler.model() != null) {
            tab.setText(handler.type() + ": " + handler.model().getName());
        } else {
            tab.setText(handler.type());
        }
        
        final ModelViewer viewer = new ModelViewer(tabFolder, handler.model());
        tab.setControl(viewer);
        
        viewer.setLayoutData(GridDataFactory.fillDefaults().grab(true, true).create());
        handler.configureDragAndDrop(viewer);
        viewer.setInput(handler.model());
        viewer.layout();
        
        tabFolder.setSelection(tab);
        
        return viewer;
    }
    
}
