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

package org.cougaar.core.service;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.component.Service;
import org.cougaar.core.persist.PersistenceState;
import org.cougaar.core.persist.StatePersistable;

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
