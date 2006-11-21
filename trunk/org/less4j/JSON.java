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

package org.less4j; // less java for more applications

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

/**
 * <p>A convenience with static methods to serialize java objects as JSON
 * strings and to evaluate a strict JSON expression as a limited
 * tree of the seven Java types
 * 
 * <blockquote>
 * <code>HashMap</code>, 
 * <code>ArrayList</code>, 
 * <code>String</code>, 
 * <code>Double</code>, 
 * <code>BigDecimal</code>, 
 * <code>BigInteger</code>, 
 * <code>Boolean</code>
 * </blockquote>
 * 
 * and the untyped <code>null</code> value.</p>
 * 
 * <h3>Synopsis</h3>
 * 
 * <p>The JSON static methods allow to evaluate strings as an untyped
 * <code>Object</code> or validate and cast them as an <code>ArrayList</code>
 * or a <code>HashMap</code>, for JSON arrays and objects respectively:
 * 
 * <blockquote>
 * <pre>try {
 *    Object value = JSON.eval("null");
 *    HashMap map = JSON.object("{}");
 *    ArrayList list = JSON.array("[1,2,3]");
 *} catch (JSON.Error e) {
 *    System.out.println(e.getMessage())
 *}</pre>
 * </blockquote>
 * 
 * Lower limits than the defaults maximum of 65355 can be set for the
 * number of objects and arrays and the count of values, for containers
 * and iterations:
 * 
 * <blockquote>
 * <pre>try {
 *    ArrayList list = JSON.array("[{},{},{},{},{},{}]", 1, 4);
 *    HashMap map = JSON.object("[{},{},{},{},{},{}]", 1, 100);
 *} catch (JSON.Error e) {
 *    System.out.println(e.getMessage())
 *}</pre>
 * </blockquote>
 * 
 * making JSON evaluation safe for public interfaces.</p>
 * 
 * <p>There are also static methods to serialize java instances as
 * JSON strings
 * 
 * <blockquote>
 * <pre>System.out.println(JSON.str(value));
 *System.out.println(JSON.str(map));
 *System.out.println(JSON.str(list));</pre>
 * </blockquote>
 * 
 * to append distinct values into a <code>StringBuffer</code>
 * 
 * <blockquote>
 * <pre>StringBuffer sb = new StringBuffer();
 *sb.append("{\"size\":");
 *JSON.strb(sb, value);
 *sb.append(",\"map\": ");
 *JSON.strb(sb, map);
 *sb.append(",\"list\": ");
 *JSON.strb(sb, list.iterator());
 *sb.append("}");
 *System.out.println(sb.toString());</pre>
 *</blockquote>
 *
 * using templates for constants, or to pretty print an indented
 * representation of a java instance in JSON 
 * 
 * <blockquote>
 * <pre>System.out.println(JSON.repr(value));
 *System.out.println(JSON.repr(map));
 *System.out.println(JSON.repr(list));</pre>
 * </blockquote>
 * 
 * Instances of JSON class itself are made to serialize partial instance 
 * tree and assemble reponses from java objects and JSON strings:
 * 
 * <blockquote>
 * <pre>try {
 *    HashMap map = JSON.object("{}");
 *    map.put("list", JSON.array("[1,2,3]");
 *    json = new JSON();
 *    json.string = "{\"constant\": null"}";
 *    map.put("value", json);
 *    System.out.println(JSON.str(map));
 *} catch (JSON.Error e) {
 *    System.out.println(e.getMessage())
 *}</pre>
 * 
 * Note that the additional distinction between JSON number types is made 
 * by considering numbers with an exponent as Doubles, the ones with decimals 
 * as BigDecimal and the others as BigInteger.</p>
 * 
 * <p><b>Copyright</b> &copy; 2006 Laurent A.V. Szyster</p>
 * 
 * @author Laurent Szyster
 * @version 0.1.0
 */
public class JSON {
    
    private static final String NULL_JSON_STRING = "null JSON string";
    protected static final char DONE = CharacterIterator.DONE;
    
    /**
     * A simple JSON exception throwed for any syntax error found by the
     * interpreter. 
     * 
     * <p><b>Copyright</b> &copy; 2006 Laurent A.V. Szyster</p>
     * 
     * @author Laurent Szyster
     * @version 0.1.0
     */
    public static class Error extends Exception {
        private static final long serialVersionUID = 0L;
        public Error(String message) {super(message);}
    }
    
    /**
     * <p>A relatively strict JSON intepreter to evaluate a UNICODE string 
     * as a tree of basic Java instances with maximum limits on the number
     * of containers and iterations, protecting the JVM, CPU and RAM from 
     * malicious input.</p>
     * 
     * <h3>Synopsis</h3>
     * 
     * <p>Direct instanciation of an Interpreter is usefull to evaluate many 
     * strings under the same global constraints on their cumulated numbers 
     * of containers and iterations.</p>
     * 
     * <p>It's practical to evaluate distinct JSON values
     * 
     * <blockquote>
     * <pre>JSON.Interpreter ji = new JSON.Interpreter(16, 256);
     *try {
     *    Object one = ji.eval("{\"size\": 0}");
     *    Object two = ji.eval("[1.0, true, null]");
     *    Object three = ji.eval("1.0");
     *    Object four = ji.eval("true");
     *    Object five = ji.eval("null");
     *} catch (JSON.Error e) {
     *    System.out.println(e.getMessage());
     *}</pre>
     * </blockquote>
     * 
     * to update a <code>HashMap</code> with the members of many JSON objects:
     * 
     * <blockquote>
     * <pre>JSON.Interpreter ji = new JSON.Interpreter(16, 256);
     *try {
     *    HashMap object = ji.update(new HashMap(), "{\"width\": 200}");
     *    ji.update(object, "{\"height\": 10}");
     *} catch (JSON.Error e) {
     *    System.out.println(e.getMessage());
     *}</pre>
     * </blockquote>
     * 
     * or to extend an <code>ArrayList</code> with the collection of many 
     * JSON arrays:
     * 
     * <blockquote>
     * <pre>JSON.Interpreter ji = new JSON.Interpreter(16, 256);
     *try {
     *    ArrayList array = ji.extend(new ArrayList(), "[1,2,3]");
     *    ji.extend(array, "[null, true, 1.0]");
     *} catch (JSON.Error e) {
     *    System.out.println(e.getMessage());
     *}</pre>
     * </blockquote>
     * 
     * For a more common use, to evaluate a one string only, the
     * static <code>JSON</code> methods should be used instead.</p> 
     * 
     * <p><b>Copyright</b> &copy; 2006 Laurent A.V. Szyster</p>
     * 
     * @author Laurent Szyster
     * @version 0.1.0
     */
    public static class Interpreter {
        
        protected static final String ILLEGAL_UNICODE_SEQUENCE = 
            " illegal UNICODE sequence";
        protected static final String ILLEGAL_ESCAPE_SEQUENCE = 
            " illegal escape sequence ";
        protected static final String COLON_EXPECTED = 
            " colon expected ";
        protected static final String VALUE_EXPECTED = 
            " value expected";
        protected static final String ITERATIONS_OVERFLOW = 
            " iterations overflow";
        protected static final String STRING_EXPECTED = 
            " string expected ";
        protected static final String CONTAINERS_OVERFLOW = 
            " containers overflow";
        protected static final String UNEXPECTED_CHARACTER = 
            " unexpected character ";
        protected static final String UNEXPECTED_END = 
            " unexpected end";
        protected static final String NULL_EXPECTED = 
            " 'null' expected ";
        protected static final String FALSE_EXPECTED = 
            " 'false' expected ";
        protected static final String TRUE_EXPECTED = 
            " 'true expected ";
        protected static final String NOT_AN_OBJECT = 
            " not an object";
        protected static final String NOT_AN_ARRAY = 
            " not an array";
        
        protected char c;
        protected CharacterIterator it;
        protected StringBuffer buf;
        protected int containers = 65355;
        protected int iterations = 65355;
        
        /**
         * Instanciate a JSON interpreter with limits set to 65355 on 
         * the number of both containers and iterations.
         */
        public Interpreter() {}
        
        /**
         * Instanciate a JSON interpreter with the given limits on 
         * the number of both containers and iterations.
         *
         * @param containers a limit on the number of objects and arrays
         * @param iterations a limit on the total count of values
         */
        public Interpreter(
            int containers, int iterations
            ) {
            this.containers = (containers > 0 ? containers: 1);
            this.iterations = (iterations > 0 ? iterations: 1);
        }
        
        /**
         * Evaluates a JSON string as an untyped value, returns a 
         * <code>HashMap</code>, 
         * <code>ArrayList</code>, 
         * <code>String</code>,
         * <code>BigDecimal</code>, 
         * <code>BigInteger</code>, 
         * <code>Double</code>,
         * <code>Boolean</code>,
         * null or throws a JSON.Error if a syntax error occured.
         * 
         * @param json the string to evaluate
         * @return an untyped Object
         * @throws Error
         */
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
        
        /**
         * Evaluates a JSON string as an object, returns the updated 
         * <code>map</code> or throws a JSON.Error if the string does not
         * represent a valid object. 
         * 
         * @param map the <code>HashMap</code> to update
         * @param json the string to evaluate
         * @return an updated <code>map</code>
         * @throws Error
         */
        public HashMap update(HashMap map, String json) throws Error {
            buf = new StringBuffer();
            it = new StringCharacterIterator(json);
            try {
                c = it.first();
                while (Character.isWhitespace(c)) c = it.next();
                if (c != '{')
                    throw error(NOT_AN_OBJECT);
                else
                    return (HashMap) object(map);
            } finally {
                buf = null;
                it = null;
            }
        }
        
        /**
         * Evaluates a JSON string as an array, returns the extended
         * <code>list</code> or throws a JSON.Error if the string does not
         * represent a valid array. 
         * 
         * @param list the <code>ArrayList</code> to extend
         * @param json the string to evaluate
         * @return an updated <code>map</code>
         * @throws Error
         */
        public ArrayList extend(ArrayList list, String json) throws Error {
            buf = new StringBuffer();
            it = new StringCharacterIterator(json);
            try {
                c = it.first();
                while (Character.isWhitespace(c)) c = it.next();
                if (c != '[')
                    throw error(NOT_AN_ARRAY);
                else
                    return (ArrayList) array(list);
            } finally {
                buf = null;
                it = null;
            }
        }
        
        protected Error error(String message) {
            StringBuffer sb = new StringBuffer();
            sb.append(it.getIndex());
            sb.append(message);
            return new Error(sb.toString());
        }
        
        protected Error error(String message, char arg) {
            StringBuffer sb = new StringBuffer();
            sb.append(it.getIndex());
            sb.append(message);
            sb.append(arg);
            return new Error(sb.toString());
        }
        
        protected boolean next(char test) {
            c = it.next();
            return c == test;
        }
        
        protected static final Object OBJECT = new Object();
        protected static final Object ARRAY = new Object();
        protected static final Object COLON = new Object();
        protected static final Object COMMA = new Object();
        
        protected Object value() throws Error {
            while (Character.isWhitespace(c)) c = it.next();
            switch(c){
            case '{': {c = it.next(); return object(null);}
            case '[': {c = it.next(); return array(null);}
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
                    throw error(TRUE_EXPECTED, c);
            }
            case 'f': {
                if (next('a') && next('l') && next('s') && next('e')) {
                    c = it.next(); return Boolean.FALSE;
                } else
                    throw error(FALSE_EXPECTED, c);
            }
            case 'n': {
                if (next('u') && next('l') && next('l')) {
                    c = it.next(); return null;
                } else
                    throw error(NULL_EXPECTED, c);
            }
            case ',': {c = it.next(); return COMMA;} 
            case ':': {c = it.next(); return COLON;}
            case ']': {c = it.next(); return ARRAY;} 
            case '}': {c = it.next(); return OBJECT;}
            case DONE:
                throw error(UNEXPECTED_END);
            default: 
                throw error(UNEXPECTED_CHARACTER, c);
            }
        }
        
        protected Object object(HashMap map) throws Error {
            String key; 
            Object val;
            map = (map != null) ? map : new HashMap();
            if (--containers < 0) 
                throw error(CONTAINERS_OVERFLOW);
            
            Object token = value();
            while (token != OBJECT) {
                if (!(token instanceof String))
                    throw error(STRING_EXPECTED);
                
                if (--iterations < 0) 
                    throw error(ITERATIONS_OVERFLOW);
                
                key = (String) token;
                if (value() == COLON) {
                    val = value();
                    if (val==COLON || val==COMMA || val==OBJECT || val==ARRAY)
                        throw error(VALUE_EXPECTED);
                    
                    map.put(key, val);
                    token = value();
                    if (token == COMMA)
                        token = value();
                } else {
                    throw error(COLON_EXPECTED, c);
                }
            }
            return map;
        }
        
        protected Object array(ArrayList list) throws Error {
            list = (list != null) ? list : new ArrayList();
            if (--containers < 0) 
                throw error(CONTAINERS_OVERFLOW);
            
            Object token = value();
            while (token != ARRAY) {
                if (token==COLON || token==COMMA || token==OBJECT)
                    throw error(VALUE_EXPECTED);
                
                if (--iterations < 0) 
                    throw error(ITERATIONS_OVERFLOW);
                
                list.add(token);
                token = value(); 
                if (token == COMMA) 
                    token = value();
            }
            return list;
        }
        
        protected Object number() {
            buf.setLength(0);
            if (c == '-') {
                buf.append(c); c = it.next();
            }
            digits();
            if (c == '.') {
                buf.append(c); c = it.next();
                digits();
                if (c == 'e' || c == 'E') {
                    buf.append(c); c = it.next();
                    if (c == '+' || c == '-') {
                        buf.append(c); c = it.next();
                    }
                    digits();
                    return new Double(buf.toString());
                } else {
                    return new BigDecimal(buf.toString()); 
                }
            } else if (c == 'e' || c == 'E') {
                buf.append(c); c = it.next();
                if (c == '+' || c == '-') {
                    buf.append(c); c = it.next();
                }
                digits();
                return new Double(buf.toString());
            } else {
                return new BigInteger(buf.toString());
            }
        }
        
        protected Object string() throws Error {
            buf.setLength(0);
            while (c != '"') {
                if (c == '\\') {
                    c = it.next(); 
                    switch(c) {
                        case 'u': buf.append(unicode(4)); break;
                        case 'x': buf.append(unicode(2)); break;
                        case '\\': buf.append('\\'); break;
                        case '"': buf.append('"'); break;
                        case '/': buf.append('/'); break;
                        case 'b': buf.append('\b'); break;
                        case 'f': buf.append('\f'); break;
                        case 'n': buf.append('\n'); break;
                        case 'r': buf.append('\r'); break;
                        case 't': buf.append('\t'); break;
                        default: 
                            throw error(ILLEGAL_ESCAPE_SEQUENCE, c);
                    }
                } else if (c == DONE) {
                    throw error(UNEXPECTED_END);
                } else {
                    buf.append(c); 
                }
                c = it.next();
            }
            c = it.next();
            return buf.toString();
        }
        
        protected void digits() {
            while (Character.isDigit(c)) {buf.append(c); c = it.next();}
        }
        
        protected char unicode(int length) throws Error {
            int val = 0;
            for (int i = 0; i < length; ++i) {
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
                    throw error(UNEXPECTED_END);
                default:
                    throw error(ILLEGAL_UNICODE_SEQUENCE);
                }
            }
            return (char) val;
        }

    }

    /**
     * Evaluates a JSON string as an untyped value, returns a 
     * <code>HashMap</code>, 
     * <code>ArrayList</code>, 
     * <code>String</code>,
     * <code>BigDecimal</code>, 
     * <code>BigInteger</code>, 
     * <code>Double</code>,
     * <code>Boolean</code>,
     * null or throws a JSON.Error if a syntax error occured.
     * 
     * @param json the string to evaluate
     * @param containers the maximum number of containers allowed 
     * @param containers the limit on the count of values 
     * @return an untyped Object
     * @throws Error
     */
    public static Object eval(
        String json, int containers, int iterations
        ) throws Error {
        if (json != null)
            return (new Interpreter(containers, iterations)).eval(json);
        else
            throw new Error(NULL_JSON_STRING);
    }
    
    /**
     * Evaluates a JSON object, returns a new <code>HashMap</code>   
     * or throws a JSON.Error if the string does not represent a valid object. 
     * 
     * @param json the string to evaluate
     * @param containers the maximum number of containers allowed 
     * @param containers the limit on the count of values 
     * @return a new <code>HashMap</code>
     * @throws Error
     */
    public static HashMap object(
        String json, int containers, int iterations
        ) throws Error {
        if (json != null)
            return (
                new Interpreter(containers, iterations)
                ).update(null, json);
        else
            throw new Error(NULL_JSON_STRING);
        }
        
    /**
     * Evaluates a JSON array, returns a new <code>ArrayList</code>   
     * or throws a JSON.Error if the string does not represent a valid array. 
     * 
     * @param json the string to evaluate
     * @param containers the maximum number of containers allowed 
     * @param containers the limit on the count of values 
     * @return a new <code>ArrayList</code>
     * @throws Error
     */
    public static ArrayList array(
        String json, int containers, int iterations
        ) throws Error {
        if (json != null)
            return (
                new Interpreter(containers, iterations)
                ).extend(null, json);
        else
            throw new Error(NULL_JSON_STRING);
    }
            
    /**
     * Evaluates a JSON string as an object, returns the updated 
     * <code>map</code> or throws a JSON.Error if the string does not
     * represent a valid object. 
     * 
     * @param map the <code>HashMap</code> to update
     * @param json the string to evaluate
     * @param containers the maximum number of containers allowed 
     * @param containers the limit on the count of values 
     * @return an updated <code>map</code>
     * @throws Error
     */
    public static boolean update(
        HashMap map, String json, int containers, int iterations
        ) {
        if (json != null) try {
            (new Interpreter(containers, iterations)).update(map, json);
            return false;
            
        } catch (Error e) {}
        return false;
    }
            
    /**
     * Evaluates a JSON string as an array, returns the extended
     * <code>list</code> or throws a JSON.Error if the string does not
     * represent a valid array. 
     * 
     * @param list the <code>ArrayList</code> to extend
     * @param json the string to evaluate
     * @param containers the maximum number of containers allowed 
     * @param containers the limit on the count of values 
     * @return an updated <code>map</code>
     * @throws Error
     */
    public static boolean extend(
        ArrayList list, String json, int containers, int iterations 
        ) throws Error {
        if (json != null) try {
            (new Interpreter(containers, iterations)).extend(list, json);
            return false;
            
        } catch (Error e) {}
        return false;
    }
                
    protected static final String _quote = "\\\"";
    protected static final String _back = "\\\\";
    protected static final String _slash = "\\/";
    protected static final String _ctrl_b = "\\b";
    protected static final String _ctrl_f = "\\f";
    protected static final String _ctrl_n = "\\n";
    protected static final String _ctrl_r = "\\r";
    protected static final String _ctrl_t = "\\t";
    
    public static StringBuffer strb(StringBuffer sb, String s) {
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
        return sb;
    }
    
    protected static final String _unicode = "\\u";
    protected static final char[] _hex = "0123456789ABCDEF".toCharArray();
    
    protected static StringBuffer unicode(StringBuffer sb, char c) {
        sb.append(_unicode);
        int n = c;
        for (int i = 0; i < 4; ++i) {
            int digit = (n & 0xf000) >> 12;
            sb.append(_hex[digit]);
            n <<= 4;
        }
        return sb;
    }

    protected static final String _object = "{}";
    protected static final String _array = "[]";
    protected static final String _null = "null";
    protected static final String _true = "true";
    protected static final String _false = "false";
    
    public static StringBuffer strb(StringBuffer sb, Map map) {
        Object key; 
        Iterator it = map.keySet().iterator();
        if (!it.hasNext()) {
            sb.append(_object);
            return sb;
        }
        sb.append('{');
        key = it.next();
        strb(sb, key);
        sb.append(':');
        strb(sb, map.get(key));
        while (it.hasNext()) {
            sb.append(',');
            key = it.next();
            strb(sb, key);
            sb.append(':');
            strb(sb, map.get(key));
        }
        sb.append('}');
        return sb;
    }
    
    public static StringBuffer strb(StringBuffer sb, Iterator it) {
        if (!it.hasNext()) {
            sb.append(_array);
            return sb;
        }
        sb.append('[');
        strb(sb, it.next());
        while (it.hasNext()) {
            sb.append(',');
            strb(sb, it.next());
        }
        sb.append(']');
        return sb;
    }
    
    public static StringBuffer strb(StringBuffer sb, Object value) {
        if (value == null) 
            sb.append(_null);
        else if (value instanceof Boolean)
            sb.append((((Boolean) value).booleanValue() ? _true : _false));
        else if (value instanceof Number) 
            sb.append(value);
        else if (value instanceof String) 
            strb(sb, (String) value);
        else if (value instanceof Character) 
            strb(sb, ((Character) value).toString());
        else if (value instanceof Map)
            strb(sb, (Map) value);
        else if (value instanceof List) 
            strb(sb, ((List) value).iterator());
        else if (value instanceof Object[]) 
            strb(sb, Simple.iterator((Object[]) value));
        else if (value instanceof JSON) 
            strb(sb, ((JSON) value).string);
        else 
            strb(sb, value.toString());
        return sb;
    }
    
    public static String str(Object value) {
        return strb(new StringBuffer(), value).toString();
    }
    
    protected static final String _crlf = "\r\n";
    protected static final String _indent = "  ";
    
    protected static StringBuffer repr(
        StringBuffer sb, Map map, String indent
        ) {
        Object key; 
        Iterator it = map.keySet().iterator();
        if (!it.hasNext()) {
            sb.append("{}");
            return sb;
        }
        indent += _indent;
        sb.append('{');
        sb.append(indent);
        key = it.next();
        repr(sb, key, indent);
        sb.append(": ");
        repr(sb, map.get(key), indent);
        while (it.hasNext()) {
            sb.append(", ");
            sb.append(indent);
            key = it.next();
            repr(sb, key, indent);
            sb.append(": ");
            repr(sb, map.get(key), indent);
        }
        sb.append(indent);
        sb.append('}');
        return sb;
    }
    
    protected static StringBuffer repr(
        StringBuffer sb, Iterator it, String indent
        ) {
        if (!it.hasNext()) {
            sb.append("[]");
            return sb;
        }
        sb.append('[');
        indent += _indent;
        sb.append(indent);
        repr(sb, it.next(), indent);
        while (it.hasNext()) {
            sb.append(", ");
            sb.append(indent);
            repr(sb, it.next(), indent);
        }
        sb.append(indent);
        sb.append(']');
        return sb;
    }
    
    protected static StringBuffer repr(
        StringBuffer sb, Object value, String indent
        ) {
        if (value == null) 
            sb.append(_null);
        else if (value instanceof Boolean)
            sb.append((
                ((Boolean) value).booleanValue() ? "true": "false"
                ));
        else if (value instanceof Number) 
            sb.append(value);
        else if (value instanceof String) 
            strb(sb, (String) value);
        else if (value instanceof Character) 
            strb(sb, ((Character) value).toString());
        else if (value instanceof Map)
            repr(sb, (Map) value, indent);
        else if (value instanceof List) 
            repr(sb, ((List) value).iterator(), indent);
        else if (value instanceof JSON) 
            strb(sb, ((JSON) value).string);
        else 
            strb(sb, value.toString());
        return sb;
    }
    
    public static String repr(Object value) {
        return repr(new StringBuffer(), value, _crlf).toString();
    }
    
    public String string;
    
    public JSON(Object value) {this.string = str(value);}
    
}

