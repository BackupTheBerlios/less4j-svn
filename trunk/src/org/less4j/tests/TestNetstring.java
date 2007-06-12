package org.less4j.tests;

import org.less4j.Netstring;

import java.net.Socket;
import java.util.Iterator;


public class TestNetstring {
    
    private static final String _utf8 = "UTF-8";

    /**
     * Try to connect ansql2 server on TCP port 3999 at address 127.0.0.2
     * and pipeline two constant statements to be encoded as UTF-8.
     * 
     * Note that there is no way to pipeline
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        // open a TCP/IP socket to a local's ansql2 server
        Socket conn = new Socket("127.0.0.2", 3999);
        // instanciate an iterator through netstrings received
        Iterator recv = Netstring.recv(conn, 16384, _utf8);
        try {
            // send two netstrings encoded in UTF-8
            Netstring.send(
                conn, "[\"CREATE TABLE relation (s, p, o, c)\", []]", _utf8
                );
            Netstring.send(
                conn, "[\"SELECT * FROM relation\", []]", _utf8
                );
            // receive two netstrings decoded from UTF-8
            System.out.println (recv.next());
            System.out.println (recv.next());
            // interleave requests and responses 
            String request = "[\"SELECT * FROM relation\", []]"; 
            Netstring.send(conn, request, _utf8);
            Netstring.send(conn, request, _utf8);
            System.out.println (recv.next());
            Netstring.send(conn, request, _utf8);
            System.out.println (recv.next());
            System.out.println (recv.next());
        } finally {
            conn.close ();
        }
    }

}
