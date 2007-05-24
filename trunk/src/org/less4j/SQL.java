/* Copyright (C) 2007 Laurent A.V. Szyster

This library is free software; you can redistribute it and/or modify
it under the terms of version 2 of the GNU General Public License as
published by the Free Software Foundation.

   http://www.gnu.org/copyleft/gpl.html

This library is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

You should have received a copy of the GNU General Public License
along with this library; if not, write to the Free Software Foundation, 
Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA */

package org.less4j;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * Simple Object Relational Mapping between JDBC result sets and six JSON 
 * patterns: table, relations, collection, dictionary, one or many objects.
 * 
 * <p>This class bundles a simple <code>ORM</code> interface and singletons 
 * for each of its six protected implementations, one per patterns supported.
 * Most applications of less4j don't need more and you may as well use the
 * appropriate methods of <code>Actor</code> or <code>Controller</code>.</p>
 * 
 * <h3>About ORMs</h3>
 * 
 * <p>The benefit of an ORM in a J2EE controller is mainly to paliate the
 * complications of JDBC and a brain-dead synchronous share-everything design.
 * Good ORMs can provide developers a convenient API without leaks that allows 
 * controllers to release SQL connections asap.</p>
 * 
 * <p>I doubt there is any benefit for J2EE applications in trying to map
 * instances of arbitrary Java objects in an SQL database. First because
 * in most case, data is allready available in a schema that just does not
 * map to the application's object model. Second because when data is
 * not an SQL legacy, there is an opportunity <em>not</em> to use a 
 * relations as object properties.</p>
 * 
 * <p>So, you won't find anything like Hibernate here. I expect less4j's
 * applications to store JSON objects as they are first, then latter
 * update one or more database with indexes and statistics about those
 * objects.</p>
 * 
 * @author Laurent Szyster
 * @version 0.30
 */
public class SQL {
    
    /**
     * The simplest ORM interface possible for JDBC <code>ResultSet</code>.
     * 
     * <h3>Synopsis</h3>
     * 
     *<pre>protected static class _Collection implements ORM {
     *    public Object jdbc2 (ResultSet rs) throws SQLException {
     *        JSON.Array collection = null;
     *        if (rs.next()) {
     *            collection = new JSON.Array();
     *            do {collection.add(rs.getObject(1));} while (rs.next());
     *        }
     *        rs.close();
     *        return collection;
     *    }
     *}
     *
     *public static ORM collection = new _Collection ();</pre>
     *
     * @author Laurent Szyster
     * @version 0.30
     */
    public static interface ORM {
        /**
         * Try to map a <code>ResultSet</code> into a Java <code>Object</code>.
         *  
         * Note that the six implementations provided return either a 
         * <code>JSON.Array</code> or a <code>JSON.Object</code>.
         * 
         * @param rs a JDBC <code>ResultSet</code>
         * @return an <code>Object</code>
         * @throws SQLException
         */
        public Object jdbc2 (ResultSet rs) throws SQLException;
    }
    
    protected static class _Table implements ORM {
        public Object jdbc2 (ResultSet rs) throws SQLException {
            JSON.Object object = null;
            JSON.Array rows = null, row;
            if (rs.next()) {
                int i;
                object = new JSON.Object();
                rows = new JSON.Array();
                object.put("rows", rows);
                ResultSetMetaData mt = rs.getMetaData();
                int L = mt.getColumnCount()+1;
                JSON.Array columns = new JSON.Array();
                object.put("columns", columns);
                for (i=1; i<L; i++) columns.add(mt.getColumnName(i));
                do {
                    row = new JSON.Array ();
                    for (i=1; i<L; i++) row.add(rs.getObject(i));
                    rows.add(row);
                } while (rs.next());
            }
            rs.close();
            return object;
        }
    }

    /**
     * An ORM singleton to map a JDBC ResultSet with metadata into a
     * single JSON.Object with the obvious "columns" and "rows" members.
     */
    public static ORM table = new _Table ();

    protected static class _Relations implements ORM {
        public Object jdbc2 (ResultSet rs) throws SQLException {
            JSON.Array rows = null, row;
            if (rs.next()) {
                int i;
                rows = new JSON.Array ();
                ResultSetMetaData mt = rs.getMetaData();
                int L = mt.getColumnCount()+1;
                do {
                    row = new JSON.Array ();
                    for (i = 1; i < L; i++) row.add(rs.getObject(i));
                    rows.add(row);
                } while (rs.next());
            }
            rs.close();
            return rows;
        }
    }

    /**
     * An ORM singleton to map a JDBC ResultSet into a JSON.Array
     * of JSON.Array, without metadata.
     */
    public static ORM relations = new _Relations ();

    protected static class _Collection implements ORM {
        public Object jdbc2 (ResultSet rs) throws SQLException {
            JSON.Array collection = null;
            if (rs.next()) {
                collection = new JSON.Array();
                do {collection.add(rs.getObject(1));} while (rs.next());
            }
            rs.close();
            return collection;
        }
    }

    /**
     * An ORM singleton to map the first column of a JDBC ResultSet 
     * into an JSON.Array.
     */
    public static ORM collection = new _Collection ();

    protected static class _Dictionary implements ORM {
        public Object jdbc2 (ResultSet rs) throws SQLException {
            JSON.Object dictionary = null;
            if (rs.next()) {
                dictionary = new JSON.Object();
                do {
                    dictionary.put(rs.getObject(1), rs.getObject(2));
                    } 
                while (rs.next());
            }
            rs.close();
            return dictionary;
        }
    }

    /**
     * An ORM singleton to map a JDBC ResultSet into an JSON.Object,
     * using the first column as key and the following(s) as value.
     */
    public static ORM dictionary = new _Dictionary ();

    protected static class _Objects implements ORM {
        public Object jdbc2 (ResultSet rs) throws SQLException {
            JSON.Array relations = new JSON.Array();
            if (rs.next()) {
                JSON.Object object = new JSON.Object();
                ResultSetMetaData mt = rs.getMetaData();
                int i, L = mt.getColumnCount() + 1;
                do {
                    object = new JSON.Object();
                    for (i = 1; i < L; i++) 
                        object.put(mt.getColumnName(i), rs.getObject(i));
                    relations.add(object);
                } while (rs.next());
            }
            rs.close();
            return relations;
        }
    }

    /**
     * An ORM singleton to map a JDBC ResultSet into a JSON.Array of
     * JSON.Object, using column names as keys.
     */
    public static ORM objects = new _Objects ();

    protected static class _Object implements ORM {
        public Object jdbc2 (ResultSet rs) throws SQLException {
            JSON.Object object = null;
            if (rs.next()) {
                object = new JSON.Object();
                ResultSetMetaData mt = rs.getMetaData();
                int L = mt.getColumnCount();
                for (int i = 0; i < L; i++) 
                    object.put(mt.getColumnName(i), rs.getObject(i));
            }
            rs.close();
            return object;
        }
    }

    /**
     * An ORM singleton to map the first row of a JDBC ResultSet into a
     * single JSON.Object, using column names as keys.
     */
    public static ORM object = new _Object ();

}
