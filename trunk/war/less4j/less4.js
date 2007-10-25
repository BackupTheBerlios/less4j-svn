/* Copyright (C) 2007 Laurent A.V. Szyster

This library is free software; you can redistribute it and/or modify
it under the terms of version 2 of the GNU General Public License as
published by the Free Software Foundation.

    http://www.gnu.org/copyleft/gpl.html

This library is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

You should have received a copy of the GNU General Public License
along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA */

$$.extend('hide', function (el) {CSS.add(el, ['hide']);});

$$.extend('show', function (el) {CSS.remove(el, ['hide']);});

$$.extend('fitTextareaHeight', function (el) {
    var lines = el.value.split('\r\n').length;
    el.parentNode.style.height = (1.5*lines) + "em";
    el.rows = lines;
});


HTML.onload.push(function () {
    JSON.GET('script/services', null, less4js.refreshServices);
});

JSON.errors['401'] = function NotAuthorized (status, request) {
    $$('#Login').show();
    $$('#Subject, #Object').hide();
}

if ((function(){}).toString)
    function jsbeauty (source) {
        var lines, i, s;
        lines = eval(source).toString().split('\n');
        lines[0] = '';
        lines.pop();
        for (i=1; s=lines[i]; i++) {
            lines[i] = lines[i].substr(4) + '\r\n';
        }
        return lines.join('');
    }
else
    function jsbeauty (source) {return source};


var less4js = {
    Controller: Protocols(JSON.Regular),
    services: {
        interfaces: {},
        dynamics: [],
        statics: []
    }
};

less4js.fitTextToArea = function (el, outlined) {
    var lines = outlined.split('\r\n').length;
    el.parentNode.style.height = (1.5*lines) + "em";
    el.rows = lines;
    el.value = outlined;
}

less4js.control = new less4js.Controller('less4js.control');

less4js.login = function () {
    JSON.POST(
        'script/login', 
        {
            'username': $('username').value,
            'password': $('password').value
            }, 
        function () {
            JSON.GET('script/services', null, less4js.refreshServices);
        });
}

less4js.refreshServices = function (request) {
    less4js.services = JSON.decode(request.responseText);
    var sb;
    if (less4js.services.dynamics.length > 0) {
        less4js.services.dynamics.sort();
        sb = ['<h3>Dynamics</h3><div class="actions">'];
        for (var i=0, k; k=less4js.services.dynamics[i]; i++) {
            sb.push('<span class="action" onclick="less4js.selectDynamic(');
            sb.push(HTML.cdata(JSON.encode(k)));
            sb.push(');">');
            sb.push(HTML.cdata(k));
            sb.push('</span>');
            sb.push(', ');
        }
        sb.pop();
        sb.push('</div>')
        HTML.update($('serviceDynamics'), sb.join(''));
    } else
        HTML.update($('serviceDynamics'), '');
    if (less4js.services.statics.length > 0) {
        less4js.services.statics.sort();
        sb = ['<h3>Statics</h3><div>'];
        for (var i=0, k; k=less4js.services.statics[i]; i++) {
            sb.push('<span class="action" onclick="less4js.selectStatic(');
            sb.push(HTML.cdata(JSON.encode(k)));
            sb.push(');">');
            sb.push(HTML.cdata(k));
            sb.push('</span>');
            sb.push(', ');
        }
        sb.pop();
        sb.push('</div>')
        HTML.update($('serviceStatics'), sb.join(''));
    } else
        HTML.update($('serviceStatics'), '');
    $$('#Login').hide();
    $$('#Subject, #Object').show();
};

less4js.toggle = function (_from, _to) {
    $$('h3.'+_from + ', div.'+_from).hide();
    $$('h3.'+_to + ', div.'+_to).show();
};

less4js.modelRefresh = function (model) {
    less4js.fitTextToArea($('jsonrModel'), JSON.pprint([], model).join(''));
}

less4js.selectDynamic = function (name) {
    $('serviceName').value = name;
    less4js.modelRefresh (less4js.services.interfaces[name]);
    less4js.control = new less4js.Controller('less4js.control');
    less4js.updateViewInput();
    less4js.loadSources(name);
    HTML.update($('jsonrViewOutput'), '');
}

less4js.serviceStatics = function (name) {
    $('serviceName').value = name;
    less4js.modelRefresh (less4js.services.interfaces[name]);
    less4js.control = new less4js.Controller('less4js.control');
    less4js.updateViewInput();
    HTML.update($('jsonrViewOutput'), '');
}

less4js.updateViewInput = function () {
	try {
		less4js.control.model = eval('('+$('jsonrModel').value+')');
		HTML.update($('jsonrModelError'), '');
	} catch (e) {
		HTML.update($('jsonrModelError'), HTML.cdata(e.toString()));
		return;
	}
	var t = less4js.control.type();
	if (t == "collection" || t == "relation")
	    less4js.control.json = [];
	else if (t == "namespace" || t == "dictionary")
	    less4js.control.json = {};
    else
	    less4js.control.json = null;
	HTML.update($('jsonrViewInput'), less4js.control.view());
}

less4js.postView = function () {
    HTTP.request(
        'POST', 'script' + $('serviceName').value, {
            'Content-type': 'application/json'
        },
        JSON.encode(less4js.control.json),
        function ok (req) {
            var pre = HTML.cdata(
                JSON.pprint([], JSON.decode(req.responseText)).join('')
                );
            HTML.update($('jsonrViewOutput'), '<pre>' + pre + '</pre>');
        },
        function error (status, req) {
            HTML.update(
                $('jsonrViewOutput'), 
                '<code class="error">' 
                + status
                + '</code><tt>' 
                + HTML.cdata(req.responseText)
                + '</tt>'
                );
        });
}

less4js.loadSources = function (name) {
    JSON.POST(
        'script/getServiceMethods', name,
        function ok (request) {
            var beautified;
            var json = JSON.decode(request.responseText);
            for (var k in json) {
                less4js.fitTextToArea($(k), jsbeauty(json[k]));
            };
        });
};

less4js.updateSource = function (el) {
    JSON.POST('script/setServiceMethod', {
        "service": $('serviceName').value,
        "method": el.id,
        "source": 'function ($, httpMethod, contentType) {' + el.value + '}'
        }, function ok (request) {
            var json = JSON.decode(request.responseText);
            if (json.error)
                CSS.add (el, ['error']);
            else
                CSS.remove (el, ['error']);
        });
}

