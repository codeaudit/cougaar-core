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

public class TestPlugin implements Plugin {
  public TestPlugin() {
    System.err.println("TestPlugin()");
  }
  private Object parameter = null;
  public void setParameter(Object o) {
    parameter = o;
    System.err.println("TestPlugin.setParameter("+o+")");
  }

  private PluginBindingSite binder;
  public void setBinder(Binder b) {
    binder = (PluginBindingSite) b;
    System.err.println("TestPlugin.setBinder("+b+")");
  }
  public void initialize(Binder b) {
    System.err.println("TestPlugin.initialize("+b+")");    
  }
  public void initialize() {
    System.err.println("TestPlugin.initialize()");    
  }

}
