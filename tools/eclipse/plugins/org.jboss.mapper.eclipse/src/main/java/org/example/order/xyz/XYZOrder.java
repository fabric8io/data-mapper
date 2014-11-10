package org.example.order.xyz;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Generated;

/**
 * XYZOrder
 * <p>
 * 
 * 
 */
// @JsonInclude(JsonInclude.Include.NON_NULL)
@Generated( "org.jsonschema2pojo" )
// @JsonPropertyOrder({
// "custId",
// "priority",
// "orderId",
// "lineItems"
// })
public class XYZOrder {
    
    // @JsonProperty( "custId" )
    private String custId;
    // @JsonProperty( "priority" )
    private String priority;
    // @JsonProperty( "orderId" )
    private String orderId;
    // @JsonProperty( "lineItems" )
    private List< LineItem > lineItems = new ArrayList< LineItem >();
    
    /**
     * 
     * @return The custId
     */
    // @JsonProperty( "custId" )
    public String getCustId() {
        return custId;
    }
    
    /**
     * 
     * @return The lineItems
     */
    // @JsonProperty( "lineItems" )
    public List< LineItem > getLineItems() {
        return lineItems;
    }
    
    /**
     * 
     * @return The orderId
     */
    // @JsonProperty( "orderId" )
    public String getOrderId() {
        return orderId;
    }
    
    /**
     * 
     * @return The priority
     */
    // @JsonProperty( "priority" )
    public String getPriority() {
        return priority;
    }
    
    /**
     * 
     * @param custId
     *            The custId
     */
    // @JsonProperty( "custId" )
    public void setCustId( final String custId ) {
        this.custId = custId;
    }
    
    /**
     * 
     * @param lineItems
     *            The lineItems
     */
    // @JsonProperty( "lineItems" )
    public void setLineItems( final List< LineItem > lineItems ) {
        this.lineItems = lineItems;
    }
    
    /**
     * 
     * @param orderId
     *            The orderId
     */
    // @JsonProperty( "orderId" )
    public void setOrderId( final String orderId ) {
        this.orderId = orderId;
    }
    
    /**
     * 
     * @param priority
     *            The priority
     */
    // @JsonProperty( "priority" )
    public void setPriority( final String priority ) {
        this.priority = priority;
    }
    
}
