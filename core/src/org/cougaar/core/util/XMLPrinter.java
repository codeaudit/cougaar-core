/*
 * <copyright>
 *  Copyright 1999-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
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
    print(c);
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
