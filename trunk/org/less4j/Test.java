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

import java.io.File;
import java.util.Iterator;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Test {
    
    private static String sha1Text = (
        "a relatively short string, but long enough to simulate a key or URL "
        );
    private static byte[] sha1Salt = "*9a7short1but2secret1key*".getBytes();
    
    public static long sha1SUN(int scale, byte[] sha1Text) 
    throws NoSuchAlgorithmException {
        MessageDigest md;
        System.out.print(scale);
        System.out.print(" message of ");
        System.out.print(sha1Text.length);
        System.out.print(" bytes digested in ");
        long t = System.currentTimeMillis();
        for (int i = 0; i < scale; i++) {
            md = MessageDigest.getInstance("SHA1");
            md.update(sha1Text);
            md.update(sha1Salt);
            md.digest();
        }
        t = System.currentTimeMillis() - t;
        System.out.print(t);
        if (t > 0) {
            System.out.print(" ms or ");
            System.out.print(scale/t);
            System.out.println(" digests per ms.");
        } else
            System.out.println(" milliseconds.");
        return t;
    }
    
    public static long sha1Jython(int scale, byte[] sha1Text) {
        SHA1 md;
        System.out.print(scale);
        System.out.print(" message of ");
        System.out.print(sha1Text.length);
        System.out.print(" bytes digested in ");
        long t = System.currentTimeMillis();
        for (int i = 0; i < scale; i++) {
            md = new SHA1();
            md.update(sha1Text);
            md.update(sha1Salt);
            md.hexdigest();
        }
        t = System.currentTimeMillis() - t;
        System.out.print(t);
        if (t > 0) {
            System.out.print(" ms, ");
            System.out.print(scale/t);
            System.out.println(" digests per ms.");
        } else
            System.out.println(" milliseconds.");
        return t;
    }

    public static void sha1(int scale) {
        int i;
        StringBuffer sb = new StringBuffer();
        sb.append(sha1Text);
        byte[] sha1TextShort = sb.toString().getBytes();
        for (i=0;i<4;i++) sb.append(sb.toString());
        byte[] sha1TextLong = sb.toString().getBytes();
        for (i=0;i<4;i++) sb.append(sb.toString());
        byte[] sha1TextHuge = sb.toString().getBytes();
        System.out.println("Jython SHA1");
        long jythonS = sha1Jython(scale, sha1TextShort);
        long jythonL = sha1Jython(scale/10, sha1TextLong);
        long jythonH = sha1Jython(scale/100, sha1TextHuge);
        System.out.println();
        System.out.println("Java SHA1");
        try {
            long javaS = sha1SUN(scale, sha1TextShort);
            long javaL = sha1SUN(scale/10, sha1TextLong);
            long javaH = sha1SUN(scale/100, sha1TextHuge);
            System.out.println();
            System.out.print(sha1TextShort.length);
            System.out.print(" bytes: Java 100 Jython ");
            System.out.println(100*javaS/jythonS);
            System.out.print(sha1TextLong.length);
            System.out.print(" bytes: Java 100, Jython ");
            System.out.println(100*javaL/jythonL);
            System.out.print(sha1TextHuge.length);
            System.out.print(" bytes: Java 100, Jython ");
            System.out.println(100*javaH/jythonH);
        } catch (NoSuchAlgorithmException e) {}
    }
    
    public static void json(String dir, int scale) {
        String filename;
        String[] dirlist = (new File(dir)).list();
        if (dirlist == null)
            return;
        
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
                    String output = JSON.repr(o);
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
                    System.out.print("... ");
                    t = System.currentTimeMillis();
                    for (int i = 0; i < scale; i++)
                        JSON.repr(o);
                    t = System.currentTimeMillis() - t;
                    System.out.print(output.length()*scale);
                    if (t > 0) {
                        System.out.print(" characters in ");
                        System.out.print(t);
                        System.out.print(" milliseconds, ");
                        System.out.print(output.length()*scale/t);
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
        return;
    }

    /**
     * A test suite for less java.
     * 
     * @param args
     */
    public static void main(String[] args) {
        int scale = 1000;
        if (args.length > 0)
            scale = Integer.parseInt(args[0]);
        try {
            if (args.length > 1) for (int i=1; i < args.length; i++) {
                if (args[i].equals("JSON"))
                    json("test", scale);
                else if (args[i].equals("SHA1")) {
                    sha1(scale);
                }
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

}
