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
 * <h3>An HTTP Servlet Controller</h3>
 * 
 * <p>The less4j's Controller extends javax's HttpServlet to provide a 
 * practical API for configuration of its so typical applications:

 * <blockquote>
 * <pre>GET /resource
 * GET /resource?action
 * ...
 * GET /resource?action
 * POST /resource</pre> 
 * </blockquote>
 *  
 * a statefull transaction that starts when the user access a resource,
 * then interact with it and finally commits all interactions. It's your
 * run-of-the-mill data input screen, an airline ticketing, etc ...</p>
 * 
 * <p>Applications derive their action controllers from this servlet and 
 * override the methods <code>doGet</code> and <code>doPost</code>
 * to implement control of a web 2.0 view of SQL relational database(s) 
 * and LDAPA metadata directory(ies), using one <code>Actor</code> instances 
 * for each request and response.</p> 
 * 
 * @author Laurent Szyster
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
     * Note that this is a point of "low contention" where speed has been
     * once again traded for memory in order not to induce waite state in
     * the application and more contention.
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
            throw new ServletException("less4j configuration failed");
        }
    }
    
}
