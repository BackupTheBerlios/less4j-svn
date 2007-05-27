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
 * {"function": ...}</pre>
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
    // static
    static final long serialVersionUID = 0L; // TODO: regenerate
    // statefull once
    private Scriptable _scope = null;
    private JSON.Object _functions = null;
    public boolean less4jConfigure (Actor $) {
        if (super.less4jConfigure($) && sqlOpen($)) try {
            // fetch the function name and source for this servlet 
            // from its resources.
            String resource = Simple.read(
                getServletContext().getResource("/WEB-INF/lib/functions.js")
                );
            if (_functions == null)
                // if no functions available, stop the configuration and 
                // continue in test or fail in production.
                return $.test;
            // compile the functions selected in a shared scope 
            Context cx = Context.enter();
            try {
                _scope = cx.initStandardObjects(null, true);
                Iterator names = _functions.keySet().iterator();
                String name;
                while (names.hasNext()) {
                    name = (String) names.next();
                    _functions.put(name, cx.compileFunction(
                        _scope, _functions.S(name), name, 1, null 
                        ));
                    _scope.put(name, (
                        org.mozilla.javascript.Function
                        ) _functions.get(name), null);
                } // note: the sources are compiled in place.
            } finally {
                Context.exit();
            }
            return true;
        } catch (Exception e) {
            $.logError(e);
        } finally {
            $.sqlClose();
        }
        return false;
    }
    // stateless thereafter ... 
    protected static class Application implements Controller.Function {
        private Scriptable _scope;
        private org.mozilla.javascript.Function _function;
        public Application (
            Scriptable scope, org.mozilla.javascript.Function fun
            ) {
            _scope = scope; _function = fun;
        }
        public boolean call (Actor $) {
            Context cx = Context.enter();
            try {
                Scriptable scope = cx.newObject(_scope); // copy a scope
                scope.setPrototype(_scope);
                scope.setParentScope(null);
                _function.call(cx, scope, scope, new Object[]{$});
                return true;
            } catch (Exception e) {
                $.logError(e);
                return false;
            } finally {
                Context.exit();
            }
        }
    } 
}