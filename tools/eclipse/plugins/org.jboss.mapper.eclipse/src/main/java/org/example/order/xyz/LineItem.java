package org.example.order.xyz;

import javax.annotation.Generated;

/**
 * LineItem
 * <p>
 * 
 * 
 */
// @JsonInclude(JsonInclude.Include.NON_NULL)
@Generated( "org.jsonschema2pojo" )
// @JsonPropertyOrder( {
// "itemId",
// "amount",
// "cost"
// } )
public class LineItem {
    
    // @JsonProperty( "itemId" )
    private String itemId;
    // @JsonProperty( "amount" )
    private int amount;
    // @JsonProperty( "cost" )
    private double cost;
    
    /**
     * 
     * @return The amount
     */
    // @JsonProperty( "amount" )
    public int getAmount() {
        return amount;
    }
    
    /**
     * 
     * @return The cost
     */
    // @JsonProperty( "cost" )
    public double getCost() {
        return cost;
    }
    
    /**
     * 
     * @return The itemId
     */
    // @JsonProperty( "itemId" )
    public String getItemId() {
        return itemId;
    }
    
    /**
     * 
     * @param amount
     *            The amount
     */
    // @JsonProperty( "amount" )
    public void setAmount( final int amount ) {
        this.amount = amount;
    }
    
    /**
     * 
     * @param cost
     *            The cost
     */
    // @JsonProperty( "cost" )
    public void setCost( final double cost ) {
        this.cost = cost;
    }
    
    /**
     * 
     * @param itemId
     *            The itemId
     */
    // @JsonProperty( "itemId" )
    public void setItemId( final String itemId ) {
        this.itemId = itemId;
    }
    
}
