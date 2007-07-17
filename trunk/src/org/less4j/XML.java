package org.less4j;

import java.net.URL;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.StringWriter;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.HashSet;
import java.util.NoSuchElementException;

import com.jclark.xml.parse.*;
import com.jclark.xml.parse.base.*;

/**
 * An port of Greg Stein's <a 
 * href="http://www.lyra.org/greg/python/qp_xml.py"
 * >qp_xml.py</a> for Java, producing the simplest and most practical XML 
 * element tree, enhanced by an extensible type system to develop XML 
 * language interpreters.
 * 
 * <h3>Synopsis</h3>
 * 
 * <p>This is a practical convenience for simple XML file processing:</p>
 * 
 * <pre>import org.less4j.XML;
 *import java.io.File;
 *import java.io.IOException;
 *
 *try {
 *    XML.Document document = XML.read(new File("my.xml"));
 *    XML.writeUTF8(System.out, document);
 *} catch (XML.Error e) {
 *    : // handle XML errors
 *} catch (IOException e) {
 *    ; // handle I/O errors
 *}</pre>
 *
 * <p>And it also provides a memory expensive but network efficient encoder,
 * using BufferedInputStream</p>
 *
 * <h3>Applications</h3>
 *
 * <p>The minimal Document Object Model (DOM) provided is the defacto standard
 * element tree found across all development environments.</p>
 *
 * <p>The XP implementation provided by the protected <code>XML.QP</code>
 * class provides an extensible type system for <code>Element</code> nodes
 * that support reuse of classes in the development of XML interpreters,
 * turning the DOM into an AST.</p>
 * 
 * <pre>
 * ...
 * </pre>
 *  
 * @author Laurent Szyster
 *
 */
public class XML {

    /**
     * An error class derived from <code>RuntimeException</code>, throwed
     * by the <code>QP</code> application of <code>XP</code> parser when
     * the XML processed is not-well formed.
     */
    public static class Error extends RuntimeException {
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
     * 
     * <h3>Synopsis</h3>
     *
     * <pre>...</pre>
     *
     */
    public static interface Type {
        public Element newElement (String name, HashMap attributes);
    }
     
    /**
     * A base class for all nodes in an XML element tree, with only five
     * properties, just enough to fully represent an XML string (without
     * comments) as a compact tree of Java objects with the smallest
     * memory footprint.
     * 
     * <h3>Synopsis</h3>
     * 
     * <p>...</p>
     * 
     */
    public static class Element {
        /**
         * This class <code>Type</code> singleton exposing this class
         * <code>newElement</code> method. 
         * 
         * Java sucks.
         */
        public static Type TYPE = null;
        /**
         * The fully qualified name of this element, either a simple XML
         * tag or the concatenation of a namespace and a tag joined by
         * a white space.
         */
        public String name = null;
        /**
         * A pointer to this element's parent while the tree built, or null
         * once this element's has been added to the tree.
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
         * Instanciate a new <code>Element</code>.
         * 
         * @param name the fully qualified name of this element
         * @param attributes a <code>HashMap</code> of named attributes
         */
        public Element (String name, HashMap attributes) {
            this.name = name;
            this.attributes = attributes;
            }
        /**
         * 
         * @param child
         */
        public void addChild (Element child) {
            if (children == null) 
                children = new ArrayList();
            children.add(child);
        }
        /**
         * 
         * @param cdata
         */
        public void addCdata (String cdata) {
            if (children==null)
                first = cdata;
            else
                getChild(children.size()-1).follow = cdata;
        }
        /**
         * A convenience to access child <code>Element</code> by index. 
         * 
         * @param index of the child in this element's children.
         * @return an <code>XML.Element</code> of null
         */
        public Element getChild (int index) {
            return (Element) children.get(index);
        }
        /**
         * A convenience to access a first child <code>Element</code> by name. 
         * 
         * @param name of the child.
         * @return an <code>XML.Element</code> or null
         */
        public Element getChild (String name) {
            Element child;
            for (int i=0, L=children.size(); i<L; i++) {
                child = (Element) children.get(i);
                if (child.name.equals(name)) 
                    return child;
            }
            return null;
        }
        
        protected class ChildrenIterator implements Iterator {
            private Iterator _children = null;
            private Element _next = null;
            private HashSet _names = null;
            public ChildrenIterator (Element element, HashSet names) {
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
         * <h4>Synopsis</h4>
         * 
         * <pre>Iterator named = element.getChildren(
         *    Simple.set({"a", "A"})
         *    )</pre>
         * 
         * @param name of the children.
         * @return an <code>Iterator</code> of <code>XML.Element</code>
         */
        public Iterator getChildren (HashSet names) {
            return new ChildrenIterator(this, names);
        }
        /**
         * A convenience to access an attribute <code>String</code> value
         * by name. 
         * 
         * @param name of the attribute
         * @return the value of the named attribute as a string
         */
        public String getAttribute (String name) {
            return (String) attributes.get(name);
        }
        /**
         * This is a method called by <code>QP</code> when an element has 
         * been parsed without errors, but before the circular reference
         * to it's parent is removed.
         * 
         * <p>The purpose of this interface is to allow derived classes of
         * <code>Element</code> to implement "Object Oriented Pull Parsers"
         * (OOPP). The benefit of this pattern is to avoid the need to walk
         * the element tree and instead process elements as they are
         * validated by the parser.</p>
         * 
         * @param doc the Document currently parsed
         */
        public void valid (Document doc) {;}
    }
    
    /**
     * A class with just enough properties to fully represent an XML
     * document with namespaces and processing instructions. 
     * 
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
        protected HashMap types = null;
        protected Element _curr = null;
        public QP (Document doc, HashMap types) {
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
        protected static String fqn (String name, HashMap ns) 
        throws Error {
            int colon = name.indexOf(':');
            if (colon > -1) {
                String prefix = name.substring(0,colon);
                if (prefix.equals(_xml))
                    return name.substring(colon+1);
                else if (ns.containsKey(prefix))
                    return (
                        ((String)ns.get(prefix)) 
                        + ' ' + name.substring(colon+1)
                        );
                else
                    throw new Error(_namespace_prefix_not_found);
            } else if (ns.containsKey(_no_prefix))
                return (
                    ((String)ns.get(_no_prefix)) 
                    + ' ' + name.substring(colon+1)
                    );
            else
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
                    if (name.equals(_xmlns))
                        doc.ns.put("", event.getAttributeValue(i));
                    else if (name.startsWith(_xmlns_colon))
                        doc.ns.put(
                            name.substring(6), event.getAttributeValue(i)
                            );
                    else {
                        attributeNames[i] = name; 
                        A++;
                    }
                }
                if (A > 0) {
                    attributes = new HashMap ();
                    for (int i=0; i<L; i++) 
                        if (attributeNames[i]!=null)
                            attributes.put(
                                fqn(attributeNames[i], doc.ns), 
                                event.getAttributeValue(i)
                                );
                }
            }
            name = fqn(event.getName(), doc.ns);
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
        public void endElement(EndElementEvent event) {
            _curr.valid(doc);
            Element parent = _curr.parent;
            _curr.parent = null; // break circular reference!
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
     * Try to parse an XML <code>InputStream</code> with path and base URL 
     * using the extension <code>types</code> and an <code>XML.Document</code>. 
     * 
     * @param is InputStream to parse
     * @param types to use as extensions
     * @return a <code>XML.Document</code>
     * @throws Error if the XML file is not well-formed or if one of the
     *               extension type is broken
     * @throws IOException raised by accessing the XML file
     */
    public static final Document read(
        InputStream is, String path, URL baseURL, HashMap types, Document doc
        ) throws Error, IOException {
        QP qp = new QP(doc, types);
        try {
            DocumentParser.parse(new OpenEntity(
                is, path, baseURL
                ), new EntityManagerImpl(), qp, Locale.US);
        } catch (ApplicationException e) {
            Element parent = qp._curr.parent;
            while (parent != null) {
                qp._curr.parent = null;
                qp._curr = parent;
                parent = qp._curr.parent;
            }
            throw new Error(e.getMessage());
        }
        return doc;
    }
    
    /**
     * Try to parse an XML <code>file</code> using the extension 
     * <code>types</code> and an <code>XML.Document</code>. 
     * 
     * @param file to parse
     * @param types to use as extensions
     * @return a <code>XML.Document</code>
     * @throws Error if the XML file is not well-formed or if one of the
     *               extension type is broken
     * @throws IOException raised by accessing the XML file
     */
    public static final Document read(File file, HashMap types, Document doc) 
    throws Error, IOException {
        return read (
            new FileInputStream(file), file.getAbsolutePath(), file.toURL(),
            types, doc
            );
    }
    
    /**
     * Try to parse an XML <code>file</code> using extension 
     * <code>types</code> and return a new <code>XML.Document</code>. 
     * 
     * @param file to parse
     * @param types to use as extensions
     * @return a <code>XML.Document</code>
     * @throws Error if the XML file is not well-formed or if one of the
     *               extension type is broken
     * @throws IOException raised by accessing the XML file
     */
    public static final Document read(File file, HashMap types) 
    throws Error, IOException {
        return read(
            new FileInputStream(file), file.getAbsolutePath(), file.toURL(),
            types, new Document()
            );
    }
    
    /**
     * Try to parse an XML <code>file</code> return a new 
     * <code>XML.Document</code>. 
     * 
     * @param file to parse
     * @return a <code>XML.Document</code>
     * @throws Error if the XML file is not well-formed
     * @throws IOException raised by accessing the XML file
     */
    public static final Document read(File file) 
    throws Error, IOException {
        return read(
            new FileInputStream(file), file.getAbsolutePath(), file.toURL(),
            null, new Document()
            );
    }
    /**
     * 
     * @param os
     * @param element
     */
    public static final void writeUTF8 (
        OutputStream os, Document document, Element element
        )
    throws IOException {
        ;
    }
    /**
     * 
     * @param os
     * @param document
     */
    public static final void writeUTF8 (OutputStream os, Document document)
    throws IOException {
        ;
    }
    /**
     * 
     * @param file
     * @param document
     */
    public static final void writeUTF8 (File file, Document document) 
    throws IOException {
        writeUTF8(new FileOutputStream(file), document);
    }
    // Note that using an unbuffered OutputStream will induce the worse 
    // possible network performances: globbing is good. The alternative
    // however is practical to write long XML documents.
    //
    // Encoding 64KB chunks at once is better in most J2EE container ... 
    // but only where there's less Java and more memory for applications ;-)
    public static final Document decode (byte[] body) {
        return null;
    }
}
