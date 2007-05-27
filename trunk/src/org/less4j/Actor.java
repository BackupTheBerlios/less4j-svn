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

// import java.util.HashMap;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

//import java.net.URLEncoder;

import java.io.IOException;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import javax.sql.DataSource;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
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
 * <p>The <code>Actor</code> provides a rich state and a flat "full-stack" 
 * API to develop complex entreprise Web 2.0 controllers of SQL databases 
 * and LDAP directories.</p>
 * 
 * <h3>Table of Content</h3>
 * 
 * <p>The actor members and methods are grouped by aspects of its
 * applications as follow:</p>
 * 
 * <dl>
 * <di><dt>Environment:
 * <code>test</code>, 
 * <code>configuration</code>, 
 * <code>logOut</code>, 
 * <code>logInfo</code>, 
 * <code>logError</code>.
 * <code>logAudit</code>, 
 * </dt><dd>
 * Test or production environment, standard output and error. No fancy 
 * logging formats and long stack traces, the bare minimum for all actor: 
 * STDOUT and STDERR, a compact exception trace and unconstrained logging 
 * categories for information.
 * <dd></di>
 * <di><dt>IRTD2:
 * <code>salts</code>,
 * <code>identity</code>,
 * <code>rights</code>,
 * <code>time</code>,
 * <code>digest</code>,
 * <code>digested</code>,
 * <code>irtd2Digest</code>, 
 * <code>irtd2Digested</code>, 
 * </dt><dd>
 * Identification, authorization and audit of the user agent, without
 * the woes of <code>HttpServletSession</code>. A practical implementation
 * of a simple protocol based on HTTP cookies and SHA1 digest, cross
 * plateform and designed to let the weight of persistent sessions be
 * distributed on the clients. Eventually, it also provides an effective
 * audit follow the multiple paths of interactions of a single user and
 * detect fraud attemps.
 * </dd></di>
 * <di><dt>HTTP:
 * <code>url</code>,
 * <code>context</code>,
 * <code>request</code>,
 * <code>response</code>,
 * <code>httpError</code>,
 * <code>http200Ok</code>, 
 * <code>http302Redirect</code>,
 * </dt><dd>
 * ...
 * </dd></di>
 * <di><dt>Regular JSON:
 * <code>json</code>,
 * <code>jsonGET</code>,
 * <code>jsonPOST</code>,
 * <code>jsonDigest</code>,
 * <code>jsonResponse</code>,
 * </dt><dd>
 * ...
 * </dd></di>
 * <di><dt>SQL:
 * <code>sql</code>,
 * <code>sqlOpenJ2EE</code>,
 * <code>sqlOpenJDBC</code>,
 * <code>sqlClose</code>,
 * <code>sqlQuery</code>, 
 * <code>sqlTable</code>, 
 * <code>sqlObject</code>, 
 * <code>sqlObjects</code>, 
 * <code>sqlCollection</code>, 
 * <code>sqlUpdate</code>,
 * <code>sqlUpdateMany</code>.
 * </dt><dd>
 * Enough SQL conveniences up to all the Object Relational Mapping you
 * will ever need for JSON applications: table, relations, collection and
 * object(s). 
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
 * @version 0.30
 */
public class Actor {
    
    protected static final String less4j = "less4j";
    
    /**
     * <p>The default charset encoding in less4j, something supported by
     * web 2.0 browsers (IE6, Firefox, etc ...) and java runtime
     * environments. Also the defacto standard for JSON encoding.</p>
     */
    protected static final String less4jCharacterSet = "UTF-8";
    
    /**
     * A copy of the servlet controller's configuration, mapping all
     * options listed in the servlet's XML configuration file and whose 
     * name start with <code>"less4j"</code>. 
     */
    public JSON.Object configuration;

    /**
     * A boolean that indicates wether the Actor runtime environment is
     * a test or a production one. It is set to <code>false</code> by
     * defautlt.
     */
    public boolean test = false;
    
    /**
     * A usefull copy of <code>request.getContextPath()</code> to build
     * dispatch the URL or redirect relatively to this servlet's context.
     */
    public String context;
    
    /**
     * The <code>HttpServletRequest</code> handled by this Actor instance.
     */
    public HttpServletRequest request;
    
    /**
     * The <code>HttpServletResponse</code> completed by this Actor instance.
     */
    public HttpServletResponse response;

    /**
     * The IRTD2 salts, encoded as 8-bit bytes
     */
    public byte[][] salts = null;
    
    /**
     * A usefull copy of <code>request.getRequestURL().toString()</code>
     * to quickly dispatch the request's URL through simple String tests.
     */
    public String url;
    
    /**
     * The IRDT2 state for this actor's actions as an US ASCII string.
     */
    public String irtd2 = "";
    
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
     * The time of this Actor's, in seconds since epoch, as an java
     * <code>int</code> value 
     * (ie: 01/01/1970)  
     */
    public long time = System.currentTimeMillis()/1000;
    
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
     * The JSON object associated with the Actor's request and response.
     */
    public JSON.Object json = null;
    
    /**
     * An open JDBC connection or <code>null</code>.
     */
    public Connection sql = null;
    
    /**
     * An open LDAP connection or <code>null</code>.
     */
    public InitialDirContext ldap = null;
    
    /**
     * Serialize this Actor's public state to a JSON <code>StringBuffer</code>
     */
    public StringBuffer strb(StringBuffer sb) {
        sb.append("{\"time\":");
        sb.append(time);
        sb.append(",\"identity\":");
        JSON.strb(sb, identity);
        sb.append(",\"rights\":");
        JSON.strb(sb, rights);
        sb.append(",\"digest\":");
        JSON.strb(sb, digest);
        sb.append(",\"digested\":");
        JSON.strb(sb, digested);
        sb.append(",\"url\":");
        JSON.strb(sb, url);
        sb.append(",\"context\":");
        JSON.strb(sb, context);
        sb.append(",\"json\":");
        JSON.strb(sb, json);
        sb.append('}');
        return sb;
    }
    
    /**
     * Return a JSON <code>String</code> of this Actor's public state.
     * 
     * <h4>Synopsis</h4>
     * 
     * <blockquote>
     * <pre>$.json200Ok ($.toString());</pre>
     * </blockquote>
     * 
     * <p>The string returned matches this JSONR pattern:
     * 
     * <blockquote>
     * <pre>{
     *    "identity": "$[0-9a-zA-Z]+^", 
     *    "rights": "$[0-9a-zA-Z]+^",
     *    "time": 0, 
     *    "digest": "[0-9a-f]{20}",
     *    "digested": "[0-9a-f]{20}",
     *    "about": [""],
     *    "json": null
     *}</pre>
     * </blockquote>
     * 
     * <p>Note that it does not include the controller's private part
     * of its state: configuration, salt, salted, sql or ldap.</p>
     */
    public String toString() {
        return strb(new StringBuffer()).toString();
    }
    
    /**
     * Initialize a new Actor to test a controller servlet's configuration 
     * when it is initialized.
     * 
     * @param conf the controller's configuration HashMap
     */
    public Actor (JSON.Object conf) {
        test = conf.B("test", false);
        configuration = conf;
    }
    
    /**
     * Initialize a new Actor to handle an HTTP request and response, 
     * set the Actor's audit digest salt and eventually load ...
     * 
     * @param conf the controller's configuration HashMap
     * @param req the HTTP request to handle 
     * @param res the HTTP response to complete
     */
    public Actor (
        JSON.Object conf, HttpServletRequest req, HttpServletResponse res
        ) {
        test = conf.B("test", false);
        configuration = conf;
        request = req;
        response = res;
        url = request.getRequestURL().toString();
        context = request.getContextPath() + '/';
        try {
            JSON.Array _salts = configuration.A("irtd2Salts");
            int L=_salts.size();
            salts = new byte[L][];
            for (int i=0; i<L; i++)
                salts[i] = _salts.S(i).getBytes();
        } catch (JSON.Error e) {
            logError(e);
        };
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
     * other usefull audit information, from outside your application 
     * where it belong.</p>
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
    
    /**
     * <p>Write a compact stack trace to STDERR in the "StackTrace" category,
     * in one line, like:
     * 
     * <blockquote>
     * <pre>StackTrace: 34|thrower.called 12|catcher.calling</pre>
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
     * <pre>StackTrace: error 34|thrower.called 12|catcher.calling {...}</pre>
     * </blockquote>
     *
     * This is what the helpdesk needs and it still fits on one line.</p>
     *
     * @param error the throwable instance catched
     * 
     */
    public void logError (Throwable error) {
        StackTraceElement[] stacktrace = error.getStackTrace();
        StackTraceElement catcher, thrower;
        int i = 0;
        do {
            thrower = stacktrace[i++];
        } while (thrower.getLineNumber() == -1 && i < stacktrace.length);
        catcher = stacktrace[i];
        StringBuffer sb = new StringBuffer();
        sb.append(stackTraceCategory);
        sb.append(error.getMessage());
        sb.append(' ');
        sb.append(thrower.getLineNumber());
        sb.append('|');
        sb.append(thrower.getClassName());
        sb.append('.');
        sb.append(thrower.getMethodName());
        sb.append(' ');
        sb.append(catcher.getLineNumber());
        sb.append('|');
        sb.append(catcher.getClassName());
        sb.append('.');
        sb.append(catcher.getMethodName());
        if (!test && json != null) {
            sb.append(' ');
            JSON.strb(sb, json);
        }
        System.err.println(sb.toString());
    }
    
    protected static String logLESS4J = "LESS4J: ";
    
    /** 
     * Log an audit of this HTTP request and response in one line. 
     * 
     * <p>For instance:
     * 
     * <blockquote>
     * <pre>LESS4J: identity roles time digested digest GET url HTTP/1.1 200</pre>
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
    public void logAudit (int status) {
        StringBuffer sb = new StringBuffer();
        sb.append(logLESS4J);
        sb.append(request.getMethod());
        sb.append(' ');
        sb.append(request.getRequestURI());
        String query = request.getQueryString();
        if (query != null) sb.append(query);
        sb.append(' ');
        sb.append(request.getProtocol());
        sb.append(' ');
        sb.append(status);
        sb.append(' ');
        sb.append(irtd2);
        logOut(sb.toString());
    }
    
    protected static final String irtd2Name = "IRTD2";
    
    protected static Pattern irtd2Split = Pattern.compile(":");
    
    /**
     * Try to collect a IRTD2 cookie in the request and test its digest 
     * against the secret(s) set by configuration for this actor's controller, 
     * digest a new cookie only if a the digested cookie is still valid in 
     * time and bears the signature of this servlet, return false otherwise. 
     * 
     * <p>There are four benefits to expect from IRTD2 cookies for
     * J2EE public applications:</p>
     * 
     * <ol>
     * <li>Remove the weight of statefull sessions in the J2EE container.</li>
     * <li>Distribute the load of authorization on a cluster of servers 
     * without adding more contention and latency.</li>
     * <li>Trace identifed and authorized interactions in sequence.</li>
     * <li>Audit impersonation exploit by a man-in-the-middle.</li>
     * </ol>
     * 
     * <p>Note that it does <em>not</em> prevent cookie theft but it
     * does the only next-best actually possible for a public network
     * application: detect and report fraudulent actions.</p>
     * 
     * @param timeout the limit of an IRTD2 cookie's age, in seconds  
     * @return true if the request failed to be authenticated
     */
    public boolean irtd2Digested (int timeout) {
        try {
            int i; 
            /* get the request's IRTD2 authorization cookies ... */
            Cookie irtd2Cookie = null; 
            Cookie[] cookies = request.getCookies();
            if (cookies != null) for (i = 0; i < cookies.length; i++) {
                if (cookies[i].getName().equals(irtd2Name)) {
                    irtd2Cookie = cookies[i]; break;
                }
            }
            if (irtd2Cookie == null) {
                if (test) logInfo("Not Found", "IRTD2");
                return false;  
            }
            /* unpack the IRTD2 Cookie */
            irtd2 = irtd2Cookie.getValue();
            Iterator tokens = Simple.split(irtd2, ' ');
            identity = (String) tokens.next();
            rights = (String) tokens.next();
            String lastTime = (String) tokens.next();
            digest = (String) tokens.next();
            digested = (String) tokens.next();
            if (((int)(time - Long.parseLong(lastTime))) > timeout) {
                if (test) logInfo("Timeout", "IRTD2");
                return false; 
            } 
            /* get the IRTD 8-bit bytes from the IRTD2 UNICODE string */
            byte[] irtd = irtd2.substring(
                0, identity.length() + 1 + rights.length() + 1 + 
                lastTime.length() + 1 + digested.length()
                ).getBytes();
            /* digest an SHA1 hexadecimal with one of the salts or fail */
            String d = null;
            for (i=0; i<salts.length; i++) {
                SHA1 md = new SHA1();
                md.update(irtd);
                md.update(salts[i]);
                d = md.hexdigest();
                if (d.equals(digested)) break;
            }
            if (!digested.equals(d)) {
                if (test) logInfo("Not Digested", "IRTD2");
            } else
                return true; // digested in time with salt! 
        } 
        catch (Exception e) { // just in case ... still beta
            logError(e);
        }
        return false; // exception or invalid.
    }
    
    /**
     * Literally "digest a cookie", transform the IRTD2 cookie sent with
     * the request into a new cookie bearing the Actor's time, to be sent
     * with the response.
     * 
     * <p>The cookie value is a formatted string made as follow:</p>
     * 
     * <blockquote>
     * <pre>Cookie: IRTD2=<strong>identity roles time digested digest</strong>; </pre>
     * </blockquote>
     * 
     * <p>where <code>identity</code> and <code>roles</code> are respectively
     * Public Names and netstrings of 7-bit ASCII characters only, followed
     * by the controller's <code>time</code> representation and two SHA1 hex
     * digests: the client's last digest for this cookie and the one computed 
     * from the byte string that precedes it.</p>
     * 
     * <p>This method is usefull in authorization controllers, like user
     * identification or roles attribution services.</p>
     *
     * @param path in which the IRTD2 Cookie's is applied
     */
    public void irtd2Digest() {
        StringBuffer sb = new StringBuffer();
        SHA1 md = new SHA1();
        sb.append(identity);
        sb.append(' ');
        sb.append(rights);
        sb.append(' ');
        sb.append(Long.toString(time));
        sb.append(' ');
        if (digested != null) sb.append(digested);
        String irtd = sb.toString();
        md.update(irtd.getBytes());
        md.update(salts[0]);
        digest = md.hexdigest();
        sb = new StringBuffer();
        sb.append(irtd);
        sb.append(' ');
        sb.append(digest);
        irtd2 = sb.toString();
        // This sucks because of ... 
        Cookie ck = new Cookie(irtd2Name, irtd2);
        ck.setDomain(request.getServerName());
        ck.setPath(context);
        ck.setMaxAge(Integer.MAX_VALUE); // ... devilish details.  
        response.addCookie(ck);
    }
    
    protected static final Pattern ascii7bit = Pattern.compile(
        "$[\\x21-\\x7E]+^" // ASCII 7-bit string
        );
    
    protected static final Pattern httpPreferences = 
        Pattern.compile("^(.*?)(;.+?=.+?)?((,.*?)(/s*?;.+?=.+?)?)*$");
    
    protected static final String urlHTTP = "http";
    protected static final String urlHTTPS = "https";
    protected static final String urlHTTPHost = "://";
    
    protected void urlDomain(StringBuffer sb) {
        int port;
        String protocol;
        protocol = request.getProtocol().split("/")[0].toLowerCase();
        sb.append(protocol);
        sb.append(urlHTTPHost);
        sb.append(request.getServerName());
        port = request.getServerPort();
        if (
            (protocol.equals(urlHTTP) && port != 80) ||
            (protocol.equals(urlHTTPS) && port != 443)
            ) {
            sb.append(':');
            sb.append(port);
        }
    } 
    
    protected static final Pattern urlHTTPAbsolute = 
        Pattern.compile("https?:.*//.+"); 
    
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
     * to the resource context path
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
    public String httpLocation(String location) {
        if (location.length() == 0)
            return url;
        
        StringBuffer sb;
        char first = location.charAt(0);
        if (first == '?') {
            sb = new StringBuffer(); sb.append(url); sb.append(location);
        } else if (first == '/') {
            sb = new StringBuffer(); urlDomain(sb); sb.append(location);
        } else if (urlHTTPAbsolute.matcher(location).matches())
            return location;
            
        else {
            sb = new StringBuffer();
            urlDomain(sb);
            sb.append(context);
            sb.append('/');
            sb.append(location);
        }
        return sb.toString();
    };
    
    public byte[] httpPOST(int limit) {
        int contentLength = request.getContentLength(); 
        if (contentLength < 1 || contentLength > limit)
            return null; // don't do null or excess data
        byte[] body = new byte[contentLength];
        try { // fill that buffer ASAP
            ServletInputStream is = request.getInputStream();
            int len;
            int off = 0;
            while (off < contentLength) {
                len = is.read(body, off, contentLength - off);
                if (len > -1)
                    off += len; 
                else if (len == 0)
                    Thread.yield(); // ... wait for input ...
                else break;
            }
            return body;
        } catch (IOException ioe) {
            logError(ioe); return null;
        }
    }
    
    /**
     * <p>Try to send an HTTP error response, rely on the J2EE implementation
     * to produce headers and body. Audit a successfull response or log an 
     * error.</p>
     * 
     * @param code HTTP error to send
     */
    public void httpError (int code) {
        try {
            response.sendError(code); logAudit(code);
        } catch (IOException e) {
            logError(e);
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
    public void http200Ok (byte[] body, String type, String charset) {
        response.setStatus(HttpServletResponse.SC_OK);
        if (charset != null) type += ";charset=" + charset;
        // if (charset != null) response.setCharacterEncoding(charset);
        response.setContentType(type);
        response.setContentLength(body.length);
        try {
            ServletOutputStream os = response.getOutputStream(); 
            os.write(body);
            os.flush();
            logAudit(200);
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
     * <pre>$.http200Ok("&lt;hello-world/&gt;", "text/xml", "ASCII")</pre>
     * </blockquote>
     * 
     * where <code>$</code> is an <code>Actor</code> instance.</p>
     *
     * @param body a string
     * @param type the resource content type
     * @param charset the character set encoding used (eg: "UTF-8")
     */
    public void http200Ok (String body, String type, String charset) {
        http200Ok(Simple.encode(body, charset), type, charset);
    }
    
    /**
     * The default content type of resources is XML, the defacto
     * standard supported by XSL, CSS and JavaScript in web 2.0
     * browsers.
     */
    protected static final String htmlContentType = "text/html";
    
    protected static final String httpLocation = "Location";
    
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
     */
    protected void http302Redirect (String location, byte[] body, String type) {
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.addHeader(httpLocation, httpLocation(location));
        response.setContentType(type);
        response.setContentLength(body.length);
        try {
            ServletOutputStream os = response.getOutputStream(); 
            os.write(body);
            os.flush();
            logAudit(302);
        } catch (IOException e) {
            logError(e);
            logAudit(500); // TODO: ? work out "own" error code ? 
        }
    }
    
    protected static final String http302RedirectXML = (
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
    public void http302Redirect(String location) {
        http302Redirect(
            location, http302RedirectXML.getBytes(), htmlContentType
            );
    }
    
    /**
     * Send a 200 Ok HTTP response with the appropriate headers for
     * an XML string using the UTF-8 character set encoding. Audit a
     * successfull response or log an error.
     *
     * <p>Usage:
     * 
     * <blockquote>
     * <pre>$.html200Ok("&lt;hello-world/&gt;")</pre>
     * </blockquote>
     * 
     * where <code>$</code> is an <code>Actor</code> instance.</p>
     *
     * @param body a string
     */
    public void html200Ok (String body) {
        http200Ok(
            Simple.encode(body, less4jCharacterSet), 
            htmlContentType, 
            less4jCharacterSet
            );
    }
    
    protected static final String jsonXJSON = "X-JSON";
    
    /**
     * 
     * @param containers
     * @param iterations
     * @return
     */
    public boolean jsonGET(int containers, int iterations) {
        json = new JSON.Object();
        Map query = request.getParameterMap();
        String name;
        String[] strings;
        Iterator iter = query.keySet().iterator();
        containers--;
        while (containers > 0 && iterations > 0 && iter.hasNext()) {
            name = (String) iter.next();
            strings = (String[]) query.get(name);
            if (strings != null)
                if (strings.length > 1) { 
                    containers--;
                    JSON.Array list = new JSON.Array();
                    for (int i=0; i < strings.length; i++) { 
                        list.add(strings[i]); iterations--;
                    }
                    json.put(name, list);
                } else if (strings.length > 0)
                    json.put(name, strings[0]);
                else
                    json.put(name, null);
            iterations--;
        }
        String xjson = request.getHeader(jsonXJSON);
        if (xjson != null) {
            JSON parse = new JSON(containers, iterations);
            JSON.Error e = parse.update(json, xjson); 
            if (e != null) {logError(e); return false;}
        }
        return true;
    }
    
    /**
     * 
     * @param containers
     * @param iterations
     * @param type
     * @return
     */
    public boolean jsonGET(int containers, int iterations, JSONR.Type type) {
        json = new JSON.Object();
        Map query = request.getParameterMap();
        HashMap namespace = ((JSONR.TypeNamespace) type).namespace;
        JSONR.Type pattern;
        String name;
        String[] strings;
        Iterator iter = query.keySet().iterator();
        containers--;
        while (containers > 0 && iterations > 0 && iter.hasNext()) try {
            name = (String) iter.next();
            pattern = (JSONR.Type) namespace.get(name);
            strings = (String[]) query.get(name);
            if (pattern != null && strings != null) 
                if (strings.length > 1) { 
                    containers--;
                    JSON.Array list = new JSON.Array();
                    for (int i=0; i < strings.length; i++) { 
                        list.add(pattern.eval(strings[i])); iterations--;
                    }
                    json.put(name, list);
                } else if (strings.length > 0)
                    json.put(name, pattern.eval(strings[0]));
                else
                    json.put(name, null);
            iterations--;
        } catch (JSON.Error e) {logError(e); return false;}
        String xjson = request.getHeader(jsonXJSON);
        if (xjson != null) {
            JSONR validate = new JSONR(type, containers, iterations);  
            JSON.Error e = validate.update(json, xjson); 
            if (e != null) {logError(e); return false;}
        }
        return true;
    }
    
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
        if (body == null) return false; else try {
            // parse JSON when the buffer is filled but not overflowed
            json = (new JSON(containers, iterations)).object(
                new String(body, less4jCharacterSet)
                );
            return (json != null);
        } catch (Exception e) {
            logError(e);
            return false;
        }
    }
    
    /**
     * 
     * @param limit
     * @param containers
     * @param iterations
     * @param type
     * @return
     */
    public boolean jsonPOST (
        int limit, int containers, int iterations, JSONR.Type type
        ) {
        byte[] body = httpPOST(limit);
        if (body == null) return false; else try {
            json = (
                new JSONR(type, containers, iterations)
                ).object(new String(body, less4jCharacterSet));
            return true;
        } catch (Exception e) {
            logError(e);
            return false;
        }
    }
    
    /**
     * 
     * @param digestedName
     * @param digestName
     */
    public void jsonDigest(String digestedName, String digestName) {
        byte[] buff = JSON.encode(json.get(digestedName)).getBytes();
        SHA1 md = new SHA1();
        md.update(buff);
        md.update(salts[0]);
        json.put(digestName, md.hexdigest());
    }
    
    public boolean jsonDigested(String digestedName, String digestName) {
        String sign = json.S(digestName, "");
        byte[] buff = JSON.encode(json.get(digestedName)).getBytes();
        String d = null;
        for (int i=0; i<salts.length; i++) {
            SHA1 md = new SHA1();
            md.update(buff);
            md.update(salts[i]);
            d = md.hexdigest();
            if (d.equals(sign)) return true;
        }
        return false;
    }
    
    /**
     * The constant content type for JSON includes the UTF-8 character set
     * as only encoding supported by less4j. This web 2.0 is UNICODEd and 
     * its applications may apply only one character set.
     */
    protected static final String jsonContentType = 
        "text/javascript;charset=UTF-8";
    
    /**
     * <p>Try to complete an HTTP/1.X response <code>code</code> with a byte
     * string as body and audit the response, or log an error.</p>
     */
    public void jsonResponse (int code, byte[] body) {
        /* the response body must be short enough to be buffered fully */
        response.setStatus(code);
        response.setContentType(jsonContentType);
        response.setContentLength(body.length);
        try {
            // response.setBufferSize(16384);
            ServletOutputStream os = response.getOutputStream();
            os.write(body);
            os.flush();
            // response.flushBuffer();
            logAudit(code);
        } catch (IOException e) {
            logError(e);
        }
    }
    
    /**
     * <p>Try to complete an HTTP/1.X response <code>code</code> with a 
     * string encoded in UTF-8 as body and audit the response, or log an 
     * error.</p>
     */
    public void jsonResponse (int code, String body) {
        jsonResponse(code, Simple.encode(body, less4jCharacterSet));
    }
    
    /**
     * <p>Try to complete an HTTP/1.X response <code>code</code> with the 
     * actor's JSON object encoded in UTF-8 as body and audit the response, 
     * or log an error.</p>
     */
    public void jsonResponse (int code) {
        jsonResponse(200, JSON.encode(json));
    }
    /**
     * Try to open a J2EE datasource and disable AutoCommit, return 
     * <code>true</code> and if in "test" mode, log information, or
     * return <code>false</code> and log error.
     * 
     * @return true if the connection was successfull, false otherwise
     */
    public boolean sqlOpenJ2EE (String datasource) {
        try {
            sql = (
                (DataSource) (new InitialContext()).lookup(datasource)
                ).getConnection(); 
        } catch (Exception e) {
            logError(e);
            return false;
        }
        if (test) logInfo("connected to J2EE datasource", less4j);
        try {
            sql.setAutoCommit(false);
        } catch (SQLException e) {
            logError(e);
            try {sql.close();} catch (SQLException ae) {logError(e);}
            return false;
        }
        return true;
    }

    /**
     * Try to open a JDBC connection from its URL, with the given username
     * and password, disable AutoCommit. Allways log error, log success only 
     * in test mode.
     * 
     * @return true if the connection was successfull, false otherwise
     */
    public boolean sqlOpenJDBC (
        String dburl, String username, String password
        ) {
        try {
            sql = DriverManager.getConnection(dburl, username, password);
        } catch (Exception e) {
            logError(e);
            return false;
        }
        if (test) logInfo("connected to JDBC database", less4j);
        try {
            sql.setAutoCommit(false);
        } catch (SQLException e) {
            logError(e);
            try {sql.close();} catch (SQLException ae) {logError(e);}
            return false;
        }
        return true;
    }

    /**
     * Try to rollback any pending transaction and then close the current 
     * JDBC connection. Allways log error and log success only in test mode.
     */
    public void sqlClose () {
        // I suppose that rolling-back when commit has just been called
        // is an error condition that can be handled properly by the
        // JDBC driver and *not* yield one more network transaction ... 
        //
        // TODO: test it!
        //
        // The alternative is to raise a flag for each update, add a
        // sqlCommit action that lower this flag and test for it here
        // before eventually rolling back an "pending" transaction.
        //
        try {sql.rollback();} catch (SQLException e) {logError(e);}
        try {sql.close();} catch (SQLException e) {logError(e);}
        sql = null;
        if (test) {logInfo("disconnected from SQL", less4j);}
    }
    
    // missing closures ...
    
    /**
     * Try to query the <code>sql</code> JDBC connection with an SQL
     * statement and an argument iterator, use an RS2 collector to return 
     * a JSON.Array, a JSON.Object or null if the result set was empty.
     * 
     * <h4>Synopsis</h4>
     * 
     * <pre>try {
     *    JSON.Array relations = (JSON.Array) $.<strong>sqlQuery(</strong>
     *        "select * from TABLE wher COLUMN=?", 
     *        Simple.iterator (new String[]{"criteria"}),
     *        100, Actor.sql2relations
     *        <strong>)</strong>;
     *} catch (SQLException e) {
     *    $.logError(e);
     *}</pre>
     * 
     * @param statement to prepare and execute as a query
     * @param args an iterator through arguments
     * @param fetch the number of rows to fetch
     * @param collector the RS2 instance used to collect the result set
     * @return an <code>ArrayList</code>, a <code>HashMap</code> or null
     * @throws SQLException
     */
    public Object sqlQuery (
        String statement, Iterator args, int fetch, SQL.ORM collector
        ) 
    throws SQLException {
        if (test) logInfo(statement, "SQL");
        Object result = null;
        PreparedStatement st = null;
        try {
            st = sql.prepareStatement(statement);
            st.setFetchSize(fetch);
            int i = 1; 
            while (args.hasNext()) {st.setObject(i, args.next()); i++;}
            result = collector.jdbc2(st.executeQuery());
            st.close();
            st = null;
        } finally {
            if (st != null) {
                try {st.close();} catch (SQLException e) {;}
                st = null;
            }
        }
        return result;
    }

    /**
     * Try to query the <code>sql</code> JDBC connection with an SQL
     * statement and an argument array, return a <code>JSON.Object</code>
     * with the obvious "columns" and "rows" members, or <code>null</code> 
     * if the result set was empty.
     * 
     * @param statement to prepare and execute as a query
     * @param arguments an array of <code>String[]</code> 
     * @param fetch the number of rows to fetch
     * @return a <code>JSON.Object</code> or null 
     * @throws SQLException
     */
    public JSON.Object sqlTable (
        String statement, String[] arguments, int fetch
        ) 
    throws SQLException {
        return (JSON.Object) sqlQuery (
            statement, Simple.itermap(json, arguments), fetch, SQL.table
            );
    }
    
    /**
     * Try to query the <code>sql</code> JDBC connection with an SQL
     * statement and an argument array, return a <code>JSON.Array</code> 
     * or use <code>null</code> if the result set was empty.
     * 
     * <h4>Synopsis</h4>
     * 
     * <blockquote>
     * <pre>try {
     *    $.json.put("relations", $.<strong>sqlRelations(</strong>
     *        "select * from TABLE where KEY=?", 
     *        new String[]{"key"},
     *        100
     *        <strong>)</strong>)
     *} catch (SQLException e) {
     *    $.logError(e);
     *}</pre>
     * </blockquote>
     * 
     * @param statement to prepare and execute as a query
     * @param arguments an array of <code>String[]</code> 
     * @param fetch the number of rows to fetch
     * @return a <code>JSON.Array</code> or null 
     * @throws an <code>SQLException</code>
     */
    public JSON.Array sqlRelations (
        String statement, String[] arguments, int fetch
        ) 
    throws SQLException {
        return (JSON.Array) sqlQuery (
            statement, Simple.itermap(json, arguments), fetch, SQL.relations
            );
    }

    /**
     * Try to query the <code>sql</code> JDBC connection with an SQL
     * statement and an argument array, name a <code>JSON.Array</code> 
     * as <code>collection</code> in <code>$.json</code> and return true
     * or set <code>collection</code> to <code>null</code> if the result 
     * set was empty and return false.
     * 
     * <h4>Synopsis</h4>
     * 
     * <pre>try {
     *    $.json.put("collection", ($.<strong>sqlCollection(</strong>
     *        "select COLUMN from TABLE wher KEY=?", 
     *        new String[]{"key"},
     *        100
     *        <strong>)</strong>);
     *} catch (SQLException e) {
     *    $.logError(e);
     *}</pre>
     * 
     * @param statement to prepare and execute as a query
     * @param arguments an array of <code>String[]</code> 
     * @param fetch the number of rows to fetch
     * @return a <code>JSON.Array</code> or null 
     * @throws an <code>SQLException</code>
     */
    public JSON.Array sqlCollection (
        String statement, String[] arguments, int fetch
        ) 
    throws SQLException {
        return (JSON.Array) sqlQuery (
            statement, Simple.itermap(json, arguments), fetch, SQL.collection
            );
    }
    
    /**
     * 
     * @param statement
     * @param arguments
     * @param fetch
     * @return
     * @throws SQLException
     */
    public JSON.Object sqlDictionary (
        String statement, String[] arguments, int fetch
        ) 
    throws SQLException {
        return (JSON.Object) sqlQuery (
            statement, Simple.itermap(json, arguments), fetch, SQL.dictionary
            );
    }
        
    /**
     * Try to query the <code>sql</code> JDBC connection with an SQL
     * statement and an argument array, name a <code>JSON.Object</code> 
     * as <code>object</code> in <code>$.json</code> and return true or set 
     * <code>object</code> to <code>null</code> if the result set was 
     * empty and return false.
     * 
     * <h4>Synopsis</h4>
     * 
     * <pre>try {
     *    $.json.put("objects", $.<strong>sqlObjects(</strong>
     *        "select * from TABLE where KEY=?",
     *        new String[]{"key"},
     *        10
     *        <strong>)</strong>);
     *} catch (SQLException e) {
     *    $.logError(e);
     *}</pre>
     * 
     * @param objects the name of the result in <code>$.json</code>
     * @param statement to prepare and execute as a query
     * @param arguments an array of <code>String[]</code> 
     * @return a  
     * @throws an <code>SQLException</code>
     */
    public JSON.Array sqlObjects ( 
        String statement, String[] arguments, int fetch
    ) 
    throws SQLException {
        return (JSON.Array) sqlQuery (
            statement, Simple.itermap(json, arguments), fetch, SQL.objects
            );
    }
    
    /**
     * Try to query the <code>sql</code> JDBC connection with an SQL
     * statement and an argument array, name a <code>JSON.Object</code> 
     * as <code>object</code> in <code>$.json</code> and return true or set 
     * <code>object</code> to <code>null</code> if the result set was 
     * empty and return false.
     * 
     * <h4>Synopsis</h4>
     * 
     * <pre>try {
     *    if ($.<strong>sqlObject(</strong>
     *        "object",
     *        "select * from TABLE where PRIMARY=?",
     *        new String[]{"key"}
     *        <strong>)</strong>);
     *        ... // process object
     *    else
     *        ... // error, null result
     *} catch (Exception e) {
     *    $.logError(e);
     *}</pre>
     * 
     * @param object the name of the result in <code>$.json</code>
     * @param statement to prepare and execute as a query
     * @param arguments an array of <code>String[]</code> 
     * @return false if no collection was returned 
     * @throws an <code>SQLException</code>
     */
    public JSON.Object sqlObject ( 
        String statement, String[] arguments, int fetch
    ) 
    throws SQLException {
        return (JSON.Object) sqlQuery (
            statement, Simple.itermap(json, arguments), fetch, SQL.object
            );
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
        if (test) logInfo (statement, less4j);
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
     * with an arguments iterator, close the JDBC/DataSource 
     * statement and return the number of rows updated.
     * 
     * @param statement the SQL statement to execute
     * @param args the statement arguments iterator
     * @return -1 if the statement failed, 0 if no row update took place, 
     *         or the numbers of rows updated, deleted or inserted.
     * @throws SQLException
     */
    public int sqlUpdate (String statement, Iterator args) 
    throws SQLException {
        int result = -1;
        if (test) logInfo (statement, less4j);
        PreparedStatement st = null;
        try {
            st = sql.prepareStatement(statement);
            int i=0; while (args.hasNext()) st.setObject(i++, args.next());
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
     * with many arguments iterator, close the JDBC/DataSource 
     * statement and return a JSON array that list the number of rows 
     * updated for each set of arguments.
     * 
     * @param statement the SQL statement to execute
     * @param params the JSON array of parameter arrays
     * @return a JSON array of integers
     * @throws SQLException, JSON.Error
     */
    public JSON.Array sqlUpdateMany (String statement, Iterator params) 
    throws SQLException, JSON.Error {
        int i, L;
        JSON.Array args;
        JSON.Array result = null;
        if (test) logInfo (statement, less4j);
        PreparedStatement st = null;
        try {
            st = sql.prepareStatement(statement);
            while (params.hasNext()) {
                args = (JSON.Array) params.next();
                for (i=0, L=args.size(); i < L; i++)
                    st.setObject(i, args.get(i));
                result.add(new Integer(st.executeUpdate()));
            }
            st.close();
            st = null;
        } finally {
            if (st != null) try {st.close();} catch (SQLException se) {;}
        }
        return result;
    }
    
    protected static final 
    String ldapCtxFactory = "com.sun.jndi.ldap.LdapCtxFactory"; 
    protected static final String ldapSecurity = "simple"; 

    /**
     * Try to open a new anonymous connection to the LDAP server configured,
     * given principal and credentials. Catch any JNDI exception, return 
     * true on success and false otherwise, log an information message
     * in test mode.
     * 
     * @return true if the connection was successfull, false otherwise
     */
    public boolean 
    ldapOpen (String url) {
        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, ldapCtxFactory);
        env.put(Context.PROVIDER_URL, url);
        env.put(Context.SECURITY_AUTHENTICATION, ldapSecurity);
        try {
            ldap = new InitialDirContext(env);
        } catch (NamingException e) {
            logError(e);
            return false;
        }
        if (test) logInfo("connected to LDAP", less4j);
        return true;
    }

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
    public boolean 
    ldapOpen (String url, Object principal, Object credentials) {
        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, ldapCtxFactory);
        env.put(Context.PROVIDER_URL, url);
        env.put(Context.SECURITY_AUTHENTICATION, ldapSecurity);
        env.put(Context.SECURITY_PRINCIPAL, principal);
        env.put(Context.SECURITY_CREDENTIALS, credentials);  
        try {
            ldap = new InitialDirContext(env);
        } catch (NamingException e) {
            logError(e);
            return false;
        }
        if (test) logInfo("connected to LDAP", less4j);
        return true;
    }

    /**
     * Try to close the current LDAP connection. Catch and log error any
     * JNDI exception, log success in test mode.
     */
    public void ldapClose () {
        try {ldap.close();} catch (NamingException e) {logError(e);}
        ldap = null;
        if (test) logInfo("disconnected from LDAP", less4j);
    }
    
    /**
     * Try to resolve an LDAP context and update a <code>Map</code> 
     * with the attributes named by the <code>names</code> iterator either as 
     * <code>null</code>, strings or <code>JSON.Array</code> of strings, then 
     * return true if the context's name was resolved, false otherwise.
     * 
     * <h4>Synopsis</h4>
     * 
     * <pre>if ($.ldapOpen("ldap://host:port", "user", "pass")) try {
     *    JSON.Object attributes = new JSON.Object();
     *    if ($.<strong>ldapResolve(</strong>
     *        "uid=lszyster,ou=People,...", attributes, Simple.iterator(
     *            new String[]{"cn", "mail", "homeDirectory"}
     *            ) 
     *        <strong>)</strong>)
     *        ...
     *} catch (Exception e) {
     *    $.logError(e);
     *} finally {
     *    $.ldapClose();
     *}</pre>
     *
     * <p>The purpose of this method is to allow applications not to worry 
     * about the specialized types and the API details of JNDI. Instead they 
     * can use <code>Map</code>, <code>List</code> and <code>String</code>.</p>
     * 
     * <p>Note that if the <code>Base</code> instance's <code>test</code> is 
     * true, then values of the attributes resolved will be logged as one 
     * information message.</p>
     * 
     * @param dn the distinguished name to resolve
     * @param map the Map to update
     * @param names of the attribute values to get
     * @return true if the name was resolved, false otherwise
     */
    public boolean ldapResolve (
        String dn, Map map, Iterator names
        ) {
        if (test) logInfo ("request dn=" + dn, "LDAP");
        Attributes attributes;
        try {
            attributes = ldap.getAttributes(dn);
            String key;
            Attribute attrs;
            while (names.hasNext()) {
                key = (String) names.next();
                attrs = attributes.get(key);
                if (attrs == null)
                    map.put(key, null);
                else {
                    int L = attrs.size();
                    if (L == 1)
                        map.put(key, attrs.get(0).toString());
                    else {
                        ArrayList list = new ArrayList();
                        for (int i=0; i<L; i++) 
                            list.add(attrs.get(i).toString());
                        map.put(key, list);
                    }
                }
            }
        } catch (Exception e) {
            logError(e);
            return false;
        }
        if (test) logInfo(JSON.encode(map), "LDAP");
        return true;
    }
    
    /**
     * Try to create an LDAP context with attributes values from a JSON.O 
     * object for the given attribute names, return true if the context 
     * was created, false otherwise.
     * 
     * @param dn the distinguished name of the context created
     * @param object the JSON.O object from which to set the attribute values
     * @param names of the attributes to create
     * @return true, false
     */
    public boolean ldapCreate (
        String dn, JSON.Object object, Iterator names
        ) {
        if (test) logInfo("create dn=" + dn, "LDAP");
        Iterator iter;
        String key;
        Object value;
        BasicAttribute attribute;
        BasicAttributes attributes = new BasicAttributes (false); 
        while (names.hasNext()) {
            key = (String) names.next();
            attribute = new BasicAttribute(key);
            value = object.get(key);
            if (value instanceof JSON.Array) {
                iter = ((JSON.Array) object.get(key)).iterator();
                while (iter.hasNext()) attribute.add(iter.next());
            } else 
                attribute.add(value);
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
    public boolean ldapCreate (String dn, JSON.Object object) {
        return ldapCreate (dn, object, object.keySet().iterator());
    }
    
    public boolean ldapUpdate (
        String dn, JSON.Object object, Iterator names
        ) {
        if (test) logInfo("update LDAP dn=" + dn, less4j);
        // TODO: ldapUpdate ...
        return true;
    }
    
    public boolean ldapUpdate (String dn, JSON.Object object) {
        return ldapUpdate(dn, object, object.keySet().iterator());
    }
    
    /* that's all folks */
    
}

/* Note about this implementation

This actor is meant for RESTfull applications. Grossly outlined, here is
a typical one:

    /resource(/.*)?

is the pattern of the context path dispatched to the servlet by its J2EE
container. Each servlet controller made of a doGet and doPost methods
that apply the Actor's API and possibly a flat API of action methods, all
preferrably with the same interface:

    void action (Actor $) {}

so as to be easely chained together.

For idempotent URL like

    /resource

the controller may respond a redirect to the URL of the relevant 
authorization service, an XML or JSON body as stateless as possible.

Things like a "home page" where a user's preferences are rooted
is best stored as an allready serialized XHTML string, ready to be
dumped from the database. The root of the application:

    /

is the most obvious stateless thing.

As transient GET or POST response, less4j tries to allways speak JSON,
if not as JSON embedded in XML (usually XHTML).

Note that the flow of control for all actions on the same resource is 
expected to be written entirely in java, in a single controller class.

Actions shared by controllers can be aggregated in one controller class
from which to derive all others in the domain. Functions that transcend 
all applications should be added to a site specific Actor's API.

There are no other serialization protocol implemented than JSON here,
all XML is expected to be static and the rest of the stack is better
programmed in XSLT, CSS and JavaScript, running in the browser

Last but not least, electing JSON as The Object Model saves less4j 
of having to choose an ORM. When doing a dumb thing like filling an invoice, 
instead of each time locking a table of invoice details updating, deleting 
and inserting rows in a shared resource it is better to work on a separate 
object instance?

And since all that work happens to be done in JavaScript in the browser,
then why not just forget about classes and interfaces of Java objects.

And make it asynchronous.

*/