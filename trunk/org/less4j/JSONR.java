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
import java.util.Set;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
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
 * <h3>JSON Regular</h3>
 * 
 * <p>The whole idea behing JSON Regular is to define a type model
 * <em>in</em> JSON, by interpreting an instance as a pattern for the
 * validation.</p>
 * 
 * <p>For instance
 * 
 *<pre>{"width": 800, "height": 600}</pre>
 *
 * is an object with two named numeric properties of integer type.</p>
 * 
 * <p>But it is also a practical specification of any object with
 * a non-negative integer "width" below or equal 800 and an integer
 * "height" that is not smaller than zero and not greater than 600.</p>
 * 
 * <p>And it is a <em>very</em> practical specification, because it makes
 * the implementation of all its applications a lot more shorter: there is
 * no additionnal meta-model with its grammar needed, the JSON interpreter
 * can be reused to build the type tree and derived to develop JSONR's
 * interpreter.</p>
 * 
 * <p>This is true in Java as it is in any language: all possible way to
 * specify a JSON model, JSONR is the easiest type model to implement 
 * everywhere where it is needed.</p> 
 * 
 * <p>This works quite well to specify typed values like boolean,
 * 
 * <pre>{"flag": 0.05, "value": 999.99}</pre>
 * 
 * and numeric ranges for double and decimal numbers too:
 * 
 * <pre>{"rebate": 0.05, "value": 999.99}</pre>
 * 
 * <table BORDER="1" WIDTH="100%" CELLPADDING="3" CELLSPACING="0" SUMMARY="">
 * <tr><td>null</td><td>an undefined type and value</td></tr>
 * <tr><td>true</td><td>a boolean value, true or false</td></tr>
 * <tr><td>0</td><td>any integer value</td></tr>
 * <tr><td>0e+</td><td>any double value</td></tr>
 * <tr><td>0.0</td><td>any number rounded to a precision of one decimal</td></tr>
 * <tr><td>10.1</td><td>a one digit decimal value lower than 10.1</td></tr>
 * <tr><td>""</td><td>any non empty string</td></tr>
 * <tr><td>[]</td><td>a list of undefined types an values</td></tr>
 * <tr><td>{}</td><td>an object of undefined names, types and values</td></tr>
 * </table>
 * 
 * <p>...</p>
 * 
 * <table BORDER="1" WIDTH="100%" CELLPADDING="3" CELLSPACING="0" SUMMARY="">
 * <tr><td>12</td><td>a positive integer lower than or equal 12</td></tr>
 * <tr><td>-1</td><td>an integer greater than -1</td></tr>
 * <tr><td>-1.0</td><td>a decimal greater than -1.0, maybe rounded</td></tr>
 * <tr><td>-9.99</td><td>a two digit greater or equal than -9.99</td></tr>
 * <tr><td>1.0e+</td><td>a positive double lower than or equal 1.0</td></tr>
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
 * <p><b>Copyright</b> &copy; 2006 Laurent A.V. Szyster</p>
 * 
 * @author Laurent Szyster
 * @version 0.1.0
 */
public class JSONR {
    
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
     *    System.out.println(e.jsonError())
     *} catch (JSON.Error e) {
     *    System.out.println(e.jsonError())
     *}</pre>
     * </blockquote>
     * 
     * <p><b>Copyright</b> &copy; 2006 Laurent A.V. Szyster</p>
     * 
     * @author Laurent Szyster
     * @version 0.1.0
     */
    public static class Error extends JSON.Error {
        private static final long serialVersionUID = 0L;
        /**
         * Instanciate a JSONR error with an error message.
         * 
         * @param message the error message
         */
        public Error(String message) {super(message);}
    }
    
    private static final long serialVersionUID = 0L; 
    
    /**
     * The only interface in all less4j, made public to extend JSONR with 
     * application types.
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
     * <pre>"yyyyMMddHHmmssZ"</pre>
     * </blockquote>
     * 
     * to cast a JSON string like
     * 
     * <blockquote>
     * <pre>"20060704120856-0700"</pre>
     * </blockquote>
     * 
     * into the appropriate <code>java.util.Date</code> instance.</p>
     * 
     * @author Laurent Szyster
     *
     */
    public static interface Type {
        public final static Type singleton = null;
        public Object value(Object instance) throws Error ;
        public Object eval(String string) throws JSON.Error;
        public Type copy();
    } // at last some use for java interfaces ;-)
    
    // the built-in singletons
    
    protected static class TypeUndefined implements Type {
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

    protected static class TypeBoolean implements Type {
        protected static final String NOT_A_BOOLEAN_VALUE = 
            "not a boolean value";
        protected static final String NOT_A_BOOLEAN_TYPE = 
            "not a Boolean type";
        public static final TypeBoolean singleton = new TypeBoolean();
        public Object value (Object instance) throws Error {
            if (instance instanceof Boolean)
                return instance;
            else
                throw new Error(NOT_A_BOOLEAN_TYPE);
        }
        public Object eval (String string) throws JSON.Error {
            if (string.equals(JSON._true))
                return Boolean.TRUE;
            else if (string.equals(JSON._false))
                return Boolean.FALSE;
            else
                throw new Error(NOT_A_BOOLEAN_VALUE);
        }
        public Type copy() {return singleton;}
    }

    protected static class TypeInteger implements Type {
        protected static final String NOT_AN_INTEGER_VALUE = 
            "not an integer value";
        protected static final String NOT_AN_INTEGER_TYPE = 
            "not an integer type";
        public static final TypeInteger singleton = new TypeInteger();
        public Object value (Object instance) throws Error {
            if (instance instanceof BigInteger)
                return instance;
            else
                throw new Error(NOT_AN_INTEGER_TYPE);
        }
        public Object eval (String string) throws JSON.Error {
            if (string != null) {
                try {
                    return new BigInteger(string);
                } catch (Exception e) {
                    throw new Error(NOT_AN_INTEGER_VALUE);
                }
            } else
                throw new Error(NOT_AN_INTEGER_VALUE);
        }
        public Type copy() {return singleton;}
    }

    protected static class TypeDouble implements Type {
        protected static final String NOT_A_DOUBLE_VALUE = 
            "not a double value";
        protected static final String NOT_A_DOUBLE_TYPE = 
            "not a double type";
        public static final TypeDouble singleton = new TypeDouble();
        public Object value (Object instance) throws Error {
            if (instance instanceof Double)
                return instance;
            else if (instance instanceof Number)
                return new Double(((Number) instance).doubleValue());
            else
                throw new Error(NOT_A_DOUBLE_TYPE);
        }
        public Object eval (String string) throws JSON.Error {
            if (string != null) {
                return new Double(string);
            } else
                throw new Error(NOT_A_DOUBLE_VALUE);
        }
        public Type copy() {return singleton;}
    }

    protected static class TypeDecimal implements Type {
        protected static final String NOT_A_DECIMAL_VALUE = 
            "not a decimal value";
        protected static final String NOT_A_DECIMAL_TYPE = 
            "not a decimal type";
        public Object value (Object instance) throws Error {
            BigDecimal b;
            if (instance instanceof BigDecimal) {
                b = (BigDecimal) instance;
            } else if (instance instanceof Number) {
                b = new BigDecimal(((Number) instance).doubleValue());
            } else
                throw new Error(NOT_A_DECIMAL_TYPE);
            return b;
        }
        public Object eval (String string) throws JSON.Error {
            if (string != null) {
                return (new BigDecimal(string));
            } else
                throw new Error(NOT_A_DECIMAL_VALUE);
        }
        public static final Type singleton = new TypeDecimal();
        public Type copy() {return singleton;}
    }

    protected static class TypeString implements Type {
        protected static final String NULL_STRING_VALUE = 
            "null string value";
        protected static final String NOT_A_STRING_TYPE = 
            "not a string type";
        public static final TypeString singleton = new TypeString();
        public Object value (Object instance) throws Error {
            if (instance instanceof String) {
                String s = (String) instance;
                if (s.length() > 0)
                    return instance;
                else
                    throw new Error(NULL_STRING_VALUE);
            } else
                throw new Error(NOT_A_STRING_TYPE);
        }
        public Object eval (String string) throws JSON.Error {
            if (string == null || string.length() == 0) 
                throw new Error(NULL_STRING_VALUE);
            else
                return string;
        }
        public Type copy() {return singleton;}
    }
    
    protected static class TypeRegular implements Type {
        protected static final String IRREGULAR_STRING_VALUE = 
            "irregular string value";
        protected Pattern pattern = null;
        protected TypeRegular (Pattern pattern) {
            this.pattern = pattern;
        } 
        public TypeRegular (String expression) {
            pattern = Pattern.compile(expression);
        } 
        protected Object test (String string) throws Error {
            if (pattern.matcher(string).matches())
                return string;
            else
                throw new Error(IRREGULAR_STRING_VALUE);
        }
        public Object value (Object instance) throws Error {
            if (instance instanceof String) {
                return this.test((String) instance);
            } else
                throw new Error(TypeString.NOT_A_STRING_TYPE);
        }
        public Object eval (String string) throws JSON.Error {
            if (string != null)
                return this.test(string);
            else
                return null;
        }
        public Type copy() {return new TypeRegular(pattern);}
    }
    
    protected static class TypeArray implements Type {
        protected static final String NOT_AN_ARRAY_TYPE = 
            "not an array type";
        public Type[] types = null;
        public TypeArray (Type[] types) {this.types = types;}
        public Object value (Object instance) throws Error {
            if (instance == null || instance instanceof ArrayList)
                return instance;
            else
                throw new Error(NOT_AN_ARRAY_TYPE);
            }
        public Object eval (String string) throws JSON.Error {
            ArrayList list = new ArrayList();
            JSON.Error e = (new Interpreter()).extend(list, string, this);
            if (e == null) 
                return list;
            else
                throw e;
        }
        public Iterator iterator() {return new Simple.ObjectIterator(types);}
        public static final Type singleton = new TypeArray(new Type[]{});
        public Type copy() {
            if (this == singleton) 
                return singleton;
            else
                return new TypeArray(types);
            }
    }
    
    protected static class TypeObject implements Type {
        protected static final String NOT_AN_OBJECT_TYPE = 
            "not an object type";
        public Set names;
        public HashMap namespace;
        public TypeObject (HashMap ns) {
            namespace = ns;
            names = ns.keySet();
            }
        public Object value (Object instance) throws Error {
            if (instance == null || instance instanceof HashMap)
                return instance;
            else
                throw new Error(NOT_AN_OBJECT_TYPE);
            }
        public Object eval (String string) throws JSON.Error {
            HashMap map = new HashMap();
            JSON.Error e = (new Interpreter()).update(map, string, this);
            if (e == null) 
                return map;
            else
                throw e;
        }
        public static final Type singleton = new TypeObject(new HashMap());
        public Type copy() {
            if (this == singleton) return singleton;
            
            String name;
            HashMap map = new HashMap();
            Iterator iter = namespace.keySet().iterator();
            while (iter.hasNext()) {
                name = (String) iter.next();
                map.put(name, ((Type) namespace.get(name)).copy());
            }
            return new TypeObject(map);
        }
    }
    
    public static final Type BOOLEAN = TypeBoolean.singleton;
    public static final Type INTEGER = TypeInteger.singleton;
    public static final Type DOUBLE = TypeDouble.singleton;
    public static final Type DECIMAL = TypeDecimal.singleton;
    public static final Type STRING = TypeString.singleton;
    public static final Type ARRAY = TypeArray.singleton;
    public static final Type OBJECT = TypeArray.singleton;
    
    // the built-in extension types: just JSON's DateTime, ymmv ...
    
    protected static class TypeDateTime implements JSONR.Type {
        public static final String name = "DateTime"; 
        protected static final SimpleDateFormat format = 
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        protected static final String NOT_A_DATETIME_VALUE = 
            "not a DateTime value";
        public Object value (Object instance) throws JSONR.Error {
            return eval((String) STRING.value(instance));
        }
        public Object eval (String string) throws JSONR.Error {
            try {
                string.replace('T', ' ');
                Calendar dt = Calendar.getInstance();
                dt.setTime(format.parse(string));
                return dt;
            } catch (Exception e) {
                throw new JSONR.Error(NOT_A_DATETIME_VALUE);
            }
        }
        public static final Type singleton = new TypeDateTime();
        public Type copy() {return singleton;}        
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
    
    protected static class TypeIntegerLTE implements Type {
        private static final String POSITIVE_INTEGER_OVERFLOW = 
            "positive integer overflow";
        private static final String NEGATIVE_INTEGER = 
            "negative integer";
        BigInteger limit;
        public TypeIntegerLTE (BigInteger gt) {this.limit = gt;}
        protected Object test (BigInteger i) throws Error {
            if (i.compareTo(BigInteger.ZERO) < 0)
                throw new Error(NEGATIVE_INTEGER);
            else if (i.compareTo(limit) <= 0)
                return i;
            else
                throw new Error(POSITIVE_INTEGER_OVERFLOW);
        } 
        public Object value (Object instance) throws Error {
            return test((BigInteger) INTEGER.value(instance));
        }
        public Object eval (String string) throws JSON.Error {
            return test((BigInteger) INTEGER.eval(string));
        }
        public Type copy() {return new TypeIntegerLTE(limit);}
    }

    protected static class TypeIntegerGT implements Type {
        private static final String NEGATIVE_INTEGER_OVERFLOW = 
            "negative integer overflow";
        public static final String name = "IntegerGT"; 
        BigInteger limit;
        public TypeIntegerGT (BigInteger gt) {this.limit = gt;}
        protected Object test (BigInteger i) throws Error {
            if (i.compareTo(limit) > 0)
                return i;
            else
                throw new Error(NEGATIVE_INTEGER_OVERFLOW);
        } 
        public Object value (Object instance) throws Error {
            return test((BigInteger) INTEGER.value(instance));
        }
        public Object eval (String string) throws JSON.Error {
            return test((BigInteger) INTEGER.eval(string));
        }
        public Type copy() {return new TypeIntegerGT(limit);}
    }

    private static final Double _double_zero = new Double(0.0);
    
    protected static class TypeDoubleLTE implements Type {
        private static final String POSITIVE_DOUBLE_OVERFLOW = 
            "positive double overflow";
        private static final String NEGATIVE_DOUBLE = 
            "negative double";
        public static final String name = "DoubleLT"; 
        Double limit;
        public TypeDoubleLTE (Double gt) {this.limit = gt;}
        protected Object test (Double d) throws Error {
            if (d.compareTo(_double_zero) < 0)
                throw new Error(NEGATIVE_DOUBLE);
            else if (d.compareTo(limit) <= 0)
                return d;
            else
                throw new Error(POSITIVE_DOUBLE_OVERFLOW);
        } 
        public Object value (Object instance) throws Error {
            return test((Double) DOUBLE.value(instance));
        }
        public Object eval (String string) throws JSON.Error {
            return test((Double) DOUBLE.eval(string));
        }
        public Type copy() {return new TypeDoubleLTE(limit);}
    }
    
    protected static class TypeDoubleGT implements Type {
        private static final String NEGATIVE_DOUBLE_OVERFLOW = 
            "negative double overflow";
        Double limit;
        public TypeDoubleGT (Double gt) {this.limit = gt;}
        protected Object test (Double d) throws Error {
            if (d.compareTo(limit) > 0)
                return d;
            else
                throw new Error(NEGATIVE_DOUBLE_OVERFLOW);
        } 
        public Object value (Object instance) throws Error {
            return test((Double) DOUBLE.value(instance));
        }
        public Object eval (String string) throws JSON.Error {
            return test((Double) DOUBLE.eval(string));
        }
        public Type copy() {return new TypeDoubleGT(limit);}
    }
    
    private static final BigDecimal _decimal_zero = BigDecimal.valueOf(0);
    
    protected static class TypeDecimalLT implements Type {
        private static final String POSITIVE_DECIMAL_OVERFLOW = 
            "positive decimal overflow";
        private static final String NEGATIVE_DECIMAL = 
            "negative decimal";
        BigDecimal limit;
        int scale;
        public TypeDecimalLT (BigDecimal lt) {
            limit = lt;
            scale = limit.scale(); 
        } 
        protected Object test (BigDecimal b) throws Error {
            b.setScale(scale);
            if (b.compareTo(_decimal_zero) < 0)
                throw new Error(NEGATIVE_DECIMAL);
            else if (b.compareTo(limit) < 0)
                return b;
            else
                throw new Error(POSITIVE_DECIMAL_OVERFLOW);
        }
        public Object value (Object instance) throws Error {
            return test((BigDecimal) DECIMAL.value(instance));
        }
        public Object eval (String string) throws JSON.Error {
            return test((BigDecimal) DECIMAL.eval(string));
        }
        public Type copy() {return new TypeDecimalLT(limit);}
    }
    
    protected static class TypeDecimalGT implements Type {
        private static final String NEGATIVE_DECIMAL_OVERFLOW = 
            "negative decimal overflow";
        BigDecimal limit;
        int scale;
        public TypeDecimalGT (BigDecimal gt) {
            limit = gt;
            scale = limit.scale(); 
        } 
        protected Object test (BigDecimal b) throws Error {
            b.setScale(scale);
            if (b.compareTo(limit) > 0)
                return b;
            else
                throw new Error(NEGATIVE_DECIMAL_OVERFLOW);
        }
        public Object value (Object instance) throws Error {
            return test((BigDecimal) DECIMAL.value(instance));
        }
        public Object eval (String string) throws JSON.Error {
            return test((BigDecimal) DECIMAL.eval(string));
        }
        public Type copy() {return new TypeDecimalGT(limit);}
    }
    
    /**
     * <p>A safe JSON intepreter to evaluate and validate a UNICODE string 
     * as a limited tree of Java instances that matches a regular pattern
     * of types and values.</p> 
     * 
     * <p><b>Copyright</b> &copy; 2006 Laurent A.V. Szyster</p>
     * 
     * @author Laurent Szyster
     * @version 0.1.0
     */
    public static class Interpreter extends JSON.Interpreter {
        
        protected static final String NOT_A_JSONR_ARRAY_TYPE = 
            "array type error";
        protected static final String NOT_A_JSONR_OBJECT_TYPE = 
            "object type error";
        protected static final String IRREGULAR_ARRAY = "irregular array";
        protected static final String IRREGULAR_OBJECT = "irregular object";
        protected static final String PARTIAL_ARRAY = "partial array";
        protected static final String ARRAY_OVERFLOW = "array overflow";
        protected static final String NAME_ERROR = "name error";

        public Interpreter() {super();}
        
        public Interpreter(int containers, int iterations) {
            super(containers, iterations);
        }
        
        public Object eval(String json, Type type) 
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
        
        public JSON.Error update(HashMap map, String json, Type type) {
            if (!(type instanceof TypeObject))
                return new Error(NOT_A_JSONR_OBJECT_TYPE);
            
            TypeObject to = (TypeObject) type;
            buf = new StringBuffer();
            it = new StringCharacterIterator(json);
            try {
                c = it.first();
                while (Character.isWhitespace(c)) c = it.next();
                if (c == '{') {
                    c = it.next();
                    object(map, to.namespace, to.names);
                    return null;
                } else
                    return error(NOT_AN_OBJECT);
            } catch (JSON.Error e){
                return e;
            } finally {
                buf = null;
                it = null;
            }
        }
        
        public JSON.Error extend(ArrayList list, String json, Type type) {
            if (!(type instanceof TypeArray))
                return new Error(NOT_A_JSONR_ARRAY_TYPE);
            
            buf = new StringBuffer();
            it = new StringCharacterIterator(json);
            try {
                c = it.first();
                while (Character.isWhitespace(c)) c = it.next();
                if (c == '[') {
                    c = it.next();
                    array(list, ((TypeArray) type).iterator());
                    return null;
                } else
                    return error(NOT_AN_ARRAY);
            } catch (JSON.Error e){
                return e;
            } finally {
                buf = null;
                it = null;
            }
        }
        
        protected Object value(Type type) 
        throws JSON.Error {
            while (Character.isWhitespace(c)) c = it.next();
            switch(c){
            case '{': {
                if (type instanceof TypeObject) {
                    TypeObject to = (TypeObject) type;
                    c = it.next();
                    return object(new HashMap(), to.namespace, to.names);
                } else if (type == TypeUndefined.singleton) {
                    c = it.next();
                    return object(new HashMap());
                } else
                    throw error(IRREGULAR_OBJECT);
            }
            case '[': {
                if (type instanceof TypeArray) { 
                    Iterator types = ((TypeArray) type).iterator();
                    c = it.next(); 
                    if (types.hasNext())
                        return array(new ArrayList(), types);
                    else
                        return array(new ArrayList());
                } else if (type == TypeUndefined.singleton) {
                    c = it.next();
                    return array(new ArrayList());
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
        
        protected Object value(Type type, String name) throws JSON.Error {
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
        
        protected Object value(Type type, int index) throws JSON.Error {
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
        
        protected Object object(HashMap map, HashMap namespace, Set names) 
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
                    
                    map.put(name, val);
                    token = value();
                    if (token == COMMA)
                        token = value();
                } else {
                    throw error(COLON_EXPECTED);
                }
            }
            if (map.keySet().containsAll(namespace.keySet()))
                return map;
            else
                throw new Error(IRREGULAR_OBJECT);
        }
        
        protected Object array(ArrayList list, Iterator types) 
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
                 
                    list.add(token);
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
                 
                    list.add(token);
                    token = value(); 
                    if (token == COMMA)
                        token = value(type, i++);
                }
            }
            return list;
        }
        
    }
    
    public static Type compile(Object regular, HashMap extensions) {
        if (regular == null) {
            return TypeUndefined.singleton;
        } else if (regular instanceof Boolean) {
            return BOOLEAN;
        } else if (regular instanceof String) {
            String s = (String) regular;
            if (extensions != null && extensions.containsKey(s))
                return (Type) extensions.get(s);
            else if (s.length() > 0)
                return new TypeRegular(s);
            else
                return STRING;
        } else if (regular instanceof BigInteger) {
            BigInteger i = (BigInteger) regular;
            int cmpr = i.compareTo(BigInteger.ZERO); 
            if (cmpr == 0)
                return INTEGER;
            else if (cmpr > 0)
                return new TypeIntegerLTE(i);
            else
                return new TypeIntegerGT(i);
        } else if (regular instanceof Double) {
            Double d = (Double) regular;
            int cmpr = d.compareTo(_double_zero); 
            if (cmpr == 0)
                return DOUBLE;
            else if (cmpr > 0)
                return new TypeDoubleLTE(d);
            else
                return new TypeDoubleGT(d);
        } else if (regular instanceof BigDecimal) {
            BigDecimal b = (BigDecimal) regular;
            int cmpr = b.compareTo(_decimal_zero); 
            if (cmpr == 0)
                return new TypeDecimal();
            else if (cmpr > 0)
                return new TypeDecimalLT(b);
            else
                return new TypeDecimalGT(b);
        } else if (regular instanceof ArrayList) {
            ArrayList array = (ArrayList) regular;
            int l = array.size();
            if (l > 0) {
                Type[] types = new Type[l];
                for (int i=0; i<l; i++) 
                    types[i] = compile(array.get(i), extensions);
                return new TypeArray(types);
            } else 
                return TypeArray.singleton;
        } else if (regular instanceof HashMap) {
            HashMap object = (HashMap) regular;
            Iterator iter = object.keySet().iterator();
            if (!iter.hasNext())
                return TypeObject.singleton;
            else {
                String name;
                HashMap namespace = new HashMap();
                do {
                    name = (String) iter.next();
                    namespace.put(name, compile(object.get(name), extensions));
                } while (iter.hasNext());
                return new TypeObject(namespace);
            } 
        } return null;
    }
    
    public Type type = null;
    
    public JSONR(Object object, HashMap extensions) {
        type = compile(object, extensions);
    }
    
    public JSONR(Object object) {
        type = compile(object, TYPES);
    }

    public JSONR(String string, HashMap extensions) throws JSON.Error  {
        type = compile(JSON.eval(string), extensions);
    }
    
    public JSONR(String string) throws JSON.Error {
        type = compile(JSON.eval(string), TYPES);
    }

    public Object eval(String json, int containers, int iterations) 
    throws JSON.Error {
        return (new Interpreter(containers, iterations)).eval(json, type);
    }

    public HashMap object(String json, int containers, int iterations) 
    throws JSON.Error {
        if (json == null) 
            return null;
        else {
            HashMap map = new HashMap();
            JSON.Error e = (
                new Interpreter(containers, iterations)
                ).update(map, json, type);
            if (e == null)
                return map;
            else
                throw e;
        }
    }
            
    public ArrayList array(String json, int containers, int iterations) 
    throws JSON.Error {
        if (json == null) 
            return null;
        else {
            ArrayList list = new ArrayList();
            JSON.Error e = (
                new Interpreter(containers, iterations)
                ).extend(list, json, type);
            if (e == null)
                return list;
            else
                throw e;
        } 
    }
            
    public HashMap filter(Map query) {
        Type type;
        String name;
        String[] strings;
        HashMap namespace = ((TypeObject) this.type).namespace;
        HashMap valid = new HashMap();
        Iterator iter = query.keySet().iterator();
        while (iter.hasNext()) {
            name = (String) iter.next();
            type = (Type) namespace.get(name);
            strings = (String[]) query.get(name);
            if (type != null && strings != null && strings.length > 0) try {
                valid.put(name, type.eval(strings[0]));
            } catch (JSON.Error e) {;}
        }
        return valid;
    }
    
    public HashMap match(Map query) throws Error {
        Type type;
        String name;
        String[] strings;
        HashMap namespace = ((TypeObject) this.type).namespace;
        HashMap valid = new HashMap();
        Iterator iter = query.keySet().iterator();
        while (iter.hasNext()) {
            name = (String) iter.next();
            type = (Type) namespace.get(name);
            strings = (String[]) query.get(name);
            if (type != null && strings != null && strings.length > 0) try {
                valid.put(name, type.eval(strings[0]));
            } catch (JSON.Error e) {;}
        }
        return valid;
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