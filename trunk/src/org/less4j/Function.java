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

/**
 * An interface to implement configurable functions dispatched by less4j's 
 * base <code>Controller</code> but also extended <code>Controller</code> 
 * classes.
 * 
 * @synopsis package org.less4j.tests;
 * 
 *import org.less4j.*;
 * 
 *class HelloWorld implements Function {
 *    public Function singleton = new HelloWorld ();
 *    private JSONR.Type _interface;
 *    public String jsonInterface (Actor $) {
 *        return "{\"hello\":\"world[!]\"}";
 *    }
 *    public boolean less4jConfigure (Actor $) {
 *        try {
 *            _interface = JSONR.compile(jsonInterface($)); 
 *            return true;
 *        } catch (JSON.Error e) {
 *            $.logError(e);
 *            return false;
 *        }
 *    }
 *    public boolean irtd2Identify (Actor $) {
 *        $.identity = Simple.password(10);
 *        $.rights = "";
 *        return true;
 *    }
 *    public Object jsonRegular (Actor $) {
 *        return new JSONR (_interface, 1, 2);
 *    }
 *    public void jsonApplication (Actor $) {
 *        $.jsonResponse(200);
 *    }
 *    public void httpResource (Actor $) {
 *        $.jsonResponse(200, "{\"hello\": \"world!\"}");
 *    }
 *    public void httpContinue (Actor $) {
 *        $.httpResponse(400);
 *    }
 *}
 *
 * @synopsis {
 *  "test": true;
 *  "functions": {
 *    "\/hello-world": "org.less4j.tests.HelloWorld"
 *  }
 *}
 * 
 * @copyright 2006-2007 Laurent A.V. Szyster
 *
 */
public interface Function {
    /**
     * The only instance of a <code>Function</code>.
     */
    public static Function singleton = null;
    /**
     * Return a JSONR string describing this <code>Function</code> interface. 
     */
    public String jsonInterface (Actor $);
    /**
     * Test wether this controller's configuration actually supports 
     * this <code>Function</code> at runtime. Functions that extend the 
     * namespace of less4j's configuration must override this method.
     * 
     * @synopsis public boolean less4jConfigure (Actor $) {
     *    if (super.less4jConfigure($)) {
     *        // configure your extension here
     *        return true;
     *    } else
     *        return false;
     *}
     * 
     * @return true if the configuration was successfull, false otherwise
     */
    public boolean less4jConfigure (Actor $);
    /**
     * Identify the requester, then return true to digest a new IRTD2 Cookie 
     * and continue the request or complete the response and return false, 
     * by default grant no rights to a random user ID made of ten alphanumeric 
     * characters.
     * 
     * @synopsis public boolean irtd2Identify (Actor $) {
     *    $.identity = Simple.password(10);
     *    return true;
     *}
     * 
     * @xml A simpler implementation is to reply unidentified requests
     * with a <code>401 Not Authorized</code> response:
     * 
     * @synopsis public boolean irtd2Identify (Actor $) {
     *   $.httpError(401)); 
     *   return false; 
     *}</pre>
     * 
     * @xml Or redirect the user agent to another controller:
     * 
     * @synopsis public boolean irtd2Identify (Actor $) {
     *   $.http302Redirect("/login"); 
     *   return false; 
     *}
     * 
     * @xml The simplest implementation is to pass unidentified requests 
     * through, here to handle JSON login with a configurable password
     * for a <code>root</code> access in the root context "/":
     * 
     * @synopsis public static boolean irtd2Identify (Actor $) {
     *   return true;
     *}
     *
     *public void jsonApplication (Actor $) {
     *   if ( // there is an URL query string or a JSON body with a password
     *       $.json.S("password", "").equals(
     *           $.configuration.S("password", "less4j")
     *           )
     *       )
     *       // digest a new IRTD2 cookie for user "root" with "root" role
     *       $.identity = "root";
     *       $.rights = "root";
     *       $.irtd2Digest("/");
     *       // is identified, continue the request...
     *   else
     *       $.jsonResponse(401); // Not Authorized
     *       // not identified, response completed. 
     *}
     * 
     * @xml ...
     *  
     * @param $ the Actor's state
     * @return true if the request was identified, false otherwise
     */
    public boolean irtd2Identify (Actor $);
    /**
     * Complete requests not handled by <code>httpResource</code> or
     * <code>jsonApplication</code>. 
     *
     * @synopsis public void httpContinue (Actor $) {
     *    return $.httpError(400); // Bad Request
     *}
     *
     * @xml ...
     *
     * @param $
     */
    public void httpContinue (Actor $, String method, String contentType);
    /**
     * Reply to idempotent HTTP requests not handled by a configured
     * <code>Function</code> or this controller.
     * 
     * @synopsis public void httpResource (Actor $) {
     *    return $.httpError(404); // Not Found
     *}
     *
     * @xml This is a method to overload in an application controller that
     * serve resources in this servlet context to identified users.
     * 
     * @xml Practically, for database and directory controllers there is
     * little else to do short of implementing your own database to URI 
     * namespace mapping for resources. Subclassing this method makes
     * it possible, but most controller will only need a static page
     * to act as a bootstrap for a JSON application.
     * 
     * @param $ the Actor's state
     */
    public void httpResource (Actor $);
    /**
     * Returns a <code>JSON</code> or <code>JSONR</code> interpreter
     * to validate a GET request's query string or a POSTed JSON request 
     * body. 
     * 
     * @synopsis public Object jsonRegular (Actor $) {
     *    return new JSON();
     *}
     * 
     * @param $ the Actor's state
     * @return a <code>JSON</code> or <code>JSONR</code> interpreter
     */
    public Object jsonRegular (Actor $);
    /**
     * Control an audited interaction between an identified user and a 
     * JSON application.
     * 
     * @synopsis public void jsonApplication (Actor $) {
     *    $.jsonResponse(501); // Not implemented
     *}
     *
     * @param $ the Actor's state
     */
    public void jsonApplication (Actor $);
};

