/*
 * <copyright>
 *  Copyright 1999-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.util;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * Factory which can <tt>create</tt> <code>Operator</code>s.
 * <p>
 * See <tt>main</tt> for sample usage.
 * <p>
 * <b>Note:</b> The default implementation of <code>Operator</code> 
 * is currently kept in the utility module as "org.cougaar.lib.contract.lang" -- 
 * see the "index.html" file there for language details.
 * <p>
 * @see Operator
 */
public abstract class OperatorFactory {

  /**
   * System property tags for setting the <tt>default*</tt> values.
   * <p>
   * Java usage is "-Dtagname=value"
   */
  public static final String PROPERTY_CLASSNAME = "org.cougaar.util.op.name";
  public static final String PROPERTY_STYLE = "org.cougaar.util.op.style";

  /**
   * Default style for <tt>create(Object)</tt>.
   */
  private static int defaultStyle;

  /**
   * Default classname for <tt>getInstance()</tt>.
   */
  private static String defaultClassname;

  /**
   * Default instance of <code>OperatorFactory</code>.
   */
  private static OperatorFactory defaultOperatorFactory;

  /**
   * Set the <tt>default*</tt>s.
   */
  static {
    // overwrite from optional system properties
    String s;
    if ((s = System.getProperty(PROPERTY_STYLE)) != null) {
      if (s.regionMatches(true, 0, "xml", 0, 3)) {
        defaultStyle = Operator.XML_FLAG;
      } else if (s.regionMatches(true, 0, "paren", 0, 5)) {
        defaultStyle = Operator.PAREN_FLAG;
      } else {
        System.err.println("Invalid property -D"+PROPERTY_STYLE+"="+s);
      }
    } else {
      defaultStyle = Operator.DEFAULT_STYLE;
    }
    if ((s = System.getProperty(PROPERTY_CLASSNAME)) != null) {
      defaultClassname = s;
    } else {
      defaultClassname = "org.cougaar.lib.contract.lang.OperatorFactoryImpl";
    }
    // load the factory
    defaultOperatorFactory = getInstance(defaultClassname);
  }

  /**
   * Get the default <code>OperatorFactory</code> instance.
   */
  public static OperatorFactory getInstance() {
    return defaultOperatorFactory;
  }

  /**
   * Get the default style for <tt>create(Object)</tt>
   */
  public static int getDefaultStyle() {
    return defaultStyle;
  }

  /**
   * Get the default classname for <tt>getInstance(String)</tt>
   */
  public static String getDefaultClassname() {
    return defaultClassname;
  }

  /**
   * <tt>create</tt> using the <tt>getDefaultStyle()</tt>.
   * <p>
   * @see #create
   */
  public Operator create(Object inObj) throws Exception {
    return create(defaultStyle, inObj);
  }

  /**
   * Create an <code>Operator</code>.
   * <p>
   * Implementations of the contract language parser must implement
   * this method.
   * <p>
   * @param style either (Operator.XML_FLAG) or (Operator.PAREN_FLAG)
   * @param inObj an instance of 
   *   <code>java.lang.String</code>, 
   *   <code>java.io.InputStream</code>, 
   *   <code>java.io.Reader</code>, or
   *   <code>org.w3c.dom.Element</code>
   * @throws Exception if parse failed -- toString will display
   *    useful stack trace
   */
  public abstract Operator create(
      int style, Object inObj) throws Exception;
  
  /**
   * @deprecated See create(int, Object) method to use new symbolic int 
   *   flags, e.g. Operator.XML_FLAG or Operator.PAREN_FLAG
   */
  public Operator create(
      boolean isParenStyle, Object inObj) throws Exception {
    return create(
      (isParenStyle ? Operator.PAREN_FLAG : Operator.XML_FLAG),
      inObj);
  }

  /**
   * Get an instance of a specific <code>OperatorFactory</code>.
   */
  public static OperatorFactory getInstance(final String classname) {
    try {
      // load class
      Class c = Class.forName(classname);
      return (OperatorFactory)c.newInstance(); 
    } catch (Exception e) {
      // unable to load class!
      final String errorMsg = e.toString();
      // create dummy factory that throws an Exception!
      return new OperatorFactory() {
        public Operator create(int style, Object inObj) 
            throws Exception {
          throw new UnsupportedOperationException(
            "OperatorFactory \""+classname+"\" not available!\n"+
            errorMsg);
        }
      };
    }
  }

  /**
   * A testing utility!.
   * <p>
   * Try with a ".txt" file, for example:<pre><code>
   *   (and (isString) (equals "foo"))
   * </pre></code>
   */
  public static void main(String[] args) {
    System.out.println("Test the operator factory");

    // get the arguments
    Object objIn;
    int style = 
      (Operator.PAREN_FLAG | 
       Operator.PRETTY_FLAG |
       Operator.VERBOSE_FLAG);
    if (args.length != 1) {
      // simple test
      objIn = "(and (isString) (equals \"TEST\"))";
    } else {
      // read from the input file
      String filename = args[0];
      if (filename.regionMatches(true, filename.length()-4, ".xml", 0, 4)) {
        // filename ends in ".xml";
        style = Operator.XML_FLAG;
      }
      try {
        objIn = new FileReader(filename);
      } catch (Exception e) {
        System.err.println("Unable to read \""+filename+"\":");
        e.printStackTrace();
        return;
      }
    }

    // get an instance of the default operator factory
    OperatorFactory operFactory = OperatorFactory.getInstance();

    // parse the input
    Operator oper;
    try {
      oper = operFactory.create(style, objIn);
    } catch (Exception e) {
      System.err.println("Unable to parse: ");
      e.printStackTrace();
      return;
    }

    // test for null operator
    if (oper == null) {
      System.out.println("EMPTY?");
      return;
    }

    // display the parsed operator
    System.out.println("parsed: \n"+oper);

    // set optional testing template
    /************************************************/
    oper.setConst("foo", "bar");
    /************************************************/

    // create a testing object
    Object testObject;
    /************************************************/
    testObject = "TEST";
    /************************************************/
    System.out.println("test with: \n  "+testObject);

    // execute the predicate on the test object
    System.out.println("execute(): ");
    try {
      boolean b = oper.execute(testObject);
      System.out.println("  return value: "+b);
    } catch (Exception e) {
      System.err.println("  execute failed: "+e);
      e.printStackTrace();
    }

    // finished the test
    System.out.println("done.");
  }

}
