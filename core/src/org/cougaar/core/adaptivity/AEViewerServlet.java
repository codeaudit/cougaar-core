/* 
 * <copyright>
 * Copyright 2002 BBNT Solutions, LLC
 * under sponsorship of the Defense Advanced Research Projects Agency (DARPA).

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).

 * THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 * PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 * IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 * ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 * HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 * TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.core.adaptivity;

import java.io.PrintWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StreamTokenizer;

import java.util.Collection;
import java.util.Iterator;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.cougaar.util.UnaryPredicate;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.servlet.SimpleServletSupport;
import org.cougaar.core.servlet.BlackboardServletSupport;


/**
 * Servlet to view adaptivity objects and edit operating mode policies
 */
public class AEViewerServlet extends HttpServlet {

  private BlackboardServletSupport support;

  private static UnaryPredicate conditionPredicate = 
    new UnaryPredicate() { 
	public boolean execute(Object o) {
	  if (o instanceof Condition) {
	    return true;
	  }
	  return false;
	}
      };

  private static UnaryPredicate omPredicate = 
    new UnaryPredicate() { 
	public boolean execute(Object o) {
	  if (o instanceof OperatingMode) {
	    return true;
	  }
	  return false;
	}
      };

  private static UnaryPredicate omPolicyPredicate = 
    new UnaryPredicate() { 
	public boolean execute(Object o) {
	  if (o instanceof OperatingModePolicy) {
	    return true;
	  }
	  return false;
	}
      };

  public void setSimpleServletSupport(SimpleServletSupport support) {
    if (support instanceof BlackboardServletSupport) {
      this.support = (BlackboardServletSupport)support;
    } else {
      throw new RuntimeException("AEViewerServlet must be started with BlackboardServletComponent");
    }
  }


  public void doGet(HttpServletRequest request, HttpServletResponse response) {
    String uid = request.getParameter("uid");
    //System.out.println("\n\nAEViewer got request " + uid);
    response.setContentType("text/html");

    try {
      PrintWriter out = response.getWriter();

      /* Policy change request */
      if (uid != null) {
	changePolicy(request, out, uid);
      } else {
	/* send adaptivity objects */
	sendData(out);
      }
      out.close();
    } catch (java.io.IOException ie) { ie.printStackTrace(); }
  }

  private void changePolicy(HttpServletRequest request, PrintWriter out, String uid) {

    // get the string representing the policy
    String policyString = request.getParameter("kernel");
    StringReader reader = new StringReader(policyString);
    OperatingModePolicy[] policies = null;
    try {
      // Use the parser to create a new policy
      Parser parser = new Parser(reader, support.getLog());
      policies = parser.parseOperatingModePolicies();
    } catch (java.io.IOException ioe) {
      ioe.printStackTrace();
    } finally {
      reader.close();
    }

    // find the existing policy on the blackboard
    Collection blackboardCollection 
      = support.queryBlackboard(new UIDPredicate(uid));
    OperatingModePolicy bbPolicy = (OperatingModePolicy)blackboardCollection.iterator().next();

    // set the existing policy's kernel to be that of the newly
    // parsed policy
    bbPolicy.setPolicyKernel(policies[0].getPolicyKernel());
    
    BlackboardService blackboard = support.getBlackboardService();
    blackboard.openTransaction();
    // write the updated policy to the blackboard
    blackboard.publishChange(bbPolicy);
    blackboard.closeTransaction();
    
    out.println("<html><head></head><body><h1>Policy Changed</h1><br>" );
    out.println(bbPolicy.toString());
  }
  
  /**
   * Suck the Policies, Conditions, and Operating Modes out of the
   * blackboard and send them to the requestor
   */
  private void sendData(PrintWriter out) {
    out.println("<html><head></head><body><h1><CENTER>Conditions</CENTER></h1><br>" );
    Collection conditions = support.queryBlackboard(conditionPredicate);
    out.print("<UL>");
    for (Iterator it = conditions.iterator(); it.hasNext();) {
      out.print("<LI>");
      out.println(it.next().toString());
    }
    out.print("</UL>");
    
    out.println("<html><head></head><body><h1><CENTER>OperatingModes</CENTER></h1><br>" );
    Collection operatingModes = support.queryBlackboard(omPredicate);
    out.print("<UL>");
    for (Iterator it = operatingModes.iterator(); it.hasNext();) {
      out.println("<LI>");
      out.println(it.next().toString());
    }
    out.print("</UL>");
    
    out.print("<H1><CENTER>Operating Mode Policies</CENTER></H1><P>\n");
    
    out.print("<font size=+1>Edit Policies for <b>" + support.getAgentIdentifier() + "</b></font><p>\n");
  
    writePolicyTable(out);
  }


  /**
   * Create a HTML table with a form in each row for editing a policy
   */
  private void writePolicyTable(PrintWriter out) {
    out.println ("<table>\n");
    out.println ("<tr><th>");
    out.print("Name</th><th>Authority</th><th>UID</th><th>Kernel</th>");
    Collection policies = support.queryBlackboard(omPolicyPredicate);
    for (Iterator it = policies.iterator(); it.hasNext();) {
      
      out.print("<FORM METHOD=\"GET\" ACTION=\"/$");
      out.print(support.getEncodedAgentName());
      out.print(support.getPath());
      out.print("\">\n");
      
      OperatingModePolicy omp = (OperatingModePolicy) it.next();
      out.println ("<tr><td>");
      out.print(omp.getName());
      out.println("</td><td>");
      out.println(omp.getAuthority());
      out.println("</td><td>");
      out.println(omp.getUID());
      out.print("<INPUT TYPE=hidden NAME=uid VALUE=\"");
      out.print(omp.getUID());
      out.print("\"SIZE=20>");
      out.println("</td><td>");
      out.print("<INPUT TYPE=\"text\" NAME=kernel VALUE=\"");
      out.print(omp.getPolicyKernel().toString());
      out.print("\"SIZE=80>");
      out.println("</td><td>");
      out.print("<input type=submit value=\"Submit\">");
      out.println("</td></tr>");
      out.println("</form>");
    }
    out.println("</table></body></html>");	
  }
  private class UIDPredicate implements UnaryPredicate { 
    String uid;
    public UIDPredicate(String uidString) {
      uid = uidString;
    }
	
    public boolean execute(Object o) {
      if (o instanceof OperatingModePolicy) {
	OperatingModePolicy omp = (OperatingModePolicy) o;
	if (uid.equals(omp.getUID().toString())) {
	  return true;
	}
      }
      return false;
    }
  }
}
