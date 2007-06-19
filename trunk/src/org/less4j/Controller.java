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
 * <p>In most case, developping a new Web 2.0 service with less4j translates
 * into a simple subclass of <code>Controller</code>, with only one overriden
 * method for a single JSON application state.</p>
 * 
 * <blockquote>
 * <pre>class HelloWorld extends Controller {
 *     public void jsonApplication(Actor $) {
 *         $.jsonResponse(200)
 *     }
 *}</pre></blockquote>
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
 * <p>and one of more of the six methods:</p>
 * 
 * <blockquote>
 *<pre>less4jConfigure(Actor)
 *irtd2Identify(Actor)
 *jsonRegular(Actor)
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
 * <p><b>Copyright</b> &copy; 2006 Laurent A.V. Szyster</p>
 * 
 * @version 0.30
 *
 */
public class Controller extends HttpServlet {
    
    static final long serialVersionUID = 0L; // TODO: regenerate
    
    protected static final String less4j = "less4j";

    protected static final String _test = "test";
    protected static final String _irtd2Salts = "irtd2Salts";
    protected static final String _irtd2Timeout = "irtd2Timeout"; 
    protected static final String _jsonContainers = "jsonContainers";
    protected static final String _jsonIterations = "jsonIterations";
    protected static final String _jsonRegular = "jsonRegular";
    protected static final String _jsonBytes = "jsonBytes";
    protected static final String _jdbcDriver = "jdbcDriver";
    protected static final String _jdbcURL = "jdbcURL";
    protected static final String _jdbcUsername = "jdbcUsername";
    protected static final String _jdbcPassword = "jdbcPassword";
    protected static final String _j2eeDataSource = "j2eeDataSource";
    protected static final String _ldapURL = "ldapURL";
    protected static final String _ldapUsername = "ldapUsername";
    protected static final String _ldapPassword = "ldapPassword";
    
    private JSON.Object configuration = new JSON.Object ();
    
    protected JSON.Object getConfiguration() {
        synchronized (configuration) {return configuration;}
    }
    
    protected void setConfiguration(JSON.Object object) {
        synchronized (configuration) {configuration = object;}
    }
    
    private static String _configurationPattern = ("{" +
        "\"test\": false," + // optional boolean
        "\"irtd2Salts\": [\"^...........*$\"]," + 
        "\"irtd2Service\": null," +
        "\"irtd2Timeout\": null," +
        "\"jsonBytes\": null," +
        "\"jsonContainers\": null," +
        "\"jsonIterations\": null," +
        "\"jsonRegular\": null," +
        "\"jdbcDriver\": null," +
        "\"jdbcURL\": null," +
        "\"jdbcUsername\": null," +
        "\"jdbcPassword\": null," +
        "\"j2eeDataSource\": null," +
        "\"ldapURL\": null," +
        "\"ldapUsername\": null," +
        "\"ldapPassword\": null" +
        "}"); 
           
    /**
     * <p>Returns a JSONR pattern</code> to test the actor's 
     * <code>configuration</code>.</p>
     * 
     * <h4>Synospis</h4>
     * 
     * <p>The default pattern essentially restricts configuration to a
     * namespace, typing only the values named <code>test</code> and 
     * <code>irtd2Salts</code>.</p>
     * 
     * <p>To restrict or extend the valid configuration options, override
     * this property in your class with the appropriate pattern. For instance,
     * to enforce a minimal configuration without constraints on the JSON 
     * objects handled and no SQL or LDAP connections:</p>
     * 
     * <pre>{
     *    "test": false, // default to production
     *    "irtd2Salts": [".........+"],
     *    "irtd2Timeout": 3600, // one hour maximum
     *    "jsonBytes": 4096, // relatively small passwords
     *    "jsonContainers": 1, // one object
     *    "jsonIterations": 2, // two values
     *    "jsonRegular": {
     *        "username": "@", // the universal e-mail sign by now. 
     *        "password": "........+"
     *    },
     *    "jdbcDriver": ".+",
     *    "jdbcURL": "jdbc:.+",
     *    "jdbcUsername": "(?!root|admin)", // no priviledged users 
     *    "jdbcPassword": "........+"
     *}</pre>
     */
    public JSONR.Type configurationPattern () throws JSON.Error {
        return JSONR.compile(_configurationPattern);
    }
    
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
            try {
                type = configurationPattern ();
            } catch (JSON.Error ec) {
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
        // object.put("j2eeRealPath", getServletContext().getRealPath(""));
        Actor $ = new Actor (object);
        if (!less4jConfigure ($)) throw new ServletException(
            "Failed less4j configuration."
            );
        setConfiguration(object);
    }
 
    /**
     * ...
     */
    public void service (ServletRequest req, ServletResponse res) {
        Actor $ = new Actor (
            getConfiguration(), 
            (HttpServletRequest) req, 
            (HttpServletResponse) res
            );
        if ($.irtd2Digested(
                $.configuration.intValue(_irtd2Timeout, 3600)
                ) || irtd2Identify($)) {
            if ($.digested != null) 
                $.irtd2Digest();
            String method = $.request.getMethod();
            if (method.equals(_GET))
                if ($.request.getQueryString() == null)
                    httpResource($);
                else if (jsonGET($))
                    jsonApplication($);
                else
                    httpContinue($);
            else if (
                method.equals(_POST) && 
                $.request.getContentType().equals(_application_json) &&
                $.request.getCharacterEncoding().equals(Actor._UTF_8)
                )
                if (jsonPOST($))
                    jsonApplication($); // valid JSON request
                else
                    httpContinue($);
            else
                httpContinue($);
        }
    }
    
    // private static final int jdbcTimeout = 15;
    
    /**
     * <p>Test wether this controller's configuration actually supports 
     * this Actor class at runtime.</p>
     * 
     * <p>This implementation test wether:</p> 
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
     * <h4>Synopsis</h4>
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
     * @return true if the test was successfull, false otherwise
     */
    public boolean less4jConfigure (Actor $) {
        try {
            if ($.configuration.A(_irtd2Salts).size() < 1)
                throw new Exception ();
        } catch (Exception e) {
            $.logInfo("add salt(s) to digest cookies!", less4j);
            return false;
        }
        try {
            if ($.configuration.containsKey(_jdbcDriver)) {
                Class.forName($.configuration.S(_jdbcDriver));
                //if (DriverManager.getLogWriter() != null)
                //    DriverManager.setLogWriter(null);
                //if (DriverManager.getLoginTimeout() < jdbcTimeout)
                //    DriverManager.setLoginTimeout(jdbcTimeout);
                if (sqlOpen($)) $.sqlClose(); else return false;
            } else if ($.configuration.containsKey(_j2eeDataSource))
                if (sqlOpen($)) $.sqlClose(); else return false;
        } catch (Exception e) {
            $.logError(e); return false;
        }
        if ($.configuration.containsKey(_jsonRegular)) try {
            $.configuration.put(_jsonRegular, JSONR.compile(
                $.configuration.O(_jsonRegular), JSONR.TYPES
            ));
        } catch (JSON.Error e) {$.logError(e); return false;}
        if ($.test) $.logInfo("configuration ok", less4j);
        return true;
    }

    private static final String _GET = "GET";
    private static final String _POST = "POST";
    private static final String _application_json = "application/json";

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
    protected boolean jsonGET (Actor $) {
        JSONR.Type model = jsonRegular($);
        if (model == null)
            return $.jsonGET(
                    $.configuration.intValue(_jsonContainers, 65355),
                    $.configuration.intValue(_jsonIterations, 65355)
                    );
        else 
            return $.jsonGET(
                    $.configuration.intValue(_jsonContainers, 65355),
                    $.configuration.intValue(_jsonIterations, 65355),
                    model
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
    protected boolean jsonPOST (Actor $) {
        JSONR.Type model = jsonRegular($);
        if (model == null)
            return $.jsonPOST(
                    $.configuration.intValue(_jsonBytes, 16384), 
                    $.configuration.intValue(_jsonContainers, 65355),
                    $.configuration.intValue(_jsonIterations, 65355)
                    );
        else 
            return $.jsonPOST(
                    $.configuration.intValue(_jsonBytes, 16384), 
                    $.configuration.intValue(_jsonContainers, 65355),
                    $.configuration.intValue(_jsonIterations, 65355),
                    model
                    );
        }
    
    /**
     * Identify the requester and return true to digest a new IRTD2 Cookie 
     * and continue the request or complete the response and return false, 
     * by default grant no rights to a random user ID made of ten alphanumeric 
     * characters.
     * 
     * <h4>Synopsis</h4>
     * 
     * <pre>public boolean irtd2Identify (Actor $) {
     *    $.identity = Simple.password(10);
     *    return true;
     *}</pre>
     * 
     * <p>A simpler implementation is to reply unidentified requests
     * with a <code>401 Not Authorized</code> response:
     * 
     *<pre>public boolean irtd2Identify (Actor $) {
     *   $.httpError(401)); 
     *   return false; 
     *}</pre>
     * 
     * <p>or redirect the user agent to another controller:</p>
     * 
     *<pre>public boolean irtd2Identify (Actor $) {
     *   $.http302Redirect("/login"); 
     *   return false; 
     *}</pre>
     * 
     * <p>The simplest implementation is to pass unidentified requests 
     * through, here to handle JSON login with a configurable password
     * for a <code>root</code> access in the root context "/":</p>
     * 
     *<pre>public static boolean irtd2Identify (Actor $) {
     *   return true;
     *}
     *
     *public void jsonApplication (Actor $) {
     *   if ( // there is an URL query string or a JSON body with a password
     *       $.json.S("password", "").equals(
     *           $.configuration.S("password", "less4j")
     *           )
     *       )
     *       // digest a new IRTD2 cookie for user "root" with "root" role
     *       $.identity = "root";
     *       $.rights = "root";
     *       $.irtd2Digest("/");
     *       // is identified, continue the request...
     *   else
     *       $.jsonResponse(401); // Not Authorized
     *       // not identified, response completed. 
     *}</pre>
     * 
     * <p>...</p>
     *  
     * @param $ the Actor's state
     * @return true
     */
    public boolean irtd2Identify (Actor $) {
        $.httpError(401); // Not Authorized
        return false;
    }
    
    /**
     * ...
     *
     * <h4>Synopsis</h4>
     *
     *<pre>public void httpContinue (Actor $) {
     *    return $.httpError(400); // Bad Request
     *}</pre>
     *
     * <p>...</p>
     *
     * @param $
     */
    public void httpContinue (Actor $) {
        $.httpError(400); // Bad Request
    }
    
    /**
     * <p>Transfert a resource to identified users, by default send
     * a <code>404 Not Found</code> error.</p>
     * 
     * <h4>Synopsis</h4>
     * 
     *<pre>public void httpResource (Actor $) {
     *    return $.httpError(404); // Not Found
     *}</pre>
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
    public void httpResource (Actor $) {
        $.httpError(404); // Not Found
    }

    /**
     * 
     * @param $
     * @return
     */
    public JSONR.Type jsonRegular (Actor $) {
        return (JSONR.Type) $.configuration.get(_jsonRegular);
    }
    
    /**
     * Control an audited interaction between an identified user and a 
     * JSON application.
     * 
     * <h4>Synopsis</h4>
     * 
     *<pre>public void jsonApplication (Actor $) {
     *    $.jsonResponse(400); // Bad Request
     *}</pre>
     *
     * <p>...</p>
     * 
     * @param $ the Actor's state
     */
    public void jsonApplication (Actor $) {
        $.jsonResponse(400); // Bad Request
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
        if ($.configuration.containsKey(_jdbcDriver))
            return $.sqlOpenJDBC(
                $.configuration.S(
                    _jdbcURL, "jdbc:mysql://127.0.0.1:3306/"
                    ),
                $.configuration.S(_jdbcUsername, less4j),
                $.configuration.S(_jdbcPassword, "")
                );
        else 
            return $.sqlOpenJ2EE(
                $.configuration.S(_j2eeDataSource, less4j)
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
     * @param $ the actor at play
     * @param result the name of the result to put in <code>$.json</code>
     * @param statement the SQL query to execute 
     * @param arguments the names of the arguments in <code>$.json</code>
     * @param fetch the maximum number of rows retrieved 
     * @return true if the result set is not null and nothing was throwed
     */
    public static boolean sqlQuery (
        Actor $, String result, String statement, String[] arguments, 
        int fetch, SQL.ORM model
        ) {
        if (sqlOpen($)) try {
            Object object = $.sqlQuery(
                statement, Simple.iter($.json, arguments), fetch, model
                );
            $.json.put(result, object);
            return (object == null);
        } catch (Exception e) {
            $.json.put("exception", e.getMessage());
            $.logError(e);
        } finally {
            $.sqlClose();
        }
        return false;
    }
    
    /**
     * 
     * @param $ the actor at play
     * @param result the name of the result to put in <code>$.json</code>
     * @param statement the SQL query to execute 
     * @param arguments the names of the arguments in <code>$.json</code>
     * @param fetch the maximum number of rows retrieved 
     * @return true if the result set is not null and nothing was throwed
     */
    public static boolean sqlTable (
        Actor $, String result, String statement, String[] arguments, 
        int fetch
        ) {
        return sqlQuery(
            $, result, statement, arguments, fetch, SQL.table
            );
    }
       
    /**
     * 
     * @param $ the actor at play
     * @param result the name of the result to put in <code>$.json</code>
     * @param statement the SQL query to execute 
     * @param arguments the names of the arguments in <code>$.json</code>
     * @param fetch the maximum number of rows retrieved 
     * @return true if the result set is not null and nothing was throwed
     */
    public static boolean sqlRelations (
        Actor $, String result, String statement, String[] arguments, 
        int fetch
        ) {
        return sqlQuery(
            $, result, statement, arguments, fetch, SQL.relations
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
     * <pre>if (slqCollection(
     *    $, "result", "select COLUMN from TABLE where KEY=?", 
     *    new String[]{"key"}
     *    ))
     *    ; // non-null result set fetched, maybe do something with it ...
     *else
     *    ; // exception or null result set, handle the error ...</pre>
     * 
     * @param $ the actor at play
     * @param result the name of the result to put in <code>$.json</code>
     * @param statement the SQL query to execute 
     * @param arguments the names of the arguments in <code>$.json</code>
     * @param fetch the maximum number of rows retrieved 
     * @return true if the result set is not null and nothing was throwed
     */ 
    public static boolean sqlCollection (
        Actor $, String result, String statement, String[] arguments, 
        int fetch
        ) {
        return sqlQuery(
            $, result, statement, arguments, fetch, SQL.collection
            );
    }
       
    /**
     * 
     * @param $ the actor at play
     * @param result the name of the result to put in <code>$.json</code>
     * @param statement the SQL query to execute 
     * @param arguments the names of the arguments in <code>$.json</code>
     * @param fetch the maximum number of rows retrieved 
     * @return true if the result set is not null and nothing was throwed
     */
    public static boolean sqlDictionary (
        Actor $, String result, String statement, String[] arguments,
        int fetch
        ) {
        return sqlQuery(
            $, result, statement, arguments, fetch, SQL.dictionary
            );
    }
           
    /**
     * 
     * @param $ the actor at play
     * @param result the name of the result to put in <code>$.json</code>
     * @param statement the SQL query to execute 
     * @param arguments the names of the arguments in <code>$.json</code>
     * @param fetch the maximum number of rows retrieved 
     * @return true if the result set is not null and nothing was throwed
     */
    public static boolean sqlObjects (
        Actor $, String result, String statement, String[] arguments,
        int fetch
        ) {
        return sqlQuery($, result, statement, arguments, fetch, SQL.objects);
    }
           
    /**
     * Try to open an SQL connection using the configuration properties -
     * applying <code>sqlOpenJDBC</code> or <code>sqlOpenJ2EE</code> - then
     * execute an SQL query with the named arguments to update the Actor's 
     * JSON named state and return true if no exception was raised.
     * 
     * <h4>Synopsis</h4>
     * 
     * <pre>if (slqObject(
     *    $, "result", "select * from TABLE where COLUMN=?", 
     *    String[]{"argument"}
     *    ))
     *    ; // non-null result set fetched, do something with it ...
     *else
     *    ; // exception or null result set, handle the error ...</pre>
     * 
     * @param $ the actor at play
     * @param result the name of the result to put in <code>$.json</code>
     * @param statement the SQL query to execute 
     * @param arguments the names of the arguments in <code>$.json</code>
     * @param fetch the maximum number of rows retrieved 
     * @return true if the result set is not null and nothing was throwed
     */ 
    public static boolean sqlObject (
        Actor $, String result, String statement, String[] arguments
        ) {
        return sqlQuery($, result, statement, arguments, 1, SQL.object);
    }
       
    /**
     * 
     * @param $ the actor at play
     * @param result the name of the result to put in <code>$.json</code>
     * @param statement the SQL update to execute 
     * @param arguments the names of the arguments in <code>$.json</code>
     * @return true if nothing was throwed
     */
    public static boolean sqlUpdate (
        Actor $, String result, String statement, String[] arguments
        ) {
        boolean success = false;
        if (sqlOpen($)) try {
            $.json.put(result, $.sqlUpdate(
                statement, Simple.iter($.json, arguments)
                ));
            success = true;
        } catch (Exception e) {$.logError(e);} finally {$.sqlClose();}
        return success;
    }
    
    /**
     * 
     * @param $ the actor at play
     * @param result the name of the result to put in <code>$.json</code>
     * @param statement the SQL update to execute 
     * @param relations a JSON.Array of JSON.Array with the update arguments
     * @return true if nothing was throwed
     */
    public static boolean sqlUpdate (
        Actor $, String name, String statement, JSON.Array relations
        ) {
        boolean success = false;
        if (sqlOpen($)) try {
            $.json.put(name, $.sqlBatch(statement, relations.iterator()));
            success = true;
        } catch (Exception e) {$.logError(e);} finally {$.sqlClose();}
        return success;
    }
            
    /**
     * Try to open an LDAP connection using the configuration properties,
     * or the following defaults:
     * 
     * <pre>{
     *    "ldapURL": "ldap://127.0.0.1:389/",
     *    "ldapUsername": "org.less4j.Controller",
     *    "ldapPassword": ""
     *    }</pre>
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
        if ($.configuration.containsKey(_ldapUsername))
            return $.ldapOpen(
                $.configuration.S(_ldapURL, "ldap://127.0.0.1:389/"),
                $.configuration.S(_ldapUsername, less4j), 
                $.configuration.S(_ldapPassword, "")
                );
        else
            return $.ldapOpen(
                $.configuration.S(_ldapURL, "ldap://127.0.0.1:389/")
                );
    }
    
    /**
     * Try to update the Actor's JSON with the attributes of an LDAP context, 
     * return true if the context's name was resolved, false otherwise.
     * Attributes not named in the original JSON object are filtered out.
     * 
     * @param $
     * @param dn the distinguished name to resolve
     * @param attributes names of the properties to update
     * @return true if the name was resolve, false otherwise
     * @throws JSON.Error
     */
    public static boolean ldapResolve (
        Actor $, String dn, String attributes
        ) 
    throws JSON.Error {
        if (ldapOpen($)) try {
            JSON.Object object = $.json.O(attributes);
            return $.ldapResolve(
                $.json.S(dn), object, object.keySet().iterator()
                );
        } finally {
            $.ldapClose();
        } else return false;
    }
    
    /**
     * 
     * @param $
     * @param dn the distinguished name to resolve
     * @param attributes names of the properties to create
     * @return true if the context was created, false otherwise
     * @throws JSON.Error
     */
    public static boolean ldapCreate (
        Actor $, String dn, String attributes
        ) 
    throws JSON.Error {
        if (ldapOpen($)) try {
            JSON.Object object = $.json.O(attributes);
            return $.ldapCreate(
                $.json.S(dn), object, object.keySet().iterator()
                );
        } finally {
            $.ldapClose();
        } else return false;
    }
        
} // That's all folks.