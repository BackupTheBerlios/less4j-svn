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
    
    protected static class Undefined implements Type {
        public static final Undefined singleton = new Undefined();
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

    protected static class BooleanNotNull implements Type {
        public static final BooleanNotNull singleton = new BooleanNotNull();
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

    protected static class IntegerNotNull implements Type {
        public static final IntegerNotNull singleton = new IntegerNotNull();
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

    protected static class DoubleNotNull implements Type {
        public static final DoubleNotNull singleton = new DoubleNotNull();
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

    protected static class StringNotNull implements Type {
        public static final StringNotNull singleton = new StringNotNull();
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
    
    protected static class StringRegular implements Type {
        Pattern pattern;
        public StringRegular (Pattern pattern) {
            this.pattern = pattern;
        } 
        public StringRegular (String expression) {
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
        public Object copy() {return new StringRegular(this.pattern);}
    }
    
    private static final Integer _integer_zero = new Integer(0);
    
    protected static class IntegerPositiveLTE implements Type {
        Integer limit;
        public IntegerPositiveLTE (Integer gt) {this.limit = gt;}
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
        public Object copy() {return new IntegerPositiveLTE(this.limit);}
    }

    private static final Double _double_zero = new Double(0.0);
    
    protected static class DoublePositiveLTE implements Type {
        Double limit;
        public DoublePositiveLTE (Double gt) {this.limit = gt;}
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
        public Object copy() {return new DoublePositiveLTE(this.limit);}
    }
    
    protected static class ObjectRegular implements Type {
        public JSONR ns = null;
        public ObjectRegular (JSONR ns) {this.ns = ns;}
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
        public Object copy() {return new ObjectRegular(new JSONR(ns));}
    }
    
    protected static class ArrayRegular implements Type {
        public Type[] types = null;
        public ArrayRegular (Type[] types) {this.types = types;}
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
        public Object copy() {return new ArrayRegular(types);}
    }
    
    protected static class Interpreter extends JSON.Interpreter {
        
        public Interpreter(int containers, int iterations) {
            super(containers, iterations);
        }
        
        public Error error(String message, String arg) {
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
        
        public Object object(JSONR namespace) 
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
        
        public Object array(Iterator types) 
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
        
        public Object value(Type type) 
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

/*

The purpose here is to validate a JSON value as it is interpreted, not once 
it has been evaluated. I assume that interactive input validation has 
allready taken place in the AJAX client application, here there is no need
to handle all value validation error, only to test the type and the general
form of the value submitted to the controller.

In a J2EE container, it is a requirement to compile a namespace from the 
configuration property

    less4j.namespace

for every servlet, dynamically recreating each time the model validation
data structure. Then, for each request, clone that tree of namespaces and
types, as fast as the JVM can. 

The expense is worth the benefit: there is nothing else to share between 
servlet threads. They are expected to use the same model and would otherwise 
repetitively have to synchronize namespaces and type validation objects, 
creating a nightmare of contention and potential subtle deadlocks.

Finally the balance achieved is profitable, because JSON regular expressions
are usually shorter than the JSON instances they validate, fast enough to 
instanciate without significant loss of performances in the case of a
type iteration. There is a worst case for large record instances, but cloning 
a namespace (or an iteration of types) will allways take a fraction of the
time spent applying it.

Note that any errors found in the JSON model must yield a ServletException
at initialization of the controler, because that's something the actor
cannot handle.



*/

/* let's abuse JSON and give rise to JSONR, a Regular JavaScript Object 
 * Notation. Here's a specification for the types and values
 * of a fictonnary application
 * 
 * {
 *   "type": {
 *     "bar": +0.0,            // a positive double
 *     "barf": 1000,           // a positive integer lower or equal 1000
 *     "barfo": -1.0           // a negative scale between -1.0 and -0.0
 *     "barfoo": "",            // any string, defaults to empty, not null
 *     "fobaro": null,          // any type, defaults to null
 *     "faboro": true,          // a boolean
 *     "rofabo": "[a-z]+",      // any lowercase non null string 
 *     "foo": [],               // an untyped array
 *     "barofo": [0],           // an array of positive integer
 *     "forabo": ["[a-z]*"],    // an array of lowercase string, maybe null
 *     "braoof": [
 *       +100, 
 *       -1.0, 
 *       "[a-z]+", 
 *       true
 *       ],                     // a typed and sized record
 *     "arbaoof": [[
 *       +100, 
 *       -1.0, 
 *       "[a-z]+", 
 *       true
 *       ]],                    // an array of records
 *     "oof": {},               // an object in this namespace
 *     "foobar": {
 *       "test": true
 *       },                     // an object in its own namespace
 *     }
 * 
 * This is a quite practical specification for public interfaces,
 * because it is easely portable wherever JSON and a Regular Expression 
 * engine are available (and that's pretty much everywhere now).
 * 
 * A big benefit of using JSON is to allow both the Java servlet
 * controller and the JavaScript client application to apply the same
 * namespace definition, to perform different parts of the validation 
 * in the same model.
 * 
 * An AJAX client will validate its input interactively, the Java server
 * does not have to bother with interaction errors and can focus on its
 * controller task.
 * 
 * This is more than enough to validate the bread
 * 
 * {
 *   "columns": ["name", "score", "scale", "pass"],
 *   "rows": [
 *     ["[a-Z\w]+", 1000, -1.0, true]
 *     ] 
 *   }
 * 
 * and butter
 * 
 * {
 *   "records": [
 *     {
 *       "name": "[a-Z\w]+", 
 *       "score": 1000, 
 *       "scale": -1.0, 
 *       "pass": true
 *       }
 *     ] 
 *   }
 * 
 * of most business application controllers, including the infamous object
 * known as the "one-to-many" relationship:
 * 
 * {
 *   "school": {
 *     "name": "[a-Z\w]+", 
 *     "address": ".+",
 *     "year": 2038
 *     }
 *   "students": {
 *     "columns": ["name", "score", "scale", "pass"],
 *     "rows": [["[a-Z\w]+", 1000, -1.0, true]]
 *     }
 *   }
 *   
 * the one applied for most transactions between relational databases and 
 * their web users, the one displayed by their browser and the one SQL
 * is made to select, update, insert or delete. 
 * 
 * Each form is implemented as a static subclass of Namespace, 
 * privately:
 * 
 *     TypedArray()
 *     TypedObject()
 *     DoubleGT(0.0)
 *     StringNotNull()
 *     ExistsNull()
 *     IntegerGT(0)
 *     BooleanNotNull(true)
 *     StringRegular("[a-z]+")
 *     StringRegulars("[a-z]+")
 *     IntegersGT(0)
 *     IntegersGT(0)
 *     
 * each with the same interface
 * 
 *     validate(Object data)
 *     
 * that validates untyped input data.
 * 
 * A namespace throws instances of Namespace.Error, and this should
 * be a fatal error for its application, because it signals invalid
 * input, possibly due to a malfunctionning network interface or
 * maybe caused by a malicious attacker.
 * */

/* TODO: Type
 * 
 * The idea is to evaluate a valid JSON statement and then recursively walk 
 * down it's object instance tree to compile a map of named type and regular 
 * expressions to use once values are instanciated.
 * 
 * Limited JSON
 * 
 * Then, at runtime, set a limit on the number of objects and arrays that can 
 * be instanciated. For instance, to evaluate no more than one transaction of 
 * 100 rows at most or to instanciate at most 100 records per batch.
 * 
 * Her is the API applied for those two case, as
 * 
 *     JSON.object("{}", 2, 100)
 *  
 * and
 *  
 *     JSON.array("[]", 101, 100)
 *     
 * How much input is acceptable and in which combination is the business of 
 * the controller.
 * 
 * Together with a 16KB limit on the UTF-8 encoded JSON body, here are
 * 
 *     $.jsonArrayPOST(101, 100);
 * 
 * and
 *     
 *     $.jsonObjectPOST(2, 100);
 *     
 * two convenient API to load, parse and validate a relatively small JSON 
 * string, which is precisely what you need to protect, what your program 
 * is supposed to control: entreprise resources. 
 * 
 * Here failure is not an option.
 * 
 * Within a modest 1024 bytes, a malicious attacker could instanciate
 * five hundred empty nested containers, managing to pegg the server through 
 * its X-JSON headers. Under DoS attack from a hundred attackers, that's a
 * load of 50.000 empty containers concurrently created and destroyed, 
 * straining the parser and the garbage collector, stressing CPU and RAM.
 * 
 * Think of what can be delivered by 100 concurrent attacker with 16KB.
 * 
 * Yeah, that's more than 800.000 void containers pegging your JVM.
 * 
 * The only solution is to limit those numbers ahead, testing the limits
 * as data is read, then as the interpreter parse it and before instanciation
 * of containers take place.
 * 
 * The limit on the maximum number of properties in an object is defined 
 * outside of the resource controller, at the application level as the
 * size of its namespace.
 * 
 * Namespace
 * 
 * with very different effects and yet with amazingly simple implementation,
 * thanks to a ever-usefull couple: a better convention.
 * 
 * As long as you stick with an object/configuration oriented-mind, validation
 * is bound to turn into the practical programming hell it is (and which
 * yield monsters frameworks like Spring).
 * 
 * Yet, if you consider validation globaly and functionaly, where decoupling
 * leverages more is between quantities and semantics. How input is expressed
 * regularly by text is related to its machine type, and both embrace the 
 * whole application.
 * 
 * Here's the interface:
 * 
 *     HashMap namespace = {"property": [type, pattern]}
 * 
 *     class Application extend Actor {
 *         static final Actions public;
 *     }
 *     
 *     Application $ = new Application(getConfiguration());
 * 
 * Also, it makes sharing a large validator quite practical and gives a rules 
 * to develop large a consistant network API: if you can share a name publicly,
 * you must share it in the sources too, somewhere in the same class.
 * 
 * Type of the property "username" should not vary depending on the 
 * resource of *this* application. Not all resource controllers have to support
 * it, but if they do, they must support the same definition of what a valid 
 * "username" is throughout the application.
 * 
 * Because special cases are not special enough to break the rules.
 * 
 **/

