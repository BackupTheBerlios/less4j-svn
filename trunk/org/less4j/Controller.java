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

import java.util.HashMap;     // the one obvious way to do it right ...
import java.util.Enumeration; // ... although that may not be obvious 
                              // immediately, unless you're dutch. 

import javax.servlet.ServletConfig;
import javax.servlet.ServletException; 
import javax.servlet.http.HttpServlet;

/**
 * <p>Applications derive their action servlets from this Controller
 * class, overriding the methods <code>doGet</code> and <code>doPost</code>
 * to implement control of a web 2.0 view of SQL relational database(s) 
 * and LDAP directory(ies), using one <code>Actor</code> instances 
 * for each request and response.</p>
 * 
 * <h3>Usage</h3>
 * 
 * <h4>REST</h4>
 * 
 * <p>...</p>
 * 
 * <blockquote>
 * <pre>class World extends Controller {
 *    public void doGet(HttpServlertRequest req, HttpServlertResponse res) { 
 *        (new Actor(getConfiguration()), req, res)).rest200Ok(
 *            "<World/>", 600
 *            );
 *    }
 *}</pre>
 * </blockquote>
 * 
 * <h4>JSON</h4>
 * 
 * <p>As part of a Web 2.0 framework, less4j controllers expect one kind
 * of REST request and three kinds of JSON requests: for a resource, an
 * action or a transaction.</p>
 * 
 * <p>The request for globals of the JavaScript client state and the request
 * for stateless interactions with the server use the GET method of HTTP:
 * 
 * <blockquote>
 * <pre>class HelloWorld extends Controller {
 *    public void doGet(HttpServlertRequest req, HttpServlertResponse res) {
 *        Actor $ = new Actor(getConfiguration()), req, res));
 *        <strong>if ($.httpIdempotent()) {
 *            if $.restMatch($.identity) {
 *                $.rest200Ok("{}", "application/json", 600);
 *            else {
 *                </strong>$.rest200Ok("<World/>", 600);<strong>
 *            }
 *        } else if $.hasAction("hello") {
 *            $.json200Ok(new Object[]{"hello", $.getAction("hello"})
 *        } else {
 *            $.rest302Redirect();
 *        }</strong>
 *    }
 *}</pre>
 * </blockquote>
 * 
 * Application controllers don't need and should not handle more than two 
 * HTTP response states: 200 Ok and 302 Redirect. Failure is not an option
 * on this side.</p>
 * 
 * <h4>d'irtd</h4>
 * 
 * <p>That bizarre acronym stands for: Digest Identity Roles Time Digested,
 * a simple relation to chain authentic signatures of an original digest
 * back to their source, auditing user interactions and effectively tracking 
 * impersonation fraud attempts:
 * 
 * <blockquote>
 * <pre>class SafeHelloWorld extends Controller {
 *    public void doGet(HttpServlertRequest req, HttpServlertResponse res) {
 *        Actor $ = new Actor(getConfiguration()), req, res));
 *        <strong>if $.notAuthorized() {
 *            $.rest302Redirect("?login")
 *        } else</strong> if ($.httpIdempotent()) {
 *            $.rest200Ok("{}", "application/json", 600);
 *        } else if $.hasAction("hello") {
 *            $.json200Ok(new Object[]{"hello", $.getAction("hello"})
 *        } else if $.hasAction("login") {
 *            $.rest200Ok()
 *        } else {
 *            $.rest302Redirect();
 *        }
 *    }
 *}</pre>
 * </blockquote>

 * ...</p>
 * 
 * <p>The POST method is used to commit a controlled transaction:</p>
 * 
 * @author Laurent Szyster
 * @version 0.1.0
 *
 */
public class Controller extends HttpServlet {
    
    static final long serialVersionUID = 0; // ? long hash of the full name
    
    private HashMap configuration = new HashMap ();
        
    public void setConfiguration(ServletConfig config) {
        String property;
        Enumeration properties = getServletConfig().getInitParameterNames ();
        while (properties.hasMoreElements()) {
            property = (String) properties.nextElement();
            if (property.startsWith(Actor.less4j)) {
                configuration.put(
                    property, config.getInitParameter(property)
                    );
            }
        }
    }
    
    /**
     * Clone the controller's configuration HashMap.
     * 
     * @return a new Actor, a thread-safe place for instance variables
     */
    public HashMap getConfiguration() {
        synchronized (configuration) {
            return (HashMap) configuration.clone ();
        }
    }

    /**
     * Initialize a servlet controller: extract the less4j properties,
     * test the resource configured with a new Actor and raise a
     * ServletException if the configuration is incomplete or fails
     * the tests.
     *  
     */
    public void init (ServletConfig config) throws ServletException {
        super.init(config);
        setConfiguration(config);
        Actor $ = new Actor (configuration);
        if (!$.testConfiguration(this)) {
            throw new ServletException(
                "less4j configuration failed at runtime"
            );
        }
    }
    
    /* here comes your application */
    
}
