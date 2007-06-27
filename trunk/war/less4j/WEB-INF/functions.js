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

importPackage(Packages.org.less4j); // Rhino rules!

var functions = {};

function jsonRegularFunction (fun, jsonr, containers, iterations) {
    fun.jsonr = jsonr;
    if (jsonr == null) {
        fun.jsonRegular = function ($) {
            return new JSON(containers, iterations);
        }
    } else {
        var type = JSONR.compile(jsonr);
        fun.jsonRegular = function ($) {
            return new JSONR(type, containers, iterations);
        }
    }
    return fun;
}

var httpResources = {}

function less4jConfigure ($) {
    var sb = ['{'];
    for (var k in functions) {
        sb.push(JSON.encode(k));
        sb.push(':');
        sb.push(functions[k].jsonr);
        sb.push(',')
    }
    if (sb.length > 1)
        sb[sb.length] = '}';
    else
        sb[0] = "{}";
    httpResources["/"] = sb.join('');
    return true;
}

function irtd2Identify ($) {
    if ($.about == null) { // the root is the anonymous identifier
        $.identity = Simple.password(10);
        return true;
    } else {
        $.httpError(401) // Not authorized
        return false;
    }
}

function httpResource ($) {
    var res;
    if ($.about == null)
       res = httpResources["/"];
    else
       res = httpResources[$.about];
    if (res == null)
        $.httpError(404);
    else
        $.httpResponse(200, res, "text/javascript", "UTF-8");
}

function jsonRegular ($) { // dispatch a function
    var fun = functions[$.about];
    if (fun && fun.jsonRegular)
        return fun.jsonRegular($);

    return new JSON();
}

function jsonApplication ($) { // dispatch a function
    var fun = functions[$.about];
    if (fun != null) {
        $.jsonResponse(fun($));
    } else if ($.about == null)
        if ($.json.containsKey("expression")) { // evaluate an expression
            $.json.put("result", eval("("+$.json.S("expression")+")"));
            $.jsonResponse(200);
        } else if ($.json.containsKey("script")) {
            $.json.put("result", eval("{"+$.json.S("script")+"}"));
            $.jsonResponse(200);
        } else
            $.jsonResponse(400, '"404 Bad Request"'); // Bad Request
    else
        $.jsonResponse(400, '"404 Bad Request"'); // Bad Request
}