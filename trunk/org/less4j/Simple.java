package org.less4j; // less java for more applications

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

import java.util.Iterator;

/**
 * A few simple utilities. Probably too simple for language experts, but 
 * damn usefull for J2EE application developers ... 
 * 
 * @author Laurent Szyster
 * @version 0.1.0
 */
public class Simple {
	
	// why do *I* have to write this once again?

    /**
     * The maximum block size for many file system.
     */
	public static final int fioBufferSize = 4096;
	
    /**
     * Try to read a complete file into a String.
     * 
     * <p>Usage:
     * 
     * <blockquote>
     * <pre>String resource = fileRead("my.xml");</pre>
     * </blockquote>
     *     
     * Note that since it does not throw exceptions, this method can
     * be used to load static class String members, piggy-backing the 
     * class loader to fetch text resources at runtime.</p> 
     * 
     * <p>This convenience allows the servlet controller to cache resources
     * when it is initialized.</p>
     * 
     * @param name the file's name
     * @return a String 
     */
    static public String fileRead (String name) {
        StringBuffer sb = new StringBuffer();
        try {
	        BufferedReader br = new BufferedReader(new FileReader(name));
	        try {
	            char[] buffer = new char[fioBufferSize];
	            int readLength = br.read(buffer);
	            while (readLength > 0) {
	                sb.append(buffer, 0, readLength);
	                readLength = br.read(buffer);
	            }
	            if (readLength > 0){
	                sb.append(buffer, 0, readLength);
	            }
	        } finally {
	            br.close();
	        }
        } catch (IOException e) {
        	return null;
        }
        return sb.toString();
    }

    /**
     * Try to read a complete file into a fixed size byte buffer, then
     * decode it all at once with the given encoding and return a String
     * or null if nothing was read, if the buffer has overflowed or if
     * an exception was raised.
     * 
     * <p>Usage:
     * 
     * <blockquote>
     * <pre>String resource = fileBuffer("my.xml", 16384);</pre>
     * </blockquote>
     * 
     * Note that since it does not throw exceptions, this method can
     * be used to load static class String members, piggy-backing the 
     * class loader to fetch text resources at runtime.</p> 
     *     
     * <p>This convenience allows the less4j servlet controller to cache 
     * resources  with a limited length when it is initialized. For obvious 
     * memory rationing, CPU load and network performance issues, those 
     * permanent resources should be smaller than a given buffer size.</p>
     * 
     * <p>In less4j applications, that would be 16KB, the largest UDP
     * datagram size in use by IP network peers and their defacto
     * standard for TCP applications (ymmv).</p>
     * 
     * @param name the file's name
     * @param buffer of bytes to fill
     * @param encoding to decode UNICODE string from the bytes read
     * @return a UNICODE String or null 
     */
    static public String fileBuffer (
        String name, byte[] buffer, String encoding
        ) {
        try {
            FileInputStream f = new FileInputStream(name);
            try {
                int l = f.read(buffer, 0, buffer.length);
                if (l > 0 && l < buffer.length){
                    return new String(buffer, 0, l, encoding);
                }
            } finally {
                f.close();
            }
        } catch (IOException e) {;}
        return null;
    }

    public static final int netBufferSize = 16384;
    
    /**
    * Try to read a complete file into a buffer of 16K bytes, then
    * decode it all at once with the given encoding and return a String
    * or null if nothing was read, if the buffer has overflowed or if
    * an exception was raised.

     * @param name the file's name
     * @param encoding to decode UNICODE string from the bytes read
     * @return a UNICODE String or null 
    */
    static public String fileBuffer (String name, String encoding) {
        return fileBuffer(name, new byte[netBufferSize], encoding);
    }
    
    private static final String netEncoding = "UTF8";
    
    /**
     * Try to read a complete file into a buffer of 16K bytes, then
     * decode it all at once as UTF-8 and return a String or null if nothing 
     * was read, if the buffer has overflowed or if an exception was raised.

      * @param name the file's name
      * @param encoding to decode UNICODE string from the bytes read
      * @return a UNICODE String or null 
     */
     static public String fileBuffer (String name) {
         return fileBuffer(name, new byte[netBufferSize], netEncoding);
     }
     
	/**
	 * A no-nonsense implementation of the Iterator interface for
	 * primitive array of untyped Java objects. 
	 * 
	 * <p>Usage:
	 * 
     * <blockquote>
	 * <pre>new ObjectIterator (new Object[]{1,2,3})</pre>
     * </blockquote>
     *     
     * Note that the <code>Iterator.remove</code> interface is <em>not</em> 
     * implemented. By the way, if you find a good reason for optional
     * interfaces in an API specification, send it to Sun, they need one.</p>
	 */
	protected static class ObjectIterator implements Iterator {
		private Object[] _objects;
		private int _index = -1;
		public ObjectIterator (Object[] objects) {_objects = objects;}
		public boolean hasNext () {return _index + 1 < _objects.length;}
		public Object next () {_index++; return _objects[_index];}
		public void remove () {/* optional interface? what else now ...*/}
	}

	/**
	 * A duck-typing convenience to iterate around a "primitive" array. 
	 * 
	 * <p>Usage:
	 * 
     * <blockquote>
	 * <pre>Iterator iter = Simple.iterate(new Object[]{x, y, z});</pre>
     * </blockquote>
	 *     
	 * or, by the virtues of inheritance and convention, in a less4j
	 * controller application:
	 * 
     * <blockquote>
	 * <pre>Iterator iter = $.iterate(new Object[]{x, y, z});</pre>
     * </blockquote>
	 *  
	 * Usefull to iterate through final arrays, a prime construct in web
	 * application controllers where check lists and filter sets made of
	 * primitive array abound (usually to enforce business rules).</p>
	 * 
	 * @param objects the array to iterate through
	 * @return iterator yields all objects in the array
	 */
	public static Iterator iterator (Object[] objects) {
		return new ObjectIterator(objects);
		}
	
    // a simplistic "opaque" type caster for Java 1.4.2 non-string
    // primitive type wrappers that can be expressed as JSON (some 
    // sort of web2 generics ;-)
    
    /*
     * Usage
     * 
     * Double d = (Double) cast(DOUBLE, "0.123");
     * 
     * This is only usefull when you need to dynamically map a type
     * caster to an argument name. But then, this is what happens for
     * all network peers ... 
     * 
     * */

    public static final Integer NULL = new Integer(0);
    public static final Integer INTEGER = new Integer(1);
    public static final Integer DOUBLE = new Integer(2);
    public static final Integer BOOLEAN = new Integer(3);
    public static final Integer LONG = new Integer(4);
    
    public static Object cast (Integer type, String s) {
        Object result = null;
        switch (type.intValue()) {
            case 0:;
            case 1: result = (Object) Integer.valueOf(s);
            case 2: result = (Object) Double.valueOf(s);
            case 3: result = (Object) Boolean.valueOf(s);
            case 5: result = (Object) Long.valueOf(s);
        } // that's safe, simple and fast ;-)
        return result;
    }
}
