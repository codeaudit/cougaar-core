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

import java.io.*;
import java.util.*;

import org.cougaar.util.GenericStateModelAdapter;

import org.cougaar.core.component.*;
import org.cougaar.core.servlet.ServletService;

import javax.servlet.Servlet;

/**
 * Abstract base-class for a Component that obtains the ServletService
 * and registers a Servlet.
 */
public abstract class BaseServletComponent
  extends GenericStateModelAdapter
  implements Component
{
  // subclasses are free to use both of these:
  protected BindingSite bindingSite;
  protected ServiceBroker serviceBroker;

  // this class handles the "servletService" details:
  protected ServletService servletService;

  public BaseServletComponent() {
    super();
  }

  public void setBindingSite(BindingSite bindingSite) {
    this.bindingSite = bindingSite;
    this.serviceBroker = bindingSite.getServiceBroker();
  }

  /**
   * Capture the (optional) load-time parameters.
   * <p>
   * This is typically a List of Strings.
   */
  public void setParameter(Object o) {
  }

  public void load() {
    super.load();
    
    // get the servlet service
    servletService = (ServletService)
      serviceBroker.getService(
		    this,
		    ServletService.class,
		    null);
    if (servletService == null) {
      throw new RuntimeException(
          "Unable to obtain servlet service");
    }

    // get the path for the servlet
    String path = getPath();

    // load the servlet instance
    Servlet servlet = createServlet();

    // register the servlet
    try {
      servletService.register(path, servlet);
    } catch (Exception e) {
      throw new RuntimeException(
          "Unable to register servlet \""+
          servlet.getClass().getName()+
          "\" with path \""+
          path+"\": "+e);
    }
  }

  public void unload() {
    super.unload();
    // release the servlet service, which will automatically
    //   unregister the servlet
    if (servletService != null) {
      serviceBroker.releaseService(
        this, ServletService.class, servletService);
    }
    // your subclass should also release its services here!
  }

  /**
   * Get the path for the Servlet's registration.
   */
  protected abstract String getPath();

  /**
   * Create the Servlet instance.
   * <p>
   * This is done within "load()", and is also a good time to 
   * aquire additional services from the "serviceBroker"
   * (for example, BlackboardService).
   */
  protected abstract Servlet createServlet();

}
