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
public class XMLPrinter extends AsciiPrinter {

  public static void main(String[] args) {
    testMain("xml");
  }

  public XMLPrinter(java.io.OutputStream out) {
    super(out);
  }

  public void printBegin(String type, String name) {
    print("<");
    print(name);
    if (type != null) {
      print(" type=\"");
      print(type);
      print("\"");
    }
    print(">");
  }

  public void printEnd(String name) {
    print("</");
    print(name);
    print(">");
  }

  public void print(String type, String name, String x) {
    if (x == null) 
      return;
    print("<");
    print(name);
    print(" type=\"");
    print(type);
    print("\">");
    print(x);
    print("</");
    print(name);
    print(">");
  }

  public void print(String x, String name) {
    if (x == null) 
      return;
    print("<");
    print(name);
    print(">");
    print(x);
    print("</");
    print(name);
    print(">");
  }

  public void print(boolean z, String name) {
    print("<");
    print(name);
    print(">");
    print(z);
    print("</");
    print(name);
    print(">");
  }

  public void print(byte b, String name) {
    print("<");
    print(name);
    print(">");
    print(b);
    print("</");
    print(name);
    print(">");
  }

  public void print(char c, String name) {
    print("<");
    print(name);
    print(">");
    print((int)c);
    print("</");
    print(name);
    print(">");
  }

  public void print(short s, String name) {
    print("<");
    print(name);
    print(">");
    print(s);
    print("</");
    print(name);
    print(">");
  }

  public void print(int i, String name) {
    print("<");
    print(name);
    print(">");
    print(i);
    print("</");
    print(name);
    print(">");
  }

  public void print(long l, String name) {
    print("<");
    print(name);
    print(">");
    print(l);
    print("</");
    print(name);
    print(">");
  }

  public void print(float f, String name) {
    print("<");
    print(name);
    print(">");
    print(f);
    print("</");
    print(name);
    print(">");
  }

  public void print(double d, String name) {
    print("<");
    print(name);
    print(">");
    print(d);
    print("</");
    print(name);
    print(">");
  }

}
