/*
 * Copyright 1997-1999 Defense Advanced Research Projects Agency (DARPA)
 * and ALPINE (A BBN Technologies (BBN) and Raytheon Systems Company
 * (RSC) Consortium). This software to be used in accordance with the
 * COUGAAR license agreement.  The license agreement and other
 * information on the Cognitive Agent Architecture (COUGAAR) Project can
 * be found at http://www.cougaar.org or email: info@cougaar.org.
 */

package org.cougaar.core.util;

/**
 * @see AbstractPrinter
 */
public class PrettyStringPrinter extends AsciiPrettyPrinter {

  public static void main(String[] args) {
    testMain("prettystring");
  }

  public PrettyStringPrinter(java.io.OutputStream out) {
    super(out);
  }

  public void printBegin(String type, String name) {
    printIndent();
    print(type);
    print(" ");
    print(name);
    print(" {\n");
    ++indent;
  }

  public void printEnd(String name) {
    --indent;
    printIndent();
    print("}\n");
  }

  public void printBeginCollection(String type, String name, int size) {
    printIndent();
    print(type);
    print("(");
    print(size);
    print(") ");
    print(name);
    print(" {\n");
    ++indent;
  }

  protected void printElement(StringObjectInfo soi) {
    printIndent();
    print(soi.getClassName());
    print(" element {");
    print(soi.getValue());
    print("}\n");
  }

  public void printEndCollection(String name) {
    --indent;
    printIndent();
    print("} \n");
  }

  public void print(String className, String fieldName, String value) {
    printIndent();
    print(className);
    print(" ");
    print(fieldName);
    print(": ");
    print(value);
    print("\n");
  }

  public void print(String x, String name) {
    if (x != null)
      print("String", name, x);
  }

  public void print(boolean z, String name) {
    print("boolean", name, (z ? "True" : "False"));
  }

  public void print(byte b, String name) {
    print("byte", name, Byte.toString(b));
  }

  public void print(char c, String name) {
    print("char", name, String.valueOf(c));
  }

  public void print(short s, String name) {
    print("short", name, Short.toString(s));
  }

  public void print(int i, String name) {
    print("int", name, Integer.toString(i));
  }

  public void print(long l, String name) {
    print("long", name, Long.toString(l));
  }

  public void print(float f, String name) {
    print("float", name, Float.toString(f));
  }

  public void print(double d, String name) {
    print("double", name, Double.toString(d));
  }

  public static String toString(SelfPrinter sp) {
    java.io.ByteArrayOutputStream baout = 
      new java.io.ByteArrayOutputStream();
    PrettyStringPrinter pr = new PrettyStringPrinter(baout);
    pr.print(sp, "toString");
    return baout.toString();
  }
}
