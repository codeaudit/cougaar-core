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

