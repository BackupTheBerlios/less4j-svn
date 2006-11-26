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
import java.util.HashMap;
import java.util.ArrayList;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A simple test suite of less4j base classes: Simple, SHA1, Netunicode, 
 * PublicNames, JSON and JSONR.
 * 
 * @author Laurent Szyster
 *
 */
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
        scale = scale*10;
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
    
    protected static class Semaphore {
        public int refcount;
        public Semaphore (int refcount) {this.refcount = refcount;}
        public synchronized void decref() {
            --refcount;
            notify();
        }
    }
    
    protected static class jsonEvalThread extends Thread {
        protected String input;
        protected Semaphore semaphore;
        public jsonEvalThread (String input, Semaphore semaphore) {
            this.input = input;
            this.semaphore = semaphore;
        }
        public void run () {
            try {
                JSON.eval(input, 65355, 65355);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                semaphore.decref();
            }
        }
    }
    
    protected static void jsonBenchmarkEval(String input, int scale) 
    throws JSON.Error {
        System.out.print("evaluated ");
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
        System.out.print("threaded, ");
        Semaphore lock = new Semaphore(scale);
        t = System.currentTimeMillis();
        for (int i = 0; i < scale; i++)
            new jsonEvalThread(input, lock).start();
        try {
            synchronized (lock) {
                while (lock.refcount > 0) lock.wait();
            }
        } catch (InterruptedException e) {
            ;
        } 
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
    }
    
    protected static void jsonBenchmarkStr(Object o, String output, int scale) 
    throws JSON.Error {
        System.out.print("serialized ");
        long t = System.currentTimeMillis();
        for (int i = 0; i < scale; i++)
            JSON.str(o);
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
                    String output = JSON.str(o);
                    System.out.println(JSON.repr(o));
                    jsonBenchmarkEval(input, scale);
                    jsonBenchmarkStr(o, output, scale);
                } catch (JSON.Error e) {
                    System.out.println(e.str());
                }
            }
        }
        return;
    }
 
    protected static class jsonrEvalThread extends Thread {
        protected String input;
        protected Semaphore semaphore;
        protected JSONR pattern;
        protected int containers, iterations;
        public jsonrEvalThread (
            String input, Semaphore semaphore,
            JSONR pattern, int containers, int iterations
            ) {
            this.input = input;
            this.semaphore = semaphore;
            this.pattern = pattern;
            this.containers = containers;
            this.iterations = iterations;
        }
        public void run () {
            try {
                pattern.eval(input, containers, iterations);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                semaphore.decref();
            }
        }
    }
    
    protected static void jsonrBenchmarkEval(
        String input, int scale, 
        JSONR pattern, int containers, int iterations
        ) 
    throws JSON.Error {
        System.out.print("evaluated ");
        long t = System.currentTimeMillis();
        for (int i = 0; i < scale; i++)
            pattern.eval(input, containers, iterations);
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
        System.out.print("threaded, ");
        Semaphore lock = new Semaphore(scale);
        t = System.currentTimeMillis();
        for (int i = 0; i < scale; i++)
            new jsonrEvalThread(
                input, lock, pattern, containers, iterations
                ).start();
        try {
            synchronized (lock) {
                while (lock.refcount > 0) lock.wait();
            }
        } catch (InterruptedException e) {
            ;
        } 
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
    }

    private static final String jsonrTest = 
        "{\"meta\": null, \"limits\": [65356, 65356]}";
    
    public static void jsonr(String dir, int scale) {
        JSONR testModel;
        try {
            testModel = new JSONR(jsonrTest); 
        } catch (JSON.Error e) {
            System.out.println(jsonrTest.substring(0, e.jsonIndex));
            System.out.println(e.str());
            e.printStackTrace();
            return;
        }
        String filename;
        String[] dirlist = (new File(dir)).list();
        if (dirlist == null)
            return;
        
        HashMap modelJSON;
        String model = Simple.fileRead(dir + "/test.jsonr");
        try {
            modelJSON = testModel.object(model, 65355, 65355);
        } catch (JSONR.Error e) {
            System.out.println(model.substring(0, e.jsonIndex));
            System.out.print("Type Error ");
            System.out.println(e.str());
            System.out.println(model.substring(e.jsonIndex));
            e.printStackTrace();
            return;
        } catch (JSON.Error e) {
            System.out.println(model.substring(0, e.jsonIndex));
            System.out.print("Syntax Error ");
            System.out.println(e.str());
            System.out.println(model.substring(e.jsonIndex));
            e.printStackTrace();
            return;
        }
        JSONR pattern = new JSONR(modelJSON.get("meta"));
        ArrayList limits = (ArrayList) modelJSON.get("limits");
        int containers = ((Number) limits.get(0)).intValue();
        int iterations = ((Number) limits.get(1)).intValue();
        dir += File.separatorChar; 
        Iterator filenames = Simple.iterator(dirlist);
        while (filenames.hasNext()) {
            filename = (String) filenames.next();
            if (filename.endsWith(".json")) {
                System.out.print(filename);
                String input = Simple.fileRead(dir + filename);
                System.out.print(" = ");
                try {
                    Object o = pattern.eval(input, containers, iterations);
                    System.out.print(" = ");
                    System.out.println(JSON.repr(o));
                    jsonrBenchmarkEval(
                        input, scale, pattern, containers, iterations
                        );
                } catch (JSONR.Error e) {
                    System.out.println(input.substring(0, e.jsonIndex));
                    System.out.print("Type Error ");
                    System.out.println(e.str());
                    System.out.println(input.substring(e.jsonIndex));
                } catch (JSON.Error e) {
                    System.out.println(input.substring(0, e.jsonIndex));
                    System.out.print("Syntax Error ");
                    System.out.println(e.str());
                    System.out.println(input.substring(e.jsonIndex));
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
                    json("test/json", scale);
                else if (args[i].equals("JSONR"))
                    jsonr("test/jsonr", scale);
                else if (args[i].equals("SHA1")) {
                    sha1(scale);
                }
            } else {
                json("test/json", scale);
                jsonr("test/jsonr", scale);
                sha1(scale);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

}
