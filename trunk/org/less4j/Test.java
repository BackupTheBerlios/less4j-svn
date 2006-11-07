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

import java.io.File;
import java.util.Iterator;


public class Test {
    
    public static boolean json(String dir, int scale) {
        String filename;
        String[] dirlist = (new File(dir)).list();
        if (dirlist == null)
            return false;
        
        dir += File.separatorChar; 
        Iterator filenames = Simple.iterator(dirlist);
        while (filenames.hasNext()) {
            filename = (String) filenames.next();
            if (filename.endsWith(".json")) {
                System.out.print(filename);
                String input = Simple.fileRead(dir + filename);
                try {
                    Object o = JSON.eval(input, 65355, 65355);
                    System.out.print(" = ");
                    System.out.println(JSON.print(o));
                    long t = System.currentTimeMillis();
                    for (int i = 0; i < scale; i++)
                        JSON.eval(input, 65355, 65355);
                    t = System.currentTimeMillis() - t;
                    System.out.print(input.length()*scale);
                    if (t > 0) {
                        System.out.print(" characters in ");
                        System.out.print(t);
                        System.out.print(" milliseconds, ");
                        System.out.print(input.length()*scale/t);
                        System.out.print(" char/ms, ");
                        System.out.print(scale/t);
                        System.out.println(" object/ms");
                    } else {
                        System.out.println(" characters in zero milliseconds");
                    }
                } catch (JSON.Error e) {
                    System.out.print(" ! ");
                    System.out.println(e.getMessage());
                }
            }
        }
        return true;
    }

    /**
     * A test suite for less java.
     * 
     * @param args
     */
    public static void main(String[] args) {
        try {
            json("test", 10000);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

}
