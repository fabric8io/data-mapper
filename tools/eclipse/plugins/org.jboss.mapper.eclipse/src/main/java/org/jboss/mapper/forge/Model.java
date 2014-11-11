/*
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,  
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.mapper.forge;

import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings( "javadoc" )
public class Model {
    
    private Class< ? > modelClass;
    private String name;
    private String type;
    private Model parent;
    private final HashMap< String, Model > children = new HashMap<>();
    private boolean isCollection;
    
    public Model( final String name,
                  final String type ) {
        this.name = name;
        this.type = type;
    }
    
    public Model addChild( final String name,
                           final String type ) {
        final Model n = new Model( name, type );
        n.parent = this;
        n.name = name;
        n.type = type;
        children.put( name, n );
        return n;
    }
    
    private String format( final Model node,
                           final int depth ) {
        final StringBuffer sb = new StringBuffer();
        for ( int i = 0; i < depth; i++ ) {
            sb.append( "  " );
        }
        sb.append( node.children.isEmpty() ? "- " : "* " );
        sb.append( node.name + " : " + node.type );
        return sb.toString();
    }
    
    public Model get( final String nodeName ) {
        return children.get( nodeName );
    }
    
    public Class< ? > getModelClass() {
        return modelClass;
    }
    
    public String getName() {
        return name;
    }
    
    public Model getParent() {
        return parent;
    }
    
    public String getType() {
        return type;
    }
    
    public boolean isCollection() {
        return isCollection;
    }
    
    public List< String > listFields() {
        final List< String > fields = new LinkedList<>();
        return listFields( fields, this.children.values(), "" );
    }
    
    public List< String > listFields( final List< String > fieldList,
                                      final Collection< Model > fields,
                                      final String prefix ) {
        for ( final Model field : fields ) {
            fieldList.add( prefix + field.getName() );
            listFields( fieldList, field.children.values(), prefix + field.getName() + "." );
        }
        return fieldList;
    }
    
    public void print( final PrintStream out ) {
        printModel( this, 0, out );
    }
    
    private void printModel( final Model node,
                             final int depth,
                             final PrintStream out ) {
        out.println( format( node, depth ) );
        for ( final Model child : node.children.values() ) {
            printModel( child, depth + 1, out );
        }
    }
    
    public Model setIsCollection( final boolean isCollection ) {
        this.isCollection = isCollection;
        return this;
    }
    
    public void setModelClass( final Class< ? > modelClass ) {
        this.modelClass = modelClass;
    }
}
