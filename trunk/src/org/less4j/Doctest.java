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

package org.less4j;

import java.util.Iterator;

import java.io.File;
import java.io.FileOutputStream;

import com.sun.javadoc.RootDoc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.Tag;
import com.sun.javadoc.Type;

import org.mozilla.javascript.*;

/**
 * Leverages javadoc to produce a practical web of XHTML fragment, a JSON 
 * index and JavaScript test suites. 
 *  
 * @synopsis <javadoc 
 *    docletpath="./less4j.jar;./lib/xp.jar;"
 *    doclet="org.less4j.Doctest"
 *    packagenames="org.less4j" 
 *    source="1.4" 
 *    sourcepath="src" 
 *    classpath="./less4j.jar;lib/servlet-api.jar;lib/smalljs.jar;./lib/xp.jar;/jdk1.5.0_11/lib/tools.jar" 
 *    access="public" 
 *    >
 * 
 * @synopsis java Doctest.java ./tests 1> tests.out 2> tests.err
 * 
 * @div <h3>Documentation and Test</h3>
 * 
 * <p>Documentation is one purpose of Doctest, the other is test.</p>
 * 
 * <p>Text tagged with <code>@test</code> is compiled in a JavaScript 
 * function and saved in one big script that will execute each test.
 * The only pre-processing of sources done is the extraction of import
 * statements.</p>
 * 
 * <p>Tests are expected to be a sequence of JavaScript statements that
 * return a boolean. For instance, here's a dumb test:</p>
 * 
 * @test return "A".lowercase().equals("a");
 * 
 * @div <p>In the context of org.less4j's doctests, it will yield the 
 * following JavaScript sources:</p>
 * 
 * <pre>importPackage(Packages.org.less4j);
 *var prefix = '... org.less4j.Doclet.main ';
 *function out () {
 *    System.out.println(prefix + arguments.join(' '));
 *}
 *function err () {
 *    System.err.println(prefix + arguments.join(' '));
 *}
 *function test () {
 *    <strong>return 'A'.lowercase().equals('a');</strong>
 *}
 *var result = false;
 *try {
 *    result = test();
 *    out('OK');
 *} catch (e) {
 *    err(e.toString());
 *}
 *return (result===true);</pre>
 * 
 * <p>The test sources are saved individually, named with their SHA1
 * hexdigest.</p>
 * 
 * @jsonr {
 *    "local": "[a-zA-Z$][a-zA-Z$0-9_]*",
 *    "qualified": "[a-zA-Z$][a-zA-Z$0-9_]*([.][a-zA-Z$][a-zA-Z$0-9_]+)+",
 *    "index": {"local": ["qualified"]},
 *    "packages": ["qualified"],
 *    "classes": {"local": ["local"]}
 *}
 */
public class Doctest {
    
    protected RootDoc root;
    protected JSON.Object options;
    protected String base;
    protected JSON.Object index = new JSON.Object();
    protected JSON.Array packages = new JSON.Array();
    protected JSON.Object classes = new JSON.Object();
    protected String packageName = null;
    protected String className = null;
    
    protected static final String DOCTYPE_HTML_4_01_EN_STRICT = (
        "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\"\r\n"
        + "\"http://www.w3.org/TR/html4/strict.dtd\">\r\n"
        );
    
    protected static XML.Element doXML (String text, String tag) {
        try {
            XML.Document doc = new XML.Document(); 
            doc.parse("<" + tag +">" + text + "</" + tag + ">");
            return doc.root;
        } catch (Throwable e) {
            XML.Element error = new XML.Element(
                "div", new String[]{"class", "xml"}, null, null
                );
            error.addChild(
                "div", new String[]{"class", "source"}, text, null
                );
            error.addChild(
                "div", new String[]{"class", "error"}, e.getMessage(), null
                );
            return error;
        }
    }

    protected static final String test_prologue = (
        "function out () {\r\n"
        + "    System.out.println(prefix + arguments.join(' '));\r\n"
        + "}\r\n"
        + "function err () {\r\n"
        + "    System.err.println(prefix + arguments.join(' '));\r\n"
        + "}\r\n"
        + "function test () {\r\n"
        );
    
    protected static final String test_epilogue = (
        "\r\n}\r\n"
        + "function run_test () {\r\n"
        + "    var result = false;\r\n"
        + "    try {\r\n"
        + "        result = test();\r\n"
        + "        out('OK');\r\n"
        + "    } catch (e) {\r\n"
        + "        err(e.toString());\r\n"
        + "    }\r\n"
        + "    return (result===true);\r\n"
        + "}"
        );
    
    protected XML.Element doTest(String source, String localName) 
    throws Exception {
        String fqn = packageName + "." + localName;
        SHA1 sha1 = new SHA1();
        sha1.update(Simple.encode(source, "UTF-8"));
        String hash = sha1.hexdigest();
        String test = (
            "importPackage(" + packageName + ");\r\n" 
            + "var prefix = '" + hash + " " + fqn + " ';\r\n"
            + test_prologue 
            + source
            + test_epilogue
            );
        XML.Element doctest = new XML.Element("div", new String[]{
            "class", "test",
            }, null, null);
        doctest.addChild(
            "div", new String[]{"class", "source"}, source, null
            );
        ScriptableObject scope;
        Context cx = Context.enter();
        try {
            scope = new ImporterTopLevel(cx, false);
            cx.evaluateString(
                scope, "function(){" + source + "}", hash, 1, null
                );
            (new FileOutputStream(new File(
                base + "/tests/" + hash + ".js"
                ))).write(Simple.encode(test, "UTF-8"));
        } catch (Exception e) {
            doctest.addChild(
                "div", new String[]{"class", "error"}, e.getMessage(), null
                );
        } finally {
            Context.exit();
        }
        return doctest;
    }

    protected static String simpleName(String name) {
        return name.substring(name.lastIndexOf('.')+1);
    }
    
    protected static void doParameters (MethodDoc method, XML.Element parent) 
    throws Exception {
        String text;
        Tag[] tags = method.paramTags();
        Parameter[] parameters = method.parameters();
        XML.Element tr;
        XML.Element table = parent.addChild("table", new String[]{
            "class", "parameters"
            });
        for (int i=0; i<tags.length; i++) {
            text = tags[i].text();
            if (parameters.length > i) {
                tr = table.addChild(
                    "tr", new String[]{"class", "parameter"}
                    );
                tr.addChild("td", new String[]{
                    "class", "type"
                    }, simpleName(parameters[i].typeName()), null
                    );
                int sep = text.indexOf(' ');
                if (sep > -1) {
                    tr.addChild("td", new String[]{
                        "class", "identifier"    
                        }, text.substring(0, sep), null);
                    tr.addChild(doXML(text.substring(sep+1), "td"));
                } else {
                    tr.addChild("td", new String[]{
                        "class", "identifier",
                        }, text, null);
                    tr.addChild("td", new String[]{
                        "class", "description"    
                        }, "...", null);
                }
            }
        }
    }
    
    protected static void doMethodTags (
        MethodDoc method, 
        XML.Element parent, 
        String base, 
        String packageName,
        String localName
        ) throws Exception {
        Tag[] tags = method.tags();
        String name, text;
        XML.Element tr;
        XML.Element table = parent.addChild("table", new String[]{
            "class", "tags"
            });
        for (int i=0; i<tags.length; i++) {
            name = tags[i].name();
            text = tags[i].text();
            if (name.equals("@param") && method.parameters().length > i) {
                tr = table.addChild(
                    "tr", new String[]{"class", "parameter"}
                    );
                tr.addChild("td", new String[]{
                    "class", "type"
                    }, simpleName(method.parameters()[i].typeName()), null
                    );
                int sep = text.indexOf(' ');
                if (sep > -1) {
                    tr.addChild("td", new String[]{
                        "class", "identifier"    
                        }, text.substring(0, sep), null);
                    tr.addChild(doXML(text.substring(sep+1), "td"));
                } else {
                    tr.addChild("td", new String[]{
                        "class", "identifier",
                        }, text, null);
                    tr.addChild("td", new String[]{
                        "class", "description"    
                        }, "...", null);
                }
            } else if (name.equals("@return")) {
                tr = table.addChild(
                    "tr", new String[]{"class", "return"}
                    );
                tr.addChild("td", "Return");
                tr.addChild("td", new String[]{
                    "class", "type"
                    }, simpleName(method.returnType().typeName()), null
                    );
                tr.addChild(doXML(text, "td"));
            } else if (name.equals("@throws")) {
                tr = table.addChild(
                    "tr", new String[]{"class", "throws"}
                    );
                tr.addChild("td", "Throws");
                Type[] exceptions = method.thrownExceptions();
                String[] names = new String[exceptions.length];
                for (int j=0; j<names.length; j++)
                    names[j] = exceptions[j].simpleTypeName();
                tr.addChild("td", new String[]{
                    "class", "type"
                    }, Simple.join(", ", Simple.iter(names)), null
                    );
                tr.addChild(doXML(text, "td"));
            }
        }
    }
    
    protected void doTags(
        Tag[] tags, 
        XML.Element parent, 
        String localName
        ) throws Exception {
        String name, text;
        for (int i=0; i<tags.length; i++) {
            name = tags[i].name();
            text = tags[i].text();
            if (
                name.equals("@param") ||
                name.equals("@return") ||
                name.equals("@throws")
                ) {
                ; // filters out method tags
            } else if (name.equals("@div")) {
                parent.addChild(doXML(text, "div"));
            } else if (name.equals("@test")) {
                parent.addChild(doTest(tags[i].text(), localName));
            } else
                parent.addChild("div", new String[]{
                    "class", name.substring(1)
                    }, tags[i].text(), null);
        }
    }
    
    protected void doIndex (String name, String fqn) 
    throws JSON.Error {
        if (!index.containsKey(name))
            index.put(name, new JSON.Array());
        index.A(name).add(fqn);
    }
    
    protected void doField(FieldDoc field) throws Exception {
        String name = field.name();
        String fqn = packageName + "." + className + "." + name;
        doIndex(name, fqn);
        XML.Document html = new XML.Document();
        String css = "javaField " + field.modifiers();
        html.root = new XML.Element("div");
        html.root.addChild("h2", className); 
        html.root.addChild("h3", new String[]{
            "class", css
            }, name, null);
        html.root.addChild(doXML(field.commentText(), "p"));
        doTags(field.tags(), html.root, fqn);
        html.write(new File(base + "/fragments/" + fqn), "");
    }
    
    protected void doMethod(MethodDoc method, XML.Element root) 
    throws Exception {
        String name = method.name();
        String signature = name + method.flatSignature();
        String localName = className + "." + signature;
        String css = ((method.isConstructor()) ?
            "javaConstructor ": "javaMethod "
            ) + method.modifiers();
        XML.Element javaMethod = root.addChild("div"); 
        XML.Element title = javaMethod.addChild("h3");
        title.addChild("span", new String[]{
            "class", css 
            }, method.name(), " ");
        title.addChild("span", method.flatSignature());
        doMethodTags(method, javaMethod, base, packageName, localName);
        javaMethod.addChild(doXML(method.commentText(), "p"));
        doTags(method.tags(), javaMethod, localName);
    }
        
    protected void doClass(ClassDoc Class) throws Exception {
        className = Class.name();
        String fqn = packageName + "." + Class.name();
        doIndex(className, fqn);
        JSON.Array members = new JSON.Array();
        classes.put(fqn, members);
        XML.Document html = new XML.Document();
        html.root = new XML.Element("div", new String[]{
            "class", (Class.isInterface()) ? "javaInterface" : "javaClass" 
            }, null, null);
        html.root.addChild("h2", className);
        // fields
        FieldDoc[] fields = Class.fields();
        for (int i = 0; i < fields.length; ++i) 
            if (fields[i].isPublic()) {
                doField(fields[i]);
                members.add(fields[i].name());
            }
        // methods
        MethodDoc[] methods = Class.methods();
        JSON.Object pages = new JSON.Object();
        for (int i = 0; i < methods.length; ++i) {
            if (
                methods[i].isPublic() && 
                !pages.containsKey(methods[i].name())
                ) {
                XML.Document method = new XML.Document();
                method.root = new XML.Element(
                    "div", new String[]{"class", "javaMethods"}, null, null
                    );
                method.root.addChild("h2", className); 
                pages.put(methods[i].name(), method);
            }
        }
        String methodName;
        for (int i = 0; i < methods.length; ++i)
            if (methods[i].isPublic()) {
                methodName = methods[i].name();
                doMethod(
                    methods[i], ((XML.Document) pages.get(methodName)).root
                    );
            }
        html.root.addChild(doXML(Class.commentText(), "div"));
        doTags(Class.tags(), html.root, className);
        html.write(new File(base + "/fragments/" + fqn), "");
        Iterator names = pages.keySet().iterator();
        while (names.hasNext()) {
            methodName = (String) names.next();
            doIndex(
                methodName, packageName + "." + className + "." + methodName
                );
            members.add(methodName);
            ((XML.Document) pages.get(methodName)).write(new File (
                base + "/fragments/" + fqn + "." + methodName
                ), "");
        }
    }
    
    protected void doPackage(PackageDoc Package) throws Exception {
        packageName = Package.name();
        packages.add(packageName);
        ClassDoc[] classes = Package.allClasses();
        for (int i = 0; i < classes.length; ++i) 
            if (classes[i].isPublic()) // don't doctest protected and protected
                doClass(classes[i]);
    }
    
    protected static void doDirectory (String path) throws Exception {
        File dir = new File(path);
        if (!dir.exists())
            dir.mkdir();
        else if (!dir.isDirectory()) {
            throw new Exception(path + " is not a directory");
        }
    }
    
    protected boolean doRoot (RootDoc root) throws Exception {
        options = Simple.dict(root.options());
        base = options.S("-destdir", "javadoc");
        doDirectory(base);
        doDirectory(base + "/fragments");
        PackageDoc[] docs = root.specifiedPackages();
        try {
            for (int i = 0; i < docs.length; ++i) {
                doPackage(docs[i]);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return false;
        }
        FileOutputStream os = new FileOutputStream(new File(
            base + "/index.js"
            ));
        os.write(Simple.encode("javadoc.index = ", "UTF-8"));
        JSON.pprint(index, os);
        os.write(Simple.encode(";\r\njavadoc.packages = ", "UTF-8"));
        JSON.pprint(packages, os);
        os.write(Simple.encode(";\r\njavadoc.classes = ", "UTF-8"));
        JSON.pprint(classes, os);
        os.flush();
        os.close();
        return true;
    }
    
    /**
     * The entry point exposed to the <code>javadoc</code> executable.
     * 
     * @param root of the Doclet tree
     * @return true if no exception was throwed, false otherwise
     */
    public static boolean start(RootDoc root) throws Exception {
        return (new Doctest()).doRoot(root);
    }
    
    /**
     * Run the JavaScript test suite generated from the source for one or more 
     * packages, log successes to STDOUT and errors to STDERR, as regular JSON 
     * pairs made of the test source SHA1 hexdigest and a text message, exit
     * the number of failed tests (ie: zero if all are OK).
     * 
     * @param arguments a list of package names
     *  
     * @synopsis java Doctest org.less4j \
     *  -classpath ./less4j.jar;./lib/smalljs.jar;./lib/xp.jar \
     *  1> doctests.out 2> doctests.err 
     * 
     * @div Test results are simple text messages prefixed by a hash of the
     * sources running, uniquely identifying the same tests accross versions
     * allowing to distinguish between old and new tests when comparing
     * two test logs.
     */
    public static void main (String[] arguments) {
        // boolean pass = ((Boolean) Context.jsToJava(scriptCall(
        //    $, cx, scope, run
        //    ), Boolean.class)).booleanValue();
    } 
}
