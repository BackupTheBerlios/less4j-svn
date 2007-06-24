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

import org.mozilla.javascript.*; // More JavaScript Applications!

/**
 * <p>An application of less4j and to Mozilla's JavaScript interpreter that
 * allow developers to script servers and clients in the same language.</p>
 * 
 * <h3>Synopsis</h3>
 * 
 * <p>A typical greenfield development configuration for a <code>Script</code> 
 * servlet is:</p>
 * 
 * <pre>{"test": true, "scripts": ["functions.js", "hello-world.js"]}</pre>
 * 
 * <p>Scripts loaded by servlets of this controller class can supply their
 * own JavaScript implementation of the <code>Controller</code> interfaces.
 * The purpose is to prototype new Java controller in JavaScript first, 
 * possibly directly from a web prompt.</p>
 * 
 * <p>The less4j framework comes with a <code>functions.js</code> script
 * that emulates the functional <code>Controller</code> dispatcher (actually
 * it prototyped it).</p>
 *  
 * <p>And here is the usual suspect first application:</p>
 * 
 * <pre>functions["/hello-world"] = jsonRegularFunction (
 *    function ($) {
 *        return 200;
 *    }, 
 *    '{"hello": "world!"}', 1, 2
 *    );</pre>
 * 
 * <p>...</p>
 * 
 * <h3>Applications</h3>
 * 
 * <p>This controller can be used in two different ways to cover a wide
 * range of applications.</p>
 * 
 * <p>First as-is, configured with SQL, LDAP, a Regular JSON Expression 
 * to validate its input and a set of interpreted JavaScript functions.
 * It mainly does the Right Thing for many entreprise applications, leaving
 * only the domain specific functions to be scripted.</p>
 * 
 * <p>With <code>Script</code>, "everything should just work" and every parts 
 * of the controller are tested at runtime. Configuration is validated and 
 * tested while scripts are compiled and interpreted.</p>
 * 
 * <p>Then, once applications of less4j's <code>Script</code> meet their 
 * users' requirements, this class provides a base to migrate to Java
 * the functions that cannot be entrusted to Rhino.</p>
 * 
 * <p>Note however that most of those functions are best optimized alone
 * first and can then be applied in JavaScript with at a marginal cost.
 * To override a <code>Script</code> controller methods will usually gain 
 * insignificant performances in comparison to the loss of one or more 
 * articulation for their applications.</p>
 * 
 * <p><b>Copyright</b> &copy; 2006-2007 Laurent A.V. Szyster</p>
 */
public class Script extends Controller {

    static final long serialVersionUID = 0L; // TODO: regenerate

    private static String _configurationPattern = ("{" +
        "\"test\": false," + // optional boolean
        "\"scripts\": [\"^.+?[.]js$\"]," +
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
     * Returns a <code>String</code> of the JSONR model that will be used
     * to validate this controller's JSON <code>configuration</code>.
     * 
     * <h3>Synopsis</h3>
     * 
     * <p>Override this method in a derived class of <code>Script</code>
     * to restrict and/or widden the model of your application's 
     * configuration. The default model adds an array of <code>scripts</code>
     * filename in the original <code>Controller</code> configuration 
     * namespace and disable the ability to register Controller.Function
     * classes.</p>
     * 
     * <pre>{
     *    "test": false, 
     *    "scripts": ["^.+?[.]js$"],
     *    "irtd2Salts": ["^...........*$"], 
     *    "irtd2Timeout": null,
     *    "postBytes": null,
     *    "jsonContainers": null,
     *    "jsonIterations": null,
     *    "jsonRegular": null,
     *    "jdbcDriver": null,
     *    "jdbcURL": null,
     *    "jdbcUsername": null,
     *    "jdbcPassword": null,
     *    "j2eeDataSource": null,
     *    "ldapURL": null,
     *    "ldapUsername": null,
     *    "ldapPassword": null
     *    }</pre>
     *     
     * @return a JSONR <code>String</code>
     * @throws <code>JSON.Error</code>
     */
    public JSONR.Type configurationPattern () throws JSON.Error {
        return JSONR.compile(_configurationPattern);
    }
    
    private static final String _scripts = "scripts";
    private static final String _scriptScope = "scriptScope";
    
    /**
     * Try to load the scripts configured from the servlet's context.
     * 
     * @param $ the <code>Actor</code> that loads the script
     * @return <code>true</code> if the script was
     * @throws Exception an unchecked exception.
     */
    protected boolean scriptLoad (Actor $) throws Exception {
        ScriptableObject scope;
        Context cx = Context.enter();
        try {
            scope = new ImporterTopLevel(cx, !$.test);
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
            org.mozilla.javascript.Function function = scriptFunction(
                cx, scope, _less4jConfigure
                );
            if (function != null) {
                return ((Boolean) Context.jsToJava(scriptCall(
                    $, cx, scope, function
                    ), Boolean.class)).booleanValue();
            } else
                return true;
        } finally {
            Context.exit();
        }
    }
    
    protected static org.mozilla.javascript.Function scriptFunction (
        Context cx, Scriptable scriptScope, String name
        ) {
        Object function = scriptScope.get(name, scriptScope);
        if (!(
            function == null || function == Scriptable.NOT_FOUND
            ) && function instanceof org.mozilla.javascript.Function) {
            return (org.mozilla.javascript.Function) function;
        } else 
            return null;
    }
    
    protected Object scriptCall (
        Actor $, Context cx, Scriptable scriptScope, 
        org.mozilla.javascript.Function function
        ) 
    throws Exception {
        Scriptable scope = cx.newObject(scriptScope);
        scope.setPrototype(scriptScope);
        scope.setParentScope(null);
        return (function).call(
            cx, scope, (Scriptable) Context.javaToJS(this, scope), 
            new Object[]{$}
            );
    }
    
    /**
     * Try to reload the scripts configured from the servlet's context,
     * seal it in staging and production (ie: if <code>$.test</code> 
     * is false).
     * 
     * @param $
     */
    public void scriptReload (Actor $) throws Exception {
        JSON.Object newConfiguration = new JSON.Object();
        newConfiguration.putAll($.configuration);
        $.configuration = newConfiguration;
        scriptLoad($);
        setConfiguration($.configuration);
    }

    protected static final String _less4jConfigure = "less4jConfigure"; 
    
    /**
     * Apply the inherited <code>Controller</code> configuration, then maybe 
     * try to evaluate the JavaScript sources found in this servlet's
     * <code>/WEB-INF/functions.js</code> and return true or log an error and 
     * return false.
     * 
     * <h4>Synopsis</h4>
     * 
     * <pre>var less4jConfigure = function ($) {
     *    $.configuration.put("jsonBytes", 65355);
     *}</pre>
     *
     * <p>The practical purpose of this interface is to override or supply
     * a configuration in JavaScript, for instance to load more options from
     * the network or force a limit to posted data.</p>
     * 
     * <p>Note that the default <code>Controller</code> configuration test
     * cannot be restricted or extended in JavaScript and they should not.</p>
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
    
    protected static final String _irtd2Identify = "irtd2Identify";
    
    /**
     * Delegate identification to a JavaScript function named "irtd2Identify",
     * or reply with an HTTP error 404 if no such function exists or if
     * it raised an exception.
     * 
     * <h4>Synopsis</h4>
     * 
     * <pre>var irtd2Identify = function ($) {
     *    $.identity = Simple.password(20);
     *    return true; 
     *}</pre>
     */
    public boolean irtd2Identify (Actor $) {
        Scriptable scriptScope = (Scriptable) 
            $.configuration.get(_scriptScope);
        Context cx = Context.enter();
        try {
            org.mozilla.javascript.Function function = scriptFunction(
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
                return super.irtd2Identify($);
            }
        } finally {
            Context.exit();
        }
    }
    
    protected static final String _httpContinue = "httpContinue";
    
    /**
     * ...
     *
     */
    public void httpContinue (Actor $) {
        Scriptable scriptScope = (Scriptable) 
            $.configuration.get(_scriptScope);
        Context cx = Context.enter();
        try {
            org.mozilla.javascript.Function function = scriptFunction(
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
    
    protected static final String _httpResource = "httpResource";
    
    /**
     * ...
     * 
     */
    public void httpResource (Actor $) {
        Scriptable scriptScope = (Scriptable) 
            $.configuration.get(_scriptScope);
        Context cx = Context.enter();
        try {
            org.mozilla.javascript.Function function = scriptFunction(
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
    
    protected static final String _jsonRegular = "jsonRegular";
    
    /**
     * If one has been declared try to call the JavaScript function 
     * named "jsonRegular" in this controller's scope and return the
     * <code>JSON</code> or <code>JSONR</code> interpreter used to parse
     * and validate requests, or return the configured configured one
     * if "jsonRegular" is not implemented by the scripts.
     * 
     * <h3>Synopsis</h3>
     * 
     * <p>In one of the configured scripts, define:</p>
     * 
     * <pre>importPackage(org.less4j);
     *var model = JSONR.compile ('{"hello": "world!"}');
     *function jsonRegular ($) {
     *    return new JSONR(19, 1, 1, model); // strictly!
     *}</pre>
     * 
     * <p>This interface enables application scripts to validate
     * requests against different models before they are dispatched
     * to a distinct function.<p>
     * 
     */
    public Object jsonRegular (Actor $) {
        Scriptable scriptScope = (Scriptable) 
            $.configuration.get(_scriptScope);
        Context cx = Context.enter();
        try {
            org.mozilla.javascript.Function function = scriptFunction(
                cx, scriptScope, _jsonRegular
                );
            if (function == null) {
                return super.jsonRegular($);
            } else try {
                return Context.jsToJava(scriptCall(
                    $, cx, scriptScope, function
                    ), Object.class);
            } catch (Exception e) {
                $.logError(e);
                return null;
            }
        } finally {
            Context.exit();
        }
    }

    protected static final String _jsonApplication = "jsonApplication";
    
    /**
     * If one has been declared try to call the JavaScript function 
     * named "jsonApplication" in this controller's scope and let it
     * complete the HTTP response, or reply by an error 500 if an
     * exception was throwed or 501 if "jsonApplication" is not 
     * implemented by the scripts configured.
     * 
     * <h3>Synopsis</h3>
     * 
     * <p>In one of the configured scripts, define:</p>
     * 
     * <pre>function jsonApplication ($) {
     *    $.jsonResponse (200, "Hello World!");
     *}</pre>
     * 
     */
    public void jsonApplication (Actor $) {
        Scriptable scriptScope = (Scriptable) 
            $.configuration.get(_scriptScope);
        Context cx = Context.enter();
        try {
            org.mozilla.javascript.Function function = scriptFunction(
                cx, scriptScope, _jsonApplication
                );
            if (function == null) {
                super.jsonApplication($);
            } else try {
                scriptCall($, cx, scriptScope, function);
            } catch (Exception e) {
                $.logError(e);
                $.jsonResponse(500, e.getMessage()); // Server Error
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