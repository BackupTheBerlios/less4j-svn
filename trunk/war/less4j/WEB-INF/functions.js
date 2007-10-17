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

// prototype a basic Service implementation

var Service = function (implementation) {
    for (var i in implementation) this[i] = implementation[i];
};
Service.prototype.less4jConfigure = function ($) {
    return true;
};
Service.prototype.irtd2Identify = function ($) {
    $.httpError(401);
    return false;
};
Service.prototype.jsonInterface = function ($) {
    return "null";
};
Service.prototype.httpResource = function ($) {
    $.jsonResponse(200, this.jsonInterface($));
};
Service.prototype.jsonRegular = function ($) {
    var model = this.jsonInterface ($);
    if (model == "null")
        return new JSON();
    else
        return new JSONR(JSONR.compile(model));
};
Service.prototype.jsonApplication = function ($) {
    $.jsonResponse(200);
};

// re-implement the Controller in JavaScript

var functions = {};

function less4jConfigure ($) {
    Service.controller = this;
    return true;
}

function jsonInterface ($) {
    var fun, k, sb = ['{'];
    var funs = Service.controller.functions.keySet().iterator();
    while (funs.hasNext()) {
        k = funs.next();
        sb.push(JSON.encode(k));
        sb.push(':');
        sb.push(Service.controller.functions.get(k).jsonInterface($));
        sb.push(',');
    }
    for (k in functions) {
        sb.push(JSON.encode(k));
        sb.push(':');
        fun = functions[k];
        if (!fun.less4jConfigure($) && !$.test)
            return false;
            
        sb.push(fun.jsonInterface($));
        sb.push(',');
    }
    sb.pop();
    if (sb.length > 0) {
        sb.push('}');
        return sb.join('');
    } else
        return '{}';
}

function irtd2Identify ($) {
    var fun = functions[$.about];
    if (fun == null) {
        $.jsonResponse (401, false);
        return false
    }
    return fun.irtd2Identify($);
}

function httpResource ($) {
    if ($.about == null)
        $.jsonResponse (200, jsonInterface($));
    else {
        var fun = functions[$.about];
        if (fun == null)
            $.httpError(404);
        else
            fun.httpResource($);
    }
}

function jsonRegular ($) { // dispatch a function
    var fun = functions[$.about];
    if (fun && fun.jsonRegular)
        return fun.jsonRegular($);

    return new JSON();
}

function jsonApplication ($) { // dispatch a function
    var fun = functions[$.about];
    if (fun != null)
        fun.jsonApplication($); // delegate ...
    else
        $.jsonResponse(501); // Not implemented
}

// add the three basic functions for a practical Rhino controller

functions["/login"] = new Service ({
    irtd2Identify: function ($) {
        return true;
    },
    jsonInterface: function ($) {
        return '{"username": "[^ ]+", "password": "[^ ]+"}';
    },
    jsonApplication: function ($) {
        $.identity = $.json.S("username");
        $.rights = "";
        $.irtd2Digest();
        $.jsonResponse(200, true); // Ok
    }
});

functions["/eval"] = new Service ({
    jsonInterface: function ($) {return '""';},
    jsonApplication: function ($) {
        $.json.put("result", eval("("+$.json.S("arg0")+")"));
        $.jsonResponse(200); // Ok
    }
});

functions["/exec"] = new Service ({
    jsonInterface: function ($) {return '""';},
    jsonApplication: function ($) {
        $.json.put("result", eval("{"+$.json.S("arg0")+"}"));
        $.jsonResponse(200); // Ok
    }
});