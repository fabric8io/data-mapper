package org.jboss.mapper.dozer;

import org.jboss.mapper.dozer.config.Field;
import org.jboss.mapper.dozer.config.Mapping;
import org.jboss.mapper.model.Model;
import org.jboss.mapper.model.ModelBuilder;
import org.junit.Assert;
import org.junit.Test;


public class ConfigBuilderTest {

    /*
     * Verifies that mapping a field that is not a direct child of the class
     * mapping is prefixed correctly.
     */
    @Test
    public void mapGrandchild() throws Exception {
        ConfigBuilder config = ConfigBuilder.newConfig();
        Model modelA = ModelBuilder.fromJavaClass(A.class);
        Model modelB = ModelBuilder.fromJavaClass(B.class);
        config.addClassMapping(modelA.getType(), modelB.getType());
        config.map(modelA.get("data"), modelB.get("c").get("d").get("data"));
        
       Mapping mapping = config.getClassMapping(modelA);
       Field field = (Field)mapping.getFieldOrFieldExclude().get(0);
       Assert.assertEquals("data", field.getA().getContent());
       Assert.assertEquals("c.d.data", field.getB().getContent());
    }
    
    /*
     * Basic validation of a field mapping.
     */
    @Test
    public void mapFields() throws Exception {
        ConfigBuilder config = ConfigBuilder.newConfig();
        Model modelA = ModelBuilder.fromJavaClass(A.class);
        Model modelB = ModelBuilder.fromJavaClass(B.class);
        config.addClassMapping(modelA.getType(), modelB.getType());
        config.map(modelA.get("data"), modelB.get("data"));
        
       Mapping mapping = config.getClassMapping(modelA);
       Field field = (Field)mapping.getFieldOrFieldExclude().get(0);
       Assert.assertEquals("data", field.getA().getContent());
       Assert.assertEquals("data", field.getB().getContent());
    }
}

class A {
    private String data;
}

class B {
    private C c;
    private String data;
}

class C {
    private D d;
}

class D {
    private String data;
}