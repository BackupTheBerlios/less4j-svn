package org.less4j.tests;

import org.less4j.*;
import org.less4j.protocols.JSON;
import org.less4j.protocols.JSONR;
import org.less4j.serlvet.Actor;
import org.less4j.serlvet.Service;
import org.less4j.simple.Strings;

/**
 * A job function.
 */
public class Job implements Service {

    public static Service singleton = new Job ();
    
    private JSONR.Type _interface;
    
    public String jsonInterface (Actor $) {
        return "{\"seconds\":3599, \"ms\": 99}";
    }
    
    public boolean less4jConfigure (Actor $) {
        try {
            _interface = JSONR.compile(jsonInterface($)); 
            return true;
        } catch (JSON.Error e) {
            $.logError(e);
            return false;
        }
    }
    
    public boolean irtd2Identify (Actor $) {
        $.identity = Strings.password(10);
        $.rights = "";
        return true;
    }
    
    public Object jsonRegular (Actor $) {
        return new JSONR (_interface, 1, 2);
    }
    
    public void jsonApplication (Actor $) {
        $.jsonResponse(200);
        long t = System.currentTimeMillis();
        int sleep_ms = $.json.intValue("ms", 0) + 1;
        int loop_for = sleep_ms * 100;
        long sleep_to = t + (($.json.intValue("seconds", 0) + 1) * 1000);
        try {
            while (System.currentTimeMillis() < sleep_to) {
                Thread.sleep(sleep_ms); // simulate latency on I/O
                for (int i=0; i<loop_for; i++) { // consume CPU and RAM
                    new JSON.Object();
                }
            }
            t = System.currentTimeMillis() - t;
            $.logInfo("done in " + (t/1000) + " seconds", "JOB");
        } catch (InterruptedException e) {
            t = System.currentTimeMillis() - t;
            $.logInfo("interrupted after " + (t/1000) + " seconds", "JOB");    
        }
        Thread.yield();
    }
    
    public void httpResource (Actor $) {
        $.jsonResponse( 
            200, 
            "{\"seconds\": 59, \"ms\": 99}"
            );
    }
    
    public void httpContinue (Actor $, String method, String contentType) {
        $.httpError(400); // Bad request
    }
    
    /**
     * ...
     * 
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

}
