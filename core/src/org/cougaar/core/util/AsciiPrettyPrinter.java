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
public abstract class AsciiPrettyPrinter extends AsciiPrinter {

  public AsciiPrettyPrinter(java.io.OutputStream out) {
    super(out);
  }

  protected int indent = 0;

  protected static String[] presetIndents;
  static {
    growPresetIndents();
  }

  protected static void growPresetIndents() {
    int newLen = ((presetIndents != null) ? 
	          (2*presetIndents.length) :
		  10);
    presetIndents = new String[newLen];
    StringBuffer sb = new StringBuffer(2*newLen);
    for (int i = 0; i < newLen; i++) {
      presetIndents[i] = sb.toString();
      sb.append("  ");
    }
  }

  protected void printIndent() {
    while (true) {
      try {
        print(presetIndents[indent]);
	return;
      } catch (ArrayIndexOutOfBoundsException e) {
	// rare!
	if (indent < 0)
	  throw new RuntimeException("NEGATIVE INDENT???");
	growPresetIndents();
      }
    }
  }

}
