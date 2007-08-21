/* Copyright (C) 2006 Laurent A.V. Szyster

This library is free software; you can redistribute it and/or modify
it under the terms of version 2 of the GNU Lesser General Public License as
published by the Free Software Foundation.

   http://www.gnu.org/copyleft/lesser.html

This library is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

You should have received a copy of the GNU Lesser General Public License
along with this library; if not, write to the Free Software Foundation, 
Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA */

package org.less4j.tests; // less java for more applications

import org.less4j.*;

import java.io.File;
import java.util.Iterator;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * A simple test suite of less4j base classes: Simple, SHA1, Netunicode, 
 * PublicNames, JSON and JSONR.
 * 
 * <p><b>Copyright</b> &copy; 2006 Laurent A.V. Szyster</p>
 * 
 * @version 0.10
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
    
    protected static class ThreadCount {
        private int count;
        public ThreadCount (int count) {this.count = count;}
        public synchronized void decr() {
            if (--count == 0) notify();
        }
        public synchronized boolean zero() {
            try {wait();} catch (InterruptedException e) {;}
            return count == 0;
        }
    }
    
    protected static class jsonEvalThread extends Thread {
        protected String input;
        protected ThreadCount tc;
        public jsonEvalThread (String input, ThreadCount tc) {
            this.input = input;
            this.tc = tc;
        }
        public void run () {
            try {
                for (int i=0;i<10;i++) 
                    (new JSON()).eval(input);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                tc.decr();
            }
        }
    }
    
    protected static void jsonBenchmarkEval(String input, int scale) 
    throws JSON.Error {
        System.out.print("evaluated ");
        long t = System.currentTimeMillis();
        for (int i = 0; i < scale; i++)
            (new JSON()).eval(input);
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
        ThreadCount tc = new ThreadCount(scale/10);
        t = System.currentTimeMillis();
        for (int i = 0; i < scale/10; i++)
            new jsonEvalThread(input, tc).start();
        while (!tc.zero()) {;}
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
            JSON.encode(o);
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
        Iterator filenames = Simple.iter(dirlist);
        while (filenames.hasNext()) {
            filename = (String) filenames.next();
            if (filename.endsWith(".json")) {
                System.out.print(filename);
                String input = Simple.read(dir + filename);
                try {
                    Object o = (new JSON()).eval(input);
                    System.out.print(" = ");
                    String output = JSON.encode(o);
                    System.out.println(JSON.repr(o));
                    jsonBenchmarkEval(input, scale);
                    jsonBenchmarkStr(o, output, scale);
                } catch (JSON.Error e) {
                    System.out.println(e.toString());
                }
            }
        }
        return;
    }
 
    protected static class jsonrEvalThread extends Thread {
        protected String input;
        protected ThreadCount tc;
        protected JSONR pattern;
        protected int containers, iterations;
        public jsonrEvalThread (
            String input, ThreadCount tc, JSONR pattern
            ) {
            this.input = input;
            this.tc = tc;
            this.pattern = pattern;
        }
        public void run () {
            try {
                for (int i=0; i<10; i++)
                    pattern.eval(input);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                tc.decr();
            }
        }
    }
    
    protected static void jsonrBenchmarkEval(
        String input, int scale, JSONR pattern
        ) 
    throws JSON.Error {
        System.out.print("evaluated ");
        long t = System.currentTimeMillis();
        for (int i = 0; i < scale; i++)
            pattern.eval(input);
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
        ThreadCount tc = new ThreadCount(scale/10);
        t = System.currentTimeMillis();
        for (int i = 0; i < scale/10; i++)
            new jsonrEvalThread(input, tc, new JSONR(
                pattern.type, pattern.containers, pattern.iterations
                )).start();
        while (!tc.zero()) {;}
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
            System.out.println(e.toString());
            e.printStackTrace();
            return;
        }
        String filename;
        String[] dirlist = (new File(dir)).list();
        if (dirlist == null)
            return;
        
        JSON.Object modelJSON;
        JSONR pattern; 
        String model = Simple.read(dir + "/test.jsonr");
        try {
            modelJSON = testModel.object(model);
            pattern = new JSONR(modelJSON.get("meta"));
        } catch (JSONR.Error e) {
            System.out.println(model.substring(0, e.jsonIndex));
            System.out.print("Type Error ");
            System.out.println(e.toString());
            System.out.println(model.substring(e.jsonIndex));
            e.printStackTrace();
            return;
        } catch (JSON.Error e) {
            System.out.println(model.substring(0, e.jsonIndex));
            System.out.print("Syntax Error ");
            System.out.println(e.toString());
            System.out.println(model.substring(e.jsonIndex));
            e.printStackTrace();
            return;
        }
        JSON.Array limits = modelJSON.A("limits", new JSON.Array());
        pattern.containers = limits.intValue(0, 65355);
        pattern.iterations = limits.intValue(1, 65355);
        dir += File.separatorChar; 
        Iterator filenames = Simple.iter(dirlist);
        while (filenames.hasNext()) {
            filename = (String) filenames.next();
            if (filename.endsWith(".json")) {
                System.out.print(filename);
                String input = Simple.read(dir + filename);
                System.out.print(" = ");
                try {
                    Object o = pattern.eval(input);
                    System.out.print(" = ");
                    System.out.println(JSON.repr(o));
                    jsonrBenchmarkEval(
                        input, scale, pattern
                        );
                } catch (JSONR.Error e) {
                    if (e.jsonIndex > -1) {
                        System.out.println(input.substring(0, e.jsonIndex));
                        System.out.print("Irregular JSON value ");
                        System.out.println(e.toString());
                        System.out.println(input.substring(e.jsonIndex));
                    } else {
                        System.out.print("Irregular JSON value ");
                        System.out.println(e.toString());
                    }
                } catch (JSON.Error e) {
                    if (e.jsonIndex > -1) {
                        System.out.println(input.substring(0, e.jsonIndex));
                        System.out.print("Invalid JSON string ");
                        System.out.println(e.toString());
                        System.out.println(input.substring(e.jsonIndex));
                    } else {
                        System.out.print("Invalid JSON string ");
                        System.out.println(e.toString());
                    }
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
