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
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;
import java.text.StringCharacterIterator;

/**
 * Compile simple JSON Regular patterns to validate the input values against 
 * practical numeric ranges, regular expression patterns, typed collections,
 * fixed length records definitions and complex objects.
 * 
 * <p>...</p>
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
    
    protected static final String NOT_A_JSONR_ARRAY_TYPE = 
        "not a JSONR array type";
    protected static final String NOT_A_JSONR_OBJECT_TYPE = 
        "not a JSONR object type";

    protected static final String NOT_A_BOOLEAN_VALUE = 
        "not a boolean value";
    protected static final String NOT_A_BOOLEAN_TYPE = 
        "not a Boolean type";
    protected static final String NOT_AN_INTEGER_VALUE = 
        "not an integer value";
    protected static final String NOT_AN_INTEGER_TYPE = 
        "not an integer type";
    protected static final String NOT_A_DOUBLE_VALUE = 
        "not a double value";
    protected static final String NOT_A_DOUBLE_TYPE = 
        "not a double type";
    protected static final String NOT_A_DECIMAL_VALUE = 
        "not a decimal value";
    protected static final String NOT_A_DECIMAL_TYPE = 
        "not a decimal type";
    protected static final String NULL_STRING = 
        "null string";
    protected static final String NOT_A_STRING_TYPE = 
        "not a string type";
    
    private static final long serialVersionUID = 0L; 
    
    /**
     * The interface is made public to allow extension of the framework by 
     * mappings regular expressions or names to a specialized regular type.
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
     *public TypeDateInSecondsTZ implements JSONR.Type {
     *
     *    private static final 
     *    format = new SimpleDateFormat("yyyyMMddHHmmssZ");
     *    
     *    private static final
     *    NOT_A_SIMPLEDATEFORMAT = "not a valid SimpleDateFormat";
     *     
     *    public Object value (Object instance) throws JSONR.Error {
     *        return eval((String) JSONR.STRING.value(instance));
     *    }
     *    
     *    public Object eval (String string) throws JSONR.Error {
     *        try {
     *            return format.parse(string);
     *        } catch (Exception e) {
     *            throw new JSONR.Error(NOT_A_SIMPLEDATEFORMAT);
     *        }
     *    }
     *    
     *    public static final 
     *    JSONR.Type singleton = new TypeDateInSecondsTZ();
     *    
     *    public Type copy() {return this.singleton;}
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
        public Object eval(String string) throws Error;
        public Type copy();
    } 
    // at last some use for java interfaces ;-)
    
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
        public static final TypeBoolean singleton = new TypeBoolean();
        public Object value (Object instance) throws Error {
            if (instance instanceof Boolean)
                return instance;
            else
                throw new Error(NOT_A_BOOLEAN_TYPE);
        }
        public Object eval (String string) throws Error {
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
        public static final TypeInteger singleton = new TypeInteger();
        public Object value (Object instance) throws Error {
            if (instance instanceof BigInteger)
                return instance;
            else
                throw new Error(NOT_AN_INTEGER_TYPE);
        }
        public Object eval (String string) throws Error {
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
        public static final TypeDouble singleton = new TypeDouble();
        public Object value (Object instance) throws Error {
            if (instance instanceof Double)
                return instance;
            else if (instance instanceof Number)
                return new Double(((Number) instance).doubleValue());
            else
                throw new Error(NOT_A_DOUBLE_TYPE);
        }
        public Object eval (String string) throws Error {
            if (string != null) {
                return new Double(string);
            } else
                throw new Error(NOT_A_DOUBLE_VALUE);
        }
        public Type copy() {return singleton;}
    }

    protected static class TypeDecimal implements Type {
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
        public Object eval (String string) throws Error {
            if (string != null) {
                return (new BigDecimal(string));
            } else
                throw new Error(NOT_A_DECIMAL_VALUE);
        }
        public static final Type singleton = new TypeDecimal();
        public Type copy() {return singleton;}
    }

    protected static class TypeString implements Type {
        public static final TypeString singleton = new TypeString();
        public Object value (Object instance) throws Error {
            if (instance instanceof String) {
                String s = (String) instance;
                if (s.length() > 0)
                    return instance;
                else
                    throw new Error(NULL_STRING);
            } else
                throw new Error(NOT_A_STRING_TYPE);
        }
        public Object eval (String string) throws Error {
            if (string == null || string.length() == 0) 
                throw new Error(NULL_STRING);
            else
                return string;
        }
        public Type copy() {return singleton;}
    }
    
    public static final Type BOOLEAN = TypeBoolean.singleton;
    public static final Type INTEGER = TypeInteger.singleton;
    public static final Type DOUBLE = TypeDouble.singleton;
    public static final Type DECIMAL = TypeDecimal.singleton;
    public static final Type STRING = TypeString.singleton;
    
    protected static class TypeIntegerLTE implements Type {
        BigInteger limit;
        public TypeIntegerLTE (BigInteger gt) {this.limit = gt;}
        protected Object test (BigInteger i) throws Error {
            if (i.compareTo(BigInteger.ZERO) < 0)
                throw new Error("integer not positive");
            else if (limit.compareTo(i) >= 0)
                return i;
            else
                throw new Error("positive integer overflow");
        } 
        public Object value (Object instance) throws Error {
            return test((BigInteger) INTEGER.value(instance));
        }
        public Object eval (String string) throws Error {
            return test((BigInteger) INTEGER.eval(string));
        }
        public Type copy() {return new TypeIntegerLTE(limit);}
    }

    protected static class TypeIntegerGT implements Type {
        BigInteger limit;
        public TypeIntegerGT (BigInteger gt) {this.limit = gt;}
        protected Object test (BigInteger i) throws Error {
            if (limit.compareTo(i) < 0)
                return i;
            else
                throw new Error("negative integer overflow");
        } 
        public Object value (Object instance) throws Error {
            return test((BigInteger) INTEGER.value(instance));
        }
        public Object eval (String string) throws Error {
            return test((BigInteger) INTEGER.eval(string));
        }
        public Type copy() {return new TypeIntegerGT(limit);}
    }

    private static final Double _double_zero = new Double(0.0);
    
    protected static class TypeDoubleLTE implements Type {
        Double limit;
        public TypeDoubleLTE (Double gt) {this.limit = gt;}
        protected Object test (Double d) throws Error {
            if (d.compareTo(_double_zero) < 0)
                throw new Error("double not positive");
            else if (limit.compareTo(d) >= 0)
                return d;
            else
                throw new Error("positive double overflow");
        } 
        public Object value (Object instance) throws Error {
            return test((Double) DOUBLE.value(instance));
        }
        public Object eval (String string) throws Error {
            return test((Double) DOUBLE.eval(string));
        }
        public Type copy() {return new TypeDoubleLTE(limit);}
    }
    
    protected static class TypeDoubleGT implements Type {
        Double limit;
        public TypeDoubleGT (Double gt) {this.limit = gt;}
        protected Object test (Double d) throws Error {
            if (limit.compareTo(d) < 0)
                return d;
            else
                throw new Error("negative double overflow");
        } 
        public Object value (Object instance) throws Error {
            return test((Double) DOUBLE.value(instance));
        }
        public Object eval (String string) throws Error {
            return test((Double) DOUBLE.eval(string));
        }
        public Type copy() {return new TypeDoubleGT(limit);}
    }
    
    private static final BigDecimal _decimal_zero = BigDecimal.valueOf(0);
    
    protected static class TypeDecimalLT implements Type {
        BigDecimal limit;
        int scale;
        public TypeDecimalLT (BigDecimal lt) {
            limit = lt;
            scale = limit.scale(); 
        } 
        private Object test (BigDecimal b) throws Error {
            b.setScale(scale);
            if (b.compareTo(_decimal_zero) < 0)
                throw new Error("decimal not positive");
            else if (limit.compareTo(b) > 0)
                return b;
            else
                throw new Error("positive decimal overflow");
        }
        public Object value (Object instance) throws Error {
            return test((BigDecimal) DECIMAL.value(instance));
        }
        public Object eval (String string) throws Error {
            return test((BigDecimal) DECIMAL.eval(string));
        }
        public Type copy() {return new TypeDecimalLT(limit);}
    }
    
    protected static class TypeDecimalGT implements Type {
        BigDecimal limit;
        int scale;
        public TypeDecimalGT (BigDecimal gt) {
            limit = gt;
            scale = limit.scale(); 
        } 
        private Object test (BigDecimal b) throws Error {
            b.setScale(scale);
            if (limit.compareTo(b) < 0)
                return b;
            else
                throw new Error("negative decimal overflow");
        }
        public Object value (Object instance) throws Error {
            return test((BigDecimal) DECIMAL.value(instance));
        }
        public Object eval (String string) throws Error {
            return test((BigDecimal) DECIMAL.eval(string));
        }
        public Type copy() {return new TypeDecimalGT(limit);}
    }
    
    protected static class TypeRegular implements Type {
        private Pattern pattern = null;
        private TypeRegular (Pattern pattern) {
            this.pattern = pattern;
        } 
        public TypeRegular (String expression) {
            pattern = Pattern.compile(expression);
        } 
        private Object test (String string) throws Error {
            if (pattern.matcher(string).matches())
                return string;
            else
                throw new Error("irregular string value");
        }
        public Object value (Object instance) throws Error {
            if (instance instanceof String) {
                return this.test((String) instance);
            } else
                throw new Error(NOT_A_STRING_TYPE);
        }
        public Object eval (String string) throws Error {
            if (string != null)
                return this.test(string);
            else
                return null;
        }
        public Type copy() {return new TypeRegular(pattern);}
    }
    
    protected static class TypeArray implements Type {
        public Type[] types = null;
        public TypeArray (Type[] types) {this.types = types;}
        public Object value (Object instance) throws Error {
            if (instance instanceof ArrayList)
                return instance;
            else
                throw new Error("not an array type");
            }
        public Object eval (String instance) throws Error {
            try {
                return (new Interpreter()).extend(instance, this, null);
            } catch (JSON.Error e) {
                return null;
            }
        }
        public Iterator iterator() {return new Simple.ObjectIterator(types);}
        public Type copy() {
            return new TypeArray(types);
        }
    }
    
    protected static class TypeObject implements Type {
        public HashMap namespace = null;
        public TypeObject (HashMap ns) {namespace = ns;}
        public Object value (Object instance) throws Error {
            if (instance instanceof HashMap)
                return instance;
            else
                throw new Error("not an object type");
            }
        public Object eval (String instance) throws Error {
            try {
                return (new Interpreter()).update(instance, this, null);
            } catch (JSON.Error e) {
                return null;
            }
        }
        public Type copy() {
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
        
        protected static final String IRREGULAR_ARRAY = " irregular array";
        protected static final String IRREGULAR_OBJECT = " irregular object";
        protected static final String PARTIAL_ARRAY = " partial array";
        protected static final String ARRAY_OVERFLOW = " array overflow";
        protected static final String NO_TYPE_FOR = " no type for ";

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
        
        public HashMap update(String json, Type type, HashMap map) 
        throws JSON.Error {
            if (!(type instanceof TypeObject))
                throw new Error(NOT_A_JSONR_OBJECT_TYPE);
            
            buf = new StringBuffer();
            it = new StringCharacterIterator(json);
            try {
                c = it.first();
                while (Character.isWhitespace(c)) c = it.next();
                if (c == '{') {
                    c = it.next();
                    return (HashMap) object(
                        ((TypeObject) type).namespace, map
                        );
                }else
                    throw error(NOT_AN_OBJECT);
            } finally {
                buf = null;
                it = null;
            }
        }
        
        public ArrayList extend(String json, Type type, ArrayList list) 
        throws JSON.Error {
            if (!(type instanceof TypeArray))
                throw new Error(NOT_A_JSONR_ARRAY_TYPE);
            
            buf = new StringBuffer();
            it = new StringCharacterIterator(json);
            try {
                c = it.first();
                while (Character.isWhitespace(c)) c = it.next();
                if (c == '[') {
                    c = it.next();
                    return (ArrayList) array(
                        ((TypeArray) type).iterator(), list
                        );
                } else
                    throw error(NOT_AN_ARRAY);
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
                    HashMap namespace = ((TypeObject) type).namespace;
                    c = it.next();
                    if (namespace.isEmpty())
                        return object(null);
                    else
                        return object(namespace, null);
                } else if (type == TypeUndefined.singleton) {
                    c = it.next();
                    return object(null);
                } else
                    throw error(IRREGULAR_OBJECT);
            }
            case '[': {
                if (type instanceof TypeArray) { 
                    Iterator types = ((TypeArray) type).iterator();
                    c = it.next(); 
                    if (types.hasNext())
                        return array(types, null);
                    else
                        return array(null);
                } else if (type == TypeUndefined.singleton) {
                    c = it.next();
                    return array(null);
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
                e.jsonNames.add(0, name);
                throw e;
            } catch (JSON.Error e) {
                e.jsonNames.add(0, name);
                throw e;
            }
        }
        
        protected Object value(Type type, int index) throws JSON.Error {
            try {
                return value(type);
            } catch (Error e) {
                e.jsonIndex = it.getIndex();
                e.jsonNames.add(0, BigInteger.valueOf(index));
                throw e;
            } catch (JSON.Error e) {
                e.jsonNames.add(0, BigInteger.valueOf(index));
                throw e;
            }
        }
        
        protected Object object(HashMap namespace, HashMap map) 
        throws JSON.Error {
            if (--containers < 0) 
                throw error(CONTAINERS_OVERFLOW);
            
            Type type;
            String name; 
            Object val;
            map = (map != null) ? map : new HashMap();
            Object token = value();
            while (token != OBJECT) {
                if (!(token instanceof String))
                    throw error(STRING_EXPECTED);
                
                if (--iterations < 0) 
                    throw error(ITERATIONS_OVERFLOW);
                
                name = (String) token;
                type = (Type) namespace.get(name);
                if (type == null)
                    throw new Error(NO_TYPE_FOR);
                
                if (value() == COLON) {
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
            return map;
        }
        
        protected Object array(Iterator types, ArrayList list) 
        throws JSON.Error {
            if (--containers < 0) 
                throw error(CONTAINERS_OVERFLOW);

            int i = 0;
            list = (list != null) ? list : new ArrayList();
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
    
    protected static Type compile(Object regular, HashMap extensions) {
        if (regular == null) {
            return TypeUndefined.singleton;
        } else if (regular instanceof Boolean) {
            return BOOLEAN;
        } else if (regular instanceof String) {
            String s = (String) regular;
            if (extensions != null && extensions.containsKey(s))
                return (Type) extensions.get(s);
            else if (s.length() > 0)
                return STRING;
            else
                return new TypeRegular(s);
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
                return new TypeArray(new Type[]{});
        } else if (regular instanceof HashMap) {
            HashMap object = (HashMap) regular;
            Iterator iter = object.keySet().iterator();
            if (!iter.hasNext())
                return new TypeObject(null);
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
    
    protected Type type = null;
    
    protected JSONR(Type type) {this.type = type;}
    
    public JSONR(JSONR jsonr) {type = jsonr.type.copy();}
    
    public JSONR(Object regular, HashMap extensions) {
        type = compile(regular, null);
        }
    
    public JSONR(Object regular) {type = compile(regular, null);}
    
    public JSONR(String string, HashMap extensions) throws JSON.Error  {
        type = compile((new JSON.Interpreter()).eval(string), extensions);
    }
    
    public JSONR(String string) throws JSON.Error {
        type = compile((new JSON.Interpreter()).eval(string), null);
    }

    public Object eval(String json, int containers, int iterations) 
    throws JSON.Error {
        return (
            new Interpreter(containers, iterations)
            ).eval(json, this.type);
    }

    public HashMap object(String json, int containers, int iterations) 
    throws JSON.Error {
        return (
            new Interpreter(containers, iterations)
            ).update(json, ((TypeObject) type), null);
    }
            
    public ArrayList array(String json, int containers, int iterations) 
    throws JSON.Error {
        return (
            new Interpreter(containers, iterations)
            ).extend(json, ((TypeArray) type), null);
    }
            
    public HashMap update(
        HashMap map, String json, int containers, int iterations
        ) throws JSON.Error {
        return (
            new Interpreter(containers, iterations)
            ).update(json, (type), map);
    }
            
    public ArrayList extend(
        ArrayList list, String json, int containers, int iterations
        ) throws JSON.Error {
        return (
            new Interpreter(containers, iterations)
            ).extend(json, (type), list);
    }
            
    public HashMap filter(Map query) throws Error {
        if (!(type instanceof TypeObject))
            throw new Error(NOT_A_JSONR_OBJECT_TYPE);
        
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
            if (type != null && strings != null && strings.length > 0) {
                valid.put(name, type.eval(strings[0]));
            }
        }
        return valid;
    }
    
    public HashMap match(Map query) throws Error {
        if (!(type instanceof TypeObject))
            throw new Error(NOT_A_JSONR_OBJECT_TYPE);
        
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
            if (type != null && strings != null && strings.length > 0) {
                valid.put(name, type.eval(strings[0]));
            }
        }
        return valid;
    }
    
    public static StringBuffer strb(StringBuffer sb, Map map, Iterator it) {
        Object key; 
        if (!it.hasNext()) {
            sb.append(JSON._object);
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
            sb.append(JSON._array);
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
            sb.append(JSON._null);
        else if (value instanceof Boolean)
            sb.append(
                ((Boolean) value).booleanValue() ? JSON._true : JSON._false
                );
        else if (value instanceof Number) 
            sb.append(value);
        else if (value instanceof String) 
            strb(sb, (String) value);
        else if (value instanceof Character) 
            strb(sb, ((Character) value).toString());
        else if (value instanceof Map) {
            Map object = (Map) value;
            String[] names = (String[]) object.keySet().toArray();
            Arrays.sort(names);
            strb(sb, object, Simple.iterator(names));
        } else if (value instanceof List) 
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
    
    public static String digest(Object value, byte[] salt) {
        SHA1 md = new SHA1();
        md.update(str(value).getBytes());
        md.update(salt);
        return md.hexdigest();
    }
    
    public static boolean verify(
        Object value, byte[] salt, String signature
        ) {
        return signature.equals(digest(value, salt));
    }
    
}

