package org.jboss.mapper.camel.transform;

import org.dozer.ConfigurableCustomConverter;

public class LiteralMapper implements ConfigurableCustomConverter {
    
    private String literal;
    
    @Override
    public Object convert(Object existingDestinationFieldValue, 
            Object sourceFieldValue, 
            Class<?> destinationClass,
            Class<?> sourceClass) {
        return literal;
    }
    
    public String getLiteral() {
        return literal;
    }

    public void setLiteral(String literal) {
        this.literal = literal;
    }

    @Override
    public void setParameter(String parameter) {
        literal = parameter;
    }

}
