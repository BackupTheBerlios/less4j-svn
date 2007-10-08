package org.less4j.tests;

import org.less4j.*;
import java.io.File;
import java.net.URL;
import java.util.Iterator;

public class TestSOAP extends SOAP {
    
    public static final TestSOAP singleton = new TestSOAP();

    public String jsonInterface (Actor $) {
        return JSON.encode(JSON.dict(new Object[]{
            "Request", JSON.list(new Object[]{
                JSON.dict(new Object[]{
                    "item", "[0-9A-Z]{12}", 
                    "quantity", new Integer(20),
                    "description", ".+",
                    "price", new Integer(100)
                    })
                }),
            "Response", JSON.list(new Object[]{
                JSON.dict(new Object[]{
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
        return null; // ... here goes your business logic
    }
        
    
    
    /**
     * Try to write the WSDL description of this class' 
     * <code>jsonInterface</code>, use the first argument as service 
     * URL, derive the operation name from the last.
     * 
     * <h4>Synopsis</h4>
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        // write the WSDL document to a named file
        String space = "urn:Test", name = null;
        if (args.length > 0)
            space = args[0];
        if (space.startsWith("urn:"))
            name = space.substring(4);
        else {
            URL url = new URL(space);
            Iterator parts = Simple.split(url.getPath(), '/');
            while(parts.hasNext()) 
                name = (String) parts.next();
        }
        SOAP.wsdl(space.toString(), name, JSONR.compile(
            singleton.jsonInterface(new Actor())
            )).write(new File(name + ".xml")); // do you like one-liner too?
    }

}
