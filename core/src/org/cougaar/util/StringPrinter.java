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

/**
 * @see AbstractPrinter
 */
public class StringPrinter extends AsciiPrinter {

  public static void main(String[] args) {
    testMain("string");
  }

  public StringPrinter(java.io.OutputStream out) {
    super(out);
  }

  public void printBegin(String type, String name) {
    print(type);
    print(" ");
    print(name);
    print(" {\n");
  }

  public void printEnd(String name) {
    print("}\n");
  }

  public void printBeginCollection(String type, String name, int size) {
    print(type);
    print("(");
    print(size);
    print(") ");
    print(name);
    print(" {\n");
  }

  protected void printElement(StringObjectInfo soi) {
    print(soi.getClassName());
    print(" element {");
    print(soi.getValue());
    print("}\n");
  }

  public void printEndCollection(String name) {
    print("} \n");
  }

  public void print(String className, String fieldName, String value) {
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
    StringPrinter pr = new StringPrinter(baout);
    pr.printBegin(sp.getClass().getName(), "toString");
    sp.printContent(pr);
    pr.printEnd("toString");
    return baout.toString();
  }
}
