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

import java.lang.reflect.Constructor;

import java.io.PrintWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StreamTokenizer;

import java.util.Comparator;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Iterator;
import java.util.TreeSet;

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

  public static final String OPERATINGMODE = "OperatingMode";
  public static final String POLICY = "Policy";
  public static final String CHANGE = "change";
  public static final String UID = "uid";
  public static final String NAME = "name";
  public static final String KERNEL = "kernel";
  public static final String VALUE = "value";

  private BlackboardServletSupport support;

  private OMComparator omComparator = new OMComparator();
  private OMPComparator ompComparator = new OMPComparator();

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
    String objectType = request.getParameter(CHANGE);
    //System.out.println("\n\nAEViewer got request " + objectType);
    response.setContentType("text/html");

    try {
      PrintWriter out = response.getWriter();

      if (objectType != null) {
	if (objectType.equals(POLICY)) {
	  changePolicy(request, out);
	} else if (objectType.equals(OPERATINGMODE)) {
	  changeOperatingMode(request, out);
	}
      } else {
	/* send adaptivity objects */
	sendData(out);
      }
      out.close();
    } catch (java.io.IOException ie) { ie.printStackTrace(); }
  }

  private void changePolicy(HttpServletRequest request, PrintWriter out) {

    String uid = request.getParameter(UID);

    // get the string representing the policy
    String policyString = request.getParameter(KERNEL);
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
    
    out.println("<html><head></head><body><h2>Policy Changed</h2><br>" );
    out.println(bbPolicy.toString());
  }

  private void changeOperatingMode(HttpServletRequest request, PrintWriter out) {

    // get the string representing the operating mode
    String omName = request.getParameter(NAME);
    // find the existing operating mode on the blackboard
    Collection blackboardCollection 
      = support.queryBlackboard(new OMByNamePredicate(omName));
    OperatingMode bbOM = (OperatingMode)blackboardCollection.iterator().next();

    String newValue = request.getParameter(VALUE);

    Class omClass = bbOM.getValue().getClass();

    // Is it a String?
    try {
      if (omClass == String.class) {
	// set it and be done with it.
	bbOM.setValue(newValue);
      } 
      else {
	// If not, hope that whatever it is has a String constructor
	Constructor cons = null;
	try {
	  cons = omClass.getConstructor( new Class[] {String.class});
	} catch (NoSuchMethodException nsme) {
	  System.err.println("AEViewerServlet: Error, no String constructor for OperatingMode containing class " + omClass + " " + nsme);
	  out.println("<html><head></head><body><h2>ERROR - OperatingMode Not Changed</h2><br>" );
	  out.println("There is no String constructor for OperatingMode containing class " +omClass + " " + nsme);
	  return;
	} catch (RuntimeException re) {
	  out.println("<html><head></head><body><h2>ERROR - OperatingMode Not Changed</h2><br>" );
	  out.println(re);
	  return;
	}
	
	if (cons != null) {
	  // Make a new one of whatever it is and set OM value
	  Comparable newThing = 
	    (Comparable) cons.newInstance(new String[] {newValue});
	  bbOM.setValue(newThing);
	} else {
	  out.println("<html><head></head><body><h2>ERROR - OperatingMode Not Changed</h2><br>" );
	  out.print("Can't set ");
	  out.print(bbOM.getName());
	  out.print("to " + newValue);
	  out.println("<br>No constructor " + omClass +"(String)");
	}
      }
    } catch (IllegalArgumentException iae) {
      out.println("<html><head></head><body><h2>ERROR - OperatingMode Not Changed</h2><br>" );
      out.print(newValue);
      out.print(" is not a valid value for ");
      out.println(bbOM.getName());
      out.print("<br>");
      out.println(iae);
      return;
    } catch (java.lang.reflect.InvocationTargetException ite) {
      out.println("<html><head></head><body><h2>ERROR - OperatingMode Not Changed</h2><br>" );
      out.print(newValue);
      out.print(" is not a valid value for ");
      out.println(bbOM.getName());
      out.print("<br>");
      out.println(ite);
      return;
    } catch (InstantiationException ie) {
      out.println("<html><head></head><body><h2>ERROR - OperatingMode Not Changed</h2><br>" );
      out.print(ie);
      return;
    } catch (IllegalAccessException iae) {
      out.println("<html><head></head><body><h2>ERROR - OperatingMode Not Changed</h2><br>" );
      out.print(iae);
      return;
    } catch (RuntimeException re) {
      out.println("<html><head></head><body><h2>ERROR - OperatingMode Not Changed</h2><br>" );
      out.print(re);
      return;
    }
    
    BlackboardService blackboard = support.getBlackboardService();
    blackboard.openTransaction();
    // write the updated operating mode to the blackboard
    blackboard.publishChange(bbOM);
    blackboard.closeTransaction();
    
    out.println("<html><head></head><body><h2>OperatingMode Changed</h2><br>" );
    out.println(bbOM.toString());
  }
  
  /**
   * Suck the Policies, Conditions, and Operating Modes out of the
   * blackboard and send them to the requestor
   */
  private void sendData(PrintWriter out) {
    out.println("<html><head></head><body>");

    writeAgentSelector(out);

    writeConditions(out);
    
    out.println("<h2><CENTER>Operating Modes</CENTER></h2>" );
    writeOMTable(out);

    out.print("<H2><CENTER>Operating Mode Policies</CENTER></H2><P>\n");
    writePolicyTable(out);
  }

  private void writeAgentSelector(PrintWriter out) {

    out.print(
	      "<script language=\"JavaScript\">\n"+
	      "<!--\n"+
	      "function mySubmit() {\n"+
	      "  var tidx = document.myForm.formCluster.selectedIndex\n"+
	      "  var cluster = document.myForm.formCluster.options[tidx].text\n"+
	      "  document.myForm.action=\"/$\"+cluster+\"");
    out.print(support.getPath());
    out.print("\"\n"+
	      "  return true\n"+
	      "}\n"+
	      "// -->\n"+
	      "</script>\n");
    out.print("<h2><center>Adaptivity Viewer at ");
    out.print(support.getEncodedAgentName());
    out.print(
	      "</center></h2>\n"+
	      "<form name=\"myForm\" method=\"get\" "+
	      "onSubmit=\"return mySubmit()\">\n"+
	      "Adaptivity Objects at "+
	      "<select name=\"formCluster\">\n");
    // lookup all known cluster names
    List names = support.getAllEncodedAgentNames();
    int sz = names.size();
    for (int i = 0; i < sz; i++) {
      String n = (String) names.get(i);
      out.print("  <option ");
      if (n.equals(support.getEncodedAgentName())) {
	out.print("selected ");
      }
      out.print("value=\"");
      out.print(n);
      out.print("\">");
      out.print(n);
      out.println("</option>");
    }
    out.print("</select>");
    out.println("<input type=submit name=\"formSubmit\" value=\"Reload\"><br>\n</form>");

  }

  /**
   * Creates a sorted HTML list of the conditions in the blackboard
   **/
  private void writeConditions(PrintWriter out) {
    out.println("<h2><CENTER>Conditions</CENTER></h2><br>" );
    Collection conditions = support.queryBlackboard(conditionPredicate);

    // Sort the Conditions. Is there a better way of doing this?
    TreeSet sortedConditions = new TreeSet();
    for (Iterator it = conditions.iterator(); it.hasNext();) {
      sortedConditions.add(it.next().toString());
    }
    out.print("<UL>");
    for (Iterator it = sortedConditions.iterator(); it.hasNext();) {
      out.print("<LI>");
      out.println(it.next().toString());
    }
    out.println("</UL>");
  }

  /**
   * Create a HTML table with a form in each row for editing a policy
   */
  private void writeOMTable(PrintWriter out) {
    out.println ("<table>");
    out.println("<tr><th>OperatingMode Name</th><th>Valid Values</th><th>Value</th></tr>");

    // Sort the OperatingModes
    List oms = (List) support.queryBlackboard(omPredicate);
    try {
      Collections.sort(oms, omComparator);
    } catch (ClassCastException e) {
      e.printStackTrace();
    }
				 
    for (Iterator it = oms.iterator(); it.hasNext();) {
      
      out.print("<FORM METHOD=\"GET\" ACTION=\"/$");
      out.print(support.getEncodedAgentName());
      out.print(support.getPath());
      out.println("\">");
      
      OperatingMode om = (OperatingMode) it.next();

      out.print("<td>");
      out.print(om.getName());
      out.println("</td>");

      out.print("<td>");
      out.print(om.getAllowedValues().toString());
      out.println("</td>");

      out.print("<td>");
      out.print("<INPUT TYPE=text NAME=");
      out.print(VALUE);
      out.print(" VALUE=\"");
      out.print(om.getValue().toString());
      out.print("\"SIZE=20>");
      out.println("</td>");

      out.print("<td> <INPUT TYPE=submit value=\"Submit\"> </td>");

      out.print("<tr> <td><INPUT TYPE=hidden NAME=");
      out.print(CHANGE);
      out.print(" VALUE=\"");
      out.print(OPERATINGMODE);
      out.println("\"</td>");

      out.print("<td>");
      out.print("<INPUT TYPE=hidden NAME=");
      out.print(NAME);
      out.print(" VALUE=\"");
      out.print(om.getName());
      out.println("\"> </td> </tr>");
      out.println("</form>");
    }
    out.println("</table>");	
  }


  /**
   * Create a HTML table with a form in each row for editing a policy
   */
  private void writePolicyTable(PrintWriter out) {
    out.println ("<table>\n");
    out.println("<tr><th>Name</th><th>Authority</th><th>UID</th><th>Kernel</th></tr>");

    // Sort the OperatingModePolicies
    List policies = (List) support.queryBlackboard(omPolicyPredicate);
    try {
      Collections.sort(policies, ompComparator);
    } catch (ClassCastException e) {
      e.printStackTrace();
    }

    for (Iterator it = policies.iterator(); it.hasNext();) {
      
      out.print("<FORM METHOD=\"GET\" ACTION=\"/$");
      out.print(support.getEncodedAgentName());
      out.print(support.getPath());
      out.print("\">\n");
      
      OperatingModePolicy omp = (OperatingModePolicy) it.next();
      out.println ("<tr> <td>");
      out.print(omp.getName());
      out.println("</td><td>");
      out.println(omp.getAuthority());
      out.println("</td><td>");
      out.print(omp.getUID());
      out.println("</td>");

      out.print("<td> <INPUT TYPE=\"text\" NAME=");
      out.print(KERNEL);
      out.print(" VALUE=\"");
      out.print(omp.getPolicyKernel().toString());
      out.print(";\"SIZE=80> </td>");

      out.println("<td> <input type=submit value=\"Submit\"></td></tr>");

      out.print("<td> <INPUT TYPE=hidden NAME=");
      out.print(CHANGE);
      out.print(" VALUE=\"");
      out.print(POLICY);
      out.println("\" <td>");

      out.print("<td> <INPUT TYPE=hidden NAME=");
      out.print(UID);
      out.print(" VALUE=\"");
      out.print(omp.getUID());
      out.print("\" </td>");

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

  private class OMByNamePredicate implements UnaryPredicate { 
    String name;
    public OMByNamePredicate(String omName) {
      name = omName;
    }
	
    public boolean execute(Object o) {
      if (o instanceof OperatingMode) {
	OperatingMode om = (OperatingMode) o;
	if (name.equals(om.getName())) {
	  return true;
	}
      }
      return false;
    }
  }

  private class OMComparator implements Comparator {

    // alphabetical sort
    public int compare(Object o1, Object o2) {
      if ((o1 instanceof OperatingMode) &&
	  (o2 instanceof OperatingMode)) {
	OperatingMode om1 = (OperatingMode) o1;
	OperatingMode om2 = (OperatingMode) o2;
	
	String om1Name = om1.getName();
	String om2Name = om2.getName();
      
	return om1Name.compareTo(om2Name);
      } 
      throw new ClassCastException("Expecting OperatingMode");
    }

    public boolean equals(Object other) {
      if (other instanceof OMComparator) {
	return true;
      }
      return false;
    }
  }


  private class OMPComparator implements Comparator {

    // alphabetical sort
    public int compare(Object o1, Object o2) {
      if ((o1 instanceof OperatingModePolicy) &&
	  (o2 instanceof OperatingModePolicy)) {
	OperatingModePolicy omp1 = (OperatingModePolicy) o1;
	OperatingModePolicy omp2 = (OperatingModePolicy) o2;
	
	String omp1Name = omp1.getName();
	String omp2Name = omp2.getName();
      
	return omp1Name.compareTo(omp2Name);
      } 
      throw new ClassCastException("Expecting OperatingModePolicy");
    }

    public boolean equals(Object other) {
      if (other instanceof OMComparator) {
	return true;
      }
      return false;
    }
  }
}

