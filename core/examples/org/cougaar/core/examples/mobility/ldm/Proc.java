/* 
 * <copyright>
 *  Copyright 2002-2003 BBNT Solutions, LLC
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
package org.cougaar.core.examples.mobility.ldm;

import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;

/**
 * A Proc is a running Script -- it's short for "process",
 * which is a classname that's already used by 
 * "java.lang.Process".
 */
public interface Proc extends UniqueObject {

  /**
   * UID support from unique-object.
   */
  UID getUID();

  /**
   * Script UID, which is immutable.
   */
  UID getScriptUID();

  /**
   * Start time of the proc.
   * <p>
   * Pause and timeout times starting with "@" are offset
   * by this time; see ScriptParser for details.
   */
  long getStartTime();

  /**
   * Get the time of the proc completion.
   * <p>
   * If the proc failed, the current script index will
   * be less than the script size and the step uid
   * will be non-null.  If the proc succeeded, the
   * current script index will be equal to the script
   * size and the step uid will be null.
   */
  long getEndTime();

  /**
   * Get the count of the total number of moves requested
   * by this proc.
   */
  int getMoveCount();

  /**
   * Current Step UID, which changes as the script is 
   * proc.
   */
  UID getStepUID();

  /**
   * Current index in the script, which changes as the
   * script is proc.  I.e. program-counter.
   */
  int getScriptIndex();

  // add Map for proc-global variables?

  /**
   * For infrastructure use only!.  Set the end time.
   */
  void setEndTime(long time);

  /**
   * For infrastructure use only!.  Set the move count.
   */
  void setMoveCount(int count);

  /**
   * For infrastructure use only!.  Set the current step.
   */
  void setStepUID(UID uid);

  /**
   * For infrastructure use only!.  Set the current index.
   */
  void setScriptIndex(int index);

}

