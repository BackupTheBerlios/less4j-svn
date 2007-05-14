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
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>A stateless servlet to configure, test and apply the <code>Actor</code>
 * programming intefaces, the base class from which to derive RESTfull
 * application resource controllers for Web 2.0 applications.</p>
 * 
 * <h3>Synopsis</h3>
 * 
 * <p>This class implements the <code>doGet</code> and <code>doPost</code>
 * methods of <code>HttpServlet</code>, solving common issues for RESTful 
 * AJAX applications: a request/response state configuration, test and 
 * instanciation, user identification, requests authorization, input
 * validation and application audit trail.</p>
 * 
 * <p>Protocol Interfaces</p>
 * 
 * <p>It does The Right Thing for its applications, leaving to developpers
 * the profitable and creative part of the job as the implementation of one
 * to four simple methods:</p>
 * 
 * <blockquote>
 *<pre>less4jConfigure (Actor $)
 *irtd2Identify (Actor $)
 *httpResource (Actor $)
 *jsonApplication (Actor $)</pre>
 * </blockquote>
 * 
 * <p>...</p>
 * 
 * <h4>Configurable Conveniences</h4>
 * 
 * <blockquote>
 * <pre>{
 *  "": { 
 *     "irtd2Timeout": 3600,
 *     "jsonBytes": 4096,
 *     "jsonContainers": 128,
 *     "jsonIterations": 4096,
 *     "jsonModel": {"irdt2": {"username": ".+", "password": null}}
 *  }
 *}</pre>
 * </blockquote>
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
 
    // I'm wary of subclassing doService, some HttpServlet implementation
    // may or may not support it, this is the safe bet.
    
    public void doGet (HttpServletRequest req, HttpServletResponse res) {
        Actor $ = new Actor (getConfiguration(), req, res);
        if (irtd2Identified($)||irtd2Identify($))
            less4jControl ($);
    }
    
    public void doPost (HttpServletRequest req, HttpServletResponse res) {
        Actor $ = new Actor (getConfiguration(), req, res);
        if (irtd2Identified($)||irtd2Identify($))
            less4jControl ($);
    }
    
    // private static final int jdbcTimeout = 15;
    
    /**
     * <p>Test wether this controller's configuration actually supports 
     * this Actor class at runtime.</p>
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

    public static void less4jControl (Actor $) {
        String method = $.request.getMethod();
        if (method.equals(_GET))
            if ($.httpIdempotent())
                httpResource($); // subject/predicate
            else if (jsonGET($))
                jsonApplication($); // subject/predicate?query
            else
                $.http302Redirect($.context); // ?
        else if (method.equals(_POST))
            if ($.request.getHeader("Accept").indexOf("application/json")>-1)
                if (jsonPOST($))
                    jsonApplication($); // valid JSON request
                else 
                    $.http302Redirect($.context); // invalid JSON object
            else
                ; // httpContinue() 
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
    public static boolean sqlNative (Actor $, String statement) {
        boolean success = false;
        if (sqlOpen($)) try {
            $.sql.nativeSQL(statement); success = true;
        } catch (Exception e) {$.logError(e);} finally {$.sqlClose();}
        return success;
    }
    
    public static boolean sqlQuery (
        Actor $, String statement, String[] parameters
        ) {
        boolean success = false;
        if (sqlOpen($)) try {
            $.json.putAll($.sqlQuery(statement, Simple.iterator(parameters)));
            success = true;
        } catch (Exception e) {$.logError(e);} finally {$.sqlClose();}
        return success;
    }
    
    public static boolean sqlQuery (
        Actor $, String statement, String[] parameters, int fetch
        ) {
        boolean success = false;
        if (sqlOpen($)) try {
            $.json.putAll($.sqlQuery(
                statement, Simple.iterator(parameters), fetch
                ));
        } catch (Exception e) {$.logError(e);} finally {$.sqlClose();}
        return success;
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
     * <pre>if (slqQuery(
     *    $, "result", "select * from TABLE where COLUMN=?", 
     *    new String[]{"argument"}
     *    ) && $.json.get("result") != null)
     *    ; // non-null result set fetched, do something with it ...
     *else
     *    ; // exception or null result set, handle the error ...</pre>
     * 
     * @return true if no exception was raised
     */ 
    public static boolean sqlQuery (
        Actor $, String name, String statement, String[] names
        ) {
        boolean success = false;
        if (sqlOpen($)) try {
            $.json.put(name, $.sqlQuery(
                statement, Simple.itermap($.json, names)
                ));
            success = true;
        } catch (Exception e) {$.logError(e);} finally {$.sqlClose();}
        return success;
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
     * <pre>if (slqQuery(
     *    $, "result", "select * from TABLE where COLUMN=?", 
     *    String[]{"argument"}, 10
     *    ) && $.json.get("result") != null)
     *    ; // non-null result set fetched, do something with it ...
     *else
     *    ; // exception or null result set, handle the error ...</pre>
     * 
     * @return true if no exception was raised
     */ 
    public static boolean sqlQuery (
        Actor $, String name, String statement, String[] names, int fetch
        ) {
        boolean success = false;
        if (sqlOpen($)) try {
            $.json.put(name, $.sqlQuery(
                statement, Simple.itermap($.json, names), fetch
                ));
            success = true;
        } catch (Exception e) {$.logError(e);} finally {$.sqlClose();}
        return success;
    }
        
    public static boolean sqlUpdate (
        Actor $, String name, String statement, String[] names
        ) {
        boolean success = false;
        if (sqlOpen($)) try {
            $.json.put(name, new Integer($.sqlUpdate(
                statement, Simple.itermap($.json, names)
                )));
            success = true;
        } catch (Exception e) {$.logError(e);} finally {$.sqlClose();}
        return success;
    }
            
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
    
    // standard handlers for most Web 2.0 applications of J2EE 
    //
    // irdt2Identify -> Cookie
    // httpResource -> HTML, XML, CSS, JavaScript, ...       
    // jsonApplication -> JSON
    //
    // "Everythin is going to be allright"
    
    /**
     * Identify the requester and return true to continue the request or
     * complete the response and return false.
     * 
     * <h4>Synopsis</h4>
     * 
     * <p>This is a method to overload in an application controller that
     * identify and grant rights. The default implemented by this
     * <code>Controller</code> is to identify an anonymous user with
     * a random SHA1 key and grant him no rights.</p>
     *  
     * @param $ the Actor's state
     */
    public static boolean irtd2Identify (Actor $) {
        $.identity = ""; // TODO: digest an SHA1 key as identity.
        $.irtd2Digest($.context);
        return true;
    }
    
    /**
     * <p>Transfert a resource to identified users, by default redirect to
     * the <code>index.html</code> page in this controller's context.</p>
     * 
     * <h4>Synopsis</h4>
     * 
     * <p>This is a method to overload in an application controller that
     * serve resources in this servlet context to identified users. The 
     * default is to delegate that service to another controller and redirect 
     * the user agent to a static page <code>index.html</code>.</p>
     * 
     * <p>Practically, for database and directory controllers there is
     * little else to do short of implementing your own database to URI 
     * namespace mapping for resources. Subclassing this method makes
     * it possible, but most controller will only need a static page
     * to act as a bootstrap for a JSON application.</p>
     * 
     * @param $ the Actor's state
     */
    public static void httpResource (Actor $) {
        $.http302Redirect("index.html");
    }

    /**
     * Control an audited interaction between an identified user and a 
     * JSON application.
     * 
     * <h4>Synopsis</h4>
     * 
     * <p>...</p>
     * 
     * @param $ the Actor's state
     */
    public static void jsonApplication (Actor $) {
        $.json200Ok();
    }
    
} // Three little birds on my doorstep, Singing: "this is a message to you"
