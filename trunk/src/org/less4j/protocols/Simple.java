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

package org.less4j.protocols; // less java for more applications

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
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import java.net.URL; 

/**
 * A few general-purpose conveniences maybe too simple for language experts, 
 * but damn usefull for application developers.
 */
public class Simple {
    
    /**
     * A simple function interface definition, usefull whenever your
     * application requires first-class functions that take a single
     * <code>Object</code> as input state and return <code>null</code> or
     * a single <code>Object</code> result.
     */
    public static interface Function {
        /**
         * A method that implements the function's call, taking any 
         * <code>Object</code> type as input and returning as result
         * an instance of any type or <code>null</code>.
         * 
         * @param input of any <code>Object</code> type
         * @return an instance of any type or <code>null</code>.
         * 
         * @pre class Print implements Simple.Function {
         *    public Object apply (Object input) {
         *        try {
         *            System.out.println(input);
         *        } catch (Throwable e) {
         *            return e;
         *        }
         *        return null;
         *    }
         *}
         */
        public Object apply (Object input);
    }
    
    protected static final String _utf_8 = "UTF-8";
    
    /**
     * The default file I/O buffer size, 4096 bytes.
     * 
     * @p 4046 bytes is the maximum block size for many file system, so
     * it makes a rather good default size for file I/O buffers.
     */
	public static final int fioBufferSize = 4096;

    /**
     * The default network I/O buffer size, 16384 bytes.
     * 
     * @p A string of 16KB can represent 3,4 pages of 66 lines and 72 columns 
     * of ASCII characters. Or more information than what most people can 
     * read in a few minutes. It is therefore a reasonable maximum to set for 
     * chunks of I/O in an application web interface updated by its user every 
     * few seconds.
     * 
     * @p Sixteen Kilobytes (16384 8-bit bytes) happens to be the defacto 
     * standard buffer size for TCP peers and networks since it's the maximum 
     * UDP datagram size in use. Which makes anything smaller than this limit 
     * a better candidate for the lowest possible IP network latency.
     * 
     * @p Finally, 16KB buffers can hold more than 65 thousand concurrent
     * responses in one GB of RAM, a figure between at least one order of 
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
    public static final int netBufferSize = 16384;
    
    /**
     * Read byte arrays from a <code>BufferedReader</code> by chunks of 
     * <code>fioBufferSize</code> until the input stream buffered is
     * closed, accumulate those chunks in a <code>StringBuffer</code>,
     * join them and  then returns a UNICODE string (implicitely using the 
     * default character set encoding). 
     * 
     * @p This is a "low-level" API to support convenience to glob files,
     * URLs resources or any input stream that can be wrapped with a
     * <code>BufferedReader</code>.
     * 
     * @param reader to to glob.
     * @return the string read 
     * @throws IOException
     */
    protected static String read (BufferedReader reader) throws IOException {
        StringBuffer sb = new StringBuffer();
        try {
            char[] buffer = new char[fioBufferSize];
            int readLength = reader.read(buffer);
            while (readLength > 0) {
                sb.append(buffer, 0, readLength);
                readLength = reader.read(buffer);
            }
            if (readLength > 0){
                sb.append(buffer, 0, readLength);
            }
        } finally {
            reader.close();
        }
        return sb.toString();
    }
    
    /**
     * Try to read a complete <code>InputStream</code> into a 
     * <code>String</code> using a buffer of <code>fioBufferSize</code>.
     * 
     * @pre String resource = Simple.read(System.in);
     *     
     * @param stream to read from
     * @return the string read or <code>null</code>
     */
    public static final String read (InputStream stream) throws IOException {
        return read (new BufferedReader(
            new InputStreamReader(stream), fioBufferSize
            ));
     }
        
    /**
     * Try to open and read a complete <code>URL</code> into a 
     * <code>String</code> using a buffer of <code>fioBufferSize</code>.
     * 
     * @pre String resource = Simple.read(new URL("http://w3c.org/"));
     *     
     * @p Note that since it does not throw exceptions, this method can
     * be used to load static class String members, piggy-backing the 
     * class loader to fetch text resources at runtime. 
     * 
     * @param url to read from
     * @return the string read or <code>null</code>
     */
    public static final String read (URL url) throws IOException {
        return read (url.openStream());
    }
    
    /**
     * Try to open and read a named file into a <code>String</code>
     * using a buffer of <code>fioBufferSize</code>.
     * 
     * @pre String resource = Simple.read("my.xml");
     *     
     * @param name the file's name
     * @return the string read or <code>null</code>
     * @throws IOException 
     */
    public static final String read (String name) 
    throws IOException {
        return read(new BufferedReader(
            new FileReader(name), fioBufferSize
            ));
    }

    /**
     * Try to open and read a named file or URL into a <code>String</code>
     * using a buffer of <code>fioBufferSize</code>, return a default in
     * case of failure.
     * 
     * @pre String resource = Simple.read("my.xml");
     *     
     * @p Note that since it does not throw exceptions, this method can
     * be used to load static class String members, piggy-backing the 
     * class loader to fetch text resources at runtime. 
     * 
     * @param name the file's name
     * @param def the default <code>String</code> to return, may be null
     * @return the string read or <code>null</code>
     */
    public static final String read (String name, String def) {
        try {
            return read(new BufferedReader(
                new FileReader(name), fioBufferSize
                ));
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
     * @param bytes to merge
     * @return an NIO byte buffer
     */
    public static final ByteBuffer buffer (byte[][] bytes) {
        int i, ct = 0;
        for (i=0; i<bytes.length; i++) ct += bytes[i].length;
        ByteBuffer bb = ByteBuffer.allocate(ct);
        for (i=0; i<bytes.length; i++) bb.put(bytes[i]);
        return bb;
    }

    /**
     * Send to an output stream the content of a byte buffer by chunks of
     * <code>netBufferSize</code>, starting at a given offset and stopping 
     * after a given length, yielding to other threads after each chunk.
     * 
     * @param stream to send output to
     * @param buffer of bytes to read from
     * @param offset to start from
     * @param length of output to send
     * @throws IOException
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
     * @p Note also the importance of yielding execution to other threads
     * after each chunk is sent in order <em>not</em> to overflow the OS
     * network buffers and avoid as much wait state as possible.
     * 
     */
    public static final void send (
        OutputStream stream, ByteBuffer buffer, int offset, int length
        ) throws IOException {
        buffer.position(offset);
        byte[] bytes = new byte[netBufferSize];
        while (length > netBufferSize) {
            length -= netBufferSize;
            buffer.get(bytes);
            stream.write(bytes);
            stream.flush();
            Thread.yield();
        }
        buffer.get(bytes, 0, length);
        stream.write(bytes, 0, length);
        stream.flush();
    }

    /**
     * Send to an output stream the content of a byte buffer by chunks of
     * <code>netBufferSize</code>, yielding to other threads after each chunk.
     * 
     * @param stream to send output to
     * @param buffer of bytes to read from
     * @throws IOException
     */
    public static final void send (OutputStream stream, ByteBuffer buffer) 
    throws IOException {
        send(stream, buffer, 0, buffer.capacity());
    }

    /**
     * Fill a <code>byte</code> buffer with data read from an 
     * <code>InputStream</code>, starting at a given <code>offset</code>
     * until the buffer is full or the stream is closed, then return
     * the position in the buffer after the last byte received (ie:
     * the length of the buffer if it was filled).
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
     * @param stream to receive from
     * @param buffer of bytes to fill
     * @param offset to start from
     * @return the position in the buffer after the last byte received
     * @throws IOException
     */
    public static final int recv (InputStream stream, byte[] buffer, int offset) 
    throws IOException {
        int len = 0;
        while (offset < buffer.length) {
            len = stream.read(buffer, offset, buffer.length - offset);
            if (len > -1)
                offset += len; 
            else if (len == 0)
                Thread.yield(); // ... wait for input, cooperate!
            else 
                break; // ... end of stream.
        }
        return offset;
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
     * @pre List sequence = Simple.extend(
     *     new ArrayList(), new Object[]{"a", "b", "c"}
     *     );
     * 
     * @param items to add in the sequence
     * @return the extended <code>List</code>
     */
    public static final List extend (List sequence, Object[] items) {
        for (int i=0; i<items.length; i++)
            sequence.add(items[i]);
        return sequence;
    }
    
    /**
     * A convenience to create an <code>ArrayList</code> from an array of 
     * <code>Objects</code>.
     * 
     * @pre ArrayList sequence = Simple.list(
     *     new Object[]{"a", "b", "c"}
     *     );
     * 
     * @param items to add in the sequence
     * @return a new <code>ArrayList</code>
     */
    public static final ArrayList list (Object[] items) {
        ArrayList sequence = new ArrayList();
        for (int i=0; i<items.length; i++)
            sequence.add(items[i]);
        return sequence;
    }
    
    /**
     * A convenience to build a <code>HashSet</code> from an array of 
     * <code>Objects</code>.
     * 
     * @pre HashSet set = Simple.set(new String[]{"a", "b", "c"});
     * 
     * @param items to add in the set 
     * @return a <code>HashSet</code>
     */
    public static final HashSet set (Object[] items) {
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
	public static final Iterator iter (Object[] objects) {
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
     * @pre HashMap map = Simple.update(new HashMap(), new Object[]{
     *     "A", "test", 
     *     "B", true, 
     *     "C": new Integer(1), 
     *     "D", false
     *     });
     * 
     * @p This method is convenient to instanciate or update dictionaries 
     * with a clearer syntax than using a sequence of <code>Map.put</code>.
     * 
     * @param map to update
     * @param pairs of key and value to add
     * @return the updated <code>Map</code>
     */
    public static final Map update (Map map, Object[] pairs) {
        for (int i=0; i<pairs.length; i=i+2)
            map.put(pairs[i], pairs[i+1]);
        return map;
    }
    
    /**
     * Update a new <code>HashMap</code> with the keys and values sequence 
     * found in an even <code>Object</code> array.
     * 
     * @pre HashMap map = Simple.dict(new Object[]{
     *     "A", "test", 
     *     "B", true, 
     *     "C": new Integer(1), 
     *     "D", false
     *     });
     * 
     * @p This method is convenient to instanciate unsynchronized dictionaries 
     * with a clearer syntax than using a sequence of <code>HashMap.put</code>.
     * 
     * @param map to update
     * @param pairs of key and value to add
     * @return the updated <code>HashMap</code>
     */
    public static final HashMap dict (Object[] pairs) {
        HashMap map = new HashMap();
        for (int i=0; i<pairs.length; i=i+2)
            map.put(pairs[i], pairs[i+1]);
        return map;
    }
    
    /**
     * Iterate through arbitrary values in a <code>Map</code>.
     * 
     * @pre HashMap map = Simple.dict(new Object[]{
     *    "A", "test", 
     *    "B", true, 
     *    "C": new Integer(1), 
     *    "D", false
     *    });
     * Iterator values = Simple.iter(map, new Object[]{
     *    "A", "C", "E"
     *    });
     * 
     * @p This method is convenient to extract an ordered set of named
     * values from a dictionary using a <code>Object</code> array.
     * 
     * @test var map = Simple.dict([
     *    "A", "test", 
     *    "B", true, 
     *    "C", 1, 
     *    "D", false
     *    ]);
     *var values = Simple.iter(map, ["A", "C", "E"]);
     *return (
     *    values.next() == "test" &&
     *    values.next() == 1 &&
     *    values.next() == null &&
     *    values.hasNext() == false
     *    );
     * 
     * @param map the <code>Map</code> to iterate through
     * @param keys and array of keys to iterate through 
     * @return an <code>Iterator</code>
     */
    public static final Iterator iter (Map map, Object[] keys) {
        return new MapIterator(map, iter(keys));
    }
    
    /**
     * Decode a <code>byte</code> array to UNICODE from the given character 
     * set <code>encoding</code> or use the default if a 
     * <code>UnsupportedEncodingException</code> was throwed.
     * 
     * @test return Simple.decode(
     *    [116, 101, 115, 116], "UTF-8"
     *    ).equals("test"); 
     * 
     * // TODO: actually test UTF-8 encoding, not just ASCII-US
     * 
     * @param bytes the <code>byte</code> array to decode 
     * @param encoding the character set name
     * @return a UNICODE <code>String</code>
     */
    public static final String decode(byte[] bytes, String encoding) {
        try {
            return new String (bytes, encoding);
        } catch (UnsupportedEncodingException e) {
            return new String (bytes);
        }
    }
    
    /**
     * Encode a <code>unicode</code> string in the given character set 
     * <code>encoding</code> or use the default if a 
     * <code>UnsupportedEncodingException</code> was throwed.
     * 
     * @test return Simple.decode(
     *    Simple.encode("test", "UTF-8"), "UTF-8"
     *    ).equals("test");
     * 
     * @param unicode the <code>String</code> to encode
     * @param encoding the character set name
     * @return a array of <code>byte</code>
     */
    public static final byte[] encode(String unicode, String encoding) {
        try {
            return unicode.getBytes(encoding);
        } catch (UnsupportedEncodingException e) {
            return unicode.getBytes();
        }
    }
    
    protected static class CharSplitIterator implements Iterator {
        private String _splitted;
        private char _splitter;
        private int _current = 0;
        private int _next;
        public CharSplitIterator (String splitted, char splitter) {
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
     * @test strings = Simple.split("one two three", ' ');
     *return (
     *    strings.next() == "one" &&
     *    strings.next() == "two" &&
     *    strings.next() == "three"
     *    );
     * 
     * @param text to split
     * @param pattern used to split input
     * @return an <code>Iterator</code> of <code>String</code>
     */
    public static final Iterator split (String text, char splitter) {
        return new CharSplitIterator (text, splitter);
    }
	
    protected static class ReSplitIterator implements Iterator {
        private String _splitted;
        private Matcher _matcher;
        private int _current = 0;
        private int _group = 0;
        private int _groups = 0;
        public ReSplitIterator (String splitted, Pattern splitter) {
            _splitted = splitted;
            _matcher = splitter.matcher(splitted);
        }
        public boolean hasNext () {
            return _matcher != null;
        }
        public Object next () {
            String token;
            if (_matcher == null) {
                return null;
            } else if (_group < _groups) { // groups
                _group++;
                token = _matcher.group(_group);
            } else {
                _group = 0;
                if (_matcher.find(_current)) {
                    token = _splitted.substring(_current, _matcher.start());
                    _current = _matcher.end();
                    _groups = _matcher.groupCount();
                } else {
                    token = _splitted.substring(_current);
                    _matcher = null;
                    _splitted = null;
                    _groups = 0;
                }
            }
            return token;
        }
        public void remove () {/* optional interface? what else now ...*/}
    }

    /**
     * Returns an <code>Iterator</code> that splits a string with a regular
     * expression but - unlike the standard Java API and like Python's re - 
     * does the right thing and also iterates through the expression groups.
     * 
     * @pre Iterator strings = Simple.split(
     *    "one\t  and  \r\n three", Pattern.compile("\\s+(and|or)\\s+")
     *    );
     * 
     * @test importClass(Packages.java.util.regex.Pattern)
     *strings = Simple.split(
     *    "one\t  and  \r\n three", Pattern.compile("\\s+(and|or)\\s+")
     *    );
     *return (
     *    strings.next() == "one" &&
     *    strings.next() == "and" &&
     *    strings.next() == "three"
     *    );
     * 
     * @param text to split
     * @param pattern used to split input
     * @return an <code>Iterator</code> of <code>String</code>
     */
    public static final Iterator split (String text, Pattern pattern) {
        return new ReSplitIterator (text, pattern);
    }
    
    /**
     * Join the serialized objects produced by an <code>Iterator</code> in a 
     * <code>StringBuffer</code>, using another serializable 
     * <code>Object</code> as separator between items.
     * 
     * @pre StringBuffer buffer = new StringBuffer(); 
     *Iterator objects = Simple.iter(new Object[]{"A", "B", "C"});
     *Simple.join(", ", objects, buffer);
     * 
     * @param separator between joined strings
     * @param objects to join as strings
     * @param buffer to append strings and separators to
     * @return the appended buffer
     */
    public static final StringBuffer join (
        Object separator, Iterator objects, StringBuffer buffer
        ) {
        if (objects.hasNext()) {
            buffer.append(objects.next());
            while (objects.hasNext()) {
                buffer.append(separator);
                buffer.append(objects.next()); 
            }
        }
        return buffer;
    }
    
    /**
     * Join object's strings with any other type joinable in a 
     * <code>StringBuffer</code>. 
     * 
     * @test return Simple.join(
     *    ", ", Simple.iter(["A", "B", "C"])
     *    ).equals("A, B, C");
     * 
     * @param separator between joined objects
     * @param objects to join as strings
     * @return the joined string
     */
    public static final String join (Object separator, Iterator objects) {
        return join(separator, objects, new StringBuffer()).toString();
    }
    
    /**
     * The strictly alphanumeric set of ASCII characters, usefull for
     * ubiquitous identifiers on any devices and in any languages, including
     * American English and all phone slangs.
     */
    public static final char[] ALPHANUMERIC = new char[]{
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N',
        'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
        'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'        
    };
    
    /**
     * @p Generate a random string of a given <code>length</code> composed
     * from the given character set.
     * 
     * @param length of the string to generate
     * @param set of character to compose from
     * @return a string of <code>length</code> characters
     */
    public static final String random (int length, char[] set) {
        Random random = new Random();
        char[] characters = new char[length];
        for (int i=0; i<length; i++) 
            characters[i] = set[random.nextInt(set.length)];
        return String.copyValueOf(characters);
    }
    
    /**
     * @p Generate a random password of a given <code>length</code>, using
     * only US ASCII characters.
     * 
     * @param length of the password to generate
     * @return a string of <code>length</code> characters
     */
    public static final String password (int length) {
        return random(length, ALPHANUMERIC);
    }
    
}
