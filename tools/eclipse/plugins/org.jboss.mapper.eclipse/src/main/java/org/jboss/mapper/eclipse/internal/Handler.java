package org.jboss.mapper.eclipse.internal;

import java.util.Stack;

import org.jboss.mapper.eclipse.ModelViewer;
import org.jboss.mapper.model.Model;

public interface Handler {
    void configureDragAndDrop( ModelViewer viewer );

    Model model();

    void setModel( Model model );

    String type();
    
    Stack<Model> getModelHistory();

}
