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

package org.cougaar.core.security;

import org.cougaar.core.component.*;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.agent.*;
import org.cougaar.core.mts.*;
import org.cougaar.core.service.MessageTransportService;
import org.cougaar.util.*;
import java.lang.reflect.*;
/**
 * "default" security manager for integration work.
 * Supports old properties org.cougaar.core.security.manager and org.cougaar.manager for
 * backward compatability.
 * @property org.cougaar.core.security.Domain
 *   The security domain ID to use for the StandardSecurityManager.
 **/

public final class StandardSecurityComponent
  extends SecurityComponent
  implements MessageTransportClient
{
  public final static String SMD_PROP = PROP_PREFIX+".Domain";

  protected Object guard = null;
  protected AgentChildBindingSite bindingSite = null;

  public StandardSecurityComponent() {
  }

  public void setBindingSite(BindingSite bs) {
    if (bs instanceof AgentChildBindingSite) {
      bindingSite = (AgentChildBindingSite) bs;
    } else {
      throw new RuntimeException("Tried to load "+this+" into " + bs);
    }
  }

  protected MessageTransportService mts = null;
  public synchronized final void setMessageTransportService(MessageTransportService mts) {
    if (this.mts == null && mts != null) {
      this.mts = mts;
      maybeActivate();
    }
  }

  private NodeIdentificationService nodeIdentificationService = null;
  public void setNodeIdentificationService(NodeIdentificationService nis) {
    nodeIdentificationService = nis;
  }
  protected NodeIdentificationService getNodeIdentificationService() {
    return nodeIdentificationService;
  }

  public void load() {
    if (nodeIdentificationService == null) {
      throw new RuntimeException("StandardSecurityComponent requires the NodeIdentificationService");
    }

    super.load();
    String dmId = System.getProperty(SMD_PROP);
    // hacks for backward compatability
    {
      String foo = System.getProperty("org.cougaar.core.security.manager");
      if (foo != null) {
        System.err.println("Warning! org.cougaar.core.security.manager is obsolete: use org.cougaar.core.security.Domain!");
        if (dmId == null) {
          dmId = foo;
        } else {
          //System.exit(-1);
        }
      }
    }
    { 
      String foo = System.getProperty("org.cougaar.manager");
      if (foo != null) {
        System.err.println("Warning! org.cougaar.manager is obsolete: use org.cougaar.core.security.Domain!");
        if (dmId == null) {
          dmId = foo;
        } else {
          //System.exit(-1);
        }
      }
    }
    
    // figure out the name
    String name = getNodeIdentificationService().getNodeIdentifier().toString();

    if (dmId == null) {
      //System.err.println("System property "+SMD_PROP+" not set.\nProceeding without Guard!");
    } else {
      System.err.println("Creating Guard");
      try {
        Class gc = Class.forName("SAFE.Guard.NodeGuard");
        Class pbc = Class.forName("org.cougaar.core.security.policy.PolicyBootstrapper");
        //Class pbc = Class.forName("com.nai.security.PolicyBootstrapper");
        Constructor gcc = gc.getConstructor(
                                            new Class[] {
                                              String.class, String.class,
                                              String.class, pbc
                                            });
        //System.out.println("Creating Guard.");
        guard = gcc.newInstance(new Object[] { name, name, dmId, pbc.newInstance() });
        
        //System.out.println("Initializing Guard.");
        Method ginit = guard.getClass().getMethod("initialize", new Class[] {});
        ginit.invoke(guard, new Object[] {});
      } catch (Exception e) {
        System.err.println("ERROR: while loading NodeGuard: " + e);
      }
    }
    
    final ServiceBroker sb = bindingSite.getServiceBroker();
    sb.addServiceListener(new ServiceAvailableListener() {
        public void serviceAvailable(ServiceAvailableEvent ae) {
          Class sc = ae.getService();
          if (MessageTransportService.class.isAssignableFrom(sc)) {
            Object ts = sb.getService(StandardSecurityComponent.this, MessageTransportService.class, null);
            if (ts instanceof MessageTransportService) {
              StandardSecurityComponent.this.setMessageTransportService((MessageTransportService)ts);
              maybeActivate();
            }
          }
        }
      });

    maybeActivate();
  }
  
  private boolean isActivated = false;

  protected synchronized void maybeActivate() {
    if (!isActivated) {
      if (guard != null && mts != null) {
        System.err.println("Activating Guard");
        try {
          Method gsmt = guard.getClass()
            .getMethod("setMessageTransport", new Class[] { MessageTransportService.class });
          gsmt.invoke(guard, new Object[] { mts } );
        } catch (Exception e) {
          e.printStackTrace();
          System.exit(0);
        }
        isActivated = true;
      }
    }
  }

  public void setState(Object loadState) {}
  public Object getState() {return null;}

  public synchronized void unload() {
    super.unload();
    // unload services in reverse order of "load()"
    ServiceBroker sb = bindingSite.getServiceBroker();
    // release services
    if (mts != null) {
      sb.releaseService(this, MessageTransportService.class, mts);
      mts = null;
    }
  }

  // implement MessageTransportClient
  public void receiveMessage(Message m) {}
  public MessageAddress getMessageAddress() {return null;}

}
