/* Copyright (C) 2006 Laurent A.V. Szyster

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

package org.less4j; // less java for more applications

// import java.sql.DriverManager;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException; 
import javax.servlet.ServletRequest; 
import javax.servlet.ServletResponse; 
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>The one - and preferrably only - obvious way to do REST right
 * with J2EE as it is since version 1.4.2: stateless, bended like PHP with a
 * healthy dose of JSON dynamism, more SQL and LDAP, less Java and no XML.</p>
 * 
 * <h3>Synopsis</h3>
 * 
 * <blockquote><pre>...</pre></blockquote>
 * 
 * <p>This class implements <code>HttpServlet.service</code> providing: 
 * a request/response state configuration, test and instanciation, user 
 * identification, requests authorization, input validation and application 
 * audit trail.</p>
 * 
 * <p>By default, each implementation of its interfaces does The Right Thing 
 * for its applications, leaving to developpers the profitable and creative 
 * part of the job as the implementation of a single static property:</p>
 * 
 * <blockquote>
 * <pre>configurationPattern</pre>
 * </blockquote>
 * 
 * <p>and one of more of the five methods:</p>
 * 
 * <blockquote>
 *<pre>less4jConfigure(Actor)
 *irtd2Identify(Actor)
 *jsonApplication(Actor)
 *httpResource(Actor)
 *httpContinue(Actor)</pre>
 * </blockquote>
 * 
 * <p>In many use case, <code>jsonApplication</code> may be the only method
 * to override. And they may be deceptively simple because this 
 * <code>Controller</code> also provides a full stack of configurable 
 * conveniences for entreprise web 2.0 applications (ie: for HTTP, JSON, SQL 
 * and LDAP).</p>
 * 
 * <h4>less4jConfigure(Actor)</h4>
 * 
 * <p>Upon initialization, the servlet's <code>less4jConfigure</code>
 * method is called with a new <code>Actor</code> instance (represented
 * throughout less4j by the <code>$</code> symbol).</p>
 * 
 * <p>The <code>Actor</code> passed has a <code>configuration</code> 
 * property decoded from the <code>less4j</code> parameter found in the 
 * servlet's <code>WEB-INF/web.xml</code> configuration file (or any other 
 * deployement configuration medium with a J2EE interface).</p>
 * 
 * <p>By convention, this method is expected to test the servlet's named
 * parameters found in the <code>$.configuration</code> JSON object.</p>
 * 
 * <p>...</p>
 * 
 * <p><b>Copyright</b> &copy; 2006 Laurent A.V. Szyster</p>
 * 
 * @version 0.30
 *
 */
public class Controller extends HttpServlet {
    
    static final long serialVersionUID = 0L; // TODO: regenerate
    
    protected static final String less4j = "less4j";
    
    private JSON.Object configuration = new JSON.Object ();
    
    protected JSON.Object getConfiguration() {
        synchronized (configuration) {return configuration;}
    }
    
    protected void setConfiguration(JSON.Object object) {
        synchronized (configuration) {configuration = object;}
    }
    
    /**
     * <p>If a valid JSONR <code>configurationPattern</code> string has 
     * been set for the controller class as a static member, it will be 
     * compiled and used to test the actor's <code>configuration</code>.
     * </p>
     * 
     * <h4>Synospis</h4>
     * 
     * <p>...</p>
     * 
     * <blockquote>
     * <pre>{"test": true}</pre>
     * </blockquote>
     * 
     * <p>...</p>
     * 
     * <blockquote>
     * <pre>{
     *    "test": false, // default to production
     *    "irtd2Salt": ".........+",
     *    "irtd2Salted": ".........+",
     *    "irtd2Timeout": 3600, // one hour maximum
     *    "jsonBytes": 4096, // relatively small passwords
     *    "jsonContainers": 1, // one object
     *    "jsonIterations": 2, // two values
     *    "jsonRegular": {
     *        "username": "@", // the universal e-mail sign by now. 
     *        "password": ".......+"
     *    }
     *}</pre>
     * </blockquote>
     * 
     * <p>...</p>
     */
    public static String configurationPattern = ("{" +
        "\"test\": false," + // optional boolean
        "\"irtd2Salt\": \"^...........*$\"," + // mandatory, 10 chars minimum
        "\"irtd2Salted\": null," +
        "\"irtd2Service\": null," +
        "\"irtd2Timeout\": null," +
        "\"jsonBytes\": null," +
        "\"jsonContainers\": null," +
        "\"jsonIterations\": null," +
        "\"jsonrModel\": null," +
        "\"jdbcDriver\": null," +
        "\"jdbcUsername\": null," +
        "\"jdbcPassword\": null," +
        "\"j2eeDataSource\": null," +
        "\"ldapURL\": null," +
        "\"ldapUsername\": null," +
        "\"ldapPassword\": null," +
        "}");
                
    /**
     * Initialize a servlet controller: extract the less4j properties,
     * test the resource configured with a new Actor and call the
     * controller's <code>less4jConfigure</code> method or raise a
     * ServletException if the configuration is incomplete and fails
     * the Actor's tests.
     *  
     * @param config the Servlet configuration <code>Map</code>
     */
    public void init (ServletConfig config) throws ServletException {
        super.init(config);
        JSON.Object object = new JSON.Object();
        String conf = config.getInitParameter(less4j);
        if (conf != null) {
            JSONR.Type type;
            try {type = JSONR.compile(configurationPattern);} 
            catch (JSON.Error ec) {
                throw new ServletException(
                    "Invalid less4j configuration JSONR pattern.", ec
                    );
            }
            JSON.Error e;
            if (type == null) {
                e = (new JSON()).update(object, conf);
                if (e != null) throw new ServletException(
                    "Invalid less4j configuration JSON string.", e
                    );
            } else {
                e = (new JSONR(type)).update(object, conf);
                if (e != null) throw new ServletException(
                    "Irregular less4j configuration.", e
                    );
            }
        }
        object.put("j2eeRealPath", getServletContext().getRealPath(""));
        Actor $ = new Actor (object);
        if (!less4jConfigure ($)) throw new ServletException(
            "Failed less4j configuration, check your SQL or LDAP parameters."
            );
        setConfiguration(object);
    }
 
    public void service (ServletRequest req, ServletResponse res) {
        Actor $ = new Actor (
            getConfiguration(), 
            (HttpServletRequest) req, 
            (HttpServletResponse) res
            );
        if (irtd2Identified($)||irtd2Identify($))
            less4jControl ($);
    }
    
    // private static final int jdbcTimeout = 15;
    
    /**
     * <p>Test wether this controller's configuration actually supports 
     * this Actor class at runtime.</p>
     * 
     * <h4>Synopsis</h4>
     * 
     * <p>For this class, test wether:</p> 
     * 
     * <ol>
     * <li>the actor instances has salt to digest a cookie and audit of
     * identification, authorizations, time and the response digested
     * previously</li>
     * <li>the configured JDBC driver or DataSource is available.</li>
     * <li>the LDAP server is also accessible, if one is configured.</li>
     * </ol>
     * 
     * <p>By definition a servlet configuration is a runtime environment
     * variable, something that cannot be relied upon without testing
     * it each time the J2EE container initialize it.</p>
     * 
     * <p>Application developers that extend the namespace of less4j's 
     * configuration and the Actor class must overload this method.</p>
     * 
     * @return true if the test was successfull, false otherwise
     */
    public static boolean less4jConfigure (Actor $) {
        if (!$.configuration.containsKey("irtd2Salt")) {
            $.logInfo("add salt to digest cookies!", less4j);
            return false;
        }
        try {
            if ($.configuration.containsKey("jdbcDriver")) {
                Class.forName($.configuration.S("jdbcDriver"));
                //if (DriverManager.getLogWriter() != null)
                //    DriverManager.setLogWriter(null);
                //if (DriverManager.getLoginTimeout() < jdbcTimeout)
                //    DriverManager.setLoginTimeout(jdbcTimeout);
                if (sqlOpen($)) $.sqlClose(); else return false;
            } else if ($.configuration.containsKey("j2eeDataSource"))
                if (sqlOpen($)) $.sqlClose(); else return false;
        } catch (Exception e) {
            $.logError(e); return false;
        }
        if ($.configuration.containsKey("jsonrModel")) try {
            $.configuration.put("jsonrModel", JSONR.compile(
                $.configuration.O("jsonModel"), JSONR.TYPES
            ));
        } catch (JSON.Error e) {$.logError(e); return false;}
        if ($.test) $.logInfo("configuration ok", less4j);
        return true;
    }

    private static final String _GET = "GET";
    private static final String _POST = "POST";

    /**
     * <p>...</p>
     * 
     * <h4>Synopsis</h4>
     * 
     * <p>...</p>
     * 
     * <blockquote>
     * <pre>GET /subject/predicate
     *Host: context
     *...</pre>
     *</blockquote>
     *
     * <blockquote>
     * <pre>GET /subject/predicate?query
     *Host: context
     *...</pre>
     * </blockquote>
     *
     * <blockquote>
     * <pre>POST /subject/predicate
     *Host: context
     *Content-type: application/javascript; charset=UTF-8
     *...</pre>
     * </blockquote>
     *
     * @param $
     * @return
     */
    public static boolean less4jControl (Actor $) {
        String method = $.request.getMethod();
        if (method.equals(_GET))
            if ($.request.getQueryString() == null)
                return httpResource($);
            else if (jsonGET($))
                return jsonApplication($); 
        else if (
            method.equals(_POST) && 
            $.request.getContentType().equals("application/json") &&
            $.request.getCharacterEncoding().equals(Actor.less4jCharacterSet)
            )
            if (jsonPOST($))
                return jsonApplication($); // valid JSON request
        return httpContinue($);
    }

    /**
     * Test the IRTD2 cookie if any, returns true only if such cookie was
     * digested with one of the two salts in this Actor's configuration
     * before the <code>irdt2Timeout</code> configured or the one hour 
     * set as default.
     * 
     * @param $ the Actor state
     * @return true if the HTTP request came with a digestable IRTD2 cookie
     */
    public static boolean irtd2Identified (Actor $) {
        return $.irtd2Digested(
            $.configuration.intValue("irtd2Timeout", 3600)
            );
        } 
    
    /**
     * Parse the request query string in the Actor's <code>json</code> state
     * enforce the configured constraints on containers and iterations
     * and eventualy apply a configured JSONR model, return <code>true</code>
     * if a valid JSON object was successfully updated.  
     * 
     * <h4>Synospis</h3>
     * 
     * <p>This method is applied by <code>doGet</h3> to discriminate 
     * requests that will be passed to <code>jsonApplication</code>,
     * providing one way to instanciate the JSON state of an action.</p>
     * 
     * <p>Practically, there's little else to do: query strings are too 
     * short to go crazy about more than what less4j allready provides.</p>
     * 
     * @param $ the Actor state
     * @return true if a valid JSON expression was found in a GET request
     */
    public static boolean jsonGET (Actor $) {
        if ($.configuration.containsKey("jsonrModel"))
            return $.jsonGET(
                $.configuration.intValue("jsonContainers", 65355),
                $.configuration.intValue("jsonIterations", 65355),
                (JSONR.Type) $.configuration.get("jsonrModel")
                );
        else 
            return $.jsonGET(
                $.configuration.intValue("jsonContainers", 65355),
                $.configuration.intValue("jsonIterations", 65355)
                );
        }
    
    /**
     * Parse the request posted JSON string in the Actor's state, enforce the 
     * configured constraints on POSTed bytes, containers and iterations,
     * eventualy apply a configured JSONR model, return <code>true</code>
     * if a valid JSON object was successfully updated.  
     * 
     * <h4>Synospis</h3>
     * 
     * <p>This method is applied by <code>doPost</h3> to discriminate 
     * requests that will be passed to <code>jsonApplication</code>,
     * providing one way to instanciate the JSON state of an action.</p>
     * 
     * <p>By default it enforces a strict 16KB limit on POSTed JSON data
     * and allows for up to 65355 containers and iterations.</p>
     * 
     * <p>Again, there's little else to do, but you can still decide to
     * handle POSTed JSON data in a completely different way and yet 
     * let <code>doPost</code> work as expected by <code>jsonApplication</code>
     * developed with this implementation.</p>
     * 
     * @param $ the Actor state
     * @return true if a valid JSON expression was found in a POST request
     */
    public static boolean jsonPOST (Actor $) {
        if ($.configuration.containsKey("jsonrModel")) 
            return $.jsonPOST(
                $.configuration.intValue("jsonBytes", 16384), 
                $.configuration.intValue("jsonContainers", 65355),
                $.configuration.intValue("jsonIterations", 65355),
                (JSONR.Type) $.configuration.get("jsonrModel")
                );
        else 
            return $.jsonPOST(
                $.configuration.intValue("jsonBytes", 16384), 
                $.configuration.intValue("jsonContainers", 65355),
                $.configuration.intValue("jsonIterations", 65355)
                );
        }
    
    /**
     * Try to open an SQL connection using the configuration properties,
     * applying <code>sqlOpenJDBC</code> or <code>sqlOpenJ2EE</code>.
     * 
     * <h4>Synopsis</h4>
     * 
     * <p>A typical use of <code>sqlOpen</code> is to try to open a
     * database connection and make a sequence of requests, handling each
     * error conditions as branches in a simple procedure.</p>  
     * 
     * <pre>if (sqlOpen($)) {
     *    try {
     *        $.sqlQuery('this', 'SELECT ...', 10);
     *        doThisAndThat();
     *        $.sqlUpdate('that', 'INSERT ...');
     *        $.sqlUpdate('didThisAndThat', 'UPDATE ...');
     *        $.json200Ok();
     *    catch (Exception e) {
     *        $.logError(e);
     *        undoThat();
     *        $.sqlUpdate('undidThat', 'DELETE ...');
     *        $.rest302Redirect();
     *    } finally {
     *        $.sqlClose();
     *    }
     *}</pre>
     *
     * <p>And do it without naming the database, user name and password
     * in the application code, leaving it for configuration ... or better.</p>
     * 
     * <p>Here a convention can be used before configuration: use the
     * local MySQL server and identify a <code>Class</code> user with
     * an empty password string. That's a decent default which allows
     * to configure authorization on the SQL server once for all the
     * executions of this controller's class.</p>
     * 
     * @return true if the connection was successfull, false otherwise
     */
    public static boolean sqlOpen (Actor $) {
        if ($.configuration.containsKey("jdbcDriver"))
            return $.sqlOpenJDBC(
                $.configuration.S(
                    "jdbcURL", "jdbc:mysql://127.0.0.1:3306/"
                    ),
                $.configuration.S("jdbcUsername", less4j),
                $.configuration.S("jdbcPassword", "")
                );
        else 
            return $.sqlOpenJ2EE(
                $.configuration.S("j2eeDataSource", less4j)
                );
    }

    /**
     * Try to open an SQL connection using the configuration properties -
     * applying <code>sqlOpenJDBC</code> or <code>sqlOpenJ2EE</code> - then
     * execute a native SQL statement and return true if no exception was
     * raised.
     * 
     * @return true if no exception was raised
     */ 
    public static boolean sqlExecute (Actor $, String statement) {
        boolean success = false;
        if (sqlOpen($)) try {
            $.sql.nativeSQL(statement); success = true;
        } catch (Exception e) {$.logError(e);} finally {$.sqlClose();}
        return success;
    }
    
    /**
     * 
     * @param $
     * @param result
     * @param statement
     * @param arguments
     * @param fetch
     * @param sql2json
     * @return
     */
    public static boolean sqlQuery (
        Actor $, String result, String statement, String[] arguments, 
        int fetch, Actor.SQL2 sql2json
        ) {
        boolean success = false;
        if (sqlOpen($)) try {
            $.json.put(result, $.sqlQuery(
                statement, Simple.itermap($.json, arguments), 
                fetch, sql2json
                ));
            success = ($.json.get(result) != null);
        } catch (Exception e) {
            $.json.put("exception", e.getMessage());
            $.logError(e);
        } finally {
            $.sqlClose();
        }
        return success;
    }
    
    /**
     * 
     * @param $
     * @param result
     * @param statement
     * @param arguments
     * @param fetch
     * @return
     */
    public static boolean sqlTable (
            Actor $, String result, String statement, String[] arguments, 
            int fetch
            ) {
            return sqlQuery(
                $, result, statement, arguments, fetch, Actor.sql2Table
                );
        }
       
    /**
     * 
     * @param $
     * @param result
     * @param statement
     * @param arguments
     * @param fetch
     * @return
     */
    public static boolean sqlRelations (
        Actor $, String result, String statement, String[] arguments, 
        int fetch
        ) {
        return sqlQuery(
            $, result, statement, arguments, fetch, Actor.sql2Relations
            );
    }
        
    /**
     * Try to open an SQL connection using the configuration properties -
     * applying <code>sqlOpenJDBC</code> or <code>sqlOpenJ2EE</code> - then
     * execute an SQL query with the named arguments to update the Actor's 
     * JSON named state and return true if no exception was raised.
     * 
     * <h4>Synopsis</h4>
     * 
     * <p>...</p>
     * 
     * <pre>if (slqCollection(
     *    $, "result", "select COLUMN from TABLE where KEY=?", 
     *    new String[]{"key"}
     *    ))
     *    ; // non-null result set fetched, maybe do something with it ...
     *else
     *    ; // exception or null result set, handle the error ...</pre>
     * 
     * @return true if no exception was raised
     */ 
    public static boolean sqlCollection (
        Actor $, String result, String statement, String[] arguments, 
        int fetch
        ) {
        return sqlQuery(
            $, result, statement, arguments, fetch, Actor.sql2Collection
            );
    }
       
    /**
     * 
     * @param $
     * @param result
     * @param statement
     * @param arguments
     * @param fetch
     * @return
     */
    public static boolean sqlObjects (
            Actor $, String result, String statement, String[] arguments,
            int fetch
            ) {
            return sqlQuery(
                $, result, statement, arguments, fetch, Actor.sql2Objects
                );
        }
           
    /**
     * Try to open an SQL connection using the configuration properties -
     * applying <code>sqlOpenJDBC</code> or <code>sqlOpenJ2EE</code> - then
     * execute an SQL query with the named arguments to update the Actor's 
     * JSON named state and return true if no exception was raised.
     * 
     * <h4>Synopsis</h4>
     * 
     * <p>...</p>
     * 
     * <pre>if (slqObject(
     *    $, "result", "select * from TABLE where COLUMN=?", 
     *    String[]{"argument"}
     *    ))
     *    ; // non-null result set fetched, do something with it ...
     *else
     *    ; // exception or null result set, handle the error ...</pre>
     * 
     * @return true if no exception was raised
     */ 
    public static boolean sqlObject (
        Actor $, String result, String statement, String[] arguments
        ) {
        return sqlQuery(
            $, result, statement, arguments, 1, Actor.sql2Object
            );
    }
       
    /**
     * 
     * @param $
     * @param result
     * @param statement
     * @param arguments
     * @return
     */
    public static boolean sqlUpdate (
        Actor $, String result, String statement, String[] arguments
        ) {
        boolean success = false;
        if (sqlOpen($)) try {
            $.json.put(result, new Integer($.sqlUpdate(
                statement, Simple.itermap($.json, arguments)
                )));
            success = true;
        } catch (Exception e) {$.logError(e);} finally {$.sqlClose();}
        return success;
    }
    
    /**
     * 
     * @param $
     * @param name
     * @param statement
     * @param relations
     * @return
     */
    public static boolean sqlUpdate (
        Actor $, String name, String statement, JSON.Array relations
        ) {
        boolean success = false;
        if (sqlOpen($)) try {
            $.json.put(name, $.sqlUpdateMany(statement, relations.iterator()));
            success = true;
        } catch (Exception e) {$.logError(e);} finally {$.sqlClose();}
        return success;
    }
            
    /**
     * Try to open an LDAP connection using the configuration properties,
     * or the following defaults:
     * 
     * {
     *    "ldapURL": "ldap://127.0.0.1:389/",
     *    "ldapUsername": "org.less4j.Controller",
     *    "ldapPassword": ""
     *    }
     * 
     * and return true if the connection was successfull, false otherwise.
     * 
     * <h4>Synopsis</h4>
     * 
     * <p>Here a convention can be used before configuration: use the
     * local LDAP server and identify a <code>Class</code> user with
     * an empty password string. That's a decent default which allows
     * to configure authorization on the LDAP server once for all the
     * executions of this controller's class.</p>
     * 
     * <p>A typical use of <code>ldapOpen</code> is to try to open an
     * LDAP connection and make a sequence of requests, handling each
     * error conditions as branches in a simple procedure.</p>  
     * 
     * <pre>if (ldapOpen($)) {
     *    try {
     *        $.ldapResolve('cn=cname');
     *        $.json200Ok();
     *    catch (Exception) {
     *        $.rest302Redirect();
     *    } finally {
     *        $.ldapClose();
     *    }
     *}</pre>
     *
     * <p>...</p>
     * 
     * @return true if the connection was successfull, false otherwise
     */
    public static boolean ldapOpen (Actor $) {
        return $.ldapOpen(
            $.configuration.S("ldapURL", "ldap://127.0.0.1:389/"),
            $.configuration.S("ldapUsername", less4j), 
            $.configuration.S("ldapPassword", "")
            );
    }
    
    /**
     * Identify the requester and return true to continue the request or
     * complete the response and return false, by default grant no rights
     * to a random user ID made of ten alphanumeric characters.
     * 
     * <h4>Synopsis</h4>
     * 
     * <blockquote>
     * <pre>public static boolean irtd2Identify (Actor $) {
     *    $.identity = Simple.password(10);
     *    $.irtd2Digest($.context);
     *    return true;
     *}</pre>
     * </blockquote>
     * 
     * <p>A simpler implementation is to reply unidentified requests
     * with a <code>401 Not Authorized</code> response:
     * 
     * <blockquote>
     *<pre>public static boolean irtd2Identify (Actor $) {
     *   $.httpError(401)); return false; // Not Authorized
     *}</pre>
     * </blockquote>
     * 
     * <p>or redirect the user agent to another controller:</p>
     * 
     * <blockquote>
     *<pre>public static boolean irtd2Identify (Actor $) {
     *   $.http302Redirect("/login"); return false; 
     *}</pre>
     * </blockquote>
     * 
     * <p>The simplest implementation is to pass unidentified requests 
     * through, here to handle JSON login with a configurable password
     * for a <code>root</code> access in the root context "/":</p>
     * 
     * <blockquote>
     *<pre>public static boolean irtd2Identify (Actor $) {
     *   return true;
     *}
     *
     *public static boolean jsonApplication (Actor $) {
     *   if ( // there is an URL query string or a JSON body with a password
     *       $.json.S("password", "").equals(
     *           $.configuration.S("password", "less4j")
     *           )
     *       )
     *       // digest a new IRTD2 cookie for user "root" with "root" role
     *       $.identity = "root";
     *       $.rights = "root";
     *       $.irtd2Digest("/");
     *       return true; 
     *       // is identified, continue the request...
     *   else
     *       $.httpError(401); // Not Authorized
     *       return false; 
     *       // not identified, response completed. 
     *}</pre>
     * </blockquote>
     * 
     * <p>...</p>
     *  
     * @param $ the Actor's state
     * @return true
     */
    public static boolean irtd2Identify (Actor $) {
        $.identity = Simple.password(10);
        $.irtd2Digest($.context);
        return true;
    }
    
    /**
     * ...
     *
     * <h4>Synopsis</h4>
     *
     * <blockquote>
     *<pre>public static boolean httpContinue (Actor $) {
     *    return $.httpError(400); // Bad Request
     *}</pre>
     *</blockquote>
     *
     * <p>...</p>
     *
     * @param $
     * @return true
     */
    public static boolean httpContinue (Actor $) {
        return $.httpError(400); // Bad Request
    }
    
    /**
     * <p>Transfert a resource to identified users, by default send
     * a <code>404 Not Found</code> error.</p>
     * 
     * <h4>Synopsis</h4>
     * 
     * <blockquote>
     *<pre>public static boolean httpResource (Actor $) {
     *    return $.httpError(404); // Not Found
     *}</pre>
     *</blockquote>
     *
     * <p>This is a method to overload in an application controller that
     * serve resources in this servlet context to identified users.</p>
     * 
     * <p>Practically, for database and directory controllers there is
     * little else to do short of implementing your own database to URI 
     * namespace mapping for resources. Subclassing this method makes
     * it possible, but most controller will only need a static page
     * to act as a bootstrap for a JSON application.</p>
     * 
     * @param $ the Actor's state
     */
    public static boolean httpResource (Actor $) {
        return $.httpError(404); // Not Found
    }

    /**
     * Control an audited interaction between an identified user and a 
     * JSON application.
     * 
     * <h4>Synopsis</h4>
     * 
     * <blockquote>
     *<pre>public static boolean jsonApplication (Actor $) {
     *    return $.json200Ok();
     *}</pre>
     *</blockquote>
     *
     * <p>...</p>
     * 
     * @param $ the Actor's state
     */
    public static boolean jsonApplication (Actor $) {
        return $.json200Ok($.toString());
    }
    
} // That's all folks.
