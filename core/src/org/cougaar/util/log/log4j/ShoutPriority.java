/*
 * <copyright>
 *  Copyright 2001 BBNT Solutions, LLC
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

package org.cougaar.util.log.log4j;

import org.apache.log4j.Priority;

/**
 * Logging level that is higher than error but less than 
 * fatal, which is used for information messages that should 
 * rarely be filtered.
 * <p>
 * <b>NOTE:</b> a reference to the "SHOUT" priority from a
 * log4j properties file must specify the full 
 * "SHOUT#org.cougaar.util.log.log4j.ShoutPriority"
 * line, otherwise log4j doesn't know this class and will
 * default the level to "DEBUG".
 * <p>
 * For example, so set "com.foo.*" to SHOUT or higher:<pre>
 *   ...
 *   log4j.category.com.foo=SHOUT#org.cougaar.util.log.log4j.ShoutPriority
 *   ...
 * </pre>
 * Other levels do not need this "#.." suffix, e.g.:<pre>
 *   ...
 *   log4j.category.com.foo=INFO
 *   ...
 * </pre>
 */
public class ShoutPriority extends Priority {

  static final int SHOUT_INT = Priority.ERROR_INT + 1;

  static String SHOUT_STR = "SHOUT";

  static final ShoutPriority SHOUT =
    new ShoutPriority(SHOUT_INT, SHOUT_STR, 0);

  protected ShoutPriority(int level, String strLevel, int syslogEquiv) {
    super(level, strLevel, syslogEquiv);
  }

  public static Priority toPriority(String sArg, Priority defaultValue) {
    return 
      ((SHOUT_STR.equalsIgnoreCase(sArg)) ?
       (SHOUT) :
       (Priority.toPriority(sArg, defaultValue)));
  }

  public static Priority toPriority(int i) throws  IllegalArgumentException {
    return 
      ((i == SHOUT_INT)?
       (SHOUT) :
       (Priority.toPriority(i)));
  }
}
