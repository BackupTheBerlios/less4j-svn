package org.less4j.tests;

import org.less4j.*;

public class TestSOAP extends SOAP {
    
    public static final TestSOAP singleton = new TestSOAP();

    public String jsonInterface (Actor $) {
        return JSON.encode(JSON.object(new Object[]{
            "Request", JSON.array(new Object[]{
                JSON.object(new Object[]{
                    "item", "[0-9A-Z]{12}", 
                    "quantity", new Integer(20),
                    "description", ".+",
                    "price", new Integer(100)
                    })
                }),
            "Response", JSON.array(new Object[]{
                JSON.object(new Object[]{
                    "item", "[0-9A-Z]{12}",
                    "quantity", new Integer(20),
                    "price", new Integer(100)
                    })
                })
            }));
    }

    public void call (Actor $, Document request) throws Throwable {
        response($, purchaseOrder (
           $.json.S("item"), 
           $.json.I("quantity").intValue(), 
           $.json.S("description")
           ));
    }
    
    public static Object purchaseOrder (
        String item, int quantity, String description
        ) {
        return "rejected"; // ... here goes your business logic
    }
        
    public static void main(String[] args) throws Exception {
    }

}
