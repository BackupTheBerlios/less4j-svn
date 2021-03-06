package org.less4j.simple;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.nio.ByteBuffer;

/**
 * Functional conveniences to handle simple I/O.
 */
public class IO {

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

}
