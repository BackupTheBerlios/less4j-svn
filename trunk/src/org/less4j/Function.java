package org.less4j;

/**
 * An interface to implement configurable functions dispatched by less4j's 
 * base <code>Controller</code> but also extended <code>Controller</code> 
 * classes.
 * 
 * <h3>Synopsis</h3>
 * 
 * <p>... <code>HelloWorld.java</code> ...</p>
 *
 * <pre>package org.less4j.tests;
 * 
 *import org.less4j.*;
 * 
 *class HelloWorld implements Function {
 *    public Function singleton = new HelloWorld ();
 *    private JSONR.Type _interface;
 *    public String jsonInterface (Actor $) {
 *        return "{\"hello\":\"world[!]\"}";
 *    }
 *    public boolean less4jConfigure (Actor $) {
 *        try {
 *            _interface = JSONR.compile(jsonInterface($)); 
 *            return true;
 *        } catch (JSON.Error e) {
 *            $.logError(e);
 *            return false;
 *        }
 *    }
 *    public boolean irtd2Identify (Actor $) {
 *        $.identity = Simple.password(10);
 *        $.rights = "";
 *        return true;
 *    }
 *    public Object jsonRegular (Actor $) {
 *        return new JSONR (_interface, 1, 2);
 *    }
 *    public void jsonApplication (Actor $) {
 *        $.jsonResponse(200);
 *    }
 *    public void httpResource (Actor $) {
 *        $.jsonResponse(200, "{\"hello\": \"world!\"}");
 *    }
 *    public void httpContinue (Actor $) {
 *        $.httpResponse(400);
 *    }
 *}</pre>
 *
 * <p>... <code>web.xml</code> ...</p>
 *
 *<pre>{
 *  "test": true;
 *  "functions": {
 *    "\/hello-world": "org.less4j.tests.HelloWorld"
 *  }
 *}</pre>
 * 
 * <p>...</p>
 *
 * <p><b>Copyright</b> &copy; 2006-2007 Laurent A.V. Szyster</p>
 *
 */
public interface Function {
    /**
     * The only instance of a <code>Function</code>.
     */
    public static Function singleton = null;
    /**
     * Returns a JSONR string describing this <code>Function</code> interface. 
     */
    public String jsonInterface (Actor $);
    /**
     * <p>Test wether this controller's configuration actually supports 
     * this <code>Function</code> at runtime.</p>
     * 
     * <h4>Synopsis</h4>
     * 
     * <p>Functions that extend the namespace of less4j's configuration must 
     * overload this method.</p>
     * 
     * <pre>public boolean less4jConfigure (Actor $) {
     *    if (super.less4jConfigure($)) {
     *        // configure your extension here
     *        return true;
     *    } else
     *        return false;
     *}</pre>
     * 
     * @return true if the configuration was successfull, false otherwise
     */
    public boolean less4jConfigure (Actor $);
    /**
     * Identify the requester, then return true to digest a new IRTD2 Cookie 
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
     * @return true if the request was identified, false otherwise
     */
    public boolean irtd2Identify (Actor $);
    /**
     * Complete requests not handled by <code>httpResource</code> or
     * <code>jsonApplication</code>. 
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
    public void httpContinue (Actor $, String method, String contentType);
    /**
     * <p>Reply to idempotent HTTP requests not handled by a configured
     * <code>Function</code> or this controller.</p>
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
    public void httpResource (Actor $);
    /**
     * <p>Returns a <code>JSON</code> or <code>JSONR</code> interpreter
     * to validate a GET request's query string or a POSTed JSON request 
     * body.</p> 
     * 
     * <h4>Synopsis</h4>
     * 
     *<pre>public Object jsonRegular (Actor $) {
     *    return new JSON();
     *}</pre>
     *
     * <p>...</p>
     * 
     * @param $ the Actor's state
     * @return a <code>JSON</code> or <code>JSONR</code> interpreter
     */
    public Object jsonRegular (Actor $);
    /**
     * Control an audited interaction between an identified user and a 
     * JSON application.
     * 
     * <h4>Synopsis</h4>
     * 
     *<pre>public void jsonApplication (Actor $) {
     *    $.jsonResponse(501); // Not implemented
     *}</pre>
     *
     * <p>...</p>
     * 
     * @param $ the Actor's state
     */
    public void jsonApplication (Actor $);
};

