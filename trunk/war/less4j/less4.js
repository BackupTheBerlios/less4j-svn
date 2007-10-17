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

var css = function (selector, root) {
    css.selected = CSS.select(selector, root);
    return css;
};
css.extend = function (name, fun) {
    css[name] = function (selected) {
        map(fun, selected || css.selected); 
        return css;
    };
};
css.extend('hide', function (el) {
    CSS.add(el, ['hide']);
    });
css.extend('show', function (el) {
    CSS.remove(el, ['hide']);
    });

HTTP.except = function (key, e) {
    HTML.insert(
       $('httpExceptions'), 
       '<div>' 
       + HTML.cdata(key) 
       + ': ' 
       + HTML.cdata(e.messageText) 
       + '</div>', 
       'afterBegin'
       );
};

HTML.onload.push(function () {
    less4js.describeServices('script');
});

var less4js = {};

less4js.services = {};

less4js.login = function () {
    JSON.POST(
        'script/login', 
        {
            'username': $('username').value,
            'password': $('password').value,
            }, 
        function ok (req) {
            css.hide([$('Login')]);
            css.show([$('Subject'), $('Object')]);
            less4js.describeServices ('script');
        });
}

less4js.describeServices = function (url) {
    JSON.GET(url, null, function (req) {
        less4js.services = JSON.decode(req.responseText);
        var sb = [];
        for (var k in less4js.services) {
            sb.push('<div><span class="action" onclick="less4js.selectService(');
            sb.push(HTML.cdata(JSON.encode(k)));
            sb.push(');">');
            sb.push(HTML.cdata(k));
            sb.push('</span></div>');
        }
        HTML.update($('servicesMenu'), sb.join(''));
        css.show([$('Subject'), $('Object')]);
    });
};

less4js.Controller = Protocols(JSON.Regular);

less4js.toggle = function (from, to) {
    var hide = ['hide'];
    CSS.add($(from), hide);
    CSS.remove($(to), hide);
};

less4js.fitTextToArea = function (el, outlined) {
    el.value = outlined;
    var lines = outlined.split('\r\n').length + 1;
    el.parentNode.style.height = (1.5*lines) + "em";
    el.rows = lines;
}

less4js.modelRefresh = function (model) {
    less4js.fitTextToArea($('jsonrModel'), JSON.pprint([], model).join(''));
}

less4js.selectService = function (name) {
    $('serviceName').value = name;
    less4js.modelRefresh (less4js.services[name]);
    less4js.control = new less4js.Controller('less4js.control');
    less4js.control.extensions = {};
    less4js.control.json = {};
    less4js.updateViewInput();
    less4js.loadSources(name);
}


less4js.updateViewInput = function () {
	try {
		less4js.control.model = eval('('+$('jsonrModel').value+')');
		HTML.update($('jsonrModelError'), '');
	} catch (e) {
		HTML.update($('jsonrModelError'), HTML.cdata(e.toString()));
		return;
	}
	HTML.update($('jsonrViewInput'), less4js.control.view());
	// hide('jsonrModelTab');
	// show('jsonrViewTab');
}

less4js.postView = function () {
    JSON.POST(
        'script' + $('serviceName').value,
        less4js.control.json,
        JSON.update('jsonrViewOutput', 'output')
        );
}

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

less4js.loadSources = function (name) {
    JSON.POST(
        'script/exec', 
        '(function () {'
        + 'var dummy = function () {};'
        + 'var fun = functions["' + name + '"];'
        + 'var result = new JSON.Object();'
        + 'result.put("less4jConfigure", (fun.less4jConfigure||dummy).toSource());'
        + 'result.put("irtd2Identify", (fun.irtd2Identify||dummy).toSource());'
        + 'result.put("jsonApplication", (fun.jsonApplication||dummy).toSource());'
        + 'result.put("httpResource", (fun.httpResource||dummy).toSource());'
        + 'result.put("httpContinue", (fun.httpContinue||dummy).toSource());'
        + 'return result;'
        + '})()',
        function ok (request) {
            var json = JSON.decode(request.responseText)['result'];
            for (var k in json) {
                less4js.fitTextToArea($(k), jsbeauty(json[k]));
            };
        });
};

JSON.errors['401'] = function NotAuthorized (status, request) {
    css.hide([$('Subject'), $('Object')]);
    css.show([$('Login')]);
}
