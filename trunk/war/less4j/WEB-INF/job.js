/*  Copyright (C) 2007 Laurent A.V. Szyster

This library is free software; you can redistribute it and/or modify
it under the terms of version 2 of the GNU Lesser General Public License as
published by the Free Software Foundation.

    http://www.gnu.org/copyleft/lesser.html

This library is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

You should have received a copy of the GNU Lesser General Public License
along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA */

importClass(java.lang.System);
importClass(java.lang.Thread);

/**
 * An unobvious right way to do it wrong in a J2EE container.
 * 
 * This is a simulation of a long-running jobs in a J2EE container, 
 * controlled and executed in one thread taken from the server's 
 * pool. 
 * 
 */
functions["/job"] = new Service ({
    jsonApplication: function ($) {
        // first test the database and/or the directory for a lock on the
        // current job list if this is a strictly synchronized batch. ie:
        // try to an INSERT statement in an SQL database or create a new
        // context in an LDAP directory, report the exception and stop
        // or success and ...
        
        $.jsonResponse(200);
        // ... continue with the job processing.
        var i;
        var t = System.currentTimeMillis();
        var sleep_to = t + (parseInt($.json.get("seconds")) + 1)*1000;
        var sleep_ms = parseInt($.json.get("ms")) + 1;
        var loop_for = sleep_ms * 100;
        while (System.currentTimeMillis() < sleep_to) {
            Thread.sleep(sleep_ms);
            for (i=0; i<loop_for; i++);
        }
        var t = System.currentTimeMillis() - t;
        // log precision
        $.logInfo("done in " + (t/1000) + " seconds", "JOB");
        // just in case the pool needs a signal ...
        Thread.yield(); 
    } 
});