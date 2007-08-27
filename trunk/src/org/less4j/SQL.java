/* Copyright (C) 2007 Laurent A.V. Szyster

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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;

/**
 * Conveniences to query and update an SQL database with a simple object 
 * relational interface to map between JDBC result sets and seven JSON 
 * patterns: table, relations, collection, index, dictionary, one or many 
 * objects.
 * 
 * <h3>Synopsis</h3>
 * 
 * <p>This class is a library of static functions applied by less4j's 
 * <code>Actor</code> methods <code>sql*</code>, so unless you want to
 * apply it somewhere else, you should use those higher level methods
 * instead.</p>
 * 
 * <p>Also, note that this implementation depends on less4j's 
 * <code>JSON</code> and makes little sense out of a web controller.</p>
 * 
 * <p><b>Copyright</b> &copy; 2006-2007 Laurent A.V. Szyster</p>
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
     * <h3>Applications</h3>
     * 
     * <p>The benefit of an ORM in a J2EE controller is mainly to paliate the
     * complications of JDBC and a brain-dead synchronous share-everything 
     * design. The good ones provide developers a convenient API without leaks 
     * that allows controllers to release SQL connections asap.</p>
     * 
     * <p>However, I doubt there is any benefit for J2EE applications in 
     * trying to map instances of arbitrary Java objects in an SQL database. 
     * First because in most case, data is allready available in a schema that
     * just does not map to the application's object model. Second because 
     * when data is not an SQL legacy, there is an opportunity <em>not</em> 
     * to use a relations as object properties.</p>
     * 
     * <p>So, you won't find anything like Hibernate here. I expect less4j's
     * applications to store JSON objects as they are first, then latter
     * update one or more database with indexes and statistics about those
     * objects.</p>
     * 
     * <p><b>Copyright</b> &copy; 2006-2007 Laurent A.V. Szyster</p>
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
    
    private static final String _rows = "rows";
    private static final String _columns = "columns";
    
    protected static class _Table implements ORM {
        public Object jdbc2 (ResultSet rs) throws SQLException {
            JSON.Object object = null;
            JSON.Array rows = null, row;
            if (rs.next()) {
                int i;
                object = new JSON.Object();
                rows = new JSON.Array();
                object.put(_rows, rows);
                ResultSetMetaData mt = rs.getMetaData();
                int L = mt.getColumnCount()+1;
                JSON.Array columns = new JSON.Array();
                object.put(_columns, columns);
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

    protected static class _Index implements ORM {
        public Object jdbc2 (ResultSet rs) throws SQLException {
            JSON.Object index = null;
            if (rs.next()) {
                index = new JSON.Object();
                ResultSetMetaData mt = rs.getMetaData();
                int columns = mt.getColumnCount();
                if (columns > 2) do {
                    JSON.Object object = new JSON.Object();
                    for (int i=2, L=columns+1; i<L; i++)
                        object.put(mt.getColumnName(i), rs.getObject(i));
                    String key = rs.getObject(1).toString();
                    if (index.containsKey(key))
                        ((JSON.Array) index.get(key)).add(object);
                    else {
                        JSON.Array list = new JSON.Array();
                        list.add(object);
                        index.put(key, list);
                    }
                } while (rs.next());
                else if (columns > 1) do {
                    String key = rs.getObject(1).toString();
                    if (index.containsKey(key))
                        ((JSON.Array) index.get(key)).add(rs.getObject(2));
                    else {
                        JSON.Array list = new JSON.Array();
                        list.add(rs.getObject(2));
                        index.put(key, list);
                    }
                } while (rs.next());
                else do {
                    index.put(rs.getObject(1), new JSON.Array());
                } while (rs.next());
            }
            rs.close();
            return index;
        }
    }

    /**
     * An ORM singleton to map a JDBC ResultSet into an JSON.Object
     * index of JSON.Array collections, using the first column as key 
     * and the following(s) as value(s).
     */
    public static ORM index = new _Index ();
    
    protected static class _Dictionary implements ORM {
        public Object jdbc2 (ResultSet rs) throws SQLException {
            JSON.Object dictionary = null;
            if (rs.next()) {
                dictionary = new JSON.Object();
                int columns = rs.getMetaData().getColumnCount();
                if (columns > 2) do {
                    JSON.Array list = new JSON.Array(); 
                    for (int i=1; i<columns; i++)
                        list.add(rs.getObject(i+1));
                    dictionary.put(rs.getObject(1), list);
                } while (rs.next());
                else if (columns > 1) do {
                    dictionary.put(rs.getObject(1), rs.getObject(2));
                } while (rs.next());
                else do {
                    dictionary.put(rs.getObject(1), null);
                } while (rs.next());
            }
            rs.close();
            return dictionary;
        }
    }

    /**
     * An ORM singleton to map a JDBC ResultSet into an JSON.Object,
     * using the first column as key and the following(s) as value(s).
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
                int i, L = mt.getColumnCount() + 1;
                for (i = 1; i < L; i++) 
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

    /**
     * Try to query the <code>sql</code> JDBC connection with an SQL
     * statement and an argument iterator, use an <code>ORM</code> to return 
     * a <code>JSON.Array</code>, a <code>JSON.Object</code> or 
     * <code>null</code> if the result set was empty.
     * 
     * <h4>Synopsis</h4>
     * 
     * <pre>try {
     *    JSON.Array relations = (JSON.Array) <strong>SQL.query(</strong>
     *        "select * from TABLE wher COLUMN=?", 
     *        Simple.iterator (new String[]{"criteria"}),
     *        100, SQL.relations
     *        <strong>)</strong>;
     *} catch (SQLException e) {
     *    e.println(System.err);
     *}</pre>
     * 
     * @param sql the <code>Connection</code> to query
     * @param statement to prepare and execute as a query
     * @param args an iterator through arguments
     * @param fetch the number of rows to fetch
     * @param collector the <code>ORM</code> used to map the result set
     * @return a <code>JSON.Array</code>, a <code>JSON.Object</code> or 
     *         <code>null</code>
     * @throws SQLException
     */
    public static Object query (
        Connection sql, String statement, Iterator args, 
        int fetch, SQL.ORM collector
        ) 
    throws SQLException {
        Object result = null;
        PreparedStatement st = null;
        try {
            st = sql.prepareStatement(statement);
            st.setFetchSize(fetch);
            int i = 1; 
            while (args.hasNext()) {st.setObject(i, args.next()); i++;}
            result = collector.jdbc2(st.executeQuery());
            st.close();
            st = null;
        } finally {
            if (st != null) {
                try {st.close();} catch (SQLException e) {;}
                st = null;
            }
        }
        return result;
    }
    
    /**
     * Try to execute and UPDATE, INSERT, DELETE or DDL statement, close
     * the JDBC/DataSource statement, return the number of rows updated.
     * 
     * @param sql the <code>Connection</code> to update
     * @param statement the SQL statement to execute
     * @return -1 if the statement failed, 0 if no row update took place, 
     *         or the numbers of rows updated, deleted or inserted.
     * @throws SQLException
     */
    public static Integer update (Connection sql, String statement) 
    throws SQLException {
        int result = -1;
        Statement st = null;
        try {
            st = sql.createStatement(); 
            result = st.executeUpdate(statement);
            st.close();
            st = null;
        } finally {
            if (st != null) {
                try {st.close();} catch (SQLException e) {;}
                st = null;
            }
        }
        return new Integer(result);
    }
    
    /**
     * Try to execute a prepared UPDATE, INSERT, DELETE or DDL statement 
     * with an arguments iterator, close the JDBC/DataSource statement and 
     * returns the number of rows updated.
     * 
     * @param sql the <code>Connection</code> to update
     * @param statement the SQL statement to execute
     * @param args an <code>Iterator</code> of <code>JSON.Array</code>s
     * @return an <code>Integer</code>
     * @throws <code>SQLException</code>
     */
    public static Integer update (
        Connection sql, String statement, Iterator args
        ) 
    throws SQLException {
        int result = -1;
        PreparedStatement st = null;
        try {
            st = sql.prepareStatement(statement);
            int i=0; while (args.hasNext()) st.setObject(i++, args.next());
            result = st.executeUpdate();
            st.close();
            st = null;
        } finally {
            if (st != null)  {
                try {st.close();} catch (SQLException e) {;}
                st = null;
            }
        }
        return new Integer(result);
    }
    
    /**
     * Try to execute a prepared database update statement with more than one
     * set of arguments in one batch, close the JDBC/DataSource statement and 
     * returns the number of rows updated.
     * 
     * @param sql
     * @param statement
     * @param params
     * @return an <code>Integer</code>
     * @throws SQLException
     */
    public static Integer batch (
        Connection sql, String statement, Iterator params
        ) 
    throws SQLException {
        int i, L, result = -1;
        JSON.Array args;
        PreparedStatement st = null;
        try {
            st = sql.prepareStatement(statement);
            while (params.hasNext()) {
                args = (JSON.Array) params.next();
                for (i=0, L=args.size(); i < L; i++)
                    st.setObject(i, args.get(i));
                st.addBatch();
            }
            st.execute();
            result = st.getUpdateCount();
            st.close();
            st = null;
        } finally {
            if (st != null) {
                try {st.close();} catch (SQLException e) {;}
                st = null;
            }
        }
        return new Integer(result);
    }
    
}
