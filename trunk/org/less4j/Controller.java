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

import java.sql.DriverManager;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException; 
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * <p>A stateless servlet to configure, test and apply the <code>Actor</code>
 * programming intefaces, the base class from which to derive RESTfull
 * application resource controllers (aka SOA).</p>
 * 
 * <h3>Synopsis</h3>
 * 
 * <p>This class implements the <code>doGet</code> and <code>doPost</code>
 * methods of <code>HttpServlet</code>, solving common issues for RESTful 
 * AJAX applications: a request/response state configuration, test and 
 * instanciation, user identification, requests authorization, input
 * validation and application audit trail.</p>
 * 
 * <p>It does The Right Thing for its applications, leaving to developpers
 * the profitable and creative part of the job as the implementation of one
 * to four simple methods:</p>
 * 
 * <blockquote>
 *<pre>less4jConfigure (Actor $)
 *irtd2Authorize (Actor $)
 *httpResource (Actor $)
 *jsonApplication (Actor $)</pre>
 * </blockquote>
 * 
 * <p>To quickly and safely scaffold applications from prototype to 
 * production, all those methods are allready implemented.</p>
 * 
 * <blockquote>
 * <pre>{
 *  "": { 
 *     "irtd2Timeout": 3600,
 *     "jsonBytes": 4096,
 *     "jsonContainers": 128,
 *     "jsonIterations": 4096,
 *     "jsonModel": {"irdt2": {"username": ".+", "password": null}}
 *  }
 *}</pre>
 * </blockquote>
 * 
 * ...</p>
 * 
 * <p><b>Copyright</b> &copy; 2006 Laurent A.V. Szyster</p>
 * 
 * @version 0.20
 *
 */
public class Controller extends HttpServlet {
    
    static final long serialVersionUID = 0L; // TODO: regenerate
    
    private JSON.O configuration = new JSON.O ();
    
    /**
     * Get the controller's configuration HashMap, synchronously.
     * 
     * @return a new Actor, a thread-safe place for instance variables
     */
    protected JSON.O getConfiguration() {
        synchronized (configuration) {return configuration;}
    }
    
    /**
     * Set the controller's configuration HashMap, synchronously.
     * 
     * @return a new Actor, a thread-safe place for instance variables
     */
    protected void setConfiguration(JSON.O o) {
        synchronized (configuration) {configuration = o;}
    }
    
    /**
     * Initialize a servlet controller: extract the less4j properties,
     * test the resource configured with a new Actor and call the
     * controller's <code>less4jConfigure</code> method or raise a
     * ServletException if the configuration is incomplete and fails
     * the Actor's tests.
     *  
     * @param config the Servlet configuration <code>Map</code>
     */
    public void init (ServletConfig config) throws ServletException {
        super.init(config);
        JSON.O o = new JSON.O();
        String conf = config.getInitParameter("less4j");
        String model = config.getInitParameter("less4jModel");
        if (model == null) model = less4jModel; 
        if (conf != null) {
            JSONR.Type type = null;
            try {
                type = JSONR.compile(model);
            } catch (JSON.Error e) {
                throw new ServletException(
                    "Invalid less4j configuration model"
                    );
            }
            JSON.Error e;
            if (type == null) {
                e = (new JSON()).update(o, conf);
                if (e != null) throw new ServletException(
                    "Invalid less4j configuration string", e
                    );
            } else {
                e = (new JSONR(type)).update(o, conf);
                if (e != null) throw new ServletException(
                    "Irregular less4j configuration", e
                    );
            }
        }
        o.put("j2eeRealPath", getServletContext().getRealPath(""));
        Actor $ = new Actor (o);
        if (!this.less4jConfigure ($)) throw new ServletException(
            "Failed less4j configuration"
            );
        
        setConfiguration(o);
    }
 
    private static final String less4jModel = ("{" +
        "\".*\": {" +
            "\"irtd2Salt\": \".+\"," +
            "\"irtd2Salted\": null," +
            "\"irtd2Service\": null," +
            "\"irtd2Timeout\": null," +
            "\"jsonBytes\": null," +
            "\"jsonContainers\": null," +
            "\"jsonIterations\": null," +
            "\"jsonrModel\": null," +
            "\"jdbcDriver\": null," +
            "\"jdbcUsername\": null," +
            "\"jdbcPassword\": null," +
            "\"j2eeDataSource\": null," +
            "\"ldapURL\": null," +
            "\"ldapUsername\": null," +
            "\"ldapPassword\": null," +
            "}" + 
        "}");
                
    private static final int jdbcTimeout = 15;
    
    /**
     * <p>Test wether this controller's configuration actually supports 
     * this Actor class at runtime.</p>
     * 
     * <p>For this class, test wether:</p> 
     * 
     * <ol>
     * <li>the actor instances has salt to digest a cookie and audit of
     * identification, authorizations, time and the response digested
     * previously</li>
     * <li>the configured JDBC driver or DataSource is available.</li>
     * <li>the LDAP server is also accessible, if one is configured.</li>
     * </ol>
     * 
     * <p>By definition a servlet configuration is a runtime environment
     * variable, something that cannot be relied upon without testing
     * it each time the J2EE container initialize it.</p>
     * 
     * <p>Application developers that extend the namespace of less4j's 
     * configuration and the Actor class must overload this method.</p>
     * 
     * @return true if the test was successfull, false otherwise
     */
    public boolean less4jConfigure (Actor $) {
        if (!$.configuration.containsKey("irtd2Salt")) {
            $.logInfo("add salt to digest cookies!", "less4j");
            return false;
        }
        try {
            if ($.configuration.containsKey("jdbcDriver")) {
                Class.forName($.configuration.stri("jdbcDriver"));
                if (DriverManager.getLogWriter() != null)
                    DriverManager.setLogWriter(null);
                if (DriverManager.getLoginTimeout() < jdbcTimeout)
                    DriverManager.setLoginTimeout(jdbcTimeout);
                if (sqlOpen($)) $.sqlClose(); else return false;
                
            } else if ($.configuration.containsKey("j2eeDataSource"))
                if (sqlOpen($)) $.sqlClose(); else return false;
            
        } catch (Exception e) {
            $.logError(e); return false;
        }
        if ($.configuration.containsKey("jsonrModel")) try {
            $.configuration.put("jsonrModel", JSONR.compile(
                $.configuration.objc("jsonModel"), JSONR.TYPES
            ));
        } catch (JSON.Error e) {
            $.logError(e); return false;
        }
        if ($.test) $.logInfo("configuration ok", "less4j");
        return true;
    }
    
    public boolean irtd2Digested (Actor $) {
        return $.irtd2Digested(
            $.configuration.intValue("irtd2Timeout", 3600)
            );
        } 
    
    public boolean jsonGET (Actor $) {
        if ($.configuration.containsKey("jsonrModel"))
            return $.jsonGET(
                $.configuration.intValue("jsonContainers", 65355),
                $.configuration.intValue("jsonIterations", 65355),
                (JSONR.Type) $.configuration.get("jsonrModel")
                );
        else 
            return $.jsonGET(
                $.configuration.intValue("jsonContainers", 65355),
                $.configuration.intValue("jsonIterations", 65355)
                );
        }
    
    public boolean jsonPOST (Actor $) {
        if ($.configuration.containsKey("jsonrModel")) 
            return $.jsonPOST(
                $.configuration.intValue("jsonBytes", 16384), 
                $.configuration.intValue("jsonContainers", 65355),
                $.configuration.intValue("jsonIterations", 65355),
                (JSONR.Type) $.configuration.get("jsonrModel")
                );
        else 
            return $.jsonPOST(
                $.configuration.intValue("jsonBytes", 16384), 
                $.configuration.intValue("jsonContainers", 65355),
                $.configuration.intValue("jsonIterations", 65355)
                );
        }
    
    /**
     * Try to open an SQL connection using the configuration properties,
     * applying <code>sqlOpenJDBC</code> or <code>sqlOpenJ2EE</code>.
     * 
     * @return true if the connection was successfull, false otherwise
     */
    public boolean sqlOpen (Actor $) {
        if ($.configuration.containsKey("jdbcDriver"))
            return $.sqlOpenJDBC(
                $.configuration.stri(
                    "jdbcURL", 
                    "jdbc:mysql://localhost/less4j"),
                $.configuration.stri("jdbcUsername", "less4j"),
                $.configuration.stri("jdbcPassword", "less4j")
                );
        else 
            return $.sqlOpenJ2EE(
                $.configuration.stri("j2eeDataSource", "less4j")
                );
    }

    public boolean sqlNative (Actor $, String statement) {
        boolean success = false;
        if (sqlOpen($)) try {
            $.sql.nativeSQL(statement); success = true;
        } catch (Exception e) {$.logError(e);} finally {$.sqlClose();}
        return success;
    }
    
    public boolean 
    sqlQuery (Actor $, String statement, String[] names, int fetch) {
        boolean success = false;
        if (sqlOpen($)) try {
            $.json.putAll($.sqlQuery(statement, $.json, names, fetch)); 
            success = true;
        } catch (Exception e) {$.logError(e);} finally {$.sqlClose();}
        return success;
    }
        
    public boolean 
    sqlQuery (Actor $, String statement, String name, int fetch) {
        boolean success = false;
        if (sqlOpen($)) try {
            $.json.put(name, $.sqlQuery(statement, fetch)); 
            success = true;
        } catch (Exception e) {$.logError(e);} finally {$.sqlClose();}
        return success;
    }
        
    /**
     * Try to open an LDAP connection using the configuration properties,
     * or the following defaults:
     * 
     * {
     *    "ldapURL": "ldap://127.0.0.1:389/",
     *    "ldapPrincipal": "guest",
     *    "ldapCredentials": ""
     *    }
     * 
     * and return true if the connection was successfull, false otherwise.
     * 
     * @return true if the connection was successfull, false otherwise
     */
    public boolean ldapOpen (Actor $) {
        return $.ldapOpen(
            $.configuration.stri("ldapURL", "ldap://localhost/"),
            $.configuration.stri("ldapPrincipal", "less4j"), 
            $.configuration.stri("ldapCredentials", "less4j")
            );
    }
    
    public void doGet (HttpServletRequest req, HttpServletResponse res) {
        Actor $ = new Actor (getConfiguration(), req, res);
        if (irtd2Digested($))
            if ($.httpIdempotent())
                httpResource($);
            else if (jsonGET($))
                jsonApplication($);
            else
                $.rest302Redirect();
        else 
            irtd2Authorize($);
    }
    
    public void doPost (HttpServletRequest req, HttpServletResponse res) {
        Actor $ = new Actor (getConfiguration(), req, res);
        if (irtd2Digested($)) 
            if (jsonPOST($)) 
                jsonApplication($); 
            else 
                $.rest302Redirect();
        else
            irtd2Authorize($);
    }
    
    // a standard HTTP handler for most Web 2.0 applications of J2EE 
    //
    // irdt2Authorize     Identify and grant rights in a URI context.
    // httpResource       Transfert a resource to identified users.
    // jsonApplication    Control an audited interaction between an 
    //                    identified user and a JSON application.
    //
    // "Everythin is going to be allright"
    
    public void irtd2Authorize (Actor $) {
        $.rest302Redirect($.configuration.stri("irtd2Service", "/"));
    }
    
    public void httpResource (Actor $) {
        $.rest302Redirect("index.html");
    }
    
    public void jsonApplication (Actor $) {
        $.json200Ok();
    }
    
} // Three little birds on my doorstep, Singing: "this is a message to you"
