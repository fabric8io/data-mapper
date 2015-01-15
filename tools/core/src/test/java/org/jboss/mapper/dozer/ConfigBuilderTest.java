package org.jboss.mapper.dozer;

import java.io.File;
import java.util.List;

import org.jboss.mapper.FieldMapping;
import org.jboss.mapper.Literal;
import org.jboss.mapper.LiteralMapping;
import org.jboss.mapper.MappingOperation;
import org.jboss.mapper.MappingType;
import org.jboss.mapper.dozer.config.Field;
import org.jboss.mapper.dozer.config.Mapping;
import org.jboss.mapper.model.Model;
import org.jboss.mapper.model.ModelBuilder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ConfigBuilderTest {
    
    private static final File CONFIG_ROOT = 
            new File("target/test-classes/org/jboss/mapper/dozer");
    
    private Model modelA;
    private Model modelB;
    
    @Before
    public void setUp() {
        modelA = new Model("A", "example.a");
        modelA.addChild("A1", "java.lang.String");
        modelA.addChild("A2", "java.lang.String");
        
        modelB = new Model("B", "example.b");
        modelB.addChild("B1", "java.lang.String");
        modelB.addChild("B2", "java.lang.String");
        modelB.addChild("B3", "java.lang.String");
    }
    
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
    
    @Test
    public void clearMappings() throws Exception {
        ConfigBuilder config = loadConfig("emptyDozerMapping.xml");
        config.map(modelA.get("A1"), modelB.get("B1"));
        Assert.assertEquals(1, config.getMappings().size());
        config.clearMappings();
        Assert.assertEquals(0, config.getMappings().size());
    }
    
    @Test
    public void getMappings() throws Exception {
        ConfigBuilder config = loadConfig("fieldAndLiteralMapping.xml");
        Assert.assertEquals(2, config.getMappings().size());
        int fieldMappings = 0;
        int literalMappings = 0;
        for (MappingOperation<?,?> mapping : config.getMappings()) {
            if (MappingType.LITERAL.equals(mapping.getType())) {
                fieldMappings++;
            } else if (MappingType.FIELD.equals(mapping.getType())) {
                literalMappings++;
            }
        }
        Assert.assertEquals(1, fieldMappings);
        Assert.assertEquals(1, literalMappings);
    }
    
    @Test
    public void getLiterals() throws Exception {
        ConfigBuilder config = loadConfig("fieldAndLiteralMapping.xml");
        Literal acme = new Literal("ACME");
        config.map(acme, modelB.get("B3"));
        List<Literal> literals = config.getLiterals();
        Assert.assertTrue(literals.contains(acme));
        Assert.assertEquals(2, literals.size());
    }
    
    @Test
    public void mapField() throws Exception {
        ConfigBuilder config = loadConfig("emptyDozerMapping.xml");
        config.map(modelA.get("A1"), modelB.get("B1"));
        FieldMapping mapping = (FieldMapping)config.getMappings().get(0);
        Assert.assertEquals("A1", mapping.getSource());
        Assert.assertEquals("B1", mapping.getTarget());
    }
    
    @Test
    public void mapLiteral() throws Exception {
        ConfigBuilder config = loadConfig("emptyDozerMapping.xml");
        Literal literal = new Literal("literally?!");
        config.map(literal, modelB.get("B1"));
        LiteralMapping mapping = (LiteralMapping)config.getMappings().get(0);
        Assert.assertEquals(literal, mapping.getSource());
        Assert.assertEquals("B1", mapping.getTarget());
    }
    
    private ConfigBuilder loadConfig(String configName) throws Exception {
        return ConfigBuilder.loadConfig(new File(CONFIG_ROOT, configName));
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