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

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.Iterator;
import java.util.HashMap;
import java.util.ArrayList;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;

/**
 * <p>A relatively strict JSON intepreter to evaluate a UNICODE string 
 * as a tree of basic Java instances with maximum limits on the number
 * of containers and iterations, plus static methods to serialize java 
 * objects as JSON strings.</p>
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
 * <pre>JSON json = new JSON(16, 256);
 *try {
 *    Object one = json.eval("{\"size\": 0}");
 *    Object two = json.eval("[1.0, true, null]");
 *    Object three = json.eval("1.0");
 *    Object four = json.eval("true");
 *    Object five = json.eval("null");
 *} catch (JSON.Error e) {
 *    System.out.println(e.getMessage());
 *}</pre>
 * </blockquote>
 * 
 * to update any instance of <code>Map</code> with the members of many 
 * JSON objects:
 * 
 * <blockquote>
 * <pre>JSON.Error e;
 *JSON json = new JSON(16, 256);
 *HashMap map = new HashMap(); 
 *e = json.update(map, "{\"width\": 200}");
 *if (e != null)
 *    System.out.println(e.str());
 *e = json.update(map, "{\"pass\": 1, fail: true}");
 *if (e != null)
 *    System.out.println(e.str());</pre>
 * </blockquote>
 * 
 * or to extend any <code>List</code> with the collection of many 
 * JSON arrays:
 * 
 * <blockquote>
 * <pre>JSON json = new JSON(16, 256);
 *ArrayList list = new ArrayList(); 
 *e = json.extend(list, "[1,2,3]");
 *if (e != null)
 *    System.out.println(e.str());
 *e = json.extend(list, "[null, true, 1.0]");
 *if (e != null)
 *    System.out.println(e.str());</pre>
 * </blockquote>
 * 
 * For a more common use, to evaluate one JSON string only, the
 * static <code>JSON</code> methods should be used instead.</p> 
 * 
 * <h4>JSON Types</h4>
 * 
 * <p>A convenience with static methods to serialize java objects as JSON
 * strings and to evaluate a strict JSON expression as a limited tree of
 * the five Java types
 * 
 * <blockquote>
 * <code>String</code>, 
 * <code>Double</code>, 
 * <code>BigDecimal</code>, 
 * <code>BigInteger</code>, 
 * <code>Boolean</code>,
 * </blockquote>
 *
 * two convenience extending <code>HashMap</code> and <code>ArrayList</code>, 
 * plus the untyped <code>null</code> value.</p>.
 * 
 * <p>Note that the additional distinction between JSON number types is made 
 * by considering numbers with an exponent as Doubles, the ones with decimals 
 * as BigDecimal and the others as BigInteger.</p>
 * 
 * <h3>Conveniences: O and A</h3>
 * 
 * <p>Conveniences JSON static methods allow to evaluate strings as an untyped
 * <code>Object</code>, an <code>JSON.O</code> map or a <code>JSON.A</code> 
 * list: 
 * 
 * <blockquote>
 * <pre>JSON json = new JSON() 
 *try {
 *    Object value = json.eval("null");
 *    JSON.O map = json.object("{\"pass\": true}");
 *    JSON.A list = json.array("[1,2,3]");
 *} catch (JSON.Error e) {
 *    System.out.println(e.getMessage())
 *}</pre>
 * </blockquote>
 * 
 * access them simply and practically
 * 
 * <blockquote>
 * <pre>try {
 *    if (map.bool("pass"))
 *        BigInteger i = list.intg(2);
 *} catch (JSON.Error e) {
 *    System.out.println(e.getMessage())
 *}</pre>
 * </blockquote>
 * 
 * and serialize java instances as JSON strings
 * 
 * <blockquote>
 * <pre>System.out.println(JSON.str(value));
 *System.out.println(JSON.str(map));
 *System.out.println(JSON.str(list));</pre>
 * </blockquote>
 * 
 * Note that you can serialize not just the types instanciated by JSON
 * but also any <code>Map</code> or <code>List</code> of many other
 * java object.</p> 
 * 
 * <p>Also, JSON object are serialized with their properties sorted by names,
 * allowing to compare two objects for equality by comparing such string.
 * That's handy in many case, most remarkably in order to sign and check
 * digest for JSON objects.</p>
 * 
 * <h4>Safety Limits</h4>
 * 
 * <p>Lower limits than the defaults maximum of 65355 can be set for the
 * number of objects and arrays and the count of values, for containers
 * and iterations:
 * 
 * <blockquote>
 * <pre>try {
 *    JSON.A list = (new JSON(1, 4)).array("[1,2,3,4,5]");
 *} catch (JSON.Error e) {
 *    System.out.println(e.getMessage())
 *}</pre>
 * </blockquote>
 * 
 * making JSON evaluation safe for public interfaces.</p>
 * 
 * <h4>Serialization</h4>
 * 
 * <p>To append distinct values into a <code>StringBuffer</code>
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
 * using templates for constants.</p> 
 * 
 * <h4>Partial Serialization</h4>
 * 
 * <p>Instances of JSON class itself are made to serialize partial instance 
 * tree and assemble reponses from java objects and JSON strings:
 * 
 * <blockquote>
 * <pre>JSON json = new JSON();
 *try {
 *    JSON.O map = json.object("{}");
 *    map.put("list", json.array("[1,2,3]");
 *    json = new JSON();
 *    json.string = "{\"constant\": null"}";
 *    map.put("value", json);
 *    System.out.println(JSON.str(map));
 *} catch (JSON.Error e) {
 *    System.out.println(e.getMessage())
 *}</pre>
 *</blockquote>
 * 
 * ...</p>
 * 
 * <h4>Pretty Print</h4>
 * 
 * <p>To pretty print an indented representation of a java instance in JSON:
 * 
 * <blockquote>
 * <pre>System.out.println(JSON.repr(value));
 *System.out.println(JSON.repr(map));
 *System.out.println(JSON.repr(list));</pre>
 * </blockquote>
 * 
 * ...</p>
 * 
 * <p><b>Copyright</b> &copy; 2006 Laurent A.V. Szyster</p>
 * 
 * @version 0.20
 */
public class JSON {
    
    /**
     * A simple JSON exception throwed for any syntax error found by the
     * interpreter.
     * 
     * <h3>Synopsis</h3>
     * 
     * There is just enough in a JSON.Error to identify the error 
     * 
     * <pre>String string = "{\"test\": fail}";
     *try {
     *    Object value = (new JSON()).eval(string)
     *} catch (JSON.Error e) {
     *    System.out.println(e.jstr());
     *}</pre>
     * 
     * <p><b>Copyright</b> &copy; 2006 Laurent A.V. Szyster</p>
     * 
     * @version 0.20
     */
    public static class Error extends Exception {
        
        static final long serialVersionUID = 0L; // TODO: regenerate
        
        /**
         * The position of the JSON syntax error, -1 by default.
         */
        public int jsonIndex = -1;
        
        /**
         * The path to the JSON error value, if any.
         */
        public ArrayList jsonPath = new ArrayList();
        
        /**
         * Instanciate a JSON error with an error message.
         * 
         * @param message the error message
         */
        public Error(String message) {super(message);}
        
        /**
         * Instanciate a JSON error with an error message and the index 
         * in the JSON string at which the error occured.
         * 
         * @param message the error message
         * @param index position at which the error occured
         */
        public Error(String message, int index) {
            super(message);
            jsonIndex = index;
            }
        
        /**
         * <p>Buffers a JSON error as a JSON string:
         * 
         * <blockquote>
         *<pre>StringBuffer sb = new StringBuffer();
         *try {
         *    String model = (new JSON()).eval("{fail}");
         *} catch (JSONR.Error e) {
         *    e.jsonError(sb);
         *}</pre>
         *System.out.println(sb.toString());
         *</blockquote>
         *
         * ...</p>
         * 
         * @return the updated StringBuffer
         */
        public final StringBuffer jsrtb(StringBuffer sb) {
            sb.append('[');
            JSON.strb(sb, getMessage());
            sb.append(',');
            sb.append(jsonIndex);
            sb.append(',');
            JSON.strb(sb, jsonPath);
            sb.append(']');
            return sb;
        }
        
        /**
         * <p>Represents a JSON error as an array like
         * 
         *   ["error message"], 23, [1, 2]] 
         *   
         * and return a JSON string.</p>
         * 
         * @return a JSON string
         */
        public final String jstr() {
            return jsrtb(new StringBuffer()).toString();
        }
        
    }
    
    protected static final String OBJECT_TYPE_ERROR = 
        "Object type error";
    protected static final String ARRAY_TYPE_ERROR = 
        "Array type error";
    protected static final String STRING_TYPE_ERROR = 
        "String type error";
    protected static final String BOOLEAN_TYPE_ERROR = 
        "Boolean type error";
    protected static final String DOUBLE_TYPE_ERROR = 
        "Double type error";
    protected static final String BIGDECIMAL_TYPE_ERROR = 
        "BigDecimal type error";
    protected static final String BIGINTEGER_TYPE_ERROR = 
        "BigInteger type error";
    protected static final String NULL_JSON_STRING = 
        "null JSON string";
    
    protected static final char _done = CharacterIterator.DONE;
    
    protected static final BigInteger intg (Object value) throws Error {
        if (value instanceof BigInteger) {
            return (BigInteger) value;
        } else throw new Error(BIGINTEGER_TYPE_ERROR);
    }   
    protected static final BigDecimal deci(Object value) throws Error {
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        } else throw new Error(BIGDECIMAL_TYPE_ERROR);
    }
    protected static final Double dble(Object value) throws Error {
        if (value instanceof Double) {
            return (Double) value;
        } else throw new Error(DOUBLE_TYPE_ERROR);
    }
    protected static final Boolean bool(Object value) throws Error {
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else throw new Error(BOOLEAN_TYPE_ERROR);
    }
    protected static final String stri(Object value) throws Error {
        if (value instanceof String) {
            return (String) value;
        } else throw new Error(STRING_TYPE_ERROR);
    }
    protected static final A arry(Object value) throws Error {
        if (value instanceof A) {
            return (A) value;
        } else throw new Error(ARRAY_TYPE_ERROR);
    }
    protected static final O objc(Object value) throws Error {
        if (value instanceof O) {
            return (O) value;
        } else throw new Error(OBJECT_TYPE_ERROR);
    }
    
    /**
     * An extension of HashMap with type-casting convenience methods
     * that throw <code>JSON.Error</code> or return a typed object.
     * 
     * <h3>Synopsis</h3>
     * 
     * <p>...
     * 
     * </blockquote>
     * <pre>try {
     *    JSON.O map = (new JSON()).object("{" +
     *        "\"nothing\": null," +
     *        "\"a boolean\": true," +
     *        "\"a big integer\": 2," +
     *        "\"a big decimal\": +1234567.89," +
     *        "\"a double\": -123456789e-4," +
     *        "\"unicode"\: \"hello world!\", "+
     *        "\"a list\": [null,true,1,2.0,3e+3]" +
     *        "\"another map\": {}" +
     *        "}");
     *    Boolean b = map.objc("test");
     *    BigInteger i = map.intg("a big integer");
     *    BigDecimal d = map.deci("a big decimal");
     *    Double r = map.dble("a double");
     *    String s = map.stri("unicode");
     *    JSON.A a = map.stri("a list");
     *    JSON.O o = map.stri("a string");
     *} catch (JSON.Error e) {
     *    System.out.println(e.jstr());
     *}</pre>
     * </blockquote>
     * 
     * The convenience is double. At runtime it distinguishes a JSON
     * type value error from other type casting allow programs to be
     * executed like a scripting language against to access a dynamic  
     * object model and continue in Java.</p>
     * 
     * <p>The second advantage for developper is trivial but practical:
     * most java IDE support autocompletion and namespace browsing by
     * type. Not having to break the chain for "manual" type casting
     * helps a lot and make the whole a lot clearer to read and debug.</p> 
     * 
     * <p><b>Copyright</b> &copy; 2006 Laurent A.V. Szyster</p>
     * 
     * @version 0.20
     */
    public static final class O extends HashMap {
        static final long serialVersionUID = 0L; // TODO: regenerate
        public final BigInteger intg(String name) throws Error {
            return (JSON.intg(get(name)));
        }
        public final BigDecimal deci(String name) throws Error {
            return JSON.deci(get(name));
        }
        public final Double dble(String name) throws Error {
            return (JSON.dble(get(name)));
        }
        public final Boolean bool(String name) throws Error {
            return (JSON.bool(get(name)));
        }
        public final String stri(String name) throws Error {
            return (JSON.stri(get(name)));
        }
        public final A arry(String name) throws Error {
            return (JSON.arry(get(name)));
        }
        public final O objc(String name) throws Error {
            return (JSON.objc(get(name)));
        }
    }
    
    /**
     * An extension of ArrayList with type-casting convenience methods
     * that throw <code>JSON.Error</code> or return a typed object.
     * 
     * <h3>Synopsis</h3>
     * 
     * <p>...
     * 
     * </blockquote>
     * <pre>try {
     *    JSON.A list = (new JSON()).array(
     *        "[true, 1, 3.0, 1234e-2, \"test\", [], {}]"
     *    );
     *    Object o = list.get(0); 
     *    Boolean b = list.objc(1);
     *    BigInteger i = list.intg(2);
     *    BigDecimal d = list.deci(3);
     *    Double r = list.dble(4);
     *    JSON.A a = list.arry(5);
     *    JSON.O o = list.objc(6);
     *    Boolean b = list.objc(5).bool(2);
     *} catch (JSON.Error e) {
     *    System.out.println(e.jstr());
     *}</pre>
     * </blockquote>
     * 
     * ...</p>
     * 
     * <p><b>Copyright</b> &copy; 2006 Laurent A.V. Szyster</p>
     * 
     * @version 0.20
     */
    public static final class A extends ArrayList {
        static final long serialVersionUID = 0L; // TODO: regenerate
        public final BigInteger intg(int index) throws Error {
            return (JSON.intg(get(index)));
        }
        public final BigDecimal deci(int index) throws Error {
            return JSON.deci(get(index));
        }
        public final Double dble(int index) throws Error {
            return (JSON.dble(get(index)));
        }
        public final Boolean bool(int index) throws Error {
            return (JSON.bool(get(index)));
        }
        public final String stri(int index) throws Error {
            return (JSON.stri(get(index)));
        }
        public final A arry(int index) throws Error {
            return (JSON.arry(get(index)));
        }
        public final O objc(int index) throws Error {
            return (JSON.objc(get(index)));
        }
    }
    
    public static final class Partial {

        protected String string = null;
        
        public Partial(Object value) {string = str(value);}
        
        public StringBuffer jstrb(StringBuffer sb) {
            sb.append(string);
            return sb;
        }
        
        public String jstr() {return string;}
        
    }

    protected static final String _quote = "\\\"";
    protected static final String _back = "\\\\";
    protected static final String _slash = "\\/";
    protected static final String _ctrl_b = "\\b";
    protected static final String _ctrl_f = "\\f";
    protected static final String _ctrl_n = "\\n";
    protected static final String _ctrl_r = "\\r";
    protected static final String _ctrl_t = "\\t";
    
    public static final 
    StringBuffer strb(StringBuffer sb, String s) {
        sb.append('"');
        CharacterIterator it = new StringCharacterIterator(s);
        for (char c = it.first(); c != _done; c = it.next()) {
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
    
    protected static final StringBuffer unicode(StringBuffer sb, char c) {
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
    
    public static final 
    StringBuffer strb(StringBuffer sb, Map map, Iterator it) {
        Object key; 
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
    
    public static final 
    StringBuffer strb(StringBuffer sb, Iterator it) {
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
    
    public static final 
    StringBuffer strb(StringBuffer sb, Object value) {
        if (value == null) 
            sb.append(_null);
        else if (value instanceof Boolean)
            sb.append(((Boolean) value).booleanValue() ? _true : _false);
        else if (value instanceof Number) 
            sb.append(value);
        else if (value instanceof String) 
            strb(sb, (String) value);
        else if (value instanceof Character) 
            strb(sb, ((Character) value).toString());
        else if (value instanceof Map) {
            Map object = (Map) value;
            Object[] names = object.keySet().toArray();
            Arrays.sort(names);
            strb(sb, object, object.keySet().iterator());
        } else if (value instanceof List) 
            strb(sb, ((List) value).iterator());
        else if (value instanceof Object[]) 
            strb(sb, Simple.iterator((Object[]) value));
        else if (value instanceof Partial) 
            ((Partial) value).jstrb(sb);
        else if (value instanceof Error) 
            ((Error) value).jsrtb(sb);
        else 
            strb(sb, value.toString());
        return sb;
    }
    
    public static final String str(Object value) {
        return strb(new StringBuffer(), value).toString();
    }
    
    protected static final StringBuffer xjson(StringBuffer sb, String s) {
        sb.append('"');
        CharacterIterator it = new StringCharacterIterator(s);
        for (char c = it.first(); c != _done; c = it.next()) {
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
                if (c > 126 || c < 32)
                    unicode(sb, c);
                else
                    sb.append(c);
            }
        }
        sb.append('"');
        return sb;
    }
    
    protected static final 
    StringBuffer xjson(StringBuffer sb, Map map, Iterator it) {
        Object key; 
        if (!it.hasNext()) {
            sb.append(_object);
            return sb;
        }
        sb.append('{');
        key = it.next();
        xjson(sb, key);
        sb.append(':');
        xjson(sb, map.get(key));
        while (it.hasNext()) {
            sb.append(',');
            key = it.next();
            xjson(sb, key);
            sb.append(':');
            xjson(sb, map.get(key));
        }
        sb.append('}');
        return sb;
    }
    
    protected static final 
    StringBuffer xjson(StringBuffer sb, Iterator it) {
        if (!it.hasNext()) {
            sb.append(_array);
            return sb;
        }
        sb.append('[');
        xjson(sb, it.next());
        while (it.hasNext()) {
            sb.append(',');
            xjson(sb, it.next());
        }
        sb.append(']');
        return sb;
    }
    
    protected static final 
    StringBuffer xjson(StringBuffer sb, Object value) {
        if (value == null) 
            sb.append(_null);
        else if (value instanceof Boolean)
            sb.append(((Boolean) value).booleanValue() ? _true : _false);
        else if (value instanceof Number) 
            sb.append(value);
        else if (value instanceof String) 
            xjson(sb, (String) value);
        else if (value instanceof Character) 
            xjson(sb, ((Character) value).toString());
        else if (value instanceof Map) {
            Map object = (Map) value;
            String[] names = (String[]) object.keySet().toArray();
            Arrays.sort(names);
            strb(sb, object, object.keySet().iterator());
        } else if (value instanceof List) 
            xjson(sb, ((List) value).iterator());
        else if (value instanceof Object[]) 
            xjson(sb, Simple.iterator((Object[]) value));
        else if (value instanceof Partial) 
            ((Partial) value).jstrb(sb);
        else if (value instanceof Error) 
            ((Error) value).jsrtb(sb);
        else 
            xjson(sb, value.toString());
        return sb;
    }
    
    public static final String xjson(Object value) {
        return xjson(new StringBuffer(), value).toString();
    }
    
    protected static final String _crlf = "\r\n";
    protected static final String _indent = "  ";
    
    protected static final 
    StringBuffer repr(StringBuffer sb, Map map, Iterator it, String indent) {
        Object key; 
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
    
    protected static final 
    StringBuffer repr(StringBuffer sb, Iterator it, String indent) {
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
    
    protected static final 
    StringBuffer repr(StringBuffer sb, Object value, String indent) {
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
        else if (value instanceof Map) {
            Map object = (Map) value;
            Object[] names = object.keySet().toArray();
            Arrays.sort(names);
            repr(sb, object, Simple.iterator(names), indent);
        } else if (value instanceof List) 
            repr(sb, ((List) value).iterator(), indent);
        else if (value instanceof JSON) {
            try {
                strb(sb, (new JSON()).eval(((Partial) value).jstr()));
            } catch (Error e) {
                e.jsrtb(sb);
            }
        } else if (value instanceof Error) 
            ((Error) value).jsrtb(sb);
        else 
            strb(sb, value.toString());
        return sb;
    }
    
    public static final String repr(Object value) {
        return repr(new StringBuffer(), value, _crlf).toString();
    }
    
    // The Interpreter
    
    protected static final String ILLEGAL_UNICODE_SEQUENCE = 
        "illegal UNICODE sequence";
    protected static final String ILLEGAL_ESCAPE_SEQUENCE = 
        "illegal escape sequence";
    protected static final String COLON_EXPECTED = 
        "colon expected";
    protected static final String VALUE_EXPECTED = 
        "value expected";
    protected static final String STRING_EXPECTED = 
        "string expected ";
    protected static final String UNEXPECTED_CHARACTER = 
        "unexpected character";
    protected static final String UNEXPECTED_END = 
        "unexpected end";
    protected static final String NULL_EXPECTED = 
        "null expected";
    protected static final String FALSE_EXPECTED = 
        "false expected";
    protected static final String TRUE_EXPECTED = 
        "true expected";
    protected static final String CONTAINERS_OVERFLOW = 
        "containers overflow";
    protected static final String ITERATIONS_OVERFLOW = 
        "iterations overflow";
    
    protected char c;
    protected CharacterIterator it;
    protected StringBuffer buf;
    
    public int containers = 65355;
    
    public int iterations = 65355;
    
    /**
     * Instanciate a JSON interpreter with limits set to 65355 on 
     * the number of both containers and iterations.
     */
    public JSON() {}
    
    /**
     * Instanciate a JSON interpreter with the given limits on 
     * the number of both containers and iterations.
     *
     * @param containers a limit on the number of objects and arrays
     * @param iterations a limit on the total count of values
     */
    public JSON(int containers, int iterations) {
        this.containers = (containers > 0 ? containers: 1);
        this.iterations = (iterations > 0 ? iterations: 1);
    }
    
    /**
     * Evaluates a JSON string as an untyped value, returns a 
     * <code>JSON.O</code>, 
     * <code>JSON.A</code>, 
     * <code>String</code>,
     * <code>BigDecimal</code>, 
     * <code>BigInteger</code>, 
     * <code>Double</code>,
     * <code>Boolean</code>,
     * null or throws a JSON.Error if a syntax error occured.
     * 
     * @param json string to evaluate
     * @return an untyped Object
     * @throws Error
     */
    public Object eval(String json) throws Error {
        buf = new StringBuffer();
        it = new StringCharacterIterator(json);
        try {
            c = it.first();
            if (c == _done)
                throw error(NULL_JSON_STRING);
            else
                return value();
        } finally {
            buf = null;
            it = null;
        }
    }
    
    /**
     * Evaluates a JSON object, returns a new <code>JSON.O</code>   
     * or throws a JSON.Error if the string does not represent a 
     * valid object or if it exceeds the limits set on the number
     * of containers and iterations. 
     * 
     * @param json string to evaluate
     * @return a new <code>JSON.O</code>
     * @throws Error
     */
    public O object(String json) throws Error {
        O o = new O();
        Error e = update(o, json);
        if (e == null)
            return o;
        else
            throw e;
    }

    /**
     * Evaluates a JSON array, returns a new <code>JSON.A</code> or throws   
     * a <code>JSON.Error</code> if the string does not represent a valid 
     * array or if it exceeds the limits on containers and iterations. 
     * 
     * @param json string to evaluate
     * @return a new <code>JSON.A</code> array
     * @throws Error
     */
    public A array(String json) throws Error {
        A a = new A();
        Error e = extend(a, json);
        if (e == null)
            return a;
        else
            throw e;
    }
            
    /**
     * Evaluates a JSON string and the update the object <code>map</code>, 
     * return null or a <code>JSON.Error</code> if the string does not 
     * represent a valid object. 
     * 
     * @param map the <code>Map</code> to update
     * @param json string to evaluate
     * @return null or a <code>JSON.Error</code>
     */
    public Error update(Map map, String json) {
        buf = new StringBuffer();
        it = new StringCharacterIterator(json);
        try {
            c = it.first();
            while (Character.isWhitespace(c)) c = it.next();
            if (c == '{') {
                c = it.next();
                object(map);
                return null;
            } else
                return error(OBJECT_TYPE_ERROR);
        } catch (Error e) {
            return e;
        } finally {
            buf = null;
            it = null;
        }
    }
    
    /**
     * Evaluates a JSON string and extends the <code>list</code>,
     * return null or a <code>JSON.Error</code> if the string does not 
     * represent a valid array. 
     * 
     * @param list the <code>List</code> to extend
     * @param json string to evaluate
     * @return null or a <code>JSON.Error</code>
     */
    public Error extend(List list, String json) {
        buf = new StringBuffer();
        it = new StringCharacterIterator(json);
        try {
            c = it.first();
            while (Character.isWhitespace(c)) c = it.next();
            if (c == '[') {
                c = it.next();
                array(list);
                return null;
            } else
                return error(ARRAY_TYPE_ERROR);
        } catch (Error e) {
            return e;
        } finally {
            buf = null;
            it = null;
        }
    }
    
    protected final Error error(String message) {
        return new Error(message, it.getIndex());
    }
    
    protected final boolean next(char test) {
        c = it.next();
        return c == test;
    }
    
    protected static final Object OBJECT = new Object();
    protected static final Object ARRAY = new Object();
    protected static final Object COLON = new Object();
    protected static final Object COMMA = new Object();
    
    protected final Object value() throws Error {
        while (Character.isWhitespace(c)) c = it.next();
        switch(c){
        case '{': {c = it.next(); return object(new O());}
        case '[': {c = it.next(); return array(new A());}
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
                throw error(TRUE_EXPECTED);
        }
        case 'f': {
            if (next('a') && next('l') && next('s') && next('e')) {
                c = it.next(); return Boolean.FALSE;
            } else
                throw error(FALSE_EXPECTED);
        }
        case 'n': {
            if (next('u') && next('l') && next('l')) {
                c = it.next(); return null;
            } else
                throw error(NULL_EXPECTED);
        }
        case ',': {c = it.next(); return COMMA;} 
        case ':': {c = it.next(); return COLON;}
        case ']': {c = it.next(); return ARRAY;} 
        case '}': {c = it.next(); return OBJECT;}
        case _done:
            throw error(UNEXPECTED_END);
        default: 
            throw error(UNEXPECTED_CHARACTER);
        }
    }
    
    protected final Object value(String name) throws Error {
        try {
            return value();
        } catch (Error e) {
            e.jsonPath.add(0, name);
            throw e;
        }
    }
    
    protected final Object value(int index) throws Error {
        try {
            return value();
        } catch (Error e) {
            e.jsonPath.add(0, BigInteger.valueOf(index));
            throw e;
        }
    }
    
    protected final Object object(Map o) throws Error {
        if (--containers < 0) 
            throw error(CONTAINERS_OVERFLOW);
        
        String name; 
        Object val;
        Object token = value();
        while (token != OBJECT) {
            if (!(token instanceof String))
                throw error(STRING_EXPECTED);
            
            if (--iterations < 0) 
                throw error(ITERATIONS_OVERFLOW);
            
            name = (String) token;
            if (value() == COLON) {
                val = value(name);
                if (val==COLON || val==COMMA || val==OBJECT || val==ARRAY)
                    throw error(VALUE_EXPECTED);
                
                o.put(name, val);
                token = value();
                if (token == COMMA)
                    token = value();
            } else {
                throw error(COLON_EXPECTED);
            }
        }
        return o;
    }
    
    protected final Object array(List a) throws Error {
        if (--containers < 0) 
            throw error(CONTAINERS_OVERFLOW);
        
        int i = 0;
        Object token = value(i++);
        while (token != ARRAY) {
            if (token==COLON || token==COMMA || token==OBJECT)
                throw error(VALUE_EXPECTED);
            
            if (--iterations < 0) 
                throw error(ITERATIONS_OVERFLOW);
            
            a.add(token);
            token = value(); 
            if (token == COMMA) 
                token = value(i++);
        }
        return a;
    }
    
    protected final Object number() {
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
    
    protected final Object string() throws Error {
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
                        throw error(ILLEGAL_ESCAPE_SEQUENCE);
                }
            } else if (c == _done) {
                throw error(UNEXPECTED_END);
            } else {
                buf.append(c); 
            }
            c = it.next();
        }
        c = it.next();
        return buf.toString();
    }
    
    protected final void digits() {
        while (Character.isDigit(c)) {buf.append(c); c = it.next();}
    }
    
    protected final char unicode(int length) throws Error {
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
            case _done:
                throw error(UNEXPECTED_END);
            default:
                throw error(ILLEGAL_UNICODE_SEQUENCE);
            }
        }
        return (char) val;
    }

}

/* Note about this implementation

If you read the sources, you should have noticed the high density of final
classes, members and methods. This is not an interface and besides the
error and interpreter classes, there is little to extend.

JSON is an application protocol, this is a final implementation in Java,
an API to use more and extend less.

Remember? 

It's all about less java for more application.

That why I used short names, to do the simplest:

    Object value = JSON.eval("null");
    
and the complex

    BigDecimal d = JSON.object(
        "{\"test\":[null,true,1,2.3,56789e-4]}", 2, 6
        ).arry("test").deci(4);

in one readable line, raising a practical JSON.Error that identify an
error in the instanciation and typed access to a network object model.
  
*/