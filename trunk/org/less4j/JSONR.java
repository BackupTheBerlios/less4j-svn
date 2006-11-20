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
    
    protected static final String NOT_A_JSONR_ARRAY_TYPE = 
        "not a JSONR array type";
    protected static final String NOT_A_JSONR_OBJECT_TYPE = 
        "not a JSONR object type";

    private static final long serialVersionUID = 0L; 
    
    private static final String _null_string = "";
    
    public static class Error extends JSON.Error  {
        private static final long serialVersionUID = 0L;
        public Error(String message) {super(message);}
    }
    
    /**
     * The interface is made public to allow extension of the framework by 
     * mappings regular expressions or names to a specialized regular type.
     * 
     * <h3>Synopsis</h3>
     * 
     * <p>Custom type classes must implement the <code>value</code>,
     * <code>eval</code> and <code>copy</code> methods:
     * 
     * <pre>import org.less4j.JSONR;
     *import java.text.SimpleDateFormat;
     *public TypeDateInSecondsTZ implements JSONR.Type {
     *    private static final format = new SimpleDateFormat("yyyyMMddHHmmssZ");
     *    public static final JSONR.Type singleton = new TypeDateInSecondsTZ(); 
     *    public Object cast (String string) throws JSONR.Error {
     *        try {
     *            return format.parse(string);
     *        } catch (Exception e) {
     *            throw new JSONR.Error();
     *        }
     *    public Type copy() {return this.singleton;}
     *    } 
     *}</pre>
     * 
     * can be mapped to this name 
     * 
     * <pre>"yyyyMMddHHmmssZ"</pre>
     * 
     * to cast a JSON string like
     * 
     * <pre>"20060704120856-0700"</pre>
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
        public Object value (Object instance) {return instance;}
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
        public Object value (Object data) throws Error {
            if (data instanceof Boolean)
                return data;
            else
                throw new Error("not a Boolean type");
        }
        public Object eval (String string) throws Error {
            if (string.equals(JSON._true))
                return Boolean.TRUE;
            else if (string.equals(JSON._false))
                return Boolean.FALSE;
            else
                throw new Error("not a boolean value");
        }
        public Type copy() {return singleton;}
    }

    protected static class TypeInteger implements Type {
        public static final TypeInteger singleton = new TypeInteger();
        public Object value (Object data) throws Error {
            if (data instanceof BigInteger)
                return data;
            else
                throw new Error("not a BigInteger type");
        }
        public Object eval (String string) throws Error {
            if (string != null) {
                try {
                    return new BigInteger(string);
                } catch (Exception e) {
                    throw new Error("not an integer value");
                }
            } else
                throw new Error("not an integer value");
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
                throw new Error("not a Double type");
        }
        public Object eval (String string) throws Error {
            if (string != null) {
                return new Double(string);
            } else
                throw new Error("not a double value");
        }
        public Type copy() {return singleton;}
    }

    protected static class TypeDecimal implements Type {
        private int scale;
        private int round = BigDecimal.ROUND_HALF_DOWN;
        public TypeDecimal (int scale) {
            this.scale = scale;
        } 
        public Object value (Object instance) throws Error {
            BigDecimal b;
            if (instance instanceof BigDecimal) {
                b = (BigDecimal) instance;
            } else if (instance instanceof Number) {
                b = new BigDecimal(((Number) instance).doubleValue());
            } else
                throw new Error("not a decimal type");
            b.setScale(scale, round);
            return b;
        }
        public Object eval (String string) throws Error {
            if (string != null) {
                return (new BigDecimal(string)).setScale(scale, round);
            } else
                throw new Error("not a decimal value");
        }
        public Type copy() {return new TypeDecimal(scale);}
    }

    protected static class TypeString implements Type {
        public static final TypeString singleton = new TypeString();
        public Object value (Object instance) throws Error {
            if (instance instanceof String) {
                String s = (String) instance;
                if (s.length() > 0)
                    return instance;
                else
                    throw new Error("null string");
            } else
                throw new Error("not a String type");
        }
        public Object eval (String string) throws Error {
            if (string == null || _null_string.equals(string)) 
                throw new Error("null string");
            else
                return string;
        }
        public Type copy() {return singleton;}
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
                throw new Error("not a String type");
        }
        public Object eval (String string) throws Error {
            if (string != null)
                return this.test(string);
            else
                return null;
        }
        public Type copy() {return new TypeRegular(pattern);}
    }
    
    protected static class TypeIntegerLTE implements Type {
        BigInteger limit;
        public TypeIntegerLTE (BigInteger gt) {this.limit = gt;}
        public Object value (Object instance) throws Error {
            if (instance instanceof BigInteger) {
                BigInteger i = (BigInteger) instance;
                if (i.compareTo(BigInteger.ZERO) < 0)
                    throw new Error("integer not positive");
                else if (limit.compareTo(i) >= 0)
                    return i;
                else
                    throw new Error("positive integer overflow");
            } else
                throw new Error("not an Integer type");
        }
        public Object eval (String string) throws Error {
            if (string != null) {
                try {
                    BigInteger i = new BigInteger(string);
                    if (i.compareTo(BigInteger.ZERO) < 0)
                        throw new Error("integer not positive");
                    else if (limit.compareTo(i) >= 0)
                        return i;
                    else
                        throw new Error("positive integer overflow");
                } catch (Exception e) {
                    throw new Error("not an integer value");
                }
            } else
                throw new Error("not an integer value");
        }
        public Type copy() {return new TypeIntegerLTE(limit);}
    }

    protected static class TypeIntegerGT implements Type {
        BigInteger limit;
        public TypeIntegerGT (BigInteger gt) {this.limit = gt;}
        public Object value (Object instance) throws Error {
            if (instance instanceof BigInteger) {
                BigInteger i = (BigInteger) instance;
                if (limit.compareTo(i) < 0)
                    return i;
                else
                    throw new Error("negative integer overflow");
            } else
                    throw new Error("not an integer type");
        }
        public Object eval (String string) throws Error {
            if (string != null) { 
                try {
                    BigInteger i = new BigInteger(string);
                    if (limit.compareTo(i) < 0)
                        return i;
                    else
                        throw new Error("negative integer overflow");
                } catch (Exception e) {
                    throw new Error("not an integer value");
                }
            } else
                throw new Error("not an integer value");
        }
        public Type copy() {return new TypeIntegerGT(limit);}
    }

    private static final Double _double_zero = new Double(0.0);
    
    protected static class TypeDoubleLTE implements Type {
        Double limit;
        public TypeDoubleLTE (Double gt) {this.limit = gt;}
        public Object value (Object instance) throws Error {
            if (instance instanceof Double) {
                Double d = (Double) instance;
                if (d.compareTo(_double_zero) < 0)
                    throw new Error("double not positive");
                else if (limit.compareTo(d) >= 0)
                    return d;
                else
                    throw new Error("positive double overflow");
            } else
                throw new Error("not a Double type");
        }
        public Object eval (String string) throws Error {
            if (string != null) { 
                Double d = new Double(string);
                if (d.compareTo(_double_zero) < 0)
                    throw new Error("double not positive");
                else if (limit.compareTo(d) >= 0)
                    return d;
                else
                    throw new Error("positive double overflow");
            } else
                throw new Error("not a double value");
        }
        public Type copy() {return new TypeDoubleLTE(limit);}
    }
    
    protected static class TypeDoubleGT implements Type {
        Double limit;
        public TypeDoubleGT (Double gt) {this.limit = gt;}
        public Object value (Object instance) throws Error {
            if (instance instanceof Double) {
                Double d = (Double) instance;
                if (limit.compareTo(d) < 0)
                    return d;
                else
                    throw new Error("negative double overflow");
            } else
                throw new Error("not a Double type");
        }
        public Object eval (String string) throws Error {
            if (string != null) { 
                Double d = new Double(string);
                if (limit.compareTo(d) < 0)
                    return d;
                else
                    throw new Error("negative double overflow");
            } else
                throw new Error("not a double value");
        }
        public Type copy() {return new TypeDoubleGT(limit);}
    }
    
    private static final BigDecimal _decimal_zero = BigDecimal.valueOf(0);
    
    protected static class TypeDecimalLT extends TypeDecimal {
        BigDecimal limit;
        public TypeDecimalLT (BigDecimal lt) {
            super(lt.scale());
            this.limit = lt;
        } 
        private Object test (BigDecimal b) throws Error {
            if (b.compareTo(_decimal_zero) < 0)
                throw new Error("decimal not positive");
            else if (limit.compareTo(b) > 0)
                return b;
            else
                throw new Error("positive decimal overflow");
        }
        public Object value (Object instance) throws Error {
            return test((BigDecimal) super.value(instance));
        }
        public Object eval (String string) throws Error {
            return test((BigDecimal) super.eval(string));
        }
        public Type copy() {return new TypeDecimalLT(limit);}
    }
    
    protected static class TypeDecimalGT extends TypeDecimal {
        BigDecimal limit;
        public TypeDecimalGT (BigDecimal gt) {
            super(gt.scale());
            this.limit = gt;
        } 
        private Object test (BigDecimal b) throws Error {
            if (limit.compareTo(b) < 0)
                return b;
            else
                throw new Error("negative decimal overflow");
        }
        public Object value (Object instance) throws Error {
            return test((BigDecimal) super.value(instance));
        }
        public Object eval (String string) throws Error {
            return test((BigDecimal) super.eval(string));
        }
        public Type copy() {return new TypeDecimalGT(limit);}
    }
    
    protected static class TypeArray implements Type {
        private Type[] types = null;
        public TypeArray (Type[] types) {this.types = types;}
        public Object value (Object instance) throws Error {
            if (instance instanceof ArrayList)
                return instance;
            else
                throw new Error("not an array type");
            }
        public Object eval (String instance) throws Error {
            try {
                return (new Interpreter()).array(instance, this, null);
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
        private HashMap namespace = null;
        public TypeObject (HashMap ns) {namespace = ns;}
        public Object value (Object instance) throws Error {
            if (instance instanceof HashMap)
                return instance;
            else
                throw new Error("not an object type");
            }
        public Object eval (String instance) throws Error {
            try {
                return (new Interpreter()).object(instance, this, null);
            } catch (JSON.Error e) {
                return null;
            }
        }
        public HashMap namespace() {return namespace;}
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
        
        protected Error error(String message, String arg) {
            StringBuffer sb = new StringBuffer();
            sb.append(it.getIndex());
            sb.append(message);
            sb.append(arg);
            return new Error(sb.toString());
        }
        
        public Object eval(String json, JSONR jsonr) 
        throws JSON.Error {
            buf = new StringBuffer();
            it = new StringCharacterIterator(json);
            try {
                c = it.first();
                return (HashMap) value(jsonr.type);
            } finally {
                buf = null;
                it = null;
            }
        }
        
        protected HashMap object(String json, TypeObject type, HashMap map) 
        throws JSON.Error {
            buf = new StringBuffer();
            it = new StringCharacterIterator(json);
            try {
                c = it.first();
                while (Character.isWhitespace(c)) c = it.next();
                if (c != '{')
                    throw error(NOT_AN_OBJECT);
                else
                    return (HashMap) object(type.namespace(), map);
            } finally {
                buf = null;
                it = null;
            }
        }
        
        protected ArrayList array(String json, TypeArray type, ArrayList list) 
        throws JSON.Error {
            buf = new StringBuffer();
            it = new StringCharacterIterator(json);
            try {
                c = it.first();
                while (Character.isWhitespace(c)) c = it.next();
                if (c != '[')
                    throw error(NOT_AN_ARRAY);
                else
                    return (ArrayList) array(type.iterator(), list);
            } finally {
                buf = null;
                it = null;
            }
        }
        
        protected Object object(HashMap namespace, HashMap map) 
        throws JSON.Error {
            String name; 
            Object val;
            map = (map != null) ? map : new HashMap();
            if (--containers < 0) 
                throw error(CONTAINERS_OVERFLOW);
            
            Type type;
            Object token = value();
            while (token != OBJECT) {
                if (!(token instanceof String))
                    throw error(STRING_EXPECTED);
                
                if (--iterations < 0) 
                    throw error(ITERATIONS_OVERFLOW);
                
                name = (String) token;
                type = (Type) namespace.get(name);
                if (type == null)
                    throw error(NO_TYPE_FOR, name);
                
                if (value() == COLON) {
                    val = value(type);
                    if (val==COLON || val==COMMA || val==OBJECT || val==ARRAY)
                        throw error(VALUE_EXPECTED);
                    
                    map.put(name, val);
                    token = value();
                    if (token == COMMA)
                        token = value(type);
                } else {
                    throw error(COLON_EXPECTED, c);
                }
            }
            return map;
        }
        
        protected Object array(Iterator types, ArrayList list) 
        throws JSON.Error {
            list = (list != null) ? list : new ArrayList();
            if (--containers < 0) 
                throw error(CONTAINERS_OVERFLOW);

            Type type = (Type) types.next();
            Object token = value(type);
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
                            token = value((Type) types.next());
                        else
                            throw error(ARRAY_OVERFLOW);
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
                        token = value(type);
                }
            }
            return list;
        }
        
        protected Object value(Type type) 
        throws JSON.Error {
            while (Character.isWhitespace(c)) c = it.next();
            switch(c){
            case '{': {
                if (type instanceof TypeObject) {
                    HashMap namespace = ((TypeObject) type).namespace();
                    c = it.next();
                    if (namespace.isEmpty())
                        return object(null);
                    else
                        return object(namespace, null);
                    }
                else
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
            case JSON.DONE:
                throw error(UNEXPECTED_END);
            default: 
                throw error(UNEXPECTED_CHARACTER, c);
            }
        }
        
    }
    
    protected static Type compile(Object regular) {
        if (regular == null) {
            return TypeUndefined.singleton;
        } else if (regular instanceof String) {
            String s = (String) regular;
            if (_null_string.equals(s))
                return TypeString.singleton;
            else
                return new TypeRegular(s);
        } else if (regular instanceof BigInteger) {
            BigInteger i = (BigInteger) regular;
            int cmpr = i.compareTo(BigInteger.ZERO); 
            if (cmpr == 0)
                return TypeInteger.singleton;
            else if (cmpr > 0)
                return new TypeIntegerLTE(i);
            else
                return new TypeIntegerGT(i);
        } else if (regular instanceof BigDecimal) {
            BigDecimal b = (BigDecimal) regular;
            int cmpr = b.compareTo(_double_zero); 
            if (cmpr == 0)
                return new TypeDecimal(b.scale());
            else if (cmpr > 0)
                return new TypeDecimalLT(b);
            else
                return new TypeDecimalGT(b);
        } else if (regular instanceof Double) {
            Double d = (Double) regular;
            int cmpr = d.compareTo(_double_zero); 
            if (cmpr == 0)
                return TypeDouble.singleton;
            else if (cmpr > 0)
                return new TypeDoubleLTE(d);
            else
                return new TypeDoubleGT(d);
        } else if (regular instanceof Boolean) {
            return TypeBoolean.singleton;
        } else if (regular instanceof ArrayList) {
            ArrayList array = (ArrayList) regular;
            int l = array.size();
            if (l > 0) {
                Type[] types = new Type[l];
                for (int i=0; i<l; i++) types[i] = compile(array.get(i));
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
                    namespace.put(name, compile(object.get(name)));
                } while (iter.hasNext());
                return new TypeObject(namespace);
            } 
        } return null;
    }
    
    protected Type type = null;
    
    protected JSONR(Type type) {this.type = type;}
    
    public JSONR(JSONR jsonr) {type = jsonr.type.copy();}
    
    public JSONR(Object regular) {type = compile(regular);}
    
    public JSONR(String jsonr) throws JSON.Error {
        type = compile((new JSON.Interpreter()).eval(jsonr));
    }

    public Object eval(String json, int containers, int iterations) 
    throws JSON.Error {
        return (new Interpreter(containers, iterations)).eval(json, this);
    }

    public HashMap object(String json, int containers, int iterations) 
    throws JSON.Error {
    if (type instanceof TypeObject) 
        return (
            new Interpreter(containers, iterations)
            ).object(json, ((TypeObject) type), null);
    else
        throw new Error(NOT_A_JSONR_OBJECT_TYPE);
    }
            
    public ArrayList array(String json, int containers, int iterations) 
    throws JSON.Error {
    if (type instanceof TypeArray) 
        return (
            new Interpreter(containers, iterations)
            ).array(json, ((TypeArray) type), null);
    else
        throw new Error(NOT_A_JSONR_ARRAY_TYPE);
    }
            
    public HashMap filter(Map query) 
    throws Error {
        if (!(type instanceof TypeObject))
            throw new Error(NOT_A_JSONR_OBJECT_TYPE);
        
        Type type;
        String name;
        String[] strings;
        HashMap namespace = ((TypeObject) this.type).namespace();
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
    
    public HashMap match(Map query) 
    throws Error {
        if (!(type instanceof TypeObject))
            throw new Error(NOT_A_JSONR_OBJECT_TYPE);
        
        Type type;
        String name;
        String[] strings;
        HashMap namespace = ((TypeObject) this.type).namespace();
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
    
    public boolean update(
        HashMap map, String json, int containers, int iterations
        ) 
        throws JSON.Error {
        if (type instanceof TypeObject) try { 
            (
                new Interpreter(containers, iterations)
                ).object(json, ((TypeObject) type), map);
            return true;
            
        } catch (JSON.Error e) {}
        return false;
    }
            
    public boolean extend(
        ArrayList list, String json, int containers, int iterations
        ) 
    throws JSON.Error {
        if (type instanceof TypeArray) try { 
            (
                new Interpreter(containers, iterations)
                ).array(json, ((TypeArray) type), list);
            return true;
            
        } catch (JSON.Error e) {}
        return false;
    }
            
}

