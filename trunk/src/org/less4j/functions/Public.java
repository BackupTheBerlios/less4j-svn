package org.less4j.functions;

import org.less4j.Service;
import org.less4j.Actor;
import org.less4j.protocols.JSON;
import org.less4j.simple.Strings;

/**
 * A base class to derive public functions.  
 */
public class Public implements Service {
    /**
     * This function's single instance.
     */
    public Service singleton = new Public ();
    /**
     * Return the string "null" as a regular JSON interface description, 
     * accept by default any JSON types and values.
     */
    public String jsonInterface (Actor $) {
        return "null";
    }
    /**
     * Allways return true.
     */
    public boolean less4jConfigure (Actor $) {
        return true;
    }
    /**
     * Identify any request with a random string of 10 characters and
     * set this request <code>Actor</code>'s right to the empty string.
     */
    public boolean irtd2Identify (Actor $) {
        $.identity = Strings.password(10);
        $.rights = "";
        return true;
    }
    /**
     * Return an unlimited <code>JSON</code> interpreter, accept by default 
     * any JSON types and values.
     */
    public Object jsonRegular (Actor $) {
        return new JSON();
    }
    /**
     * Echo the JSON input state without transformation for non-idempotent
     * GET or POST requests with a JSON or urlencoded body.
     */
    public void jsonApplication (Actor $) {
        $.jsonResponse(200);
    }
    /**
     * Send this function's regular JSON interface description as response 
     * to idempotent GET requests.
     */
    public void httpResource (Actor $) {
        $.jsonResponse(200, jsonInterface($));
    }
    /**
     * Reply with an HTTP <code>400 Bad Request</code> error response.
     */
    public void httpContinue (Actor $, String method, String contentType) {
        $.httpError(400);
    }
}