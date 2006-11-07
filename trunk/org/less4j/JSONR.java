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
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;

import java.util.regex.Pattern;
import java.text.StringCharacterIterator;

/**
 * A JSON regular pattern compiler, complete with extensible types and a safe
 * validating interpreter for J2EE applications.
 * 
 * <p>...</p>
 * 
 * @author Laurent Szyster
 *
 */
public class JSONR extends HashMap {
    
    private static final long serialVersionUID = 0L; 
    
    public static class Error extends JSON.Error  {
        private static final long serialVersionUID = 0L;
        public Error(String message) {super(message);}
    }
    
    /**
     * <p>An interface for the interpretation of URL query string as
     *  
     * <code>String</code>,
     * <code>Double</code>, 
     * <code>Integer</code>,
     * <code>Boolean</code> or 
     * <code>null</code>

     * and the validation of those types and values, enough to implement
     * a consistent API to interpret and validate URL and JSON requests
     * in the same application.</p>
     */
    protected static interface Type {
        /**
         * <p>Validate an Object instance type and value.</p>
         * 
         * @param instance of undefined type
         * @return true if the instance type and value are valid
         */
        public Object value(Object instance) throws Error ;
        /**
         * <p>Evaluate a string as a valid Object instance type and value</p>
         * 
         * @param string representing a valid instance type and value
         * @return the valid instance value, or
         * @throws a <code>Namespace.Error</code> if the instance is invalid 
         */
        public Object eval(String string) throws Error;
        public JSONR namespace();
        public Iterator iterator();
        public Object copy();
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
        public JSONR namespace() {return null;}
        public Iterator iterator() {return null;}
        public Object copy() {return singleton;}
    }

    protected static class TypeBoolean implements Type {
        public static final TypeBoolean singleton = new TypeBoolean();
        public Object value (Object data) throws Error {
            if (data instanceof Boolean)
                return data;
            else
                throw new Error("boolean error");
        }
        public Object eval (String string) throws Error {
            if (string.equals(JSON._true))
                return Boolean.TRUE;
            else if (string.equals(JSON._false))
                return Boolean.FALSE;
            else
                throw new Error("boolean error");
        }
        public JSONR namespace() {return null;}
        public Iterator iterator() {return null;}
        public Object copy() {return singleton;}
    }

    protected static class TypeInteger implements Type {
        public static final TypeInteger singleton = new TypeInteger();
        public Object value (Object data) throws Error {
            if (data instanceof Integer)
                return data;
            else
                throw new Error("not an integer");
        }
        public Object eval (String string) throws Error {
            if (string != null) {
                return new Integer(string);
            } else
                throw new Error("not an integer");
        }
        public JSONR namespace() {return null;}
        public Iterator iterator() {return null;}
        public Object copy() {return singleton;}
    }

    protected static class TypeDouble implements Type {
        public static final TypeDouble singleton = new TypeDouble();
        public Object value (Object data) throws Error {
            if (data instanceof Double)
                return data;
            else
                throw new Error("not a double");
        }
        public Object eval (String string) throws Error {
            if (string != null) {
                return new Double(string);
            } else
                throw new Error("not a double");
        }
        public JSONR namespace() {return null;}
        public Iterator iterator() {return null;}
        public Object copy() {return singleton;}
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
                throw new Error("not a string");
        }
        private static final String _null_string = "";
        public Object eval (String string) throws Error {
            if (string == null || _null_string.equals(string)) 
                throw new Error("null string");
            else
                return string;
        }
        public JSONR namespace() {return null;}
        public Iterator iterator() {return null;}
        public Object copy() {return singleton;}
    }
    
    protected static class TypeStringRegular implements Type {
        Pattern pattern;
        public TypeStringRegular (Pattern pattern) {
            this.pattern = pattern;
        } 
        public TypeStringRegular (String expression) {
            pattern = Pattern.compile(expression);
        } 
        public Object value (Object instance) throws Error {
            if (instance instanceof String) {
                if (pattern.matcher((String) instance).matches())
                    return instance;
                else
                    throw new Error("irregular string");
            } else
                throw new Error("not a string");
        }
        public Object eval (String string) throws Error {
            if (string == null && pattern.matcher(string).matches()) 
                return string;
            else
                throw new Error("irregular string");
        }
        public JSONR namespace() {return null;}
        public Iterator iterator() {return null;}
        public Object copy() {return new TypeStringRegular(this.pattern);}
    }
    
    private static final Integer _integer_zero = new Integer(0);
    
    protected static class TypeIntegerLTE implements Type {
        Integer limit;
        public TypeIntegerLTE (Integer gt) {this.limit = gt;}
        public Object value (Object instance) throws Error {
            if (instance instanceof Integer) {
                Integer i = (Integer) instance;
                if (i.compareTo(_integer_zero) < 0)
                    throw new Error("integer not positive");
                else if (limit.compareTo(i) >= 0)
                    return i;
                else
                    throw new Error("integer positive overflow");
            } else
                throw new Error("not an integer");
        }
        public Object eval (String string) throws Error {
            if (string != null) { 
                Integer i = new Integer(string);
                if (i.compareTo(_integer_zero) < 0)
                    throw new Error("integer not positive");
                else if (limit.compareTo(i) >= 0)
                    return i;
                else
                    throw new Error("integer positive overflow");
            } else
                throw new Error("not an integer");
        }
        public JSONR namespace() {return null;}
        public Iterator iterator() {return null;}
        public Object copy() {return new TypeIntegerLTE(this.limit);}
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
                    throw new Error("double positive overflow");
            } else
                throw new Error("not a double");
        }
        public Object eval (String string) throws Error {
            if (string != null) { 
                Double d = new Double(string);
                if (d.compareTo(_double_zero) < 0)
                    throw new Error("double not positive");
                else if (limit.compareTo(d) >= 0)
                    return d;
                else
                    throw new Error("double positive overflow");
            } else
                throw new Error("not a double");
        }
        public JSONR namespace() {return null;}
        public Iterator iterator() {return null;}
        public Object copy() {return new TypeDoubleLTE(this.limit);}
    }
    
    protected static class TypeObject implements Type {
        private JSONR ns = null;
        public TypeObject (JSONR ns) {this.ns = ns;}
        public Object value (Object instance) throws Error {
            if (instance instanceof HashMap)
                return instance;
            else
                throw new Error("not an object");
            }
        public Object eval (String instance) throws Error {
            throw new Error("use JSON instead");
        }
        public JSONR namespace() {return ns;}
        public Iterator iterator() {return null;}
        public Object copy() {return new TypeObject(new JSONR(ns));}
    }
    
    protected static class TypeArray implements Type {
        private Type[] types = null;
        public TypeArray (Type[] types) {this.types = types;}
        public Object value (Object instance) throws Error {
            if (instance instanceof ArrayList)
                return instance;
            else
                throw new Error("not an array");
            }
        public Object eval (String instance) throws Error {
            try {
                return (new Interpreter(65355, 65355)).array(
                    instance, this.iterator()
                    );
            } catch (JSON.Error e) {
                return null;
            }
        }
        public JSONR namespace() {return null;}
        public Iterator iterator() {return new Simple.ObjectIterator(types);}
        public Object copy() {return new TypeArray(types);}
    }
    
    public static class Interpreter extends JSON.Interpreter {
        
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
        
        public HashMap object(String json, JSONR ns) 
        throws JSON.Error {
            buf = new StringBuffer();
            it = new StringCharacterIterator(json);
            try {
                c = it.first();
                while (Character.isWhitespace(c)) c = it.next();
                if (c != '{')
                    throw error(" not an object");
                else
                    return (HashMap) object(ns);
            } finally {
                buf = null;
                it = null;
            }
        }
        
        public ArrayList array(String json, Iterator types) 
        throws JSON.Error {
            buf = new StringBuffer();
            it = new StringCharacterIterator(json);
            try {
                c = it.first();
                while (Character.isWhitespace(c)) c = it.next();
                if (c != '[')
                    throw error(" not an array");
                else
                    return (ArrayList) array(types);
            } finally {
                buf = null;
                it = null;
            }
        }
        
        protected Object object(JSONR namespace) 
        throws JSON.Error {
            String name; 
            Object val;
            if (--containers < 0) 
                throw error(" containers overflow");
            
            Type type;
            HashMap map = new HashMap();
            Object token = value();
            while (token != OBJECT) {
                if (!(token instanceof String))
                    throw error(" string expected ");
                
                if (--iterations < 0) 
                    throw error(" iterations overflow");
                
                name = (String) token;
                type = (Type) namespace.get(name);
                if (type == null)
                    throw error(" invalid name ", name);
                
                if (value() == COLON) {
                    val = value(type);
                    if (val==COLON || val==COMMA || val==OBJECT || val==ARRAY)
                        throw error(" value expected");
                    
                    map.put(name, val);
                    token = value();
                    if (token == COMMA)
                        token = value(type);
                } else {
                    throw error(" colon expected ", c);
                }
            }
            return map;
        }
        
        protected Object array(Iterator types) 
        throws JSON.Error {
            if (--containers < 0) 
                throw error(" containers overflow");

            ArrayList list = new ArrayList();
            Type type = (Type) types.next();
            Object token = value(type);
            if (types.hasNext()) {
                while (token != ARRAY) {
                    if (token==COLON || token==COMMA || token==OBJECT)
                        throw error(" value expected");
                    
                    if (--iterations < 0) 
                        throw error(" iterations overflow");
                 
                    list.add(token);
                    token = value(); 
                    if (token == COMMA)
                        if (types.hasNext())
                            token = value((Type) types.next());
                        else
                            throw error(" array overflow");
                }
                if (types.hasNext())
                    throw error(" partial array");
            } else {
                while (token != ARRAY) {
                    if (token==COLON || token==COMMA || token==OBJECT)
                        throw error(" value expected");
                    
                    if (--iterations < 0) 
                        throw error(" iterations overflow");
                 
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
                JSONR namespace = type.namespace();
                if (namespace != null) {
                    c = it.next();
                    if (namespace.isEmpty())
                        return object();
                    else
                        return object(namespace);
                    }
                else
                    throw error(" irregular object");
                }
            case '[': {
                Iterator types = type.iterator();
                if (types == null)
                    throw error(" irregular array");
                else { 
                    c = it.next(); 
                    if (types.hasNext())
                        return array(types);
                    else
                        return array();
                    }
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
                    throw error(" 'true' expected ", c);
            }
            case 'f': {
                if (next('a') && next('l') && next('s') && next('e')) {
                    c = it.next(); return type.value(Boolean.FALSE);
                } else
                    throw error(" 'false' expected ", c);
            }
            case 'n': {
                if (next('u') && next('l') && next('l')) {
                    c = it.next(); return type.value(null);
                } else
                    throw error(" 'null' expected ", c);
            }
            case ',': {c = it.next(); return COMMA;} 
            case ':': {c = it.next(); return COLON;}
            case ']': {c = it.next(); return ARRAY;} 
            case '}': {c = it.next(); return OBJECT;}
            case JSON.DONE:
                throw error(" unexpected end");
            default: 
                throw error(" unexpected character ", c);
            }
        }
        
    }
    
    public JSONR(String json) {
        // TODO: here comes a kind of compiler ... 
    }
    
    public JSONR (JSONR namespace) {
        String name;
        Iterator iter = namespace.keySet().iterator();
        while (iter.hasNext()) {
            name = (String) iter.next();
            put(name, ((Type) namespace.get(name)).copy());
        }
    }
    
    public HashMap filter(Map query) throws Error {
        Type type;
        String name;
        String[] strings;
        HashMap valid = new HashMap();
        Iterator iter = query.keySet().iterator();
        while (iter.hasNext()) {
            name = (String) iter.next();
            type = (Type) get(name);
            strings = (String[]) query.get(name);
            if (type != null && strings != null && strings.length > 0) {
                valid.put(name, type.eval(strings[0]));
            }
        }
        return valid;
    }
    
    public HashMap match(Map query) throws Error {
        Type type;
        String name;
        String[] strings;
        HashMap valid = new HashMap();
        Iterator iter = query.keySet().iterator();
        while (iter.hasNext()) {
            name = (String) iter.next();
            type = (Type) get(name);
            strings = (String[]) query.get(name);
            if (type != null && strings != null && strings.length > 0) {
                valid.put(name, type.eval(strings[0]));
            }
        }
        return valid;
    }
    
    public HashMap object(String json, int containers, int iterations) 
    throws JSON.Error {
        return (new Interpreter(containers, iterations)).object(json, this);
        }
            
    public ArrayList array(
        String json, int containers, int iterations, String name
        ) 
    throws JSON.Error {
        Type type = (Type) get(name);
        if (type == null)
            throw new Error("array name error");
        else {
            Iterator types = type.iterator();
            if (types == null)
                throw new Error("array type error");
            return (new Interpreter(containers, iterations)).array(
                json, types
                );
        }
    }
}

