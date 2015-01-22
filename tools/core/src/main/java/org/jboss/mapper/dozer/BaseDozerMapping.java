package org.jboss.mapper.dozer;

import org.jboss.mapper.dozer.config.Field;
import org.jboss.mapper.dozer.config.Mapping;

public abstract class BaseDozerMapping {
    
    private Mapping mapping;
    private Field field;

    protected BaseDozerMapping(Mapping mapping, Field field) {
        this.mapping = mapping;
        this.field = field;
    }

    /**
     * Deletes this field mapping from the Dozer configuration.
     */
    public void delete() {
        mapping.getFieldOrFieldExclude().remove(field);
    }
}
