package example;

/**
 * Custom mapping class with multiple operations.
 */
public class CustomMapperOperations {

    public Object custom1(String source) {
        return "custom1:" + source.toString();
    }
    
    public Object custom2(String source) {
        return "custom2:" + source.toString();
    }
}
