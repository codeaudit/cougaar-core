/* 
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
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
