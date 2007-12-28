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

package org.less4j.servlet;

/**
 * An interface to implement web services that: a) describe their input model
 * with <a 
 * href="http://laurentszyster.be/jsonr"
 * >JSONR</a> expressions; b) eventually identify requests; c) serve idempotent 
 * requests for HTTP resources; d) reply to GET and POST requests 
 * with valid JSON or URL encoded form body; e) and may handle other 
 * types HTTP requests. 
 * 
 * @h3 Synopsis
 * 
 * @p Application developers must either derive a new <code>Controller</code> 
 * class or a new function to be dispatched by one of the existing controllers 
 * provided.
 * 
 * @pre package org.less4j.functions;
 * 
 *import org.less4j.Service;
 *import org.less4j.Actor;
 *import org.less4j.Simple;
 *import org.less4j.JSON;
 *
 *public class PrivateService implements Service {
 *    public Service singleton = new Public ();
 *    public String jsonInterface (Actor $) {
 *        return "null"; // interface input description 
 *    }
 *    public boolean less4jConfigure (Actor $) {
 *        return true; // allways configured
 *    }
 *    public boolean irtd2Identify (Actor $) {
 *        $.httpError(401); // not authorized
 *        return false; // not identified
 *    }
 *    public Object jsonRegular (Actor $) {
 *        return new JSON(); // accept any JSON as input
 *    }
 *    public void jsonApplication (Actor $) {
 *        $.jsonResponse(200); // echo the input
 *    }
 *    public void httpResource (Actor $) {
 *        $.jsonResponse(200, jsonInterface($)); // describe the interface
 *    }
 *    public void httpContinue (Actor $, String method, String contentType) {
 *        $.httpError(400); // bad request
 *    }
 *}
 * 
 */
public interface Service {
    /**
     * The only instance of a <code>Service</code>.
     */
    public static Service singleton = null;
    /**
     * Return a <a 
     * href="http://laurentszyster.be/jsonr"
     * >JSONR</a> string describing this <code>Service</code> interface.
     * 
     * @p ...
     * 
     * @pre public static String jsonInterface (Actor $) {
     *   return "null";
     *}
     *
     * @p ...
     * 
     * @param $ the actor at play
     * @return a JSONR string
     */
    public String jsonInterface (Actor $);
    /**
     * Test wether this controller's configuration actually supports 
     * this <code>Service</code> at runtime. 
     * 
     * @p Functions that extend the namespace of less4j's configuration must 
     * override this method.
     * 
     * @pre public boolean less4jConfigure (Actor $) {
     *    if (super.less4jConfigure($)) {
     *        // configure your extension here
     *        return true;
     *    } else
     *        return false;
     *}
     *
     * @p Note that overriden method should be called. 
     * 
     * @param $ the actor at play
     * @return true if the configuration was successfull, false otherwise
     */
    public boolean less4jConfigure (Actor $);
    /**
     * Identify the requester, then return true to digest a new IRTD2 Cookie 
     * and continue the request or complete the response and return false, 
     * by default grant no rights to a random user ID made of ten alphanumeric 
     * characters.
     * 
     * @p A simple implementation is to reply unidentified requests
     * with a <code>401 Not Authorized</code> response:
     * 
     * @pre public boolean irtd2Identify (Actor $) {
     *   $.httpError(401)); 
     *   return false; 
     *}
     * 
     * @p The simplest implementation is to pass unidentified requests 
     * through, here to handle JSON login with a configurable password
     * for a <code>root</code> access in the root context "/":
     * 
     * @pre public static boolean irtd2Identify (Actor $) {
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
     * @param $ the actor's state
     * @return true if the request was identified, false otherwise
     */
    public boolean irtd2Identify (Actor $);
    /**
     * Returns a <code>JSON</code> or <code>JSONR</code> interpreter to 
     * validate a GET request's query string or a POSTed JSON request body.
     * Returns <code>null</code> to disable non-idempotent request
     * handling. 
     * 
     * @p For instance, allow any JSON type but limit the numbers of
     * container objects to 4 and set a maximum of 16 instances:
     * 
     * @pre public Object jsonRegular (Actor $) {
     *    return new JSON(4, 16);
     *}
     * 
     * @p Or constraint the input to a JSONR pattern:
     * 
     * @pre private static final JSONR.Type model = JSONR.compile (
     *    JSON.dict(new Object[]{
     *        "hello", "^.+$",
     *        "test", true
     *        })
     *    ); 
     *public Object jsonRegular (Actor $) {
     *    return new JSONR(model);
     *}
     * 
     * @p To prevent handling of URL encoded form and JSON request, return
     * <code>null</code>: 
     * 
     * @pre public Object jsonRegular (Actor $) {
     *    return null;
     *}
     * 
     * @param $ the Actor's state
     * @return <code>null</code>, a <code>JSON</code> or <code>JSONR</code> 
     *         interpreter
     */
    public Object jsonRegular (Actor $);
    /**
     * Control an audited interaction between an identified user and a 
     * JSON application.
     * 
     * @p ...
     * 
     * @pre public void jsonApplication (Actor $) {
     *    $.jsonResponse(501); // Not implemented
     *}
     *
     * @p ...
     * 
     * @param $ the Actor's state
     */
    public void jsonApplication (Actor $);
    /**
     * Reply to idempotent HTTP requests not handled by a configured
     * <code>Service</code> or this controller.
     * 
     * @p This is a method to overload in an application controller that
     * serve resources in this servlet context to identified users.
     * 
     * @pre public void httpResource (Actor $) {
     *    return $.httpError(404); // Not Found
     *}
     *
     * @p Practically, for database and directory controllers there is
     * little else to do short of implementing your own database to URI 
     * namespace mapping for resources. Subclassing this method makes
     * it possible, but most controller will only need a static page
     * to act as a bootstrap for a JSON application.
     * 
     * @param $ the Actor's state
     */
    public void httpResource (Actor $);
    /**
     * Complete requests not handled by <code>httpResource</code> or
     * <code>jsonApplication</code>. 
     *
     * @p ...
     * 
     * @pre public void httpContinue (Actor $) {
     *    return $.httpError(400); // Bad Request
     *}
     *
     * @p ...
     * 
     * @param $ the actor's state
     * @param method the request's HTTP method ("GET", "POST", etc)
     * @param contentType of the request's body
     */
    public void httpContinue (Actor $, String method, String contentType);
};

