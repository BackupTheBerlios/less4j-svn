package org.less4j.simple;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

/**
 * Functional conveniences to process <code>byte[]</code>.
 */
public class Bytes {

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

    /**
     * Find the starting position of a bytes string in another one.
     * 
     * @param what to search
     * @param in a bytes string
     * @param from the given position
     * @return the starting position of a match or -1 if not found
     * 
     * @test return Bytes.find(
     *    Bytes.encode("World", "UTF-8"), 
     *    Bytes.encode("Hello World!", "UTF-8"), 
     *    0
     *    ) == 6;
     *
     * @test return Bytes.find(
     *    Bytes.encode("World", "UTF-8"), 
     *    Bytes.encode("Hello World!", "UTF-8"), 
     *    5
     *    ) == 6;
     *    
     * @test return Bytes.find(
     *    Bytes.encode("World", "UTF-8"), 
     *    Bytes.encode("Hello World!", "UTF-8"), 
     *    7
     *    ) == -1;
     *    
     * @test return Bytes.find(
     *    Bytes.encode("world", "UTF-8"), 
     *    Bytes.encode("Hello World!", "UTF-8"), 
     *    0
     *    ) == -1;
     */
    public static final int find (byte[] what, byte[] in, int from) {
        int i;
        int limit = in.length - what.length;
        for (; from < limit; from++) {
            if (in[from]==what[0]) {
                for (i=1; i<what.length; i++) {
                    if (in[from+i]!=what[i]) {
                        break;
                    }
                }
                if (i==what.length) {
                    return from;
                }
            }
        }
        return -1;
    }

    public static final String UTF8 = "UTF-8";

}
