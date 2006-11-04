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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Test {

    public static class TestJSON extends JSON {
        
        private static final String CRLF = "\r\n";
        private static final String INDT = "  ";
        
        public static void print(StringBuffer sb, Map map, String indent) {
            Object key; 
            Iterator it = map.keySet().iterator();
            if (!it.hasNext()) {
                sb.append("{}");
                return;
            }
            indent += INDT;
            sb.append('{');
            sb.append(indent);
            key = it.next();
            print(sb, key, indent);
            sb.append(": ");
            print(sb, map.get(key), indent);
            while (it.hasNext()) {
                sb.append(", ");
                sb.append(indent);
                key = it.next();
                print(sb, key, indent);
                sb.append(": ");
                print(sb, map.get(key), indent);
            }
            sb.append(indent);
            sb.append('}');
        }
        
        public static void print(StringBuffer sb, Iterator it, String indent) {
            if (!it.hasNext()) {
                sb.append("[]");
                return;
            }
            sb.append('[');
            indent += INDT;
            sb.append(indent);
            print(sb, it.next(), indent);
            while (it.hasNext()) {
                sb.append(", ");
                sb.append(indent);
                print(sb, it.next(), indent);
            }
            sb.append(indent);
            sb.append(']');
        }
        
        public static void print(StringBuffer sb, Object value, String indent) {
            if (value == null) 
                sb.append("null");
            else if (value instanceof Boolean)
                sb.append((
                    ((Boolean) value).booleanValue() ? "true": "false"
                    ));
            else if (value instanceof Number) 
                sb.append(value);
            else if (value instanceof String) 
                encode(sb, (String) value);
            else if (value instanceof Character) 
                encode(sb, ((Character) value).toString());
            else if (value instanceof Map)
                print(sb, (Map) value, indent);
            else if (value instanceof List) 
                print(sb, ((List) value).iterator(), indent);
            else if (value instanceof JSON) 
                encode(sb, ((JSON) value).string);
            else 
                encode(sb, value.toString());
        }
        
        public static String print(Object value) {
            StringBuffer sb = new StringBuffer();
            print(sb, value, CRLF);
            return sb.toString();
        }
        
        public TestJSON (String encoded) throws SyntaxError {
            super(encoded);
        }
        
        public boolean test(int scale) {
            try {
                for (int j=0; j < scale; j++) encode(eval(string));
                return true;
            } catch (SyntaxError e) {
                string = e.getMessage();
                return false;
            }
        }
        
    }
    
    /**
     * A test suite for less java.
     * 
     * Execute each test class's main with the same set of arguments,
     * maybe update that set.
     * 
     * @param args
     */
    public static void main(String[] args) {
        long t;
        int scale = 10000;
        TestJSON json;
        for (int i=0; i < args.length; i++) try {
            System.out.print(args[i]);
            json = new TestJSON(Simple.fileRead(args[i]));
            System.out.print(" = ");
            t = System.currentTimeMillis();
            json.test(scale);
            t = System.currentTimeMillis() - t;
            System.out.println(TestJSON.print(TestJSON.eval(json.string)));
            System.out.print(json.string.length()*scale);
            System.out.print(" characters in ");
            System.out.print(t);
            System.out.print(" milliseconds, ");
            System.out.print(json.string.length()*scale/t);
            System.out.println(" char/ms");
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

}
