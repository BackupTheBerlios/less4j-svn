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

package org.less4j; // less java for more applications

import javax.servlet.ServletConfig;
import javax.servlet.ServletException; 
import javax.servlet.ServletRequest; 
import javax.servlet.ServletResponse; 
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.HashMap;
import java.util.Iterator;

/**
 * <p>One obvious way to do REST right with J2EE as it is since version 1.4.2: 
 * stateless, bended like PHP with a healthy dose of JSON dynamism, more 
 * protocols, less Java and no XML.</p>
 * 
 * <h3>Synopsis</h3>
 * 
 * <pre>&lt;servlet&gt;
 *  &lt;servlet-name&gt;less4jscript&lt;/servlet-name&gt;
 *  &lt;servlet-class&gt;org.less4j.Controller&lt;/servlet-class&gt;
 *  &lt;init-param&gt;
 *    &lt;param-name&gt;less4j&lt;/param-name&gt;
 *    &lt;param-value&gt;&lt;![CDATA[
 *    {
 *      "test": false, 
 *      "functions": {
 *        "\/hello-world", "org.less4j.tests.HelloWorld"
 *        }
 *      }
 *    ]]&gt;&lt;/param-value&gt;
 *  &lt;/init-param&gt;
 *&lt;/servlet&gt;</pre>
 * 
 * <p>This class can be used as-is, with configured functions only and
 * no other HTTP resources than a dictionary of JSONR interfaces.</p> 
 * 
 * <p>It fits the common use case of a web controller that aggregates 
 * functions on a set of resource like file folders, an SQL database or 
 * an LDAP direcory. This implementation supports dispatching of an HTTP 
 * requests to a configured <code>Function</code> based on the request URL's
 * <code>PATH_INFO</code>.</p>
 * 
 * <p>Application developpers can also derive from <code>Controller</code>
 * to implement ad-hoc servlets. The <code>Script</code> controller
 * included in less4j provides a JavaScript interpreter to prototype 
 * new <code>Function</code> and <code>Controller</code> classes.</p>
 * 
 * @copyright 2006-2007 Laurent Szyster
 * 
 */
public class Controller extends HttpServlet implements Function {
    
    /**
     * A <code>HashMap</code> of URL <code>PATH_INFO</code> keys to less4j
     * <code>Function</code>.
     */
    public HashMap functions = null;
    
    /**
     * A JSON string that represents the configured functions' interfaces.
     */
    public String interfaces = "null";
    
    protected static final String less4j = "less4j";

    protected static final String _test = "test";
    protected static final String _irtd2Salts = "irtd2Salts";
    protected static final String _irtd2Timeout = "irtd2Timeout"; 
    protected static final String _postBytes = "postBytes";
    protected static final String _jsonContainers = "jsonContainers";
    protected static final String _jsonIterations = "jsonIterations";
    protected static final String _jsonRegular = "jsonRegular";
    protected static final String _jdbcDriver = "jdbcDriver";
    protected static final String _jdbcURL = "jdbcURL";
    protected static final String _jdbcUsername = "jdbcUsername";
    protected static final String _jdbcPassword = "jdbcPassword";
    protected static final String _j2eeDataSource = "j2eeDataSource";
    protected static final String _ldapURL = "ldapURL";
    protected static final String _ldapUsername = "ldapUsername";
    protected static final String _ldapPassword = "ldapPassword";
    
    private JSON.Object _configuration = new JSON.Object ();
    
    protected JSON.Object getConfiguration() {
        synchronized (_configuration) {return _configuration;}
    }
    
    protected void setConfiguration(JSON.Object object) {
        synchronized (_configuration) {_configuration = object;}
    }
    
    protected static String _configurationPattern = ("{" +
        "\"test\": false," + // optional boolean
        "\"functions\": {\"\\/.*\": \".+\"}," + 
        "\"irtd2Salts\": [\"^...........*$\"]," + 
        "\"irtd2Timeout\": null," +
        "\"postBytes\": null," +
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
     *    "irtd2Timeout": 3600, // one hour maximum
     *    "postBytes": 4096, // relatively small passwords
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
        object.put("j2eeRealPath", getServletContext().getRealPath(""));
        Actor $ = new Actor (object);
        $.context = "";
        if (!less4jConfigure ($)) throw new ServletException(
            "Failed less4j configuration."
            );
        setConfiguration(object);
    }
 
    // private static final int jdbcTimeout = 15;
    
    protected static final String _functions = "functions";
    protected static final String _singleton = "singleton";
    
    /**
     * <p>Test wether this controller's configuration actually supports 
     * this Actor class at runtime, supply a random IRTD2 salt if missing
     * and return false if the configured SQL or LDAP resources are not
     * available.</p>
     * 
     * <h4>Synopsis</h4>
     * 
     * <p>Application developers that extend the namespace of less4j's 
     * configuration and the Actor class must overload this method.</p>
     * 
     * <pre>public boolean less4jConfigure (Actor $) {
     *    if (super.less4jConfigure($)) {
     *        // configure your extension here
     *        return true;
     *    } else
     *        return false;
     *}</pre>
     * 
     * <p>By definition a servlet configuration is a runtime environment
     * variable, something that cannot be relied upon without testing
     * it each time the J2EE container initialize it.</p>
     * 
     * @return true if the configuration was successfull, false otherwise
     */
    public boolean less4jConfigure (Actor $) {
        // Configure the controller's IRTD2, JSONR, SQL and LDAP first ...
        JSON.Array salts = $.configuration.A(_irtd2Salts, null);
        if (salts == null || salts.size() < 1) {
            salts = new JSON.Array();
            salts.add(Simple.password(20));
            $.configuration.put(_irtd2Salts, salts);
        }
        if ($.configuration.containsKey(_jsonRegular)) try {
            $.configuration.put(_jsonRegular, JSONR.compile(
                $.configuration.O(_jsonRegular), JSONR.TYPES
            ));
        } catch (JSON.Error e) {
            $.logError(e); 
            return false;
            }
        if ($.configuration.containsKey(_jdbcDriver)) {
            try {
                Class.forName($.configuration.S(_jdbcDriver));
            } catch (Exception de) {
                $.logError(de); 
                return false;
            }
            if (sqlOpen($)) 
                $.sqlClose(); 
            else 
                return false;
            
        } else if ($.configuration.containsKey(_j2eeDataSource))
            if (sqlOpen($)) 
                $.sqlClose(); 
            else 
                return false;
        
        if ($.configuration.containsKey(_ldapURL)) {
            if (ldapOpen($)) 
                $.ldapClose();
            else
                return false;
        }
        // ... then configure the functions and compile JSONR interfaces.
        functions = new HashMap();
        if ($.configuration.containsKey(_functions)) try {
            JSON.Object classes = $.configuration.O(_functions);
            functions = new HashMap();
            Iterator paths = classes.keySet().iterator();
            if (paths.hasNext()) {
                StringBuffer sb = new StringBuffer();
                sb.append('{');
                String path = (String) paths.next();
                JSON.strb(sb, path);
                sb.append(':');
                Class fun =  Class.forName(classes.S(path));
                Function function = (
                    (Function) fun.getDeclaredField(_singleton).get(fun)
                    );
                $.about = path;
                if (!function.less4jConfigure($) && !$.test)
                    return false;
                
                functions.put(path, function);
                JSON.strb(sb, (function).jsonInterface($));
                while (paths.hasNext()) {
                    sb.append(',');
                    path = (String) paths.next();
                    JSON.strb(sb, path);
                    sb.append(':');
                    fun = Class.forName(classes.S(path));
                    function = (
                        (Function) fun.getDeclaredField(_singleton).get(fun)
                        );
                    $.about = path;
                    if (!function.less4jConfigure($) && !$.test)
                        return false;
                    
                    functions.put(path, function);
                    JSON.strb(sb, (function).jsonInterface($));
                }
                sb.append('}');
                interfaces = sb.toString();
            }
        } catch (Exception fe) {
            $.logError(fe); 
            return false;
        }
        if ($.test) $.logInfo("configuration ok", less4j);
        return true;
    }

    /**
     * Returns a JSON string mapping the URL path of the <code>Function</code>
     * configured to their JSONR pattern. 
     */
    public String jsonInterface (Actor $) {
        return interfaces;
    }

    protected static final String _GET = "GET";
    protected static final String _POST = "POST";
    protected static final String _application_json = "application/json";

    /**
     * Dispatch IRTD2 identified requests to one of the interfaces of either 
     * a configured <code>Function</code> or this <code>Controller</code>.
     */
    public void service (ServletRequest req, ServletResponse res) {
        Actor $ = new Actor (
            getConfiguration(), 
            (HttpServletRequest) req, 
            (HttpServletResponse) res
            );
        Function function;
        if ($.about == null || !functions.containsKey($.about))
            function = this;
        else {
            function = (Function) functions.get($.about);
        } 
        if ($.irtd2Digested(
                $.configuration.intValue(_irtd2Timeout, 3600)
                ) || function.irtd2Identify($)) {
            if ($.digested != null) 
                $.irtd2Digest();
            String method = $.request.getMethod();
            if (method.equals(_GET)) {
                if ($.request.getQueryString() == null)
                    function.httpResource($);
                else if ($.jsonGET(jsonRegular($)))
                    function.jsonApplication($);
                else
                    $.httpError(400);
                return;
            } 
            String contentType = $.request.getContentType();
            if (
                method.equals(_POST) && contentType != null &&
                contentType.startsWith(_application_json)
                ) {
                if ($.jsonPOST($.configuration.intValue(
                    _postBytes, Simple.netBufferSize
                    ), function.jsonRegular($)))
                    function.jsonApplication($);
                else
                    $.httpError(400);
                return;
            }
            function.httpContinue($, method, contentType);
        }
    }
    
    /**
     * Set the request IRTD2 with a random identity, no rights and 
     * allways return true.
     *  
     * @param $ the Actor's state
     * @return true
     */
    public boolean irtd2Identify (Actor $) {
        $.identity = Simple.password(10);
        $.rights = "";
        return true;
    }
    
    /**
     * Reply with an HTTP <code>400</code> error to requests not handled by 
     * this controller's or a function's <code>httpResource</code> and 
     * <code>jsonApplication</code> methods. 
     *
     * @param $ the Actor's state
     */
    public void httpContinue (Actor $, String method, String contentType) {
        $.httpError(400); // Bad Request
    }
    
    /**
     * <p>Reply to idempotent HTTP requests not handled by a configured
     * <code>Function</code> with the JSON interfaces description of the
     * functions supported if the request exactly matches this servlet
     * context path (ie: its root), or replies a <code>404 Not 
     * Found</code> error.</p>
     * 
     * @param $ the Actor's state
     */
    public void httpResource (Actor $) {
        if ($.about == null)
            $.jsonResponse(200, interfaces);
        else
            $.httpError(404); // Not Found
    }

    /**
     * <p>Returns the configured <code>JSON</code> or <code>JSONR</code> 
     * interpreter to validate a GET request's query string or a POSTed 
     * JSON request body.</p> 
     * 
     * @param $ the Actor's state
     * @return a <code>JSON</code> or <code>JSONR</code> interpreter
     */
    public Object jsonRegular (Actor $) {
        JSONR.Type model = (JSONR.Type) $.configuration.get(_jsonRegular);
        if (model == null) {
            return new JSON(
                $.configuration.intValue(_jsonContainers, 65355),
                $.configuration.intValue(_jsonIterations, 65355)
                );
        } else {
            return new JSONR(
                model,
                $.configuration.intValue(_jsonContainers, 65355),
                $.configuration.intValue(_jsonIterations, 65355)
                );
        }
    }
    
    /**
     * Reply with an HTTP error 501 to JSON application requests not handled
     * by the configured <code>Function</code>s.
     * 
     * @param $ the Actor's state
     */
    public void jsonApplication (Actor $) {
        $.jsonResponse(501);
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
        else if ($.configuration.containsKey(_j2eeDataSource))
            return $.sqlOpenJ2EE(
                $.configuration.S(_j2eeDataSource, less4j)
                );
        else {
            $.logError(new Exception(
                "No JDBC or J2EE datasource configured"
                ));
        }
        return false;
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
    
    /**
     * ...
     * 
     * @param $ the actor at play
     * @param name of the result to put in <code>$.json</code>
     * @param statement the SQL query to execute 
     * @param arguments the names of the arguments in <code>$.json</code>
     * @param fetch the maximum number of rows retrieved 
     * @return true if the result set is not null and nothing was throwed
     */
    public static boolean sqlQuery (
        Actor $, String name, String statement, String[] arguments, 
        int fetch, SQL.ORM model
        ) {
        if (sqlOpen($)) try {
            Object object = $.sqlQuery(
                statement, Simple.iter($.json, arguments), fetch, model
                );
            $.json.put(name, object);
            return (object != null);
        } catch (Exception e) {
            $.json.put("exception", e.getMessage());
            $.logError(e);
        } finally {
            $.sqlClose();
        }
        return false;
    }
    
    /**
     * ...
     * 
     * @param $ the actor at play
     * @param name of the result to put in <code>$.json</code>
     * @param statement the SQL query to execute 
     * @param arguments the names of the arguments in <code>$.json</code>
     * @param fetch the maximum number of rows retrieved 
     * @return true if the result set is not null and nothing was throwed
     */
    public static boolean sqlTable (
        Actor $, String name, String statement, String[] arguments, 
        int fetch
        ) {
        return sqlQuery($, name, statement, arguments, fetch, SQL.table);
    }
       
    /**
     * 
     * @param $ the actor at play
     * @param name of the result to put in <code>$.json</code>
     * @param statement the SQL query to execute 
     * @param arguments the names of the arguments in <code>$.json</code>
     * @param fetch the maximum number of rows retrieved 
     * @return true if the result set is not null and nothing was throwed
     */
    public static boolean sqlRelations (
        Actor $, String name, String statement, String[] arguments, 
        int fetch
        ) {
        return sqlQuery($, name, statement, arguments, fetch, SQL.relations);
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
     * @param name of the result to put in <code>$.json</code>
     * @param statement the SQL query to execute 
     * @param arguments the names of the arguments in <code>$.json</code>
     * @param fetch the maximum number of rows retrieved 
     * @return true if the result set is not null and nothing was throwed
     */ 
    public static boolean sqlCollection (
        Actor $, String name, String statement, String[] arguments, 
        int fetch
        ) {
        return sqlQuery($, name, statement, arguments, fetch, SQL.collection);
    }
       
    /**
     * 
     * @param $ the actor at play
     * @param name of the result to put in <code>$.json</code>
     * @param statement the SQL query to execute 
     * @param arguments the names of the arguments in <code>$.json</code>
     * @param fetch the maximum number of rows retrieved 
     * @return true if the result set is not null and nothing was throwed
     */
    public static boolean sqlDictionary (
        Actor $, String name, String statement, String[] arguments,
        int fetch
        ) {
        return sqlQuery($, name, statement, arguments, fetch, SQL.dictionary);
    }
           
    /**
     * 
     * @param $ the actor at play
     * @param name of the result to put in <code>$.json</code>
     * @param statement the SQL query to execute 
     * @param arguments the names of the arguments in <code>$.json</code>
     * @param fetch the maximum number of rows retrieved 
     * @return true if the result set is not null and nothing was throwed
     */
    public static boolean sqlIndex (
        Actor $, String name, String statement, String[] arguments,
        int fetch
        ) {
        return sqlQuery($, name, statement, arguments, fetch, SQL.index);
    }
               
    /**
     * 
     * @param $ the actor at play
     * @param name of the result to put in <code>$.json</code>
     * @param statement the SQL query to execute 
     * @param arguments the names of the arguments in <code>$.json</code>
     * @param fetch the maximum number of rows retrieved 
     * @return true if the result set is not null and nothing was throwed
     */
    public static boolean sqlObjects (
        Actor $, String name, String statement, String[] arguments,
        int fetch
        ) {
        return sqlQuery($, name, statement, arguments, fetch, SQL.objects);
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
     * @param name of the result to put in <code>$.json</code>
     * @param statement the SQL query to execute 
     * @param arguments the names of the arguments in <code>$.json</code>
     * @return true if the result set is not null and nothing was throwed
     */ 
    public static boolean sqlObject (
        Actor $, String name, String statement, String[] arguments
        ) {
        return sqlQuery($, name, statement, arguments, 1, SQL.object);
    }
       
    /**
     * ...
     * 
     * @param $ the actor at play
     * @param name of the result to put in <code>$.json</code>
     * @param statement the SQL update to execute 
     * @param arguments the names of the arguments in <code>$.json</code>
     * @return true if nothing was throwed
     */
    public static boolean sqlUpdate (
        Actor $, String name, String statement, String[] arguments
        ) {
        boolean success = false;
        if (sqlOpen($)) try {
            $.json.put(name, $.sqlUpdate(
                statement, Simple.iter($.json, arguments)
                ));
            $.sql.commit();
            success = true;
        } catch (Exception e) {$.logError(e);} finally {$.sqlClose();}
        return success;
    }
    
    /**
     * ...
     * 
     * @param $ the Actor at play
     * @param name of the result to put in <code>$.json</code>
     * @param statement the SQL update to execute 
     * @param relations a JSON.Array of JSON.Array with the update arguments
     * @return true if nothing was throwed
     */
    public static boolean sqlBatch (
        Actor $, String name, String statement, JSON.Array relations
        ) {
        boolean success = false;
        if (sqlOpen($)) try {
            $.json.put(name, $.sqlBatch(statement, relations.iterator()));
            $.sql.commit();
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
     * @param $ the Actor at play
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
     * Try to open the configured LDAP connection to to update the Actor's 
     * JSON with the named attributes of a context, return true if the 
     * context's distinguished name was resolved, false otherwise.
     * 
     * @param $ the Actor at play
     * @param dn the distinguished name of the context to resolve
     * @param attributes names of the context properties to resolve
     * @return true if the context was resolved, false otherwise
     */
    public static boolean ldapResolve (
        Actor $, String dn, String[] attributes
        ) {
        if (ldapOpen($)) try {
            return $.ldapResolve(dn, $.json, Simple.iter(attributes));
        } finally {
            $.ldapClose();
        } else 
            return false;
    }
    
    /**
     * Try to open the configured LDAP connection to to update a context with 
     * the named attributes of the Actor's JSON state, return true if the 
     * context was updated, false otherwise.
     * 
     * @param $ the Actor at play
     * @param dn the distinguished name of the context to update
     * @param attributes names of the properties to update
     * @return true if the context was updated, false otherwise
     */
    public static boolean ldapUpdate (
        Actor $, String dn, String[] attributes
        ) {
        if (ldapOpen($)) try {
            return $.ldapUpdate(dn, $.json, Simple.iter(attributes));
        } finally {
            $.ldapClose();
        } else return false;
    }
    
    /**
     * Try to open the configured LDAP connection to create a context with 
     * the named attributes of the Actor's JSON state, return true if the 
     * context was created, false otherwise.
     * 
     * @param $ the Actor at play
     * @param dn the distinguished name of the context to create
     * @param attributes names of the properties to create
     * @return true if the context was created, false otherwise
     */
    public static boolean ldapCreate (
        Actor $, String dn, String[] attributes
        ) {
        if (ldapOpen($)) try {
            return $.ldapCreate(dn, $.json, Simple.iter(attributes));
        } finally {
            $.ldapClose();
        } else return false;
    }
    
} // That's all folks.