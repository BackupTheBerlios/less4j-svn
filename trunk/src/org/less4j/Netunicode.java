/* Copyright (C) 2006-2007 Laurent A.V. Szyster

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
import java.util.NoSuchElementException;

/**
 * A protocol to encode and iterate strings without escaping characters, 
 * the fast and easy way to serialize and parse sequences of UNICODE strings 
 * (see netstrings, the 8-bit byte original).
 * 
 * <p><b>Copyright</b> &copy; 2006 Laurent A.V. Szyster</p>
 */
public class Netunicode {
    
    public static void encode (String[] strings, StringBuffer sb) {
        String string;
        for (int i = 0; i < strings.length; i++) {
            string = strings[i];
            sb.append(string.length());
            sb.append(':');
            sb.append(string);
            sb.append(',');
        }
    }

    public static String encode (String[] strings) {
        StringBuffer sb = new StringBuffer();
        encode(strings, sb);
        return sb.toString();
    }
    
    public static void encode (Iterator iter, StringBuffer sb) {
        Object item;
        String s;
        while(iter.hasNext()) {
            item = iter.next();
            if (item instanceof String)
                s = (String) item;
            else if (item instanceof String[])
                s = encode((String[])item);
            else if (item instanceof Iterator)
                s = encode((Iterator)item);
            else
                s = item.toString();
            sb.append(s.length());
            sb.append(':');
            sb.append((String)item);
            sb.append(',');
        }
    }

    public static String encode (Iterator iter) {
        StringBuffer sb = new StringBuffer();
        encode(iter, sb);
        return sb.toString();
    }

    protected static class Netiterator implements Iterator {
        
        private String buffer;
        private String item;
        private int size;
        private int prev = 0;
        private int pos, length, next;
        private boolean nostrip;
        
        public Netiterator(String encoded, boolean strip) {
            buffer = encoded;
            size = buffer.length();
            nostrip = !strip;
            try {next();} catch (NoSuchElementException e) {;}
        }
        
        public boolean hasNext() {return item != null;}
        
        public Object next() throws NoSuchElementException {
            if (item == null) 
                throw new NoSuchElementException();
            
            Object result = (Object) item;
            item = null;
            while (prev < size) {
                pos = buffer.indexOf (':', prev);
                if (pos < 1) prev = size;
                else {
                    try {
                        length = Integer.parseInt(
                            buffer.substring (prev, pos)
                            );
                    } catch (NumberFormatException e) {
                        prev = size;
                    }
                    next = pos + length + 1;
                    if (next >= size) prev = size;
                    else if (buffer.charAt(next) == ',') {
                        if (nostrip || next-pos > 1)
                            item = buffer.substring (pos+1, next);
                        prev = next + 1;
                    } else prev = size;
                }
            }
            return result;
        }
        
        public void remove() {} // optional interfaces? what else now ...
        
    }
    
    public static Iterator iterator(String encoded) {
        return new Netiterator(encoded, true);
    }
    
}
