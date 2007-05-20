package org.less4j;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

/**
 * The simplest implementation of an Object Relational Mapping between JDBC
 * and JSON, with support for the six patterns of object instanciation only: 
 * table, relations, collection, dictionary, many objects or a single object.
 * 
 * <h3>Synopsis</h3>
 * 
 * @author Laurent Szyster
 * @version 0.30
 */
public class SQL {
    
    /**
     * 
     * @author Laurent Szyster
     * @version 0.30
     */
    protected static interface ORM {
        /**
         * 
         * @param rs
         * @return
         * @throws SQLException
         */
        public Object jdbc2json (ResultSet rs) throws SQLException;
    }
    
    protected static class _Table implements ORM {
        public Object jdbc2json (ResultSet rs) throws SQLException {
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
        public Object jdbc2json (ResultSet rs) throws SQLException {
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
        public Object jdbc2json (ResultSet rs) throws SQLException {
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
        public Object jdbc2json (ResultSet rs) throws SQLException {
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
        public Object jdbc2json (ResultSet rs) throws SQLException {
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
        public Object jdbc2json (ResultSet rs) throws SQLException {
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
