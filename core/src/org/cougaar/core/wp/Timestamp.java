/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

package org.cougaar.core.wp;


/**
 * Utility class to format longs as:<pre>
 *    TIME{[+-]RELATIVE_TO_NOW}
 * </pre>.
 * <p>
 * For example, if t=123 and now=300, then <tt>toString(t,now)</tt>
 * will return:<pre>
 *    123{-177}
 * </pre>
 * <p>
 * One exception is when t=0, in which case the method will
 * return:<pre>
 *    0
 * </pre>
 */
public final class Timestamp {
  private Timestamp() { }

  public static String toString(long t) {
    if (t == 0) {
      return "0";
    } else {
      return toString(t, System.currentTimeMillis());
    }
  }

  public static String toString(long t, long now) {
    if (t == 0) {
      return "0";
    } else {
      long diff = t - now;
      return 
        t+"{"+
        (diff >= 0 ?
         "+"+diff+"}" :
         diff+"}");
    }
  }
}
