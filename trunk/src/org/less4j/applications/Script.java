package org.less4j.applications;

import org.less4j.Controller;
import org.less4j.Actor;
import org.less4j.JSON;
import org.less4j.Simple;

import org.mozilla.javascript.*;

/**
 * The <code>org.less4j.Script</code> servlet delivers "More JavaScript 
 * Applications", from development to production.
 * 
 * <h3>Synopsis</h3>
 * 
 * <h4>Configuration</h4>
 * 
 * <pre>{"test": true}</pre>
 * 
 * <h4>Script</h4>
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
        "\"scripts\": [\"^.+?[.]js$\"]," +
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
           
    private static final String _scripts = "scripts";
    private static final String _scriptScope = "scriptScope";
    
    /**
     * 
     * @param $
     * @return
     * @throws Exception
     */
    public boolean scriptLoad (Actor $) throws Exception {
        ScriptableObject scope;
        Context cx = Context.enter();
        try {
            scope = new ImporterTopLevel(cx, true); // sealed
            JSON.Array scripts = $.configuration.A(_scripts);
            String name, source;
            for (int i=0, L=scripts.size(); i<L; i++) {
                name = scripts.S(i);
                source = Simple.read(
                    getServletContext().getResource("/WEB-INF/" + name)
                    );
                if (source == null)
                    throw new Exception ("Could not read script " + name);
                else
                    cx.evaluateString(scope, source, name, 1, null);
            }
            $.configuration.put(_scriptScope, scope);
        } finally {
            Context.exit();
        }
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
    
    private static Function scriptFunction (
        Scriptable scriptScope, String name
        ) {
        Object function = scriptScope.get(name, scriptScope);
        if (!(
            function == null || function == Scriptable.NOT_FOUND
            ) && function instanceof Function) {
            return (Function) function;
        } else 
            return null;
    }
    
    private static Object scriptCall (
        Actor $, Scriptable scriptScope, Function function
        ) 
    throws Exception {
        Object result = null;
        Context cx = Context.enter();
        try {
            Scriptable scope = cx.newObject(scriptScope);
            scope.setPrototype(scriptScope);
            scope.setParentScope(null);
            result = ((Function) function).call(
                cx, scope, scope, new Object[]{$}
                );
        } finally {
            Context.exit();
        }
        return result;
    }
    
    private static final String _less4jConfigure = "_less4jConfigure"; 
    
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
            if (scriptLoad($)) {
                Scriptable scriptScope = (Scriptable) 
                    $.configuration.get(_scriptScope);
                Function function = scriptFunction(
                    scriptScope, _less4jConfigure
                    );
                if (function == null)
                    return true;
                else {
                    Object result = scriptCall($, scriptScope, function);
                    // TODO: unwrap and test
                    return true;
                }
            } else
                return false;
        } catch (Exception e) {
            $.logError(e);
        }
        return false;
    }
    
    private static final String _irtd2Identify = "irtd2Identify";
    
    public boolean irtd2Identify (Actor $) {
        Scriptable scriptScope = (Scriptable) 
            $.configuration.get(_scriptScope);
        Function function = scriptFunction(scriptScope, _irtd2Identify);
        if (function == null) {
            return false;
        } else try {
            Object result = scriptCall($, scriptScope, function);
            return true;
        } catch (Exception e) {
            $.logError(e);
            return false;
        }
    }
    
    private static final String _httpContinue = "httpContinue";
    
    public void httpContinue (Actor $) {
        Scriptable scriptScope = (Scriptable) 
            $.configuration.get(_scriptScope);
        Function function = scriptFunction(scriptScope, _httpContinue);
        if (function == null) {
            $.httpError(400); // Bad Request
        } else try {
            scriptCall($, scriptScope, function);
        } catch (Exception e) {
            $.logError(e);
            $.httpError(500); // Server Error
        }
    }
    
    private static final String _httpResource = "httpResource";
    
    public void httpResource (Actor $) {
        Scriptable scriptScope = (Scriptable) 
            $.configuration.get(_scriptScope);
        Function function = scriptFunction(scriptScope, _httpResource);
        if (function == null) {
            $.httpError(404); // Not Found
        } else try {
            scriptCall($, scriptScope, function);
        } catch (Exception e) {
            $.logError(e);
            $.httpError(500); // Server Error
        }
    }
    
    private static final String _jsonApplication = "jsonApplication";
    
    public void jsonApplication (Actor $) {
        Scriptable scriptScope = (Scriptable) 
            $.configuration.get(_scriptScope);
        Function function = scriptFunction(scriptScope, _jsonApplication);
        if (function == null) {
            $.jsonResponse(400); // Bad Request
        } else try {
            scriptCall($, scriptScope, function);
        } catch (Exception e) {
            $.logError(e);
            $.json.put("exception", e.getMessage());
            $.jsonResponse(500); // Server Error
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