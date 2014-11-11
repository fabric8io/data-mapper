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

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

@SuppressWarnings( "javadoc" )
public class ModelBuilder {
    
    private static void addFieldsToModel( final Field[] fields,
                                          final Model model ) {
        for ( final Field field : fields ) {
            String fieldType;
            Field[] childFields = null;
            boolean isCollection = false;
            
            if ( field.getType().isArray() ) {
                isCollection = true;
                fieldType = getListName( field.getType().getComponentType() );
                childFields = field.getType().getComponentType().getDeclaredFields();
            } else if ( Collection.class.isAssignableFrom( field.getType() ) ) {
                isCollection = true;
                final Type t = field.getGenericType();
                if ( t instanceof ParameterizedType ) {
                    final Class< ? > tClass = ( Class< ? > ) ( ( ParameterizedType ) t ).getActualTypeArguments()[ 0 ];
                    fieldType = getListName( tClass );
                    childFields = tClass.getDeclaredFields();
                } else {
                    fieldType = getListName( Object.class );
                }
            } else {
                fieldType = field.getType().getName();
                if ( !field.getType().isPrimitive()
                     && !field.getType().getName().equals( String.class.getName() )
                     && !field.getType().getName().startsWith( "java.lang" ) ) {
                    
                    childFields = field.getType().getDeclaredFields();
                }
            }
            
            final Model child = model.addChild( field.getName(), fieldType );
            child.setIsCollection( isCollection );
            if ( childFields != null ) {
                addFieldsToModel( childFields, child );
            }
        }
    }
    
    public static Model fromJavaClass( final Class< ? > javaClass ) {
        final Model model = new Model( javaClass.getSimpleName(), javaClass.getName() );
        addFieldsToModel( javaClass.getDeclaredFields(), model );
        model.setModelClass( javaClass );
        return model;
    }
    
    public static Class< ? > getFieldType( final Field field ) {
        Class< ? > type;
        
        if ( field.getType().isArray() ) {
            return field.getType().getComponentType();
        } else if ( Collection.class.isAssignableFrom( field.getType() ) ) {
            final Type t = field.getGenericType();
            if ( t instanceof ParameterizedType ) {
                type = ( Class< ? > ) ( ( ParameterizedType ) t ).getActualTypeArguments()[ 0 ];
            } else {
                type = Object.class;
            }
        } else {
            type = field.getType();
        }
        
        return type;
    }
    
    public static String getListName( final Class< ? > listType ) {
        return "[" + listType.getName() + "]";
    }
    
    public static String getListType( final String listName ) {
        return listName.split( "\\[" )[ 1 ].split( "\\]" )[ 0 ];
    }
}
