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
public class DefaultPrinter extends AsciiPrinter {

  public static void main(String[] args) {
    testMain("default");
  }

  public DefaultPrinter(java.io.OutputStream out) {
    super(out);
  }

  public void printBegin(String type, String name) {
  }

  public void printEnd(String name) {
  }

  public void print(Object o, String name) {
    print(o);
  }

  public void print(String x, String name) {
    print(x);
  }

  public void print(boolean z, String name) {
    print(z);
  }

  public void print(byte b, String name) {
    print(b);
  }

  public void print(char c, String name) {
    print(c);
  }

  public void print(short s, String name) {
    print(s);
  }

  public void print(int i, String name) {
    print(i);
  }

  public void print(long l, String name) {
    print(l);
  }

  public void print(float f, String name) {
    print(f);
  }

  public void print(double d, String name) {
    print(d);
  }

}
