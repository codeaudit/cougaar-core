/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
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

package org.cougaar.core.service;

import org.cougaar.core.component.Service;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.persist.PersistenceState;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;

/** Now an interface for backwards compatability.
 ** Real stuff is in UIDServiceImpl and the real interface
 ** should be UIDService(which is a marker for now).
 **/

public interface UIDServer extends Service {
 /** MessageAddress of the proxy server.
   *  This might go away if we ever really separated proxy 
   * servers from clusters.
   **/
  MessageAddress getMessageAddress();

 /** get the next Unique ID for the Proxiable object registry
   * at this server.
   * It is better for Factories to use the registerUniqueObject method.
   **/
  UID nextUID();

  /** assign a new UID to a unique object.
   **/
  UID registerUniqueObject(UniqueObject o);


  // persistence backwards compat
  PersistenceState getPersistenceState();

  /** called during persistence rehydration to reset the state **/
  void setPersistenceState(PersistenceState state);


}
