Milestone 2 - Transformation App
================================
This application provides an example of a Camel application which uses the tooling support in milestone 2 to support data transformation.  The application demonstrates a transformation from an XML input to a JSON output.  The input and output models have different structures, so this is more than a simple data binding problem.

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
[Input XML](https://github.com/kcbabo/sandbox/blob/master/mapper/examples/map-2/src/data/abc-order.xml)

[Input XSD](https://github.com/kcbabo/sandbox/blob/master/mapper/examples/map-2/src/main/resources/abc-order.xsd)

[Generated Java Input](https://github.com/kcbabo/sandbox/tree/master/mapper/examples/map-2/src/main/java/abcorder)

[Generated Java Output](https://github.com/kcbabo/sandbox/tree/master/mapper/examples/map-2/src/main/java/xyzorderschema)

[Camel Config](https://github.com/kcbabo/sandbox/blob/master/mapper/examples/map-2/src/main/resources/META-INF/spring/camel-context.xml)

[Dozer Config](https://github.com/kcbabo/sandbox/blob/master/mapper/examples/map-2/src/main/resources/dozerBeanMapping.xml)

[Output JSON Schema](https://github.com/kcbabo/sandbox/blob/master/mapper/examples/map-2/src/main/resources/xyz-order-schema.json)

[Output JSON](https://github.com/kcbabo/sandbox/blob/master/mapper/examples/map-2/src/data/xyz-order.json)
