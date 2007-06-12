/* Copyright (C) 2007 Laurent A.V. Szyster

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

import java.util.Iterator;
import java.util.ArrayList;
import java.util.NoSuchElementException;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.IOException;

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
 * @version 0.10
 */
public class Netstring {
    
    /**
     * Iterate through <code>String</code>s, queue the encoded 8-bit byte 
     * netstrings in a single <code>ByteBuffer</code>, then dump it in a 
     * single <code>byte</code> array, write it to the <code>conn</code>
     * socket's output stream and finally flush that stream. 
     * 
     * <p>The purpose is to buffer small strings before actually sending
     * data through the socket in order to minimize local overhead and
     * network latency. I found out writing a byte at a time to a TCP
     * socket <code>OutputStream</code> that each byte can waste a few 
     * UDP datagrams and considerably slow down its application.</p>
     * 
     * <p>Again, let's trade space for speed.</code>
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
        OutputStream os = conn.getOutputStream();
        os.write(bb.array());
        os.flush();
    }
    
    /**
     * 
     * @param os
     * @param buffer
     * @param off
     * @param len
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
     * 
     * @param os
     * @param buffer
     * @throws IOException
     */
    static public void send (Socket conn, byte[] buffer) 
    throws IOException {
        send (conn, buffer, 0, buffer.length);
    }
       
    /**
     * 
     * @param os
     * @param string
     * @param encoding
     * @throws IOException
     */
    static public void 
    send (Socket conn, String string, String encoding) 
    throws IOException {
        send(conn, Simple.encode(string, encoding));
    }
        
    static protected class ByteCollector implements Iterator {
        
        private static final String 
        _unexpected_end = "unexpected InputStream's end";
        private static final String 
        _invalid_prologue = "invalid netstring prologue"; 
        private static final String 
        _invalid_epilogue = "invalid netstring epilogue"; 
        private static final String 
        _too_long = "too long netstring";
        
        private InputStream _is = null;
        private int _limit, _length, _off, _pos;
        private byte[] _buffer;
        private boolean _next = false;
        
        public String error = null;
        
        public ByteCollector (InputStream is, int limit)
        throws IOException {
            _is = is;
            _limit = limit;
            _buffer = new byte[Integer.toString(limit).length() + 1];
            _buffer[0] = ',';
            _next = _pull(1);
        }

        private boolean _pull (int off) throws IOException {
            _pos = Simple.recv(_is, _buffer, off);
            if (_pos == off) {
                return false; // nothing to read, stop iteration.
            }
            if (_buffer[0] != ',') {
                error = _invalid_epilogue; return false;
            }
            // read the prologue up to ':', assert numeric only
            byte c;
            for (_off = 1; _off < _pos; _off++) {
                c = _buffer[_off];
                if (c == ':') break;
                else if (!(c >= '0' && c <= '9')) {
                    error = _invalid_prologue; return false;
                }
            }
            if (_off == _buffer.length) {
                error = _too_long; return false;
            }
            _length = Integer.parseInt(new String(_buffer, 1, _off - 1));
            if (_length > _limit) {
                error = _too_long; return false;
            }
            _off += 1;
            return true;
        }
        
        public boolean hasNext () {return _next;}
        
        public Object next () {
            if (!_next)
                throw new NoSuchElementException();
            try {
                byte[] bytes = new byte[_length];
                for (int i=_off; i < _pos; i++) bytes[i] = _buffer[i];
                _pos = Simple.recv(_is, bytes, _pos - _off);
                if (_pos != _length) {
                    error = _unexpected_end; _next = false;
                } else 
                    _next = _pull(0);
                return bytes;
            } catch (IOException e) {
                _next = false;
                error = e.toString();
                throw new NoSuchElementException();
            }
        }
        public void remove () {/* how can an iterator API suck more? */}
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
     * 
     * @param conn
     * @param limit
     * @return
     * @throws IOException
     */
    public static Iterator recv (Socket conn, int limit) 
    throws IOException {
        return new ByteCollector (conn.getInputStream(), limit);
    }

    /**
     * 
     * @param conn
     * @param limit
     * @param encoding
     * @return
     * @throws IOException
     */
    public static Iterator recv (Socket conn, int limit, String encoding) 
    throws IOException {
        return new StringIterator(
            new ByteCollector(conn.getInputStream(), limit), encoding
            );
    }
    
}
