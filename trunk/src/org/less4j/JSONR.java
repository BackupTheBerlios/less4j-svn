/* Copyright (C) 2006-2007 Laurent A.V. Szyster

This library is free software; you can redistribute it and/or modify
it under the terms of version 2 of the GNU Lesser General Public License as
published by the Free Software Foundation.

   http://www.gnu.org/copyleft/lesser.html

This library is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

You should have received a copy of the GNU Lesser General Public License
along with this library; if not, write to the Free Software Foundation, 
Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA */

package org.less4j; // less java for more applications

import java.util.Set;
import java.util.HashSet;
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
 * Compile simple <a href="http://laurentszyster.be/jsonr/index.html">JSON 
 * Regular</a> patterns to evaluate and validate a JSON string against an 
 * extensible type model of JSON types, numeric ranges, regular string, 
 * formated dates, collections, relations, regular dictionaries and
 * relevant namespaces.
 * 
 * <h3>Synopsis</h3>
 * 
 * <p>A safe JSONR intepreter to evaluate and validate a UNICODE string 
 * as a limited tree of Java instances that matches a regular pattern
 * of types and values.</p> 
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
 * <h4>JSON Regular Evaluation</h4>
 * 
 * <p>The <code>JSONR</code> class provides similar interfaces than 
 * <code>JSON</code> in the form of instance methods to evaluate and
 * validate any JSON values:
 * 
 * <blockquote>
 * <pre>Object map;
 *try {
 *    JSONR pattern = new JSONR("{\"pass\": null}");
 *    map = pattern.eval(
 *        "{\"pass\": true}"
 *        );
 *    map = pattern.eval(
 *        "{\"will this fail?\": true}"
 *        );
 *} catch (JSONR.Error e) {
 *    System.out.println(e.str());
 *}</pre>
 * </blockquote>
 * 
 * extend arrays
 * 
 * <blockquote>
 * <pre>JSONR pattern = new JSONR("[\".+\"]");
 *JSON.Array list = new JSON.Array(); 
 *JSONR.Error e = pattern.extend(
 *    list, "[\"my model\": \"is a constant\"]"
 *    );
 *if (e != null)    
 *    System.out.println(e.str());</pre>
 * </blockquote>
 * 
 * or update maps
 *  
 * <blockquote>
 * <pre>JSONR pattern = new JSONR("[\".+\"]");
 *JSON.Object map = new JSON.Object();
 *JSONR.Error e = pattern.update(
 *    map, "{\"will this fail?\": true}"
 *    );
 *if (e != null)    
 *    System.out.println(e.str());</pre>
 * </blockquote>
 * 
 * eventually with safety limits
 * 
 * <blockquote>
 * <pre>JSONR.Error e;
 *JSON.Array list = new JSON.Array();
 *JSONR pattern = new JSONR("[0]", 1, 7);
 *e = pattern.extend(list, "[1,2,3,4,5]"
 *if (e != null)    
 *    System.out.println(e.str());
 *e = pattern.extend(list, "[1,2,3,4,5,6,7,8,9,0]");
 *if (e != null)
 *    System.out.println(e.str());</pre>
 * </blockquote>
 * 
 * or throw a <code>JSONR.Error</code> and stop the evaluation.</p>
 * 
 * <h4>Extension Types</h4>
 * 
 * <p>Some application demand specialized data types, most notably date
 * and time types. This implementation provides only one, the most common
 * JSON extension type:
 * 
 * <table BORDER="1" WIDTH="100%" CELLPADDING="3" CELLSPACING="0" SUMMARY="">
 * <tr>
 *  <td><code>"yyyy-MM-ddTHH:mm:ss"</code></td>
 *  <td>a valid date and type value formated alike, as the defacto standard
 *  JavaScript 1.7 serialization of a date and time instance.</td>
 * </tr>
 * </table>
 * 
 * As a decent implementation of JSONR, this one is extensible and provides
 * a Java interface and enough singleton to do so easely.</p>
 * 
 * <p><b>Copyright</b> &copy; 2006 Laurent A.V. Szyster</p>
 * 
 * <p><b>Copyright</b> &copy; 2006-2007 Laurent A.V. Szyster</p>
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
     * <p><b>Copyright</b> &copy; 2006-2007 Laurent A.V. Szyster</p>
     */
    public static class Error extends JSON.Error {
        
        static final long serialVersionUID = 0L; // TODO: regenerate
        
        /**
         * Instanciate a JSONR error with an error message.
         * 
         * @param message the error message
         */
        public Error(String message) {super(message);}
        
        /**
         * ...
         */
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("JSONR error ");
            strb(sb);
            return sb.toString(); 
        }
        
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
     * <p><b>Copyright</b> &copy; 2006-2007 Laurent A.V. Szyster</p>
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
        public java.lang.Object value(java.lang.Object instance) throws Error ;
        /**
         * Evaluate a JSON string <em>and</em> validate it as regular
         * 
         * @param string to evaluate as a regular type and value
         * @return a regular <code>Object</code>, or
         * @throws JSON.Error if the string is irregular
         */
        public java.lang.Object eval(String string) throws JSON.Error;
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
        public java.lang.Object value (java.lang.Object instance) {
            return instance;
            }
        public java.lang.Object eval (String string) {
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
        public final java.lang.Object value (java.lang.Object instance) 
        throws Error {
            if (instance instanceof Boolean)
                return instance;
            else
                throw new Error(JSON.BOOLEAN_TYPE_ERROR);
        }
        public final java.lang.Object eval (String string) 
        throws JSON.Error {
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
            "Integer value error";
        public static final TypeInteger singleton = new TypeInteger();
        public final java.lang.Object value (java.lang.Object instance) 
        throws Error {
            if (instance instanceof Integer)
                return instance;
            else
                throw new Error(JSON.INTEGER_TYPE_ERROR);
        }
        public final java.lang.Object eval (String string) 
        throws JSON.Error {
            if (string != null) {
                try {
                    return new Integer(string);
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
        public final java.lang.Object value (java.lang.Object instance) 
        throws Error {
            if (instance instanceof Double)
                return instance;
            else if (instance instanceof Number)
                return new Double(((Number) instance).doubleValue());
            else
                throw new Error(JSON.DOUBLE_TYPE_ERROR);
        }
        public final java.lang.Object eval (String string) 
        throws JSON.Error {
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
        public final java.lang.Object value (java.lang.Object instance) 
        throws Error {
            BigDecimal b;
            if (instance instanceof BigDecimal) {
                b = (BigDecimal) instance;
            } else if (instance instanceof Number) {
                b = new BigDecimal(((Number) instance).doubleValue());
            } else
                throw new Error(JSON.DOUBLE_TYPE_ERROR);
            return b;
        }
        public final java.lang.Object eval (String string) 
        throws JSON.Error {
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
        public final java.lang.Object value (java.lang.Object instance) 
        throws Error {
            if (instance instanceof String) {
                return instance;
            } else
                throw new Error(JSON.STRING_TYPE_ERROR);
        }
        public final java.lang.Object eval (String string) 
        throws JSON.Error {
            if (string == null || string.length() == 0) 
                throw new Error(STRING_VALUE_ERROR);
            else
                return string;
        }
        public final Type copy() {return singleton;}
    }
    
    protected static final class TypeRegular implements Type {
        protected static final String IRREGULAR_STRING = 
            "irregular String";
        protected Pattern pattern = null;
        protected TypeRegular (Pattern pattern) {
            this.pattern = pattern;
        } 
        public TypeRegular (String expression) {
            pattern = Pattern.compile(expression);
        } 
        protected final java.lang.Object test (String string) throws Error {
            if (pattern.matcher(string).matches())
                return string;
            else
                throw new Error(IRREGULAR_STRING);
        }
        public final java.lang.Object value (java.lang.Object instance) 
        throws Error {
            if (instance instanceof String) {
                return this.test((String) instance);
            } else
                throw new Error(JSON.STRING_TYPE_ERROR);
        }
        public final java.lang.Object eval (String string) 
        throws JSON.Error {
            if (string != null)
                return this.test(string);
            else
                return null;
        }
        public Type copy() {return new TypeRegular(pattern);}
    }

    protected static final class TypeArray implements Type {
        public Type[] types = null;
        public TypeArray (Type[] types) {this.types = types;}
        public final java.lang.Object value (java.lang.Object instance) 
        throws Error {
            if (instance == null || instance instanceof ArrayList)
                return instance;
            else
                throw new Error(JSON.ARRAY_TYPE_ERROR);
        }
        public final java.lang.Object eval (String string) 
        throws JSON.Error {
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

    protected static final class TypeDictionary implements Type {
        protected static final String IRREGULAR_DICTIONARY = 
            "irregular Dictionary";
        public Type[] types;
        public TypeDictionary (Type[] types) {
            this.types = types;
            }
        public final java.lang.Object value (java.lang.Object instance) 
        throws Error {
            if (instance instanceof HashMap) {
                HashMap map = (HashMap) instance;
                if (map.keySet().iterator().hasNext())
                    return map;
                else
                    throw new Error(IRREGULAR_DICTIONARY);
            } else
                throw new Error(JSON.OBJECT_TYPE_ERROR);
        }
        public final java.lang.Object eval (String string) 
        throws JSON.Error {
            return (new JSONR(this)).eval(string);
        }
        public static final Type singleton = new TypeDictionary(new Type[]{
            new TypeRegular(".+"), TypeUndefined.singleton 
        });
        public final Type copy() {
            if (this == singleton) return singleton;
            return new TypeDictionary(new Type[]{types[0], types[1]});
        }
    }

    protected static final class TypeNamespace implements Type {
        protected static final String IRREGULAR_OBJECT = 
            "irregular Namespace";
        public Set names;
        public Set mandatory;
        public HashMap namespace;
        public TypeNamespace (HashMap ns) {
            namespace = ns;
            names = ns.keySet();
            mandatory = new HashSet();
            Iterator i = names.iterator();
            while (i.hasNext()) {
                String name = (String) i.next();
                java.lang.Object value = namespace.get(name); 
                if (!(
                    value instanceof TypeUndefined ||
                    value instanceof TypeArray ||
                    value instanceof TypeNamespace
                    ))
                    mandatory.add(name);
                }
            }
        public final java.lang.Object value (java.lang.Object instance) 
        throws Error {
            if (instance == null)
                return null;
            else if (instance instanceof HashMap) {
                HashMap map = (HashMap) instance;
                if (map.keySet().containsAll(mandatory))
                    return map;
                else
                    throw new Error(IRREGULAR_OBJECT);
            } else
                throw new Error(JSON.OBJECT_TYPE_ERROR);
        }
        public final java.lang.Object eval (String string) 
        throws JSON.Error {
            return (new JSONR(this)).eval(string);
        }
        public static final Type singleton = new TypeNamespace(new JSON.Object());
        public final Type copy() {
            if (this == singleton) return singleton;
            
            String name;
            JSON.Object o = new JSON.Object();
            Iterator iter = namespace.keySet().iterator();
            while (iter.hasNext()) {
                name = (String) iter.next();
                o.put(name, ((Type) namespace.get(name)).copy());
            }
            return new TypeNamespace(o);
        }
    }
    
    public static final Type BOOLEAN = TypeBoolean.singleton;
    public static final Type INTEGER = TypeInteger.singleton;
    public static final Type DOUBLE = TypeDouble.singleton;
    public static final Type DECIMAL = TypeDecimal.singleton;
    public static final Type STRING = TypeString.singleton;
    protected static final Type ARRAY = TypeArray.singleton;
    protected static final Type DICTIONARY = TypeDictionary.singleton;
    protected static final Type NAMESPACE = TypeNamespace.singleton;
    
    // the built-in extension types: just JSON's DateTime, ymmv ...
    
    protected static final class TypeDateTime implements JSONR.Type {
        public static final String name = "DateTime"; 
        protected static final SimpleDateFormat format = 
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        protected static final String DATETIME_VALUE_ERROR = 
            "DateTime value error";
        public final java.lang.Object value (java.lang.Object instance) 
        throws JSONR.Error {
            return eval((String) STRING.value(instance));
        }
        public final java.lang.Object eval (String string) 
        throws JSONR.Error {
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
        Integer limit;
        public TypeIntegerAbsolute (Integer gt) {this.limit = gt;}
        protected final java.lang.Object test (Integer i) throws Error {
            if (i.compareTo(BigInteger.ZERO) < 0)
                throw new Error(NEGATIVE_INTEGER);
            else if (i.compareTo(limit) <= 0)
                return i;
            else
                throw new Error(POSITIVE_INTEGER_OVERFLOW);
        } 
        public final java.lang.Object value (java.lang.Object instance) 
        throws Error {
            return test((Integer) INTEGER.value(instance));
        }
        public final java.lang.Object eval (String string) throws JSON.Error {
            return test((Integer) INTEGER.eval(string));
        }
        public final Type copy() {return new TypeIntegerAbsolute(limit);}
    }

    protected static final class TypeIntegerRelative implements Type {
        private static final String INTEGER_OVERFLOW = 
            "integer overflow";
        int limit;
        public TypeIntegerRelative (Integer gt) {
            this.limit = Math.abs(gt.intValue());
        }
        protected final java.lang.Object test (Integer i) 
        throws Error {
            if (Math.abs(i.intValue()) < limit)
                return i;
            else
                throw new Error(INTEGER_OVERFLOW);
        } 
        public final java.lang.Object value (java.lang.Object instance) 
        throws Error {
            return test((Integer) INTEGER.value(instance));
        }
        public final java.lang.Object eval (String string) 
        throws JSON.Error {
            return test((Integer) INTEGER.eval(string));
        }
        public final Type copy() {
            return new TypeIntegerRelative(new Integer(limit));
        }
    }

    private static final Double _double_zero = new Double(0.0);
    
    protected static final class TypeDoubleAbsolute implements Type {
        private static final String POSITIVE_DOUBLE_OVERFLOW = 
            "positive double overflow";
        private static final String NEGATIVE_DOUBLE = 
            "negative double";
        Double limit;
        public TypeDoubleAbsolute (Double gt) {this.limit = gt;}
        protected final java.lang.Object test (Double d) 
        throws Error {
            if (d.compareTo(_double_zero) < 0)
                throw new Error(NEGATIVE_DOUBLE);
            else if (d.compareTo(limit) <= 0)
                return d;
            else
                throw new Error(POSITIVE_DOUBLE_OVERFLOW);
        } 
        public final java.lang.Object value (java.lang.Object instance) 
        throws Error {
            return test((Double) DOUBLE.value(instance));
        }
        public final java.lang.Object eval (String string) 
        throws JSON.Error {
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
        protected final java.lang.Object test (Double d) throws Error {
            if (Math.abs(d.doubleValue()) <= limit)
                return d;
            else
                throw new Error(DOUBLE_OVERFLOW);
        } 
        public final java.lang.Object value (java.lang.Object instance) 
        throws Error {
            return test((Double) DOUBLE.value(instance));
        }
        public final java.lang.Object eval (String string) 
        throws JSON.Error {
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
        protected final java.lang.Object test (BigDecimal b) 
        throws Error {
            b.setScale(scale);
            if (b.compareTo(_decimal_zero) < 0)
                throw new Error(NEGATIVE_DECIMAL);
            else if (b.compareTo(limit) < 0)
                return b;
            else
                throw new Error(POSITIVE_DECIMAL_OVERFLOW);
        }
        public final java.lang.Object value (java.lang.Object instance) 
        throws Error {
            return test((BigDecimal) DECIMAL.value(instance));
        }
        public final java.lang.Object eval (String string) 
        throws JSON.Error {
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
        protected final java.lang.Object test (BigDecimal b) 
        throws Error {
            b.setScale(scale);
            if (b.abs().compareTo(limit) > 0)
                return b;
            else
                throw new Error(DECIMAL_OVERFLOW);
        }
        public final java.lang.Object value (java.lang.Object instance) 
        throws Error {
            return test((BigDecimal) DECIMAL.value(instance));
        }
        public final java.lang.Object eval (String string) 
        throws JSON.Error {
            return test((BigDecimal) DECIMAL.eval(string));
        }
        public Type copy() {return new TypeDecimalRelative(limit);}
    }
    
    protected static final Type compile(
        java.lang.Object regular, Map extensions, HashMap cache
        ) {
        String pattern = JSON.encode(regular);
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
                type = new TypeRegular(s);
            else
                type = STRING;
        } else if (regular instanceof Integer) {
            Integer i = (Integer) regular;
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
                type = TypeNamespace.singleton;
            else {
                String name;
                int i = 0;
                HashMap namespace = new HashMap();
                do {
                    name = (String) iter.next();
                    namespace.put(name, compile(
                        object.get(name), extensions, cache
                        )); i++;
                } while (iter.hasNext());
                if (i==1)
                    type = new TypeDictionary(new Type[]{
                        compile(name, extensions, cache),
                        compile(object.get(name), extensions, cache)
                        });
                else
                    type = new TypeNamespace(namespace);
            } 
        } else
            type = null;
        cache.put(pattern, type);
        return type;
    }
    
    public static final 
    Type compile(java.lang.Object regular, Map extensions) {
        return compile(regular, extensions, new HashMap());
    }
    
    public static final Type compile(String pattern, Map extensions)
    throws JSON.Error {
        return compile((new JSON()).eval(pattern), extensions);
    }
    
    public static final Type compile(String pattern)
    throws JSON.Error {
        return compile((new JSON()).eval(pattern), TYPES);
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
    
    public JSONR(java.lang.Object regular) throws JSON.Error {
        super(); type = compile(regular, TYPES);
    }
    
    public JSONR(java.lang.Object regular, int containers, int iterations) 
    throws JSON.Error {
        super(containers, iterations); type = compile(regular, TYPES);
    }
    
    public JSONR(String pattern) throws JSON.Error {
        super(); type = compile(pattern, TYPES);
    }
    
    public JSONR(String pattern, int containers, int iterations) 
    throws JSON.Error {
        super(containers, iterations); type = compile(pattern, TYPES);
    }
    
    public java.lang.Object eval(String json) 
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
        buf = new StringBuffer();
        it = new StringCharacterIterator(json);
        try {
            c = it.first();
            while (Character.isWhitespace(c)) c = it.next();
            if (c == '{') {
                if (type instanceof TypeNamespace) {
                    c = it.next();
                    namespace(o, ((TypeNamespace) type).namespace);
                    return null;
                } else if (type instanceof TypeDictionary) {
                    c = it.next();
                    dictionary(o, ((TypeDictionary) type).types);
                    return null;
                } else
                    return new Error(JSON.OBJECT_TYPE_ERROR);
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
    
    protected final java.lang.Object value(Type type) 
    throws JSON.Error {
        while (Character.isWhitespace(c)) c = it.next();
        switch(c){
        case '{': {
            if (type instanceof TypeNamespace) {
                TypeNamespace t = (TypeNamespace) type;
                c = it.next();
                return t.value(namespace(new JSON.Object(), t.namespace));
            } else if (type instanceof TypeDictionary) {
                TypeDictionary t = (TypeDictionary) type;
                c = it.next();
                return t.value(dictionary(new JSON.Object(), t.types));
            } else if (type == TypeUndefined.singleton) {
                c = it.next();
                return object(new JSON.Object());
            } else
                throw error(TypeNamespace.IRREGULAR_OBJECT);
        }
        case '[': {
            if (type instanceof TypeArray) { 
                c = it.next(); 
                Iterator types = ((TypeArray) type).iterator();
                if (types.hasNext())
                    return array(new JSON.Array(), types);
                else
                    return array(new JSON.Array());
            } else if (type == TypeUndefined.singleton) {
                c = it.next();
                return array(new JSON.Array());
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
        case ',': {c = it.next(); return JSON.COMMA;} 
        case ':': {c = it.next(); return JSON.COLON;}
        case ']': {c = it.next(); return JSON.ARRAY;} 
        case '}': {c = it.next(); return JSON.OBJECT;}
        case JSON._done:
            throw error(UNEXPECTED_END);
        default: 
            throw error(UNEXPECTED_CHARACTER);
        }
    }
    
    protected final java.lang.Object value(Type type, String name) 
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
    
    protected final java.lang.Object value(Type type, int index) 
    throws JSON.Error {
        try {
            return value(type);
        } catch (Error e) {
            e.jsonIndex = it.getIndex();
            e.jsonPath.add(0, new Integer(index));
            throw e;
        } catch (JSON.Error e) {
            e.jsonPath.add(0, new Integer(index));
            throw e;
        }
    }
    
    protected final java.lang.Object dictionary(Map o, Type[] types) 
    throws JSON.Error {
        if (--containers < 0) 
            throw error(CONTAINERS_OVERFLOW);
        
        java.lang.Object val;
        java.lang.Object token = value(types[0]);
        while (token != JSON.OBJECT) {
            if (!(token instanceof String))
                throw error(STRING_EXPECTED);
            
            if (--iterations < 0) 
                throw error(ITERATIONS_OVERFLOW);
            
            if (value() == JSON.COLON) {
                val = value(types[1]);
                if (val==JSON.COLON || val==JSON.COMMA || val==JSON.OBJECT || val==ARRAY)
                    throw error(VALUE_EXPECTED);
                
                o.put(token, val);
                token = value(types[0]);
                if (token == JSON.COMMA)
                    token = value();
            } else {
                throw error(COLON_EXPECTED);
            }
        }
        return o;
    }
    
    protected final java.lang.Object namespace(Map o, HashMap namespace) 
    throws JSON.Error {
        if (--containers < 0) 
            throw error(CONTAINERS_OVERFLOW);
        
        Type type;
        String name; 
        java.lang.Object val;
        java.lang.Object token = value();
        while (token != JSON.OBJECT) {
            if (!(token instanceof String))
                throw error(STRING_EXPECTED);
            
            if (--iterations < 0) 
                throw error(ITERATIONS_OVERFLOW);
            
            name = (String) token;
            type = (Type) namespace.get(name);
            if (value() == JSON.COLON) {
                if (type == null)
                    throw new Error(NAME_ERROR);
                else
                    val = value(type, name);
                if (val==JSON.COLON || val==JSON.COMMA || val==JSON.OBJECT || val==ARRAY)
                    throw error(VALUE_EXPECTED);
                
                o.put(name, val);
                token = value();
                if (token == JSON.COMMA)
                    token = value();
            } else {
                throw error(COLON_EXPECTED);
            }
        }
        return o;
    }
    
    protected final java.lang.Object array(List a, Iterator types) 
    throws JSON.Error {
        if (--containers < 0) 
            throw error(CONTAINERS_OVERFLOW);

        int i = 0;
        Type type = (Type) types.next();
        java.lang.Object token = value(type, i++);
        if (types.hasNext()) {
            while (token != JSON.ARRAY) {
                if (token==JSON.COLON || token==JSON.COMMA || token==JSON.OBJECT)
                    throw error(VALUE_EXPECTED);
                
                if (--iterations < 0) 
                    throw error(ITERATIONS_OVERFLOW);
             
                a.add(token);
                token = value(); 
                if (token == JSON.COMMA)
                    if (types.hasNext())
                        token = value((Type) types.next(), i++);
                    else
                        throw new Error(ARRAY_OVERFLOW);
            }
            if (types.hasNext())
                throw error(PARTIAL_ARRAY);
        } else {
            while (token != JSON.ARRAY) {
                if (token==JSON.COLON || token==JSON.COMMA || token==JSON.OBJECT)
                    throw error(VALUE_EXPECTED);
                
                if (--iterations < 0) 
                    throw error(ITERATIONS_OVERFLOW);
             
                a.add(token);
                token = value(); 
                if (token == JSON.COMMA)
                    token = value(type, i++);
            }
        }
        return a;
    }
    
}