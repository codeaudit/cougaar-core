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
import org.cougaar.core.service.PrototypeRegistryService;
import org.cougaar.core.service.DomainService;
import java.util.*;
import org.cougaar.util.*;
import org.cougaar.core.component.*;
import org.cougaar.core.blackboard.*;
import org.cougaar.core.mts.*;
import org.cougaar.core.mts.*;
import org.cougaar.core.agent.*;
import org.cougaar.core.domain.*;

/** placeholder to clean up plugin->manager interactions **/
public class LDMServiceProvider implements ServiceProvider
{
  private LDMService ls;
  /*
  public LDMServiceProvider(LDMServesPlugin lsp, PrototypeRegistryService prs, DomainService ds) {
    // rather this was an assert!
    if (lsp == null || prs == null || ds == null)
      throw new IllegalArgumentException("LDMServiceProvider Constructor arguments must be non-null ("+
                                         lsp+", "+prs+", "+ds+")");

    this.ls = new LDMServiceImpl(lsp,prs,ds);
  }
  */
  public LDMServiceProvider(LDMService ls) {
    this.ls = ls;
  }

  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    if (LDMService.class.isAssignableFrom(serviceClass)) {
      return ls;
    } else {
      return null;
    }
  }

  public void releaseService(ServiceBroker sb, Object requestor, Class serviceClass, Object service) {
  }

  private static class LDMServiceImpl implements LDMService {
    private LDMServesPlugin lsp;
    private PrototypeRegistryService prs;
    private DomainService ds;
    private LDMServiceImpl(LDMServesPlugin lsp, PrototypeRegistryService prs, DomainService ds) {
      this.lsp = lsp;
      this.prs = prs;
      this.ds = ds;
    }
    public LDMServesPlugin getLDM() {
      return lsp;
    }
    public RootFactory getFactory() {
      return ds.getFactory();
    }
    public Factory getFactory(String s) {
      return ds.getFactory(s);
    }

    // standin API for LDMService called by PluginBinder for temporary support
    public void addPrototypeProvider(PrototypeProvider plugin) {
      prs.addPrototypeProvider(plugin);
    }
    public void addPropertyProvider(PropertyProvider plugin) {
      prs.addPropertyProvider(plugin);
    }
    public void addLatePropertyProvider(LatePropertyProvider plugin) {
      prs.addLatePropertyProvider(plugin);
    }
  }
}
  
