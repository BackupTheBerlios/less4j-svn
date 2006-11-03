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

package org.less4j; // less java for fore applications

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Public Names is a protocol to encode uniformely a non-dispersed context 
 * graph of character strings as one UNICODE string. It's a better URL.
 * 
 * It is designed to bring some intelligent order to semantic chaos in 
 * network applications.
 * 
 * For more on Public Names, see:
 * 
 *   http://laurentszyster.be/blog/
 * 
 * @author Laurent Szyster
 * @version 0.1.0
 */
public class PublicNames {
    
    private static final char _COLON = ':';
    private static final char _COMMA = ',';

    public static void netunicode (String[] strings, StringBuffer sb) {
        String string;
        for (int i = 0; i < strings.length; i++) {
            string = strings[i];
            sb.append(string.length());
            sb.append(_COLON);
            sb.append(string);
            sb.append(_COMMA);
        }
    }

    public static String netunicode (String[] strings) {
        StringBuffer sb = new StringBuffer();
        netunicode(strings, sb);
        return sb.toString();
    }
    
    public static void netunicode (ArrayList items, StringBuffer sb) {
        Object item;
        Iterator iter = items.iterator();
        while(iter.hasNext()) {
            item = iter.next();
            if (item instanceof String) {
                sb.append(((String)item).length());
                sb.append(_COLON);
                sb.append((String)item);
                sb.append(_COMMA);
            } else if (item instanceof ArrayList) {
                sb.append(netunicode((ArrayList)item));
            }
        }
    }

    public static String netunicode (ArrayList items) {
        StringBuffer sb = new StringBuffer();
        netunicode(items, sb);
        return sb.toString();
    }

    /**
     * Decode a buffer of netunicodes as a new ArrayList of Strings,
     * maybe stripping out any null netunicoded strings (ie. the 
     * netnull literal "0:,"), returns <code>null</code> if the buffer
     * does not contain at least one netunicoded string.
     * 
     * @param buffer
     * @param strip
     * @return a non-empty ArrayList or null
     */
    public static ArrayList netunidecode (String buffer, boolean nostrip) {
        ArrayList list = null;
        int size = buffer.length();
        int prev = 0;
        int pos, length, next;
        while (prev < size) {
            pos = buffer.indexOf (_COLON, prev);
            if (pos < 1) {
                prev = size;
            } else {
                try {
                    length = Integer.parseInt(buffer.substring (prev, pos));
                } catch (NumberFormatException e) {
                    prev = size;
                    continue;
                }
                next = pos + length + 1;
                if (next >= size) {
                    prev = size;
                } else if (buffer.charAt(next) == _COMMA) {
                    if (nostrip || next-pos > 1) {
                        if (list == null) list = new ArrayList();
                        list.add(buffer.substring (pos+1, next));
                        };
                    prev = next + 1;
                } else {
                    prev = size;
                }
            }
        }
        return list;
    }

    public static ArrayList netunidecode (String buffer) {
        return netunidecode(buffer, false);
    }

    /**
     * A iterator of netunicoded strings, leaner on memory than the static 
     * method <code>netunidecode</code> method.
     * 
     * @author Laurent Szyster
     */
    public static class Netunidecode implements Iterator {
        private String buffer;
        private String item;
        private int size;
        private int prev = 0;
        private int pos, length, next;
        private boolean nostrip;
        public Netunidecode(String encoded, boolean strip) {
            buffer = encoded;
            size = buffer.length();
            nostrip = !strip;
            next();
        }
        public Netunidecode(String encoded) {
            buffer = encoded;
            size = buffer.length();
            nostrip = false;
            next();
        }
        public boolean hasNext() {
            return item != null;
            }
        public Object next() throws NoSuchElementException {
            if (item == null) 
                throw new NoSuchElementException();
            
            Object result = (Object)item;
            item = null;
            while (prev < size) {
                pos = buffer.indexOf (_COLON, prev);
                if (pos < 1) {
                    prev = size;
                } else {
                    try {
                        length = Integer.parseInt(
                            buffer.substring (prev, pos)
                            );
                    } catch (NumberFormatException e) {
                        prev = size;
                    }
                    next = pos + length + 1;
                    if (next >= size) {
                        prev = size;
                    } else if (buffer.charAt(next) == _COMMA) {
                        if (nostrip || next-pos > 1) {
                            item = buffer.substring (pos+1, next);
                            };
                        prev = next + 1;
                    } else {
                        prev = size;
                    }
                }
            }
            return result;
        }
        public void remove() {} // optional interfaces? what else now ...
    }
    
    public static String validate (
        String encoded, HashSet field, int horizon
        ) {
        Iterator names = new Netunidecode(encoded);
        if (!names.hasNext()) {
            if (field.contains(encoded))
                return null;
            
            field.add(encoded);
            return encoded;

        } else {
            String name;
            ArrayList valid = new ArrayList();
            do {
                name = validate((String)names.next(), field, horizon);
                if (name != null) {
                    valid.add(name);
                    if (field.size() >= horizon) 
                        break;
                    
                }
            } while (names.hasNext());
            if (valid.size() > 1) {
                Collections.sort(valid);
                return netunicode (valid);

            }
            if (valid.size() > 0) {
                return (String)valid.get(0);

            }
            return null;

        }
    }
        
    public static String validate (
        String encoded, HashSet field, int horizon, ArrayList tree
        ) {
        Iterator names = new Netunidecode(encoded);
        if (!names.hasNext()) {
            if (field.contains(encoded))
                return null;
            
            field.add(encoded);
            return encoded;

        } else {
            String name;
            ArrayList valid = new ArrayList();
            ArrayList branch = new ArrayList();
            do {
                name = validate((String)names.next(), field, horizon, branch);
                if (name != null) {
                    if (branch.size() > 1) {
                        tree.add(branch);
                    } else {
                        tree.add(name);
                    }
                    branch = new ArrayList();
                    valid.add(name);
                    if (field.size() >= horizon) 
                        break;
                    
                }
            } while (names.hasNext());
            if (valid.size() > 1) {
                Collections.sort(valid);
                return netunicode (valid);

            }
            if (valid.size() > 0) {
                return (String)valid.get(0);

            }
            return null;

        }
    }
    
    public static String validate (
        ArrayList articulated, HashSet field, int horizon
        ) {
        Iterator names = articulated.iterator();
        if (!names.hasNext()) {
            return null;
            
        } else {
            Object item;
            String name;
            ArrayList valid = new ArrayList();
            do {
                item = names.next(); 
                if (item instanceof String) {
                    name = (String)item;
                    if (!field.contains(name)) {
                        valid.add(name);
                        field.add(name);
                        if (field.size() >= horizon) 
                            break; 
                    }
                } else if (item instanceof ArrayList) {
                    name = validate((ArrayList)item, field, horizon);
                    if (name != null) 
                        valid.add(name);
                }
            } while (names.hasNext());
            if (valid.size() > 1) {
                Collections.sort(valid);
                return netunicode (valid);

            }
            if (valid.size() > 0) {
                return (String)valid.get(0);

            }
            return null;

        }
    }
        
    public String encoded;
    public ArrayList articulated;
    public HashSet field = new HashSet();
    
    public PublicNames(String encoded, int horizon) {
        articulated = new ArrayList();
        this.encoded = validate(encoded, field, horizon, articulated);
    }
    
    public PublicNames(ArrayList articulated, int horizon) {
        this.articulated = articulated;
        this.encoded = validate(articulated, field, horizon);
    }
    
    public static final int HORIZON = 126;
    
    public PublicNames(String encoded) {
        articulated = new ArrayList();
        this.encoded = validate(encoded, field, HORIZON, articulated);
    }
    
    public PublicNames(ArrayList articulated) {
        this.articulated = articulated;
        encoded = validate(articulated, field, HORIZON);
    }
    
} // that's all folks.