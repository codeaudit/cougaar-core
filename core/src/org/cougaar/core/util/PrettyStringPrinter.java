/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
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
