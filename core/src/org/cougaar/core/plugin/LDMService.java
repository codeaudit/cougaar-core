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
package org.cougaar.core.plugin;

import org.cougaar.core.domain.*;

import java.util.*;
import org.cougaar.util.*;
import org.cougaar.core.component.*;
import org.cougaar.core.blackboard.*;
import org.cougaar.core.mts.*;
import org.cougaar.core.mts.*;
import org.cougaar.core.agent.*;
import org.cougaar.core.domain.*;


/** placeholder to clean up plugin->manager interactions **/
public interface LDMService 
  extends Service 
{
  /** Get a reference to the LDM object.  
   * @todo This should be refactored for Cougaar 9.4.
   **/
  LDMServesPlugin getLDM();

  /** Get a reference to the root factory.
   * @deprecated Use DomainService instead (9.2).
   **/
  RootFactory getFactory();
  /** Get a reference to a specific factory.
   * @deprecated Use DomainService instead (9.2).
   **/
  Factory getFactory(String s);

  // standin API for LDMService called by PluginBinder for temporary support
  /** Add a PrototypeProvider.
   * @deprecated Use PrototypeRegistryService instead (9.2).
   **/
  void addPrototypeProvider(PrototypeProvider plugin);
  /** Add a PropertyProvider.
   * @deprecated Use PrototypeRegistryService instead (9.2).
   **/
  void addPropertyProvider(PropertyProvider plugin);
  /** Add a LatePropertyProvider.
   * @deprecated Use PrototypeRegistryService instead (9.2).
   **/
  void addLatePropertyProvider(LatePropertyProvider plugin);
}
  
