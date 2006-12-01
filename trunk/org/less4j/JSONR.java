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

import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.regex.Pattern;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.text.StringCharacterIterator;

/**
 * Compile simple regular JSON patterns to evaluate and validate a JSON 
 * string against an extensible type model with built-in JSON types, numeric 
 * ranges, regular text, formated dates, typed collections, typed records and 
 * typed objects. 
 * 
 * <p>A safe JSON intepreter to evaluate and validate a UNICODE string 
 * as a limited tree of Java instances that matches a regular pattern
 * of types and values.</p> 
 *
 * <h3>JSON Regular</h3>
 * 
 * <p>JSON Regular (JSONR) is a protocol to define a JSON regular type
 * and value pattern <em>in</em> JSON.</p>
 * 
 * <p>For instance, the JSON object
 * 
 *<pre>{"width": 800, "height": 600}</pre>
 *
 * is is also a practical specification of any object with a non-negative 
 * integer "width" below or equal 800 and an integer "height" that is not 
 * smaller than zero and not greater than 600.</p>
 * 
 * <p>And this JSON array
 * 
 *<pre>[[800, 600, true, 0.0]]</pre>
 *
 * is quite a telling and concise expression to define a collection of 
 * records, a table of zero or more fixed length rows of typed values</p>
 * 
 * <p>JSONR is a <em>very</em> practical protocol, because it makes
 * the implementation of all its applications a lot simpler since a JSON 
 * interpreter can be reused to parse and compile the type tree. This is true 
 * in Java as it is in any language: of all the possible ways to specify a 
 * JSON model, this the easiest to implement everywhere it is needed.</p> 
 * 
 * <table BORDER="1" WIDTH="100%" CELLPADDING="3" CELLSPACING="0" SUMMARY="">
 * <tr><td>null</td><td>an undefined type and value</td></tr>
 * <tr><td>true</td><td>a boolean value, true or false</td></tr>
 * <tr><td>0</td><td>any integer value</td></tr>
 * <tr><td>0e+</td><td>a double value</td></tr>
 * <tr><td>0.0</td><td>any decimal value</td></tr>
 * <tr><td>""</td><td>any non empty string</td></tr>
 * <tr><td>[]</td><td>a list of undefined types an values</td></tr>
 * <tr><td>{}</td><td>an object of undefined names, types and values</td></tr>
 * </table>
 * 
 * <p>...</p>
 * 
 * <table BORDER="1" WIDTH="100%" CELLPADDING="3" CELLSPACING="0" SUMMARY="">
 * <tr><td>12</td><td>a non-negative integer lower than or equal 12</td></tr>
 * <tr><td>1e+3</td><td>a non-negative double value lower than or equal 1000</td></tr>
 * <tr><td>10.01</td><td>a non-negative decimal rounded to a two digits value 
 * lower than 10.01, or more practically defined, any two digit decimal between
 * 0 and 10</td></tr>
 * <tr><td>-1</td><td>an integer greater than -1</td></tr>
 * <tr><td>1e-3</td><td>a double greater than -1000</td></tr>
 * <tr><td>-9.99</td><td>a two digit decimal greater or equal than -9.99</td></tr>
 * <tr><td>"[a-z]*"</td><td>any string matching this regular expression</td></tr>
 * </table>
 * 
 * <p>...</p>
 * 
 * <table BORDER="1" WIDTH="100%" CELLPADDING="3" CELLSPACING="0" SUMMARY="">
 * <tr><td>[12]</td><td>a list of zero or more integers between 1 and 12</td></tr>
 * <tr><td>[1.0e-1]</td><td>a list of zero or more doubles above -1.0</td></tr>
 * <tr><td>[""]</td><td>a list of zero or more non-empty strings</td></tr>
 * <tr>
 * <td>["[a-z]*"]</td>
 * <td>a list of zero or more strings matching this regular expression</td></tr>
 * <tr>
 * <td>["[a-z]+", 0.0, "", true]</td>
 * <td>a fixed list of four typed instances, aka a row</td>
 * </tr>
 * <tr>
 * <td>[["[a-z]+", 0.0, "", true]]</td>
 * <td>a list of at least one row, aka a table</td>
 * </tr>
 * </table>
 * 
 * <p>...</p>
 * 
 * <h3>Synopsis</h3>
 * 
 * <p>To compile a JSONR pattern with the default set of extension types,
 * for instance a dummy constant model:
 * 
 * <blockquote>
 * <pre>JSONR pattern = new JSONR("["my model", "is a constant"]");</pre>
 * </blockquote>
 * 
 * that validates only the first JSON array evaluated:</p>
 * 
 * <blockquote>
 * <pre>try {
 *    Object value = pattern.eval("[\"my model\", \"is a constant\"]);
 *    Object value = pattern.eval("[\"will this fail?\", true]);
 *} catch (JSONR.Error e) {
 *    System.out.println(e.str());
 *}</pre>
 * </blockquote>
 * 
 * but will raise a <code>JSONR.Error</code> for the second.</p>
 * 
 * <h4>Regular Evaluation</h4>
 * 
 * <p>The <code>JSONR</code> class provides similar interfaces than 
 * <code>JSON</code> in the form of instance methods to evaluate and
 * validate JSON objects
 * 
 * <blockquote>
 * <pre>try {
 *    JSONR.O jsonr = new JSONR.O ("{\"pass\": null);
 *    JSON.O map = jsonr.eval(
 *        "{\"pass\": true}", 1, 2
 *        );
 *    JSON.A list = JSONR.A("[]").eval(
 *        "{\"will this fail?\": true}", 1, 2
 *        );
 *} catch (JSONR.Error e) {
 *    System.out.println(e.str());
 *}</pre>
 * </blockquote>
 * 
 * and arrays 
 * 
 * <blockquote>
 * <pre>try {
 *    ArrayList list = JSONR("[\".+\"]").array(
 *        "[\"my model\": \"is a constant\"]", 1, 2
 *        );
 *    ArrayList list = JSONR("[\".+\"]").array(
 *        "[\"will this fail?\", true]", 1, 2
 *        );
 *} catch (JSONR.Error e) {
 *    System.out.println(e.str());
 *}</pre>
 * </blockquote>
 * 
 * against a pattern, with safety limits.</p>
 * 
 * <p><b>Copyright</b> &copy; 2006 Laurent A.V. Szyster</p>
 * 
 * @version 0.20
 */
public class JSONR extends JSON {
    
    /**
     * A simple JSONR exception throwed for any type or value error found 
     * by the regular interpreter.
     * 
     * <h3>Synopsis</h3>
     * 
     * This class is a shallow copy of JSON.Error to distinguish between
     * a syntax and a regular error, allowing the interpreter to recover
     * valid JSON from an invalid JSONR (ie: to do error handling).
     * 
     * <blockquote>
     * <pre>String model = "[[true , \"[a-z]+\", null]]";
     * String string = "[[false, \"test\", 1.0][true, \"ERROR\" {}]]";
     * try {
     *    Object json = JSONR(model).eval(string)
     *} catch (JSONR.Error e) {
     *    System.out.println(e.jstr())
     *} catch (JSON.Error e) {
     *    System.out.println(e.jstr())
     *}</pre>
     * </blockquote>
     * 
     * <p><b>Copyright</b> &copy; 2006 Laurent A.V. Szyster</p>
     * 
     * @version 0.20
     */
    public static class Error extends JSON.Error {
        static final long serialVersionUID = 0L; // TODO: regenerate
        /**
         * Instanciate a JSONR error with an error message.
         * 
         * @param message the error message
         */
        public Error(String message) {super(message);}
    }
    
    private static final long serialVersionUID = 0L; 
    
    /**
     * An interface to extend JSONR with application types.
     * 
     * <h3>Synopsis</h3>
     * 
     * <p>Custom type classes must implement the <code>value</code>,
     * <code>eval</code> and <code>copy</code> methods:
     * 
     * <blockquote>
     * <pre>import org.less4j.JSONR;
     *import java.text.SimpleDateFormat;
     *
     *public static class TypeDateTime implements JSONR.Type {
     *
     *    private static final SimpleDateFormat format = 
     *        new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
     *    
     *    private static final String DATETIME_VALUE_ERROR = 
     *        "DateTime value error";
     *     
     *    public Object value (Object instance) throws JSONR.Error {
     *        return eval((String) JSONR.STRING.value(instance));
     *    }
     *    
     *    public Object eval (String string) throws JSONR.Error {
     *        try {
     *            string.replace('T', ' ');
     *            Calendar dt = Calendar.getInstance();
     *            dt.setTime(format.parse(string));
     *            return dt;
     *        } catch (Exception e) {
     *            throw new JSONR.Error(DATETIME_VALUE_ERROR);
     *        }
     *    }
     *    
     *    public static final JSONR.Type singleton = new TypeDateTime();
     *    
     *    public Type copy() {return singleton;}
     *    
     *}</pre>
     * </blockquote>
     * 
     * can be mapped to this name 
     * 
     * <blockquote>
     * <pre>"yyyy-MM-ddTHH:mm:ss"</pre>
     * </blockquote>
     * 
     * to cast a JSON string like
     * 
     * <blockquote>
     * <pre>"2006-07-04T12:08:56"</pre>
     * </blockquote>
     * 
     * into the appropriate <code>java.util.Date</code> instance.</p>
     * 
     * <p><b>Copyright</b> &copy; 2006 Laurent A.V. Szyster</p>
     * 
     * @version 0.20
     */
    public static interface Type {
        /**
         * This public <code>singleton</code> is <code>null</code> by
         * default and needs only to be assigned an instance of the
         * type class if you expect it to be reused in other types.
         */
        public final static Type singleton = null;
        /**
         * 
         * @param instance to validate as a regular type and value
         * @return a regular <code>Object</code>, or
         * @throws Error if the type or value is irregular
         */
        public Object value(Object instance) throws Error ;
        /**
         * Evaluate a JSON string <em>and</em> validate it as regular
         * 
         * @param string to evaluate as a regular type and value
         * @return a regular <code>Object</code>, or
         * @throws JSON.Error if the string is irregular
         */
        public Object eval(String string) throws JSON.Error;
        /**
         * Make a "deep" copy of the <code>Type</code>, something that
         * can safely be passed to a distinct thread.
         * 
         * <p>Note that is is only required by applications that expect to
         * alter their JSONR patterns after compilation, something quite
         * unsual. Applications that don't (the overwhelming majority)
         * can consider a <code>JSONR.Type</code> as thread-safe.</p>
         * 
         * @return an unsynchronized copy as a <code>Type</code> 
         */
        public Type copy();
    } // at last some use for java interfaces ;-)
    
    // the built-in singletons
    
    protected static final class TypeUndefined implements Type {
        public static final TypeUndefined singleton = new TypeUndefined();
        public Object value (Object instance) {
            return instance;
            }
        public Object eval (String string) {
            if (string.equals(JSON._null))
                return null;
            else
                return string;
            }
        public Type copy() {return singleton;}
    }

    protected static final class TypeBoolean implements Type {
        protected static final String BOOLEAN_VALUE_ERROR = 
            "Boolean value error";
        public static final TypeBoolean singleton = new TypeBoolean();
        public final Object value (Object instance) throws Error {
            if (instance instanceof Boolean)
                return instance;
            else
                throw new Error(JSON.BOOLEAN_TYPE_ERROR);
        }
        public final Object eval (String string) throws JSON.Error {
            if (string.equals(JSON._true))
                return Boolean.TRUE;
            else if (string.equals(JSON._false))
                return Boolean.FALSE;
            else
                throw new Error(BOOLEAN_VALUE_ERROR);
        }
        public final Type copy() {return singleton;}
    }

    protected static final class TypeInteger implements Type {
        protected static final String BIGINTEGER_VALUE_ERROR = 
            "BigInteger value error";
        public static final TypeInteger singleton = new TypeInteger();
        public final Object value (Object instance) throws Error {
            if (instance instanceof BigInteger)
                return instance;
            else
                throw new Error(JSON.BIGINTEGER_TYPE_ERROR);
        }
        public final Object eval (String string) throws JSON.Error {
            if (string != null) {
                try {
                    return new BigInteger(string);
                } catch (Exception e) {
                    throw new Error(BIGINTEGER_VALUE_ERROR);
                }
            } else
                throw new Error(BIGINTEGER_VALUE_ERROR);
        }
        public final Type copy() {return singleton;}
    }

    protected static final class TypeDouble implements Type {
        protected static final String DOUBLE_VALUE_ERROR = 
            "Double value error";
        public static final TypeDouble singleton = new TypeDouble();
        public final Object value (Object instance) throws Error {
            if (instance instanceof Double)
                return instance;
            else if (instance instanceof Number)
                return new Double(((Number) instance).doubleValue());
            else
                throw new Error(JSON.DOUBLE_TYPE_ERROR);
        }
        public final Object eval (String string) throws JSON.Error {
            if (string != null) {
                return new Double(string);
            } else
                throw new Error(DOUBLE_VALUE_ERROR);
        }
        public final Type copy() {return singleton;}
    }

    protected static final class TypeDecimal implements Type {
        protected static final String DOUBLE_VALUE_ERROR = 
            "BigDecimal value error";
        public final Object value (Object instance) throws Error {
            BigDecimal b;
            if (instance instanceof BigDecimal) {
                b = (BigDecimal) instance;
            } else if (instance instanceof Number) {
                b = new BigDecimal(((Number) instance).doubleValue());
            } else
                throw new Error(JSON.DOUBLE_TYPE_ERROR);
            return b;
        }
        public final Object eval (String string) throws JSON.Error {
            if (string != null) {
                return (new BigDecimal(string));
            } else
                throw new Error(DOUBLE_VALUE_ERROR);
        }
        public static final Type singleton = new TypeDecimal();
        public final Type copy() {return singleton;}
    }

    protected static final class TypeString implements Type {
        protected static final String STRING_VALUE_ERROR = 
            "String value error";
        public static final TypeString singleton = new TypeString();
        public final Object value (Object instance) throws Error {
            if (instance instanceof String) {
                String s = (String) instance;
                if (s.length() > 0)
                    return instance;
                else
                    throw new Error(STRING_VALUE_ERROR);
            } else
                throw new Error(JSON.STRING_TYPE_ERROR);
        }
        public final Object eval (String string) throws JSON.Error {
            if (string == null || string.length() == 0) 
                throw new Error(STRING_VALUE_ERROR);
            else
                return string;
        }
        public final Type copy() {return singleton;}
    }
    
    protected static final class TypeRegexp implements Type {
        protected static final String IRREGULAR_STRING = 
            "irregular String";
        protected Pattern pattern = null;
        protected TypeRegexp (Pattern pattern) {
            this.pattern = pattern;
        } 
        public TypeRegexp (String expression) {
            pattern = Pattern.compile(expression);
        } 
        protected final Object test (String string) throws Error {
            if (pattern.matcher(string).matches())
                return string;
            else
                throw new Error(IRREGULAR_STRING);
        }
        public final Object value (Object instance) throws Error {
            if (instance instanceof String) {
                return this.test((String) instance);
            } else
                throw new Error(JSON.STRING_TYPE_ERROR);
        }
        public final Object eval (String string) throws JSON.Error {
            if (string != null)
                return this.test(string);
            else
                return null;
        }
        public Type copy() {return new TypeRegexp(pattern);}
    }
    
    protected static final class TypeArray implements Type {
        public Type[] types = null;
        public TypeArray (Type[] types) {this.types = types;}
        public final Object value (Object instance) throws Error {
            if (instance == null || instance instanceof ArrayList)
                return instance;
            else
                throw new Error(JSON.ARRAY_TYPE_ERROR);
            }
        public final Object eval (String string) throws JSON.Error {
            return (new JSONR(this)).eval(string);
        }
        protected final Iterator iterator() {
            return new Simple.ObjectIterator(types);
            }
        public static final Type singleton = new TypeArray(new Type[]{});
        public final Type copy() {
            if (this == singleton) 
                return singleton;
            else
                return new TypeArray(types);
            }
    }
    
    protected static final class TypeObject implements Type {
        protected static final String IRREGULAR_OBJECT = 
            "irregular Object";
        public Set names;
        public HashMap namespace;
        public TypeObject (HashMap ns) {
            namespace = ns;
            names = ns.keySet();
            }
        public final Object value (Object instance) throws Error {
            if (instance == null)
                return null;
            else if (instance instanceof HashMap) {
                HashMap map = (HashMap) instance;
                if (map.keySet().containsAll(names))
                    return map;
                else
                    throw new Error(IRREGULAR_OBJECT);
            } else
                throw new Error(JSON.OBJECT_TYPE_ERROR);
        }
        public final Object eval (String string) throws JSON.Error {
            return (new JSONR(this)).eval(string);
        }
        public final JSON.O filterQuery(Map query) {
            Type type;
            String name;
            String[] strings;
            JSON.O o = new JSON.O();
            Iterator iter = query.keySet().iterator();
            while (iter.hasNext()) {
                name = (String) iter.next();
                type = (Type) namespace.get(name);
                strings = (String[]) query.get(name);
                if (type != null && strings != null && strings.length > 0) 
                    try {
                        o.put(name, type.eval(strings[0]));
                    } catch (JSON.Error e) {;}
            }
            return o;
        }
        public final JSON.O matchQuery(Map query) throws JSON.Error {
            Type type;
            String name;
            String[] strings;
            JSON.O o = new JSON.O();
            Iterator iter = query.keySet().iterator();
            while (iter.hasNext()) {
                name = (String) iter.next();
                type = (Type) namespace.get(name);
                strings = (String[]) query.get(name);
                if (type != null && strings != null && strings.length > 0)
                    o.put(name, type.eval(strings[0]));
            }
            return o;
        }
        public static final Type singleton = new TypeObject(new JSON.O());
        public final Type copy() {
            if (this == singleton) return singleton;
            
            String name;
            JSON.O o = new JSON.O();
            Iterator iter = namespace.keySet().iterator();
            while (iter.hasNext()) {
                name = (String) iter.next();
                o.put(name, ((Type) namespace.get(name)).copy());
            }
            return new TypeObject(o);
        }
    }
    
    public static final Type BOOLEAN = TypeBoolean.singleton;
    public static final Type INTEGER = TypeInteger.singleton;
    public static final Type DOUBLE = TypeDouble.singleton;
    public static final Type DECIMAL = TypeDecimal.singleton;
    public static final Type STRING = TypeString.singleton;
    protected static final Type ARRAY = TypeArray.singleton;
    protected static final Type OBJECT = TypeArray.singleton;
    
    // the built-in extension types: just JSON's DateTime, ymmv ...
    
    protected static final class TypeDateTime implements JSONR.Type {
        public static final String name = "DateTime"; 
        protected static final SimpleDateFormat format = 
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        protected static final String DATETIME_VALUE_ERROR = 
            "DateTime value error";
        public final Object value (Object instance) throws JSONR.Error {
            return eval((String) STRING.value(instance));
        }
        public final Object eval (String string) throws JSONR.Error {
            try {
                string.replace('T', ' ');
                Calendar dt = Calendar.getInstance();
                dt.setTime(format.parse(string));
                return dt;
            } catch (Exception e) {
                throw new JSONR.Error(DATETIME_VALUE_ERROR);
            }
        }
        public static final Type singleton = new TypeDateTime();
        public final Type copy() {return singleton;}        
    }
    
    /**
     * Cast java.util.Calendar objects from JSON DateTime strings.
     */
    public static final Type DATETIME = TypeDateTime.singleton;
    
    /**
     * <p>The built-in extension types, currently mapping only one
     * bizarre yet relevant and unambiguous type name, the obvious:
     * 
     * <pre>"yyyy-MM-ddTHH:mm:ss"</pre>
     * 
     * to the <code>JSONR.DATETIME</code> type.</p>
     * 
     */
    public static HashMap TYPES = new HashMap();
    static {
        TYPES.put("yyyy-MM-ddTHH:mm:ss", DATETIME);
    }
    
    // the built-in regular numeric types
    
    protected static final class TypeIntegerAbsolute implements Type {
        private static final String POSITIVE_INTEGER_OVERFLOW = 
            "positive integer overflow";
        private static final String NEGATIVE_INTEGER = 
            "negative integer";
        BigInteger limit;
        public TypeIntegerAbsolute (BigInteger gt) {this.limit = gt;}
        protected final Object test (BigInteger i) throws Error {
            if (i.compareTo(BigInteger.ZERO) < 0)
                throw new Error(NEGATIVE_INTEGER);
            else if (i.compareTo(limit) <= 0)
                return i;
            else
                throw new Error(POSITIVE_INTEGER_OVERFLOW);
        } 
        public final Object value (Object instance) throws Error {
            return test((BigInteger) INTEGER.value(instance));
        }
        public final Object eval (String string) throws JSON.Error {
            return test((BigInteger) INTEGER.eval(string));
        }
        public final Type copy() {return new TypeIntegerAbsolute(limit);}
    }

    protected static final class TypeIntegerRelative implements Type {
        private static final String INTEGER_OVERFLOW = 
            "integer overflow";
        BigInteger limit;
        public TypeIntegerRelative (BigInteger gt) {this.limit = gt.abs();}
        protected final Object test (BigInteger i) throws Error {
            if (i.abs().compareTo(limit) < 0)
                return i;
            else
                throw new Error(INTEGER_OVERFLOW);
        } 
        public final Object value (Object instance) throws Error {
            return test((BigInteger) INTEGER.value(instance));
        }
        public final Object eval (String string) throws JSON.Error {
            return test((BigInteger) INTEGER.eval(string));
        }
        public final Type copy() {return new TypeIntegerRelative(limit);}
    }

    private static final Double _double_zero = new Double(0.0);
    
    protected static final class TypeDoubleAbsolute implements Type {
        private static final String POSITIVE_DOUBLE_OVERFLOW = 
            "positive double overflow";
        private static final String NEGATIVE_DOUBLE = 
            "negative double";
        Double limit;
        public TypeDoubleAbsolute (Double gt) {this.limit = gt;}
        protected final Object test (Double d) throws Error {
            if (d.compareTo(_double_zero) < 0)
                throw new Error(NEGATIVE_DOUBLE);
            else if (d.compareTo(limit) <= 0)
                return d;
            else
                throw new Error(POSITIVE_DOUBLE_OVERFLOW);
        } 
        public final Object value (Object instance) throws Error {
            return test((Double) DOUBLE.value(instance));
        }
        public final Object eval (String string) throws JSON.Error {
            return test((Double) DOUBLE.eval(string));
        }
        public final Type copy() {return new TypeDoubleAbsolute(limit);}
    }
    
    protected static final class TypeDoubleRelative implements Type {
        private static final String DOUBLE_OVERFLOW = 
            "double overflow";
        double limit;
        public TypeDoubleRelative (double v) {
            this.limit = Math.abs(v);
            }
        protected final Object test (Double d) throws Error {
            if (Math.abs(d.doubleValue()) <= limit)
                return d;
            else
                throw new Error(DOUBLE_OVERFLOW);
        } 
        public final Object value (Object instance) throws Error {
            return test((Double) DOUBLE.value(instance));
        }
        public final Object eval (String string) throws JSON.Error {
            return test((Double) DOUBLE.eval(string));
        }
        public final Type copy() {return new TypeDoubleRelative(limit);}
    }
    
    private static final BigDecimal _decimal_zero = BigDecimal.valueOf(0);
    
    protected static final class TypeDecimalAbsolute implements Type {
        private static final String POSITIVE_DECIMAL_OVERFLOW = 
            "positive decimal overflow";
        private static final String NEGATIVE_DECIMAL = 
            "negative decimal";
        BigDecimal limit;
        int scale;
        public TypeDecimalAbsolute (BigDecimal lt) {
            limit = lt;
            scale = limit.scale(); 
        } 
        protected final Object test (BigDecimal b) throws Error {
            b.setScale(scale);
            if (b.compareTo(_decimal_zero) < 0)
                throw new Error(NEGATIVE_DECIMAL);
            else if (b.compareTo(limit) < 0)
                return b;
            else
                throw new Error(POSITIVE_DECIMAL_OVERFLOW);
        }
        public final Object value (Object instance) throws Error {
            return test((BigDecimal) DECIMAL.value(instance));
        }
        public final Object eval (String string) throws JSON.Error {
            return test((BigDecimal) DECIMAL.eval(string));
        }
        public final Type copy() {return new TypeDecimalAbsolute(limit);}
    }
    
    protected static final class TypeDecimalRelative implements Type {
        private static final String DECIMAL_OVERFLOW = 
            "decimal overflow";
        BigDecimal limit;
        int scale;
        public TypeDecimalRelative (BigDecimal gt) {
            limit = gt;
            scale = limit.scale(); 
        } 
        protected final Object test (BigDecimal b) throws Error {
            b.setScale(scale);
            if (b.abs().compareTo(limit) > 0)
                return b;
            else
                throw new Error(DECIMAL_OVERFLOW);
        }
        public final Object value (Object instance) throws Error {
            return test((BigDecimal) DECIMAL.value(instance));
        }
        public final Object eval (String string) throws JSON.Error {
            return test((BigDecimal) DECIMAL.eval(string));
        }
        public Type copy() {return new TypeDecimalRelative(limit);}
    }
    
    protected static final Type compile(
        Object regular, Map extensions, HashMap cache
        ) {
        String pattern = JSON.str(regular);
        if (cache.containsKey(pattern))
            return (Type) cache.get(pattern);
        
        Type type;
        if (regular == null) {
            type = TypeUndefined.singleton;
        } else if (regular instanceof Boolean) {
            type = BOOLEAN;
        } else if (regular instanceof String) {
            String s = (String) regular;
            if (extensions != null && extensions.containsKey(s))
                type = (Type) extensions.get(s);
            else if (s.length() > 0)
                type = new TypeRegexp(s);
            else
                type = STRING;
        } else if (regular instanceof BigInteger) {
            BigInteger i = (BigInteger) regular;
            int cmpr = i.compareTo(BigInteger.ZERO); 
            if (cmpr == 0)
                type = INTEGER;
            else if (cmpr > 0)
                type = new TypeIntegerAbsolute(i);
            else
                type = new TypeIntegerRelative(i);
        } else if (regular instanceof Double) {
            Double d = (Double) regular;
            int cmpr = d.compareTo(_double_zero); 
            if (cmpr == 0)
                type = DOUBLE;
            else if (cmpr > 0)
                type = new TypeDoubleAbsolute(d);
            else
                type = new TypeDoubleRelative(d.doubleValue());
        } else if (regular instanceof BigDecimal) {
            BigDecimal b = (BigDecimal) regular;
            int cmpr = b.compareTo(_decimal_zero); 
            if (cmpr == 0)
                type = new TypeDecimal();
            else if (cmpr > 0)
                type = new TypeDecimalAbsolute(b);
            else
                type = new TypeDecimalRelative(b);
        } else if (regular instanceof ArrayList) {
            ArrayList array = (ArrayList) regular;
            int l = array.size();
            if (l > 0) {
                Type[] types = new Type[l];
                for (int i=0; i<l; i++) 
                    types[i] = compile(array.get(i), extensions, cache);
                type = new TypeArray(types);
            } else 
                type = TypeArray.singleton;
        } else if (regular instanceof HashMap) {
            HashMap object = (HashMap) regular;
            Iterator iter = object.keySet().iterator();
            if (!iter.hasNext())
                type = TypeObject.singleton;
            else {
                String name;
                HashMap namespace = new HashMap();
                do {
                    name = (String) iter.next();
                    namespace.put(name, compile(
                        object.get(name), extensions, cache
                        ));
                } while (iter.hasNext());
                type = new TypeObject(namespace);
            } 
        } else
            type = null;
        cache.put(pattern, type);
        return type;
    }
    
    public static final Type compile(Object regular, Map extensions) {
        return compile(regular, extensions, new HashMap());
    }
    
    protected static final String IRREGULAR_ARRAY = 
        "irregular array";
    protected static final String PARTIAL_ARRAY = 
        "partial array";
    protected static final String ARRAY_OVERFLOW = 
        "array overflow";
    protected static final String NAME_ERROR = 
        "name error";

    public Type type = null;
    
    public JSONR(Type type) {super(); this.type = type;}
    
    public JSONR(Type type, int containers, int iterations) {
        super(containers, iterations); this.type = type;
    }
    
    public JSONR(Object regular) throws JSON.Error {
        super(); type = compile(regular, TYPES);
    }
    
    public JSONR(Object regular, int containers, int iterations) 
    throws JSON.Error {
        super(containers, iterations); type = compile(regular, TYPES);
    }
    
    public JSONR(String pattern) throws JSON.Error {
        super(); type = compile((new JSON()).eval(pattern), TYPES);
    }
    
    public JSONR(String pattern, int containers, int iterations) 
    throws JSON.Error {
        super(containers, iterations); 
        type = compile((new JSON()).eval(pattern), TYPES);
    }
    
    public Object eval(String json) 
    throws JSON.Error {
        buf = new StringBuffer();
        it = new StringCharacterIterator(json);
        try {
            c = it.first();
            return value(type);
        } finally {
            buf = null;
            it = null;
        }
    }
    
    public JSON.Error update(Map o, String json) {
        if (!(type instanceof TypeObject))
            return new Error(JSON.OBJECT_TYPE_ERROR);
        
        TypeObject to = (TypeObject) type;
        buf = new StringBuffer();
        it = new StringCharacterIterator(json);
        try {
            c = it.first();
            while (Character.isWhitespace(c)) c = it.next();
            if (c == '{') {
                c = it.next();
                to.value(object(o, to.namespace));
                return null;
            } else
                return error(JSON.OBJECT_TYPE_ERROR);
        } catch (JSON.Error e){
            return e;
        } finally {
            buf = null;
            it = null;
        }
    }
    
    public JSON.Error extend(List a, String json) {
        if (!(type instanceof TypeArray))
            return new Error(JSON.ARRAY_TYPE_ERROR);
        
        buf = new StringBuffer();
        it = new StringCharacterIterator(json);
        try {
            c = it.first();
            while (Character.isWhitespace(c)) c = it.next();
            if (c == '[') {
                c = it.next();
                array(a, ((TypeArray) type).iterator());
                return null;
            } else
                return error(JSON.ARRAY_TYPE_ERROR);
        } catch (JSON.Error e){
            return e;
        } finally {
            buf = null;
            it = null;
        }
    }
    
    protected final Object value(Type type) 
    throws JSON.Error {
        while (Character.isWhitespace(c)) c = it.next();
        switch(c){
        case '{': {
            if (type instanceof TypeObject) {
                TypeObject to = (TypeObject) type;
                c = it.next();
                return to.value(object(new JSON.O(), to.namespace));
            } else if (type == TypeUndefined.singleton) {
                c = it.next();
                return object(new JSON.O());
            } else
                throw error(TypeObject.IRREGULAR_OBJECT);
        }
        case '[': {
            if (type instanceof TypeArray) { 
                c = it.next(); 
                Iterator types = ((TypeArray) type).iterator();
                if (types.hasNext())
                    return array(new JSON.A(), types);
                else
                    return array(new JSON.A());
            } else if (type == TypeUndefined.singleton) {
                c = it.next();
                return array(new JSON.A());
            } else 
                throw error(IRREGULAR_ARRAY);
        }
        case '"': {c = it.next(); return type.value(string());}
        case '0': case '1': case '2': case '3': case '4':  
        case '5': case '6': case '7': case '8': case '9': 
        case '-': {
            return type.value(number());
        }
        case 't': {
            if (next('r') && next('u') && next('e')) {
                c = it.next(); return type.value(Boolean.TRUE);
            } else
                throw error(TRUE_EXPECTED);
        }
        case 'f': {
            if (next('a') && next('l') && next('s') && next('e')) {
                c = it.next(); return type.value(Boolean.FALSE);
            } else
                throw error(FALSE_EXPECTED);
        }
        case 'n': {
            if (next('u') && next('l') && next('l')) {
                c = it.next(); return type.value(null);
            } else
                throw error(NULL_EXPECTED);
        }
        case ',': {c = it.next(); return COMMA;} 
        case ':': {c = it.next(); return COLON;}
        case ']': {c = it.next(); return ARRAY;} 
        case '}': {c = it.next(); return OBJECT;}
        case JSON._done:
            throw error(UNEXPECTED_END);
        default: 
            throw error(UNEXPECTED_CHARACTER);
        }
    }
    
    protected final Object value(Type type, String name) 
    throws JSON.Error {
        try {
            return value(type);
        } catch (Error e) {
            e.jsonIndex = it.getIndex();
            e.jsonPath.add(0, name);
            throw e;
        } catch (JSON.Error e) {
            e.jsonPath.add(0, name);
            throw e;
        }
    }
    
    protected final Object value(Type type, int index) 
    throws JSON.Error {
        try {
            return value(type);
        } catch (Error e) {
            e.jsonIndex = it.getIndex();
            e.jsonPath.add(0, BigInteger.valueOf(index));
            throw e;
        } catch (JSON.Error e) {
            e.jsonPath.add(0, BigInteger.valueOf(index));
            throw e;
        }
    }
    
    protected final Object object(Map o, HashMap namespace) 
    throws JSON.Error {
        if (--containers < 0) 
            throw error(CONTAINERS_OVERFLOW);
        
        Type type;
        String name; 
        Object val;
        Object token = value();
        while (token != OBJECT) {
            if (!(token instanceof String))
                throw error(STRING_EXPECTED);
            
            if (--iterations < 0) 
                throw error(ITERATIONS_OVERFLOW);
            
            name = (String) token;
            type = (Type) namespace.get(name);
            if (value() == COLON) {
                if (type == null)
                    // throw new Error(NAME_ERROR);
                    val = value(name);
                else
                    val = value(type, name);
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
    
    protected final Object array(List a, Iterator types) 
    throws JSON.Error {
        if (--containers < 0) 
            throw error(CONTAINERS_OVERFLOW);

        int i = 0;
        Type type = (Type) types.next();
        Object token = value(type, i++);
        if (types.hasNext()) {
            while (token != ARRAY) {
                if (token==COLON || token==COMMA || token==OBJECT)
                    throw error(VALUE_EXPECTED);
                
                if (--iterations < 0) 
                    throw error(ITERATIONS_OVERFLOW);
             
                a.add(token);
                token = value(); 
                if (token == COMMA)
                    if (types.hasNext())
                        token = value((Type) types.next(), i++);
                    else
                        throw new Error(ARRAY_OVERFLOW);
            }
            if (types.hasNext())
                throw error(PARTIAL_ARRAY);
        } else {
            while (token != ARRAY) {
                if (token==COLON || token==COMMA || token==OBJECT)
                    throw error(VALUE_EXPECTED);
                
                if (--iterations < 0) 
                    throw error(ITERATIONS_OVERFLOW);
             
                a.add(token);
                token = value(); 
                if (token == COMMA)
                    token = value(type, i++);
            }
        }
        return a;
    }
    
}

/* Note about this implementation

JSONR could be developped much further, for instance to collect all type
and value error from a valid JSON string or to support a wider range of
extension types.

However, the simplicity of the protocol is what guarantees that a all
implementations are practically within the reach of any JSON capable
environment. 

The value of JSON is more than what it can bring to a java web controller
in terms of input validation, because it can also as easely be implemented 
for other applications of an object model definition. In JavaScript, the
obvious candidate, that would be input screen HTML generation from a model,
complete with interactive input validation.

*/