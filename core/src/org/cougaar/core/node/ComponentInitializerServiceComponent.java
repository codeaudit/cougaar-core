/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.util.GenericStateModelAdapter;

/** 
 * A component which creates and advertises the appropriate 
 * ComponentInitializerService ServiceProvider.
 * <p>
 * @see FileComponentInitializerServiceProvider
 * @see DBComponentInitializerServiceProvider
 **/
public final class ComponentInitializerServiceComponent
extends GenericStateModelAdapter
implements Component 
{
  private ServiceBroker sb;

  private DBInitializerService dbInit;
  private ServiceProvider theSP;

  public void setBindingSite(BindingSite bs) {
    // this is the *node* service broker!  The NodeControlService
    // is not available until the node-agent is created...
    this.sb = bs.getServiceBroker();
  }

  public void setDBInitializerService(DBInitializerService dbInit) {
    this.dbInit = dbInit;
  }

  public void load() {
    super.load();
    theSP = chooseSP();
    sb.addService(ComponentInitializerService.class, theSP);
  }

  public void unload() {
    sb.revokeService(ComponentInitializerService.class, theSP);
    super.unload();
  }

  private ServiceProvider chooseSP() {
    try {
      ServiceProvider sp;
      if (dbInit == null) {
        String prop = System.getProperty("org.cougaar.core.node.XML");
        if(prop != null && prop.equals("true")) {
          sp = new XMLFileComponentInitializerServiceProvider();
        } else {
          sp = new FileComponentInitializerServiceProvider();
        }
      } else {
        sp = new DBComponentInitializerServiceProvider(dbInit);
      }
      return sp;
    } catch (Exception e) {
      throw new RuntimeException("Exception while creating "+getClass().getName(), e);
    }
  }
}
