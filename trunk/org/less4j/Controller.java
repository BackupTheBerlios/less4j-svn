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
 * <h3>Synopsis</h3>
 * 
 * <p>...</p>
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
        // TODO: move to JSON configuration ...
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
        if (!$.testConfiguration()) {
            throw new ServletException(
                "less4j configuration failed at runtime"
            );
        }
    }
    
    // TODO: add common J2EE acts for SQL database administration and
    //       simple LDAP access, crafted so that a JSONR model can be
    //       scaffolded as:
    //
    //       GET  /database                   -> DATABASE view
    //       POST /database                   -> SQL CREATE or UPDATE TABLE
    //       GET  /database/table             -> TABLE view 
    //       POST /database/table             -> SQL INSERT or UPDATE many 
    //       GET  /database/table?insert&...  -> INSERT one
    //       GET  /database/table?update&...  -> UPDATE one
    //       GET  /database/table?select&...  -> SELECT where
    
    /*
     * We can have a JSON object posted, for instance a signed context
     * and its digest followed by a collection of actions to control:
     * 
     *    [{"signed":null},"digest",[{"insert":null}]]
     * 
     * or a JSON object mapped from the URL query string
     * 
     *    {"insert": null, ...}
     *    
     * plus Cookies and HTTP headers to transfer all state of the application,
     * leaving the controller stateless.
     * 
     * sqlDatabaseGET();
     * sqlDatabasePOST();
     * sqlGET("table");
     * sqlPOST("table", model);
     * sqlInsert("table", model);
     * sqlUpdate("table", model);
     * sqlSelect("table", model);
     * 
     * This Controller should be all you ever need to scaffold an instant 
     * database application prototyped from the browser as a loose JSON model 
     * first and quickly put to test with a strict JSON Regular expression.
     * 
     * Assembly of the user interface takes place in the browser, not in Java.
     * 
     * Any other J2EE API you need to support can be placed in the Controller  
     * doGet and doPost methods, as subclass redefine how the Actor actions
     * on entreprise resources are controlled.
     * 
     * If you can't cut straight to the database or the directory, if the
     * controller must remote or message legacy beans, this is the place
     * to do it, not in the generic Actor. Because in that case, interfacing
     * with legacy *is* the application itself.
     * 
     */
}
