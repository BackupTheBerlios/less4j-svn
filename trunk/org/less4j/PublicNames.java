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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

/**
 * <p>Public Names is a better URL protocol to <em>uniformely</em> encode a 
 * <em>well-articulated context graph</em> of UNICODE strings, it can be 
 * applied to bring some intelligent order to semantic chaos in network 
 * applications (see http://laurentszyster.be/blog/public-names).</p>
 * 
 * <p><b>Copyright</b> &copy; 2006 Laurent A.V. Szyster</p>
 * 
 * @author Laurent Szyster
 * @version 0.1.0
 */
public class PublicNames {
    
    public static String validate (
        String encoded, HashSet field, int horizon
        ) {
        Iterator names = Netunicode.iterator(encoded);
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
                return Netunicode.encode(valid.iterator());

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
        Iterator names = Netunicode.iterator(encoded);
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
                return Netunicode.encode(valid.iterator());

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
                return Netunicode.encode(valid.iterator());

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