package org.less4j.applications;

import org.less4j.Controller;
import org.less4j.Actor;
import org.less4j.JSON;
import org.less4j.Simple;

import org.mozilla.javascript.*;

import java.util.Iterator;

/**
 * A controller for JavaScript actions, from development to production.
 * 
 * <p>Servlet instances of <code>Script</code> ready to run scripts
 * loaded from the configured SQL connection in a production environment.</p>
 * 
 * <p>Literraly, <code>org.less4j.Script</code> is "More JavaScript 
 * Applications". There is a least-resistance path in programming the 
 * Web 2.0 client and server sides with the same language.</p>
 * 
 * <h3>Synopsis</h3>
 * 
 * <pre>GET /script?expression=...</pre>
 * Accept: text/javascript; charset=UTF-8
 * </pre>
 * 
 * <pre>POST /script</pre>
 * Accept: text/javascript; charset=UTF-8
 * Content-Type: application/json; charset=UTF-8
 * 
 * {"expression": ...}</pre>
 * 
 * <pre>GET /script/function?...</pre>
 * Accept: text/javascript; charset=UTF-8
 * </pre>
 * 
 * <pre>POST /script/function
 * Accept: text/javascript; charset=UTF-8
 * Content-Type: application/json; charset=UTF-8
 * 
 * {...}</pre>
 * 
 * <h3>Configuration</h3>
 * 
 * <pre>{"test": true}</pre>
 * 
 * <pre>GET /script?eval</pre>
 * 
 * <pre>"function jsonApplication($) {
 *     $.jsonResponse(200);
 *}"</pre>
 * 
 * <pre>GET fun?...</pre>
 * 
 * <pre>POST fun
 *Host: domain
 *Content-Type: application/json; charset=UTF-8
 *
 *{}</pre>
 * 
 * <h3>Applications</h3>
 * 
 * <p>This controller can be used in two different ways to cover a wide
 * range of applications.</p>
 * 
 * <p>First as-is, configured with SQL, LDAP, a Regular JSON Expression 
 * to validate its input and a set of interpreted JavaScript functions.
 * It mainly does the Right Thing for many entreprise applications, leaving
 * only the domain specific functions ... to be scripted.</p>
 * 
 * <p>With <code>Script</code>, "everything should just work" and every parts 
 * of the controller are tested at runtime. Configuration is validated and 
 * tested while scripts are compiled and interpreted.</p>
 * 
 * <p>To introduce execution faults in the application you can 
 * derive a controller from <code>Script</code> and reimplement some of
 * its interfaces, for instance to provide another script loader.</p>
 * 
 * @author Laurent Szyster
 *
 */
public class Script extends Controller {

    static final long serialVersionUID = 0L; // TODO: regenerate

    public static String configurationPattern = ("{" +
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
     * 
     * @param $
     * @return
     * @throws Exception
     */
    public boolean scriptLoad (Actor $) throws Exception {
        String source = Simple.read(
            getServletContext().getResource("/WEB-INF/functions.js")
            ); // synchronized!
        if (source == null)
            return true;
        ScriptableObject scope;
        Context cx = Context.enter();
        try {
            scope = new ImporterTopLevel(cx, true); // sealed
            cx.evaluateString(
                scope, source, this.getClass().getName(), 1, null
                );
            $.configuration.put("scriptScope", scope);
        } finally {
            Context.exit();
        }
        Object function = scope.get("jsonApplication", scope);
        if (function instanceof org.mozilla.javascript.Function) {
            $.configuration.put("scriptApplication", function);
            return true;
        } else if (function != null) {
            $.logInfo(
                "Variable 'jsonApplication' is not a function", 
                "org.less4j.applications.Script"
                );
            return false;
        } else
            return true;
    }
    /**
     * 
     * @param $
     */
    public void scriptReload (Actor $) {
        try {
            JSON.Object newConfiguration = new JSON.Object();
            newConfiguration.putAll($.configuration);
            $.configuration = newConfiguration;
            if (scriptLoad($)) {
                $.json.put("reloaded", Boolean.TRUE);
                setConfiguration($.configuration);
            } else
                $.json.put("reloaded", Boolean.FALSE);
        } catch (Exception e) {
            if ($.test) $.logError(e);
            $.json.put("exception", e.getMessage());
        }
    }
    /**
     * 
     * @param $
     */
    public void scriptApply (Actor $, String fun) throws Exception {
        Scriptable scriptScope = (Scriptable) 
        $.configuration.get("scriptScope");
        Object function = scriptScope.get(fun, scriptScope);
        if (function == Scriptable.NOT_FOUND) {
            $.jsonResponse(501); // Not Implemented
            return;
        }
        if (function == null) {
            $.jsonResponse(400); // Bad Request
            return;
        }
        Context cx = Context.enter();
        try {
            Scriptable scope = cx.newObject(scriptScope);
            scope.setPrototype(scriptScope);
            scope.setParentScope(null);
            ((org.mozilla.javascript.Function) function).call(
                cx, scope, scope, new Object[]{$}
                );
        } catch (Exception e) {
            $.logError(e);
            $.json.put("exception", e.getMessage());
            $.jsonResponse(500); // Server Error
        } finally {
            Context.exit();
        }
    }
    /**
     * Apply the inherited <code>Controller</code> configuration, then maybe 
     * try to evaluate the JavaScript sources found in this servlet's
     * <code>/WEB-INF/functions.js</code> and return true or log an error and 
     * return false.
     * 
     * @param $ the <code>Actor</code> used to configure this controller.
     */
    public boolean less4jConfigure (Actor $) {
        if (super.less4jConfigure($)) try {
            return scriptLoad($);
        } catch (Exception e) {
            $.logError(e);
        }
        return false;
    }
    /**
     * Dispatch requests based on their path between arbitrary expression 
     * evaluation and the application of configured functions.
     * 
     * Try to evaluate requests such as 
     * 
     * <pre>?expression=...</pre>
     * 
     * or try to apply functions for requests like
     * 
     * <pre>function?...</pre>
     * 
     * Otherwise reply with an HTTP 400 Bad Request and a JSON body.
     * 
     * @param $ the request's <code>Actor</code> controlled.
     */
    public void jsonApplication (Actor $) {
        Scriptable scriptScope = (Scriptable) 
            $.configuration.get("scriptScope");
        Object function = scriptScope.get("jsonApplication", scriptScope);
        if (function == Scriptable.NOT_FOUND) {
            $.jsonResponse(501); // Not Implemented
            return;
        }
        if (function == null) {
            $.jsonResponse(400); // Bad Request
            return;
        }
        Context cx = Context.enter();
        try {
            Scriptable scope = cx.newObject(scriptScope);
            scope.setPrototype(scriptScope);
            scope.setParentScope(null);
            ((org.mozilla.javascript.Function) function).call(
                cx, scope, scope, new Object[]{$}
                );
        } catch (Exception e) {
            $.logError(e);
            $.json.put("exception", e.getMessage());
            $.jsonResponse(500); // Server Error
        } finally {
            Context.exit();
        }
    }
}

/* Note about this implementation
 * 
 * The purpose of org.less4j.applications.Script is to develop rapidely.
 * 
 * There is ample room for optimization here, using two axis: bytecode
 * compilation of JavaScript sources and implementations of Scriptable
 * interfaces for each Java class applied. However, it may be easier
 * to actually cut and paste a working but slow JavaScript application
 * in a new Controller in Java: if you sticked to less4j's API, its name 
 * conventions and a java-like syntax then your prototype might compile
 * without changes.
 * 
 */