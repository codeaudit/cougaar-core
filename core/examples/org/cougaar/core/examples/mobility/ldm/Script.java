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

import java.io.Serializable;
import org.cougaar.core.mobility.Ticket;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;

/**
 * A Script is an immutable list of mobility steps and 
 * control loops.
 * <p>
 * See ScriptParser for parsing details.
 * <p>
 * A Proc is a runtime (mutable) instance of a 
 * Script.  The Script itself is immutable, like code.
 * <p>
 * This will very likely be enhanced with more complex 
 * control logic.  For now there's just:<ul>
 *    <li>Step options, to create a single step</li>
 *    <li>Goto entries, to create loops</li>
 * </ul>
 * Any step failure will halt a script.
 */
public interface Script extends UniqueObject {

  /**
   * UID support from unique-object.
   */
  UID getUID();

  /**
   * Get the number of entries.
   */
  int getSize();

  /**
   * Get the script entry at the specified index.
   */
  Entry getEntry(int idx);

  // marker for entries
  interface Entry {
  }

}
