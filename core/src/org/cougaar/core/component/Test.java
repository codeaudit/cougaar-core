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

import java.util.*;
import java.net.URL;

/** Test case for component model 
 **/
public class Test {
  public static void main(String[] args) {
    PluginManager pm = new PluginManager();

    ComponentDescription bd = 
      new ComponentDescription("agent.plugin",
                               "org.cougaar.core.component.TestPlugin",
                               null, // codebase
                               "Foo", // parameter
                               null, // cert
                               null, // lease
                               null // policy
                               );
    pm.add(bd);
    System.err.println("Added "+bd);
    
    System.err.println("Contents of PluginManager:");
    for (Iterator i = pm.iterator(); i.hasNext();) {
      Object c = i.next();
      System.err.println("\t"+c);
    }


    System.err.println("Done");
  }
}
