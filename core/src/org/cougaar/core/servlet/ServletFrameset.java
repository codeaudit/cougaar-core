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

package org.cougaar.core.servlet;

import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.service.ServletService;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.PrintWriter;


abstract public class ServletFrameset extends HttpServlet
{

    // url paramter
    private static final String FRAME="frame";

    // values for the FRAME url parameter
    private static final String DATA_FRAME="data";
    private static final String BOTTOM_FRAME="bottom";
    private static final String REFRESH_FIELD_PARAM = "refresh";

    private int refreshSeconds; // remember the latest value
    private MessageAddress nodeAddr;


    public ServletFrameset(ServiceBroker sb) 
    {
	NodeIdentificationService nis = (NodeIdentificationService)
	    sb.getService(this, NodeIdentificationService.class, null);
	nodeAddr = nis.getMessageAddress();

	// Register our servlet.
	ServletService servletService = (ServletService)
	    sb.getService(this, ServletService.class, null);
	if (servletService == null) {
	    throw new RuntimeException("Unable to obtain ServletService");
	}
	try {
	    servletService.register(getPath(), this);
	} catch (Exception e) {
	    throw new RuntimeException("Unable to register servlet at path <"
				       +getPath()+ ">: " +e.getMessage());
	}


    }


    abstract public String getTitle();
    abstract public String getPath();
    abstract public void printPage(PrintWriter out);

    // Not abstract, but a no-op by default
    public void printBottomPage(PrintWriter out)
    {
    }

    public int topPercentage() 
    {
	return 10;
    }

    public int dataPercentage() 
    {
	return 80;
    }

    public int bottomPercentage() 
    {
	return 10;
    }

    public MessageAddress getNodeID() 
    {
	return nodeAddr;
    }

    private void printRefreshForm(HttpServletRequest request,
				  PrintWriter out)
    {
	out.print("<table><tr>");
	out.print("<td valign=\"middle\">Refresh (in seconds):</td>");
	out.print("<td valign=\"middle\"><input type=\"text\" size=3 name=\"");
	out.print(REFRESH_FIELD_PARAM);
	out.print("\"");
	out.print(" value=\"");
	out.print(refreshSeconds);
	out.print("\"");
	out.print("></td>");

	// refesh button
	out.print("<td valign=\"middle\"><input type=\"submit\" name=\"action\" value=\"Refresh\"></td>");

	out.print("</tr></table>");
    }


    private void writeJavascript(PrintWriter out) 
    {
	out.print(
		  "<script language=\"JavaScript\">\n"+
		  "<!--\n"+
		  "function mySubmit() {\n"+
		  "  var obj = top.agentFrame.document.agent.name;\n"+
		  "  var encAgent = obj.value;\n"+
		  "  if (encAgent.charAt(0) == '.') {\n"+
		  "    alert(\"Please select an agent name\")\n"+
		  "    return false;\n"+
		  "  }\n"+
		  "  document.myForm.target=\""+DATA_FRAME+"\"\n"+
		  "  document.myForm.action=\"/$\"+encAgent+\""+
		  getPath()+"\"\n"+
		  "  return true\n"+
		  "}\n"+
		  "// -->\n"+
		  "</script>\n");
    }

    private void printBottomFrame(HttpServletRequest request,
				  PrintWriter out)
    {
	out.print("<html><HEAD>");


        // write javascript
        writeJavascript(out);

        // begin form
        out.print(
		  "<form name=\"myForm\" method=\"get\" "+
		  "onSubmit=\"return mySubmit()\">\n"+
		  "<input type=hidden name=\""+FRAME+
		  "\" value=\""+DATA_FRAME+"\">\n");

	printRefreshForm(request, out);

        // end form
	out.print("</form>");

	printBottomPage(out);

	out.print("</body></html>\n");
    }


    private void printDataFrame(HttpServletRequest request,
				PrintWriter out)
    {
	out.print("<html><HEAD>");

	if (refreshSeconds > 0 ) {
	    out.print("<META HTTP-EQUIV=\"refresh\" content=\"");
	    out.print(refreshSeconds);
	    out.print("\">");
	}
 
	out.print("<TITLE>");
	out.print(getTitle());
	out.print("</TITLE></HEAD><body><H1>");
	out.print(getTitle());
	out.print("</H1>");

        out.print("Node: "+nodeAddr+"<br>");
	out.print("Date: ");
	out.print(new java.util.Date());
	
	printPage(out);
	out.print("</body></html>\n");
    }


    private void printOuterFrame(HttpServletRequest request,
				 PrintWriter out)
    {    
	// Header
	out.print("<html><head><title>");
	out.print(getTitle());
	out.print("</title></head>");

	// Frameset
	out.print("<frameset rows=\"");
	out.print(topPercentage());
	out.print("%,");
	out.print(dataPercentage());
	out.print("%,");
	out.print(bottomPercentage());
	out.print("%\">\n");
	
	// Top frame.  Assume there's a node here, at least.
	out.print("<frame src=\"/agents?format=select&suffix=");
	out.print(nodeAddr);
	out.print("\" name=\"agentFrame\">\n");

	// Data frame
	out.print("<frame src=\"");
	out.print(request.getRequestURI());
	out.print("?");
	out.print(FRAME);
	out.print("=");
	out.print(DATA_FRAME);
	out.print("\" name=\"");
	out.print(DATA_FRAME);
	out.print("\">\n");

	// Bottom
	out.print("<frame src=\"");
	out.print(request.getRequestURI());
	out.print("?");
	out.print(FRAME);
	out.print("=");
	out.print(BOTTOM_FRAME);
	out.print("\" name=\"");
	out.print(BOTTOM_FRAME);
	out.print("\">\n");

	// End frameset
	out.print("</frameset>\n");

	// Frameless browser hack
	out.print("<noframes>Please enable frame support</noframes>");

	// End
	out.print("</html>\n");
    }




    public void doGet(HttpServletRequest request,
		      HttpServletResponse response) 
	throws java.io.IOException 
    {
	String frame = request.getParameter(FRAME);

	// get the refresh value and make this page refresh
	String refresh  = request.getParameter(REFRESH_FIELD_PARAM);
        if (refresh != null) {
	    try {
		refreshSeconds = Integer.parseInt(refresh);
	    }
	    catch (Exception e) {
	    }
	}

	response.setContentType("text/html");
        try {
	    PrintWriter out = response.getWriter();
            if (DATA_FRAME.equals(frame)) {
		printDataFrame(request, out);
            } else if (BOTTOM_FRAME.equals(frame)) {
		printBottomFrame(request, out);
            } else {
		printOuterFrame(request, out);
	    }
	    out.close();
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

}
