/* Copyright (C) 2007 Laurent A.V. Szyster

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

package org.less4j;

import java.util.Iterator;
import java.util.ArrayList;
import java.util.NoSuchElementException;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.net.Socket;

import java.nio.ByteBuffer;

/**
 * Conveniences to send and receive netstrings efficiently over Java's 
 * synchronous socket API.
 * 
 * <h3>Synosis</h3>
 * 
 * <pre>import org.less4j.Netstring;
 *import java.net.Socket;
 *import java.util.Iterator;
 *
 *Socket conn = new Socket("127.0.0.2", 3999)
 *try {
 *    Netstring.send(
 *        conn, "[\"CREATE TABLE relation (s, p, o, c)\", []]", "UTF-8"
 *        );
 *    Netstring.send(
 *        conn, "[\"SELECT * FROM relation\", []]", "UTF-8"
 *        );
 *    Iterator netstring = new Netstring.recv(conn, 16384, "UTF-8");
 *    while (netstring.hasNext())
 *        System.out.println (netstring.next());
 *} finally {
 *    conn.close ();
 *}</pre>
 * 
 * <p>This class is a lazy implementation of an netstring stream
 * collector, just enough to support netstring protocols.</p>
 * 
 * @copyright 2006-2007 Laurent Szyster
 */
public class Netstring {
    
    /**
     * Iterate through <code>String</code>s, queue the encoded 8-bit byte 
     * netstrings in a single <code>ByteBuffer</code>, then dump it in a 
     * single <code>byte</code> array, write it to the <code>conn</code>
     * socket's output stream and finally flush that stream. 
     * 
     * <h4>Synopsis</h4>
     * 
     * <pre>import org.less4j.Netstring;
     *import org.less4j.Simple;
     *import java.util.Iterator;
     *import java.net.Socket;
     *
     *Iterator netstrings = Simple.iterator(new String[]{
     *    "one", "two", "three", "four"
     *});
     *Socket conn = new Socket("server", 1234);
     *try {
     *    Netstring.send(conn, netstrings, "UTF-8");
     *} finally {
     *    conn.close();
     *}</pre>
     *
     * <p>The code above connects to a host named 'server' on TCP port 1234,
     * sends the netstrings below:</p>
     * 
     * <pre>3:one,3:two,5:three,4:four,</pre>
     * 
     * <p>The purpose is to buffer small strings before actually sending
     * data through the socket in order to minimize local overhead and
     * network latency. I found out writing a byte at a time to a TCP
     * socket <code>OutputStream</code> that each byte can waste a few 
     * UDP datagrams and considerably slow down its application.</p>
     * 
     * @param conn
     * @param strings
     * @param encoding
     * @throws IOException
     */
    static public void 
    send (Socket conn, Iterator strings, String encoding) 
    throws IOException {
        int chunk = 0;
        ArrayList netstrings = new ArrayList();
        byte[] digits, buffer;
        while (strings.hasNext()) {
            buffer = Simple.encode((String) strings.next(), encoding);
            digits = Integer.toString(buffer.length).getBytes();
            chunk += buffer.length + digits.length + 2;
            netstrings.add(new byte[][]{buffer, digits});
        }
        ByteBuffer bb = ByteBuffer.allocate(chunk);
        Iterator bytes = netstrings.iterator();
        byte[][] digits_buffer;
        while (bytes.hasNext()) {
            digits_buffer = (byte[][]) bytes.next(); 
            bb.put(digits_buffer[0]); // len
            bb.put((byte)58); // :
            bb.put(digits_buffer[1]); // encoded 8-bit byte string
            bb.put((byte)44); // ,
        }
        Simple.send(conn.getOutputStream(), bb);
    }
    
    /**
     * A low level interface to send data from a <code>byte</code> array
     * as one netstring.
     * 
     * <h4>Synopsis</h4>
     * 
     * <pre>import org.less4j.Netstring;
     *import java.net.Socket;
     *
     *byte[] data = new byte[]{
     *    'h', 'e', 'l', 'l', 'o', 'w', 'o', 'r', 'l', 'd'
     *};
     *Socket conn = new Socket("server", 1234);
     *try {
     *    Netstring.send(conn, data, 0, 5);
     *    Netstring.send(conn, data, 5, 5);
     *} finally {
     *    conn.close();
     *}</pre>
     * 
     * @param conn the <code>Socket</code> connection to send to
     * @param buffer the <code>byte</code> from which to send data
     * @param off position of the first byte to send in <code>buffer</code>
     * @param len the number of bytes to send
     * @throws IOException
     */
    static public void 
    send (Socket conn, byte[] buffer, int off, int len) 
    throws IOException {
        byte[] digits = Integer.toString(len).getBytes();
        ByteBuffer bb = ByteBuffer.allocate(len + digits.length + 2);
        bb.put(digits); // len
        bb.put((byte)58); // :
        bb.put(buffer, off, len); // encoded 8-bit byte string
        bb.put((byte)44); // ,
        OutputStream os = conn.getOutputStream();
        os.write(bb.array());
    }
    
    /**
     * A low level interface to send a <code>byte</code> array as one 
     * netstring.
     * 
     * <h4>Synopsis</h4>
     * 
     * <pre>import org.less4j.Netstring;
     *import java.net.Socket;
     *
     *byte[] data = new byte[]{'h', 'e', 'l', 'l', 'o'};
     *Socket conn = new Socket("server", 1234);
     *try {
     *    Netstring.send(conn, data);
     *} finally {
     *    conn.close();
     *}</pre>
     * 
     * @param conn the <code>Socket</code> connection to send to
     * @param buffer the <code>byte</code> from which to send data
     * @throws IOException
     */
    static public void send (Socket conn, byte[] buffer) 
    throws IOException {
        send (conn, buffer, 0, buffer.length);
    }
       
    /**
     * A convenience to encode a UNICODE string in 8-bit and send the byte
     * array result as one netstring.
     * 
     * <h4>Synopsis</h4>
     * 
     * <pre>import org.less4j.Netstring;
     *import java.net.Socket;
     *
     *Socket conn = new Socket("server", 1234);
     *try {
     *    Netstring.send(conn, "hello", "UTF-8");
     *} finally {
     *    conn.close();
     *}</pre>
     * 
     * @param conn the <code>Socket</code> connection to send to
     * @param string to encoded as 8-bit bytes and send
     * @param encoding the character set encoding (eg: "UTF-8") 
     * @throws IOException
     */
    static public void 
    send (Socket conn, String string, String encoding) 
    throws IOException {
        send(conn, Simple.encode(string, encoding));
    }
    
    private static final String 
    _unexpected_end = "unexpected InputStream's end";
    private static final String 
    _invalid_prologue = "invalid netstring prologue"; 
    private static final String 
    _invalid_epilogue = "invalid netstring epilogue"; 
    private static final String 
    _too_long = "too long netstring";
    
    static protected class ByteCollector implements Iterator {
        
        private int _limit;
        private byte[] _buffer;
        private InputStream _is = null;
        
        public String error = null;
        
        public ByteCollector (InputStream is, int limit)
        throws IOException {
            _is = is;
            _limit = limit;
            _buffer = new byte[Integer.toString(limit).length() + 1];
        }

        public boolean hasNext () {
            return true; // synchronous API can't stall
        }
        
        public Object next () {
            int read = 0, len, c;
            try {
                // read the prologue up to ':', assert numeric only
                do {
                    c = _is.read(); 
                    if (c == 58) {
                        break; 
                    } else if (c < 48 || c > 57)
                        throw new NoSuchElementException(_invalid_prologue);
                    else if (c == -1)
                        break;
                    else if (read < _buffer.length)
                        _buffer[read] = (byte) c;
                    else
                        throw new NoSuchElementException(_too_long);
                    read ++;
                } while (true);
                if (read == 0) {
                    _is = null;
                    return null; // nothing more to read, stop iteration.
                }
                len = Integer.parseInt(new String(_buffer, 0, read));
                if (len > _limit)
                    throw new NoSuchElementException(_too_long);
                
                byte[] bytes = new byte[len + 1];
                read = Simple.recv(_is, bytes, 0);
                if (read != len + 1)
                    throw new NoSuchElementException(_unexpected_end);
                
                if (bytes[len] != 44)
                    throw new NoSuchElementException(_invalid_epilogue);
                
                return bytes;
                
            } catch (IOException e) {
                throw new NoSuchElementException(e.toString());
            } // checked exception really sucks.
        }
        
        public void remove () {
            /* how can an iterator API suck more than this one? */
        }
    } 
    
    protected static class StringIterator implements Iterator {
        private Iterator _byteIterator;
        private String _encoding;
        public StringIterator (Iterator byteIterator, String encoding) 
        throws IOException, Error {
            _byteIterator = byteIterator; 
            _encoding = encoding;
        }
        public boolean hasNext() {
            return _byteIterator.hasNext();
        }
        public Object next() {
            return Simple.decode((byte[]) _byteIterator.next(), _encoding);
        }
        public void remove () {/* worse than failure, repeated ... */}
    }
    
    /**
     * Instanciate an <code>Iterator</code> of <code>byte[]</code> received.
     * 
     * <h4>Synopsis</h4>
     * 
     * <pre>import org.less4j.Netstring;
     *import java.util.Iterator;
     *import java.net.Socket;
     *
     *Socket conn = new Socket("server", 1234);
     *try {
     *    Iterator recv = Netstring.recv(conn, 16384);
     *    byte[] data = recv.next();
     *    while (data != null)
     *        data = recv.next();
     *} (catch NoSuchElementException e) {
     *    ; // handle error
     *} finally {
     *    conn.close();
     *}</pre>
     *
     * <p>The code above opens a connection to "server" on TCP port 1234
     * and receive all netstrings one by one until the connection is closed
     * by the server.</p>
     * 
     * @param conn the <code>Socket</code> to receive from 
     * @param limit the maximum netstring length
     * @return an <code>Iterator</code> of <code>byte[]</code>
     * @throws IOException
     */
    public static Iterator recv (Socket conn, int limit) 
    throws IOException {
        return new ByteCollector (conn.getInputStream(), limit);
    }

    /**
     * Instanciate an <code>Iterator</code> of <code>String</code> received
     * and decoded from the given 8-bit character set <code>encoding</code>.
     * 
     * <h4>Synopsis</h4>
     * 
     * <pre>import org.less4j.Netstring;
     *import java.util.Iterator;
     *import java.net.Socket;
     *
     *Socket conn = new Socket("server", 1234);
     *try {
     *    Iterator recv = Netstring.recv(conn, 16384, "UTF-8");
     *    String data = recv.next ();
     *    while (data != null)
     *        data = recv.next();
     *} (catch NoSuchElementException e) {
     *    ; // handle error
     *} finally {
     *    conn.close();
     *}</pre>
     *
     * <p>The code above opens a connection to "server" on TCP port 1234,
     * then receive and decode from "UTF-8" all incoming netstrings until the 
     * connection is closed by the server.</p>
     * 
     * @param conn the <code>Socket</code> to receive from 
     * @param limit the maximum netstring length
     * @param encoding the character set to decode <code>byte</code> received
     * @return an <code>Iterator</code> of <code>String</code>
     * @throws IOException
     */
    public static Iterator recv (Socket conn, int limit, String encoding) 
    throws IOException {
        return new StringIterator(
            new ByteCollector(conn.getInputStream(), limit), encoding
            );
    }
    
    static protected class NetstringIterator implements Iterator {
        private Object _next;
        private byte[] _buffer;
        private int _limit, _end;
        private String _encoding, _error;
        public NetstringIterator (byte[] buffer, String encoding) {
            _end = -1;
            _buffer = buffer;
            _limit = Integer.toString(_buffer.length).length() + 1;
            _encoding = encoding;
            _next = pull ();
        }
        protected Object pull () {
            _end++; 
            if (_end >= _buffer.length) {
                _error = "end of netstrings"; return null;
            }
            int pos = _end;
            int limit = pos + _limit; 
            byte c;
            do {
                c = _buffer[pos];
                if (c == 58) {
                    break; 
                } else if (c < 48 || c > 57) {
                    _error = _invalid_prologue; return null;
                }
                pos ++;
                if (pos == limit) {
                    _error = _too_long; return null;
                }
            } while (true);
            int len = Integer.parseInt(
                new String(_buffer, _end, pos - _end)
                );
            pos++;
            _end = pos + len;
            if (_end > _buffer.length){
                len = _buffer.length - pos;
                try {
                    return new String(_buffer, pos, len, _encoding);
                } catch (UnsupportedEncodingException e) {
                    return new String(_buffer, pos, len);
                }
            } else if (_buffer[_end] == 44) {
                try {
                    return new String(_buffer, pos, len, _encoding);
                } catch (UnsupportedEncodingException e) {
                    return new String(_buffer, pos, len);
                }
            } else {
                _error = _invalid_epilogue; return null;
            }
        }
        public boolean hasNext () {
            return (_next != null);
        }
        public Object next () {
            if (_next == null)
                throw new NoSuchElementException(_error);
            Object result = _next;
            _next = pull ();
            return result;
        }
        public void remove () {};
    }
    
    /**
     * Iterate through netstrings found in a byte <code>buffer</code> and
     * decode them from the given character set <code>encoding</cide>.
     * 
     * <h4>Synopsis</h4>
     * 
     * <pre>import org.less4j.Netstring;
     *import java.util.Iterator;
     *0
     *byte[] buffer = new byte[]{
     *    '0', ':', ',', '1', ':', 'A', ',', '2', ':', 'B', 'C', ','
     *}
     *Iterator netstrings = Netstring.decode(buffer, "UTF-8");
     *String data;
     *while (netstrings.hasNext())
     *    data = netstrings.next();
     *</pre>
     *
     * @param buffer a <code>byte</code> array
     * @param encoding
     * @return an <code>Iterator</code> of <code>String</code>
     */
    public static Iterator decode (byte[] buffer, String encoding) {
        return new NetstringIterator(buffer, encoding);
    }
        
}
