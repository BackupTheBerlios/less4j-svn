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
 * An interface to implement web services that eventually identify requests,
 * serve idempotent requests for resources, reply to idempotent GET and POST 
 * requests with JSON and URL encoded form body that match a regular JSON
 * pattern, or handle any other HTTP request. 
 * 
 * @h3 Synopsis
 * 
 * @p For sample use of <code>Service</code> see its many implementations 
 * found in less4j.
 * 
 * @p It is implemented by the base <code>Controller</code> function 
 * dispatcher and the derived classes found in 
 * <code>org.less4j.controllers</code>. 
 * This interface is also implemented by classes packaged in 
 * <code>org.less4j.functions</code>.
 * 
 * @p Note that this interface also supports a WS-* implementation in 
 * the <code>SOAP</code> class, complete with WSDL generation from regular 
 * JSON expressions and two-way JSON/SOAP translation. 
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
     * @pre public boolean less4jConfigure (Actor $) {
     *    if (super.less4jConfigure($)) {
     *        // configure your extension here
     *        return true;
     *    } else
     *        return false;
     *}
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
     * @pre public boolean irtd2Identify (Actor $) {
     *    $.identity = Simple.password(10);
     *    return true;
     *}
     * 
     * @p A simpler implementation is to reply unidentified requests
     * with a <code>401 Not Authorized</code> response:
     * 
     * @pre public boolean irtd2Identify (Actor $) {
     *   $.httpError(401)); 
     *   return false; 
     *}</pre>
     * 
     * @p Or redirect the user agent to another controller:
     * 
     * @pre public boolean irtd2Identify (Actor $) {
     *   $.http302Redirect("/login"); 
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
     * @p ...
     *  
     * @param $ the actor's state
     * @return true if the request was identified, false otherwise
     */
    public boolean irtd2Identify (Actor $);
    /**
     * Complete requests not handled by <code>httpResource</code> or
     * <code>jsonApplication</code>. 
     *
     * @pre public void httpContinue (Actor $) {
     *    return $.httpError(400); // Bad Request
     *}
     *
     * @p ...
     *
     * @param $ the actor's state
     */
    public void httpContinue (Actor $, String method, String contentType);
    /**
     * Reply to idempotent HTTP requests not handled by a configured
     * <code>Function</code> or this controller.
     * 
     * @pre public void httpResource (Actor $) {
     *    return $.httpError(404); // Not Found
     *}
     *
     * @p This is a method to overload in an application controller that
     * serve resources in this servlet context to identified users.
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
     * Returns a <code>JSON</code> or <code>JSONR</code> interpreter
     * to validate a GET request's query string or a POSTed JSON request 
     * body. 
     * 
     * @pre public Object jsonRegular (Actor $) {
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
     * @pre public void jsonApplication (Actor $) {
     *    $.jsonResponse(501); // Not implemented
     *}
     *
     * @param $ the Actor's state
     */
    public void jsonApplication (Actor $);
};

