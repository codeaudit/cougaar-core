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

package org.cougaar.core.node;

import org.cougaar.core.mts.*;

import org.cougaar.core.component.Service;

import java.util.Observer;

public interface TrustStatusService extends Service {
  /** Get the Trust level of the Society.
   *  Trust level is an int between 0 and 10
   *  with 0 being trust no one.
   *  @return int The Trust Level for the Society
   **/
  int getSocietyTrust();

  /** Register with the Observable for Society Trust levels 
   *  so that when the value changes, you will be notified via
   *  the the Observer object passed in.
   *  @return Observer
   **/
  void registerSocietyTrustObserver(Observer o);

  /** Unregister the Observer for Society Trust Levels.
   *  Note this should always be called before releasing 
   *  this service!
   *  @param Observer To unregister
   **/
  void unregisterSocietyTrustObserver(Observer obs);

}  
