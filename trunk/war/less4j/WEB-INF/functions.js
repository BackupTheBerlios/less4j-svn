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
Service.prototype.httpContinue = function ($, httpMethod, contentType) {
    $.jsonResponse(400);
};

// re-implement the Controller in JavaScript

var functions = {};

function less4jConfigure ($) {
    Service.controller = this;
    for (k in functions) {
        if (!functions[k].less4jConfigure($) && !$.test)
            return false;
    }
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
        sb.push(functions[k].jsonInterface($));
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
    if (fun != null) {
        fun.jsonApplication($); // delegate ...
    } else
        $.jsonResponse(501); // Not implemented
}

// add the three basic functions for a practical Rhino controller

functions["/login"] = new Service ({
    irtd2Identify: function ($) {
        return true;
    },
    jsonInterface: function ($) {
        return ('{'
            + '"username": "[\\\\x21-\\\\x7E]+", '
            + '"password": "[\\\\x21-\\\\x7E]+"'
            + '}');
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
        $.json.put("result", eval("("+$.json.S("arg0", "null")+")"));
        $.jsonResponse(200); // Ok
    }
});

functions["/exec"] = new Service ({
    jsonInterface: function ($) {return '""';},
    jsonApplication: function ($) {
        $.json.put("result", eval(
            "(function(){"+$.json.S("arg0", "null")+"})()"
            ));
        $.jsonResponse(200); // Ok
    }
});

functions["/services"] = new Service ({
    jsonInterface: function ($) {return 'null';},
    jsonRegular: function ($) {return null;},
    httpResource: function ($) {
        res = new JSON.Object();
        res.put('interfaces', JSON.decode(jsonInterface($)));
        var l = JSON.Array(); 
        for (var k in functions) 
            l.add(k); 
        res.put('dynamics', l);
        res.put('statics', Service.controller.functions.keySet().iterator());
        $.jsonResponse(200, res);
    }
});

functions["/reload"] = new Service ({
    jsonInterface: function ($) {return 'null';},
    jsonRegular: function ($) {return null;},
    httpResource: function ($) {
        Service.controller.scriptReload($);
        functions['/services'].httpResource($);
    }
});

functions["/getServiceMethods"] = new Service ({
    jsonInterface: function ($) {
        return '"[\\/][\\\\x20-\\\\x7E]*"';
    },
    jsonApplication: function ($) {
        var fun = functions[$.json.S("arg0")];
        if (!fun) {
            $.json.put("error", 'service not found');
            $.jsonResponse(500);
            return;
        }
        var dummy = function () {};
        var result = new JSON.Object();
        result.put("less4jConfigure", (fun.less4jConfigure||dummy).toSource());
        result.put("irtd2Identify", (fun.irtd2Identify||dummy).toSource());
        result.put("jsonApplication", (fun.jsonApplication||dummy).toSource());
        result.put("httpResource", (fun.httpResource||dummy).toSource());
        result.put("httpContinue", (fun.httpContinue||dummy).toSource());
        $.jsonResponse(200, result);
    }
});

functions["/setServiceMethod"] = new Service ({
    jsonInterface: function ($) {
        return ('{'
            + '"service": "[\\/][\\\\x21-\\\\x7E]*",'
            + '"method": "[\\\\x21-\\\\x7E]+", '
            + '"source": ""'
            + '}');
        },
    jsonApplication: function ($) {
        try {
            functions[$.json.S("service")][$.json.S("method")] = eval ('('
                + $.json.S("source")
                + ')'
                );
            $.json.put("error", null);
        } catch (e) {
            $.json.put("error", e.toString());
        }
        $.jsonResponse(200);
    }
});

