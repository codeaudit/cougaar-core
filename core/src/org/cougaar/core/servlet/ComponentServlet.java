/*
 * <copyright>
 *  Copyright 2000-2001 BBNT Solutions, LLC
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

package org.cougaar.core.servlet;

import java.util.*;
import java.net.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.cougaar.core.agent.*;
import org.cougaar.core.component.*;
import org.cougaar.core.mts.*;
import org.cougaar.core.service.*;
import org.cougaar.util.*;

/**
 * Abstract base-class for a Component that is also a Servlet.
 * <p>
 * This is very similar to BaseServletComponent, except that
 * the component itself is registered as the servlet.
 */
public abstract class ComponentServlet
extends HttpServlet
implements Component {

  // default state model (no multiple inheritence!):
  private final GenericStateModel gsm = 
    new GenericStateModelAdapter() {};

  // path parameter:
  private String myPath;

  // subclasses are free to use both of these:
  protected BindingSite bindingSite;
  protected ServiceBroker serviceBroker;

  // this class handles the "servletService" details:
  protected ServletService servletService;

  // the local agent address:
  protected AgentIdentificationService agentIdService;
  protected MessageAddress agentId;
  protected String encAgentName;

  public ComponentServlet() {
    super();
  }

  //
  // inherits "doGet(..)" and all other HttpServlet methods
  //

  /**
   * Capture the (optional) load-time parameters.
   * <p>
   * This is typically a List of Strings.
   */
  public void setParameter(Object o) {
    if (o instanceof String) {
      myPath = (String) o;
    } else if (o instanceof List) {
      List l = (List) o;
      if (l.size() > 0) {
        Object o1 = l.get(0);
        if (o1 instanceof String) {
          myPath = (String) o1;
        }
      }
    }
  }

  /**
   * Get the path for the Servlet's registration.
   * <p>
   * Typically supplied by the component parameter, but
   * subclasses can hard-code the path by overriding
   * this method.
   */
  protected String getPath() {
    return myPath;
  }

  protected MessageAddress getAgentIdentifier() {
    return agentId;
  }

  /** URL-encoded name of the local agent */
  protected String getEncodedAgentName() {
    return encAgentName;
  }

  public void setBindingSite(BindingSite bindingSite) {
    this.bindingSite = bindingSite;
    this.serviceBroker = bindingSite.getServiceBroker();
  }

  protected ServiceBroker getServiceBroker() {
    return serviceBroker;
  }

  public void setAgentIdentificationService(
      AgentIdentificationService agentIdService) {
    this.agentIdService = agentIdService;
    this.agentId = agentIdService.getMessageAddress();
    if (agentId != null) {
      try {
        String name = agentId.getAddress();
        encAgentName = URLEncoder.encode(name, "UTF-8");
      } catch (java.io.UnsupportedEncodingException e) {
        // should never happen
        throw new RuntimeException("Unable to encode to UTF-8?");
      }
    }
  }

  public void setServletService(ServletService servletService) {
    this.servletService = servletService;
  }

  public void initialize() throws StateModelException { gsm.initialize(); }

  public void load() {
    gsm.load();
    
    // register this servlet
    String path = getPath();
    try {
      servletService.register(path, this);
    } catch (Exception e) {
      String failMsg =
        ((path == null) ?
         "Servlet path not specified" :
         (servletService == null) ?
         "Unable to obtain servlet service" :
         "Unable to register servlet with path \""+path+"\"");
      throw new RuntimeException(failMsg, e);
    }

    // unlike ComponentPlugin, we typically do NOT want
    //   BlackboardService or AlarmService or SchedulerService,
    // since servlets run in the servlet server's thread.
    //
    // see the BlackboardQueryService for simple blackboard
    // access.
  }

  public void start()   throws StateModelException { gsm.start();   }
  public void suspend() throws StateModelException { gsm.suspend(); }
  public void resume()  throws StateModelException { gsm.resume();  }
  public void stop()    throws StateModelException { gsm.stop();    }
  public void halt()    throws StateModelException { gsm.halt();    }
  public int getModelState() { return gsm.getModelState(); }

  public void unload() {
    gsm.unload();
    // release the servlet service, which will automatically
    //   unregister the servlet
    if (servletService != null) {
      serviceBroker.releaseService(
        this, ServletService.class, servletService);
      servletService = null;
    }
    if (agentIdService != null) {
      serviceBroker.releaseService(
          this, AgentIdentificationService.class, agentIdService);
      agentIdService = null;
    }
    // your subclass should also release its services here!
  }
}
