/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.component;

import java.beans.*;
import java.beans.beancontext.*;
import java.util.*;
import java.net.URL;

/** This is a "leaf" component which supports a 
 * simple run model.  A more explicit run state API
 * might be preferrable, but implementations are 
 * encouraged to implement this interface in a way
 * which enforces the documented contract.
 * <P>
 * Call sequence: classload, instantiation, load (setBeanContext),
 * intialize(), start(), stop(), finalize().
 **/

public interface Plugin 
  extends Component
{
  /** initialize() is called once shortly after instantiation.  
   * A plugin may request and offer services, but plugins
   * should not make calls to granted services at this point.
   **/
  void initialize();

  /** start() is called after initialize() or stop() 
   * to indicate that the Plugin may run by making calls
   * on granted services, etc.
   **/
  void start();
  
  /** stop() is called after a call to start() to indicate
   * that the Plugin should stop processing 
   **/
  void stop();

  // finalize is the same as usual in java
}

  
