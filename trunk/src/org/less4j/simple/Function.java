package org.less4j.simple;

/**
 * A simple function interface definition, usefull whenever your
 * application requires first-class functions that take a single
 * <code>Object</code> as input state and return <code>null</code> or
 * a single <code>Object</code> result.
 */
public interface Function {
    /**
     * A method that implements the function's call, taking any 
     * <code>Object</code> type as input and returning as result
     * an instance of any type or <code>null</code>.
     * 
     * @param input of any <code>Object</code> type
     * @return an instance of any type or <code>null</code>.
     * 
     * @pre class Print implements Simple.Function {
     *    public Object apply (Object input) {
     *        try {
     *            System.out.println(input);
     *        } catch (Throwable e) {
     *            return e;
     *        }
     *        return null;
     *    }
     *}
     */
    public Object apply (Object input);
}