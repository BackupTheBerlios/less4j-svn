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

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.util.Iterator;
import java.util.Map;

/**
 * A few utilities too simple for language experts, but damn usefull for 
 * application developers.
 * 
 * <p><b>Copyright</b> &copy; 2006 Laurent A.V. Szyster</p>
 * 
 * @version 0.10
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
     * <h4>Synopsis</h4>
     * 
     * <p>...
     * 
     * <blockquote>
     * <pre>String resource = fileBuffer("my.xml", new byte[16384], "UTF-8");</pre>
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

    /**
     * <p>Sixteen Kilobytes (16384 8-bit bytes) represent 3,4 pages of
     * 66 lines and 72 columns of ASCII characters. Much more information 
     * than what most of us can digest in a few minutes. It's a reasonable
     * maximum to set for an application web interface updated by its
     * user every few seconds.</p>
     * 
     * <p>Also, 16KB happens to be the defacto standard buffer size
     * for TCP peers and networks since it's the maximum UDP datagram size
     * in use. Which makes anything smaller than this limit a better candidate 
     * for the lowest possible IP network latency.</p>
     * 
     * <p>Finally, sixteen KB buffers can hold more than 65 thousand concurrent
     * responses in one GB of RAM, a figure between one and two orders of 
     * magnitude larger than what you can reasonably expect from a J2EE 
     * container running some commodity hardware. At an average speed of
     * 0.5 millisecond per concurrent request/response 16KB buffers sums
     * up to 36MBps, less than 1Gbps.</p>
     * 
     * <p>So, that sweet sixteen spot is also a safe general maximum for a 
     * web 2.0 application controller that needs to keep lean on RAM and
     * wants the fastest possible network and the smallest possible
     * imbalance between input and output.</p>
     */
    public static final int netBufferSize = 16384;
    
    /**
     * Try to read a complete file into a buffer of 16K bytes, then
     * decode it all at once with the given encoding and return a String
     * or null if nothing was read, if the buffer has overflowed or if
     * an exception was raised.
     *
     * <h4>Synopsis</h4>
     * 
     * <p>...
     * 
     * <blockquote>
     * <pre>String resource = fileBuffer("my.xml", "UTF-8");</pre>
     * </blockquote>
     * 
     * ...</p>
     * 
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
     *
     * <h4>Synopsis</h4>
     * 
     * <p>...
     * 
     * <blockquote>
     * <pre>String resource = fileBuffer("my.xml");</pre>
     * </blockquote>
     * 
     * ...</p>
     * @param name the file's name
     * @return a UNICODE String or null 
     */
     static public String fileBuffer (String name) {
         return fileBuffer(name, new byte[netBufferSize], netEncoding);
     }
     
	protected static class ObjectIterator implements Iterator {
		private Object[] _objects;
		private int _index = -1;
		public ObjectIterator (Object[] objects) {_objects = objects;}
		public boolean hasNext () {return _index + 1 < _objects.length;}
		public Object next () {_index++; return _objects[_index];}
		public void remove () {/* optional interface? what else now ...*/}
	}

	/**
	 * A convenience to iterate around a "primitive" array. 
	 * 
	 * <h4>Synopsis</h4>
     * 
     * <p>...
	 * 
     * <blockquote>
	 * <pre>Iterator iter = Simple.iterator(new Object[]{x, y, z});</pre>
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
    
    protected static class MapIterator implements Iterator {
        private Map _map;
        private Iterator _iter;
        public MapIterator (Map map, Iterator iter) {
            _map = map; _iter = iter;
            }
        public boolean hasNext () {return _iter.hasNext();}
        public Object next () {return _map.get(_iter.next());}
        public void remove () {/* optional interface? what else now ...*/}
    }

    public static Iterator itermap (Map map, Object[] keys) {
        return new MapIterator(map, iterator(keys));
        }
    
    /**
     * Encode a <code>unicode</code> string in the given characterset 
     * <code>encoding</code> or use the default if a 
     * <code>UnsupportedEncodingException</code> was throwed.
     * 
     * @param unicode the <code>String</code> to encode
     * @param encoding the character set name
     * @return a array of <code>byte</code>
     */
    public static byte[] encode(String unicode, String encoding) {
        try {
            return unicode.getBytes(encoding);
        } catch (UnsupportedEncodingException e) {
            return unicode.getBytes();
        }
    }
	
    /**
     * If you miss Python's <code>join</code>, here it is ;-) 
     * 
     * <h4>Synopsis</h4>
     * 
     * <p>...
     * 
     * <blockquote>
     * <pre>Iterator iter = Simple.iterator(new Object[]{"A", "B", "C"});
     *StringBuffer joined = Simple.join(", ", iter, new StringBuffer())</pre>
     * </blockquote>
     * 
     * ...</p>
     * 
     * @param separator
     * @param iter
     * @param sb
     */
    public static StringBuffer join (
        Object separator, Iterator iter, StringBuffer sb
        ) {
        if (iter.hasNext())
            sb.append(iter.next());
            while (iter.hasNext()) {
                sb.append(separator);
                sb.append(iter.next()); 
            }
        return sb;
    }
    
    /**
     * If you miss Python's <code>join</code>, here it is ;-) 
     * 
     * <h4>Synopsis</h4>
     * 
     * <p>...
     * 
     * <blockquote>
     * <pre>Iterator iter = Simple.iterator(new Object[]{"A", "B", "C"});
     *String joined = Simple.join(", ", iter)</pre>
     * </blockquote>
     * 
     * ...</p>
     * 
     * @param separator
     * @param iter
     * @return
     */
    public static String join (Object separator, Iterator iter) {
        return join(separator, iter, new StringBuffer()).toString();
    }
    
}
