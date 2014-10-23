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
package org.example.order.xyz;

public class XYZOrder {

    private String custId;
    private String priority;
    private String orderId;
    private LineItem[] lineItems;
    
    public String getCustId() {
        return custId;
    }
    
    public XYZOrder setCustId(String custId) {
        this.custId = custId;
        return this;
    }
    
    public String getPriority() {
        return priority;
    }
    
    public XYZOrder setPriority(String priority) {
        this.priority = priority;
        return this;
    }
    
    public String getOrderId() {
        return orderId;
    }
    
    public XYZOrder setOrderId(String orderId) {
        this.orderId = orderId;
        return this;
    }

    public LineItem[] getLineItems() {
        return lineItems;
    }

    public void setLineItems(LineItem[] lineItems) {
        this.lineItems = lineItems;
    }
    
    public String toString() {
        String order =
               "============== XYZ Order =============\n"
               + "customerId : " + custId + "\n"
               + "priority : " + priority + "\n"
               + "orderId : " + orderId + "\n"
               + "============= Line Items =============\n";
        if (lineItems != null) {
            for (LineItem item : lineItems) {
                order += "item id : " + item.getItemId() + "\n"
                        + "cost : " + item.getCost() + "\n"
                        + "amount : " + item.getAmount() + "\n";
            }
        }
        
        return order;
    }
    
}
