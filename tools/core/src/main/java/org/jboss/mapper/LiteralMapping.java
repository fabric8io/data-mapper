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
 * A LiteralMapping represents a mapping where the source is a string literal,
 * represented by Literal, and the target is a model field.
 */
public class LiteralMapping implements MappingOperation<Literal, String> {

    private Literal source;
    private String target;

    /**
     * Create a new LiteralMapping.
     * @param source source literal
     * @param target target field name
     */
    public LiteralMapping(Literal source, String target) {
        this.source = source;
        this.target = target;
    }
    
    /**
     * Returns the literal used as the source for this mapping.
     */
    @Override
    public Literal getSource() {
        return source;
    }
    
    /**
     * Returns the field name used as the target for this mapping.
     */
    @Override
    public String getTarget() {
        return target;
    }
    
    @Override
    public MappingType getType() {
        return MappingType.LITERAL;
    }
}
