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

package org.cougaar.core.thread;

import java.io.PrintWriter;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



class TopServlet extends HttpServlet
{

    private ThreadStatusService service;

    // Higher times appear earlier in the list
    private Comparator comparator = new Comparator() {
	    public int compare(Object x, Object y) {
		ThreadStatusService.Record r = (ThreadStatusService.Record) x;
		ThreadStatusService.Record s = (ThreadStatusService.Record) y;
		if (r.elapsed == s.elapsed)
		    return 0;
		else if (r.elapsed < s.elapsed)
		    return 1;
		else
		    return -1;
	    }

	    public boolean equals(Object x) {
		return x == this;
	    }
	};

    public TopServlet(ThreadStatusService service) {
	this.service = service;
    }

    String getPath() {
	return "/threads/top";
    }

    String getTitle() {
	return "Threads";
    }


    private void printHeaders(PrintWriter out) {
	out.print("<tr>");
	out.print("<th align=left><b>State</b></th>");
	out.print("<th align=left><b>Time</b></th>");
	out.print("<th align=left><b>Level</b></th>");
	out.print("<th align=left><b>Thread</b></th>");
	out.print("<th align=left><b>Client</b></th>");
	out.print("</tr>");
    }

    private void printCell(String data, boolean italics, PrintWriter out) {
	out.print("<td>");
	if (italics) out.print("<i>");
	out.print(data);
	if (italics) out.print("</i>");
	out.print("</td>");
    }

    private void printCell(long data, boolean italics, PrintWriter out) {
	out.print("<td align=right>");
	if (italics) out.print("<i>");
	out.print(data);
	if (italics) out.print("</i>");
	out.print("</td>");
    }

    private void printRecord(ThreadStatusService.Record record,
			     PrintWriter out) 
    {
	boolean is_queued = record.getState() == ThreadStatusService.QUEUED;
	out.print("<tr>");
	printCell(record.getState(), is_queued, out);
	printCell(record.elapsed, is_queued, out);
	printCell(record.scheduler, is_queued, out);
	printCell(record.schedulable, is_queued, out);
	printCell(record.consumer, is_queued, out);
	out.print("</tr>");
    }

    void printPage(PrintWriter out) {
	List status = service.getStatus();
	if (status == null || status.size() == 0) {
	    out.print("<br><i>none</i>");
	    return;
	}

	// Sort the records by time
	java.util.Collections.sort(status, comparator);

	out.print("<table>");
	printHeaders(out);

	Iterator itr = status.iterator();
	while (itr.hasNext()) {
	    ThreadStatusService.Record record = (ThreadStatusService.Record)
		itr.next();
	    printRecord(record, out);
	}
	
	out.print("</table>");
    }

    public void doGet(HttpServletRequest request,
		      HttpServletResponse response) 
	throws java.io.IOException 
    {

	String refresh = request.getParameter("refresh");
	int refreshSeconds = 
	    ((refresh != null) ?
	     Integer.parseInt(refresh) :
	     0);

	response.setContentType("text/html");
	PrintWriter out = response.getWriter();

	out.print("<html><HEAD>");
	if (refreshSeconds > 0) {
	    out.print("<META HTTP-EQUIV=\"refresh\" content=\"");
	    out.print(refreshSeconds);
	    out.print("\">");
	}
	out.print("<TITLE>");
	out.print(getTitle());
	out.print("</TITLE></HEAD><body><H1>");
	out.print(getTitle());
	out.print("</H1>");

	out.print("Date: ");
	out.print(new Date());
	
	printPage(out);

	out.print("<hr>RefreshSeconds: ");	
	out.print(refreshSeconds);

	out.print("</body></html>\n");

	out.close();
    }
}
