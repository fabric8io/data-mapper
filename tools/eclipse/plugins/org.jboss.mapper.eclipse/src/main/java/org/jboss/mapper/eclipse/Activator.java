package org.jboss.mapper.eclipse;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {
    
    // The shared instance
    private static Activator plugin;
    
    /**
     * Returns the shared instance
     * 
     * @return the shared instance
     */
    public static Activator plugin() {
        return plugin;
    }
    
    /**
     * The constructor
     */
    public Activator() {}
    
    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
     */
    @Override
    public void start( final BundleContext context ) throws Exception {
        super.start( context );
        plugin = this;
    }
    
    /**
     * {@inheritDoc}
     * 
     * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
     */
    @Override
    public void stop( final BundleContext context ) throws Exception {
        plugin = null;
        super.stop( context );
    }
}
