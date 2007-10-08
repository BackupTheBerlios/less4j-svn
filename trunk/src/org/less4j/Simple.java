/* Copyright (C) 2006-2007 Laurent A.V. Szyster

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

package org.less4j; // less java for more applications

import java.io.OutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.nio.ByteBuffer;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import java.net.URL; 

/**
 * A few utilities too simple for language experts, but damn usefull for 
 * application developers.
 */
public class Simple {
    
    /**
     * The default file I/O buffer size.
     * 
     * @h4 Synopsis
     * 
     * @p 4046 bytes is the maximum block size for many file system, so
     * it makes a rather good default size for file I/O buffers.
     */
	public static int fioBufferSize = 4096;

    /**
     * The default network I/O buffer size.
     * 
     * @h4 Synopsis
     * 
     * @p Sixteen Kilobytes (16384 8-bit bytes) represent 3,4 pages of
     * 66 lines and 72 columns of ASCII characters. Much more information 
     * than what most of us can digest in a few minutes. It's a reasonable
     * maximum to set for an application web interface updated by its
     * user every few seconds.
     * 
     * @p Also, 16KB happens to be the defacto standard buffer size
     * for TCP peers and networks since it's the maximum UDP datagram size
     * in use. Which makes anything smaller than this limit a better candidate 
     * for the lowest possible IP network latency.
     * 
     * @p Finally, sixteen KB buffers can hold more than 65 thousand concurrent
     * responses in one GB of RAM, a figure between one and two orders of 
     * magnitude larger than what you can reasonably expect from a J2EE 
     * container running some commodity hardware. At an average speed of
     * 0.5 millisecond per concurrent request/response 16KB buffers sums
     * up to 36MBps, less than 1Gbps.
     * 
     * @p So, that sweet sixteen spot is also a safe general maximum for a 
     * web 2.0 application controller that needs to keep lean on RAM and
     * wants the fastest possible network and the smallest possible
     * imbalance between input and output.
     */
    public static int netBufferSize = 16384;
    
    /**
     * Read byte arrays from a BufferedReader by chunks of 
     * <code>fioBufferSize</code> until the input stream buffered is
     * exhausted, accumulate those chunks in a <code>StringBuffer</code>,
     * join them and  then returns a UNICODE string (implicitely using the 
     * default character set encoding). 
     * 
     * @h4 Synopsis
     * 
     * @p This is a "low-level" API to support convenience to glob files,
     * URLs resources or any input stream that can be wrapped with a
     * <code>BufferedReader</code>.
     * 
     * @param br a <code>BufferedReader</code> to glob.
     * @return a <code>String</code> with all data read
     * @throws IOException
     */
    public static String read (BufferedReader br) throws IOException {
        StringBuffer sb = new StringBuffer();
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
        return sb.toString();
    }
    
    /**
     * Try to read a complete file into a String.
     * 
     * @h4 Synopsis
     * 
     * @pre String resource = Simple.read("my.xml");
     *     
     * @p Note that since it does not throw exceptions, this method can
     * be used to load static class String members, piggy-backing the 
     * class loader to fetch text resources at runtime. 
     * 
     * @param name the file's name
     * @return a <code>String</code> or <code>null</code> 
     */
    static public String read (String name) {
        try {
            return read(new BufferedReader(new FileReader(name)));
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Try to read a complete InputStream into a String.
     * 
     * @h4 Synopsis
     * 
     * @pre String resource = Simple.read(System.in);
     *     
     * @param is <code>InputStream</code> to read from
     * @return a <code>String</code> or <code>null</code>
     */
    static public String read (InputStream is) throws IOException {
        return read (new BufferedReader(new InputStreamReader(is)));
     }
        
    /**
     * Try to read a complete file into a String.
     * 
     * @h4 Synopsis
     * 
     * @pre String resource = Simple.read("http://w3c.org/");
     *     
     * @p Note that since it does not throw exceptions, this method can
     * be used to load static class String members, piggy-backing the 
     * class loader to fetch text resources at runtime. 
     * 
     * @param url to read from
     * @return a <code>String</code> or <code>null</code>
     */
    static public String read (URL url) {
        try {
            return read (url.openStream());
        } catch (IOException e) {
            return null;
        }
    }
    
    /**
     * Use a <code>ByteBuffer</code> to efficiently merge arrays
     * of byte as one array, sacrife space to gain speed when
     * sending one or more byte strings at once over synchronous
     * TCP streams.
     * 
     * @h4 Synopsis
     * 
     * @param bytes
     * @return
     */
    public static ByteBuffer buffer (byte[][] bytes) {
        int i, ct = 0;
        for (i=0; i<bytes.length; i++) ct += bytes[i].length;
        ByteBuffer bb = ByteBuffer.allocate(ct);
        for (i=0; i<bytes.length; i++) bb.put(bytes[i]);
        return bb;
    }

    /**
     * ...
     * 
     * @h4 Synopsis
     * 
     * @pre ...
     * 
     * @h4 Application
     * 
     * @p TCP/IP is a reliable stream protocol (TCP) implemented on top
     * of an unreliable packet protocol (IP) and this has implications
     * when programming network peers.
     * 
     * @p As soon as latency between peers rise up, buffering data and sending 
     * it in chunks that fit the common TCP windows is the only way to reach
     * decent performances. On the contrary, unbuffered output may translate
     * into colossal waiste of network bandwith, RAM space and CPU power. 
     * 
     * @p So, to avoid the worse case regardless of the runtime environment, 
     * responses should be buffered at once and then chunked out in blocks 
     * that try their best to fit the local operating system buffers and the
     * usual network packets sizes.
     * 
     * @p Note that this is only applicable when the output is expected to
     * allways be below or in the order of magnitude of the common TPC 
     * window size, somewhere between 16KB and 64KB.
     * 
     * @param os
     * @param bb
     * @param off
     * @param len
     */
    static public void send (
        OutputStream os, ByteBuffer bb, int off, int len
        ) throws IOException {
        bb.position(off);
        byte[] bytes = new byte[netBufferSize];
        while (len > netBufferSize) {
            Thread.yield();
            len -= netBufferSize;
            bb.get(bytes);
            os.write(bytes);
            os.flush();
        }
        bb.get(bytes, 0, len);
        os.write(bytes, 0, len);
        os.flush();
    }

    static public void send (OutputStream os, ByteBuffer bb) 
    throws IOException {
        send(os, bb, 0, bb.capacity());
    }

    /**
     * Fill the <code>byte</code> buffer with data read from an 
     * <code>InputStream</code>, starting at position <code>off</code>
     * until the buffer is full or the stream is closed, then return
     * the position in the buffer after the last byte received (ie:
     * the length of the buffer if it was filled).
     * 
     * @h4 Synopsis</code>
     * 
     * @pre import org.less4j.Simple;
     *import java.net.Socket;
     *
     *byte[] buffer = new byte[4096];
     *Socket conn = new Socket("server", 1234);
     *try {
     *    int pos = Simple.recv(conn.getInputStream(), buffer, 0);
     *    if (pos == buffer.length)
     *        System.out.println("buffer filled");
     *} finally {
     *    conn.close();
     *}
     * 
     * 
     * @param is the <code>InputStream</code> to receive from
     * @param buffer a <code>byte</code> array to fill
     * @param off the position to start from
     * @return the position in the buffer after the last byte received
     * @throws IOException
     */
    static public int recv (InputStream is, byte[] buffer, int off) 
    throws IOException {
        int len = 0;
        while (off < buffer.length) {
            len = is.read(buffer, off, buffer.length - off);
            if (len > -1)
                off += len; 
            else if (len == 0)
                Thread.yield(); // ... wait for input, cooperate!
            else 
                break; // ... end of stream.
        }
        return off;
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
     * A convenience to extend a <code>List</code> from an array of 
     * <code>Objects</code>.
     * 
     * @pre List sequence = Simple.list(
     *     new ArrayList(), new Object[]{"a", "b", "c"}
     *     );
     * 
     * @param items to add in the sequence
     * @return the extended <code>List</code>
     */
    public static List list (List sequence, Object[] items) {
        for (int i=0; i<items.length; i++)
            sequence.add(items[i]);
        return sequence;
    }
    
    /**
     * A convenience to build a <code>HashSet</code> from an array of 
     * <code>Objects</code>.
     * 
     * @pre HashSet set = Simple.set({"a", "b", "c"});
     * 
     * @param items to add in the set 
     * @return a <code>HashSet</code>
     */
    public static HashSet set (Object[] items) {
        HashSet result = new HashSet();
        for (int i=0; i<items.length; i++)
            result.add(items[i]);
        return result;
    }
    
	/**
	 * A convenience to iterate around a "primitive" array. 
	 * 
	 * @pre Iterator iter = Simple.iter(new Object[]{x, y, z});
	 *     
	 * @p Usefull to iterate through final arrays, a prime construct in web
	 * application controllers where check lists and filter sets made of
	 * primitive array abound (usually to enforce business rules).
	 * 
	 * @param objects the array to iterate through
	 * @return iterator yields all objects in the array
	 */
	public static Iterator iter (Object[] objects) {
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

    /**
     * Update a <code>Map</code> with the keys and values sequence found in
     * an even <code>Object</code> array.
     * 
     * @pre HashMap map = Simple.dict(new HashMap(), new Object[]{
     *     "A", "test", 
     *     "B", true, 
     *     "C": 1.0, 
     *     "D", null
     *     });
     * 
     * @p This method is convenient to instanciate or update dictionaries 
     * with a clearer syntax than using a sequence of <code>Map.put</code>.
     * 
     * @param map to update
     * @param pairs of key and value to add
     * @return the updated <code>Map</code>
     */
    public static Map dict (Map map, Object[] pairs) {
        for (int i=0; i<pairs.length; i=i+2)
            map.put(pairs[i], pairs[i+1]);
        return map;
    }
    
    /**
     * ...
     * 
     * @param map to update
     * @param pairs of key and value to add
     * @return the updated <code>Map</code>
     */
    public static Map dict (Map map, String[][] pairs) {
        for (int i=0; i<pairs.length; i++) if (pairs[i].length > 1)
            map.put(pairs[i][0], pairs[i][1]);
        return map;
    }
    
    /**
     * Iterate through arbitrary values in a <code>Map</code>.
     * 
     * @pre HashMap map = Simple.dict(new HashMap(), new Object[]{
     *     "A", "test", 
     *     "B", true, 
     *     "C": 1.0, 
     *     "D", null
     *     });;
     * Iterator values = Simple.iter(map, new Object[]{
     *     "A", "C", "E"
     *     };);
     * 
     * @p This method is convenient to extract an ordered set of named
     * values from a dictionary using a <code>Object</code> array.
     * 
     * @param map the <code>Map</code> to iterate through
     * @param keys and array of keys to iterate through 
     * @return an <code>Iterator</code>
     */
    public static Iterator iter (Map map, Object[] keys) {
        return new MapIterator(map, iter(keys));
    }
    
    /**
     * Encode a <code>unicode</code> string in the given character set 
     * <code>encoding</code> or use the default if a 
     * <code>UnsupportedEncodingException</code> was throwed.
     * 
     * @h4 Synopsis
     * 
     * @pre byte[] encoded = Simple.encode("test", "UTF-8");
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
     * Decode a <code>byte</code> array to UNICODE from the given character 
     * set <code>encoding</code> or use the default if a 
     * <code>UnsupportedEncodingException</code> was throwed.
     * 
     * @pre String decoded = Simple.decode(
     *    new byte[]{'t', 'e', 's', 't'}, "UTF-8"
     *    );
     * 
     * @param bytes the <code>byte</code> array to decode 
     * @param encoding the character set name
     * @return a UNICODE <code>String</code>
     */
    public static String decode(byte[] bytes, String encoding) {
        try {
            return new String (bytes, encoding);
        } catch (UnsupportedEncodingException e) {
            return new String (bytes);
        }
    }
    
    protected static class SplitIterator implements Iterator {
        private String _splitted;
        private char _splitter;
        private int _current = 0;
        private int _next;
        public SplitIterator (String splitted, char splitter) {
            _splitted = splitted;
            _splitter = splitter;
            _next = splitted.indexOf(splitter);
            }
        public boolean hasNext () {
            return !(_next == -1 && _current == -1);
        }
        public Object next () {
            String token;
            if (_current == -1)
                return null;
            else if (_next == -1) {
                token = _splitted.substring(_current);
                _splitted = null; // free willy-memory!
                _current = -1;
            } else {
                if (_next > _current)
                    token = _splitted.substring(_current, _next);
                else
                    token = "";
                _current = _next + 1;
                _next = _splitted.indexOf(_splitter, _current);
            }
            return token;
        }
        public void remove () {/* optional interface? what else now ...*/}
    }

    /**
     * Returns an <code>Iterator</code> that splits a string with a single
     * character as fast an lean as possible in Java (without a PCRE and
     * for a maybe too simple use case).
     * 
     * @pre Iterator strings = Simple.split("one two three", ' ');
     * 
     * @param splitted the <code>String</code> to split
     * @param splitter the <code>char</char> used to split input
     * @return an <code>Iterator</code> of <code>String</code>
     */
    public static Iterator split (String splitted, char splitter) {
        return new SplitIterator (splitted, splitter);
    }
	
    /**
     * Join the items produced by an <code>Iterator</code> in a 
     * <code>StringBuffer</code>, using an <code>Object</code> as
     * separator between items.
     * 
     * @pre Iterator iter = Simple.iter(new Object[]{"A", "B", "C"});
     *StringBuffer joined = Simple.join(", ", iter, new StringBuffer())
     * 
     * @param separator
     * @param iter
     * @param sb
     */
    public static StringBuffer join (
        Object separator, Iterator iter, StringBuffer sb
        ) {
        if (iter.hasNext()) {
            sb.append(iter.next());
            while (iter.hasNext()) {
                sb.append(separator);
                sb.append(iter.next()); 
            }
        }
        return sb;
    }
    
    /**
     * If you miss Python's <code>join</code>, here it is ;-) 
     * 
     * @pre Iterator strings = Simple.iter(new Object[]{"A", "B", "C"});
     *String joined = Simple.join(", ", strings)
     * 
     * @param separator 
     * @param iter 
     * @return the joined string
     */
    public static String join (Object separator, Iterator iter) {
        return join(separator, iter, new StringBuffer()).toString();
    }
    
    /**
     * The strictly alphanumeric set of ASCII characters, usefull for
     * ubiquitous identifiers on any devices and in any languages, including
     * American English and all phone slangs.
     */
    public static char[] ALPHANUMERIC = new char[]{
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
        'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
        'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'        
    };
    
    /**
     * @p Generate a random password of a given <code>length</code>, using
     * only US ASCII characters.
     * 
     * @param length of the password to generate
     * @return a string of <code>length</code> characters
     */
    public static String password (int length) {
        Random random = new Random();
        char[] characters = new char[length];
        for (int i=0; i<length; i++) 
            characters[i] = ALPHANUMERIC[random.nextInt(62)];
        return String.copyValueOf(characters);
    }
    
}
