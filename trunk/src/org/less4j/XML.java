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

import java.net.URL;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.ByteArrayOutputStream;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Locale;
import java.util.HashSet;
import java.util.NoSuchElementException;

import com.jclark.xml.parse.ProcessingInstructionEvent;
import com.jclark.xml.parse.StartElementEvent;
import com.jclark.xml.parse.EndElementEvent;
import com.jclark.xml.parse.CharacterDataEvent;
import com.jclark.xml.parse.DocumentParser;
import com.jclark.xml.parse.OpenEntity;
import com.jclark.xml.parse.EntityManagerImpl;
import com.jclark.xml.parse.base.ApplicationImpl;
import com.jclark.xml.output.UTF8XMLWriter;
import com.jclark.xml.sax.ReaderInputStream;

/**
 * An port of Greg Stein's <a 
 * href="http://www.lyra.org/greg/python/qp_xml.py"
 * >qp_xml.py</a> for Java, producing the simplest and most practical XML 
 * element tree, enhanced by an extensible type system to develop XML 
 * language interpreters.
 * 
 * @synopsis import org.less4j.XML;
 *import org.less4j.Simple;
 *import java.util.Iterator;
 *import java.io.File;
 *import java.io.IOException;
 *
 *try {
 *    // read an XML file into an element tree
 *    XML.Document document = new XML.Document();
 *    document.read(new File("data.xml"));
 *    // access and update the element tree 
 *    Iterator items = document.root
 *        .getChild("details")
 *        .getChildren(Simple.set(new String[]{"item"}));
 *    int total = 0;
 *    while (items.hasNext()) {
 *        total = total + Integer.parseInt(
 *            ((XML.Element) items.next()).getAttribute("value")
 *            );
 *    }
 *    document.root.addChild("total", Integer.toString(total));
 *    // write the transformed XML to STDOUT
 *    document.write(System.out);
 *} catch (XML.Error e) {
 *    : // handle XML errors
 *} catch (IOException e) {
 *    ; // handle I/O errors
 *}
 *
 * @div <h3>Applications</h3>
 *
 * <p>The minimal Document Object Model (DOM) provided is the defacto standard
 * element tree found across all development environments. It makes simple
 * XML data processing easy and complex one possible.</p>
 *
 * <p>This API support XML namespaces in the simplest possible way,
 * as the later C versions expat implemented them. Qualified names are
 * represented as a string joining the namespace and the local name with
 * a single space character, unqualified names (aka tags) are represented
 * as a string without white spaces.</p>
 * 
 * <p>The implementation of this <code>XP</code> parser application also 
 * provides an extensible type system for <code>Element</code> nodes
 * that support reuse of classes in the development of XML interpreters,
 * turning the DOM into an AST.</p>
 * 
 */
public class XML {

    public static final String MIME_TYPE = "text/xml";

    protected static final String _utf8 = "UTF-8";
    
    /**
     * An error class derived from <code>Exception</code>, throwed
     * by the <code>XP</code> parser when the XML 1.0 processed is 
     * not-well formed, a namespace prefix is not declared or an
     * exception is raised by its application.
     */
    public static class Error extends Exception {
        private String _message;
        /**
         * Instanciate a new exception with the given message.
         * 
         * @param message the error(s information <code>String</code>
         */
        public Error (String message) {_message = message;}
        /**
         * Returns the error's message, usually it will be <code>XP</code>
         * exception's message unless the error was raised by a broken XML
         * namespace prefix.
         * 
         * @return a message <code>String</code>
         */
        public String getMessage () {return _message;}
    };
    
    /**
     * An interface to extend the tree builder with new classes of element 
     * derived from <code>Element</code>.
     */
    public static interface Type {
        public Element newElement (String name, HashMap attributes);
    }
     
    /**
     * A base class for all nodes in an XML element tree, with only five
     * properties, just enough to fully represent an XML string (without
     * comments) as a compact tree of Java objects with the smallest
     * memory footprint.
     */
    public static class Element implements XML.Type {
        /**
         * The fully qualified name of this element, either a simple XML
         * tag or the concatenation of a namespace and a tag joined by
         * a white space.
         */
        public String name = null;
        /**
         * A pointer to this element's parent.
         */
        public Element parent = null;
        /**
         * The first CDATA (ie: text) after this element's opening tag
         * and before this element's first child.
         */
        public String first = null;
        /**
         * The CDATA after this element's closing tag and before this 
         * element's next sibbling.
         */
        public String follow = null;
        /**
         * A list of this element's children. 
         */
        public ArrayList children = null;
        /**
         * A map of this element's fully qualified attributes.
         */
        public HashMap attributes = null;
        /**
         * Instanciate a new empty <code>Element</code> with name.
         * 
         * @param name the fully qualified name of this element
         */
        public Element (String name) {
            this.name = name;
        }
        /**
         * Instanciate a new empty <code>Element</code> with name and 
         * attributes.
         * 
         * @param name the fully qualified name of this element
         * @param attributes a <code>HashMap</code> of named attributes
         */
        public Element (String name, HashMap attributes) {
            this.name = name;
            this.attributes = attributes;
        }
        /**
         * Instanciate a new childless <code>Element</code> with attributes,
         * first and following CDATA.
         * 
         * @param name the fully qualified name of this element
         * @param attributes as an even array of names and values
         * @param first text after the opening tag 
         * @param follow text after the closing tag
         * 
         * @synopsis new XML.Element("a", new String[]{
         *    "href", "#", "name", "top"
         *    }, "go to top", "\r\n");
         *    
         * @div This constructor is usefull when building element trees
         * from scratch.
         * 
         */
        public Element (
            String name, String[] attributes, String first, String follow
            ) {
            this.name = name;
            if (attributes != null)
                this.attributes = (HashMap) Simple.dict(
                    new HashMap(), attributes
                    );
            this.first = first;
            this.follow = follow;
        }
        /**
         * Let the tree builder instanciate a new <code>Element</code> 
         * with attributes using this <code>Type</code> interface of
         * this class' <code>singleton</code>. 
         * 
         * @param name the fully qualified name of this element
         * @param attributes a HashMap of names and values
         */
        public Element newElement (String name, HashMap attributes) {
            return new Element(name, attributes);
        }
        /**
         * This class <code>Type</code> singleton exposing this class
         * <code>newElement</code> method. 
         * 
         * Java sucks.
         */
        public static Type TYPE = new Document();
        /**
         * Add a child element, creating the <code>children</code> array list
         * if it does not exist yet.
         * 
         * @param child
         */
        public XML.Element addChild (Element child) {
            if (children == null) 
                children = new ArrayList();
            children.add(child);
            child.parent = this;
            return child;
        }
        /**
         * Add a new named child element, creating the <code>children</code> 
         * array list if it does not exist yet.
         * 
         * @param name
         * @return
         */
        public XML.Element addChild (String name) {
            return addChild(new Element(name, null));
        }
        /**
         * ...
         * 
         * @param name
         * @return
         */
        public XML.Element addChild (String name, String first) {
            XML.Element child = addChild(new Element(name, null));
            child.first = first;
            return child;
        }
        /**
         * ...
         * 
         * @param name
         * @return
         */
        public XML.Element addChild (String name, String[] attrs) {
            return addChild(new Element(
                name, (HashMap) Simple.dict(new HashMap(), attrs)
                ));
        }
        /**
         * ...
         * 
         * @param name
         * @return
         */
        public XML.Element addChild (
            String name, String[] attrs, String first, String follow
            ) {
            XML.Element child = addChild(new Element(
                name, (HashMap) Simple.dict(new HashMap(), attrs)
                ));
            child.first = first;
            child.follow = follow;
            return child;
        }
        /**
         * ...
         * 
         * @param cdata
         * 
         * @synopsis XML.Element parent = new XML.Element("tag");
         * parent.addCdata("first text inside the parent");
         * parent.addChild("child");
         * parent.addCdata("text following ..."
         * parent.addCdata("... the first child");
         */
        public void addCdata (String cdata) {
            if (children == null || children.size() == 0) {
                if (first == null)
                    first = cdata;
                else
                    first = first + cdata;
            } else {
                Element child = getChild(children.size()-1);
                if (child.follow == null)
                    child.follow = cdata;
                else
                    child.follow = child.follow + cdata;
            }
        }
        /**
         * Returns the element's local name.
         * 
         * @synopsis (new XML.Element("urn:NameSpace tag")).getLocalName();
         * 
         */
        public String getLocalName () {
            int local = name.indexOf(' ');
            if (local > -1)
                return name.substring(local+1);
            else
                return name;
        }
        /**
         * A convenience to access child <code>Element</code> by index. 
         * 
         * @param index of the child in this element's children.
         * @return an <code>XML.Element</code> of null
         * 
         * @synopsis (new XML.Element("tag")).getChild(3);
         */
        public Element getChild (int index) {
            if (children == null)
                return null;
            else
                return (Element) children.get(index);
        }
        /**
         * A convenience to access a first child <code>Element</code> by name,
         * returns null. 
         * 
         * @param name of the child.
         * @return an <code>XML.Element</code> or null
         * 
         * @synopsis (new XML.Element("tag")).getChild("name");
         */
        public Element getChild (String name) {
            Element child;
            if (children != null) for (int i=0, L=children.size(); i<L; i++) {
                child = (Element) children.get(i);
                if (child.name.equals(name)) 
                    return child;
            }
            return null;
        }
        
        public Element getLastChild () {
            if (children == null)
                return null;
            
            return getChild(children.size()-1);
        }
        
        protected class ChildrenIterator implements Iterator {
            private Iterator _children = null;
            private Element _next = null;
            private HashSet _names = null;
            public ChildrenIterator (Element element, HashSet names) {
                if (element.children == null)
                    return;
                
                _children = element.children.iterator();
                _names = names;
                try {
                    next();
                } catch (NoSuchElementException e) {
                    _next = null;
                }
            };
            public boolean hasNext() {
                return (_next == null);
            }
            public Object next() {
                Object result = _next;
                while (_children.hasNext()) {
                    _next = (Element) _children.next();
                    if (_names.contains(_next.name))
                        return result;
                }
                throw new NoSuchElementException();
            }
            public void remove() {} // wtf?
        }

        /**
         * A convenience to iterate through named children.
         * 
         * @param name of the children.
         * @return an <code>Iterator</code> of <code>XML.Element</code>
         * 
         * @synopsis Iterator named = element.getChildren(
         *    Simple.set({"a", "A"})
         *    );
         */
        public Iterator getChildren (HashSet names) {
            return new ChildrenIterator(this, names);
        }
        /**
         * A convenience to access an attribute <code>String</code> value
         * by name, returns null if the named attribute does not exists. 
         * 
         * @param name of the attribute
         * @return the value of the named attribute as a string
         * 
         * @synopsis (new XML.Element("tag")).getAttribute("name");
         */
        public String getAttribute (String name) {
            if (attributes == null)
                return null;
            else
                return (String) attributes.get(name);
        }
        /**
         * Set the named attribute's value, creating the attributes' map if
         * it does not exist yet.
         * 
         * @param name
         * @param value
         * 
         * @synopsis (new XML.Element("tag")).setAttribute("name", "value");
         */
        public void setAttribute (String name, String value) {
            if (attributes == null) 
                attributes = new HashMap();
            attributes.put(name, value);
        }
        /**
         * Break all circular reference for this element and all its children.
         */
        public void collect () {
            parent = null;
            if (children != null) {
                Iterator child = children.iterator();
                while (child.hasNext()) {
                    ((Element) child.next()).collect();
                }
            }
        }
        /**
         * Remove this element and all its children from the tree, folding
         * up the following CDATA and collecting circular references in its
         * branch.
         */
        public void delete () {
            int index = parent.children.indexOf(this);
            if (index == 0) {
                if (parent.first == null)
                    parent.first = follow;
                else
                    parent.first = parent.first + follow; 
            } else {
                XML.Element previous = (
                    (Element) parent.children.get(index-1)
                    );
                if (previous.follow == null)
                    previous.follow = follow;
                else
                    previous.follow = previous.follow + follow;
            }
            parent.children.remove(index);
            collect();
        }
        /**
         * This is a method called by <code>QP</code> when an element has 
         * been parsed without errors.
         * 
         * @param doc the Document currently parsed
         * 
         * @div The purpose of this interface is to allow derived classes of
         * <code>Element</code> to implement "Object Oriented Pull Parsers"
         * (OOPP). The benefit of this pattern is to avoid the need to walk
         * the element tree and instead process elements as they are
         * validated by the parser, using this simple DOM as an abstract
         * syntax tree.
         * 
         */
        public void valid (Document doc) throws Error {
            ;
        }
        public byte[] encodeUTF8(Map ns) {
            return XML.encodeUTF8(this, ns);
        }
        /**
         * 
         */
        public String toString() {
            return Simple.decode(XML.encodeUTF8(this, null), _utf8);
        }
    }
    
    /**
     * A class with just enough properties to fully represent an XML
     * document with namespaces and processing instructions. 
     */
    public static class Document implements Type {
        /**
         * This document's root <code>XML.Element</code>.
         */
        public Element root = null;
        /**
         * A <code>HashMap</code> of processing instructions, mapping
         * instruction names to lists of processing parameter strings.
         */
        public HashMap pi;
        /**
         * A map of prefix strings to namespace strings.
         */
        public HashMap ns;
        /**
         * Instanciate a new document.
         */
        public Document () {
            pi = new HashMap();
            ns = new HashMap();
        } 
        public Element newElement (String name, HashMap attributes) {
            return new Element(name, attributes);
        }
        /**
         * Try to parse an XML <code>InputStream</code> with path and base URL 
         * using the extension <code>types</code>. 
         * 
         * @param is InputStream to parse
         * @param path locating the parsed entity
         * @param baseURL for external entity resolution
         * @param types to use as extensions
         * @throws Error if the XML file is not well-formed or if one of the
         *               extension type is broken
         * @throws IOException raised by accessing the XML file
         */
        public void read (InputStream is, String path, URL baseURL, Map types) 
        throws Throwable {
            QP qp = new QP(this, types);
            DocumentParser.parse(new OpenEntity(
                is, path, baseURL
                ), new EntityManagerImpl(), qp, Locale.US);
        }
        /**
         * Try to parse an XML <code>file</code> using the extension 
         * <code>types</code>. 
         * 
         * @param file to parse
         * @param types to use as extensions
         * @throws Error if the XML file is not well-formed or if one of the
         *               extension type is broken
         * @throws IOException raised by accessing the XML file
         */
        public void read(File file, Map types) throws Throwable {
            read (
                new FileInputStream(file), 
                file.getAbsolutePath(), 
                file.toURL(),
                types
                );
        }
        public void read(File file) throws Throwable {
            read(
                new FileInputStream(file), 
                file.getAbsolutePath(), 
                file.toURL(), 
                null
                );
        }
        public static final String PROLOGUE = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>";
        public void write (OutputStream os, String prologue) 
        throws IOException {
            UTF8XMLWriter writer = new UTF8XMLWriter(os);
            writer.markup(prologue);
            // TODO: processing instructions
            writeUTF8(writer, root, ns);
            writer.flush();
        }
        public void write (OutputStream os) throws IOException {
            write(os, PROLOGUE);
        }
        public void write (File file, String prologue) throws IOException {
            FileOutputStream os = new FileOutputStream(file);
            write(os, prologue);
            os.close();
        }
        public void write (File file) throws IOException {
            write(file, PROLOGUE);
        }
        /**
         * Try to parse an XML <code>String</code>. 
         * 
         * @param string to parse
         * @param path locating the parsed entity
         * @param baseURL for external entity resolution
         * @param types to use as extensions
         * @throws Error if the XML file is not well-formed
         * @throws IOException raised by accessing the XML file
         */
        public void parse(String string, String path, URL baseURL, Map types) 
        throws Throwable {
            read(
                new ReaderInputStream(new StringReader(string)), 
                path, baseURL, types
                );
        }
        /**
         * Try to parse an XML <code>String</code>. 
         * 
         * @param string to parse
         * @param types to use as extensions
         * @throws Error if the XML file is not well-formed
         * @throws IOException raised by accessing the XML file
         */
        public void parse(String string, Map types) 
        throws Throwable {
            read(
                new ReaderInputStream(new StringReader(string)), 
                "", null, types
                );
        }
        /**
         * Try to parse an XML <code>String</code>. 
         * 
         * @param string to parse
         * @throws Error if the XML file is not well-formed
         * @throws IOException raised by accessing the XML file
         */
        public void parse(String string) 
        throws Throwable {
            read(
                new ReaderInputStream(new StringReader(string)), 
                "", null, null
                );
        }
        /**
         * 
         * @return
         */
        public byte[] encodeUTF8 () {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            try {
                this.write(os, "");
            } catch (IOException e) {
                ; // checked exceptions suck.
            }
            return os.toByteArray();
        }
        /**
         * 
         * @return
         */
        public String toString () {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            try {
                this.write(os, "");
            } catch (IOException e) {
                ; // checked exceptions suck ...
            }
            try {
                return os.toString(_utf8);
            } catch (Exception e){ 
                return os.toString(); // ... a lot!
            }
        }
    }    
    // The Quick Parser, in Java (a "little bit" slower).
    private static final String _xml = "xml";
    private static final String _xmlns = "xmlns";
    private static final String _xmlns_colon = "xmlns:";
    private static final String _no_prefix = "";
    private static final String 
    _namespace_prefix_not_found = "namespace prefix not found";
    protected static class QP extends ApplicationImpl {
        protected Document doc;
        protected Map types = null;
        protected HashMap prefixes = new HashMap();
        protected Element _curr = null;
        public QP (Document doc, Map types) {
            this.doc = doc;
            this.types = types;
            };
        public void processingInstruction(ProcessingInstructionEvent event) {
            String name = event.getName();
            if (doc.pi.containsKey(name))
                ((ArrayList) doc.pi.get(name)).add(event.getInstruction());
            else {
                ArrayList pi = new ArrayList();
                pi.add(event.getInstruction());
                doc.pi.put(name, pi);
            }
        }
        protected static String fqn (String name, HashMap prefixes) 
        throws Error {
            int colon = name.indexOf(':');
            if (colon > -1) {
                String prefix = name.substring(0, colon);
                if (prefix.equals(_xml))
                    return name.substring(colon+1);
                
                else if (prefixes.containsKey(prefix))
                    return (
                        ((String)prefixes.get(prefix)) 
                        + ' ' + name.substring(colon+1)
                        );
                
                throw new Error(_namespace_prefix_not_found);
                
            } else if (prefixes.containsKey(_no_prefix))
                return (
                    ((String)prefixes.get(_no_prefix)) 
                    + ' ' + name.substring(colon+1)
                    );
            
            return name;
        }
        public void startElement(StartElementEvent event) 
        throws Error {
            String name;
            HashMap attributes = null;
            int L = event.getAttributeCount();
            if (L > 0) {
                int A = 0;
                String[] attributeNames = new String[L]; 
                for (int i=0; i<L; i++) {
                    name = event.getAttributeName(i);
                    if (name.equals(_xmlns)) {
                        doc.ns.put(event.getAttributeValue(i), _no_prefix);
                        prefixes.put(_no_prefix, event.getAttributeValue(i));
                    } else if (name.startsWith(_xmlns_colon)) {
                        doc.ns.put(
                            event.getAttributeValue(i), name.substring(6)
                            );
                        prefixes.put(
                            name.substring(6), event.getAttributeValue(i)
                            );
                    } else {
                        attributeNames[i] = name; 
                        A++;
                    }
                }
                if (A > 0) {
                    attributes = new HashMap ();
                    for (int i=0; i<L; i++) 
                        if (attributeNames[i]!=null)
                            attributes.put(
                                fqn(attributeNames[i], prefixes), 
                                event.getAttributeValue(i)
                                );
                }
            }
            name = fqn(event.getName(), prefixes);
            Element e;
            if (types != null && types.containsKey(name))
                e = ((Type)types.get(name)).newElement(name, attributes);
            else
                e = doc.newElement(name, attributes);
            e.parent = _curr;
            if (_curr == null)
                doc.root = e;
            else
                _curr.addChild(e);
            _curr = e;
        }
        public void endElement(EndElementEvent event) throws Error {
            Element parent = _curr.parent;
            _curr.valid(doc);
            _curr = parent;
        }
        public void characterData (CharacterDataEvent event) 
        throws IOException {
            StringWriter sw = new StringWriter();
            event.writeChars(sw);
            sw.flush();
            _curr.addCdata(sw.toString());
        }
    }
    
    /**
     * Map any XML tag soup into a tree of JSON types that is as flat as 
     * possible, using local names only as identifiers.
     * 
     * @div This class extends XML.Element with a JSON object and overrides
     * the valid method to map the XML names and structures to a flatter,
     * and less ambiguous object model made of JSON objects, arrays and
     * strings. Its purpose is to avoid the complications of DOM walks
     * through XML data and instead provide practical JSON types that can
     * be validated by a regular JSON expression.
     * 
     * @synopsis import org.less4j.XML;
     *import org.less4j.JSON;
     *import org.less4j.JSONR;
     * 
     *public class MyXMLObject extends XML.Regular {
     *    private static final JSONR.Type model = JSONR.compile(
     *        "{\"item\": \"[0-9]{13}\", \"quantity\": [320.01]}"
     *        );
     *    public validate (XML.Tree application) {
     *        try {
     *            model.validate(this.json);
     *            // ... 
     *            // handle this element's data for the application
     *            // ... 
     *        } catch (JSONR.Error e) {
     *            // ... invalid JSON input, handle the error ...
     *            JSON.pprint(System.err, this.json)
     *        }
     *        this.delete(); // eventually release some memory ... 
     *    }
     *}
     * 
     * @div Programming an XML data processor becomes a lot less tedious
     * and error-prone wi 
     * 
     */
    public static class Regular extends Element {
        public static XML.Type TYPE = new Regular(null, null);
        public JSON.Object json = null;
        public Regular (String name, HashMap attributes) {
            super(name, attributes);
        }
        public Element newElement (String name, HashMap attributes) {
            return new Regular(name, attributes);
        }
        protected void update(
            String name, Object value, Regular container
            ) {
            JSON.Object map = container.json; 
            if (map == null) {
                map = new JSON.Object();
                container.json = map;
                map.put(name, value);
            } else if (map.containsKey(name)) {
                Object o = map.get(name);
                if (o instanceof JSON.Array) {
                    ((JSON.Array) o).add(value);
                } else {
                    JSON.Array list = new JSON.Array();
                    list.add(o);
                    list.add(value);
                    map.put(name, list);
                }
            } else
                map.put(name, value);
        }
        private static final String _text = "text";
        /**
         * ...
         */
        public void valid(XML.Document document) throws XML.Error {
            Object value = null;
            Regular container = getContainer();
            if (container == null) // root 
                ;
            else if (children == null) { // leaf
                if (attributes != null) { // complex type of attributes
                    if (first != null) {
                        attributes.put(_text, first);
                    }
                    value = attributes;
                } else if (first != null) { // simple types
                    value = first;
                }
            } else { // branch, complex type of elements
                if (json != null) {
                    if (attributes != null)
                        json.putAll(attributes);
                    value = json;
                } else if (attributes != null) {
                    value = attributes;
                }
            }
            this.validate (value, container, (Tree) document);
        }
        /**
         * Applications of XML.Regular should override this method for each
         * relevant type. The default implemention is to update the container's 
         * JSON object and delete the validated element from the XML tree if 
         * there is a container, leaving only the JSON structures.    
         * 
         * @param value
         * @param container
         * @param document
         */
        public void validate(
            Object value, Regular container, Tree document
            ) {
            if (container != null) { 
                if (value != null)
                    update(getLocalName(), value, container);
                delete();
            }
        }
        public Regular getContainer() {
            Element container = this.parent;
            while (container != null) {
                if (container instanceof Regular)
                    return (Regular) container;

                container = container.parent;
            }
            return null;
        }
    } 
    
    /**
     * ...
     */
    public static class Tree extends Document {
        public Element newElement(String name, HashMap attributes) {
            return new Regular(name, attributes);
        }
    }
    
    private static final String _prefix = "ns";
    public static final String prefixed (String name, Map ns) {
        int fqn = name.indexOf(' ');
        if (fqn > -1) {
            String namespace = name.substring(0, fqn);
            String prefix = (String) ns.get(namespace);
            if (prefix == null) {
                prefix = _prefix + Integer.toString(ns.size());
                ns.put(namespace, prefix);
                name = prefix + ':' + name.substring(fqn+1);
            } else if (prefix == _no_prefix) {
                name = name.substring(fqn+1);
            } else {
                name = prefix + ':' + name.substring(fqn+1);
            }
        }
        return name;
    }
    /**
     * 
     * @param os
     * @param element
     * @param ns
     */
    public static final void writeUTF8 (
        UTF8XMLWriter writer, Element element, Map ns
        )
    throws IOException {
        String tag = prefixed (element.name, ns);
        writer.startElement(tag);
        if (element.parent == null) { // root, declare namespaces now
            String namespace;
            Iterator namespaces = ns.keySet().iterator();
            while (namespaces.hasNext()) {
                namespace = (String) namespaces.next();
                writer.attribute(
                    _xmlns_colon + (String) ns.get(namespace), namespace
                    );
            }
        }
        if (element.attributes != null) {
            String name;
            Iterator names = element.attributes.keySet().iterator();
            while (names.hasNext()) {
                name = (String) names.next();
                writer.attribute(
                    prefixed(name, ns), (String) element.attributes.get(name)
                    );
            }
        }
        if (element.first != null)
            writer.write(element.first);
        if (element.children != null) {
            Iterator _children = element.children.iterator();
            while (_children.hasNext())
                writeUTF8(writer, (Element) _children.next(), ns);
        }
        writer.endElement(tag);
        if (element.follow != null)
            writer.write(element.follow);
    }
    /**
     * 
     * @param element
     * @param ns
     * @return
     */
    public static final byte[] encodeUTF8 (Element element, Map ns) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            UTF8XMLWriter writer = new UTF8XMLWriter(os);
            writeUTF8(writer, element, ns);
            writer.flush();
        } catch (IOException e) {
            ; // checked exceptions suck.
        }
        return os.toByteArray();
    }
    
}
