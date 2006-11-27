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

import java.util.HashMap;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Pattern;

import java.net.URLEncoder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import javax.sql.DataSource;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;

import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;

/**
 * <p>A "full-stack" API to develop XML and JSON interfaces for J2EE 
 * controllers of entreprise SQL and LDAP resources, starting with 
 * no-nonsense logging and a cross-plateform protocol of identification 
 * and authorization in time, ending with a general audit that can track 
 * any paths of user interaction.</p>
 * 
 * <h3>Table of Content</h3>
 * 
 * <p>The actor members and methods are grouped by aspects of its
 * applications as follow:</p>
 * 
 * <dl>
 * <di><dt>Runtime Environment:
 * <code>time</code>,
 * <code>test</code>, 
 * <code>logOut</code>, 
 * <code>logInfo</code>, 
 * <code>logError</code>.
 * </dt><dd>
 * Time, runtime environment, standard output and error. The basics, and
 * just that. No fancy logging formats and long stack traces, the bare
 * minimum for all actor: STDOUT and STDERR, a compact exception trace
 * and unconstrained logging categories for information.
 * <dd></di>
 * <di><dt>Configuration:
 * <code>configuration</code>, 
 * <code>testConfiguration</code>, 
 * <code>request</code>,
 * <code>response</code>.
 * </dt><dd>
 * Because "everything you don't test does not work", there are two
 * Actor constructors. One for instances that test a controller's 
 * configuration and one for instances that will be apply it on an 
 * HTTP transaction.
 * </dd></di>
 * <di><dt>Security:
 * <code>identity</code>,
 * <code>roles</code>,
 * <code>digest</code>,
 * <code>digested</code>,
 * <code>notAuthorized</code>, 
 * <code>authorize</code>,
 * <code>audit</code>.
 * </dt><dd>
 * Identification, authorization and audit of the user agent, without
 * the woes of <code>HttpServletSession</code>. A practical implementation
 * of a simple protocol based on HTTP cookies and SHA1 digest, cross
 * plateform and designed to let the weight of persistent sessions be
 * distributed ... on the clients. Eventually, it also provides an effective
 * audit follow the multiple paths of interactions of a single user and
 * detect fraud attemps.
 * </dd></di>
 * <di><dt>REST:
 * <code>httpURL</code>,
 * <code>httpIdempotent</code>,
 * <code>actions</code>,
 * <code>rest200Ok</code>, 
 * <code>rest302Redirect</code>.
 * </dt><dd>
 * ...
 * </dd></di>
 * <di><dt>JSON and JSONR:
 * <code>json</code>,
 * <code>jsonr</code>,
 * <code>jsonGET</code>,
 * <code>jsonPOST</code>,
 * <code>json200Ok</code>.
 * </dt><dd>
 * ...
 * </dd></di>
 * <di><dt>SQL:
 * <code>sql</code>,
 * <code>sqlOpen</code>,
 * <code>sqlClose</code>,
 * <code>sqlQuery</code>, 
 * <code>sqlUpdate</code>.
 * </dt><dd>
 * ...
 * </dd></di>
 * <di><dt>LDAP:
 * <code>ldap</code>,
 * <code>ldapOpen</code>,
 * <code>ldapClose</code>,
 * <code>ldapResolve</code>, 
 * <code>ldapUpdate</code>, 
 * <code>ldapCreate</code>
 * </dt><dd>
 * ...
 * </dd></di>
 * </dl>
 * 
 * <p><b>Copyright</b> &copy; 2006 Laurent A.V. Szyster</p>
 * 
 * @author Laurent Szyster
 * @version 0.1.0
 */
public class Actor {
    
    public static final String TEST = "Test";
    
    public static final String less4j = "less4j";
    
    /**
     * <p>The default charset encoding in less4j, something supported by
     * web 2.0 browsers (IE6, Firefox, etc ...) and java runtime
     * environments. Also the defacto standard for JSON encoding.</p>
     */
    private static final String less4jCharacterSet = "UTF-8";
    
    private static final String less4jTrue = "true";
    private static final String less4jTest = "less4j.test";

    private static final String less4jDigestName = "IRTD2";
    private static final String less4jDigestSalt = "less4j.digest.salt";
    private static final String less4jDigestedSalt = "less4j.digested.salt";
    
    private static final String less4jJDBCDriver = "less4j.jdbc.driver";
    private static final String less4jJDBCDataSource = "less4j.jdbc.datasource";
    private static final String less4jJDBCURL = "less4j.jdbc.url";
    private static final String less4jJDBCUsername = "less4j.jdbc.username";
    private static final String less4jJDBCPassword = "less4j.jdbc.password";
    
    private static final 
    String less4jLDAPCtxFactory = "com.sun.jndi.ldap.LdapCtxFactory"; 
    private static final String less4jLDAPSecurity = "simple"; 
    private static final String less4jLDAPURL = "less4j.ldap.url";
    private static final String less4jLDAPPrincipal = "less4j.ldap.principal";
    private static final 
    String less4jLDAPCredentials = "less4j.ldap.credentials";

    /**
     * The IRTD2 salt, encoded as 8-bit bytes
     */
    public byte[] salt = null;
    
    /**
     * The previous IRTD2 salt, encoded as 8-bit bytes
     */
    public byte[] salted = null;
    
    /**
     * A boolean that indicates wether the Actor runtime environment is
     * a test or a production one. It is set to <code>false</code> by
     * defautlt.
     */
    public boolean test = false;
    
    /**
     * A copy of the servlet controller's configuration, mapping all
     * options listed in the servlet's XML configuration file and whose 
     * name start with <code>"less4j"</code>. 
     */
    public HashMap configuration;

    /**
     * The <code>HttpServletRequest</code> handled by this Actor instance.
     */
    public HttpServletRequest request;
    
    /**
     * The <code>HttpServletResponse</code> completed by this Actor instance.
     */
    public HttpServletResponse response;

    /**
     * The authenticated identity of this Actor's web user, as set from
     * the HTTP request's <code>IRTD2</code> cookie by a call to the 
     * <code>notAuthorized</code> or <code>Authorize</code> method. 
     */
    public String identity = ""; // "user@domain", the user's principal

    /**
     * The authenticated rights of this Actor's web user, as set from
     * the HTTP request's <code>IRTD2</code> cookie by a call to the 
     * <code>notAuthorized</code> or the <code>Authorize</code> method.
     */
    public String rights = ""; // whatever fits your application 
    
    /**
     * The time of this Actor's, in seconds since epoch 
     * (ie: 01/01/1970)  
     */
    public long time = (System.currentTimeMillis()/1000);
    
    /**
     * The authentication and audit digest of the previous request, as set 
     * from the HTTP request's <code>IRTD2</code> cookie by a call to the 
     * <code>notAuthorized</code> or <code>Authorize</code> method.
     */
    public String digested = null; // the previous digest
    
    /**
     * The authentication and audit digest of this response, as set 
     * from the HTTP request's <code>IRTD2</code> cookie by a call to the 
     * <code>notAuthorized</code> or <code>Authorize</code> method.
     */
    public String digest = null; // this digest
    
    /**
     * An HashMap of the validated HTTP request's query string.
     */
    public HashMap actions = null;
    
    /**
     * The JSON object associated with the Actor's request and/or response.
     */
    public HashMap json = null;
    
    /**
     * An open JDBC connection or <code>null</code>.
     */
    public Connection sql = null;
    
    /**
     * An open LDAP connection or <code>null</code>.
     */
    public InitialDirContext ldap = null;
    
    /**
     * Initialize a new Actor to test a controller servlet's configuration 
     * when it is initialized.
     * 
     * @param conf the controller's configuration HashMap
     */
    public Actor (HashMap conf) {
        configuration = conf;
        test = (less4jTrue.equals((String)configuration.get(less4jTest)));
        }
    
    /**
     * Initialize a new Actor to handle an HTTP request and response, 
     * set the Actor's audit digest salt and actions map.
     * 
     * @param conf the controller's configuration HashMap
     * @param req the HTTP request to handle 
     * @param res the HTTP response to complete
     */
    public Actor (
        HashMap conf, HttpServletRequest req, HttpServletResponse res
        ) {
        configuration = conf;
        test = (less4jTrue.equals((String) configuration.get(less4jTest)));
        salt = ((String) configuration.get(less4jDigestSalt)).getBytes();
        request = req;
        response = res;
    }

    /**
     * <p>Write a message to STDOUT, as one line: 
     * 
     * <blockquote>
     * <pre>message</pre>
     * </blockquote>
     *     
     * If you cannot apply Resin's excellent configuration of STDOUT for
     * your web applications:
     * 
     * <blockquote>
     * <pre><a href="http://wiki.caucho.com/Stdout-log"
     *   >http://wiki.caucho.com/Stdout-log</a></pre>
     * </blockquote>
     * 
     * use multilog or any other log post-processor to add timestamp and
     * other usefull audit information. From outside your application, where 
     * its test and audit functions belong.</p>
     * 
     * @param message the string logged to STDOUT
     *     
     */
    public void logOut (String message) {
        System.out.println(message);
    }
    
    private static final String logInfoDelimiter = ": "; 
    
    /**
     * <p>Write a categorized message to STDERR, as:
     * 
     * <blockquote>
     * <pre>category: message</pre>
     * </blockquote>
     *
     * <p>Again, you should use a log-posprocessor to add audit information 
     * to your logs. Or apply Resin:
     * 
     * <blockquote>
     * <pre><a href="http://wiki.caucho.com/Stderr-log"
     *   >http://wiki.caucho.com/Stderr-log</a></pre>
     * </blockquote>
     * 
     * Audit logs may inform about a completed transaction, an aborted
     * action, a authorization failure or any hazardous event for which
     * a trace is legaly required.</p>
     * 
     * <p>Categorization is unpredictable, it should not be restricted to 
     * levels of debugging which are usefull to programmers only.</p>
     * 
     * @param message
     * @param category
     *     
     */
    public void logInfo (String message, String category) {
        StringBuffer sb = new StringBuffer();
        sb.append(category);
        sb.append(logInfoDelimiter);
        sb.append(message);
        System.err.println(sb.toString());
    }

    private static final String stackTraceCategory = "StackTrace: ";
    private static final String stackTraceDelimiter = " | "; 
    private static final String stackTraceDot = "."; 
    private static final String stackTraceSpace = " ";
    
    /**
     * <p>Write a compact stack trace to STDERR in the "StackTrace" category,
     * in one line, like:
     * 
     * <blockquote>
     * <pre>StackTrace: error | thrower.called 123 | catcher.calling 12</pre>
     * </blockquote>
     * 
     * Also, if test is false, log the JSON object of this actor.</p>
     * 
     * <p><strong>Usage:</strong>
     * 
     * <blockquote>
     * <pre>try {...} catch (Exception e) {logError (e);}</pre>
     * </blockquote>
     *     
     * <p>The purpose is to give debuggers a usefull "slice" of the stack
     * trace: which error, where it occurred and what caused it. In its
     * context, amongst other log lines, not spread all over the screen.</p>
     *
     * <p>Without an error message, because developers need a stack trace of 
     * *unexpected* exception only. In such case any attempt to explicit 
     * it with a message would be ludicrous. Again, it's the meaning of
     * the log context around that do give debuggers the most relevant
     * information to understand why an error conditions was not handled
     * by the application and how to fix it.</p>
     * 
     * <p>In production (not testing), what you need to audit errors is an
     * image of the application context without confidential information: 
     * the entire JSON object model but none of the server's configuration.
     * 
     * <blockquote>
     * <pre>StackTrace: error | thrower.called 123 | catcher.calling 12 {...}</pre>
     * </blockquote>
     *
     * This is what the helpdesk needs and it still fits on one line.</p>
     *
     * @param error the throwable instance catched
     * 
     */
    public void logError (Throwable error) {
        StackTraceElement[] stacktrace = error.getStackTrace();
        StackTraceElement catcher = stacktrace[0];
        StackTraceElement thrower = stacktrace[stacktrace.length-1];
        StringBuffer sb = new StringBuffer();
        sb.append(stackTraceCategory);
        sb.append(error.getMessage());
        sb.append(stackTraceDelimiter);
        sb.append(thrower.getClassName());
        sb.append(stackTraceDot);
        sb.append(thrower.getMethodName());
        sb.append(stackTraceSpace);
        sb.append(thrower.getLineNumber());
        sb.append(stackTraceDelimiter);
        sb.append(catcher.getClassName());
        sb.append(stackTraceDot);
        sb.append(catcher.getMethodName());
        sb.append(stackTraceSpace);
        sb.append(catcher.getLineNumber());
        if (!test && json != null) {
            sb.append(stackTraceSpace);
            sb.append(json.toString());
        }
        System.err.println(sb.toString());
    }
    
    private static final int less4jJDBCTimeout = 15;
    
    /**
     * <p>Test wether this controller's configuration actually supports 
     * this Actor class at runtime</p>
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
    public boolean testConfiguration () {
        if (!configuration.containsKey(less4jDigestSalt)) {
            logInfo("add salt to digest cookies!", "Configuration");
            return false;
        }
        String dbn;
        try {
            if (DriverManager.getLoginTimeout() < less4jJDBCTimeout)
                DriverManager.setLoginTimeout(less4jJDBCTimeout);
            if (DriverManager.getLogWriter() != null)
                DriverManager.setLogWriter(null);
            dbn = (String) configuration.get(less4jJDBCDriver);
            if (dbn != null) {
                Class.forName(dbn);
                if (sqlOpen()) {sqlClose();} else return false;
            } else {
                dbn = (String) configuration.get(less4jJDBCDataSource);
                if (dbn != null) {
                    if (sqlOpen()) {sqlClose();} else return false;
                }
            }
        } catch (Exception e) {
            logError(e);
            return false;
        }
        if (test) logInfo("configuration OK", TEST);
        return true;
    }
    
    private static final int less4jCookieMaxAge = 86400; 
    
    /**
     * Literally "digest a cookie": transform the IRTD2 cookie sent with
     * the request into a new cookie to send with the response.
     * 
     * <p>The cookie value is a formatted string made as follow
     * 
     * <blockquote>
     * <pre>Cookie: IRTD2=<strong>identity:roles:time:digested:digest</strong>; </pre>
     * </blockquote>
     * 
     * ...</p>
     *
     * @param path
     */
    public void irtd2Digest(String path) {
        StringBuffer sb = new StringBuffer();
        String timeString = Long.toString(time);
        SHA1 md = new SHA1();
        sb.append(identity);
        sb.append(':');
        sb.append(rights);
        sb.append(':');
        sb.append(timeString);
        sb.append(':');
        if (digested != null) sb.append(digested);
        md.update(sb.toString().getBytes());
        md.update(salt);
        digest = md.hexdigest();
        sb.append(':');
        sb.append(digest);
        Cookie irtd2 = new Cookie(less4jDigestName, sb.toString());
        irtd2.setDomain(request.getServerName());
        irtd2.setPath(path);
        irtd2.setMaxAge(less4jCookieMaxAge); 
        response.addCookie(irtd2);
    }
    
    protected static Pattern less4jDigestSplit = Pattern.compile(":");
    
    /**
     * <p>Try to collect a IRTD2 cookie in the request and test its digest 
     * against the secret(s) set by configuration for this actor's controller, 
     * digest a new cookie only if a the digested cookie is still valid in 
     * time and bears the signature of this servlet, return false otherwise.</p> 
     * 
     * <p>There are four benefits to expect from IRTD2 cookies for
     * J2EE public applications:</p>
     * 
     * <ol>
     * <li>Remove the weight of statefull sessions in the J2EE container.</li>
     * <li>Distribute the load of authorization on a cluster of servers 
     * without adding more contention and latency.</li>
     * <li>Trace multiple interaction paths of each users.</li>
     * <li>Audit impersonation exploit by a man-in-the-middle.</li>
     * </ol>
     * 
     * <p>Note that it does <em>not</em> prevent cookie theft but it
     * does the only next-best actually possible for a public network
     * application: detect cookie theft ASAP.</p>
     * 
     * @param timeout the limit of an IRTD2 cookie's age, in seconds  
     * @return true if the request failed to be authenticated
     */
    public boolean irtd2Digested (long timeout) {
        try {
            /* the ever usefull ;-) */
            int i; 
            /* get the request's IRTD2 authorization cookies ... */
            Cookie irtd2Cookie = null; 
            Cookie[] cookies = request.getCookies();
            if (cookies != null) for (i = 0; i < cookies.length; i++) {
                if (cookies[i].getName().equals(less4jDigestName)) {
                    irtd2Cookie = cookies[i];
                    break;
                }
            }
            if (irtd2Cookie == null)
                return false; 
                /* ... do not authorize if no IRTD2 cookie found. */
            
            String[] irtd2 = less4jDigestSplit.split(irtd2Cookie.getValue());
            if (irtd2.length != 5)
                return false; 

            StringBuffer sb = new StringBuffer();
            identity = irtd2[0];
            sb.append(identity);
            sb.append(':');
            rights = irtd2[1];
            sb.append(rights);
            sb.append(':');
            long lastTime = Long.parseLong(irtd2[2]);
            sb.append(irtd2[2]);
            sb.append(':');
            sb.append(irtd2[3]);
            byte[] irtd = sb.toString().getBytes();
            digested = irtd2[4];
            if (time - lastTime > timeout) {
                return false;
                /* ... do not authorize after timeout. */
                }
            /* */
            SHA1 md = new SHA1();
            md.update(irtd);
            md.update(salt);
            String d = md.hexdigest();
            if (!d.equals(digested)) {
                // try the previously digested salt instead
                if (!configuration.containsKey(less4jDigestedSalt)) 
                    return false;
                
                salted = (
                    (String) configuration.get(less4jDigestedSalt)
                    ).getBytes();
                md = new SHA1();
                md.update(irtd);
                md.update(salted);
                d = md.hexdigest();
                if (!d.equals(digested)) {
                    // TODO: audit a possible fraud attempt!
                    return false;  
                }
            }
            irtd2Digest(irtd2Cookie.getPath());
            return false; 
        } 
        catch (Exception e) {
            logError(e);
            return true;           
        }
    }
    
    public boolean irtd2Authorized(String right, long timeout) {
        if (irtd2Digested(timeout))
            return rights.indexOf(right) > -1; 
        else
            return false;
    }
    
    private static final Pattern irtd2Identity = Pattern.compile(
        "[\\x20-\\x2B\\x2D-\\x39\\x3C-x7E]+"
        );
    
    private static final Pattern irtd2Rights = Pattern.compile(
        "([\\x20-\\x39\\x3C-x7E]+)(,[\\x20-\\x39\\x3C-x7E]+)*"
        );
    
    /**
     * <p>Set the Actor's <code>identity</code> and grant <code>rights</code>
     * digested into an HTTP cookie named <code>IRTD2</code>, like this:
     * 
     * <blockquote>
     * <pre>Cookie: IRTD2=identity:rights:time:digested:digest; ...</pre>
     * </blockquote>
     * 
     * that authenticate a one-time digest for the next invokation of
     * the application's controller in the domain and path set for
     * thise cookie.</p>
     * 
     * @param identity
     * @param rights
     * @param path
     */
    public void irtd2Authorize(String identity, String rights, String path) 
    throws Exception {
        if (!irtd2Identity.matcher(identity).matches())
            throw new Exception("irregular IRTD2 identity");
        else if (!irtd2Rights.matcher(rights).matches())
            throw new Exception("irregular IRTD2 rights");
        else {    
            this.identity = identity;
            this.rights = rights;
            irtd2Digest(path);
        }
    }
    
    private static final String less4jAudit = "AUDIT: ";
    private static final String less4jAuditDelimiter = " ";
    
    /** 
     * Log an audit of this HTTP request and response in one line. 
     * 
     * <p>For instance:
     * 
     * <blockquote>
     * <pre>identity roles time digested digest GET url HTTP/1.1 200</pre>
     * </blockquote>
     * 
     * using by convention strings without whitespace for the identity
     * and the enumeration of the rights granted. Practically, that fits
     * email addresses or names that users allready use as principals.</p>
     * 
     * <p>Role names don't *need* whitespaces, they are pretty much
     * constants defined at the network level (for instance products of an 
     * ASP or rigths over contents in a CMS, etc ...).</p>
     * 
     * <p>Note that digested is the digest of the previous request, the
     * backlink to chain a session step by step ... and detect fraud.</p>
     * 
     */
    private void audit (int status) {
        StringBuffer sb = new StringBuffer();
        sb.append(less4jAudit);
        sb.append(identity);
        sb.append(less4jAuditDelimiter);
        sb.append(rights);
        sb.append(less4jAuditDelimiter);
        sb.append(Long.toString(time));
        sb.append(less4jAuditDelimiter);
        sb.append(digested);
        sb.append(less4jAuditDelimiter);
        sb.append(digest);
        sb.append(less4jAuditDelimiter);
        sb.append(request.getMethod());
        sb.append(less4jAuditDelimiter);
        sb.append(request.getRequestURI());
        String query = request.getQueryString();
        if (query != null) sb.append(query);
        sb.append(less4jAuditDelimiter);
        sb.append(request.getProtocol());
        sb.append(less4jAuditDelimiter);
        sb.append(status);
        logOut(sb.toString());
    }
    
    protected static final Pattern httpPreferences = 
        Pattern.compile("^(.*?)(;.+?=.+?)?((,.*?)(/s*?;.+?=.+?)?)*$");
    
    /**
     * Validate the request's actions Map against a HashMap of type caster 
     * from String and regular expression patterns. Other control limits are 
     * best expressed in Java, usually to compute numbers and not process 
     * text.
     * 
     * @param model a JSONR object pattern
     * @return true if there is at least one valid action
     */
    public boolean urlActions(JSONR model) {
        actions = new HashMap(request.getParameterMap());
        return ! actions.isEmpty();
    }

    public boolean urlAction (String name) {
        return actions.containsKey(name);
    }
    
    public String urlParameter (String name) {
        String[] args = (String[]) actions.get(name);
        if (args != null && args.length > 0) 
            return args[0];
        else
            return "";
    }
    
    private static final 
    String less4jHTTP = "http";
    private static final 
    String less4jHTTPS = "https";
    private static final 
    String less4jHTTPHost = "://";
    private static final 
    String less4jHTTPPort = ":";
    
    /**
     * A convenience to validate a location as an absolute URL.
     * 
     * <p>Eventually complete a relative location to this Actor's 
     * requested resource
     * 
     * <blockquote><pre>?action</pre></blockquote>
     *
     * relative to its application's domain
     * 
     * <blockquote><pre>/resource</pre></blockquote>
     * 
     * or to the resource context path
     * 
     * <blockquote><pre>resource</pre></blockquote>
     * 
     * to produce a n absolute URL like
     *
     * <blockquote><pre>http://domain/resource?action</pre></blockquote>
     * 
     * something quite usefull for redirection in a RESTfull controller.</p>
     * 
     * @param location a relative web location
     * @return an absolute web URL
     * 
     */
    public String urlAbsolute(String location) {
        /* note that this implementation is inlined for maximum speed */
        if (location.length() == 0)
            return request.getRequestURL().toString();
        
        StringBuffer sb;
        String protocol;
        int port;
        char first = location.charAt(0);
        if (first == '?') {
            sb = request.getRequestURL();
            sb.append(location);
        } else if (first == '/') {
            sb = new StringBuffer();
            protocol = request.getProtocol().split("/")[0].toLowerCase();
            sb.append(protocol);
            sb.append(less4jHTTPHost);
            sb.append(request.getServerName());
            port = request.getServerPort();
            if (
                (protocol.equals(less4jHTTP) && port != 80) ||
                (protocol.equals(less4jHTTPS) && port != 443)
                ) {
                sb.append(less4jHTTPPort);
                sb.append(port);
            }
            sb.append(location);
        } else if (!location.matches("https?:.+")) {
            sb = new StringBuffer();
            protocol = request.getProtocol().split("/")[0].toLowerCase();
            sb.append(protocol);
            sb.append(less4jHTTPHost);
            sb.append(request.getServerName());
            port = request.getServerPort();
            if (
                    (protocol.equals(less4jHTTP) && port != 80) ||
                    (protocol.equals(less4jHTTPS) && port != 443)
                    ) {
                    sb.append(less4jHTTPPort);
                    sb.append(port);
                }
            sb.append(request.getContextPath());
            sb.append(location);
        } else {
            return location;
        }
        return sb.toString();
    };
    
    /**
     * <p>Test wether an HTTP request is idempotent or not, ie: wether it is a 
     * GET request for a resource (cachable) like
     * 
     * <blockquote>
     * <pre>/resource</pre>
     * </blockquote>
     * 
     * or a GET request for an action as
     * 
     * <blockquote>
     * <pre>/resource?action</pre>
     * </blockquote>
     * 
     * wich is not cacheable.</p>
     * 
     * @return true if the request is idempotent, false otherwise
     */
    public boolean httpIdempotent () {
        return (request.getQueryString() == null);
        }
    
    /**
     * Dispatch an idempotent Actor, split its context path (the one
     * between the resource identified and the servlet path) between
     * separators and returns an iterator. Let the accessor decide
     * how to dispatch that stack ...
     *
     */
    public Iterator httpDispatch (HashMap resources) {
        String s = request.getContextPath();
        return Simple.iterator(s.split("/"));
    }
    
    public byte[] httpPOST(int limit) {
        int contentLength = request.getContentLength(); 
        if (contentLength > 0 && contentLength < limit)
            return null;
        
        byte[] body = new byte[contentLength];
        try {
            // first fill that buffer ASAP
            ServletInputStream is = request.getInputStream();
            int len;
            int off = 0;
            while (off < contentLength) {
                len = is.read(body, off, contentLength - off); // block ...
                if (len > -1) {
                    off += len; 
                } else {
                    break; // ... maybe break, but when and what about zero?
                }
            }
            return body;
            
        } catch (IOException ioe) {
            logError(ioe);
            return null;
            
        }
    }
    
    /**
     * <p>Try to send a 200 Ok HTTP response with the appropriate headers
     * for an arbitrary bytes string as body, a given content type and 
     * charset. Audit a successfull response or log an error.</p>
     * 
     * @param body a byte string
     * @param type the resource content type
     * @param charset the character set encoding used (eg: "ASCII")
     */
    public void rest200Ok (byte[] body, String type, String charset) {
        response.setStatus(HttpServletResponse.SC_OK);
        if (charset != null) {type += ";charset=" + charset;}
        response.setContentType(type);
        response.setContentLength(body.length);
        try {
            ServletOutputStream os = response.getOutputStream(); 
            os.write(body);
            os.flush();
            audit(200);
        } catch (IOException e) {
            logError(e);
        }
    }
    
    /**
     * <p>Send a 200 Ok HTTP response with the appropriate headers for
     * a UNICODE string as body, a given content type and charset. This
     * method catches any <code>UnsupportedEncodingException</code> and 
     * uses the plateform default character set if the given encoding is
     * not supported. Audit a successfull response or log an error.</p>
     * 
     * <p>Usage:
     * 
     * <blockquote>
     * <pre>$.res200Ok("&lt;hello-world/&gt;", "text/xml", "ASCII")</pre>
     * </blockquote>
     * 
     * where <code>$</code> is an <code>Actor</code> instance.</p>
     *
     * @param body a string
     * @param type the resource content type
     * @param charset the character set encoding used (eg: "UTF-8")
     */
    public void rest200Ok (String body, String type, String charset) {
        rest200Ok(Simple.encode(body, charset), type, charset);
    }
    
    /**
     * The default content type of resources is XML, the defacto
     * standard supported by XSL, CSS and JavaScript in web 2.0
     * browsers.
     */
    private static final 
    String less4jXMLContentType = "text/xml";
    
    /**
     * Send a 200 Ok HTTP response with the appropriate headers for
     * an XML string using the UTF-8 character set encoding. Audit a
     * successfull response or log an error.
     *
     * <p>Usage:
     * 
     * <blockquote>
     * <pre>$.res200Ok("&lt;hello-world/&gt;")</pre>
     * </blockquote>
     * 
     * where <code>$</code> is an <code>Actor</code> instance.</p>
     *
     * @param body a string
     */
    public void rest200Ok (String body) {
        // String acceptCharset = request.getHeader("Accept-Charset");
        rest200Ok(body, less4jXMLContentType, less4jCharacterSet);
    }
    
    private static final 
    String less4jHTTPLocation = "Location";
    
    /**
     * Try to send a 302 Redirect HTTP response to a location and set the
     * response Content-Type and MIME body. The location may be absolute
     * or relative to this resource. Audit a successfull response or log an 
     * error.
     * 
     * <p>Note that the Content-Type specified must match the body's MIME
     * type and character set encoding.</p> 
     *
     * @param location to redirect to
     * @param body an of bytes with the response body 
     * @param type the Content-Type of the response 
     *
     */
    public void rest302Redirect (String location, byte[] body, String type) {
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.addHeader(less4jHTTPLocation, urlAbsolute(location));
        response.setContentType(type);
        response.setContentLength(body.length);
        try {
            ServletOutputStream os = response.getOutputStream(); 
            os.write(body);
            os.flush();
            audit(302);
        } catch (IOException e) {
            logError(e);
        }
    }
    
    private static final
    String less4jREST302 = (
        "<?xml version=\"1.0\" encoding=\"ASCII\" ?>" +
        "<rest302Redirect/>"
        );
    
    /**
     * <p>Try to send a 302 HTTP Redirect response to the a relative or
     * absolute location with the XML string 
     * 
     * <blockquote>
     * <pre>&lt;rest302Redirect/&gt;</pre>
     * </blockquote>
     * 
     * as body.</p>
     * 
     * <p>Usage:
     * 
     * <blockquote>
     * <pre>$.rest302Redirect("/resource?action")</pre>
     * </blockquote>
     * 
     * Note that this method does <em>not</em> apply the
     * <code>sendRedirect</code> method of <code>HttpServletResponse</code>
     * in order to send its own response body. Instead it uses the 
     * <code>URL</code> convenience to validate a location as an
     * absolute URL.</p>
     * 
     * @param location to redirect to
     */
    public void rest302Redirect(String location) {
        rest302Redirect(
            location, less4jREST302.getBytes(), less4jXMLContentType
            );
    }
    
    /**
     * <p>Bounce to this request's resource if the redirected location 
     * can redirect the user agent in response to the query string
     * completed with this request URL as larst argument.</p>
     * 
     * <p>Usage:
     * 
     * <blockquote>
     * <pre>$.rest302Bounce("https://authority/login", "?url=");</pre>
     * </blockquote>
     * 
     * redirects the user agent to
     * 
     * <blockquote>
     * <pre>GET https://authority/login?url=...</pre>
     * </blockquote>
     * 
     * which may or may not bounce it back to this Actor's request
     * resource.</p>
     * 
     * <p>Note that this function assumes that location is idempotent.</p>
     * 
     * @param location an idempotent location to redirect to
     * @param query the query string before completion 
     */
    public void rest302Bounce(String location, String query) {
        StringBuffer sb = new StringBuffer();
        sb.append(location);
        sb.append(query);
        String here = request.getRequestURL().toString();
        try {
            sb.append(URLEncoder.encode(here, less4jCharacterSet));
        } catch (UnsupportedEncodingException e) {
            sb.append(URLEncoder.encode(here));
        }
        rest302Redirect(sb.toString());
    }

    private static final
    String less4jXJSON = "X-JSON";
    
    public boolean jsonGET(int containers, int iterations) {
        String xjson = request.getHeader(less4jXJSON);
        if (xjson != null) try {
            json = JSON.object(xjson, containers, iterations);
        } catch (JSON.Error e) {;}
        return (json != null);
    }
    
    public boolean jsonGET() {return jsonGET(65355, 65355);}
    
    /**
     * <p>Try to read and parse the body of a POST request, assuming it 
     * contains a content of type <code>application/json</code> encoded
     * in UTF-8 and fitting in a limited buffer. Return true on success or 
     * log an error and return false.</p>
     * 
     * <p>This is a controller's actor and less4j is an framework for 
     * entreprise web interfaces: there is no reason to accept JSON objects 
     * larger than the limit that fits its application.</p>
     * 
     * <p>Using a the servlet getReader method directly is not an option
     * if the application controller must enforce reasonable input limits
     * per request. Here's a good place to do it once for all JSON
     * applications.</p>
     * 
     * @return true if successfull, false otherwise
     */
    public boolean jsonPOST (int limit, int containers, int iterations) {
        /*
         * Instead of parsing a JSON of any size as it is streamed,
         * fill a buffer and instanciate objects only if it is 
         * complete and not overflowed.
         * */
        byte[] body = httpPOST(limit);
        if (body == null) 
            return false; 
        else {
            // parse JSON when the buffer is filled but not overflowed
            try {
                json = JSON.object(
                    new String(body, less4jCharacterSet),
                    containers, iterations
                    );
                return (json != null);
            } catch (Exception e) {
                logError(e);
                return false;
            }
        }
    }
    
    public boolean jsonPOST () {return jsonPOST(16384, 65355, 65355);}
    
    public boolean jsonPOST (
        JSONR pattern, int limit, int containers, int iterations
        ) {
        byte[] body = httpPOST(limit);
        if (body == null) 
            return false; 
        else {
            try {
                json = pattern.object(
                    new String(body, less4jCharacterSet),
                    containers, iterations
                    );
                return (json != null);
            } catch (Exception e) {
                logError(e);
                return false;
            }
        }
    }
    
    public boolean jsonPOST (JSONR model) {
        return jsonPOST(model, 16384, 65355, 65355);
        }
    
    /**
     * The constant content type for JSON includes the UTF-8 character set
     * as only encoding supported by less4j. This web 2.0 is UNICODEd and 
     * its applications may apply only one character set.
     */
    private static final 
    String less4jJSONContentType = "application/json;charset=UTF-8";
    
    public String jsonDigest(Object value) {
        SHA1 md = new SHA1();
        md.update(JSON.str(value).getBytes());
        md.update(salt);
        return md.hexdigest();
    }
    
    /**
     * <p>Try to complete a 200 Ok HTTP/1.X response with the JSON value 
     * encoded in UTF-8 as body and audit the response, or log an error.</p>
     */
    public void json200Ok (Object value) {
        /* the response body must be short enough to be buffered fully */
        byte[] body = Simple.encode(JSON.str(value), less4jCharacterSet);
        int l = body.length;
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(less4jJSONContentType);
        response.setContentLength(l);
        try {
            // response.setBufferSize(16384);
            ServletOutputStream os = response.getOutputStream();
            os.write(body, 0, l);
            os.flush();
            // response.flushBuffer();
            audit(200);
        } catch (IOException e) {
            logError(e);
        }
    }
    
    /**
     * <p>Try to complete a 200 Ok HTTP/1.X response with the actor's JSON 
     * object encoded in UTF-8 as body and audit the response, or log
     * an error.</p>
     */
    public void json200Ok () {json200Ok(json);}
    
    // The simplest URL action dispatcher 
    
    /**
     * Try to open a JDBC connection using the configuration properties,
     * disable AutoCommit. Allways log error, log success only in test mode.
     * 
     * @return true if the connection was successfull, false otherwise
     */
    public boolean sqlOpen () {
        try {
            if (configuration.get(less4jJDBCDriver) != null) {
                sql = DriverManager.getConnection(
                    (String) configuration.get(less4jJDBCURL),
                    (String) configuration.get(less4jJDBCUsername),
                    (String) configuration.get(less4jJDBCPassword)
                    );
            } else {
                sql = ((DataSource) (new InitialContext()).lookup(
                    (String) configuration.get(less4jJDBCDataSource)
                    )).getConnection(); 
            }
        } catch (Exception e) {
            logError(e);
            return false;
        }
        try {
            sql.setAutoCommit(false);
        } catch (SQLException e) {
            logError(e);
            try {sql.close();} catch (SQLException ae) {logError(e);}
            return false;
        }
        if (test) logInfo("connected to the database", TEST);
        return true;
    }

    /**
     * Try to rollback any pending transaction and then close the current 
     * JDBC connection. Allways log error and log success only in test mode.
     */
    public void sqlClose () {
        try {sql.rollback();} catch (SQLException e) {logError(e);}
        try {sql.close();} catch (SQLException e) {logError(e);}
        sql = null;
        if (test) {logInfo("disconnected from the database", TEST);}
    }
    
    /**
     * Copy the JDBC result set into ArrayList of primitive Objects,
     * something to iterate through and index in a closed statement's
     * result, possibly defering that process to I/O time and yield 
     * faster JDBC connections pooling between threads, certainly
     * never taking the risk of leaking DataSource pools.
     * 
     * @param rs
     * @return
     * @throws SQLException
     */
    private static ArrayList jdbc2array (ResultSet rs)
    throws SQLException {
        int i;
        ArrayList rows = null;
        if (rs.next()) {
            Object[] row;
            rows = new ArrayList();
            ResultSetMetaData mt = rs.getMetaData();
            int l = mt.getColumnCount();
            do {
                row = new Object[l];
                for (i = 0; i < l; i++) row[i] = rs.getObject(i);
                rows.add(row);
            } while (rs.next());
        }
        rs.close();
        return rows;
    }
    
    /**
     * Try to query the <code>sql</code> JDBC connection with an SQL
     * statement, fetch the first relation, return an Object array or 
     * null if the result set was empty. In any case, close the statement 
     * JDBC statement to prevent any leak in the connections pool.
     * 
     * @param statement
     * @return an array of Object or null
     * @throws SQLException
     */
    public Object[] sqlQuery (String statement) 
    throws SQLException {
        if (test) logInfo(statement, TEST);
        Object[] row = null;
        Statement st = null;
        try {
            st = sql.createStatement();
            st.setFetchSize(1);
            ResultSet rs = st.executeQuery(statement);
            if (rs.next()) {
                ResultSetMetaData mt = rs.getMetaData();
                int l = mt.getColumnCount();
                row = new Object[l];
                for (int i = 0; i < l; i++) row[i] = rs.getObject(i);
            }
            rs.close();
            st.close(); 
            st = null;
        } finally {
            if (st != null) {
                try {st.close();} catch (SQLException e) {;} 
                st = null;
            }
        }
        return row;
    }
    
    /**
     * Try to query the <code>sql</code> JDBC connection with an SQL
     * statement, return an ArrayList of rows or null if the result
     * set was empty. In any case, close the statement JDBC statement
     * and result set to prevent any leak in the connections pool.
     * 
     * @param statement
     * @return an ArrayList of Object[] or null
     * @throws SQLException
     */
    public ArrayList sqlQuery (String statement, int fetch) 
    throws SQLException {
        if (test) logInfo(statement, TEST);
        ArrayList rows = null;
        Statement st = null;
        try {
            st = sql.createStatement();
            st.setFetchSize(fetch);
            rows = jdbc2array(st.executeQuery(statement));
            st.close(); 
            st = null;
        } finally {
            if (st != null) {
                try {st.close();} catch (SQLException e) {;} 
                st = null;
            }
        }
        return rows;
    }

    /**
     * Try to query the <code>sql</code> JDBC connection with an SQL
     * statement and arguments, return an ArrayList of rows or null if 
     * the result set was empty. In any case, close the statement JDBC 
     * statement and result set to prevent any leak in the connections pool.
     * 
     * @param statement to prepare and execute as a query
     * @param args a primitive array of arguments
     * @return an ArrayList of Object[] or null
     * @throws SQLException
     */
    public ArrayList sqlQuery (String statement, Object[] args, int fetch) 
    throws SQLException {
        if (test) logInfo(statement, TEST);
        ArrayList rows = null;
        PreparedStatement st = null;
        try {
            int i;
            st = sql.prepareStatement(statement);
            st.setFetchSize(fetch);
            for (i = 0; i < args.length; i++) st.setObject(i, args[i]);
            rows = jdbc2array(st.executeQuery(statement));
            st.close(); 
            st = null;
        } finally {
            if (st != null) {
                try {st.close();} catch (SQLException e) {;} 
                st = null;
            }
        }
        return rows;
    }

    private static HashMap jdbc2object (ResultSet rs)
    throws SQLException {
        int i;
        ArrayList row;
        ResultSetMetaData mt = rs.getMetaData();
        int l = mt.getColumnCount();
        HashMap model = new HashMap();
        row = new ArrayList();
        for (i = 0; i < l; i++) row.add(mt.getColumnName(i));
        model.put("columns", row);
        ArrayList rows = new ArrayList();
        while (rs.next()) {
            row = new ArrayList();
            for (i = 0; i < l; i++) row.add(rs.getObject(i));
            rows.add(row);
        }
        model.put("rows", rows);
        rs.close();
        return model;
    }
    
    public HashMap sqlQuery (String statement, ArrayList args, int fetch) 
    throws SQLException {
        if (test) logInfo(statement, TEST);
        HashMap model = null;
        PreparedStatement st = null;
        try {
            st = sql.prepareStatement(statement);
            st.setFetchSize(fetch);
            Iterator iter = args.iterator();
            int i = 0;
            while(iter.hasNext()) {st.setObject(i, iter.next()); i++;}
            ResultSet rs = st.executeQuery(statement);
            model = jdbc2object(rs);
            st.close(); 
            st = null;
        } finally {
            if (st != null) {
                try {st.close();} catch (SQLException e) {;} 
                st = null;
            }
        }
        return model;
    }

    /**
     * Try to query the <code>sql</code> JDBC connection with an SQL
     * statement and named arguments, return a JSONObject model or null if 
     * the result set was empty. In any case, close the statement JDBC 
     * statement and result set to prevent any leak in the connections pool.
     * 
     * @param statement to prepare and execute as a query
     * @param args a MashMap
     * @param names a primitive array of argument names
     * @return a JSONObject with a simple relational model
     * @throws SQLException
     */
    public HashMap sqlQuery (
        String statement, HashMap args, String[] names, int fetch
        ) 
    throws SQLException {
        if (test) logInfo(statement, TEST);
        HashMap model = null;
        PreparedStatement st = null;
        try {
            int i;
            st = sql.prepareStatement(statement);
            st.setFetchSize(fetch);
            for (i = 0; i < names.length; i++) 
                st.setObject(i, args.get(names[i]));
            model = jdbc2object(st.executeQuery(statement));
            st.close(); 
            st = null;
        } finally {
            if (st != null) {
                try {st.close();} catch (SQLException e) {;} 
                st = null;
            }
        }
        return model;
    }

    public HashMap sqlQuery (
        String statement, HashMap args, Iterator names, int fetch
        )
    throws SQLException {
        if (test) logInfo(statement, TEST);
        HashMap model = null;
        PreparedStatement st = null;
        try {
            st = sql.prepareStatement(statement);
            st.setFetchSize(fetch);
            int i = 0;
            while(names.hasNext()) {
                st.setObject(i, args.get(names.next())); 
                i++;
                }
            ResultSet rs = st.executeQuery(statement);
            model = jdbc2object(rs);
            st.close(); 
            st = null;
        } finally {
            if (st != null) {
                try {st.close();} catch (SQLException e) {;} 
                st = null;
            }
        }
        return model;
    }
    
    /**
     * Try to execute and UPDATE, INSERT, DELETE or DDL statement, close
     * the JDBC/DataSource statement, return the number of rows updated.
     * 
     * @param statement the SQL statement to execute
     * @return -1 if the statement failed, 0 if no row update took place, 
     *         or the numbers of rows updated, deleted or inserted.
     * @throws SQLException
     */
    public int sqlUpdate (String statement) throws SQLException {
        int result = -1;
        if (test) logInfo (statement, TEST);
        Statement st = null;
        try {
            st = sql.createStatement(); 
            result = st.executeUpdate(statement);
            st.close();
            st = null;
        } finally {
            if (st != null) try {st.close();} catch (SQLException se) {;}
        }
        return result;
    }

    /**
     * Try to execute a prepared UPDATE, INSERT, DELETE or DDL statement 
     * with a primitive array of arguments, close the JDBC/DataSource 
     * statement and return the number of rows updated.
     * 
     * @param statement the SQL statement to execute
     * @param args the statement arguments
     * @return -1 if the statement failed, 0 if no row update took place, 
     *         or the numbers of rows updated, deleted or inserted.
     * @throws SQLException
     */
    public int sqlUpdate (String statement, Object[] args) 
    throws SQLException {
        int result = -1;
        if (test) logInfo (statement, TEST);
        PreparedStatement st = null;
        try {
            st = sql.prepareStatement(statement);
            for (int i = 0; i < args.length; i++) st.setObject(i, args[i]);
            result = st.executeUpdate();
            st.close();
            st = null;
        } finally {
            if (st != null) try {st.close();} catch (SQLException se) {;}
        }
        return result;
    }
    
    /**
     * Try to execute a prepared UPDATE, INSERT, DELETE or DDL statement 
     * with a JSON array of arguments, close the JDBC/DataSource 
     * statement and return the number of rows updated.
     * 
     * @param statement the SQL statement to execute
     * @param args the statement arguments
     * @return -1 if the statement failed, 0 if no row update took place, 
     *         or the numbers of rows updated, deleted or inserted.
     * @throws SQLException
     */
    public int sqlUpdate (String statement, ArrayList args) 
    throws SQLException {
        int result = -1;
        if (test) logInfo (statement, TEST);
        PreparedStatement st = null;
        try {
            st = sql.prepareStatement(statement);
            Iterator iter = args.iterator();
            int i = 0;
            while(iter.hasNext()) {st.setObject(i, iter.next()); i++;}
            result = st.executeUpdate();
            st.close();
            st = null;
        } finally {
            if (st != null) try {st.close();} catch (SQLException se) {;}
        }
        return result;
    }
    
    /**
     * Try to execute a prepared UPDATE, INSERT, DELETE or DDL statement 
     * with a JSON object of named arguments, close the JDBC/DataSource 
     * statement and return the number of rows updated.
     * 
     * @param statement the SQL statement to execute
     * @param args the JSONObject containing the statement arguments
     * @param names an iterator of argument names
     * @return -1 if the statement failed, 0 if no row update took place, 
     *         or the numbers of rows updated, deleted or inserted.
     * @throws SQLException
     */
    public int sqlUpdate (
        String statement, HashMap args, Iterator names
        ) 
    throws SQLException {
        int result = -1;
        if (test) logInfo (statement, TEST);
        PreparedStatement st = null;
        try {
            st = sql.prepareStatement(statement);
            int i = 0;
            while(names.hasNext()) {
                st.setObject(i, args.get(names.next())); 
                i++;
                }
            result = st.executeUpdate();
            st.close();
            st = null;
        } finally {
            if (st != null) try {st.close();} catch (SQLException se) {;}
        }
        return result;
    }
    
    /**
     * Try to execute a prepared UPDATE, INSERT, DELETE or DDL statement 
     * with a JSON object of named arguments, close the JDBC/DataSource 
     * statement and return the number of rows updated.
     * 
     * @param statement the SQL statement to execute
     * @param args the JSONObject containing the statement arguments
     * @param names a primitive array of argument names
     * @return -1 if the statement failed, 0 if no row update took place, 
     *         or the numbers of rows updated, deleted or inserted.
     * @throws SQLException
     */
    public int sqlUpdate (
        String statement, HashMap args, Object[] names
        ) 
    throws SQLException {
        return sqlUpdate(statement, args, Simple.iterator(names));
    }
    
    public ArrayList sqlUpdate (String statement, ArrayList args, int id) 
    throws SQLException {return new ArrayList();}
    
    public ArrayList sqlUpdate (
        String statement, HashMap args, Iterator names, int id
        ) 
    throws SQLException {return new ArrayList();}
            
    public int sqlBatch (String statement, ArrayList rows) 
        throws SQLException {return 0;}
                
    /**
     * Try to open a new connection (establish a new "initial directory 
     * context" in j-speak) to the LDAP server configured, using a
     * given principal and credentials. Catch any JNDI exception, return 
     * true on success and false otherwise, log an information message
     * in test mode.
     * 
     * @param principal an LDAP user name
     * @param credentials the associated password  
     * @return true if the connection was successfull, false otherwise
     */
    public boolean ldapOpen (Object principal, Object credentials) {
        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, less4jLDAPCtxFactory);
        env.put(Context.PROVIDER_URL, configuration.get(less4jLDAPURL));
        env.put(Context.SECURITY_AUTHENTICATION, less4jLDAPSecurity);
        env.put(Context.SECURITY_PRINCIPAL, principal);
        env.put(Context.SECURITY_CREDENTIALS, credentials);  
        try {
            ldap = new InitialDirContext(env);
        } catch (NamingException e) {
            logError(e);
            return false;
        }
        if (test) logInfo("connected to the directory", TEST);
        return true;
    }

    /**
     * Try to open an LDAP connection using the configuration properties,
     * principal and credentials. Catch any JNDI exception, return 
     * true on success and false otherwise, log an information message
     * in test mode.
     * 
     * @return true if the connection was successfull, false otherwise
     */
    public boolean ldapOpen () {
        return ldapOpen(
            configuration.get(less4jLDAPPrincipal), 
            configuration.get(less4jLDAPCredentials)
            );
    }
    
    /**
     * Try to close the current LDAP connection. Catch and log error any
     * JNDI exception, log success in test mode.
     */
    public void ldapClose () {
        try {ldap.close();} catch (NamingException e) {logError(e);}
        ldap = null;
        if (test) logInfo("disconnected from the directory", TEST);
    }
    
    /**
     * Try to update a JSON object with the attributes of an LDAP context, 
     * return true if the context's name was resolved, false otherwise.
     * Attributes not named in the names iterator are filtered out.
     * 
     * @param dn the distinguished name to resolve
     * @param object the JSONObject to update
     * @param names of the attribute values to get
     * @return true if the name was resolved, false otherwise
     */
    public boolean ldapResolve (
        String dn, HashMap object, Iterator names
        ) {
        if (test) logInfo ("resolve LDAP dn=" + dn, TEST);
        Attributes attributes;
        try {
            attributes = ldap.getAttributes(dn);
        } catch (NamingException e) {
            logError(e);
            return false;

        }
        String key;
        while (names.hasNext()) {
            key = (String) names.next();
            object.put(key, attributes.get(key));
        }
        return true;
        
    }
    
    /**
     * Try to update a JSON object with the attributes of an LDAP context, 
     * return true if the context's name was resolved, false otherwise.
     * Attributes not named in the original JSON object are filtered out.
     * 
     * @param dn the distinguished name to resolve
     * @param object the JSONObject to update
     * @return true if the name was resolve, false otherwise
     */
    public boolean ldapResolve (String dn, HashMap object) {
        return ldapResolve(dn, object, object.keySet().iterator());
    }
    
    /**
     * Try to create an LDAP context with attributes values from a JSON 
     * object for the given attribute names, return true if the context 
     * was created, false otherwise.
     * 
     * @param dn the distinguished name of the context created
     * @param object the JSON object from which to set the attribute values
     * @param names of the attributes to create
     * @return true, false
     */
    public boolean ldapCreate (
        String dn, HashMap object, Iterator names
        ) {
        if (test) logInfo ("create LDAP dn=" + dn, TEST);
        String key;
        Object value;
        BasicAttribute attribute;
        BasicAttributes attributes = new BasicAttributes (false); 
        while (names.hasNext()) {
            key = (String) names.next();
            attribute = new BasicAttribute (key);
            value = object.get(key);
            if (value == null) {
                ;
            } else if (value instanceof ArrayList) {
                Object item;
                Iterator j = ((ArrayList) object.get(key)).iterator();
                while (j.hasNext()) {
                    item = j.next();
                    if (
                        item instanceof String ||
                        item instanceof Integer ||
                        item instanceof Double ||
                        item instanceof Boolean
                        ) {
                        attribute.add(item);
                    }
                }
            } else if (
                value instanceof String ||
                value instanceof Integer ||
                value instanceof Double ||
                value instanceof Boolean
                ) {
                attribute.add(value);
            }
            attributes.put(key, attribute);
        }
        try {
            ldap.createSubcontext(dn, attributes);
            return true;
            
        } catch (NamingException e) {
            logError(e);
            return false;
            
        }
    }
    
    /**
     * Try to create an LDAP context from a JSON object, return true if the 
     * context was created, false otherwise.
     * 
     * @param dn the distinguished name of the context created
     * @param object the JSON object from which to set the attribute values
     * @return true, false
     */
    public boolean ldapCreate (String dn, HashMap object) {
        return ldapCreate (dn, object, object.keySet().iterator());
    }
    
    public boolean ldapUpdate (
        String dn, HashMap object, Iterator names
        ) {
        if (test) logInfo("update LDAP dn=" + dn, TEST);
        return true;
    }
    
    public boolean ldapUpdate (String dn, HashMap object) {
        return ldapUpdate(dn, object, object.keySet().iterator());
    }
    
    /* that's all folks */
    
}

/*
 *  TODO: add validation of actions and objects against simple maps of
 *        a name to a type caster and a regular expression pattern:
 *        
 *            {"name": [caster, pattern]}
 *  
 *        enough to test most input constraints.
 *        
 *        Note that this is specialy usefull with a stack of regular
 *        expression and types, using sample data to find the strictest
 *        matching pattern and type in a test case.
 *        
 */
