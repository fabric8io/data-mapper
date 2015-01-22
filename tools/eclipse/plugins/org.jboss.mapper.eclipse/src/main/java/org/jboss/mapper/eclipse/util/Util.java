package org.jboss.mapper.eclipse.util;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.jboss.mapper.eclipse.Activator;

/**
 *
 */
public class Util {

    private static void populateResources( Shell shell,
                                           IContainer container,
                                           List< IResource > resources ) {
        try {
            for ( final IResource resource : container.members() ) {
                if ( resource instanceof IContainer ) populateResources( shell, ( IContainer ) resource, resources );
                else resources.add( resource );
            }
        } catch ( final Exception e ) {
            Activator.error( shell, e );
        }
    }

    /**
     * @param shell
     * @param project
     * @param schemaType
     * @param fileText
     * @return the selected file
     */
    public static String selectFile( final Shell shell,
                                final IProject project,
                                final String schemaType,
                                final Text fileText ) {
        final int flags = JavaElementLabelProvider.SHOW_DEFAULT |
                          JavaElementLabelProvider.SHOW_POST_QUALIFIED |
                          JavaElementLabelProvider.SHOW_ROOT;
        final ElementListSelectionDialog dlg =
            new ElementListSelectionDialog( shell, new JavaElementLabelProvider( flags ) {

                @Override
                public String getText( Object element ) {
                    return super.getText( element ) + " - " + ( ( IResource ) element ).getParent().getFullPath().makeRelative();
                }
            } );
        dlg.setTitle( "Select " + schemaType );
        dlg.setMessage( "Select the " + schemaType + " file for the transformation" );
        dlg.setMatchEmptyString( true );
        dlg.setHelpAvailable( false );
        final List< IResource > resources = new ArrayList<>();
        populateResources( shell, project, resources );
        dlg.setElements( resources.toArray() );
        if ( dlg.open() == Window.OK ) return ( ( IFile ) dlg.getFirstResult() ).getProjectRelativePath().toString();
        return null;
    }

    private Util() {}
}
