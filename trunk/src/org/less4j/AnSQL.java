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

package org.less4j;

import java.util.Iterator;
import java.io.IOException;
import java.net.Socket;
// import java.net.InetAddress;

/**
 * AnSQL implementation to replace JDBC with an protocol that supports the 
 * development of high-performance network database transaction controllers.
 * 
 * @h3 Doctest
 * 
 * @p Building or running the doc tests from sources requires an instance
 * of AnSQL server listening on IP address 127.0.0.2 at TCP port 3999.
 * 
 */
public class AnSQL {
    private Socket socket = null;
    /**
     * The IP address of the server, by default "127.0.0.2".
     */
    public String ip = "127.0.0.2";
    /**
     * The TCP port number to connect.
     */
    public int port = 3999;
    /**
     * A list of requests to submit to the server, <code>null</code> once
     * they have been sent.
     * 
     * @pre AnSQL ansql = new AnSQL("127.0.0.2", 3999);
     *ansql.requests.add(JSON.encode(new Object[]{
     *    "SELECT * FROM contest WHERE prize > ?",
     *    new Object[]{new Integer(100)}
     *    }));
     *
     * @p Directly encoding <code>Object[]</code> literals can save some
     * CPU and RAM, but there are conveniences to queue SQL statements
     * and transactions. 
     *    
     */
    public JSON.Array requests = new JSON.Array();
    /**
     * The list of responses fetched or <code>null</code> if no response
     * was expected (having an empty set of requests may be a no operation
     * but expecting some responses and getting none is an error!).
     * 
     * @h4 Synopsis
     * 
     * @pre AnSQL ansql = new AnSQL("127.0.0.2", 3999);
     *ansql.requests.add(JSON.encode(new Object[]{
     *    "SELECT * FROM contest WHERE prize > ?",
     *    new Object[]{new Integer(100)}
     *    }));
     *ansql.fetch();
     *JSON.pprint(ansql.responses, System.out); 
     *
     * @h4 Test
     * 
     * @test importClass(java.lang.System); 
     *var ansql = new AnSQL("127.0.0.2", 3999);
     *ansql.requests.add(JSON.encode(JSON.list([
     *    "SELECT * FROM contest WHERE prize > ?", [100]
     *    ])));
     *ansql.fetch();
     *return ansql.responses.S(0) == '"no such table: contest"';
     * 
     */
    public JSON.Array responses = null;
    /**
     * ...
     * 
     * @pre AnSQL ansql = new AnSQL("127.0.0.2", 3999); 
     *ansql.prepare("...");
     *JSON.Array transaction = ansql.transaction(new String[]{
     *    "..."
     *    });
     *transaction.add();
     *transaction.add();
     *transaction.add();
     *ansql.execute();
     *ansql.fetch();
     *JSON.pprint(ansql.decode(), System.out);
     *    
     * @p This new protcol implementation enables J2EE applications to: 
     * eliminate as much network latency and contention as possible; 
     * manage persistent data safely out of the J2EE container's memory; 
     * distribute database management workload to a distinct processor; 
     * eventually avoid to deserialize and reserialize JSON responses.  
     * 
     * @param ip address of the server
     * @param port number to connect
     */
    public AnSQL (String ip, int port) {
        this.ip = ip;
        this.port = port;
    }
    private static final String _prepare_epilogue = ",null]";
    /**
     * ...
     * 
     * @param statement to prepare
     */
    public void prepare (String statement) {
        StringBuffer sb = new StringBuffer();
        sb.append('[');
        JSON.strb(sb, statement);
        sb.append(_prepare_epilogue);
        requests.add(sb.toString());
    }
    private static final String _execute_epilogue = ",[]]";
    /**
     * ...
     * 
     * @param statement to execute
     */
    public void execute (String statement) {
        StringBuffer sb = new StringBuffer();
        sb.append('[');
        JSON.strb(sb, statement);
        sb.append(_execute_epilogue);
        requests.add(sb.toString());
    }
    /**
     * ...
     * 
     * @param statement to execute
     * @param arguments of the statement
     */
    public void execute (String statement, JSON.Array arguments) {
        requests.add(JSON.encode(new Object[]{statement, arguments}));
    }
    private static final String _saalmboel = 
        "statements and arguments lists must be of equal lengths";
    /**
     * ...
     * 
     * @pre ...
     * 
     * @param statements to commit or rollback
     * @param arguments of the statements
     * @throws a RuntimeException if the statements and arguments lists are 
     *         not of equal lengths
     */
    public void transaction (String[] statements, JSON.Array arguments) {
        if (statements.length == arguments.size())
            requests.add(JSON.encode(new Object[]{statements, arguments}));
        else
            throw new RuntimeException (_saalmboel);
    }
    /**
     * ...
     * 
     * @return
     */
    public boolean fetch () {
        if (requests == null || requests.size() == 0) {
            requests = null;
            return true; // nothing to do, don't die an horrible death ;-)
        }
        responses = new JSON.Array();
        try {
            socket = new Socket(ip, port);
            Iterator recv = Netstring.recv(
                socket, Simple.netBufferSize, Simple._utf_8
                );
            Netstring.send(socket, requests.iterator(), Simple._utf_8);
            for (int i=0, L=requests.size(); i<L; i++)
                responses.add(recv.next());
        } catch (Exception e) {
            responses.add(e.toString());
            return false;
        } finally {
            requests = null;
            try {
                socket.close();
            } catch (IOException e) {
                responses.add(e.toString());
            } finally {
                socket = null;
            }
        }
        return true;
    }
}
