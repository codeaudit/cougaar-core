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

package org.cougaar.core.node;

import org.cougaar.core.mts.*;

import java.io.IOException;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.Connection;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import java.util.Collection;
import java.util.Collections;
import org.cougaar.util.log.Logger;
import org.cougaar.util.log.Logging;
import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ComponentDescription;

/**
 * Implementation of ComponentInitializerServiceProvider that reads
 * initialization information from a database.
 **/
class DBComponentInitializerServiceProvider implements ServiceProvider {

  private final DBInitializerService dbInit;
  private final Logger logger;

  public DBComponentInitializerServiceProvider(DBInitializerService dbInit) {
    this.dbInit = dbInit;
    this.logger = Logging.getLogger(getClass());
  }

  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    if (serviceClass != ComponentInitializerService.class) {
      throw new IllegalArgumentException(
          getClass()+" does not furnish "+serviceClass);
    }
    return new ComponentInitializerServiceImpl();
  }

  public void releaseService(ServiceBroker sb, Object requestor,
                             Class serviceClass, Object service)
  {
  }

  private class ComponentInitializerServiceImpl implements ComponentInitializerService {
    // Remember, this returns only items strictly _below_ the given 
    // insertion point, listed as children of the given component.
    public ComponentDescription[] getComponentDescriptions(
        String parentName, String containerInsertionPoint) throws InitializerException
    {
      if (logger.isDebugEnabled()) {
        logger.debug("In getComponentDescriptions");
      }
      if (parentName == null) throw new IllegalArgumentException("parentName cannot be null");
      // append a dot to containerInsertionPoint if not already there
      if (!containerInsertionPoint.endsWith(".")) containerInsertionPoint += ".";
      Map substitutions = dbInit.createSubstitutions();
      substitutions.put(":parent_name:", parentName);
      substitutions.put(":container_insertion_point:", containerInsertionPoint);
      if (logger.isInfoEnabled()) {
        logger.info(
            "Looking for direct sub-components of " + parentName +
            " just below insertion point " + containerInsertionPoint);
      }
      try {
        Connection conn = dbInit.getConnection();
        try {
          Statement stmt = conn.createStatement();
          String query = dbInit.getQuery("queryComponents",  substitutions);

          /*
             if (logger.isDebugEnabled()) {
             logger.debug("getComponentDescriptions doing query " + query);
             }
           */

          ResultSet rs = dbInit.executeQuery(stmt, query);
          List componentDescriptions = new ArrayList();
          while (rs.next()) {
            String componentName = dbInit.getNonNullString(rs, 1, query);
            String componentClass = dbInit.getNonNullString(rs, 2, query);
            String componentId = dbInit.getNonNullString(rs, 3, query);
            String insertionPoint = dbInit.getNonNullString(rs, 4, query);
            String priority = dbInit.getNonNullString(rs, 5, query);
            Statement stmt2 = conn.createStatement();
            substitutions.put(":component_id:", componentId);
            String query2 = dbInit.getQuery("queryComponentParams",  substitutions);
            ResultSet rs2 = dbInit.executeQuery(stmt2, query2);
            Vector vParams = null; // no parameters == NO PARAMETERS! (bug 1372)
            while (rs2.next()) {
              String param = dbInit.getNonNullString(rs2, 1, query2);
              if (!param.startsWith("PROP$")) { // CSMART private arg
                if (vParams == null) vParams = new Vector(); // lazy create
                vParams.addElement(param);
              }
            }
            ComponentDescription desc =
              new ComponentDescription(componentName,
                  insertionPoint,
                  componentClass,
                  null,  // codebase
                  vParams,
                  null,  // certificate
                  null,  // lease
                  null, // policy
                  ComponentDescription.parsePriority(priority));
            componentDescriptions.add(desc);
            rs2.close();
            stmt2.close();
          }
          int len = componentDescriptions.size();
          if (logger.isDebugEnabled()) {
            logger.debug("... returning " + len + " CDescriptions");
          }
          ComponentDescription[] result = new ComponentDescription[len];
          result = (ComponentDescription[])
            componentDescriptions.toArray(result);
          if (false) {
            for (int i = 0; i < result.length; i++) {
              StringBuffer buf = new StringBuffer();
              buf.append(result[i].getInsertionPoint());
              if(result[i].getPriority() != ComponentDescription.PRIORITY_STANDARD) {
                buf.append("(");
                buf.append(ComponentDescription.priorityToString(result[i].getPriority()));
                buf.append(") ");

              }
              buf.append("=");
              buf.append(result[i].getClassname());
              Vector params = (Vector) result[i].getParameter();
              int n = params.size();
              if (n > 0) {
                for (int j = 0; j < n; j++) {
                  if (j == 0)
                    buf.append("(");
                  else
                    buf.append(", ");
                  buf.append(params.elementAt(j));
                }
                buf.append(")");
              }
              System.out.println(buf);
            }
          }
          return result;
        } finally {
          conn.close();
        }
      } catch (Exception e) {
        throw new InitializerException(
            "getComponentDescriptions("+parentName+", "+containerInsertionPoint+")",
            e);
      }
    }
  }
}
