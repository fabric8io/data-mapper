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

import java.io.File;
import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.jboss.mapper.dozer.config.Field;
import org.jboss.mapper.dozer.config.FieldDefinition;
import org.jboss.mapper.dozer.config.Mapping;
import org.jboss.mapper.dozer.config.Mappings;

public class ConfigBuilder {
	
	private static final String DOZER_SCHEMA_LOC = 
		"http://dozer.sourceforge.net http://dozer.sourceforge.net/schema/beanmapping.xsd";
	
	// JAXB classes for Dozer config model
	private JAXBContext jaxbCtx;
	private Mappings mapConfig;
	
	private ConfigBuilder() {
		this(new Mappings());
	}
	
	private ConfigBuilder(Mappings mapConfig) {
		this.mapConfig = mapConfig;
	}
	
	private ConfigBuilder(File file) throws Exception {
		mapConfig = (Mappings)getJAXBContext().createUnmarshaller().unmarshal(file);
	}
	
	public static ConfigBuilder newConfig() {
		return new ConfigBuilder();
	}
	
	public static ConfigBuilder loadConfig(File file) throws Exception {
		return new ConfigBuilder(file);
	}
	
	public void saveConfig(OutputStream output) throws Exception {
		Marshaller m = getJAXBContext().createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		m.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, DOZER_SCHEMA_LOC);
		m.marshal(mapConfig, output);
	}
	
	public Mappings getMappings() {
		return mapConfig;
	}
	
	// Adds a <class-a> and <class-b> mapping definition to the dozer config.
	// If multiple fields within a class are being mapped, this should only
	// be called once.
	public void addClassMapping(String fromClass, String toClass) {
		Mapping map = new Mapping();
		org.jboss.mapper.dozer.config.Class classA = new org.jboss.mapper.dozer.config.Class();
		org.jboss.mapper.dozer.config.Class classB = new org.jboss.mapper.dozer.config.Class();
		classA.setContent(fromClass);
		classB.setContent(toClass);
		map.setClassA(classA);
		map.setClassB(classB);
		mapConfig.getMapping().add(map);
	}
	
	public void map(Model source, Model target) {
		// Only add a class mapping if one has not been created already
		if (requiresClassMapping(source.getParent(), target.getParent())) {
			String sourceType = source.getParent().isCollection() 
					? ModelBuilder.getListType(source.getParent().getType())
				    : source.getParent().getType();
			String targetType = target.getParent().isCollection() 
					? ModelBuilder.getListType(target.getParent().getType())
				    : target.getParent().getType();
			addClassMapping(sourceType, targetType);
		}
		
		// Add field mapping details for the source and target
		addFieldMapping(source, target);
	}
	
	boolean requiresClassMapping(Model source, Model target) {
		// If a class mapping already exists, then no need to add a new one
		if (getClassMapping(source) != null || getClassMapping(target) != null) {
			return false;
		}
		
		return true;
	}
	
	// Return an existing mapping which includes the specified node's parent
	// as a source or target.  This basically fetches the mapping definition 
	// under which a field mapping can be defined.
	Mapping getClassMapping(Model model) {
		Mapping mapping = null;
		String type = model.isCollection() 
				? ModelBuilder.getListType(model.getType()) : model.getType();
		
		for (Mapping m : mapConfig.getMapping()) {
			if ((m.getClassA().getContent().equals(type)
					|| m.getClassB().getContent().equals(type))) {
				mapping = m;
				break;
			}
		}
		return mapping;
	}
	
	// Add a field mapping to the dozer config.
	void addFieldMapping(Model source, Model target) {
		boolean sourceClassMapping = getClassMapping(source.getParent()) != null;
		boolean targetClassMapping = getClassMapping(target.getParent()) != null;
		
		Mapping mapping = sourceClassMapping ? getClassMapping(source.getParent())
				: getClassMapping(target.getParent());
		
		Field field = new Field();
		field.setA(createField(source, !sourceClassMapping));
		field.setB(createField(target, !targetClassMapping));
		mapping.getFieldOrFieldExclude().add(field);
	}
	
	
	
	static FieldDefinition createField(Model model, boolean includeParentPrefix) {
		FieldDefinition fd = new FieldDefinition();
		String name = model.getName();
		if (includeParentPrefix) {
			fd.setContent(model.getParent().getName() + "." + name);
		} else {
			fd.setContent(name);
		}
		return fd;
	}
	
	private synchronized JAXBContext getJAXBContext() {
		if (jaxbCtx == null) {
			try {
				jaxbCtx = JAXBContext.newInstance("org.jboss.mapper.dozer.config");
			} catch (JAXBException jaxbEx) {
				throw new RuntimeException(jaxbEx);
			}
		}
		return jaxbCtx;
	}
	
	public static void main(String[] args) throws Exception {
		/*
		
		// Builds data models off imported type models.  Simulates a user 
		// importing a source and target model in the editor.
		Node source = createSourceNode();
		Node target = createTargetNode();
		
		// We know we will be mapping the top-level types, so as soon as the 
		// user defines the source and target, go ahead and add a class-level
		// mapping for those types.
		addClassMapping(source.type, target.type);
		
		// The following calls mimic a user creating mappings between source 
		// and target fields in the editor.
		
		// User maps header values
		map(source.get("header").get("customerNum"),
		    target.get("custId"));
		map(source.get("header").get("orderNum"),
			    target.get("orderId"));
		map(source.get("header").get("status"),
			    target.get("priority"));
		
		// User maps line items
		map(source.get("orderItems").get("item").get("quantity"),
			    target.get("lineItems").get("amount"));
		map(source.get("orderItems").get("item"),
			    target.get("lineItems"));
		map(source.get("orderItems").get("item").get("id"),
			    target.get("lineItems").get("itemId"));
		map(source.get("orderItems").get("item").get("price"),
			    target.get("lineItems").get("cost"));
		
		// Print out the completed mapping config as XML
		JAXBContext jc = JAXBContext.newInstance("org.dozer.config");
		Marshaller m = jc.createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		m.marshal(mapConfig, System.out);
		*/
	}
	
	/*
	
	// Creates a data model corresponding to the ABCOrder class.
	static Node createSourceNode() {
		final String ABC_ORDER_TYPE = "org.example.order.abc.ABCOrder";
		final String STRING_TYPE = "java.lang.String";
		// build top level order
		Node abcOrder = new Node();
		abcOrder.name = "ABCOrder";
		abcOrder.type = ABC_ORDER_TYPE;
		
		// build header
		Node header = abcOrder.addChild("header", ABC_ORDER_TYPE + ".Header");
		header.addChild("status", STRING_TYPE);
		header.addChild("customerNum", STRING_TYPE);
		header.addChild("orderNum", STRING_TYPE);
		
		// add line items
		Node orderItems = abcOrder.addChild("orderItems", ABC_ORDER_TYPE + ".OrderItems");
		Node itemList = orderItems.addChild("item", ABC_ORDER_TYPE + ".OrderItems.Item");
		itemList.isCollection = true;
		itemList.addChild("id", STRING_TYPE);
		itemList.addChild("price", "float");
		itemList.addChild("quantity", "short");
		return abcOrder;
	}
	
	// Creates a data model corresponding to the XYZOrder class.
	static Node createTargetNode() {
		final String XYZ_ORDER_TYPE = "org.example.order.xyz.XYZOrder";
		final String STRING_TYPE = "java.lang.String";
		// build top level order
		Node xyzOrder = new Node();
		xyzOrder.name = "XYZOrder";
		xyzOrder.type = XYZ_ORDER_TYPE;
		
		xyzOrder.addChild("priority", STRING_TYPE);
		xyzOrder.addChild("custId", STRING_TYPE);
		xyzOrder.addChild("orderId", STRING_TYPE);
		

		Node lineItems = xyzOrder.addChild("lineItems", "org.example.order.xyz.LineItem");
		lineItems.isCollection = true;
		lineItems.addChild("amount", "float");
		lineItems.addChild("cost", "short");
		lineItems.addChild("itemId", STRING_TYPE);
		
		return xyzOrder;
	}
	*/
}
