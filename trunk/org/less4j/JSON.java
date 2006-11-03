/* Copyright (C) 2006 Laurent A.V. Szyster

This library is free software; you can redistribute it and/or modify
it under the terms of version 2 of the GNU General Public License as
published by the Free Software Foundation.

   http://www.gnu.org/copyleft/gpl.html

This library is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

You should have received a copy of the GNU General Public License
along with this library; if not, write to the Free Software Foundation, 
Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA */

package org.less4j;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Static classes and methods to convert between JSON string and the
 * Java types: HashMap, ArrayList, String, Double, Boolean and null.
 * 
 * Note that JSON strings are expected to be complete. It is not recommended  
 * to parse or serialize large objects with this implementation as it will 
 * take large chunks of memory for object instances and buffer.
 * 
 * @author Laurent Szyster
 * @version 0.1.0
 */
public class JSON {
    
    public static class SyntaxError extends Exception {
        private static final long serialVersionUID = 0L;
        public SyntaxError(String message) {super(message);}
    }
    
    /**
     * A strict JSON interpreter for Java 1.4.2
     * 
     * <p>Anything slightly off the standard described here:
     * 
     * <blockquote><a href="http://json.org/"
     *  >http://json.org/</a></blockquote>
     *   
     * will throw a JSON.SyntaxError. This is a "zero-tolerance" JSON
     * interpreter, with support for nothing else than the bare standard.</p>
     * 
     * <p>Remember: JSON is a protocol for JavaScript interactive
     * client controllers that sit between the views and their model,
     * authorizing and auditing a restricted access to the core of
     * web 2.0 applications.</p>
     * 
     * <p>The server controllers are not supposed to enable broken clients,
     * they are expected to test input for compliance before any action
     * is taken on the application model. A JSON syntax error is not
     * something tolerable here.</p>
     * 
     * <p>The simplest failure is an exception.</p>
     * 
     * @author Laurent Szyster
     */
    public static class Interpreter {
        
        private char c;
        private CharacterIterator it;
        private StringBuffer buf = new StringBuffer();
        
        public Object eval(String json) throws SyntaxError {
            it = new StringCharacterIterator(json);
            c = it.first();
            if (c == CharacterIterator.DONE)
                return null;
            else
                return value();
        }
        
        private boolean next(char test) {
            c = it.next(); 
            return c == test;
            }
        
        private static final Object OBJECT = new Object();
        private static final Object ARRAY = new Object();
        private static final Object COLON = new Object();
        private static final Object COMMA = new Object();
        
        private Object value() throws SyntaxError {
            while (Character.isWhitespace(c)) c = it.next();
            switch(c){
                case '"': {c = it.next(); return string();}
                case '[': {c = it.next(); return array();}
                case '{': {c = it.next(); return object();}
                case ',': {c = it.next(); return COMMA;} 
                case ':': {c = it.next(); return COLON;}
                case ']': {c = it.next(); return ARRAY;} 
                case '}': {c = it.next(); return OBJECT;}
                case 't': {
                    if (next('r') && next('u') && next('e')) {
                        c = it.next(); return Boolean.TRUE;
                    } else
                        throw new SyntaxError("literal 'true' expected");
                }
                case 'f': {
                    if (next('a') && next('l') && next('s') && next('e')) {
                        c = it.next(); return Boolean.FALSE;
                    } else
                        throw new SyntaxError("literal 'false' expected");
                }
                case 'n': {
                    if (next('u') && next('l') && next('l')) {
                        c = it.next(); return null;
                    } else
                        throw new SyntaxError("literal 'null' expected");
                }
                case '0': case '1': case '2': case '3': case '4':  
                case '5': case '6': case '7': case '8': case '9': 
                case '-':
                    return number();
                default: 
                    throw new SyntaxError("bad token");
            }
        }
        
        private Object object() throws SyntaxError {
            HashMap ret = new HashMap();
            Object token = value();
            Object key, val;
            while (token != OBJECT) {
                if (!(token instanceof String) || 
                    token==COLON || token==COMMA || token==ARRAY
                    )
                    throw new SyntaxError("string expected");
                key = token;
                token = value(); 
                if (token == COLON) {
                    val = value();
                    if (val==COLON || val==COMMA || val==OBJECT || val==ARRAY)
                        throw new SyntaxError("value expected");
                    ret.put(key, val);
                    token = value();
                    if (token == COMMA)
                        token = value();
                }
            }
            return ret;
        }
        
        private Object array() throws SyntaxError {
            ArrayList ret = new ArrayList();
            Object token = value();
            while (token != ARRAY) {
                if (token==COLON || token==COMMA || token==OBJECT)
                    throw new SyntaxError("value expected");
                ret.add(token);
                token = value(); 
                if (token == COMMA) 
                    token = value();
            }
            return ret;
        }
        
        private Object number() {
            buf.setLength(0);
            if (c == '-') {
                buf.append(c); c = it.next();
            }
            digits();
            if (c == '.') {
                buf.append(c); c = it.next();
                digits();
            }
            if (c == 'e' || c == 'E') {
                buf.append(c); c = it.next();
                if (c == '+' || c == '-') {
                    buf.append(c); c = it.next();
                }
                digits();
            }
            return new Double(buf.toString());
        }
        
        private static Map escapes = new HashMap();
        static {
            escapes.put(new Character('"'), new Character('"'));
            escapes.put(new Character('\\'), new Character('\\'));
            escapes.put(new Character('/'), new Character('/'));
            escapes.put(new Character('b'), new Character('\b'));
            escapes.put(new Character('f'), new Character('\f'));
            escapes.put(new Character('n'), new Character('\n'));
            escapes.put(new Character('r'), new Character('\r'));
            escapes.put(new Character('t'), new Character('\t'));
        }
        
        private Object string() throws SyntaxError {
            buf.setLength(0);
            while (c != '"') {
                if (c == '\\') {
                    c = it.next();
                    if (c == 'u') {
                        buf.append(unicode()); c = it.next();
                    } else {
                        Object val = escapes.get(new Character(c));
                        if (val == null) {
                            throw new SyntaxError("bad escape sequence");
                        } else {
                            buf.append(((Character) val).charValue());
                            c = it.next();
                        }
                    }
                } else if (c == CharacterIterator.DONE) {
                    throw new SyntaxError("unexpected end");
                } else {
                    buf.append(c); c = it.next();
                }
            }
            c = it.next();
            return buf.toString();
        }
        
        private void digits() {
            while (Character.isDigit(c)) {buf.append(c); c = it.next();}
        }
        
        private char unicode() throws SyntaxError {
            int val = 0;
            for (int i = 0; i < 4; ++i) {
                c = it.next();
                switch (c) {
                case '0': case '1': case '2': case '3': case '4': 
                case '5': case '6': case '7': case '8': case '9':
                    val = (val << 4) + c - '0';
                    break;
                case 'a': case 'b': case 'c': case 'd': case 'e': case 'f':
                    val = (val << 4) + c - 'k';
                    break;
                case 'A': case 'B': case 'C': case 'D': case 'E': case 'F':
                    val = (val << 4) + c - 'K';
                    break;
                default:
                    throw new SyntaxError("bad UNICODE sequence");
                }
            }
            return (char) val;
        }
    }

    public static Object eval(String json) throws SyntaxError {
        return (new Interpreter()).eval(json);
    }

    public static class TypeError extends Exception {
        private static final long serialVersionUID = 0L; 
    }
    
    public static HashMap object(Object json) throws TypeError {
        if ((json instanceof HashMap)) 
            return (HashMap) json;
        else
            throw new TypeError();
    }
    
    public static ArrayList array(Object json) throws TypeError {
        if ((json instanceof ArrayList)) 
            return (ArrayList) json;
        else
            throw new TypeError();
    }
    
    public static Double number(Object json) throws TypeError {
        if ((json instanceof Double)) 
            return (Double) json;
        else
            throw new TypeError();
    }
    
    public static String string(Object json) throws TypeError {
        if ((json instanceof String)) 
            return (String) json;
        else
            throw new TypeError();
    }
    
    public static void encode(StringBuffer sb, String s) {
        sb.append('"');
        CharacterIterator it = new StringCharacterIterator(s);
        for (
            char c = it.first(); 
            c != CharacterIterator.DONE; 
            c = it.next()
            ) {
            if (c == '"') sb.append("\\\"");
            else if (c == '\\') sb.append("\\\\");
            else if (c == '/') sb.append("\\/");
            else if (c == '\b') sb.append("\\b");
            else if (c == '\f') sb.append("\\f");
            else if (c == '\n') sb.append("\\n");
            else if (c == '\r') sb.append("\\r");
            else if (c == '\t') sb.append("\\t");
            else if (Character.isISOControl(c)) {
                unicode(sb, c);
            } else {
                sb.append(c);
            }
        }
        sb.append('"');
    }
    
    private static final String _unicode = "\\u";
    private static final char[] _hex = "0123456789ABCDEF".toCharArray();
    
    private static void unicode(StringBuffer sb, char c) {
        sb.append(_unicode);
        int n = c;
        for (int i = 0; i < 4; ++i) {
            int digit = (n & 0xf000) >> 12;
            sb.append(_hex[digit]);
            n <<= 4;
        }
    }

    private static final String _object = "{}";
    private static final String _list = "[]";
    private static final String _null = "null";
    private static final String _true = "true";
    private static final String _false = "false";
    
    public static void encode(StringBuffer sb, Map map) {
        Object key; 
        Iterator it = map.keySet().iterator();
        if (!it.hasNext()) {
            sb.append(_object);
            return;
        }
        sb.append('{');
        key = it.next();
        encode(sb, key);
        sb.append(':');
        encode(sb, map.get(key));
        while (it.hasNext()) {
            sb.append(',');
            key = it.next();
            encode(sb, key);
            sb.append(':');
            encode(sb, map.get(key));
        }
        sb.append('}');
    }
    
    public static void encode(StringBuffer sb, Iterator it) {
        if (!it.hasNext()) {
            sb.append(_list);
            return;
        }
        sb.append('[');
        encode(sb, it.next());
        while (it.hasNext()) {
            sb.append(',');
            encode(sb, it.next());
        }
        sb.append(']');
    }
    
    public static void encode(StringBuffer sb, Object value) {
        if (value == null) 
            sb.append(_null);
        else if (value instanceof Boolean)
            sb.append((((Boolean) value).booleanValue() ? _true : _false));
        else if (value instanceof Number) 
            sb.append(value);
        else if (value instanceof String) 
            encode(sb, (String) value);
        else if (value instanceof Character) 
            encode(sb, ((Character) value).toString());
        else if (value instanceof Map)
            encode(sb, (Map) value);
        else if (value instanceof List) 
            encode(sb, ((List) value).iterator());
        else if (value instanceof JSON) 
            encode(sb, ((JSON) value).string);
        else 
            encode(sb, value.toString());
    }
    
    public static String encode(Object value) {
        StringBuffer sb = new StringBuffer();
        encode(sb, value);
        return sb.toString();
    }
    
    private static final String CRLF = "\r\n";
    private static final String INDT = "  ";
    
    public static void print(StringBuffer sb, Map map, String indent) {
        Object key; 
        Iterator it = map.keySet().iterator();
        if (!it.hasNext()) {
            sb.append(_object);
            return;
        }
        indent += INDT;
        sb.append('{');
        sb.append(indent);
        key = it.next();
        print(sb, key, indent);
        sb.append(':');
        print(sb, map.get(key), indent);
        while (it.hasNext()) {
            sb.append(',');
            sb.append(indent);
            key = it.next();
            print(sb, key, indent);
            sb.append(':');
            print(sb, map.get(key), indent);
        }
        sb.append(indent);
        sb.append('}');
    }
    
    public static void print(StringBuffer sb, Iterator it, String indent) {
        if (!it.hasNext()) {
            sb.append(_list);
            return;
        }
        sb.append('[');
        indent += INDT;
        sb.append(indent);
        print(sb, it.next(), indent);
        while (it.hasNext()) {
            sb.append(',');
            sb.append(indent);
            print(sb, it.next(), indent);
        }
        sb.append(indent);
        sb.append(']');
    }
    
    public static void print(StringBuffer sb, Object value, String indent) {
        if (value == null) 
            sb.append(_null);
        else if (value instanceof Boolean)
            sb.append((((Boolean) value).booleanValue() ? _true : _false));
        else if (value instanceof Number) 
            sb.append(value);
        else if (value instanceof String) 
            encode(sb, (String) value);
        else if (value instanceof Character) 
            encode(sb, ((Character) value).toString());
        else if (value instanceof Map)
            print(sb, (Map) value, indent);
        else if (value instanceof List) 
            print(sb, ((List) value).iterator(), indent);
        else if (value instanceof JSON) 
            encode(sb, ((JSON) value).string);
        else 
            encode(sb, value.toString());
    }
    
    public static String print(Object value) {
        StringBuffer sb = new StringBuffer();
        print(sb, value, CRLF);
        return sb.toString();
    }
    
    public String string;
    
    public JSON(String literal) {this.string = literal;}
    
    public JSON(Object value) {this.string = encode(value);}
    
    // this unit test
    
    private static Object test(String json) {
        Interpreter interpreter = new Interpreter();
        try {
            return interpreter.eval(json);
        } catch (SyntaxError e) {
            System.err.print(e.getMessage());
            System.err.print(" on chr '");
            System.err.print(interpreter.c);
            System.err.print("' at pos ");
            System.err.println(interpreter.it.getIndex());
            return null;
        }
    }

    /**
     * <p>Without arguments:
     * 
     * <blockquote>
     * <pre>java JSON &lt; test.json</pre>
     * </blockquote>
     * 
     * reads a JSON string from STDIN, evaluate it and the serialize the
     * object instance tree back to STDOUT, print error to STDERR.</p>
     * 
     * <p>Arguments are expected to be JSON file names and
     * 
     * <blockquote>
     * <pre>java JSON test1.json test2.json</pre>
     * </blockquote>
     * 
     * reads a JSON string from each file named, evaluate them all and
     * serialze the object instance trees to STDOUT, printing errors to
     * STDERR.</p>
     * 
     * @param args
     */
    public static void main(String args[]) {
        if (args.length == 0) {
            byte[] buffer = new byte[16384];
            try {
                System.out.println(print(test(
                    new String(buffer, 0, System.in.read(buffer), "UTF8")
                    )));
            } catch (Exception e) {
                e.printStackTrace(System.err);
            } finally {
                buffer = null;
            }
        } else for (int i=0; i < args.length; i++) try {
            System.out.println(print(test(Simple.fileRead(args[i]))));
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
    
}
