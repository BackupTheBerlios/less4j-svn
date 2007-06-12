package org.less4j.tests;

import org.less4j.Netstring;

import java.net.Socket;
import java.util.Iterator;


public class TestNetstring {

    /**
     * Try to connect ansql2 server on TCP port 3999 at address 127.0.0.2
     * and pipeline two constant statements to be encoded as UTF-8.
     * 
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        Socket conn = new Socket("127.0.0.2", 3999);
        try {
            Netstring.send(
                conn, "[\"CREATE TABLE relation (s, p, o, c)\", []]", "UTF-8"
                );
            Netstring.send(
                conn, "[\"SELECT * FROM relation\", []]", "UTF-8"
                );
            Iterator netstring = Netstring.recv(conn, 16384, "UTF-8");
            while (netstring.hasNext())
                System.out.println (netstring.next());
        } finally {
            conn.close ();
        }
    }

}
