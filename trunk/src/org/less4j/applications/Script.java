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

    // statefull
    /**
     * 
     * @param $
     * @return
     * @throws Exception
     */
    public boolean scriptLoad (Actor $) throws Exception {
        String source = Simple.read(
            getServletContext().getResource("/WEB-INF/lib/functions.js")
            ); // synchronized!
        if (source == null)
            return false;
        Scriptable scope;
        Context cx = Context.enter();
        try {
            scope = cx.initStandardObjects(null, true);
            cx.evaluateString(
                scope, source, this.getClass().getName(), 1, null
                );
        } finally {
            Context.exit();
        }
        ScriptableObject functions = (ScriptableObject) scope.get(
            "less4j", scope
            );
        // map functions found in the object named less4j, if any or throw up.
        if (functions == Scriptable.NOT_FOUND)
            if ($.test) $.logInfo(
                "Object 'less4j' not found in JavaScript sources.",
                "WARNING"
                );
        JSON.Object funs = new JSON.Object(); 
        Iterator names = Simple.iterator(functions.getIds());
        String name; Object fun;
        while (names.hasNext()) {
            name = (String) names.next();
            fun = functions.get(name, scope);
            if (fun instanceof org.mozilla.javascript.Function)
                funs.put(name, fun);
            else if ($.test) $.logInfo(
                "Property '" + name + "' of 'less4j' not a function", 
                "WARNING"
                );
        }
        $.configuration.put("scriptScope", scope);
        $.configuration.put("scriptFunctions", funs);
        return true;
    }
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
    public static void scriptEvaluate (Actor $) {
        Scriptable scriptScope = (Scriptable) 
            $.configuration.get("scriptScope");
        Context cx = Context.enter();
        try {
            Scriptable scope = cx.newObject(scriptScope);
            scope.setPrototype(scriptScope);
            scope.setParentScope(null);
            $.json.put("result", cx.evaluateString(
                scope, $.json.S("expr"), "<cmd>", 1, null
                ));
        } catch (Exception e) {
            $.json.put("exception", e.getMessage());
        } finally {
            Context.exit();
        }
        $.jsonResponse(200);
    }
    /**
     * 
     * @param $
     */
    public static void scriptApply (Actor $) {
        String name = "";
        Scriptable scriptScope = (Scriptable) 
            $.configuration.get("scriptScope");
        JSON.Object scriptFunctions = $.configuration.O(
            "scriptFunctions", null // not safe, but works ,~)
            );
        Context cx = Context.enter();
        try {
            Scriptable scope = cx.newObject(scriptScope);
            scope.setPrototype(scriptScope);
            scope.setParentScope(null);
            org.mozilla.javascript.Function fun = (
                org.mozilla.javascript.Function
                ) scriptFunctions.get(name); 
            $.json.put("result", fun.call(
                cx, scope, scope, new Object[]{$}
                ));
        } catch (Exception e) {
            $.json.put("exception", e.getMessage());
        } finally {
            Context.exit();
        }
        $.jsonResponse(200);
    }
    /**
     * 
     */
    public boolean less4jConfigure (Actor $) {
        if (super.less4jConfigure($)) try {
            return scriptLoad($);
        } catch (Exception e) {
            $.logError(e);
        }
        return $.test;
    }
    /**
     * 
     */
    public void jsonApplication (Actor $) {
        if (true)
            if ($.json.containsKey("expression"))
                if ($.test)
                    scriptEvaluate($); // Available only in development ...
                else
                    $.jsonResponse(401); // ... Not Authorized in production.
            else
                $.jsonResponse(400); // ... Bad Request. 
        else
            scriptApply($);
    }
}