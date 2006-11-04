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

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

/**
 * Static classes and methods to convert between JSON string and the
 * Java types: HashMap, ArrayList, String, Double, Boolean and null.
 * 
 * Note that JSON strings are expected to be complete, this is a
 * simple recursive-end-tail interpreter, not a validator. This
 * would require a stack and is only usefull to parse a JSON body
 * before they are completed, an unlikely application case in a
 * synchronized web controller for some entreprise database.
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
     * will throw a JSON.SyntaxError. This is a "zero-tolerance" JSON syntax
     * interpreter, with support for allmost nothing else than the bare 
     * standard.</p>
     * 
     * <p>Namely, it is possible to evaluate any JSON value, not just a
     * payload.</p>
     * 
     * @author Laurent Szyster
     */
    public static class Interpreter {
        
        public char c;
        public CharacterIterator it;
        public StringBuffer buf = new StringBuffer();
        
        private static final char DONE = CharacterIterator.DONE;
        
        public Object eval(String json) throws SyntaxError {
            it = new StringCharacterIterator(json);
            c = it.first();
            if (c == DONE)
                return null;
            else
                return value();
        }
        
        private SyntaxError error(String message) {
            StringBuffer sb = new StringBuffer();
            sb.append(it.getIndex());
            sb.append(message);
            sb.append(c);
            return new SyntaxError(sb.toString());
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
                    throw error(" 'true' expected ");
            }
            case 'f': {
                if (next('a') && next('l') && next('s') && next('e')) {
                    c = it.next(); return Boolean.FALSE;
                } else
                    throw error(" 'false' expected ");
            }
            case 'n': {
                if (next('u') && next('l') && next('l')) {
                    c = it.next(); return null;
                } else
                    throw error(" 'null' expected ");
            }
            case ',': {c = it.next(); return COMMA;} 
            case ':': {c = it.next(); return COLON;}
            case ']': {c = it.next(); return ARRAY;} 
            case '}': {c = it.next(); return OBJECT;}
            case DONE:
                throw error("unexpected end ");
            default: 
                throw error(" unexpected character ");
            }
        }
        
        private Object object() throws SyntaxError {
            HashMap map = new HashMap();
            Object token = value();
            Object key, val;
            while (token != OBJECT) {
                if (!(token instanceof String) || 
                    token==COLON || token==COMMA || token==ARRAY
                    )
                    throw error(" string expected ");
                key = token;
                token = value(); 
                if (token == COLON) {
                    val = value();
                    if (val==COLON || val==COMMA || val==OBJECT || val==ARRAY)
                        throw error(" value expected ");
                    map.put(key, val);
                    token = value();
                    if (token == COMMA)
                        token = value();
                }
            }
            return map;
        }
        
        private Object array() throws SyntaxError {
            ArrayList list = new ArrayList();
            Object token = value();
            while (token != ARRAY) {
                if (token==COLON || token==COMMA || token==OBJECT)
                    throw error(" value expected ");
                list.add(token);
                token = value(); 
                if (token == COMMA) 
                    token = value();
            }
            return list;
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
        
        private Object string() throws SyntaxError {
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
                                throw error(" illegal escape sequence ");
                        }
                    }
                } else if (c == DONE) {
                    throw error(" unexpected end ");
                } else {
                    buf.append(c); 
                }
                c = it.next();
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
                case DONE:
                    throw error(" unexpected end ");
                default:
                    throw error(" illegal UNICODE sequence ");
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
    
    private static final String _quote = "\\\"";
    private static final String _back = "\\\\";
    private static final String _slash = "\\/";
    private static final String _ctrl_b = "\\/";
    private static final String _ctrl_f = "\\/";
    private static final String _ctrl_n = "\\/";
    private static final String _ctrl_r = "\\/";
    private static final String _ctrl_t = "\\/";
    
    public static void encode(StringBuffer sb, String s) {
        sb.append('"');
        CharacterIterator it = new StringCharacterIterator(s);
        for (
            char c = it.first(); 
            c != CharacterIterator.DONE; 
            c = it.next()
            ) {
            if (c == '"') sb.append(_quote);
            else if (c == '\\') sb.append(_back);
            else if (c == '/') sb.append(_slash);
            else if (c == '\b') sb.append(_ctrl_b);
            else if (c == '\f') sb.append(_ctrl_f);
            else if (c == '\n') sb.append(_ctrl_n);
            else if (c == '\r') sb.append(_ctrl_r);
            else if (c == '\t') sb.append(_ctrl_t);
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
    
    public String string;
    
    public JSON(String literal) {this.string = literal;}
    
    public JSON(Object value) {this.string = encode(value);}
        
}
