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
 * Note:.
 *   Currently starts html page with useless "<tr><td ..>" and ends
 *   the page with a "</td></tr>".  Most browsers will ignore, and
 *   I will fix soon.
 * @see AbstractPrinter
 */
public class HTMLPrinter extends AsciiPrinter {

  public static void main(String[] args) {
    testMain("html");
  }

  public HTMLPrinter(java.io.OutputStream out) {
    super(out);
  }

  public void beforeFirstPrint() {
    print("<HTML><HEAD><TITLE>HTML Output</TITLE></HEAD><BODY>\n");
  }

  public void afterLastPrint() {
    print("</BODY></HTML>");
  }

  public void printBegin(String type, String name) {
    print("<tr><td align=right colspan=3>\n<table width=\"95%\" border=1 cellpadding=2>\n<tr><td colspan=3 bgcolor=lightgrey><center><b>");
    print(type);
    print("&nbsp;&nbsp;&nbsp;");
    print(name);
    print("</center></b></td></tr>\n");
  }

  public void printEnd(String name) {
    print("</table><p>\n</td></tr>\n");
  }

  public void printBeginCollection(String type, String name, int size) {
    printBegin(type+"("+size+")", name);
  }

  public void printBeginElement() {
  }

  protected void printElement(StringObjectInfo soi) {
    printBegin(soi.getClassName(), "element");
    print(soi.getClassName(), "&nbsp;", soi.getValue());
    printEnd("element");
  }

  public void printEndElement() {
  }

  public void printEndCollection(String name) {
    printEnd(name);
  }

  public void print(String className, String fieldName, String value) {
    print("<tr><td>");
    print(className);
    print("</td><td>");
    print(fieldName);
    print("</td><td>");
    print(value);
    print("</td></tr>\n");
  }

  public void print(String x, String name) {
    if (x != null)
      print("java.lang.String", name, x);
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

}
