/* Copyright (C) 2006-2007 Laurent A.V. Szyster

This library is free software; you can redistribute it and/or modify
it under the terms of version 2 of the GNU Lesser General Public License as
published by the Free Software Foundation.

   http://www.gnu.org/copyleft/lesser.html

This library is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

You should have received a copy of the GNU Lesser General Public License
along with this library; if not, write to the Free Software Foundation, 
Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA */

package org.less4j;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;

/**
 * A function that supports a practical subset of SOAP 1.1, just enough
 * to provide a <em>really</em> simple XML object notation that is forward
 * compatible with JSON web services.  
 * 
 * <h3>Synopsis</h3>
 * 
 * <p>Here is a web service to test the usual "Hello World" suspect SOAP 
 * remote procedure call:</p>
 * 
 * <pre>import org.less4j.*;
 * 
 *public class echoString extends SOAP {
 *
 *    public void call (Actor $, Document request) throws Throwable {
 *        String arg0 = $.json.O("Body").O("echoString").S("arg0")
 *        response ($, "echoStringResponse", new JSON.Object(
 *            "result", "Hello " + arg0 + " !"
 *            ));
 *    }
 *    
 *}</pre>
 *
 * <p>Note how the SOAP request was mapped to a JSON object, skipping
 * a lot of tedious boilerplate, transient object declaration and dropping 
 * XML namespaces.</p>
 *
 * <p>The small size and decent performances of less4j's SOAP implementation
 * are made possible by a radical design decision: to support the minimal
 * subset of SOAP 1.1 encodings, XSD schemas and WSDL service descriptions.
 * If you doubt about the relevance of that choice, see Google's WSDL
 * declaration.</p>
 *
 * <p>Here less is definitively so much more. There are no error-prone
 * code generation and compilation involved because there is no need to 
 * support the contradictions in the standards or enforce static typing
 * in the service itself.</p>
 *
 * <p>All that simplicity buys a lot of conveniences at a known up-front
 * cost (a dynamic object model). First it helps not to bother too much with
 * the implementation data structures or names, and instead focus on the
 * SOAP, JSON and SQL data and metadata. Second, it helps to keep simple
 * procedure free of anything else but functional programming patterns
 * with names that mean something in the application's network (ie: sources 
 * easy to read and undertsand).</p>
 * 
 * source file and makes its applications forward compatible
 * with the defacto network object notation.</p> 
 *
 * <pre>...
 *&lt;SOAP-ENV:Body &gt;
 *    &lt;echoStringResponse xsi:type="xsd:string" 
 *        &gt;&lt;[CDATA[Hello World!]]&gt;&lt;/echoStringResponse&gt;
 *&lt;/SOAP-ENV:Body&gt;
 *...</pre> 
 *
 * <p>...</p>
 *
 * <h4>JSONR/WSDL Interfaces</h4>
 * 
 * <p>This <code>Function</code> implementation supports a subset of WSDL
 * mapped from a regular JSON expressions. It is restricted to RPC, SOAP 
 * encoding and the XSD equivalents of JSON types. But it supports complex 
 * types as practically as interroperable XML schemas can allow.</p>
 * 
 * <p></p>
 * 
 * <pre>import org.less4j.*;
 * 
 *public class PurchaseOrder extends SOAP {
 *
 *    public String jsonInterface (Actor $) {
 *        return JSON.encode(Simple.dict({
 *            "Request", Simple.dict({
 *                "item", "", 
 *                "quantity", new Integer(0),
 *                "description", ""
 *                "price", new Integer(0),
 *                }),
 *            "Response", Simple.dict({
 *                "status", "accepted|rejected|quoted", 
 *                }),
 *            }));
 *    }
 *
 *    public void call (Actor $, Document request) throws Throwable {
 *        String po = $.json.O("Body").O("Request");
 *        response ($, purchaseOrder (
 *           po.S("item"), po.intValue("quantity", 1), po.S("description")
 *           ));
 *    }
 *    
 *    public static Object purchaseOrder (
 *        String item, int quantity, String description
 *        ) {
 *        // ... here goes your business logic
 *    }
 *    
 *}</pre>
 * 
 * <h4>Request</h4>
 * 
 * <p>Unobviously at first, the SOAP interpreter built-in does not even try
 * to wax this "industy standard" object notation. Instead it maps a simple
 * object access protocol of any XML stream into the flattest and simplest
 * JSON instance possible, considering first CDATA only, discarding attributes 
 * of non-empty elements and using types whenever an element name is mapped 
 * to a regular JSON type.</p>
 * 
 * <p>Feed test XML requests to a plain <code>SOAP.Controller</code> and see 
 * what comes out of it.</p>
 * 
 * <p>The parser applied is XP, the original XML reference implementation
 * in Java and still one of the fastest and leanest. An XML intermediary
 * data structure is used, but useless features and complications are 
 * simply ignored, only the JSON types are supported.</p>
 * 
 * <pre>...</pre> 
 * 
 * <p>Your mileage <em>will</em> vary, this XML request decoder provides
 * its applications with a single view of all messages through a simpler
 * object access protocol. It does not try to make Sun's and Microsoft's 
 * interpretation of the standard more interroperable. Instead it lets
 * you cherry-pick what you need in the whole HTTP request submitted,
 * down to the last attributes of an XML body, making the whole issue
 * goes moot and the implementation "transparent" to its protocol.</p>
 * 
 * <h4>Response</h4>
 * 
 * <p>Simple things should be easy even with SOAP:</p>
 * 
 * <pre>SOAP.response($, "ticket", "00c7932fa021c9acc530");</pre> 
 *     
 * <p>Will send an SOAP response with the simplest envelope and body:</p>
 * 
 * <pre>...
 *&lt;SOAP-ENV:Body &gt;
 *    &lt;ticket xsi:type="xsd:string" 
 *        &gt;&lt;[CDATA[00c7932fa021c9acc530]]&gt;&lt;/ticket&gt;
 *&lt;/SOAP-ENV:Body&gt;
 *...</pre> 
 * 
 * <p>Just enough basic XSD types are supported but I avoided the
 * brain dead containers' encoding required by static typing since the 
 * response objects are expected to have a dynamic JSON model.</p>
 * 
 * <p>As a result, neither relations (aka records) nor dictionaries
 * (aka indexes) are supported by this implementations.</p>
 * 
 * <pre>SOAP.response ($, "hello-test", JSON.decode(
 *     "{\"hello\": \"World!\", \"test\": [1, 2.20, null, true]}"
 *     ));</pre> 
 * 
 * <p>Would produce the following body:</p>
 * 
 * <pre>...
 *&lt;hello-test &gt;
 *    &lt;hello xsi:type="xsd:string" &gt;&lt;[CDATA[World!]]&gt;&lt;/hello&gt;
 *    &lt;test xsi:type="xsd:int" &gt;&lt;1&lt;/test&gt;
 *    &lt;test xsi:type="xsd:decimal" &gt;&lt;2.20&lt;/test&gt;
 *    &lt;test/&gt;
 *    &lt;test xsi:type="xsd:boolean" &gt;&lt;true&lt;/test&gt;
 *&lt;hello-test&gt;
 *...</pre> 
 *
 * <p>which cannot be supported by an XSD schema. Instead, namepsaces
 * and arrays should be used to describe records and indexes, leaving
 * the interpretation as more complex object to the schema's applications.</p>
 * 
 * <h4>Fault</h4>
 * 
 * <p>...</p>
 * 
 * <h4>Security</h4>
 * 
 * <p>IRTD2 cookies may be used for identification, authorization
 * and audit of the SOAP transactions by less4j, not one of the proprietary 
 * implementation of WS-Security.</p> 
 * 
 * <p>By default no identification is required. If you <em>really</em> need 
 * security features in the usual environments where WS-* has been adopted, 
 * then there's a great deal of chance that you'd rather have X509 client 
 * certificates identifying user agents using SSL/TLS encrypted connections.
 * </p>
 * 
 * <p>And that is better handled out of the J2EE container, by stunnel for
 * clients and Apache's mod_ssl in a forward proxy for servers.</p>
 * 
 * <h3>When Money Talks</h3>
 * 
 * <p>The painfull truth is that SOAP and WSDL suck. I wrote my first WS-*
 * server and client seven years ago and that "industry standard" still sucks 
 * as much as it did then. The WSDL type system is fucked-up and 
 * interoperability is problematic at best. Then, if you use something like 
 * Apache's Axis, you'll need a dozen Java and XML configuration files plus 
 * megabytes of JAR to support what is after all just yet another statically 
 * typed RPC.<p>
 * 
 * <p>Lasts but not leasts, a statically typed object notation is a recipe 
 * for distributed system failure or software fossilization, very much as
 * inflexible and unsafe on the long run as CORBA, DCOM, RMI, IIOP, etc.</p>
 * 
 * <p>WS-* only walk when money talks ...</p>
 * 
 * <p>SOAP is broken, slow and verbose compared to JSON.</p>
 * 
 * <p>WSDL is broken and unreadable compared to Regular JSON expressions.</p>
 * 
 * <p>But I like money too.</p>
 * 
 * @author Laurent Szyster
 */
public class SOAP implements Function {
    
    private static final String _name = "name";
    private static final String _element = "element";
    private static final String _Request = "Request";
    private static final String _Response = "Response";
    
    // org.less4j.Function implementation
    public static SOAP singleton = new SOAP();
    public String jsonInterface(Actor $) {
        return JSON._null; // no regular JSON interfaces
    }
    private JSONR.TypeNamespace jsonr = null;
    private XML.Element xsdRequest = null;
    private XML.Element xsdResponse = null;
    /**
     * Try to compiles the Regular JSON interface(s) to a 
     * <code>JSONR.Type</code> and then Request and Response XSD schemas, 
     * return <code>true</code> on success or log and error and return 
     * <code>false</code> on failure.   
     */
    public boolean less4jConfigure(Actor $) {
        try {
            jsonr = (JSONR.TypeNamespace) JSONR.compile(jsonInterface($));
            String name = $.about.substring(1); 
            xsdRequest = xsd(
                (JSONR.Type) jsonr.namespace.get(_Request), name + _Request
                );
            xsdResponse = xsd (
                (JSONR.Type) jsonr.namespace.get(_Response), name + _Response
                );
        } catch (Exception e) {
            $.logError(e);
            return false;
        } 
        return true; 
    }
    /**
     * Allways return <code>true</code>, to override by functions that want
     * to leverage IRTD2 cookies for auditable HTTP user agents 
     * identification.
     * 
     * <h3>Synopsis</h3>
     * 
     * <pre>public boolean irtd2Identify(Actor $) {
     *    $.httpError(401); // Not authorized
     *    return false;
     *}</pre>
     * 
     */
    public boolean irtd2Identify(Actor $) {
        return true; // no IRTD2 identification required
    }
    /**
     * Handle any text/xml content type posted to this function as a SOAP
     * request, try map the XML element tree to a practical JSON object as 
     * the input stream is parsed then execute the <code>call</code> to send
     * a response, or upon failure log an error and reply with an the
     * exception's message in a SOAP <code>fault</code>.
     */
    public void httpContinue (
        Actor $, String method, String contentType
        ) {
        if (
            method.equals(Controller._POST) && contentType != null &&
            contentType.startsWith(XML.MIME_TYPE)
            ) try {
            Document soap = new Document();
            XML.read($.request.getInputStream(), "", null, null, soap);
            $.json = ((Element) soap.root).json;
            this.call ($, soap);
        } catch (Throwable e) {
            $.logError(e);
            fault($, e.getMessage());
        } else
            $.httpError(400);
    }
    /**
     * Reply to all idempotent GET request with a complete web service
     * description.
     */
    public void httpResource(Actor $) {
        try {
            String name = $.about.substring(1); 
            $.httpResponse(200, XML.encodeUTF8(wsdl(
                $.context, name, xsdRequest, xsdResponse
                )), XML.MIME_TYPE, XML._utf8);
        } catch (Throwable e) {
            $.logError(e);
            $.httpError(500);
        }
    }
    public Object jsonRegular(Actor $) {
        return null;
    }
    public void jsonApplication(Actor $) {
        $.httpError(501); // Not implemented
    }
    public void call (Actor $, Document message) throws Throwable {
        String action = $.about.substring(1);
        JSON.Object body = $.json.O("Body");
        JSON.Object request = body.O(action+_Request);
        body.O(action+_Response).put(
            _Response, "Hello " + request.S("arg0") + " !"
            );
        response($, action, body.O(action+_Response)); 
    }
    
    // Supporting SOAP implementation
    
    protected static class Element extends XML.Element {
        public static XML.Type TYPE = new Element(null, null);
        public JSON.Object json = null;
        public Element (String name, HashMap attributes) {
            super(name, attributes);
        }
        public XML.Element newElement (String name, HashMap attributes) {
            return new Element(name, attributes);
        }
        public void jsonUpdate(Element container, Object contained) {
            String tag = this.getLocalName();
            JSON.Object map = container.json; 
            if (map == null) {
                map = new JSON.Object();
                container.json = map;
                map.put(tag, contained);
            } else if (map.containsKey(tag)) {
                Object o = map.get(tag);
                if (o instanceof JSON.Array) {
                    ((JSON.Array) o).add(contained);
                } else {
                    JSON.Array list = new JSON.Array();
                    (list).add(contained);
                    map.put(tag, list);
                }
            } else
                map.put(tag, contained);
        }
        private static final String 
            _xsi_type = "http://www.w3.org/2001/XMLSchema-instance type";
        private static final Map _xsd_types = Simple.dict(
            new HashMap(), new Object[]{
                "xsd:byte", JSONR.INTEGER,
                "xsd:short", JSONR.INTEGER,
                "xsd:int", JSONR.INTEGER,
                "xsd:integer", JSONR.DECIMAL,
                "xsd:long", JSONR.INTEGER,
                "xsd:float", JSONR.DOUBLE,
                "xsd:double", JSONR.DOUBLE,
                "xsd:decimal", JSONR.DECIMAL,
                "xsd:boolean", JSONR.BOOLEAN
                // TODO: ? base64Binary, dateTime, hexBinary, QName ?
            });
        public void valid(XML.Document document) throws XML.Error {
            XML.Element parent = this.parent;
            while (parent != null && !(parent instanceof Element))
                parent = this.parent;
            if (parent == null) // root 
                ;
            else if (children == null) { // leaf
                if (first != null) {
                    if (
                        attributes != null && 
                        attributes.containsKey(_xsi_type)
                        ) try {
                        JSONR.Type type = (JSONR.Type) _xsd_types.get(
                            (String) attributes.get(_xsi_type)
                            );
                        if (type != null)
                            jsonUpdate((Element) parent, type.eval(first));
                        else
                            jsonUpdate((Element) parent, first);
                    } catch (JSON.Error e) {
                        throw new XML.Error (e.getMessage());
                    } else
                        jsonUpdate((Element) parent, first);
                } else if (attributes != null)
                    jsonUpdate((Element) parent, attributes);
            } else if (json != null) // branch
                jsonUpdate((Element) parent, json);
        }
    } 
    protected static class Document extends XML.Document {
        public XML.Element newElement(String name, HashMap attributes) {
            return new Element(name, attributes);
        }
    }
    
    protected static final String 
        XSD_NS = "http://www.w3.org/2001/XMLSchema";
    protected static final String XSD_PREFIX = "xsd";
    
    protected static final String xsd_schema = XSD_NS + " schema";
    protected static final String xsd_element = XSD_NS + " element";
    protected static final String xsd_complexType = XSD_NS + " complexType";
    protected static final String xsd_sequence = XSD_NS + " sequence";
    protected static final String xsd_all = XSD_NS + " all";
    protected static final String xsd_any = XSD_NS + " any";
    
    /**
     * Try to translate a regular JSONR expression in a named XSD schema,
     * throws an <code>Exception</code> for models that contain records
     * or dictionaries (both are not supported by XSD in a way that 
     * could fit less4j's simpler understanding of SOAP messages).
     * 
     * <h3>Synopsis</h3>
     * 
     * <pre>JSONR.Type model = JSONR.compile(JSON.object(new Object[]{
     *    "item", "",
     *    "quantity", new Integer(1),
     *    "description", ""
     *    "notification", Boolean.TRUE
     *    }));
     *XML.element type = xsd(model, "PurchaseOrder");</pre>
     * 
     * @param model to translate from JSONR to XSD
     * @param name the name of the XSD type mapped
     * @return an <code>XML.Element</code>
     * @throws Exception
     */
    public static final XML.Element xsd (JSONR.Type model, String name) 
    throws Exception {
        if (model instanceof JSONR.TypeBoolean) {
            return new XML.Element(xsd_element, new String[]{
                "name", name, "type", "boolean", "minOccurs", "1"
                }, null, null); // string 
        } else if (model instanceof JSONR.TypeString) {
            return new XML.Element(xsd_element, new String[]{
                "name", name, "type", "string", "minOccurs", "0"
                }, null, null); 
        } else if (model instanceof JSONR.TypeRegular) {
            return new XML.Element(xsd_element, new String[]{
                "name", name, "type", "string", "minOccurs", "1"
                }, null, null); 
        } else if (
            model instanceof JSONR.TypeDecimal ||
            model instanceof JSONR.TypeDecimalAbsolute ||
            model instanceof JSONR.TypeDecimalRelative
            ) {
            return new XML.Element(xsd_element, new String[]{
                "name", name, "type", "decimal", "minOccurs", "1"
                }, null, null);
        } else if (
            model instanceof JSONR.TypeInteger ||
            model instanceof JSONR.TypeIntegerAbsolute ||
            model instanceof JSONR.TypeIntegerRelative
            ) {
            return new XML.Element(xsd_element, new String[]{
                "name", name, "type", "int", "minOccurs", "1"
                }, null, null); 
        } else if (
            model instanceof JSONR.TypeDouble ||
            model instanceof JSONR.TypeDoubleAbsolute ||
            model instanceof JSONR.TypeDoubleRelative
            ) {
            return new XML.Element(xsd_element, new String[]{
                "name", name, "type", "double", "minOccurs", "1"
                }, null, null);
        } else if (model instanceof JSONR.TypeDictionary) {
            throw new Exception(
                "JSONR dictionaries are not supported by XSD"
                ); 
        } else if (model instanceof JSONR.TypeNamespace) {
            JSONR.TypeNamespace type = (JSONR.TypeNamespace) model;
            XML.Element element = new XML.Element(xsd_element, new String[]{
                "name", name, "minOccurs", "0"
                }, null, null);
            XML.Element all = element
                .addChild(xsd_complexType).addChild(xsd_all);
            Iterator names = type.names.iterator();
            String property;
            while (names.hasNext()) {
                property = (String) names.next();
                all.addChild(xsd(
                    (JSONR.Type) type.namespace.get(property), property
                    ));
            }
            return element;
        } else if (model instanceof JSONR.TypeArray) {
            JSONR.TypeArray type = (JSONR.TypeArray) model;
            if (type.types.length == 1) {
                XML.Element element = xsd(
                    (JSONR.Type) type.types[0], name
                    );
                element.attributes.put("minOccurs", "0");
                element.attributes.put("maxOccurs", "unbounded");
                return element;
            } else
                throw new Exception(
                    "JSONR relations are not supported by XSD"
                    ); 
        } else if (model instanceof JSONR.TypeUndefined) {
            XML.Element element = new XML.Element(xsd_element, new String[]{
                "name", name, "minOccurs", "0"
                }, null, null);
            element.addChild(xsd_complexType).addChild(xsd_any);
            return element;
        }
        return null;
    };

    protected static final String 
        SOAP_NS = "http://schemas.xmlsoap.org/wsdl/soap/";    
    protected static final String SOAP_PREFIX = "soap";    
    protected static final String soap_binding = SOAP_NS + " binding";
    protected static final String soap_operation = SOAP_NS + " operation";
    protected static final String soap_body = SOAP_NS + " body";
    protected static final String soap_address = SOAP_NS + " address";
    protected static final String 
        soap_http = "http://schemas.xmlsoap.org/soap/http";
    
    protected static final String 
        WSDL_NS = "http://schemas.xmlsoap.org/wsdl/";
    protected static final String WSDL_PREFIX = "wsdl";
    
    protected static final String wsdl_types = WSDL_NS + " types";
    protected static final String wsdl_definitions = WSDL_NS + " definitions";
    protected static final String wsdl_message = WSDL_NS + " message";
    protected static final String wsdl_part = WSDL_NS + " part";
    protected static final String wsdl_portType = WSDL_NS + " portType";
    protected static final String wsdl_operation = WSDL_NS + " operation";
    protected static final String wsdl_input = WSDL_NS + " input";
    protected static final String wsdl_output = WSDL_NS + " output";
    protected static final String wsdl_fault = WSDL_NS + " fault";
    protected static final String wsdl_binding = WSDL_NS + " binding";
    protected static final String wsdl_service = WSDL_NS + " service";
    protected static final String wsdl_port = WSDL_NS + " port";
    
    private static final String _targetNamespace = "targetNamespace";
    private static final String _body = "body";
    private static final String _tns = "tns:";
    private static final String _message = "message";
    private static final String _RequestMsg = "RequestMsg";
    private static final String _ResponseMsg = "ResponseMsg";
    private static final String _Port = "Port";
    private static final String _Binding = "Binding"; 
    private static final String _type = "type";
    private static final String _style = "style"; 
    private static final String _rpc = "rpc";
    private static final String _transport = "transport";
    private static final String 
        _soap_http ="http://schemas.xmlsoap.org/soap/http";
    private static final String _soapAction = "soapAction";
    private static final String _encodingStyle = "encodingStyle"; 
    private static final String 
        _soap_encoding = "http://schemas.xmlsoap.org/soap/encoding/";
    private static final String _use = "use";
    private static final String _encoded = "encoded"; 
    private static final String _binding = "binding";
    private static final String _location = "location";
    
    /**
     * Produces a WSDL description of this SOAP function from a regular 
     * JSON interface of the form:
     * 
     * <pre>{"Request": null, "Response": null}</pre>
     * 
     * <h3>Synopsis</h3>
     * 
     * <p>To declare and enforce a different model than the default presented
     * above, override the <code>jsonInterface</code> method to return the
     * regular JSON string describing this function's input and output.</p>
     * 
     * @param context
     * @param name
     * @param input
     * @param output
     * @return
     */
    public static final XML.Document wsdl (
        String context, String name, XML.Element input, XML.Element output 
        ) {
        XML.Document doc = new XML.Document();
        doc.ns.put(XSD_NS, XSD_PREFIX);
        doc.ns.put(SOAP_NS, SOAP_PREFIX);
        doc.ns.put(WSDL_NS, WSDL_PREFIX);
        doc.root = new XML.Element(wsdl_definitions, new String[]{
            _targetNamespace, context
            }, null, null);
        XML.Element schema = doc.root.addChild(wsdl_types)
            .addChild(xsd_schema);
        schema.addChild(input);
        schema.addChild(output);
        XML.Element input_message = doc.root.addChild(
            wsdl_message, new String[]{_name, name + _RequestMsg}
            );
        input_message.addChild(wsdl_part, new String[]{
            _name, _body, _element, _tns + name + _Request
            });
        XML.Element output_message = doc.root.addChild(
            wsdl_message, new String[]{_name, name + _ResponseMsg}
            );
        output_message.addChild(wsdl_part, new String[]{
            _name, _body, _element, _tns + name + _Response
            });
        XML.Element operation = doc.root
            .addChild(wsdl_portType, new String[]{
                _name, name + _Port
                })
            .addChild(wsdl_operation);
        operation.addChild(wsdl_input, new String[]{
            _message, _tns + name + _RequestMsg
            });
        operation.addChild(wsdl_output, new String[]{
            _message, _tns + name + _ResponseMsg
            });
        XML.Element binding = doc.root.addChild(wsdl_binding, new String[]{
            _name, name + _Binding, _type, _tns + name + _Port
            });
        binding.addChild(soap_binding, new String[]{
            _style, _rpc, _transport, _soap_http    
            });
        operation = binding.addChild(wsdl_operation, new String[]{
            _name, name     
            });
        operation.addChild(soap_operation, new String[]{
            _soapAction, name
            });
        operation.addChild(wsdl_input).addChild(soap_body, new String[]{
            _encodingStyle, _soap_encoding, _use, _encoded
            });
        operation.addChild(wsdl_output).addChild(soap_body, new String[]{
            _encodingStyle, _soap_encoding, _use, _encoded
            });
        doc.root.addChild(wsdl_service)
            .addChild(wsdl_port, new String[]{
                _binding, _tns + name + _Binding,
                _name, name + _Port
                })
            .addChild(soap_address, new String[]{
                _location, context + '/' + name    
                });
        return doc;
    }
    
    /**
     * Encode an object in a really simple XML notation that is
     * backward-compatible with SOAP 1.1 but supporting "only" the 
     * JSON types: Map, List, String, Number, Boolean and null.
     * 
     * <h3>Synopsis</h3>
     * 
     * <pre>...</pre>
     * 
     */
    protected static final StringBuffer strb (
        StringBuffer sb, Object value, String name
        ) {
        if (value == null) {
            sb.append('<');
            sb.append(name); // place holder for array size
            sb.append("/>");
        } else if (value instanceof String) {
            sb.append('<');
            sb.append(name);
            sb.append(" xsi:type=\"xsd:string\"><![CDATA[");
            sb.append(value);
            sb.append("]]></");
            sb.append(name);
            sb.append('>');
        } else if (value instanceof Number) {
            if (value instanceof Integer) {
                sb.append('<');
                sb.append(name);
                sb.append(" xsi:type=\"xsd:int\">");
                sb.append(value);
                sb.append("</");
                sb.append(name);
                sb.append('>');
            } else if (value instanceof Double) {
                sb.append('<');
                sb.append(name);
                sb.append(" xsi:type=\"xsd:double\">");
                sb.append(value);
                sb.append("</");
                sb.append(name);
                sb.append('>');
            } else {
                sb.append('<');
                sb.append(name);
                sb.append(" xsi:type=\"xsd:decimal\">");
                sb.append(value);
                sb.append("</");
                sb.append(name);
                sb.append('>');
            }
        } else if (value instanceof Boolean) {
            sb.append('<');
            sb.append(name);
            sb.append(" xsi:type=\"xsd:boolean\">");
            sb.append(value);
            sb.append("</");
            sb.append(name);
            sb.append('>');
        } else if (value instanceof List) {
            Iterator it = ((List) value).iterator();
            while (it.hasNext())
                strb(sb, it.next(), name);
        } else if (value instanceof Iterator) {
            Iterator it = ((Iterator) value);
            while (it.hasNext())
                strb(sb, it.next(), name);
        } else if (value instanceof Map) {
            Iterator it = ((Map) value).keySet().iterator();
            if (it.hasNext()) {
                sb.append('<');
                sb.append(name);
                sb.append('>');
                Object key; 
                Map map = (Map) value;
                do {
                    key = it.next();
                    strb(sb, map.get(key), (String) key);
                } while (it.hasNext());
                sb.append("</");
                sb.append(name);
                sb.append('>');
            }
        }
        return sb;
    }
    
    protected static final String _utf8 = "UTF-8";
    
    public static final byte[] encode (Object value, String name) { 
        return Simple.encode(strb(
            new StringBuffer(), value, name
            ).toString(), _utf8); 
    }
    
    protected static final byte[] _response_head = Simple.encode(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<SOAP-ENV:Envelope "
            + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " 
            + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            + "xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" " 
            + ">"
            + "<SOAP-ENV:Body>", 
        _utf8
        );
    protected static final byte[] _response_tail = Simple.encode(
            "</SOAP-ENV:Body>" 
        + "</SOAP-ENV:Envelope>", 
        _utf8
        );
    
    /**
     * Encode <code>$.json</code> as a SOAP response without header and send 
     * it in the simplest envelope. 
     * 
     * <h4>Synopsis</h3>
     * 
     * <pre>...</pre>
     * 
     * @param response the response string
     * @return
     */
    public static final void response (Actor $, String name, Object body) {
        $.httpResponse(200, Simple.buffer(new byte[][]{
            _response_head, encode(body, name), _response_tail
            }), XML.MIME_TYPE, _utf8);
    } // isn't this one liner elegant?
    
    protected static final byte[] _response_envelope = Simple.encode(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<SOAP-ENV:Envelope "
            + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " 
            + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            + "xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" " 
            + ">",
        _utf8
        );
    protected static final byte[] _response_body = Simple.encode(
        "<SOAP-ENV:Body>", _utf8
        );
    
    /**
     * Encode <code>$.json</code> as a SOAP response and an arbitrary 
     * <code>JSON.Object</code> as header, then send it in the simplest 
     * envelope.
     * 
     * <h4>Synopsis</h3>
     * 
     * <pre>...</pre>
     * 
     * @param response the response string
     * @return
     */
    public static final void response (
        Actor $, String name, Object body, JSON.Object header
        ) {
        $.httpResponse(200, Simple.buffer(new byte[][]{
            _response_envelope,
            encode (header, "SOAP-ENV:Header"), 
            _response_body,
            encode(body, name), 
            _response_tail
            }), XML.MIME_TYPE, _utf8);
    }
    
    protected static final byte[] _fault_head = Simple.encode(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<SOAP-ENV:Envelope "
            + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " 
            + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            + "xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" " 
            + ">" 
            + "<SOAP-ENV:Body>"
                + "<SOAP-ENV:Fault>" 
                    + "<faultstring xsi:type=\"xsd:string\"><![CDATA[",
        _utf8
        );
    protected static final byte[] _fault_tail = Simple.encode(
                    "]]></faultstring>" 
                + "</SOAP-ENV:Fault>" 
            + "</SOAP-ENV:Body>"
        + "</SOAP-ENV:Envelope>", 
        _utf8
        );
    /**
     * Encode a SOAP fault as fast and safe as possible: made of a 
     * single <code>String</code> encoded in UTF-8 sandwiched in a 
     * template head and tail. 
     * 
     * <h4>Synopsis</h3>
     * 
     * <pre>...</pre>
     * 
     * @param $
     * @param text
     */
    public static final void fault (Actor $, String message) {
        $.httpResponse(500, Simple.buffer(new byte[][]{
            _fault_head, Simple.encode(message, _utf8), _fault_tail
            }), XML.MIME_TYPE, _utf8);
    }

    protected static final byte[] _fault_body = Simple.encode(
            "<SOAP-ENV:Fault>" 
                + "<faultstring xsi:type=\"xsd:string\"><![CDATA[",
        _utf8
        );
    
    /**
     * Encode a SOAP fault with a string as body and <code>JSON.Object</code> 
     * as header, then send it in the simplest envelope.
     * 
     * <h4>Synopsis</h3>
     * 
     * <pre>...</pre>
     * 
     * @param response the response string
     * @return
     */
    public static final void fault (
        Actor $, String message, JSON.Object header
        ) {
        $.httpResponse(500, Simple.buffer(new byte[][]{
            _response_envelope,
            SOAP.encode(header, "SOAP-ENV:Header"), 
            _fault_body, 
            Simple.encode(message, _utf8), 
            _fault_tail
            }), XML.MIME_TYPE, _utf8);
    }

}

//
// Rule N°1: don't bother with broken "industry standard" invented by 
// Microsoft, adopted by Sun and supported by IBM.
//
// Rule N°2: interpret them loosely and secure a migration path to defacto
// free standard.
//
// Rule N°3: don't brag about it, just do it.
