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

package org.cougaar.util;

import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import javax.swing.text.AttributeSet;
import java.util.StringTokenizer;
import java.util.ArrayList;

public class NumericListDocument extends PlainDocument {
  /**
   * Default constructor creates a NumericListDocument holding a single zero value.
   **/
  public NumericListDocument() {
    try {
      insertString(0, "0", null);
    }
    catch (BadLocationException e) {
    }
  }
  /**
   * Insert a string into the document. The string is checked to
   * insure that it is a comma-separated or space separated list of numbers
   * @param offset the location in the document where insertion is to occur.
   * @param s the string to insert -- all characters must be decimal digits.
   * @param attrs the set of attributes fo the inserted characters.
   **/
  public void insertString(int offset, String s, AttributeSet attrs) throws BadLocationException {
    for (int i = 0, n = s.length(); i < n; i++) {
      char c = s.charAt(i);
      if ((c < '0' || c > '9') && c != ',' && c != ' ') {
        throw new IllegalArgumentException("not a digit or separator");
      }
    }
    super.insertString(offset, s, attrs);
  }

  /**
   * Replace the current value in the document with a new value.
   * @param value the new value to insert.
   **/
  public void setValue(int value) {
    int[] v = {value};
    setValues(v);
  }

  public void setValues(int[] values) {
    try {
      remove(0, getLength());
      StringBuffer buf = new StringBuffer();
      for (int i = 0; i < values.length; i++) {
        buf.append(", ");
        buf.append(values[i]);
      }
      insertString(0, buf.substring(2), null);
    }
    catch (BadLocationException e) {
    }
  }

  /**
   * Get the current value in the document. Converts the string in
   * the document to a number.
   * @return the value in the buffer as an int.
   **/
  public int[] getValues() {
    try {
      ArrayList strings = new ArrayList();
      String text = getText(0, getLength());
      StringTokenizer tokens = new StringTokenizer(text, ", ");
      while (tokens.hasMoreTokens()) {
        strings.add(tokens.nextToken());
      }
      int[] result = new int[strings.size()];
      for (int i = 0; i < result.length; i++) {
        result[i] = Integer.parseInt((String) strings.get(i));
      }
      return result;
    }
    catch (BadLocationException e) {
      return new int[0];
    }
  }
}

