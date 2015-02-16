Milestone 3 - Transformation App
================================
This application provides an example of a Camel application which uses the tooling support in milestone 3 to support data transformation.  The application demonstrates a transformation from an XML input to a JSON output.  The input and output models have different structures, so this is more than a simple data binding problem.

A complete list of issues addressed in milestone 3 can be found here:
https://github.com/fabric8io/data-mapper/issues?q=is%3Aissue+is%3Aclosed+milestone%3A%22Milestone+3%22

**Important note on configuration**: this application contains two Camel configuration files:
* META-INF/spring/camel-context.xml : Spring-based configuration of a Camel application
* OSGI-INF/blueprint/camel-blueprint.xml : OSGi Blueprint-based configuration of a Camel application

Why two configuration files?  We want to demonstrate that the mapper works with both types of configuration.  If you want to use Spring, then select the Spring configuration in the Data Mapping wizard.  Once you are done with your mapping, you can test it out using ``mvn camel:run``.  If you want to deploy the application to Karaf, then reference the OSGi Blueprint configuration in the Data Mapping wizard and deploy using the features.xml present in the project.


####Running the App
To build this project use
```
mvn install
```
To run this project with Maven use
```
mvn camel:run
```

The application picks up input XML from the src/data directory and drops output JSON into the target/messages directory.  Note that you will need to kill the Java process (^C in terminal) when using "mvn camel:run", which is the default behavior of this plugin.

####Notable Bits
[Input XML](https://github.com/kcbabo/sandbox/blob/master/mapper/examples/map-3/src/data/abc-order.xml)

[Input XSD](https://github.com/kcbabo/sandbox/blob/master/mapper/examples/map-3/src/main/resources/abc-order.xsd)

[Generated Java Input](https://github.com/kcbabo/sandbox/tree/master/mapper/examples/map-3/src/main/java/abcorder)

[Generated Java Output](https://github.com/kcbabo/sandbox/tree/master/mapper/examples/map-3/src/main/java/xyzorderschema)

[Camel Config](https://github.com/kcbabo/sandbox/blob/master/mapper/examples/map-3/src/main/resources/META-INF/spring/camel-context.xml)

[Dozer Config](https://github.com/kcbabo/sandbox/blob/master/mapper/examples/map-3/src/main/resources/dozerBeanMapping.xml)

[Output JSON Schema](https://github.com/kcbabo/sandbox/blob/master/mapper/examples/map-3/src/main/resources/xyz-order-schema.json)

[Output JSON](https://github.com/kcbabo/sandbox/blob/master/mapper/examples/map-3/src/data/xyz-order.json)
