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

package org.less4j.functions;

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

import org.less4j.protocols.JSON;
import org.less4j.protocols.JSONR;
import org.less4j.protocols.XML;
import org.less4j.serlvet.Actor;
import org.less4j.serlvet.Service;
import org.less4j.simple.Bytes;
import org.less4j.simple.Objects;

/**
 * A function that supports a practical subset of SOAP 1.1, just enough
 * to provide a <em>really</em> simple XML object notation that is forward
 * compatible with JSON web services.
 * 
 * @div <h3>Synopsis</h3>
 * 
 * <p>Here is a web service to test the usual "Hello World" suspect SOAP 
 * remote procedure call:</p>
 * 
 * <pre>import org.less4j.*;
 * 
 *public class echoString extends SOAP {
 *
 *    public void call (Actor $, SOAP.Document request) 
 *    throws Throwable {
 *        SOAP.response($, "Hello " + $.json.S("arg0") + " !");
 *    }
 *    
 *}</pre>
 *
 * <p>The SOAP request's parameters are available to its application as
 * a practical JSON object and its response may be any value of JSON types.
 * Note that a schema or web service description is not required to cast the 
 * request body to a JSON typed object. So this function can be applied to 
 * support legacy interfaces in a quick-and-dirty-but-damn-effective way.</p>
 * 
 * <p>Also note that the original XML document from which the JSON object 
 * was mapped is also provided to the remote procedure called, allowing 
 * access to the SOAP header element.</p>
 *
 * <h3>SOAP/RPC</h3>
 *
 * <p>Configured to handle POST request at "/echoString" in its servlet 
 * context, this function will produce the following dialog of SOAP request 
 * and response bodies:</p> 
 *
 * <pre>POST /context/echoString HTTP/1.0
 *Host: localhost:8080
 *Content-type: text/xml; charset=utf-8
 *SOAPAction: echoString
 *...
 *&lt;SOAP-ENV:Body &gt;
 *    &lt;echoStringRequest&gt;
 *        &lt;arg0 xsi:type="xsd:string"&gt;World&lt;/arg0&gt;
 *    &lt;/echoStringRequest&gt;
 *&lt;/SOAP-ENV:Body&gt;
 *...</pre> 
 *
 *HTTP/1.0 200 Ok
 *Content-type: text/xml; charset=utf-8
 *...
 *&lt;SOAP-ENV:Body &gt;
 *    &lt;echoStringResponse&gt; 
 *        &lt;return xsi:type="xsd:string" 
 *            &gt;&lt;[CDATA[00c7932fa021c9acc530]]&gt;&lt;/return&gt;
 *    &lt;/echoStringResponse&gt;
 *&lt;/SOAP-ENV:Body&gt;
 *...</pre> 
 * 
 * @div <h3>WSDL for JSONR</h3>
 * 
 * <p>This <code>Service</code> implementation supports a subset of WSDL
 * mapped from a regular JSON expressions. It is restricted to RPC, SOAP 
 * encoding and the XSD equivalents of JSON types. But it supports complex 
 * types as practically as interroperable XML schemas allow.</p>
 * 
 * <pre>{
 *    "Request": {
 *        "item": "[0-9A-Z]{12}",
 *        "quantity": 20,
 *        "description": ".+",
 *        "price": 100
 *        },
 *    "Response": "accepted|rejected|quoted"
 *    }</pre>
 * 
 * <p>...</p>
 * 
 * <pre>import org.less4j.*;
 * 
 *public class PurchaseOrder extends SOAP {
 *
 *    public String jsonInterface (Actor $) {
 *        return JSON.encode(JSON.object(new Object[]{
 *            "Request", JSON.object(new Object[]{
 *                "item", "[0-9A-Z]{12}", 
 *                "quantity", new Integer(20),
 *                "description", ".+"
 *                "price", new Integer(100),
 *                }),
 *            "Response", "accepted|rejected|quoted"
 *            }));
 *    }
 *
 *    public void call (Actor $, Document request) throws Throwable {
 *        response($, purchaseOrder (
 *           $.json.S("item"), 
 *           $.json.I("quantity").intValue(), 
 *           $.json.S("description")
 *           ));
 *    }
 *    
 *    public static Object purchaseOrder (
 *        String item, int quantity, String description
 *        ) {
 *        return "rejected"; // ... here goes your business logic
 *    }
 *    
 *}</pre>
 * 
 * <h4>Request</h4>
 * 
 * <p>...</p>
 * 
 * <h4>Response</h4>
 * 
 * <p>...</p>
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
 * <h3>Where Less Is More</h3>
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
 * easy to read and undertsand). Third it ensures that its applications are
 * forward compatible with the defacto network object notation.</p> 
 * 
 * <p>But then why bother with XML and WSDL at all?</p>
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
 * <p>SOAP is broken, slow and verbose compared to JSON.</p>
 * 
 * <p>WSDL is broken and unreadable compared to Regular JSON expressions.</p>
 * 
 * <p>WS-* only walk when money talks ...</p>
 * 
 * <p>And I like money too.</p>
 */
public class SOAP implements Service {
    
    private static final String _name = "name";
    private static final String _Request = "Request";
    private static final String _Response = "Response";
    
    public static SOAP singleton = new SOAP();
    
    /**
     * 
     */
    public String jsonInterface(Actor $) {
        return "{\"Request\": \"\", \"Response\": \"\"}";
    }
    
    private JSON.Object jsonr = null;
    
    /**
     * Try to compiles the Regular JSON interface(s) to a 
     * <code>JSONR.Type</code> and then Request and Response XSD schemas, 
     * return <code>true</code> on success or log and error and return 
     * <code>false</code> on failure.
     * 
     * <p>Applications that need to override this method must call it
     * first and return false if it fails.</p>
     */
    public boolean less4jConfigure(Actor $) {
        try { 
            jsonr = (JSON.Object) JSONR.compile(jsonInterface($)).json();
            String action = $.about.substring(1);
            XML.Element schema = new XML.Element(xsd_schema, null);
            xsd(
                schema, 
                (JSONR.Type) jsonr.get(_Request), 
                action + _Request
                );
            xsd(
                schema,
                (JSONR.Type) jsonr.get(_Response), 
                action + _Response
                );
        } catch (Throwable e) { // Anything could go wrong here ...
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
    
    private static final String _Body = "Body";
    
    /**
     * Handle any text/xml content type posted to this function as a SOAP
     * request, try map the XML element tree to a practical JSON object as 
     * the input stream is parsed then execute the <code>call</code> to send
     * a response, or upon failure log an error and reply with an the
     * exception's message in a SOAP <code>fault</code>.
     * 
     * <p>Note that if the Actor's <code>test</code> is configured to 
     * <code>true</code> then a copy of the SOAP request is logged as
     * <code>SOAP</code> information (to STDERR).</p>
     * 
     * <p>Applications are not expected to override this method.</p>
     * 
     */
    public void httpContinue (
        Actor $, String method, String contentType
        ) {
        if (
            method.equals("POST") && contentType != null &&
            contentType.startsWith(XML.MIME_TYPE)
            ) try {
            String action = $.about.substring(1) + _Request;
            Document soap = new Document();
            soap.ns = new HashMap(_NS);
            soap.read($.request.getInputStream(), "", null, null);
            $.json = ((Element) soap.root).json.getObject(_Body).getObject(action);  
            if ($.test) {
                $.logInfo(soap.toString(), "SOAP");
                $.logInfo(JSON.encode($.json), "INPUT");
            }
            JSONR.Type type = (JSONR.Type) jsonr.get(_Request);
            if (type.name().equals("namespace"))
                JSONR.validate($.json, type);
            else
                JSONR.validate($.json.get(
                    $.json.keySet().iterator().next()
                    ), type);
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
            $.httpResponse(200, wsdl(
                $.url, name, jsonr
                ).encodeUTF8(), XML.MIME_TYPE, "UTF-8");
        } catch (Throwable e) {
            $.logError(e);
            $.httpError(500);
        }
    }
    
    /**
     * Returns a <code>JSONR</code> parser validating the pattern named
     * <code>"Response"</code> in the <code>jsonInterface</code> declared.
     * 
     * <p>Applications are not expected to override this method.</p>
     * 
     */
    public Object jsonRegular(Actor $) {
        return new JSONR((JSONR.Type) jsonr.get(_Request));
    }
    
    /**
     * Reply to JSON request with a <code>501 Not implemented</code> error,
     * to override by applications that need to migrate from SOAP to JSON
     * smoothly.
     */
    public void jsonApplication(Actor $) {
        $.httpError(501); // Not implemented
    }
    
    /**
     * The remote procedure call implementation, to override by applications.
     * 
     * <h3>Synopsis</h3>
     * 
     * <pre>public void call (Actor $, Document message) 
     *throws Throwable {
     *    response($, "Hello " + $.json.S("arg0") + "!");
     *}</pre>
     *
     * @param $
     * @param message
     * @throws Throwable
     */
    public void call (Actor $, Document message) throws Throwable {
        response($, "Hello " + $.json.getString("arg0") + "!");
    }
    
    // Supporting SOAP implementation, legacy at its best ...
    
    protected static final String 
    _xsi_type = "http://www.w3.org/2001/XMLSchema-instance type";
    protected static final Map _xsd_types = Objects.update(
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
        public void valid(XML.Document document) throws XML.Error {
            XML.Element parent = this.parent;
            while (parent != null && !(parent instanceof Element))
                parent = this.parent;
            if (parent == null) // root 
                ;
            else if (children == null) { // leaf
                if (first != null) { // simple types
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
                } else if (attributes != null) // complex type of attributes
                    jsonUpdate((Element) parent, attributes);
            } else if (json != null) { // branch, complex type of elements
                if (_xsi_type.equals(_soapenc_Array)) { // Array
                    Iterator names = json.keySet().iterator();
                    // move up and rename the first and only array expected
                    if (names.hasNext())
                        jsonUpdate((Element) parent, json.getArray(
                            (String) names.next(), null
                            ));
                } else // Object
                    jsonUpdate((Element) parent, json);
            }
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
    protected static final String xsd_complexContent = XSD_NS + " complexContent";
    protected static final String xsd_sequence = XSD_NS + " sequence";
    protected static final String xsd_all = XSD_NS + " all";
    protected static final String xsd_any = XSD_NS + " any";
    protected static final String xsd_minOccurs = XSD_NS + " minOccurs";
    protected static final String xsd_maxOccurs = XSD_NS + " maxOccurs";
    protected static final String xsd_restriction = XSD_NS + " restriction";
    protected static final String xsd_attribute = XSD_NS + " attribute";
    
    private static final String _soapenc_Array = "SOAP-ENC:Array";
    private static final String _soapenc_ArrayType = "SOAP-ENC:ArrayType";
    private static final String _Array = "Array";
    
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
     * @param schema to update with complexTypes
     * @param model to translate from JSONR to XSD
     * @param name the name of the XSD type mapped
     * @return an <code>XML.Element</code>
     * @throws Exception
     */
    public static final XML.Element xsd (
        XML.Element schema, JSONR.Type model, String name
        ) 
    throws Exception {
        String type = model.name();
        if (type.equals("boolean")) {
            return new XML.Element(xsd_element, new String[]{
                _name, name, _type, "xsd:boolean"
                }, null, null); // string 
        } else if (type.equals("string")) {
            return new XML.Element(xsd_element, new String[]{
                _name, name, _type, "xsd:string"
                }, null, null); 
        } else if (type.equals("pcre")) {
            return new XML.Element(xsd_element, new String[]{
                _name, name, _type, "xsd:string"
                }, null, null); 
        } else if (type.startsWith("decimal")) {
            return new XML.Element(xsd_element, new String[]{
                _name, name, _type, "xsd:decimal"
                }, null, null);
        } else if (type.startsWith("integer")) {
            return new XML.Element(xsd_element, new String[]{
                _name, name, _type, "xsd:int"
                }, null, null); 
        } else if (type.startsWith("double")) {
            return new XML.Element(xsd_element, new String[]{
                _name, name, _type, "xsd:double"
                }, null, null);
        } else if (type.equals("dictionary")) {
            throw new Exception(
                "JSONR dictionaries are not supported by XSD"
                ); 
        } else if (type.equals("namespace")) {
            JSON.Object ns = (JSON.Object) model.json();
            XML.Element namespace = schema.addChild(
                xsd_complexType, new String[]{_name, name});
            XML.Element all = namespace.addChild(xsd_all);
            Object[] names = ns.keySet().toArray(); 
            Arrays.sort(names);
            String property;
            for (int i=0; i<names.length; i++) {
                property = (String) names[i];
                all.addChild(xsd(
                    schema, (JSONR.Type) ns.get(property), property
                    ));
            }
            return new XML.Element(
                xsd_element, new String[]{
                    _name, name, _type, "tns:" + name
                    }, null, null
                );
        } else if (type.equals("array")) {
            JSON.Array types = (JSONR.Array) model.json();
            if (types.size() == 1) {
                xsd(schema, (JSONR.Type) types.get(0), name);
                XML.Element array = schema.addChild(
                    xsd_complexType, new String[]{_name, name + _Array}
                    );
                array.addChild(xsd_complexContent)
                    .addChild(xsd_restriction, new String[]{
                        "base", _soapenc_Array
                        })
                        .addChild(xsd_attribute, new String[]{
                            "ref", _soapenc_ArrayType,
                            wsdl_arrayType, "tns:" + name + "[]"
                            });
                return new XML.Element(
                    xsd_element, new String[]{
                        _name, name, _type, "tns:" + name + _Array
                        }, null, null
                    );
            } else
                throw new Exception(
                    "JSONR relations are not supported for XSD"
                    ); 
        } else if (type.equals("undefined")) {
            throw new Exception(
                "JSONR null not the intent of WSDL"
                );
        }
        return null;
    };

    protected static final String 
        SOAP_NS = "http://schemas.xmlsoap.org/wsdl/soap/";    
    protected static final String 
        SOAP_http = "http://schemas.xmlsoap.org/soap/http";
    private static final String 
        SOAP_encoding = "http://schemas.xmlsoap.org/soap/encoding/";
    protected static final String SOAP_PREFIX = "soap";
    protected static final String SOAPENC_PREFIX = "SOAP-ENC";
    protected static final String soap_binding = SOAP_NS + " binding";
    protected static final String soap_operation = SOAP_NS + " operation";
    protected static final String soap_body = SOAP_NS + " body";
    protected static final String soap_address = SOAP_NS + " address";
    
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
    protected static final String wsdl_arrayType = WSDL_NS + " arrayType";
        
    private static final String _targetNamespace = "targetNamespace";
    private static final String _tns = "tns";
    private static final String _tns_prefix = "tns:";
    private static final String _message = "message";
    private static final String _type = "type";
    private static final String _Port = "Port";
    private static final String _Binding = "Binding"; 
    private static final String _Service = "Service"; 
    private static final String _style = "style"; 
    private static final String _rpc = "rpc";
    private static final String _transport = "transport";
    private static final String _soapAction = "soapAction";
    private static final String _encodingStyle = "encodingStyle"; 
    private static final String _use = "use";
    private static final String _encoded = "encoded"; 
    private static final String _namespace = "namespace"; 
    private static final String _binding = "binding";
    private static final String _location = "location";

    /**
     * Produces a WSDL description of this SOAP function from a regular 
     * JSON expression of the form.
     * 
     * <pre>{"Request": ..., "Response": ...}</pre>
     * 
     * <h3>Synopsis</h3>
     * 
     * <p>To declare and enforce a different model than the default presented
     * above, override the <code>jsonInterface</code> method to return the
     * regular JSON string describing this function's input and output.</p>
     * 
     * <p>Note that all that port, binding and service whoopla is supported
     * for the simplest case: one method, one class, one namespace and one URL
     * for each services. This is functional WSDL, I'm not even trying to
     * make services look like objects with methods, that is not what they
     * are. Well designed web services are stateless functions.</p>
     * 
     * @param url this function's service address and its schema's namespace.
     * @param action the name of this SOAP action
     * @param model of RPC to map from JSONR to WSDL
     * @return an <code>XML.Document</code> tree ready to be serialized
     */
    public static final XML.Document wsdl (
        String url, String action, JSON.Object jsonr
        ) throws Exception {
        String urn = "urn:" + action;
        XML.Document doc = new XML.Document();
        doc.ns.put(urn, _tns);
        doc.ns.put(XSD_NS, XSD_PREFIX);
        doc.ns.put(SOAP_NS, SOAP_PREFIX);
        doc.ns.put(SOAP_encoding, SOAPENC_PREFIX);
        doc.ns.put(WSDL_NS, WSDL_PREFIX);
        doc.root = new XML.Element(wsdl_definitions, new String[]{
            _targetNamespace, urn
            }, null, null);
        // XSD schema <types> declaration
        XML.Element schema = doc.root
            .addChild(wsdl_types)
                .addChild(xsd_schema);
        // SOAP input <message>, RPC style
        XML.Element message = doc.root.addChild(
            wsdl_message, new String[]{_name, action + _Request}
            );
        JSONR.Type inputType = (JSONR.Type) jsonr.get(_Request);
        if (inputType.name().equals("namespace")) {
             JSON.Object types = (JSON.Object) inputType.json();
             Iterator names = types.keySet().iterator();
             String name;
             XML.Element element;
             while (names.hasNext()) {
                 name = (String) names.next();
                 element = xsd(schema, (JSONR.Type) types.get(name), name);
                 message.addChild(wsdl_part, new String[]{
                     _name, element.getAttribute(_name), 
                     _type, element.getAttribute(_type) 
                     });
             }
        } else {
            XML.Element input = xsd(
                schema, (JSONR.Type) inputType, action + _Request
                );
            message.addChild(wsdl_part, new String[]{
                _name, "arg0", 
                _type, input.getAttribute(_type)  
                });
        }
        // SOAP output <message>, RPC style
        XML.Element output = xsd(
            schema, 
            (JSONR.Type) jsonr.get(_Response), 
            action + _Response
            );
        message = doc.root.addChild(wsdl_message, new String[]{
            _name, action + _Response
            });
        message.addChild(wsdl_part, new String[]{
            _name, _return, 
            _type, output.getAttribute(_type)  
            });
        // one WSDL operation's <port> 
        XML.Element operation = doc.root
            .addChild(wsdl_portType, new String[]{_name, action + _Port})
            .addChild(wsdl_operation, new String[]{_name, action});
        operation.addChild(wsdl_input, new String[]{
            _name, action + _Request,
            _message, _tns_prefix + action + _Request
            });
        operation.addChild(wsdl_output, new String[]{
            _name, action + _Response,
            _message, _tns_prefix + action + _Response
            });
        // one WSDL operation's <binding>
        XML.Element binding = doc.root.addChild(wsdl_binding, new String[]{
            _name, action + _Binding, 
            _type, _tns_prefix + action + _Port
            });
        binding.addChild(soap_binding, new String[]{
            _style, _rpc, 
            _transport, SOAP_http    
            });
        operation = binding.addChild(wsdl_operation, new String[]{
            _name, action     
            });
        operation.addChild(soap_operation, new String[]{
            _soapAction, action
            });
        operation
            .addChild(wsdl_input, new String[]{
                _name, action + _Request    
                })
            .addChild(soap_body, new String[]{
                _namespace, urn,
                _encodingStyle, SOAP_encoding, 
                _use, _encoded
                });
        operation
            .addChild(wsdl_output, new String[]{
                _name, action + _Response 
            })
            .addChild(soap_body, new String[]{
                _namespace, urn,
                _encodingStyle, SOAP_encoding, 
                _use, _encoded
                });
        // one WSDL <service> port and address
        doc.root
            .addChild(wsdl_service, new String[]{
                _name, action + _Service
                })
                .addChild(wsdl_port, new String[]{
                    _binding, _tns_prefix + action + _Binding,
                    _name, action
                    })
                    .addChild(soap_address, new String[]{
                        _location, url
                        });
        return doc; // it looks like XML, does it not?
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
        } else if (value instanceof List) {
            strb(sb, ((List) value).iterator(), name);
        } else if (value instanceof Iterator) {
            sb.append('<');
            sb.append(name+_Array);
            sb.append(" SOAP-ENC:arrayType=\"");
            sb.append(name);
            sb.append("[]\" xsi:type=\"SOAP-ENC:Array\">");
            Iterator it = ((Iterator) value);
            while (it.hasNext())
                strb(sb, it.next(), name);
            sb.append("</");
            sb.append(name+_Array);
            sb.append('>');
        } else {
            Class type = null;
            try {type = value.getClass();} catch (Throwable e) {;}
            if (type !=null && type.isArray()) {
                Class component = type.getComponentType();
                if (component.isPrimitive())
                    ; // strb(sb, value, component);
                else
                    strb(sb, Objects.iter((java.lang.Object[]) value), name);
            } else
                strb(sb, value.toString(), name);
        }
        return sb;
    }
    
    protected static final String _utf8 = "UTF-8";
    
    private static final String _return = "return";
    
    /**
     * Encode a SOAP response as a single <code>return</code> element,
     * with support for a few simple types only, just enough <em>not</em> to
     * lock its application in interroperability and dependencies woes.
     * 
     * <p>Most applications are not expected to use or override this method,
     * some may have to in order to cache response as bytes buffers or 
     * supplement legacy types.</p> 
     * 
     * @param value
     * @param name
     * @return
     */
    public static final byte[] encode (Object value, String name) { 
        StringBuffer sb = new StringBuffer();
        sb.append('<');
        sb.append(name);
        sb.append('>');
        strb(sb, value, _return);
        sb.append("</");
        sb.append(name);
        sb.append('>');
        return Bytes.encode(sb.toString(), _utf8); 
    }
    
    protected static final byte[] _response_head = Bytes.encode(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<SOAP-ENV:Envelope "
            + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " 
            + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            + "xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" " 
            + ">"
            + "<SOAP-ENV:Body>", 
        _utf8
        );
    protected static final byte[] _response_tail = Bytes.encode(
            "</SOAP-ENV:Body>" 
        + "</SOAP-ENV:Envelope>", 
        _utf8
        );
    
    /**
     * Encode an Object as a SOAP response without header and send 
     * it in the simplest envelope. 
     * 
     * <h4>Synopsis</h3>
     * 
     * <pre>...</pre>
     * 
     * @param response the response string
     * @return
     */
    public static final void response (Actor $, Object body) {
        $.httpResponse(200, Bytes.buffer(new byte[][]{
            _response_head, 
            encode(body, $.about.substring(1)+_Response), 
            _response_tail
            }), XML.MIME_TYPE, _utf8);
    } // isn't this one liner elegant?
    
    protected static final byte[] _response_envelope = Bytes.encode(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<SOAP-ENV:Envelope "
            + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " 
            + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            + "xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" " 
            + ">",
        _utf8
        );
    protected static final byte[] _response_body = Bytes.encode(
        "<SOAP-ENV:Body>", _utf8
        );
    
    protected static final Map _NS = Objects.update(
            new HashMap(), new Object[]{
                "http://www.w3.org/2001/XMLSchema", "xsd",
                "http://www.w3.org/2001/XMLSchema-instance", "xsi",
                "http://schemas.xmlsoap.org/soap/envelope/", "SOAP-ENV"
                }
            );
        
    /**
     * Encode <code>$.json</code> as a SOAP response and a prefixed 
     * <code>XML.Element</code> as header, then send it in the simplest 
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
        Actor $, Object body, XML.Element header
        ) {
        $.httpResponse(200, Bytes.buffer(new byte[][]{
            _response_envelope,
            XML.encodeUTF8(header, _NS), 
            _response_body,
            encode(body, $.about.substring(1)+_Response), 
            _response_tail
            }), XML.MIME_TYPE, _utf8);
    }
    
    protected static final byte[] _fault_head = Bytes.encode(
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
    protected static final byte[] _fault_tail = Bytes.encode(
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
        $.httpResponse(500, Bytes.buffer(new byte[][]{
            _fault_head, Bytes.encode(message, _utf8), _fault_tail
            }), XML.MIME_TYPE, _utf8);
    }

    protected static final byte[] _fault_body = Bytes.encode(
            "<SOAP-ENV:Fault>" 
                + "<faultstring xsi:type=\"xsd:string\"><![CDATA[",
        _utf8
        );
    
    /**
     * Encode a SOAP fault with a string as body and an
     * <code>XML.Element</code> as header, then send it in the simplest 
     * envelope.
     * 
     * <h4>Synopsis</h3>
     * 
     * <pre>...</pre>
     * 
     * @param response the response string
     * @return
     */
    public static final void fault (
        Actor $, String message, XML.Element header
        ) {
        $.httpResponse(500, Bytes.buffer(new byte[][]{
            _response_envelope,
            XML.encodeUTF8(header, _NS), 
            _fault_body, 
            Bytes.encode(message, _utf8), 
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
