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
package org.cougaar.core.examples.servlet;

import java.io.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.blackboard.BlackboardClient;
import org.cougaar.core.component.*;
import org.cougaar.core.service.*;
import org.cougaar.core.servlet.BaseServletComponent;

import org.cougaar.core.domain.RootFactory;
import org.cougaar.planning.ldm.plan.*;

public class AddTaskServletComponent 
extends BaseServletComponent 
implements BlackboardClient
{

  private ClusterIdentifier agentId;
  private BlackboardService blackboard;
  private DomainService ds;
  private RootFactory rootFactory;

  public void load() {
    // FIXME need AgentIdentificationService
    org.cougaar.core.plugin.PluginBindingSite pbs =
      (org.cougaar.core.plugin.PluginBindingSite) bindingSite;
    this.agentId = pbs.getAgentIdentifier();

    super.load();
  }

  protected String getPath() {
    return "/addtask";
  }

  //
  // These "setXService(XService x) {..}" methods
  // are equivalent to the SimpleServletComponent's
  // "public void load() { .. serviceBroker.getService(..); .. }"
  // calls, EXCEPT that:
  //   1) these methods are only called at load-time.
  //   2) if one of these services is not available then this 
  //      Component will NOT be loaded.  In contrast, the 
  //      "load()" pattern allows the Component to (optionally) 
  //      continue loading even if any "getService(..)" returns null.
  //   3) these "setXService(..)" will request the service with
  //      "this" as the requestor.  The more generic "getService(..)"
  //      API allows the Component to pass a different class
  //      (e.g. an inner class to handle callbacks).
  //

  public void setBlackboardService(BlackboardService blackboard) {
    this.blackboard = blackboard;
  }

  public void setDomainService(DomainService ds) {
    this.ds = ds;
    this.rootFactory = ds.getFactory();
  }

  protected Servlet createServlet() {
    return new MyServlet();
  }

  public void unload() {
    super.unload();
    // FIXME release the rest!
  }

  private class MyServlet extends HttpServlet {
    public void doGet(
        HttpServletRequest req,
        HttpServletResponse res) throws IOException {
      PrintWriter out = res.getWriter();
      out.println("<html><body bgcolor=\"white\">");

      // add a new task to the blackboard:
      NewTask nt = rootFactory.newTask();
      nt.setSource(agentId);
      nt.setVerb(Verb.getVerb("fromAddTaskServlet"));
      try {
        blackboard.openTransaction();
        blackboard.publishAdd(nt);
      } finally {
        blackboard.closeTransactionDontReset();
      }

      out.println(
          "AddTaskServletComponent "+
          "publish-Added task (<tt>"+
          nt.getUID()+
          "</tt>) with verb \"<tt>"+
          nt.getVerb()+
          "</tt>\" to agent <b>"+
          agentId+
          "</b>'s Blackboard.");
      out.println("</body></html>");
    }
  }

  //
  // These are oddities of implementing BlackboardClient:
  //
  // Note: A Component must implement BlackboardClient in order 
  // to obtain BlackboardService.
  //

  // odd BlackboardClient method:
  public String getBlackboardClientName() {
    return toString();
  }

  // odd BlackboardClient method:
  public long currentTimeMillis() {
    throw new UnsupportedOperationException(
        this+" asked for the current time???");
  }

  // unused BlackboardClient method:
  public boolean triggerEvent(Object event) {
    // if we had Subscriptions we'd need to implement this.
    //
    // see "ComponentPlugin" for details.
    throw new UnsupportedOperationException(
        this+" only supports Blackboard queries, but received "+
        "a \"trigger\" event: "+event);
  }
}
