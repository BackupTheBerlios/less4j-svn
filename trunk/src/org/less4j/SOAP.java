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
import java.util.HashMap;

/**
 * A lean and simple SOAP request decoder and UTF8 response encoder that
 * produce and consume JSON objects. 
 * 
 * <h3>Synopsis</h3>
 * 
 * <p>...</p>
 * 
 * <h4>Request</h4>
 * 
 * <pre>try {
 *    SOAP.Document request = SOAP.read($);
 *    System.out.println(JSON.repr(request.json));
 *} catch (SOAP.Error e) {
 *    System.err.println(e);
 *}</pre> 
 * 
 * <p>Unobviously at first, the XML interpreter built in does not even try
 * to wax an infuckingcredible object notation. Instead it maps a simple
 * object access protocol of any XML stream into the flattest and simplest
 * JSON instance possible, using types whenever a name is mapped to a
 * regular JSON type.</p>
 * 
 * <p>Feed test XML requests to a plain <code>SOAP.Controller</code> and see 
 * what comes out of it.</p>
 * 
 * <p>The parser applied is XP, the original XML reference implementation
 * in Java and still one of the fastest and leanest. An XML intermediary
 * data structure is used, but useless features and complications are 
 * simply ignored, only the JSON types are supported in "plain old" SOAP 
 * and a subset of JSONR can be supported.</p>
 * 
 * <pre>try {
 *    SOAP.Document request = SOAP.read($, JSONR.compile("null"));
 *    System.out.println(JSON.repr(request.json));
 *} catch (SOAP.Error e) {
 *    System.err.println(e);
 *}</pre> 
 * 
 * <p>Your mileage <em>will</em> vary, this XML request decoder provides
 * its applications with a single view of all messages through a simpler
 * object access protocol. It does not try to make Sun's and Microsoft's 
 * interpretation of the standard more interroperable, instead it lets
 * you cherry-pick what you need in the whole HTTP request submitted,
 * down to the last attributes of an XML body. Or conveniently use a
 * flatter JSON object, possibly regularly typed once enough test data
 * is available.</p>
 * 
 * <h4>Response</h4>
 * 
 * <p>Only one type of response is allowed, a simple string in the simplest
 * SOAP message encoded in UTF-8:</p>
 * 
 * <pre>SOAP.response($, "00c7932fa021c9acc530");</pre> 
 * 
 * <p>What about numbers, containers, type mapping and all that 
 * family of powerfull features supported by WS-* & Co.</p>
 * 
 * <p>Well, a <code>String</code> as response is all you'll ever need, 
 * because a SOAP application in Java is allways an entreprise gateway 
 * to something asynchronous, usually a long running job on a linux server 
 * or a message in a mainframe batch. Interactive SOAP web services is a 
 * technology as alive as Java applets, IIOP, RMI, DCOM, CORBA or SNA.</p>
 * 
 * <p>Note that SOAP response messages are fully buffered, ideally they 
 * should fit in the system's TCP stack buffers and UDP datagram largest
 * packets. If you can't break transaction is such fine grains, don't
 * even try to insert a middle tier between clients and servers.</p>
 * 
 * <h4>Fault</h4>
 * 
 * <p>...</p>
 * 
 * <h3>Applications</h3>
 * 
 * <h4>Security</h4>
 * 
 * <p>Note that IRTD2 cookies are used for identification, authorization
 * and audit of the SOAP transactions by less4j, not one of the proprietary 
 * implementation of WS-Security.</p> 
 * 
 * <p>If you <em>really</em> need more security features in the usual 
 * environments where WS-* has been adopted, then there's a great deal of 
 * chance that you'd rather have X509 client certificates identifying user 
 * agents using SSL/TLS encrypted connections.</p>
 * 
 * <p>And that is better handled out of the J2EE container, by stunnel for
 * clients or Apache's mod_ssl in a forward proxy.</p>
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
public class SOAP {
    // ... devilish details ... 
    protected static class Element extends XML.Element {
        public static XML.Type TYPE = new Document();
        public JSON.Object json = null;
        public Element (String name, HashMap attributes) {
            super(name, attributes);
        }
        public void jsonUpdate(Element container, Object contained) {
            JSON.Object map = container.json; 
            if (map == null) {
                map = new JSON.Object();
                container.json = map;
                map.put(name, first);
            } else if (map.containsKey(name)) {
                Object o = map.get(name);
                if (o instanceof JSON.Array) {
                    ((JSON.Array) o).add(contained);
                } else {
                    JSON.Array list = new JSON.Array();
                    (list).add(contained);
                    map.put(name, list);
                }
            } else
                map.put(name, contained);
        }
        public void valid(Document document) {
            if (parent != null) // trunk
                if (children == null) // leaves
                    if (first != null)
                        jsonUpdate((Element) parent, first);
                    else if (attributes != null)
                        jsonUpdate((Element) parent, attributes);
                else if (json != null) // branch with leaves
                    jsonUpdate((Element) parent, json);
            else 
                ; // root
        }
    } 
    protected static class ENV_BODY extends Element {
        public static XML.Type TYPE = new ENV_BODY(
            "http://schemas.xmlsoap.org/soap/envelope/ Body", null
            ); 
        public XML.Element newElement(
            String name, HashMap attributes
            ) {
            return new ENV_BODY(name, attributes);
        };
        public ENV_BODY (String name, HashMap attributes) {
            super(name, attributes);
        }
        public void valid(Document document) {
            // attach the JSON object to the SOAP body ...
            document.json = json; 
            json = null;
            first = null;
            children = null; 
            // ... drop the markup and text inside
        }
    }
    protected static class Document extends XML.Document {
        public JSON.Object json = null;
        public XML.Element newElement(String name, HashMap attributes) {
            return new Element(name, attributes);
        }
    }
    protected static final String _text_xml = "text/xml";
    protected static final Map _SOAP_TYPES = Simple.dict(
        new HashMap(), new Object[]{
            "http://schemas.xmlsoap.org/soap/envelope/ Envelope",
            XML.Element.TYPE,
            "http://schemas.xmlsoap.org/soap/envelope/ Header",
            XML.Element.TYPE,
            "http://schemas.xmlsoap.org/soap/envelope/ Body",
            ENV_BODY.TYPE
            });

    // TODO: ? move to a more sophisticated controller, one that
    //       allows to be controlled by web 2.0 functions and
    //       yet also "host" SOAP applications dispatched from
    //       references found in its envelope at a single URL
    //       (aka "end-point") ?
    //

    public class Controller extends org.less4j.Controller {
        public void httpContinue (
            Actor $, String method, String contentType
            ) {
            if (
                method.equals(_POST) && contentType != null &&
                contentType.startsWith(_text_xml)
                ) try {
                this.soapApplication($, (Document) XML.read(
                    $.request.getInputStream(), $.url, null, 
                    _SOAP_TYPES, new Document()
                    )); 
            } catch (Exception e) {
                $.logError(e);
                $.httpError(500);
            } else
                $.httpError(400);
        }
        public void soapApplication (Actor $, Document document) {
            SOAP.response($, JSON.encode(document.json));
        }
    }

    protected static final byte[] _response_head = Simple.encode(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<SOAP-ENV:Envelope "
            + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " 
            + "xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" " 
            + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            + ">" 
            + "<SOAP-ENV:Body>" 
                + "<less4j:response " 
                    + "xmlns:less4j=\"http://less4j.org/\" " 
                    + ">" 
                    + "<result xsi:type=\"xsd:string\">",
        "UTF-8"
        );
    protected static final byte[] _response_tail = Simple.encode(
                    "</result>" 
                + "</less4j:response>" 
            + "</SOAP-ENV:Body>" 
        + "</SOAP-ENV:Envelope>", 
        "UTF-8"
        );
    /**
     * Encode a SOAP response as fast and safe as possible: made of a 
     * single <code>String</code> encoded in UTF-8 sandwiched in a template,
     * litterraly: 
     * 
     * <h4>Synopsis</h3>
     * 
     * @param response the response string
     * @return
     */
    public static boolean response (Actor $, String text) {
        return false;
    }
    
    protected static final byte[] _fault_head = Simple.encode(
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
        + "<SOAP-ENV:Envelope "
            + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " 
            + "xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" " 
            + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
            + ">" 
            + "<SOAP-ENV:Fault>" 
                + "<faultstring xsi:type=\"xsd:string\">",
        "UTF-8"
        );
    protected static final byte[] _fault_tail = Simple.encode(
                "</faultstring>" 
            + "</SOAP-ENV:Fault>" 
        + "</SOAP-ENV:Envelope>", 
        "UTF-8"
        );
    public static boolean fault (Actor $, String text) {
        return false;
    }
}

//
// Rule N°1: don't bother with broken "industry standard" invented by 
// Microsoft, adopted by Sun and supported by IBM.
//
// Rule N°2: interpret them loosely and deliver a migration path.
//
