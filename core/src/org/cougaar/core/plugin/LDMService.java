/*
 * <copyright>
 * Copyright 2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.plugin;

import java.util.*;
import org.cougaar.util.*;
import org.cougaar.core.component.*;
import org.cougaar.core.blackboard.*;
import org.cougaar.core.cluster.*;
import org.cougaar.domain.planning.ldm.*;


/** placeholder to clean up plugin->manager interactions **/
public interface LDMService 
  extends Service 
{
  LDMServesPlugIn getLDM();
  RootFactory getFactory();
  Factory getFactory(String s);

  // standin API for LDMService called by PluginBinder for temporary support
  void addPrototypeProvider(PrototypeProvider plugin);
  void addPropertyProvider(PropertyProvider plugin);
  void addLatePropertyProvider(LatePropertyProvider plugin);
}
  
