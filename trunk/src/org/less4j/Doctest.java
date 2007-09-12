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

import java.io.File;
import java.io.FileOutputStream;

import com.sun.javadoc.RootDoc;
import com.sun.javadoc.PackageDoc;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.FieldDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.Tag;


/**
 * Produce a practical web of XHTML and JSON resources for all public 
 * packages, classes, fields and methods. The result is considerably simpler, 
 * smaller and faster to load in a modern browser. Adding styles and 
 * interactivity to the javadoc produced is also far more convenient to
 * do in JavaScript.  
 * 
 * @copyright 2006-2007 Laurent A.V. Szyster
 *
 */
public class Doctest {

    public static XML.Element doComment (String text) {
        try {
            XML.Document doc = new XML.Document(); 
            doc.parse("<div>" + text + "</div>");
            return doc.root;
        } catch (Throwable e) {
            return new XML.Element("div", new String[]{
                "class", "xmlError"
                }, e.getMessage(), null);
        }
    }
    
    public static void doTags(Tag[] tags, XML.Element parent) {
        String name, text;
        for (int i=0; i<tags.length; i++) {
            name = tags[i].name();
            text = tags[i].text();
            if (name.equals("@div"))
                parent.addChild(doComment(text));
            else if (name.equals("@param")) {
                XML.Element parameter = parent.addChild(
                    "div", new String[]{"class", "param"}
                    );
                int sep = text.indexOf(' ');
                if (sep > -1) {
                    parameter.addChild("span", text.substring(0, sep));
                    parameter.addChild("span", text.substring(sep+1));
                } else {
                    parameter.first = text;
                }
            } else
                parent.addChild("div", new String[]{
                    "class", name.substring(1)
                    }, tags[i].text(), null);
        }
    }
    
    public static XML.Element doMethod(
        MethodDoc method, String path, JSON.Object index
        ) throws Exception {
        String name = method.name();
        String signature = name + method.flatSignature();
        if (!index.containsKey(name))
            index.put(name, new JSON.Array());
        index.A(name).add(path + "." + signature);
        XML.Element javaMethod = new XML.Element(
            "div", new String[]{"class", "javaMethod"}, null, null
            ); 
        javaMethod.addChild("a", new String[]{
            "name", signature
            });
        XML.Element definition = javaMethod.addChild("h3", new String[]{
            "class", "definition"
            });
        definition.addChild("span", method.name());
        definition.addChild("span", method.flatSignature());
        javaMethod.addChild(doComment(method.commentText()));
        doTags(method.tags(), javaMethod);
        return javaMethod;
    }
    
    public static XML.Element doField(
        FieldDoc field, String path, JSON.Object index
        ) throws Exception {
        String name = field.name(); 
        if (!index.containsKey(name))
            index.put(name, new JSON.Array());
        index.A(name).add(path + "." + name);
        XML.Element javaField = new XML.Element(
            "div", new String[]{"class", "javaField"}, null, null
            ); 
        javaField.addChild("a", new String[]{"name", name});
        XML.Element definition = javaField.addChild("h3", new String[]{
            "class", "definition"
            });
        definition.addChild("span", field.name());
        javaField.addChild(doComment(field.commentText()));
        doTags(field.tags(), javaField);
        return javaField;
    }
    
    public static void doClass(
        ClassDoc Class, String base, String packageName, JSON.Object index
        ) throws Exception {
        String name = Class.name();
        if (!index.containsKey(name))
            index.put(name, new JSON.Array());
        index.A(name).add(packageName + "." + Class.name());
        XML.Document html = new XML.Document();
        html.root = new XML.Element(
            "div", new String[]{"id", "javaClass"}, null, null
            );
        html.root
            .addChild("h2", packageName + "." + Class.name());
        html.root
            .addChild(doComment(Class.commentText()));
        doTags(Class.tags(), html.root);
        XML.Element javaFields = html.root
            .addChild("div", new String[]{"id", "javaFields"}); 
        FieldDoc[] fields = Class.fields();
        for (int i = 0; i < fields.length; ++i) {
            javaFields.addChild(doField(fields[i], name, index));
        }
        XML.Element javaMethods = html.root
            .addChild("div", new String[]{"id", "javaMethods"}); 
        MethodDoc[] methods = Class.methods();
        for (int i = 0; i < methods.length; ++i) {
            javaMethods.addChild(doMethod(methods[i], name, index));
        }
        html.write(new File(base + "/" + packageName + "/" + name));
    }
    
    public static void doPackage(
        PackageDoc Package, String base
        ) throws Exception {
        String name = Package.name();
        File packageDir = new File(base + "/" + name);
        if (!packageDir.exists()) packageDir.mkdir();
        JSON.Object index = new JSON.Object();
        ClassDoc[] classes = Package.allClasses();
        for (int i = 0; i < classes.length; ++i) {
            doClass(classes[i], base, name, index);
        }
        String path = base + "/" + name;
        FileOutputStream os = new FileOutputStream(new File(path + ".js"));
        os.write(Simple.encode(
            "javaDoc.package['" + name + "'] = ", "UTF-8"
            ));
        JSON.pprint(index, os);
        os.flush();
        os.close();
    }
    
    public static boolean start(RootDoc root) {
        JSON.Object options = Simple.dict(root.options());
        String base = options.S("-destdir", "javadoc");
        File basedir = new File(base);
        if (!basedir.exists())
            basedir.mkdir();
        else if (!basedir.isDirectory()) {
            root.printError(base + "is not a directory");
            return false;
        }
        PackageDoc[] packages = root.specifiedPackages();
        try {
            for (int i = 0; i < packages.length; ++i) {
                doPackage(packages[i], base);
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return false;
        }
        return true;
    }
    
}
