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

package org.less4j; // less java for fore applications

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

/**
 * <p>A convenience class with static methods to serialize java objects as
 * JSON and to evaluate a strict JSON expression as a limited tree of the 
 * six Java types
 * 
 * <blockquote>
 * <code>HashMap</code>, 
 * <code>ArrayList</code>, 
 * <code>String</code>, 
 * <code>Double</code>, 
 * <code>Boolean</code>, 
 * <code>null</code>
 * </blockquote>
 * 
 * enforcing safety limits on the number of containers and iterations 
 * evaluated, protecting the J2EE container JVM, CPU and RAM from
 * malicious JSON input.</p>
 * 
 * <h3>Evaluate Safely</h3>
 * 
 * <p>To evaluate and validate a JSON string as a single object, without
 * depth and with at most 25 simple properties, do:
 * 
 * <blockquote>
 * <pre>HashMap object = JSON.object("{...}", 1, 25);</pre>
 * </blockquote>
 * 
 * To evaluate and validate a JSON string as a single array, without depth 
 * and with at most 50 items, do:
 * 
 * <blockquote>
 * <pre>HashMap object = JSON.array("[...]", 1, 50);</pre>
 * </blockquote>
 * 
 * To evaluate and validate a JSON string as an single array of at most
 * 5 records containing an average of 10 fields, do:
 * 
 * <blockquote>
 * <pre>HashMap object = JSON.array("[...]", 6, 55);</pre>
 * </blockquote>
 * 
 * Of course this would also validate one object with five arrays of 
 * different sizes, for instance 6, 12, 3, 19 and 8. The purpose is not
 * to validate complex structure but to prevent abuse of a network interface
 * that could otherwise instanciate thousands of instances from a 16KB JSON 
 * string, straining the JVM stack, the garbage collector and the J2EE 
 * container most precious resources: memory!</p>
 * 
 * <p>Note that this is a simple recursive-end-tail interpreter, not a stream
 * parser. JSON strings are expected to be complete, which is the desired
 * use case of JSON in the synchronized web controller of some entreprise 
 * database application.</p>
 * 
 * <h3>JSON Serialization</h3>
 * 
 * <p>...</p>
 * 
 * <h3>A Simple Convenience</h3>
 * 
 * <p>...
 * 
 * <blockquote>
 * <pre>JSON serialized = JSON("[1,2,3,{},\"...\"]");</pre>
 * </blockquote>
 * 
 * or
 * 
 * <blockquote>
 * <pre>ArrayList list = new ArrayList();
 *list.add(1); list.add(2); list.add(3);
 *list.add(new HashMap()); list.add("...");
 *JSON serialized = JSON(list);</pre>
 * </blockquote>
 * 
 * ...
 * 
 * <blockquote>
 * <pre>HashMap object = new HashMap;
 *object.put("name", serialized);
 *JSON.repr(object);</pre>
 * </blockquote>
 * 
 * ...</p>
 * 
 * <p><b>Copyright</b> &copy; 2006 Laurent A.V. Szyster</p>
 * 
 * @author Laurent Szyster
 * @version 0.1.0
 */
public class JSON {
    
    protected static final char DONE = CharacterIterator.DONE;
    
    public static class Error extends Exception {
        private static final long serialVersionUID = 0L;
        public Error(String message) {super(message);}
    }
    
    protected static class Interpreter {
        
        public int containers;
        public int iterations;
        public char c;
        public CharacterIterator it;
        public StringBuffer buf;
        
        protected Interpreter(
            int containers, int iterations
            ) {
            this.containers = (containers > 0 ? containers: 1);
            this.iterations = (iterations > 0 ? iterations: 1);
        }
        
        public Object eval(String json) throws Error {
            buf = new StringBuffer();
            it = new StringCharacterIterator(json);
            try {
                c = it.first();
                if (c == DONE)
                    return null;
                else
                    return value();
            } finally {
                buf = null;
                it = null;
            }
        }
        
        public Error error(String message) {
            StringBuffer sb = new StringBuffer();
            sb.append(it.getIndex());
            sb.append(message);
            return new Error(sb.toString());
        }
        
        public Error error(String message, char arg) {
            StringBuffer sb = new StringBuffer();
            sb.append(it.getIndex());
            sb.append(message);
            sb.append(arg);
            return new Error(sb.toString());
        }
        
        public boolean next(char test) {
            c = it.next();
            return c == test;
        }
        
        public static final Object OBJECT = new Object();
        public static final Object ARRAY = new Object();
        public static final Object COLON = new Object();
        public static final Object COMMA = new Object();
        
        public Object value() throws Error {
            while (Character.isWhitespace(c)) c = it.next();
            switch(c){
            case '{': {c = it.next(); return object();}
            case '[': {c = it.next(); return array();}
            case '"': {c = it.next(); return string();}
            case '0': case '1': case '2': case '3': case '4':  
            case '5': case '6': case '7': case '8': case '9': 
            case '-': {
                return number();
                }
            case 't': {
                if (next('r') && next('u') && next('e')) {
                    c = it.next(); return Boolean.TRUE;
                } else
                    throw error(" 'true' expected ", c);
            }
            case 'f': {
                if (next('a') && next('l') && next('s') && next('e')) {
                    c = it.next(); return Boolean.FALSE;
                } else
                    throw error(" 'false' expected ", c);
            }
            case 'n': {
                if (next('u') && next('l') && next('l')) {
                    c = it.next(); return null;
                } else
                    throw error(" 'null' expected ", c);
            }
            case ',': {c = it.next(); return COMMA;} 
            case ':': {c = it.next(); return COLON;}
            case ']': {c = it.next(); return ARRAY;} 
            case '}': {c = it.next(); return OBJECT;}
            case DONE:
                throw error(" unexpected end");
            default: 
                throw error(" unexpected character ", c);
            }
        }
        
        public Object object() throws Error {
            String key; 
            Object val;
            if (--containers < 0) 
                throw error(" containers overflow");
            
            HashMap map = new HashMap();
            Object token = value();
            while (token != OBJECT) {
                if (!(token instanceof String))
                    throw error(" string expected ");
                
                if (--iterations < 0) 
                    throw error(" iterations overflow");
                
                key = (String) token;
                if (value() == COLON) {
                    val = value();
                    if (val==COLON || val==COMMA || val==OBJECT || val==ARRAY)
                        throw error(" value expected");
                    
                    map.put(key, val);
                    token = value();
                    if (token == COMMA)
                        token = value();
                } else {
                    throw error(" colon expected ", c);
                }
            }
            return map;
        }
        
        public Object array() throws Error {
            if (--containers < 0) 
                throw error(" containers overflow");
            
            ArrayList list = new ArrayList();
            Object token = value();
            while (token != ARRAY) {
                if (token==COLON || token==COMMA || token==OBJECT)
                    throw error(" value expected");
                
                if (--iterations < 0) 
                    throw error(" iterations overflow");
                
                list.add(token);
                token = value(); 
                if (token == COMMA) 
                    token = value();
            }
            return list;
        }
        
        public Object number() {
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
        
        public Object string() throws Error {
            buf.setLength(0);
            while (c != '"') {
                if (c == '\\') {
                    c = it.next(); 
                    if (c == 'u') {
                        buf.append(unicode()); 
                    } else {
                        switch(c) {
                            case '\\': buf.append('\\'); break;
                            case '"': buf.append('"'); break;
                            case '/': buf.append('/'); break;
                            case 'b': buf.append('\b'); break;
                            case 'f': buf.append('\f'); break;
                            case 'n': buf.append('\n'); break;
                            case 'r': buf.append('\r'); break;
                            case 't': buf.append('\t'); break;
                            default: 
                                throw error(" illegal escape sequence ", c);
                        }
                    }
                } else if (c == DONE) {
                    throw error(" unexpected end");
                } else {
                    buf.append(c); 
                }
                c = it.next();
            }
            c = it.next();
            return buf.toString();
        }
        
        public void digits() {
            while (Character.isDigit(c)) {buf.append(c); c = it.next();}
        }
        
        public char unicode() throws Error {
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
                case DONE:
                    throw error(" unexpected end");
                default:
                    throw error(" illegal UNICODE sequence");
                }
            }
            return (char) val;
        }
    }

    public static Object eval(String json) throws Error {
        return (new Interpreter(65355, 65355)).eval(json);
    }
    
    public static Object eval(
        String json, int containers, int iterations
        ) throws Error {
        return (new Interpreter(containers, iterations)).eval(json);
    }
    
    public static Object object(
        String json, int containers, int iterations
        ) throws Error {
        Object o = eval(json, containers, iterations);
        if ((o instanceof HashMap)) 
            return (HashMap) o;
        else
            throw new Error("type error");
        }
        
    public static Object array(
        String json, int containers, int iterations
        ) throws Error {
        Object o = eval(json, containers, iterations);
        if ((o instanceof ArrayList)) 
            return (ArrayList) o;
        else
            throw new Error("type error");
    }
            
    private static final String _quote = "\\\"";
    private static final String _back = "\\\\";
    private static final String _slash = "\\/";
    private static final String _ctrl_b = "\\b";
    private static final String _ctrl_f = "\\f";
    private static final String _ctrl_n = "\\n";
    private static final String _ctrl_r = "\\r";
    private static final String _ctrl_t = "\\t";
    
    public static void repr(StringBuffer sb, String s) {
        sb.append('"');
        CharacterIterator it = new StringCharacterIterator(s);
        for (char c = it.first(); c != DONE; c = it.next()) {
            switch(c) {
            case '"':  sb.append(_quote); break;
            case '\\': sb.append(_back); break;
            case '/': sb.append(_slash); break;
            case '\b': sb.append(_ctrl_b); break;
            case '\f': sb.append(_ctrl_f); break;
            case '\n': sb.append(_ctrl_n); break;
            case '\r': sb.append(_ctrl_r); break;
            case '\t': sb.append(_ctrl_t); break;
            default: 
                if (Character.isISOControl(c))
                    unicode(sb, c);
                else
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
    
    public static void repr(StringBuffer sb, Map map) {
        Object key; 
        Iterator it = map.keySet().iterator();
        if (!it.hasNext()) {
            sb.append(_object);
            return;
        }
        sb.append('{');
        key = it.next();
        repr(sb, key);
        sb.append(':');
        repr(sb, map.get(key));
        while (it.hasNext()) {
            sb.append(',');
            key = it.next();
            repr(sb, key);
            sb.append(':');
            repr(sb, map.get(key));
        }
        sb.append('}');
    }
    
    public static void repr(StringBuffer sb, Iterator it) {
        if (!it.hasNext()) {
            sb.append(_list);
            return;
        }
        sb.append('[');
        repr(sb, it.next());
        while (it.hasNext()) {
            sb.append(',');
            repr(sb, it.next());
        }
        sb.append(']');
    }
    
    public static void repr(StringBuffer sb, Object value) {
        if (value == null) 
            sb.append(_null);
        else if (value instanceof Boolean)
            sb.append((((Boolean) value).booleanValue() ? _true : _false));
        else if (value instanceof Number) 
            sb.append(value);
        else if (value instanceof String) 
            repr(sb, (String) value);
        else if (value instanceof Character) 
            repr(sb, ((Character) value).toString());
        else if (value instanceof Map)
            repr(sb, (Map) value);
        else if (value instanceof List) 
            repr(sb, ((List) value).iterator());
        else if (value instanceof JSON) 
            repr(sb, ((JSON) value).string);
        else 
            repr(sb, value.toString());
    }
    
    public static String repr(Object value) {
        StringBuffer sb = new StringBuffer();
        repr(sb, value);
        return sb.toString();
    }
    
    public String string;
    
    public JSON(String literal) {this.string = literal;}
    
    public JSON(Object value) {this.string = repr(value);}
    
}

