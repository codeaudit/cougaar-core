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
public class PrettyXMLPrinter extends AsciiPrettyPrinter {

  public static void main(String[] args) {
    testMain("prettyxml");
  }

  public PrettyXMLPrinter(java.io.OutputStream out) {
    super(out);
  }

  public void printBegin(String type, String name) {
    printIndent();
    print("<");
    print(name);
    if (type != null) {
      print(" type=\"");
      print(type);
      print("\"");
    }
    print(">\n");
    ++indent;
  }

  public void printEnd(String name) {
    --indent;
    printIndent();
    print("</");
    print(name);
    print(">\n");
  }

  public void print(String type, String name, String x) {
    if (x == null) 
      return;
    printIndent();
    print("<");
    print(name);
    print(" type=\"");
    print(type);
    print("\">");
    print(x);
    print("</");
    print(name);
    print(">\n");
  }

  public void print(String x, String name) {
    if (x == null) 
      return;
    printIndent();
    print("<");
    print(name);
    print(">");
    print(x);
    print("</");
    print(name);
    print(">\n");
  }

  public void print(boolean z, String name) {
    print((z ? "True" : "False"), name);
  }

  public void print(byte b, String name) {
    print(Byte.toString(b), name);
  }

  public void print(char c, String name) {
    print(String.valueOf(c), name);
  }

  public void print(short s, String name) {
    print(Short.toString(s), name);
  }

  public void print(int i, String name) {
    print(Integer.toString(i), name);
  }

  public void print(long l, String name) {
    print(Long.toString(l), name);
  }

  public void print(float f, String name) {
    print(Float.toString(f), name);
  }

  public void print(double d, String name) {
    print(Double.toString(d), name);
  }

}
