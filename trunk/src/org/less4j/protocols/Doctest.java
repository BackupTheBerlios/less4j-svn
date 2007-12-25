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

package org.less4j.protocols;

import java.util.Iterator;
import java.util.HashSet;

import java.io.File;
import java.io.FileOutputStream;

import com.sun.javadoc.RootDoc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.ExecutableMemberDoc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.Tag;
import com.sun.javadoc.ParamTag;
import com.sun.javadoc.Type;

import org.mozilla.javascript.*;

/**
 * Produce a practical web of XHTML fragment, a JSON index and a JavaScript 
 * test suite, enable developers to document and test their sources in the
 * same place and at the same time they change the code.
 * 
 * @h3 Document
 * 
 * @p Here is what a brief Doctest comment would look like for java's 
 * <code>String.equals</code> method:
 * 
 * @pre /**
 * * Compare this <code>String</code> to another one.
 * * 
 * * @param string to compare to 
 * * @return <code>true</code> if the two strings compared are equals
 * *
 * * @p For instance:
 * *
 * * @pre "A".equals("A");
 * *
 * * @p A description of <em>this</em> synopsis is not required and good
 * * synopsis should be obvious enough to avoid one.
 * *
 * * @test return (
 * *    "A".equals("A") == true &&
 * *    "A".equals("a") == false
 * *    );
 * * /
 *
 * @p For documentation purposes <code>Doctest</code> supports the original 
 * javadoc tags: <code>@param</code>, <code>@return</code> and 
 * <code>@throws</code>. 
 * 
 * @p To simply enhance your documentation with HTML elements without error, 
 * it maps a <code>@h3</code>, <code>@h4</code>, <code>@p</code> and 
 * <code>@pre</code> tag to the equivalent HTML titles, paragraph and 
 * preformatted text elements. Any other tag (including <code>test</code>) 
 * will yield a <code>div</code> element with a CSS class named after the tag. 
 * 
 * @p You can still use XHTML markup inside the tagged and the untagged 
 * comment text, Doctest will try to validate it and document any XML errors
 * in its output.
 * 
 * @p Comments for fields may be quite shorter and lack tests or synopsis,
 * class documentation may include a longer description of its application
 * purposes.
 *
 * @h3 Test
 * 
 * @p Text tagged with <code>@test</code> is compiled in a JavaScript 
 * function and saved individually, named with their SHA1 hexdigest.
 * 
 * @p Tests are expected to be a simple script that returns a boolean.
 * 
 * @p For instance here are three tests. The first one pass:
 * 
 * @test return "A".toLowerCase().equals("a");
 * 
 * @p The second fails: 
 * 
 * @test return "A".equals("a");
 * 
 * @p A third one that raise and exception (and fails): 
 * 
 * @test return "A".lowerCase().equals("a");
 *
 * @p Note that the tested class is automatically imported by Doctest. Other
 * classes must be declared in the test script sources using the methods
 * <code>importPackage</code> or <code>importClass</code>.
 *  
 * 
 * @h3 Synopsis
 *  
 * @p Here's the ANT job that generated this documentation and test suite: 
 *  
 * @pre <javadoc 
 *    docletpath="./less4j.jar;./lib/xp.jar;"
 *    doclet="org.less4j.Doctest"
 *    packagenames="org.less4j" 
 *    source="1.4" 
 *    sourcepath="src" 
 *    classpath="./less4j.jar;lib/servlet-api.jar;lib/smalljs.jar;lib/xp.jar;/jdk1.5.0_11/lib/tools.jar" 
 *    access="public" 
 *    >
 *    
 * @p To run the tests again and log their outputs: 
 *  
 * @pre java \
 *   -cp libs/less4j.jar;lib/servlet-api.jar;lib/smalljs.jar;lib/xp.jar \
 *   org.less4j.protocols.Doctest ./tests 1> tests.out 2> tests.err
 * 
 */
public class Doctest {
    
    protected RootDoc root;
    protected JSON.Object options;
    protected String base;
    protected JSON.Object index = new JSON.Object();
    protected JSON.Object packages = new JSON.Object();
    protected JSON.Object classes = new JSON.Object();
    protected String packageName = null;
    protected String className = null;
    
    protected static final String DOCTYPE_HTML_4_01_EN_STRICT = (
        "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\"\r\n"
        + "\"http://www.w3.org/TR/html4/strict.dtd\">\r\n"
        );
    
    protected static XML.Element doXML (String tag, String text) {
        try {
            XML.Document doc = new XML.Document(); 
            doc.parse("<" + tag +">" + text + "</" + tag + ">");
            return doc.root;
        } catch (Throwable e) {
            XML.Element error = new XML.Element(
                "div", new String[]{"class", "error"}, e.getMessage(), "\r\n"
                );
            error.addChild("pre", text);
            return error;
        }
    }

    protected static final String test_prologue = (
        "var doctest = function () {\r\n"
        );
    
    protected static final String test_epilogue = (
        "\r\n};\r\n"
        + "var run_test = function () {\r\n"
        + "    return (doctest()===true);\r\n"
        + "};"
        );
    
    protected XML.Element doTest(String source, String localName) 
    throws Exception {
        String fqn = packageName + "." + localName;
        SHA1 sha1 = new SHA1();
        sha1.update(Simple.encode(source, "UTF-8"));
        String hash = sha1.hexdigest();
        String prologue = (
            "importPackage("
            + (packageName.startsWith("java.") ? "": "Packages.")
            + packageName 
            + ");\r\n" 
            + "var prefix = '" + hash + " " + fqn + " ';\r\n"
            );
        String test_sources = (
            prologue + test_prologue + source + test_epilogue
            );
        (new FileOutputStream(new File(
            base + "/tests/" + hash + ".js"
            ))).write(Simple.encode(test_sources, "UTF-8"));
        XML.Element doctest = new XML.Element("div", new String[]{
            "class", "test",
            }, null, null);
        doctest.addChild("pre", source);
        ScriptableObject scope;
        Context cx = Context.enter();
        try {
            try {
                scope = new ImporterTopLevel(cx, true);
                cx.evaluateString(scope, test_sources, hash, 1, null);
                org.mozilla.javascript.Function fun = (
                    org.mozilla.javascript.Function
                    ) scope.get("run_test", scope);
                if (!((Boolean) Context.jsToJava(
                    fun.call(cx, scope, scope, new Object[]{}), Boolean.class
                    )).booleanValue())
                doctest.addChild("div", new String[]{
                    "class", "error"
                    }, "failed", null);
            } catch (RhinoException e) {
                doctest.addChild("div", new String[]{
                    "class", "error"
                    }, e.getMessage(), null);
            }
        } finally {
            Context.exit();
        }
        return doctest;
    }

    protected static String simpleName(String name) {
        return name.substring(name.lastIndexOf('.')+1);
    }
    
    protected static final HashSet NAMESPACE_HTML_4_01 = Simple.set(
            new Object[]{"@div", "@h3", "@h4", "@p"}
            );
        
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
            } else if (name.equals("@pre")) {
                parent.addChild("pre", text);
            } else if (NAMESPACE_HTML_4_01.contains(name)) {
                parent.addChild(doXML(name.substring(1), text));
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
    
    private static final char u_rarr = 8594;

    protected void doField(FieldDoc field) throws Exception {
        String name = field.name();
        String fqn = packageName + "." + className + "." + name;
        doIndex(name, fqn);
        XML.Document html = new XML.Document();
        String css = "javaField " + field.modifiers();
        html.root = new XML.Element("div");
        html.root.addChild("h2", className); 
        XML.Element title = html.root.addChild("h3");
        title.addChild(
            "span", new String[]{"class", css}, name, 
            " " + u_rarr + " " + field.type().typeName()  
            );
        html.root.addChild(doXML("p", field.commentText()));
        doTags(field.tags(), html.root, fqn);
        html.write(new File(base + "/fragments/" + fqn), "");
    }

    protected static String doParameter (
        ParamTag tag, Parameter parameter, XML.Element table
    ) {
        XML.Element div = table.addChild(
            "div", new String[]{"class", "parameter"}
            );
        div.addChild("code", parameter.typeName()).follow = " - ";
        div.addChild("span", tag.parameterName()).follow = " ";
        div.addChild(doXML("span", tag.parameterComment()));
        return tag.parameterName();
    }
    
    protected void doMethod(ExecutableMemberDoc method, XML.Element root) 
    throws Exception {
        String name = method.name();
        String signature = name + method.flatSignature();
        String localName = className + "." + signature;
        String css = ((method.isConstructor()) ?
            "javaConstructor ": "javaMethod "
            ) + method.modifiers();
        XML.Element javaMethod = root.addChild("div"); 
        XML.Element title = javaMethod.addChild("h3");
        javaMethod.addChild(doXML("p", method.commentText()));
        title.addChild(
            "span", new String[]{"class", css}, method.name(), null
            );
        XML.Element dl = javaMethod.addChild("dl");
        // parameters
        XML.Element di = dl.addChild("di");
        Parameter[] parameters = method.parameters();
        String arguments;
        if (parameters.length > 0) {
            di.addChild("dt", "Parameters");
            XML.Element dd = di.addChild("dd");
            ParamTag[] paramTags = method.paramTags();
            if (parameters.length == paramTags.length) {
                arguments = " (";
                arguments = arguments + doParameter(
                    paramTags[0], parameters[0], dd
                    );
                for (int i=1; i<parameters.length; i++) {
                    arguments = arguments + ", " + doParameter(
                        paramTags[i], parameters[i], dd
                        );
                }
                arguments = arguments + ")"; 
            } else { 
                arguments = " " + method.flatSignature();
            }
        } else {
            arguments = " ()";
        }
        // return
        if (method instanceof MethodDoc) {
            String rt = ((MethodDoc) method).returnType().typeName();
            if (rt == "void")
                title.getLastChild().follow = arguments;
            else {
                title.getLastChild().follow = (
                    arguments + " " + u_rarr + " " + rt
                    );
                if (method.tags("@return").length > 0) {
                    di = dl.addChild("di");
                    di.addChild("dt", "Return");
                    di.addChild(doXML("dd", method.tags("@return")[0].text()));
                }
            }
        } else
            title.getLastChild().follow = arguments;
        // throws
        if (method.tags("@throws").length > 0) {
            di = dl.addChild("di");
            di.addChild("dt", "Throws");
            di.addChild(doXML("dd", method.tags("@throws")[0].text()));
        }
        // other tags ...
        doTags(method.tags(), javaMethod, localName);
        //
        javaMethod.addChild("hr");
    }
        
    protected void doClass(ClassDoc Class) throws Exception {
        className = Class.name();
        String fqn = packageName + "." + Class.name();
        doIndex(className, fqn);
        JSON.Object properties = new JSON.Object();
        ClassDoc[] innerClasses = Class.innerClasses();
        if (innerClasses.length > 0) {
            JSON.Array innerTypes = new JSON.Array();
            properties.put("contains", innerTypes);
            for (int i=0; i<innerClasses.length; i++)
                innerTypes.add(innerClasses[i].qualifiedName());
        }
        if (Class.superclassType() != null)
            properties.put(
                "extends", Class.superclassType().qualifiedTypeName()
                );
        JSON.Array interfaces = new JSON.Array();
        Type[] _interfaces = Class.interfaceTypes();
        for (int i = 0; i < _interfaces.length; i++)
            interfaces.add(_interfaces[i].qualifiedTypeName());
        properties.put("implements", interfaces);
        JSON.Array members = new JSON.Array();
        properties.put("members", members);
        JSON.Array constants = new JSON.Array();
        properties.put("constants", constants);
        classes.put(fqn, properties);
        XML.Document html = new XML.Document();
        html.root = new XML.Element("div", new String[]{
            "class", (Class.isInterface()) ? "javaInterface" : "javaClass" 
            }, null, null);
        html.root.addChild("h2", className);
        // fields
        FieldDoc[] fieldsDoc = Class.fields();
        for (int i = 0; i < fieldsDoc.length; ++i) 
            if (fieldsDoc[i].isPublic()) {
                doField(fieldsDoc[i]);
                if (fieldsDoc[i].isStatic())
                    constants.add(fieldsDoc[i].name());
                else
                    members.add(fieldsDoc[i].name());
            }
        // methods
        MethodDoc[] methodsDoc = Class.methods();
        JSON.Array methods = new JSON.Array();
        properties.put("methods", methods);
        JSON.Array functions = new JSON.Array();
        properties.put("functions", functions);
        JSON.Object pages = new JSON.Object();
        String methodName;
        for (int i = 0; i < methodsDoc.length; ++i) {
            methodName = methodsDoc[i].name();
            if (
                methodsDoc[i].isPublic() && 
                !pages.containsKey(methodName)
                ) {
                XML.Document method = new XML.Document();
                method.root = new XML.Element(
                    "div", new String[]{"class", "javaMethods"}, null, null
                    );
                method.root.addChild("h2", className); 
                pages.put(methodName, method);
                if (methodsDoc[i].isStatic())
                    functions.add(methodsDoc[i].name());
                else
                    methods.add(methodsDoc[i].name());
            }
        }
        XML.Document methodPage;
        for (int i = 0; i < methodsDoc.length; ++i)
            if (methodsDoc[i].isPublic()) {
                methodPage = (XML.Document) pages.get(methodsDoc[i].name());
                doMethod(methodsDoc[i], methodPage.root);
            }
        // comments
        html.root.addChild(doXML("p", Class.commentText()));
        // constructors
        ExecutableMemberDoc[] constructors = Class.constructors();
        for (int i=0; i < constructors.length; i++) { 
            if (!constructors[i].commentText().equals("")) {
                doMethod(constructors[i], html.root);
            }
        } 
        // tags
        doTags(Class.tags(), html.root, className);
        // index and write method fragments
        html.write(new File(base + "/fragments/" + fqn), "");
        Iterator names = pages.keySet().iterator();
        while (names.hasNext()) {
            methodName = (String) names.next();
            doIndex(
                methodName, packageName + "." + className + "." + methodName
                );
            ((XML.Document) pages.get(methodName)).write(new File (
                base + "/fragments/" + fqn + "." + methodName
                ), "");
        }
    }
    
    protected void doPackage(PackageDoc packageDoc) throws Exception {
        packageName = packageDoc.name();
        JSON.Array types = new JSON.Array();
        packages.put(packageName, types);
        XML.Document html = new XML.Document();
        html.root = new XML.Element("div", new String[]{
            "class", "javaPackage" 
            }, null, null);
        html.root.addChild("h2", "Package Overview");
        XML.Element interfaces = new XML.Element("dl"); 
        XML.Element implementations = new XML.Element("dl");
        XML.Element di;
        ClassDoc[] classes = packageDoc.allClasses();
        for (int i = 0; i < classes.length; ++i) 
            if (classes[i].isPublic()) {
                className = classes[i].name();
                types.add(className);
                if (classes[i].containingClass() == null) {
                    if (classes[i].isInterface())
                        di = interfaces.addChild("di");
                    else
                        di = implementations.addChild("di");
                    di.addChild("dt", classes[i].name());
                    di.addChild(doXML("dd", classes[i].commentText()));
                }
                doClass(classes[i]);
            }
        if (interfaces.children != null) {
            html.root.addChild("h3", "Interfaces");
            html.root.addChild(interfaces);
        }
        if (implementations.children != null) {
            html.root.addChild("h3", "Implementations");
            html.root.addChild(implementations);
        }
        html.write(new File(base + "/fragments/" + packageName), "");
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
        options = JSON.options(root.options());
        base = options.S("-destdir", "doc");
        doDirectory(base);
        doDirectory(base + "/fragments");
        doDirectory(base + "/tests");
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
     * @h3 HTML fragments
     * 
     * @pre ...
     * 
     * @h3 JSON indexes
     * 
     * @pre {
     *    "index": {"identifier": ["fqn"]},
     *    "types": {"type": {
     *        "extends": "type",
     *        "implements": ["type"],
     *        "contains": ["type"],
     *        "fields": {"identifier": "modifiers"},
     *        "methods": {"identifier": "modifiers"}
     *        }},
     *    "packages": {"package": {
     *        "interfaces": ["type"],
     *        "implementations": ["type"]
     *    }
     *}
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
     * @pre java Doctest org.less4j \
     *  -classpath ./less4j.jar;./lib/smalljs.jar;./lib/xp.jar \
     *  1> doctests.out 2> doctests.err 
     * 
     * @p Test results are simple text messages prefixed by a hash of the
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
