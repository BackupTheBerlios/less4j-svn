/* Copyright (C) 2006-2007 Laurent A.V. Szyster

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

package org.less4j;

import org.mozilla.javascript.*; // More JavaScript Applications!

/**
 * <p>This application of less4j provides a Web 2.0 interface to Mozilla's 
 * JavaScript interpreter and allow developers to script a practical server
 * API in the same language as the one use to program the client's user
 * interface.</p>
 * 
 * <h3>Synopsis</h3>
 * 
 * <p>To apply org.less4j.Script</p>
 * 
 * <pre>importPackage(org.less4j);
 * 
 *function irtd2Identify($) {
 *    $.identity = Simple.password(10);
 *    return true;
 *}
 * 
 *function jsonApplication($) {
 *    $.json.put("hello", $.json.S("who", $.identity) + "!");
 *    $.jsonResponse(200);
 *}</pre>
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
 * <p><b>Copyright</b> &copy; 2006-2007 Laurent A.V. Szyster</p>
 */
public class Script extends Controller {

    static final long serialVersionUID = 0L; // TODO: regenerate

    private static String _configurationPattern = ("{" +
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
           
    public JSONR.Type configurationPattern () throws JSON.Error {
        return JSONR.compile(_configurationPattern);
    }
    
    private static final String _scripts = "scripts";
    private static final String _scriptScope = "scriptScope";
    
    /**
     * Load the scripts configured from the servlet's context, 
     * 
     * @param $
     * @return
     * @throws Exception
     */
    public boolean scriptLoad (Actor $) throws Exception {
        ScriptableObject scope;
        Context cx = Context.enter();
        try {
            scope = new ImporterTopLevel(cx, !$.test); // sealed in production!
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
        Context cx, Scriptable scriptScope, String name
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
        Actor $, Context cx, Scriptable scriptScope, Function function
        ) 
    throws Exception {
        Scriptable scope = cx.newObject(scriptScope);
        scope.setPrototype(scriptScope);
        scope.setParentScope(null);
        return ((Function) function).call(
            cx, scope, scope, new Object[]{$}
            );
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
                Context cx = Context.enter();
                try {
                    Function function = scriptFunction(
                        cx, scriptScope, _less4jConfigure
                        );
                    if (function == null)
                        return true;
                    else {
                        return ((Boolean) Context.jsToJava(scriptCall(
                            $, cx, scriptScope, function
                            ), Boolean.class)).booleanValue();
                    }
                } finally {
                    Context.exit();
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
        Context cx = Context.enter();
        try {
            Function function = scriptFunction(
                cx, scriptScope, _irtd2Identify
                );
            if (function == null) {
                return super.irtd2Identify($);
            } else try {
                return ((Boolean) Context.jsToJava(scriptCall(
                        $, cx, scriptScope, function
                        ), Boolean.class)).booleanValue();
            } catch (Exception e) {
                $.logError(e);
                return false;
            }
        } finally {
            Context.exit();
        }
    }
    
    private static final String _httpContinue = "httpContinue";
    
    public void httpContinue (Actor $) {
        Scriptable scriptScope = (Scriptable) 
            $.configuration.get(_scriptScope);
        Context cx = Context.enter();
        try {
            Function function = scriptFunction(
                cx, scriptScope, _httpContinue
                );
            if (function == null) {
                super.httpContinue($);
            } else try {
                scriptCall($, cx, scriptScope, function);
            } catch (Exception e) {
                $.logError(e);
                $.httpError(500); // Server Error
            }
        } finally {
            Context.exit();
        }
    }
    
    private static final String _httpResource = "httpResource";
    
    public void httpResource (Actor $) {
        Scriptable scriptScope = (Scriptable) 
            $.configuration.get(_scriptScope);
        Context cx = Context.enter();
        try {
            Function function = scriptFunction(
                cx, scriptScope, _httpResource
                );
            if (function == null) {
                super.httpResource($);
            } else try {
                scriptCall($, cx, scriptScope, function);
            } catch (Exception e) {
                $.logError(e);
                $.httpError(500); // Server Error
            }
        } finally {
            Context.exit();
        }
    }
    
    private static final String _jsonRegular = "jsonRegular";
    
    public JSONR.Type jsonRegular (Actor $) {
        Scriptable scriptScope = (Scriptable) 
            $.configuration.get(_scriptScope);
        Context.enter();
        try {
            Object model = scriptScope.get(_jsonRegular, scriptScope);
            if (!(
                model == null || model == Scriptable.NOT_FOUND
                ) && model instanceof JSONR.Type) {
                return (JSONR.Type) model;
            } else 
                return super.jsonRegular($);
        } finally {
            Context.exit();
        }
    }

    private static final String _jsonApplication = "jsonApplication";
    
    public void jsonApplication (Actor $) {
        Scriptable scriptScope = (Scriptable) 
            $.configuration.get(_scriptScope);
        Context cx = Context.enter();
        try {
            Function function = scriptFunction(
                cx, scriptScope, _jsonApplication
                );
            if (function == null) {
                super.jsonApplication($);
            } else try {
                scriptCall($, cx, scriptScope, function);
            } catch (Exception e) {
                $.logError(e);
                $.json.put("exception", e.getMessage());
                $.jsonResponse(500); // Server Error
            }
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