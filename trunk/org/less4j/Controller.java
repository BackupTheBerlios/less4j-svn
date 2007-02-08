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

import java.util.Iterator;

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
 * to five simple methods:</p>
 * 
 * <blockquote>
 *<pre>jsonApplication (Actor $)
 *jsonrModel(Actor $)
 *irtd2Authorize (Actor $)
 *httpResource (Actor $)
 *less4jConfigure (Actor $)</pre>
 * </blockquote>
 * 
 * <p>To quickly and safely scaffold applications from prototype to 
 * production, all those methods are allready implemented.</p>
 * 
 * <blockquote>
 * <pre>{
 *  "": { 
 *     "irtd2Timeout": 3600,
 *     "postLimit": 4096,
 *     "jsonContainers": 128,
 *     "jsonIterations": 4096,
 *     "jsonRegular": {"irdt2": {"username": ".+", "password": null}}
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
    
//    static HashMap actions = new HashMap();
//    
//    static public void action (Actor $, String name) {
//        ((Actor.Action) actions.get(name)).play($);
//    }
    
    protected JSON.O configuration = new JSON.O ();
    
    /**
     * Clone the controller's configuration HashMap.
     * 
     * @return a new Actor, a thread-safe place for instance variables
     */
    protected JSON.O getConfiguration() {
        synchronized (configuration) {return configuration;}
    }
    
    protected class Model {
        public static final String jsonr = ("{" +
            "\".*\": {" +
                "\"irtd2Timeout\": 86400," +
                "\"postLimit\": 65355," +
                "\"jsonContainers\": 65355," +
                "\"jsonIterations\": 65355," +
                "\"jsonRegular\": null" +
                "}" + 
            "}"
            );
        public String irtd2Service = null;
        public int irtd2Timeout = 3600;
        public int postLimit = 16384;
        public int jsonContainers = 635355;
        public int jsonIterations = 635355;
        public JSONR.Type jsonrType = null;
        
        public Model (JSON.O objc) {
            irtd2Service = objc.stri("irtd2Service", null);
            irtd2Timeout = objc.intValue("irtd2Timeout", 3600);
            postLimit = objc.intValue("postLimit", 16384);
            jsonContainers = objc.intValue("jsonContainers", 635355);
            jsonIterations = objc.intValue("jsonIterations", 635355);
        }
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
        String conf = config.getInitParameter("less4j");
        JSON.O o = new JSON.O();
        if (conf != null) {
            JSON.Error e = (new JSON()).update(o, conf);
            if (e != null)
                throw new ServletException(
                    "Controller JSON configuration error", e
                    );
        }
        o.put("j2eeRealPath", getServletContext().getRealPath(""));
        Actor $ = new Actor (o);
        if (!($.less4jConfigure() && this.less4jConfigure ($)))
            throw new ServletException(
                "Controller or Actor configuration failed runtime tests"
                );
        
        synchronized (configuration) {this.configuration = o;}
    }
 
    /**
     * Configure the controller for its less4j models: a map of url
     * to JSONR regular expressions, IRTD2 timeout, HTTP maximum POST 
     * content length and JSON containers and iterations limits.
     * 
     * <h3>Synopsis</h3>
     * 
     * <p>... derive ...</p>
     * 
     */
    public boolean less4jConfigure (Actor $) {
        try {
            String key;
            Model controllerModel;
            JSON.O model;
            JSON.O less4jModels = $.configuration.objc("less4jModels");
            Iterator iter = less4jModels.keySet().iterator(); 
            while (iter.hasNext()){
                key = (String) iter.next();
                model = less4jModels.objc(key);
                controllerModel = new Model(model);
                if (model.containsKey("jsonRegular"))
                    controllerModel.jsonrType = JSONR.compile(
                        model.objc("jsonRegular"), JSONR.TYPES
                        );
                less4jModels.put(key, controllerModel);
            }
        } catch (JSON.Error e) {
            $.logError(e); return false;
        }
        return true;
    }
    
    public Model less4jModel (Actor $) {
        return (Model) $.configuration.get($.url);
    }
    
    public void doGet (HttpServletRequest req, HttpServletResponse res) {
        Actor $ = new Actor (getConfiguration(), req, res);
        Model model = less4jModel ($);
        if ($.irtd2Digested(model.irtd2Timeout))
            if ($.httpIdempotent())
                this.httpResource($);
            else if ($.jsonGET(
                model.jsonContainers, model.jsonIterations, model.jsonrType
                ))
                this.jsonApplication($);
            else
                $.rest302Redirect();
        else 
            this.irtd2Authorize($, model);
    }
    
    public void doPost (HttpServletRequest req, HttpServletResponse res) {
        Actor $ = new Actor (getConfiguration(), req, res);
        Model model = less4jModel ($);
        if ($.irtd2Digested(model.irtd2Timeout)) 
            if ($.jsonPOST(
                model.postLimit, 
                model.jsonContainers, model.jsonIterations, model.jsonrType
                )) 
                this.jsonApplication($); 
            else 
                $.rest302Redirect();
        else
            this.irtd2Authorize($, model);
    }
    
    public void irtd2Authorize (Actor $, Model model) {
        $.rest302Redirect(model.irtd2Service);
    }
    
    public void httpResource (Actor $) {
        $.rest302Redirect("index.html"); // TODO: find where to redirect ... 
        //
        // return an HTML view with enough JavaScript to present a
        // comprehensive user interface to the application model
        // described in the configuration file.
        //
        // by default, for all resources the Controller redirects user agents
        // to a conventional static file, the obvious "index.html" 
    }
    
    public void jsonApplication (Actor $) {
        $.json200Ok();
    }
    
}