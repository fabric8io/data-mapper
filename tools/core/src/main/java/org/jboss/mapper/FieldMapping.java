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
package org.jboss.mapper;

/**
 * A basic mapping operation where one field is assigned to another field.
 * The source and target types are Strings which contain the name of the field 
 * being mapped.
 */
public class FieldMapping implements MappingOperation<String, String> {
    
    private String source;
    private String target;
    
    /**
     * Create a new FieldMapping.
     * @param source name of the source model field
     * @param target name of the target model field
     */
    public FieldMapping(String source, String target) {
        this.source = source;
        this.target = target;
    }

    /**
     * Returns the source model field name.
     */
    @Override
    public String getSource() {
        return source;
    }
    
    /**
     * Returns the target model field name.
     */
    @Override
    public String getTarget() {
        return target;
    }
    
    @Override
    public MappingType getType() {
        return MappingType.FIELD;
    }
}
