package org.jboss.mapper.model;

import java.math.BigDecimal;

import junit.framework.Assert;

import org.junit.Test;

public class ModelBuilderTest {

    @Test
    public void noSuper() {
        Model model = ModelBuilder.fromJavaClass(NoSuper.class);
        Assert.assertEquals(2, model.listFields().size());
    }

    @Test
    public void superSuper() {
        Model model = ModelBuilder.fromJavaClass(SuperSuper.class);
        Assert.assertEquals(3, model.listFields().size());
    }
    
    @Test
    public void screenForNumbers() {
        Model model = ModelBuilder.fromJavaClass(ContainsNumber.class);
        Assert.assertEquals(1, model.listFields().size());
    }
}

class NoSuper {
    private String fieldOne;
    private String fieldTwo;
    
    public String getFieldOne() {
        return fieldOne;
    }
    
    public void setFieldOne(String fieldOne) {
        this.fieldOne = fieldOne;
    }
    
    public String getFieldTwo() {
        return fieldTwo;
    }
    
    public void setFieldTwo(String fieldTwo) {
        this.fieldTwo = fieldTwo;
    }
}

class SuperSuper extends NoSuper {
    private String fieldThree;

    public String getFieldThree() {
        return fieldThree;
    }

    public void setFieldThree(String fieldThree) {
        this.fieldThree = fieldThree;
    }
    
}

class ContainsNumber {
    private BigDecimal bigNum;

    public BigDecimal getBigNum() {
        return bigNum;
    }

    public void setBigNum(BigDecimal bigNum) {
        this.bigNum = bigNum;
    }
    
}