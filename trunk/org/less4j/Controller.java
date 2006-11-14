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
 * <p>A stateless servlet to configure, test and apply the <code>Actor</code>
 * programming intefaces, the base class from which to derive RESTfull
 * application resource controllers (aka SOA).</p>
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
 *        <strong>(new Actor(getConfiguration()), req, res)).rest200Ok(
 *            "&lt;World/&gt;", 600
 *            );</strong>
 *    }
 *}</pre>
 * </blockquote>
 * 
 * <h4>d'irtd</h4>
 * 
 * <p>That bizarre acronym stands for: Digest Identity Roles Time Digested,
 * a simple relation to chain authentic signatures of an original digest
 * back to their source, auditing user interactions and effectively tracking 
 * impersonation fraud attempts.</p>
 * 
 * <p>Here is a simplistic Guest resource, that identify an "anonymous" user
 * and grants him the rights of a "guest":
 * 
 * <blockquote>
 * <pre>class Guest extends Controller {
 *    public void doGet(HttpServlertRequest req, HttpServlertResponse res) {
 *        Actor $ = new Actor(getConfiguration()), req, res));
 *        <strong>if ($.notAuthorized()) {
 *            $.authorize("", "guest");
 *        } else {
 *            $.authorize("guest");
 *        }</strong>
 *        $.rest200Ok("&lt;World/&gt;", 600);
 *    }
 *}</pre>
 * </blockquote>
 *
 * Note that HTTP cookies are used, must be supported by the user agent and 
 * that d'irtd digests are bound to a domain, maybe to a path (by default
 * cookies are digested for the "/" root path).</p>
 * 
 * <h4>JSON</h4>
 * 
 * <p>The request for globals of the JavaScript client state and the request
 * for stateless interactions with the server use the GET method of HTTP:
 * 
 * <blockquote>
 * <pre>class Hello extends Controller {
 *    public void doGet(HttpServlertRequest req, HttpServlertResponse res) {
 *        Actor $ = new Actor(getConfiguration()), req, res));
 *        <strong>if ($.noAuthorized("guest")) {
 *            $.rest302Redirect("/Guest");
 *        } else if ($.httpIdempotent()) {
 *            $.rest200Ok("{method: \"hello\"}", "application/json", 600);
 *        } else (if $.httpActions() && $.hasAction("hello")) {
 *            $.json200Ok(new Object[]{"hello", $.identity});
 *        } else {
 *            $.rest302Redirect();
 *        }</strong>
 *    }
 *    public void doPost(HttpServlertRequest req, HttpServlertResponse res) {
 *        Actor $ = new Actor(getConfiguration()), req, res));
 *        <strong>if ($.jsonPOST()) {
 *            $.json200Ok();
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
 * <p>The POST method implemented in the example echoes the JSON object
 * posted, as a simple test of the full stack between a browser and the
 * controller. Real applications use POST to commit transactions which don't
 * fit a URL query string (practically, anything bigger than 2048 bytes).</p>
 * 
 * <p><b>Copyright</b> &copy; 2006 Laurent A.V. Szyster</p>
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
    
}
