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

package org.less4j; // less java for more applications

import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import java.io.IOException;
import java.nio.ByteBuffer;

import java.sql.DriverManager;
import java.sql.Connection;
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Cookie;

/**
 * Hold a rich state for each request and provide many conveniences to
 * apply HTTP, JSON, SQL, LDAP and IRTD2.   
 */
public class Actor {
    
    protected static final String less4j = "less4j";
    
    /**
     * The default charset encoding in less4j, something supported by
     * web 2.0 browsers (IE6, Firefox, etc ...) and java runtime
     * environments. Also the defacto standard for JSON encoding.
     */
    protected static final String _UTF_8 = "UTF-8";
    
    /**
     * A safe copy of the servlet controller's <code>configuration</code>. 
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
     * to quickly dispatch the request's through simple String tests.
     */
    public String url;
    
    /**
     * A usefull copy of <code>request.getPathInfo()</code>
     * to quickly dispatch the request's through simple String tests.
     */
    
    public String about;
    
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
     * The time of this Actor's, in seconds since epoch (ie: 01/01/1970), 
     * as an java <code>int</code> value 
     *   
     */
    public int time = (int)(System.currentTimeMillis()/1000);
    
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
    
    protected StringBuffer strb(StringBuffer sb) {
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
        sb.append(",\"about\":");
        JSON.strb(sb, about);
        sb.append(",\"json\":");
        JSON.strb(sb, json);
        sb.append('}');
        return sb;
    }
    
    /**
     * Return a JSON <code>String</code> of this Actor's public state.
     * 
     * @return this Actor's state
     * 
     * @pre $.json200Ok ($.toString());
     * 
     * @p Will return string returned matches this JSONR pattern:
     * 
     * @jsonr {
     *    "identity": "$[0-9a-zA-Z]+^", 
     *    "rights": "$[0-9a-zA-Z]+^",
     *    "time": 0, 
     *    "digest": "[0-9a-f]{20}",
     *    "digested": "[0-9a-f]{20}",
     *    "about": [""],
     *    "json": null
     *}
     * 
     * @p Note that it does not include the controller's private part
     * of its state: configuration, salt, salted, sql or ldap.
     */
    public String toString() {
        return strb(new StringBuffer()).toString();
    }
    
    /**
     * Initialize a new Actor for development purpose with an empty 
     * <code>configuration</code> and a <code>test</code> flag set to 
     * <code>true</code>.
     */
    public Actor () {
        test = true;
        configuration = new JSON.Object();
    }
    
    /**
     * Initialize a new Actor to test a controller servlet's configuration 
     * when it is initialized.
     * 
     * @param conf the controller's configuration JSON.Object
     */
    public Actor (JSON.Object conf) {
        test = conf.B(Controller._test, false);
        configuration = conf;
    }
    
    /**
     * Initialize a new Actor to handle an HTTP request and response, 
     * set the Actor's audit digest salt and eventually load ...
     * 
     * @param conf the controller's configuration JSON.Object
     * @param req the HTTP request to handle 
     * @param res the HTTP response to complete
     */
    public Actor (
        JSON.Object conf, HttpServletRequest req, HttpServletResponse res
        ) {
        test = conf.B(Controller._test, false);
        configuration = conf;
        request = req;
        response = res;
        url = request.getRequestURL().toString();
        about = request.getPathInfo();
        context = request.getContextPath() + '/';
        try {
            JSON.Array _salts = configuration.A(Controller._irtd2Salts);
            int L=_salts.size();
            salts = new byte[L][];
            for (int i=0; i<L; i++)
                salts[i] = _salts.S(i).getBytes();
        } catch (JSON.Error e) {
            logError(e);
        };
    }

    /**
     * Write a message to STDOUT, as one line prefixed with the Actor's
     * IRTD2 <code>digested</code> hash and a white space.
     * 
     * @p You may apply Resin's excellent configuration of STDOUT for
     * your web applications:
     * 
     * @pre <a href="http://wiki.caucho.com/Stdout-log"
     *   >http://wiki.caucho.com/Stdout-log</a>
     * 
     * @p Use multilog or any other log post-processor to add timestamp 
     * and other usefull audit information, from outside your application 
     * where those functions belong.
     * 
     * @param message the text string prefixed and logged to STDOUT
     *     
     */
    public void logOut (String message) {
        StringBuffer sb = new StringBuffer();
        sb.append(digested);
        sb.append(' ');
        sb.append(message);
        System.out.println(sb.toString());
    }
    
    private static final String _logInfoDelimiter = ": "; 
    
    /**
     * @p Write a message to STDERR as one line prefixed with the Actor's
     * IRTD2 <code>digested</code> hash and a category name, both separated
     * by a whitespace.
     * 
     * @p Again, you should use a log-posprocessor to add audit information 
     * to your logs. Or apply the J2EE container's configuration features:
     * 
     * @pre <a href="http://wiki.caucho.com/Stderr-log"
     *   >http://wiki.caucho.com/Stderr-log</a>
     * 
     * @p Audit logs may inform about a completed transaction, an aborted
     * action, a authorization failure or any hazardous event for which
     * a trace is legaly required.
     * 
     * @p Categorization is unpredictable, it should not be restricted to 
     * levels of debugging which are usefull to programmers only.
     * 
     * @param message the text string prefixed and logged to STDERR
     * @param category usefull for post-processing informative logs
     *     
     */
    public void logInfo (String message, String category) {
        StringBuffer sb = new StringBuffer();
        sb.append(digested);
        sb.append(' ');
        sb.append(category);
        sb.append(_logInfoDelimiter);
        sb.append(message);
        System.err.println(sb.toString());
    }

    private static final String _stackTrace = "JAVA: ";
    
    /**
     * Write a full stack trace to STDERR in test mode or just one line
     * in production, prefix both the IRTD2 digest and categorize it as 
     * <code>JAVA</code>.
     * 
     * @pre try {...} catch (Exception e) {logError (e);}
     *     
     * @param error the throwable instance catched
     * @return the line logged
     */
    public String logError (Throwable error) {
        StackTraceElement[] stacktrace = error.getStackTrace();
        StackTraceElement thrower;
        int i = 0;
        do {
            thrower = stacktrace[i++];
        } while (thrower.getLineNumber() == -1 && i < stacktrace.length);
        StringBuffer sb = new StringBuffer();
        sb.append(_stackTrace);
        sb.append(digested);
        sb.append(' ');
        sb.append(thrower.getClassName());
        sb.append('.');
        sb.append(thrower.getMethodName());
        sb.append(' ');
        sb.append(thrower.getLineNumber());
        sb.append(' ');
        sb.append(error.getClass().getName());
        sb.append(' ');
        sb.append(error.getMessage());
        String line = sb.toString();
        if (test) {
            error.printStackTrace(System.err);
        } else
            System.err.println(line);
        return line;
    }
    
    protected static String logLESS4J = "LESS4J: ";
    
    /** 
     * Log an complete audit of this Actor's JSON application in one line.
     * 
     * @p There's no need to call this method, it will be called by the
     * httpError, httpResponse, http302Redirect and jsonResponse.
     * 
     * @p The audit log of less4j is designed to be applied outside of
     * its J2EE host. Each line is formated as a sequence of 11 byte
     * strings separated by a space.
     * 
     * @p For instance:
     * 
     * @pre LESS4J: identity roles time digested digest GET /url HTTP/1.1 200 null
     *
     * @p ...
     * 
     */
    public void logAudit (int status) {
        StringBuffer sb = new StringBuffer();
        sb.append(logLESS4J);
        sb.append(request.getMethod());
        sb.append(' ');
        sb.append(request.getRequestURI());
        sb.append(' ');
        sb.append(request.getProtocol());
        sb.append(' ');
        sb.append(status);
        sb.append(' ');
        sb.append(irtd2);
        sb.append(' ');
        if (test) {
            JSON.repr(sb, json, JSON._crlf);
        } else {
            JSON.strb(sb, json);
        }
        System.out.println(sb.toString());
    }
    
    /**
     * Get a named request cookie value.
     * 
     * @param name of the cookie
     * @return a string or null
     */
    public String httpCookie (String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (int i = 0; i < cookies.length; i++) {
                if (cookies[i].getName().equals(name)) {
                    return cookies[i].getValue(); 
                }
            }
        }
        return null;
    }
    
    protected static final String irtd2Name = "IRTD2";
    
    protected static Pattern irtd2Split = Pattern.compile(":");
    
    /**
     * Try to collect a IRTD2 cookie in the request and test its digest 
     * against the secret(s) set by configuration for this actor's controller, 
     * digest a new cookie only if a the digested cookie is still valid in 
     * time and bears the signature of this servlet, return false otherwise. 
     * 
     * @p This method is expected to be called by the <code>Actor</code>'s 
     * <code>Controller</code> before it handles the request, so it should 
     * not be called by less4j's applications.
     * 
     * @p Nevertheless, application developpers should understand what this
     * method does and why it is so usefull for web controllers.
     * 
     * @param timeout the limit of an IRTD2 cookie's age, in seconds  
     * @return true if the request failed to be authenticated
     */
    public boolean irtd2Digested (int timeout) {
        String cookie = httpCookie(irtd2Name);
        if (cookie == null) {
            return false;
        }
        String[] vector = IRTD2.parse(cookie); 
        identity = vector[0];
        rights = vector[1];
        digest = vector[3];
        digested = vector[4];
        int error = IRTD2.digested(vector, time, timeout, salts);
        if (error == 0) {
            return true;
        } else {
            if (test) {
                logInfo(IRTD2.errors[error], "IRTD2");
            }
            return false;
        }
    }
    
    /**
     * Literally "digest a cookie", transform the IRTD2 cookie sent with
     * the request into a new cookie bearing the Actor's time, to be sent
     * with the response.
     * 
     * @p The cookie value is a formatted string made as follow:
     * 
     * @pre Cookie: IRTD2=identity roles time digested digest; 
     * 
     * @p where <code>identity</code> and <code>roles</code> are 
     * 7-bit ASCII characters only but ' ' and ';', followed
     * by the controller's <code>time</code> representation and two SHA1 hex
     * digests: the client's last digest for this cookie and the one computed 
     * from the byte string that precedes it.
     * 
     */
    public void irtd2Digest() {
        String[] vector = new String[]{
            identity, rights, (new Integer(time)).toString(), digested, null
            };
        digest = IRTD2.digest(vector, salts[0]);
        vector[4] = digest; 
        irtd2 = Simple.join(" ", Simple.iter(vector));
    }
    
    protected void irtd2SetCookie () {
        // This sucks because of ... 
        Cookie ck = new Cookie(irtd2Name, irtd2);
        ck.setDomain(request.getServerName());
        ck.setPath(context);
        ck.setMaxAge(Integer.MAX_VALUE); // ... devilish details.  
        response.addCookie(ck);
        // TODO: try to bypass this brain-dead HTTP Cookie API.
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
     * @p Eventually complete a relative location to this Actor's 
     * requested resource:
     * 
     * @pre ?action
     *
     * @p Relative to its application's domain:
     * 
     * @pre /resource
     * 
     * @p To the resource context path:
     * 
     * @pre resource
     * 
     * @p Produce a n absolute URL like:
     *
     * @pre http://domain/resource?action
     * 
     * @p something quite usefull for redirection in a RESTfull controller.
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
    
    /**
     * ...
     * 
     * @param limit
     * @return a <code>byte[]</code> buffer
     */
    public byte[] httpPOST(int limit) {
        int contentLength = request.getContentLength(); 
        if (contentLength < 1 || contentLength > limit)
            return null; // don't do null or excess data
        
        byte[] body = new byte[contentLength];
        try {
            Simple.recv(request.getInputStream(), body, 0);
        } catch (IOException ioe) {
            logError(ioe); 
            body = null;
        }
        return body;
    }
    
    /**
     * Try to send an HTTP error response, rely on the J2EE implementation
     * to produce headers and body. Audit a successfull response or log an 
     * error.
     * 
     * @param code HTTP error to send
     */
    public void httpError (int code) {
        irtd2SetCookie ();
        try {
            response.sendError(code); logAudit(code);
        } catch (IOException e) {
            logError(e);
        }
    }
    
    /**
     * Try to send an HTTP response with the appropriate headers
     * for an array of bytes as body, a given content type and 
     * charset. Audit a successfull response or log an error.
     * 
     * @param code of the HTTP response status
     * @param body as a ByteBuffer
     * @param type of the response body content type
     * @param charset used to encode the body (eg: "ASCII")
     */
    public void httpResponse (
        int code, ByteBuffer body, String type, String charset
        ) {
        irtd2SetCookie ();
        response.setStatus(code);
        if (charset != null) type += ";charset=" + charset;
        response.setContentType(type);
        response.setContentLength(body.capacity());
        try {
            response.setBufferSize(Simple.netBufferSize);
            Simple.send(response.getOutputStream(), body);
            response.flushBuffer();
            logAudit(code);
        } catch (IOException e) {
            logError(e);
        }
    }
    
    /**
     * Try to send an HTTP response with the appropriate headers
     * for an arbitrary bytes string as body, a given content type and 
     * charset. Audit a successfull response or log an error.
     * 
     * @param code the HTTP response code
     * @param body a byte string
     * @param type the response body content type
     * @param charset the character set encoding used (eg: "ASCII")
     */
    public void httpResponse (
        int code, byte[] body, String type, String charset
        ) {
        irtd2SetCookie ();
        response.setStatus(code);
        if (charset != null) type += ";charset=" + charset;
        response.setContentType(type);
        response.setContentLength(body.length);
        try {
            response.setBufferSize(Simple.netBufferSize);
            response.getOutputStream().write(body);
            response.flushBuffer();
            logAudit(code);
        } catch (IOException e) {
            logError(e);
        }
    }
    
    /**
     * Send an HTTP response with the appropriate headers for
     * a UNICODE string as body, a given content type and charset. This
     * method catches any <code>UnsupportedEncodingException</code> and 
     * uses the plateform default character set if the given encoding is
     * not supported. Audit a successfull response or log an error.
     * 
     * @pre $.httpResponse(200, "&lt;hello-world/&gt;", "text/xml", "ASCII")
     * 
     * @p Note: here <code>$</code> is an <code>Actor</code> instance.
     *
     * @param body a string
     * @param type the resource content type
     * @param charset the character set encoding used (eg: "UTF-8")
     */
    public void httpResponse (
        int code, String body, String type, String charset
        ) {
        httpResponse(code, Simple.encode(body, charset), type, charset);
    }
    
    /**
     * The default content type of resources is XML, the defacto
     * standard supported by XSL, CSS and JavaScript in web 2.0
     * browsers.
     */
    protected static final String _text_html = "text/html";
    
    protected static final String _Location = "Location";
    
    /**
     * Try to send a 302 Redirect HTTP response to a location and set the
     * response Content-Type and MIME body. The location may be absolute
     * or relative to this resource. Audit a successfull response or log an 
     * error.
     * 
     * @p Note that the Content-Type specified must match the body's MIME
     * type and character set encoding. 
     *
     * @param location to redirect to
     * @param body an of bytes with the response body 
     * @param type the Content-Type of the response 
     */
    protected void http302Redirect (
        String location, byte[] body, String type
        ) {
        response.addHeader(_Location, httpLocation(location));
        httpResponse(302, body, type, null);
    }
    
    protected static final String http302RedirectXML = (
            "<?xml version=\"1.0\" encoding=\"ASCII\" ?>" +
            "<rest302Redirect/>"
            );
        
    /**
     * Try to send a 302 HTTP Redirect response to the a relative or
     * absolute location with the following XML string as body: 
     * 
     * @pre &lt;rest302Redirect/&gt;
     * 
     * @pre $.http302Redirect("/resource?action")
     * 
     * @p Note that this method does <em>not</em> apply the
     * <code>sendRedirect</code> method of <code>HttpServletResponse</code>
     * in order to send its own response body. Instead it uses the 
     * <code>URL</code> convenience to validate a location as an
     * absolute URL.
     * 
     * @param location to redirect to
     */
    public void http302Redirect(String location) {
        http302Redirect(
            location, http302RedirectXML.getBytes(), _text_html
            );
    }
    
    protected static final String jsonXJSON = "X-JSON";
    
    /**
     * ...
     * 
     * @param interpreter
     * @return true if a regular JSON request was transfered
     */
    public boolean jsonX(JSON interpreter) {
        String xjson = request.getHeader(jsonXJSON);
        if (xjson == null)
            return false;
        
        if (json == null)
            json = new JSON.Object();
        JSON.Error e = interpreter.update(json, xjson); 
        if (e != null) {logError(e); return false;}
        return true;
    }
    
    /**
     * ...
     * 
     * @param interpreter
     * @return true if a valid JSON request was GET or POSTed 
     */
    public boolean jsonGET(Object interpreter) {
        JSONR.Type model = null;
        int containers, iterations;
        if (interpreter instanceof JSONR) {
            JSONR intr = (JSONR) interpreter;
            containers = intr.containers;
            iterations = intr.iterations;
            model = intr.type;
        } else if (interpreter instanceof JSON) {
            JSON intr = (JSON) interpreter;
            containers = intr.containers;
            iterations = intr.iterations;
        } else
            return false;
        
        json = new JSON.Object();
        Map query = request.getParameterMap();
        String name;
        String[] strings;
        Iterator iter = query.keySet().iterator();
        containers--;
        while (containers > -1 && iterations > -1 && iter.hasNext()) {
            name = (String) iter.next();
            strings = (String[]) query.get(name);
            if (strings != null)
                if (strings.length > 1) { 
                    containers--;
                    JSON.Array list = new JSON.Array();
                    for (int i=0; i < strings.length; i++) { 
                        list.add(strings[i]); 
                        iterations--;
                    }
                    json.put(name, list);
                } else if (strings.length > 0)
                    json.put(name, strings[0]);
                else
                    json.put(name, null);
            iterations--;
        }
        if (model != null) try {
            JSONR.validate(json, model);
        } catch (JSONR.Error e) {
            return false;
        }
        return true;
    }
    
    private static final String _arg0 = "arg0";
    
    /**
     * Try to read and parse the body of a POST request, assuming it 
     * contains a content of type <code>application/json</code> encoded
     * in UTF-8 and fitting in a limited buffer. Return true on success or 
     * log an error and return false.
     * 
     * @p This is a controller's actor and less4j is a framework for 
     * entreprise web interfaces: there is no reason to accept JSON objects 
     * larger than the limit that fits its application.
     * 
     * @p Using a the servlet getReader method directly is not an option
     * if the application controller must enforce reasonable input limits
     * per request. Here's a good place to do it once for all JSON
     * applications.
     * 
     * @param limit on the response body's content length
     * @param interpreter
     * @return true if successfull, false otherwise
     */
    public boolean jsonPOST(int limit, Object interpreter) {
        byte[] body = httpPOST(limit);
        Object object;
        if (body == null) 
            return false; 
        if (interpreter instanceof JSON) {
            JSON intr = (JSON) interpreter;
            try {
                // parse JSON when the buffer is filled but not overflowed
                object = intr.eval(new String(body, _UTF_8));
            } catch (Exception e) {
                logError(e);
                return false;
            }
        } else if (interpreter instanceof JSONR) {
            JSONR intr = (JSONR) interpreter;
            try {
                object = intr.eval(new String(body, _UTF_8));
            } catch (Exception e) {
                logError(e);
                return false;
            }
        } else {
            // logError(new Exception("Not a JSON or JSONR interpreter"));
            return false;
        }
        if (object != null && object instanceof JSON.Object)
            json = (JSON.Object) object;
        else {
            json = new JSON.Object();
            json.put(_arg0, object);
        }
        return true;
    }
    
    /**
     * ...
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
    
    /**
     * ...
     * 
     * @param signed
     * @param signature
     * @return true if the given JSON object matches the signature
     */
    public boolean jsonDigested(String signed, String signature) {
        String digest = json.S(signature, "");
        byte[] buff = JSON.encode(json.get(signed)).getBytes();
        String d = null;
        for (int i=0; i<salts.length; i++) {
            SHA1 md = new SHA1();
            md.update(buff);
            md.update(salts[i]);
            d = md.hexdigest();
            if (d.equals(digest)) return true;
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
     * Try to complete an HTTP/1.X response <code>code</code> with a byte
     * string as body and audit the response, or log an error.
     * 
     * @param status of the response
     * @param body of the response 
     */
    public void jsonResponse (int status, byte[] body) {
        irtd2SetCookie();
        /* the response body must be short enough to be buffered fully */
        response.setStatus(status);
        response.setContentType(jsonContentType);
        response.setContentLength(body.length);
        try {
            response.setBufferSize(Simple.netBufferSize);
            response.getOutputStream().write(body);
            response.flushBuffer();
            logAudit(status);
        } catch (IOException e) {
            logError(e);
        }
    }
    
    /**
     * Try to complete an HTTP/1.X response <code>code</code> with a 
     * string encoded in UTF-8 as body and audit the response, or log an 
     * error.
     * 
     * @param status of the response
     * @param body of the response 
     */
    public void jsonResponse (int status, String body) {
        jsonResponse(status, Simple.encode(body, _UTF_8));
    }
    
    /**
     * Try to complete an HTTP/1.X response <code>code</code> with the 
     * actor's JSON object encoded in UTF-8 as body and audit the response, 
     * or log an error.
     * 
     * @param status of the response
     * @param body of the response 
     */
    public void jsonResponse (int status) {
        if (test)
            jsonResponse(status, JSON.pprint(json));
        else
            jsonResponse(status, JSON.encode(json));
    }
    /**
     * Try to open a J2EE datasource and disable AutoCommit, return 
     * <code>true</code> and if in "test" mode, log information, or
     * return <code>false</code> and log error.
     * 
     * @param datasource name of the J2EE <code>DataSource</code>
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
     * Try to open a JDBC connection from its URL, disable AutoCommit. Allways 
     * log error, log success only in test mode.
     * 
     * @param url of the JDBC connection
     * @return true if the connection was successfull, false otherwise
     */
    public boolean sqlOpenJDBC (String url) {
        try {
            sql = DriverManager.getConnection(url);
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
     * Try to open a JDBC connection from its URL, with the given username
     * and password, disable AutoCommit. Allways log error, log success only 
     * in test mode.
     * 
     * @param url of the JDBC connection
     * @param username
     * @param password 
     * @return true if the connection was successfull, false otherwise
     */
    public boolean sqlOpenJDBC (
        String url, String username, String password
        ) {
        try {
            sql = DriverManager.getConnection(url, username, password);
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
     * statement and an argument values iterator, use an <code>ROM</code>
     * to return a <code>JSON.Array</code>, a <code>JSON.Object</code>
     * or null if the result set was empty.
     * 
     * @pre try {
     *    JSON.Array relations = (JSON.Array) $.sqlQuery(
     *        "select * from TABLE where KEY = ? and VALUE > ?", 
     *        Simple.iter(new Object[]{"key", new Integer(10)}),
     *        100, SQL.relations
     *        );
     *} catch (SQLException e) {
     *    $.logError(e);
     *}
     * 
     * @param statement to prepare and execute as a query
     * @param arguments an iterator of simple types
     * @param fetch the number of rows to fetch
     * @param collector the <code>ROM</code> used to map the result set
     * @return a <code>JSON.Array</code>, a <code>JSON.Object</code> or 
     *         <code>null</code>
     * @throws SQLException
     */
    public Object sqlQuery (
        String statement, Iterator args, int fetch, SQL.ROM collector
        ) 
    throws SQLException {
        if (test) logInfo(statement, "SQL");
        return SQL.query(sql, statement, args, fetch, collector);
    }

    /**
     * Try to query the <code>sql</code> JDBC connection with an SQL
     * statement and an argument iterator, return a <code>JSON.Object</code>
     * with the obvious "columns" and "rows" members, or <code>null</code> 
     * if the result set was empty.
     * 
     * @pre try {
     *    $.json.put("table", $.sqlTable(
     *        "select * from TABLE where KEY=?", 
     *        Simple.iter(new Object[]{"mykey"}),
     *        100
     *        ))
     *} catch (SQLException e) {
     *    $.logError(e);
     *}
     * 
     * @param statement to prepare and execute as a query
     * @param arguments an iterator of simple types
     * @param fetch the number of rows to fetch
     * @return a <code>JSON.Object</code> or null 
     * @throws SQLException
     */
    public JSON.Object sqlTable (
        String statement, Iterator arguments, int fetch
        ) 
    throws SQLException {
        return (JSON.Object) sqlQuery (
            statement, arguments, fetch, SQL.table
            );
    }
    
    /**
     * Try to query the <code>sql</code> JDBC connection with an SQL
     * statement and an argument iterator, return a <code>JSON.Array</code> 
     * of <code>JSON.Array</code>s as relations or <code>null</code> 
     * if the result set was empty.
     * 
     * @pre try {
     *    $.json.put("relations", $.sqlRelations(
     *        "select * from TABLE where KEY=?", 
     *        Simple.iter(new Object[]{"mykey"}),
     *        100
     *        ))
     *} catch (SQLException e) {
     *    $.logError(e);
     *}
     * 
     * @param statement to prepare and execute as a query
     * @param arguments an iterator of simple types
     * @param fetch the number of rows to fetch
     * @return a <code>JSON.Array</code> of relations or null 
     * @throws an <code>SQLException</code>
     */
    public JSON.Array sqlRelations (
        String statement, Iterator arguments, int fetch
        ) 
    throws SQLException {
        return (JSON.Array) sqlQuery (
            statement, arguments, fetch, SQL.relations
            );
    }

    /**
     * Try to query the <code>sql</code> JDBC connection with an SQL
     * statement and an argument iterator, returns a <code>JSON.Array</code> 
     * as a collection for the first row in the result set or 
     * <code>null</code> if the result set was empty.
     * 
     * @pre try {
     *    $.json.put("collection", ($.sqlCollection(
     *        "select COLUMN from TABLE where KEY=?", 
     *        Simple.iter(new Object[]{"mykey"}),
     *        100
     *        ));
     *} catch (SQLException e) {
     *    $.logError(e);
     *}
     * 
     * @param statement to prepare and execute as a query
     * @param arguments an iterator of simple types
     * @param fetch the number of rows to fetch
     * @return a <code>JSON.Array</code> as collection or <code>null</code> 
     * @throws an <code>SQLException</code>
     */
    public JSON.Array sqlCollection (
        String statement, Iterator arguments, int fetch
        ) 
    throws SQLException {
        return (JSON.Array) sqlQuery (
            statement, arguments, fetch, SQL.collection
            );
    }
    
    /**
     * Try to query the <code>sql</code> JDBC connection with an SQL
     * statement and argument names, returns a <code>JSON.Object</code> 
     * that maps the first column of the result set <code>JSON.Array</code>
     * of the value(s) found in the following column(s).
     * 
     * @pre try {
     *    $.json.put("index", ($.sqlIndex(
     *        "select KEY, NAME, DESCRIPTION from TABLE where VALUE > ?", 
     *        Simple.iter(new Object[]{new Integer(10)}),
     *        100
     *        ));
     *} catch (SQLException e) {
     *    $.logError(e);
     *}
     * 
     * @param statement to prepare and execute as a query
     * @param arguments an iterator of simple types
     * @param fetch the number of rows to fetch
     * @return a <code>JSON.Object</code> as dictionnary or <code>null</code> 
     * @throws SQLException
     */
    public JSON.Object sqlIndex (
        String statement, Iterator arguments, int fetch
        ) 
    throws SQLException {
        return (JSON.Object) sqlQuery (
            statement, arguments, fetch, SQL.index
            );
    }
        
    /**
     * Try to query the <code>sql</code> JDBC connection with an SQL
     * statement and argument names, returns a <code>JSON.Object</code> 
     * that maps the first column of the result set to the second or 
     * <code>null</code> if the result set was empty.
     * 
     * @pre try {
     *    $.json.put("dictionary", ($.sqlDictionary(
     *        "select KEY, VALUE from TABLE where VALUE > ?", 
     *        Simple.iter(new Object[]{new Integer(10)}),
     *        100
     *        ));
     *} catch (SQLException e) {
     *    $.logError(e);
     *}
     * 
     * @param statement to prepare and execute as a query
     * @param arguments an iterator of simple types
     * @param fetch the number of rows to fetch
     * @return a <code>JSON.Object</code> as dictionnary or <code>null</code> 
     * @throws SQLException
     */
    public JSON.Object sqlDictionary (
        String statement, Iterator arguments, int fetch
        ) 
    throws SQLException {
        return (JSON.Object) sqlQuery (
            statement, arguments, fetch, SQL.dictionary
            );
    }
        
    /**
     * ...
     * 
     * @pre try {
     *    $.json.put("objects", ($.sqlObjects(
     *        "select * from TABLE where VALUE > ?", 
     *        Simple.iter(new Object[]{new Integer(10)}),
     *        100
     *        ));
     *} catch (SQLException e) {
     *    $.logError(e);
     *}
     * 
     * @param statement to prepare and execute as a query
     * @param arguments an iterator of simple types
     * @return a <code>JSON.Array</code> of <code>JSON.Objects</code> 
     *         or <code>null</code> 
     * @throws an <code>SQLException</code>
     */
    public JSON.Array sqlObjects ( 
        String statement, Iterator arguments, int fetch
    ) 
    throws SQLException {
        return (JSON.Array) sqlQuery (
            statement, arguments, fetch, SQL.objects
            );
    }
    
    /**
     * ...
     * 
     * @pre try {
     *    $.json.put("object", ($.sqlObject(
     *        "select * from TABLE where VALUE = ?", 
     *        Simple.iter(new Object[]{new BigDecimal("99.99")}),
     *        ));
     *} catch (SQLException e) {
     *    $.logError(e);
     *}
     * 
     * @param statement to prepare and execute as a query
     * @param arguments an iterator of simple types
     * @return a <code>JSON.Object</code> or <code>null</code> 
     * @throws an <code>SQLException</code>
     */
    public JSON.Object sqlObject ( 
        String statement, Iterator arguments
    ) 
    throws SQLException {
        return (JSON.Object) sqlQuery (
            statement, arguments, 1, SQL.object
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
    public Integer sqlUpdate (String statement) throws SQLException {
        if (test) logInfo (statement, less4j);
        return SQL.update(sql, statement);
    }

    /**
     * Try to execute a prepared UPDATE, INSERT, DELETE or DDL statement 
     * with an arguments iterator, close the JDBC/DataSource 
     * statement and return the number of rows updated.
     * 
     * @param statement the SQL statement to execute
     * @param args an <code>Iterator</code> of arguments
     * @return -1 if the statement failed, 0 if no row update took place, 
     *         or the numbers of rows updated, deleted or inserted.
     * @throws SQLException
     */
    public Integer sqlUpdate (String statement, Iterator args) 
    throws SQLException {
        if (test) logInfo (statement, less4j);
        return SQL.update(sql, statement, args);
    }
    
    /**
     * Try to execute a prepared UPDATE, INSERT, DELETE or DDL statement 
     * with many arguments iterator, close the JDBC/DataSource 
     * statement and returns the number of rows updated.
     * 
     * @param statement the SQL statement to execute
     * @param params an <code>Iterator</code> of <code>JSON.Array</code>s
     * @return an <code>Integer</code>
     * @throws <code>SQLException</code>
     */
    public Integer sqlBatch (String statement, Iterator params) 
    throws SQLException {
        if (test) logInfo (statement, less4j);
        return SQL.batch(sql, statement, params);
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
     * @param url of the LDAP connection to open
     * @return true if the connection was successfull, false otherwise
     */
    public boolean ldapOpen (String url) {
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
     * @param url of the LDAP connection to open
     * @param principal an LDAP user name
     * @param credentials the associated password  
     * @return true if the connection was successfull, false otherwise
     */
    public boolean 
    ldapOpen (String url, String principal, String credentials) {
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
     * @pre if ($.ldapOpen("ldap://host:port", "user", "pass")) try {
     *    JSON.Object attributes = new JSON.Object();
     *    if ($.ldapResolve(
     *        "uid=lszyster,ou=People,...", attributes, Simple.iterator(
     *            new String[]{"cn", "mail", "homeDirectory"}
     *            ) 
     *        ))
     *        ...
     *} catch (Exception e) {
     *    $.logError(e);
     *} finally {
     *    $.ldapClose();
     *}
     *
     * @p The purpose of this method is to allow applications not to worry 
     * about the specialized types and the API details of JNDI. Instead they 
     * can use <code>Map</code>, <code>List</code> and <code>String</code>.
     * 
     * @p Note that if the <code>Actor</code> instance's <code>test</code> is 
     * true, then values of the attributes resolved will be logged as one 
     * information message.
     * 
     * @param dn the distinguished name to resolve
     * @param object the <code>JSON.Object</code> to update
     * @param names of the attribute values to get
     * @return true if the name was resolved, false otherwise
     */
    public boolean ldapResolve (
        String dn, JSON.Object object, Iterator names
        ) {
        if (test) logInfo (dn, "LDAP resolve");
        Attributes attributes;
        try {
            attributes = ldap.getAttributes(dn);
            String key;
            Attribute attrs;
            while (names.hasNext()) {
                key = (String) names.next();
                attrs = attributes.get(key);
                if (attrs == null)
                    object.put(key, null);
                else {
                    int L = attrs.size();
                    if (L == 1)
                        object.put(key, attrs.get(0).toString());
                    else {
                        JSON.Array list = new JSON.Array();
                        for (int i=0; i<L; i++) 
                            list.add(attrs.get(i).toString());
                        object.put(key, list);
                    }
                }
            }
        } catch (Exception e) {
            logError(e);
            return false;
        }
        if (test) logInfo(JSON.encode(object), "LDAP resolved");
        return true;
    }
    
    /**
     * Try to create an LDAP context with attributes values from a JSON.O 
     * object for the given attribute names, return true if the context 
     * was created, false otherwise.
     * 
     * @param dn the distinguished name of the context created
     * @param object containing the attribute values to create
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
     * ...
     * 
     * @param dn the distinguished name of the context updated
     * @param object containing the attribute values to update
     * @param names of the attributes to update
     * @return true, false
     */
    public boolean ldapUpdate (
        String dn, JSON.Object object, Iterator names
        ) {
        if (test) logInfo("update LDAP dn=" + dn, less4j);
        // TODO: ldapUpdate ...
        return true;
    }
    
}