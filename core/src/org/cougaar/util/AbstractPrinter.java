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

import java.io.OutputStream;
import java.io.PrintStream;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import org.cougaar.domain.planning.ldm.plan.Location;
import org.cougaar.domain.planning.ldm.plan.Verb;

/**
 * <code>Abstract Printer</code>.
 * <p>
 * <ul>
 * Current <code>AbstractPrinter</code> subclasses can be used to print 
 * Objects or primitives for ...
 * <li><tt>DataPrinter</tt>: Object Output serialization</li>
 * <li><tt>XMLPrinter</tt>: minimal XML for XMLObjectFactory</li>
 * <li><tt>HTMLPrinter</tt>: HTML view of an Object</li>
 * <li><tt>StringPrinter</tt>: String format without indents</li>
 * <li><tt>PrettyXMLPrinter</tt>: pretty XML, also for XMLObjectFactory</li>
 * <li><tt>PrettyStringPrinter</tt>: String format with indents</li>
 * <li><tt>DefaultPrinter</tt>: Java-style toString()</li>
 * </ul>
 * <p>
 * Somewhat awkward making this a subclass of PrintStream.  I don't
 * want callers using "print(val)" or "println(val)" methods!  Could make
 * this an interface but loose useful constructor methods and slow code
 * for no reason.  Also could move PrintStream into AsciiPrinter as a
 * field, but there would introduce useless "myPrintStream.print(val)" 
 * hand-off calls everywhere.  Almost wish Java had multiple inheritance...
 * <p>
 * @see AbstractPrinter#main() for sample usage
 */

public abstract class AbstractPrinter extends PrintStream {

  /** public static **/

  public static final boolean isValidFormat(String format) {
    return ("data".equalsIgnoreCase(format) ||
            "xml".equalsIgnoreCase(format) ||
            "html".equalsIgnoreCase(format) ||
            "string".equalsIgnoreCase(format) ||
            "prettyxml".equalsIgnoreCase(format) ||
            "prettystring".equalsIgnoreCase(format) ||
            "default".equalsIgnoreCase(format));
  }

  public static final AbstractPrinter createPrinter(
      String format, OutputStream out) {
    if ("data".equalsIgnoreCase(format))
      return ((out instanceof DataPrinter) ?
              (DataPrinter)out : 
              new DataPrinter(out));
    else if ("xml".equalsIgnoreCase(format))
      return ((out instanceof XMLPrinter) ?
              (XMLPrinter)out : 
              new XMLPrinter(out));
    else if ("html".equalsIgnoreCase(format))
      return ((out instanceof HTMLPrinter) ?
              (HTMLPrinter)out : 
              new HTMLPrinter(out));
    else if ("string".equalsIgnoreCase(format))
      return ((out instanceof StringPrinter) ?
              (StringPrinter)out : 
              new StringPrinter(out));
    else if ("prettyxml".equalsIgnoreCase(format))
      return ((out instanceof PrettyXMLPrinter) ?
              (PrettyXMLPrinter)out : 
              new PrettyXMLPrinter(out));
    else if ("prettystring".equalsIgnoreCase(format))
      return ((out instanceof PrettyStringPrinter) ?
              (PrettyStringPrinter)out : 
              new PrettyStringPrinter(out));
    else if ("default".equalsIgnoreCase(format))
      return ((out instanceof DefaultPrinter) ?
              (DefaultPrinter)out : 
              new DefaultPrinter(out));
    else
      return null;
  }

  /** public **/

  public AbstractPrinter(OutputStream out) {
    super(out);
  }

  /**
   * Print only one item and only using the <code>printObject</code> method.
   */
  public abstract void printObject(Object o);

  /**
   * Sample usage
   */

  public static void testMain(String type) {
    AbstractPrinter ap = AbstractPrinter.createPrinter(type, System.out);
    if (ap == null) {
      System.err.println("Unknown Printer type: "+type);
      return;
    }
    Object obj;
    /**/
    java.util.Vector v = new java.util.Vector();
    v.add("topv");
    v.add(new java.util.Vector());
    v.add(new Test(true));
    v.add(new java.util.Date());
    java.util.Vector v2 = new java.util.Vector();
    v2.add(new java.util.Date());
    v2.add(" ");
    v2.add(new Integer(123));
    v2.add(new Test(true));
    v2.add("tail");
    v.add(v2);
    v.add("the end");
    obj = (Object)v;
    /**/
    ap.printObject(obj);
  }

  public static class Test implements SelfPrinter, java.io.Serializable {
    private int i;
    private Float f;
    private Test nT;
    private String s;
    private Test sT;
    public Test() { /*used by others*/ }
    public Test(boolean b) { /* used by AbstractPrinter */
      i = 5;
      f = new Float(1.2);
      nT = null;
      s = "foo";
      sT = (b ? new Test(false) : null);
    }
    public void setFiveInt(int xi) { i = xi; }
    public void setOnePointTwoFloat(Float xf) { f = xf; }
    public void setNullTest(Test xnT) { nT = xnT; }
    public void setFooString(String xs) { s = xs; }
    public void setSubTest(Test xsT) { sT = xsT; }
    public void printContent(AsciiPrinter pr) {
      pr.print(i,  "FiveInt");
      pr.print(f,  "OnePointTwoFloat");
      pr.print(nT, "NullTest");
      pr.print(s,  "FooString");
      pr.print(sT, "SubTest");
    }
    public String toString() {
      return org.cougaar.util.PrettyStringPrinter.toString(this);
    }
  }

  public static void main(String[] args) {
    if (args.length != 1) {
      System.err.println("Expecting a single \"isValidFormat()\" argument, e.g. \"XML\"");
      return;
    }
    testMain(args[0]);
  }
}
