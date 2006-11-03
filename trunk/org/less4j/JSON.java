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

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Static classes and methods to convert between JSON string and the
 * Java types: HashMap, ArrayList, String, Double, Boolean and null.
 * 
 * Note that JSON strings are expected to be complete. It is not recommended  
 * to parse or serialize large objects with this implementation as it will 
 * take large chunks of memory for object instances and buffer.
 * 
 * However, for relatively small JSON strings (below 16KB), this design is  
 * fast enough and yields a low memory footprint, without intermediate object 
 * instances (like org.json), no lexxer and no special type to instanciate 
 * before serialization (as in org.json.simple), no funky Java types casting 
 * or broken validation (which is why I forked org.stringtree.json ;-)
 * 
 * @author Laurent Szyster
 */
public class JSON {
    
    public static class SyntaxError extends Exception {
        private static final long serialVersionUID = 0L;
    }
    
    public static class Interpreter {
        
        private static final Object OBJECT_END = new Object();
        private static final Object ARRAY_END = new Object();
        private static final Object COLON = new Object();
        private static final Object COMMA = new Object();
        private static Map escapes = new HashMap();
        static {
            escapes.put(new Character('"'), new Character('"'));
            escapes.put(new Character('\\'), new Character('\\'));
            escapes.put(new Character('/'), new Character('/'));
            escapes.put(new Character('b'), new Character('\b'));
            escapes.put(new Character('f'), new Character('\f'));
            escapes.put(new Character('n'), new Character('\n'));
            escapes.put(new Character('r'), new Character('\r'));
            escapes.put(new Character('t'), new Character('\t'));
        }
        
        private CharacterIterator it;
        private char c;
        private Object token;
        private StringBuffer buf = new StringBuffer();
        
        public Object eval(String string) throws SyntaxError {
            it = new StringCharacterIterator(string);
            c = it.first();
            return value();
        }
        
        private char next() {c = it.next(); return c;}
        
        private void skip() {
            while (Character.isWhitespace(c)) {c = it.next();}
        }
        
        private Object value() throws SyntaxError {
            Object ret = null;
            skip();
            if (c == '"') {
                c = it.next();
                ret = string();
            } else if (c == '[') { // TODO: move down
                c = it.next();
                ret = array();
            } else if (c == ']') {
                ret = ARRAY_END;
            } else if (c == ',') { 
                ret = COMMA; // TODO: move up second
                c = it.next();
            } else if (c == '{') {
                c = it.next();
                ret = object();
            } else if (c == '}') { // TODO: move down
                ret = OBJECT_END;
                c = it.next();
            } else if (c == ':') {
                ret = COLON;
                c = it.next();
            } else if (c=='t'&&next()=='r'&&next()=='u'&&next()=='e') {
                ret = Boolean.TRUE;
            } else if (
                c =='f'&&next()=='a'&&next()=='l'&&next()=='s'&&next()=='e'
                ) {
                ret = Boolean.FALSE;
            } else if (c =='n'&&next()=='u'&& next()=='l'&&next()=='l') {
                ret = null;
            } else if (Character.isDigit(c) || c == '-') {
                ret = number(); // TODO: move 3thd position
            } else 
                throw new SyntaxError();
            token = ret;
            return ret;
        }
        
        private Object object() throws SyntaxError {
            HashMap ret = new HashMap();
            Object key = this.value();
            while (token != OBJECT_END) {
                this.value(); // colon
                ret.put(key, this.value());
                if (this.value() == COMMA) {
                    key = this.value();
                }
            }
            return ret;
        }
        
        private Object array() throws SyntaxError {
            ArrayList ret = new ArrayList();
            Object val = value();
            while (token != ARRAY_END) {
                ret.add(val);
                if (value() == COMMA) {
                    val = value();
                }
            }
            return ret;
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
                        buf.append(unicode()); c = it.next();
                    } else {
                        Object val = escapes.get(new Character(c));
                        if (val != null) {
                            buf.append(((Character) val).charValue());
                            c = it.next();
                        }
                    }
                } else {
                    buf.append(c); c = it.next();
                }
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
                default:
                    throw new SyntaxError();
                }
            }
            return (char) val;
        }
    }

    public static class TypeError extends Exception {
        private static final long serialVersionUID = 0L; 
    }
    
    public static Object value(String encoded) throws SyntaxError {
        return (new Interpreter()).eval(encoded);
    }

    public static HashMap object(String encoded) 
    throws SyntaxError, TypeError {
        Object o = value(encoded);
        if (!(o instanceof HashMap)) 
            throw new TypeError();
        else
            return (HashMap) o;
    }
    
    public static ArrayList array(String encoded) 
    throws SyntaxError, TypeError {
        Object o = value(encoded);
        if (!(o instanceof ArrayList)) 
            throw new TypeError();
        else
            return (ArrayList) o;
    }
    
    public static Double number(String encoded) 
    throws SyntaxError, TypeError {
        Object o = value(encoded);
        if (!(o instanceof Double)) 
            throw new TypeError();
        else
            return (Double) o;
    }
    
    public static String string(String encoded) 
    throws SyntaxError, TypeError {
        Object o = value(encoded);
        if (!(o instanceof String)) 
            throw new TypeError();
        else
            return (String) o;
    }
    
    public static void encode(StringBuffer sb, String s) {
        sb.append('"');
        CharacterIterator it = new StringCharacterIterator(s);
        for (
            char c = it.first(); 
            c != CharacterIterator.DONE; 
            c = it.next()
            ) {
            if (c == '"') sb.append("\\\"");
            else if (c == '\\') sb.append("\\\\");
            else if (c == '/') sb.append("\\/");
            else if (c == '\b') sb.append("\\b");
            else if (c == '\f') sb.append("\\f");
            else if (c == '\n') sb.append("\\n");
            else if (c == '\r') sb.append("\\r");
            else if (c == '\t') sb.append("\\t");
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
    
    private static final String _list = "[]";
    
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
    
    private static final String _null = "null";
    private static final String _true = "true";
    private static final String _false = "false";
    
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
    
    /**
     * <p>Without arguments:
     * 
     * <blockquote>
     * <pre>java JSON &lt; test.json</pre>
     * </blockquote>
     * 
     * reads a JSON string from STDIN, evaluate it and the serialize the
     * object instance tree back to STDOUT, print error to STDERR.</p>
     * 
     * <p>Arguments are expected to be JSON file names and
     * 
     * <blockquote>
     * <pre>java JSON test1.json test2.json</pre>
     * </blockquote>
     * 
     * reads a JSON string from each file named, evaluate them all and
     * serialze the object instance trees to STDOUT, printing errors to
     * STDERR.</p>
     * 
     * @param args
     */
    public static void main(String args[]) {
        if (args.length == 0) {
            byte[] buffer = new byte[16384];
            try {
                System.out.print(encode(value(
                    new String(buffer, 0, System.in.read(buffer), "UTF8")
                    )));
            } catch (Exception e) {
                e.printStackTrace(System.err);
            } finally {
                buffer = null;
            }
        } else for (int i=0; i < args.length; i++) try {
            System.out.print(encode(value(Simple.fileRead(args[i]))));
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }
    
}
