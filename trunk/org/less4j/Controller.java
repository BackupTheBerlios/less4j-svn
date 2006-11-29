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

//import javax.servlet.http.HttpServletRequest;
//import javax.servlet.http.HttpServletResponse;

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
    
//    static HashMap actions = new HashMap();
//    
//    static public void action (Actor $, String name) {
//        ((Actor.Action) actions.get(name)).play($);
//    }
    
    HashMap configuration = new HashMap ();
    
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

    private static final String less4jRealPath = "less4j.realpath";
    
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
        configuration.put(less4jRealPath, getServletContext().getRealPath(""));
        Actor $ = new Actor (configuration);
        if (!$.testConfiguration()) {
            throw new ServletException(
                "less4j configuration failed at runtime"
            );
        }
    }

}

/* Note about this implementation

I though about that issue a lot but the sheer weight of Java forces to
cut the cat fast and easy: all the logic belongs to the controller, it
is the application.

From a J2EE point of view, a controller could be expressed like is this 
in tight JSON and JSONR:

    "\/insert": {
        "columns": ["name", "department", "age"],
        "rows": [
            ["[a-Z\/w]{2:75}", "[A-Z]{2}", 65]
            ]
        }

A path from the servlet's context mapping to a JSONR model controlling
the rest of the interface, the state object model, types and values. 

There is also only one XML resource, although the JSON model sandwiched 
between the hypertext may be dynamically generated. The whole REST of the
state transfered is allways sent as JSON objects or arrays, and rendered
in the browser.

Eventually each controllers may have to orchestrate many different actions
to produce compound JSON resources, at once or by pieces. That can be a 
complex process but within a few pages of readable source at most. It is
therefore not enough to dedicate an Action class to each one and suffer the
pain of magic-kool-aid-drink-hang-over. 

Action dispatchers only give your application developers enough rope to hang 
themselves with the simplest configuration file update. However, handling
more than one resource by controller makes them too complicated. Since there
is allready a URL dispatch articulation at the servlet level, let's use it!

So, for each idempotent URL identified, we have

  One Java class controlling access, actions, flow and logic for one
  identified resource.
  
  One XML template, XSLT, CSS, JavaScript and dependencies controlling the 
  client's view of the application.
    
  One J2EE descriptor with one JSONR pattern for the administration of the
  application public and private interfaces.
    
Each variation from the first trio will define a new service of the 
application. Without pain, because they are decoupled with protocols
each time. This means that it's simpler to add actions, design richer 
views or tune better configurations. 

With less code for more applications.



*/