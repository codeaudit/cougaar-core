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

import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import javax.swing.text.AttributeSet;

public class NumericDocument extends PlainDocument {
  /**
   * Default constructor creates a NumericDocument holding a zero value.
   **/
  public NumericDocument() {
    try {
      insertString(0, "0", null);
    }
    catch (BadLocationException e) {
    }
  }
  /**
   * Insert a string into the document. The string is checked to
   * insure that all characters are digits.
   * @param offset the location in the document where insertion is to occur.
   * @param s the string to insert -- all characters must be decimal digits.
   * @param attrs the set of attributes fo the inserted characters.
   **/
  public void insertString(int offset, String s, AttributeSet attrs) throws BadLocationException {
    for (int i = 0, n = s.length(); i < n; i++) {
      char c = s.charAt(i);
      if (c < '0' || c > '9') {
        throw new IllegalArgumentException("not a digit");
      }
    }
    super.insertString(offset, s, attrs);
  }

  /**
   * Replace the current value in the document with a new value.
   * @param value the new value to insert.
   **/
  public void setValue(int value) {
    try {
      remove(0, getLength());
      insertString(0, Integer.toString(value), null);
    }
    catch (BadLocationException e) {
    }
  }

  /**
   * Get the current value in the document. Converts the string in
   * the document to a number.
   * @return the value in the buffer as an int.
   **/
  public int getValue() {
    try {
      String text = getText(0, getLength());
      return Integer.parseInt(text);
    }
    catch (BadLocationException e) {
      return 0;
    }
  }
}
