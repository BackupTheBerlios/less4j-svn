package org.less4j.tests;

import org.less4j.protocols.Netstring;

import java.net.Socket;
import java.net.InetAddress;

import java.util.Iterator;


public class TestNetstring {
    
    private static final String _utf8 = "UTF-8";
    
    public static Object testAnsql2 (
        InetAddress server, String request
        ) 
    throws Exception {
        Object result;
        // open a TCP/IP socket to a local's ansql2 server
        Socket conn = new Socket(server, 3999);
        // instanciate an iterator through netstrings received
        Iterator recv = Netstring.recv(conn, 16384, _utf8);
        try {
            // send one netstrings encoded in UTF-8
            Netstring.send(conn, request, _utf8);
            // receive one netstrings decoded from UTF-8
            result = recv.next();
        } finally {
            conn.close ();
            conn = null;
        }
        return result;
    }
    
    public class TestAnsql2 extends Thread {
        public void run () {
            
        }
    }

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
        InetAddress server = InetAddress.getByName("127.0.0.2");
        System.out.println(testAnsql2(
             server, "[\"CREATE TABLE relation (s, p, o, c)\", []]"
             ));
        System.out.println(testAnsql2(server, "[" + 
            "\"INSERT INTO relation (s, p, o, c) VALUES (?, ?, ?, ?)\"," +
            "[[\"S\", \"P\", \"O\", \"C\"]," +
            " [\"Subject\", true, 1.0, 1.93839382e-6]," +
            " [1,2,3,4]]" + 
            "]"));
        System.out.println(testAnsql2(
                server, "[\"SELECT * FROM relation\", []]"
            ));
        int i, errors = 0;
        int N = Integer.parseInt(args[0]);
        long t = System.currentTimeMillis();
        //
        for (i=0; i < N; i++)
            try {
                testAnsql2(server, "[\"SELECT * FROM relation\", []]");
            } catch (Exception e) {
                errors += 1;
            }
        //
        t = System.currentTimeMillis() - t;
        System.out.print((new Long(t)).doubleValue()/N);
        System.out.println(" ms per connection.");
        System.out.print(N);
        System.out.println(" requests and connections.");
        System.out.print(errors);
        System.out.println(" errors.");
        //
        Socket conn = new Socket("127.0.0.2", 3999); 
        // instanciate an iterator through netstrings received
        Iterator recv = Netstring.recv(conn, 16384, _utf8);
        N = Integer.parseInt(args[1]);
        t = System.currentTimeMillis();
        //
        for (i=0; i < N; i++)
            Netstring.send(conn, "[\"SELECT * FROM relation\", []]", _utf8);
        for (i=0; i < N; i++) recv.next();
        //
        t = System.currentTimeMillis() - t;
        System.out.print((new Long(t)).doubleValue()/N);
        System.out.println(" ms per pipelined request/response.");
        //
        conn = new Socket("127.0.0.2", 3999); 
        // instanciate an iterator through netstrings received
        recv = Netstring.recv(conn, 16384, _utf8);
        N = Integer.parseInt(args[1]);
        t = System.currentTimeMillis();
        //
        for (i=0; i < N; i++) {
            Netstring.send(conn, "[\"SELECT * FROM relation\", []]", _utf8);
            recv.next();
        }
        //
        t = System.currentTimeMillis() - t;
        System.out.print((new Long(t)).doubleValue()/N);
        System.out.println(" ms per sequential request/response.");
    }
}
