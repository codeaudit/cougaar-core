/*
 * <copyright>
 *  Copyright 2002-2003 BBNT Solutions, LLC
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

package org.cougaar.core.logging;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Category;
import org.apache.log4j.Priority;
import org.cougaar.core.servlet.BaseServletComponent;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.log4j.DetailPriority;
import org.cougaar.util.log.log4j.ShoutPriority;

/**
 * Servlet which allows the client to view and modify
 * the logging levels.
 */
public class LoggingConfigServlet extends BaseServletComponent {

  protected String getPath() {
    return "/log";
  }

  protected Servlet createServlet() {
    return new MyServlet();
  }

  // servlet:

  private class MyServlet extends HttpServlet {

    public void doGet(
        HttpServletRequest req,
        HttpServletResponse res) throws IOException {

      String action = req.getParameter("action");
      String getlog = req.getParameter("getlog");
      String setlog = req.getParameter("setlog");
      String setlevel = req.getParameter("level");
      if (getlog != null) getlog = getlog.trim();
      if (setlog != null) setlog = setlog.trim();
      if (setlevel != null) setlevel = setlevel.trim();
      if ("".equals(getlog)) getlog = null;
      if ("".equals(setlog)) setlog = null;
      int isetlevel = 
        (setlevel != null ? 
         convertStringToInt(setlevel) :
         -1);

      res.setContentType("text/html");
      PrintWriter out = res.getWriter();

      out.print(
          "<html><head><title>"+
          "Logger Configuration"+
          "</title></head><body>\n"+
          "<h2>Logger Configuration</h2><p>");

      if (getlog != null && 
          "Get".equalsIgnoreCase(action)) { 
        // get the level
        Exception e = null;
        int l = -1;
        try {
          l = getLevel(getlog);
        } catch (Exception e2) {
          e = e2;
        }
        if (l >= 0 && e == null) {
          out.print(
              "Level for \""+getlog+"\" is "+
              convertIntToString(l));
        } else {
          out.print(
              "Unable to get logging level of \""+
              getlog+"\": ");
          if (e == null) {
            out.print("invalid name");
          } else {
            e.printStackTrace(out);
          }
        }
      } else if (
          setlog != null && 
          setlevel != null && 
          "Set".equalsIgnoreCase(action)) {
        // set the level
        Exception e = null;
        try {
          setLevel(setlog, isetlevel);
        } catch (Exception e2) {
          e = e2;
        }
        if (e == null) {
          out.print(
              "Set \""+setlog+"\" to "+
              convertIntToString(isetlevel));
        } else {
          out.print(
              "Unable to change logging level of \""+
              setlog+"\": ");
          e.printStackTrace(out);
        }
      } else {
        // usage
      }
      out.print(
          "<br><hr>\n"+
          "<form method=\"GET\" action=\""+
          req.getRequestURI()+
          "\">\n"+
          "<input type=\"RESET\" name=\"Clear\" value=\"Clear\">"+
          "<p>\n"+
          "Get the level for "+
          "<input type=\"text\" name=\"getlog\" size=\"50\""+
          (getlog != null ? (" value=\""+getlog+"\"") : "")+
          "/>"+
          "<input type=\"submit\" name=\"action\" value=\"Get\">"+
          "<p>\n"+
          "Set the level for "+
          "<input type=\"text\" name=\"setlog\" size=\"50\""+
          (setlog != null ? (" value=\""+setlog+"\"") : "")+
          "/> to "+
          "<select name=\"level\">"+
          "<option"+
          (isetlevel == Logger.DETAIL ? " selected" : "")+">DETAIL</option>"+
          "<option"+
          (isetlevel == Logger.DEBUG ? " selected" : "")+">DEBUG</option>"+
          "<option "+
          (isetlevel == Logger.INFO ? " selected" : "")+">INFO</option>"+
          "<option"+
          (isetlevel == Logger.WARN ? " selected" : "")+">WARN</option>"+
          "<option"+
          (isetlevel == Logger.ERROR ? " selected" : "")+">ERROR</option>"+
          "<option"+
          (isetlevel == Logger.SHOUT ? " selected" : "")+">SHOUT</option>"+
          "<option"+
          (isetlevel == Logger.FATAL ? " selected" : "")+">FATAL</option>"+
          "</select>"+
          "<input type=\"submit\" name=\"action\" value=\"Set\">\n"+
          "</form></body></html>\n");
      out.close();
    }
  }

  // logger utilities:

  private String convertIntToString(int level) {
    switch (level) {
      case Logger.DETAIL: return "DETAIL";
      case Logger.DEBUG: return "DEBUG";
      case Logger.INFO:  return "INFO";
      case Logger.WARN:  return "WARN";
      case Logger.ERROR: return "ERROR";
      case Logger.SHOUT: return "SHOUT";
      case Logger.FATAL: return "FATAL";
      default: return null;
    }
  }

  private int convertStringToInt(String s) {
    if (s == null) {
      return -1;
    } else if (s.equalsIgnoreCase("DETAIL")) {
      return Logger.DETAIL;
    } else if (s.equalsIgnoreCase("DEBUG")) {
      return Logger.DEBUG;
    } else if (s.equalsIgnoreCase("INFO")) {
      return Logger.INFO;
    } else if (s.equalsIgnoreCase("WARN")) {
      return Logger.WARN;
    } else if (s.equalsIgnoreCase("ERROR")) {
      return Logger.ERROR;
    } else if (s.equalsIgnoreCase("SHOUT")) {
      return Logger.SHOUT;
    } else if (s.equalsIgnoreCase("FATAL")) {
      return Logger.FATAL;
    } else {
      return -1;
    }
  }

  // log4j utilities
  //
  // these should be moved to "org.cougaar.util.log"!

  // okay public api:
  private int getLevel(String name) {
    Category cat = getCategory(name);
    return getLevel(cat);
  }

  // okay public api:
  private void setLevel(String name, int level) {
    Category cat = getCategory(name);
    setLevel(cat, level);
  }

  // hack:
  static final Priority SHOUT = 
    ShoutPriority.toPriority("SHOUT", null);

  // hack:
  static final Priority DETAIL = 
    DetailPriority.toPriority("DETAIL", null);

  // log4j private
  private Category getCategory(String name) {
    return
      (name != null ?
       ((name.equals("root") ||
         name.equals(""))?
        Category.getRoot() :
        Category.getInstance(name)) :
       null);
  }

  // log4j private
  private int getLevel(Category cat) {
    if (cat != null) {
      Priority p = cat.getChainedPriority();
      return convertPriorityToInt(p);
    } else {
      return -1;
    }
  }

  // log4j private
  private void setLevel(Category cat, int level) {
    if (cat != null) {
      Priority p = convertIntToPriority(level);
      cat.setPriority(p);
    } else {
      throw new RuntimeException("null category");
    }
  }

  // log4j private
  private Priority convertIntToPriority(int level) {
    switch (level) {
    case Logger.DETAIL : return DETAIL;
    case Logger.DEBUG : return Priority.DEBUG;
    case Logger.INFO  : return Priority.INFO;
    case Logger.WARN  : return Priority.WARN;
    case Logger.ERROR : return Priority.ERROR;
    case Logger.SHOUT : return SHOUT;
    case Logger.FATAL : return Priority.FATAL;
    default: return null;
    }
  }

  // log4j private
  private int convertPriorityToInt(Priority level) {
    switch (level.toInt()) {
      case Priority.DEBUG_INT:      return Logger.DEBUG;
      case Priority.INFO_INT :      return Logger.INFO;
      case Priority.WARN_INT :      return Logger.WARN;
      case Priority.ERROR_INT:      return Logger.ERROR;
      case Priority.FATAL_INT:      return Logger.FATAL;
      default: 
        if (level.equals(SHOUT)) {
          return Logger.SHOUT;
        } else if (level.equals(DETAIL)) {
	  return Logger.DETAIL;
	}
        return 0;
    }
  }

}
