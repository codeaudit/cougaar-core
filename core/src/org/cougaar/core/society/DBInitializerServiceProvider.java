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

package org.cougaar.core.society;

import java.io.IOException;
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
import org.cougaar.util.DBProperties;
import org.cougaar.util.DBConnectionPool;
import org.cougaar.util.Parameters;
import org.cougaar.core.component.Service;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ComponentDescription;

public class DBInitializerServiceProvider implements ServiceProvider {
  public static final String DATABASE = "org.cougaar.configuration.database";
  public static final String QUERY_FILE = "DBInitializer.q";
  public static final String NODE_COMPONENT_TYPE = "NODE";
  public static final String AGENT_COMPONENT_TYPE = "AGENT";
  public static final String EXPERIMENT_QUERY = "queryExperiment";
  public static final String AGENT_NAMES_QUERY = "queryAgentNames";
  public static final String AGENT_PROTOTYPE_QUERY = "queryAgentPrototype";
  public static final String PLUGIN_NAMES_QUERY = "queryPluginNames";
  public static final String PLUGIN_PARAMS_QUERY = "queryPluginParams";
  public static final String AGENT_PG_NAMES_QUERY = "queryAgentPGNames";
  public static final String LIB_PROPERTIES_QUERY = "queryLibProperties";
  public static final String AGENT_PROPERTIES_QUERY = "queryAgentProperties";
  public static final String AGENT_RELATION_QUERY = "queryAgentRelation";
  public static final String AGENT_INSERTION_POINT = "Node.AgentManager.Agent";
  public static final String PLUGIN_INSERTION_POINT =
    "Node.AgentManager.Agent.PluginManager.Plugin";

  private DBProperties dbp;
  private String database;
  private String username;
  private String password;
  private Map substitutions = new HashMap();

  /**
   * Constructor creates a DBProperties object from the
   * DBInitializer.q query control file and sets up variables for referencing the database.
   * @param experimentId the identifier of the experiment. Used in the
   * EXPERIMENT_QUERY to extract the societyId, laydownId, and
   * oplanIds.
   * @param node the name of this node used to extract the information
   * pertinent to this node.
   **/
  public DBInitializerServiceProvider(String experimentId, String node)
    throws SQLException, IOException
  {
    dbp = DBProperties.readQueryFile(DATABASE, QUERY_FILE);
    dbp.enableDebug(true);
    database = dbp.getProperty("database");
    username = dbp.getProperty("username");
    password = dbp.getProperty("password");
    substitutions.put(":expt_id", experimentId);
    substitutions.put(":node_name", node);
    try {
      String dbtype = dbp.getDBType();
      String driverParam = "driver." + dbtype;
      String driverClass = Parameters.findParameter(driverParam);
      if (driverClass == null)
        throw new SQLException("Unknown driver " + driverParam);
      Class.forName(driverClass);
      Connection conn = DBConnectionPool.getConnection(database, username, password);
      try {
        Statement stmt = conn.createStatement();
        String query = dbp.getQuery(EXPERIMENT_QUERY, substitutions);
        ResultSet rs = stmt.executeQuery(query);
        String societyId;
        String laydownId;
        if (rs.next()) {
          societyId = getNonNullString(rs, 1, query);
          laydownId = getNonNullString(rs, 2, query);
          if (rs.next())
            throw new SQLException("Multiple matches for experimentId: " + experimentId);
        } else {
          throw new SQLException("No match for experimentId: " + experimentId);
        }
        substitutions.put(":laydown_id", laydownId);
        substitutions.put(":society_id", societyId);
        rs.close();
        stmt.close();
      } finally {
        conn.close();
      }
    } catch (ClassNotFoundException e) {
      throw new SQLException("Driver not found for " + database);
    }
  }

  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    if (serviceClass != InitializerService.class)
      throw new IllegalArgumentException(getClass() + " does not furnish "
                                         + serviceClass);
    return new InitializerServiceImpl();
  }

  public void releaseService(ServiceBroker sb, Object requestor,
                             Class serviceClass, Object service)
  {
  }

  private static String getNonNullString(ResultSet rs, int ix, String query)
    throws SQLException
  {
    String result = rs.getString(ix);
    if (result == null)
      throw new RuntimeException("Null in DB ix=" + ix + " query=" + query);
    return result;
  }

  private class InitializerServiceImpl implements InitializerService {
    public ComponentDescription[] getAgentDescriptions(String nodeName)
      throws InitializerServiceException
    {
      try {
        Connection conn = DBConnectionPool.getConnection(database, username, password);
        try {
          Statement stmt = conn.createStatement();
          String query = dbp.getQuery(AGENT_NAMES_QUERY, substitutions);
          ResultSet rs = stmt.executeQuery(query);
          List componentDescriptions = new ArrayList();
          while (rs.next()) {
            String agentName = getNonNullString(rs, 1, query);
            Vector vParams = new Vector(1);
            vParams.add(agentName);
            ComponentDescription cd =
              new ComponentDescription("org.cougaar.core.cluster.ClusterImpl",
                                       AGENT_INSERTION_POINT,
                                       "org.cougaar.core.cluster.ClusterImpl",
                                       null,  // codebase
                                       vParams,
                                       null,  // certificate
                                       null,  // lease
                                       null); // policy
            componentDescriptions.add(cd);
          }
          int len = componentDescriptions.size();
          ComponentDescription[] result = new ComponentDescription[len];
          return (ComponentDescription[])
            componentDescriptions.toArray(result);
        } finally {
          conn.close();
        }
      } catch (Exception e) {
        e.printStackTrace();
        throw new InitializerServiceException(e);
      }
    }

    public ComponentDescription[] getPluginDescriptions(String agentName)
      throws InitializerServiceException
    {
      try {
        Connection conn = DBConnectionPool.getConnection(database, username, password);
        try {
          Statement stmt = conn.createStatement();
          substitutions.put(":agent_name", agentName);
          String query = dbp.getQuery(PLUGIN_NAMES_QUERY, substitutions);
          ResultSet rs = stmt.executeQuery(query);
          List componentDescriptions = new ArrayList();
          while (rs.next()) {
            String className = getNonNullString(rs, 1, query);
            String pluginId = getNonNullString(rs, 2, query);
            Statement stmt2 = conn.createStatement();
            substitutions.put(":agent_component_id", pluginId);
            String query2 = dbp.getQuery(PLUGIN_PARAMS_QUERY, substitutions);
            System.out.print("plugin=" + className + "(");
            ResultSet rs2 = stmt2.executeQuery(query2);
            Vector vParams = new Vector();
            boolean first = true;
            while (rs2.next()) {
              String param = getNonNullString(rs2, 1, query2);
              if (first) {
                first = false;
              } else {
                System.out.print(", ");
              }
              System.out.print(param);
              vParams.addElement(param);
            }
            System.out.println(")");
            rs2.close();
            stmt2.close();
            ComponentDescription cd =
              new ComponentDescription(className,
                                       PLUGIN_INSERTION_POINT,
                                       className,
                                       null,  // codebase
                                       vParams,
                                       null,  // certificate
                                       null,  // lease
                                       null); // policy
            componentDescriptions.add(cd);
          }
          rs.close();
          stmt.close();
          int len = componentDescriptions.size();
          ComponentDescription[] result = new ComponentDescription[len];
          return (ComponentDescription[]) componentDescriptions.toArray(result);
        } finally {
          conn.close();
        }
      } catch (Exception e) {
        throw new InitializerServiceException(e);
      }
    }

    public String getAgentPrototype(String agentName)
      throws InitializerServiceException
    {
      substitutions.put(":agent_name", agentName);
      try {
        Connection conn = DBConnectionPool.getConnection(database, username, password);
        try {
          Statement stmt = conn.createStatement();
          String query = dbp.getQuery(AGENT_PROTOTYPE_QUERY, substitutions);
          ResultSet rs = stmt.executeQuery(query);
          if (rs.next()) {
            String result = getNonNullString(rs, 1, query);
            if (rs.next())
              throw new InitializerServiceException("Multiple prototypes for " + agentName);
            return result;
          }
          throw new InitializerServiceException("No prototype for " + agentName);
        } finally {
          conn.close();
        }
      } catch (SQLException sqle) {
        throw new InitializerServiceException(sqle);
      }
    }

    public String[] getAgentPropertyGroupNames(String agentName)
      throws InitializerServiceException
    {
      substitutions.put(":agent_name", agentName);
      try {
        Connection conn = DBConnectionPool.getConnection(database, username, password);
        try {
          Statement stmt = conn.createStatement();
          String query = dbp.getQuery(AGENT_PG_NAMES_QUERY, substitutions);
          ResultSet rs = stmt.executeQuery(query);
          List result = new ArrayList();
          while (rs.next()) {
            result.add(getNonNullString(rs, 1, query));
          }
          rs.close();
          stmt.close();
          return (String[]) result.toArray(new String[result.size()]);
        } finally {
          conn.close();
        }
      } catch (SQLException e) {
        throw new InitializerServiceException(e);
      }
    }

    /**
     * Return values for all properties of a property group as an
     * array of Object arrays. For each property an array of Objects
     * has the property's name, type, and value or array of values.
     * All non-array objects are Strings. If the value is an array it
     * is an array of Strings
     **/
    public Object[][] getAgentProperties(String agentName, String pgName)
      throws InitializerServiceException
    {
      try {
        Connection conn = DBConnectionPool.getConnection(database, username, password);
        substitutions.put(":agent_name", agentName);
        substitutions.put(":pg_name", pgName);
        try {
          Statement stmt = conn.createStatement();
          String query = dbp.getQuery(LIB_PROPERTIES_QUERY, substitutions);
          ResultSet rs = stmt.executeQuery(query);
          List result = new ArrayList();
          while (rs.next()) {
            String attributeName = getNonNullString(rs, 1, query);
            String attributeType = getNonNullString(rs, 2, query);
            boolean collection = rs.getBoolean(3);
            Object attributeId = rs.getObject(4);
            Statement stmt2 = conn.createStatement();
            substitutions.put(":pg_attribute_id", attributeId);
            String query2 = dbp.getQuery(AGENT_PROPERTIES_QUERY, substitutions);
            ResultSet rs2 = stmt2.executeQuery(query2);
            Object value;
            if (collection) {
              List values = new ArrayList();
              while (rs2.next()) {
                String v = getNonNullString(rs2, 1, query2);
                values.add(v);
              }
              value = values.toArray(new String[values.size()]);
            } else if (rs2.next()) {
              value = getNonNullString(rs2, 1, query2);
              if (rs2.next())
                throw new InitializerServiceException("Multiple values for "
                                                      + attributeId);
            } else {
              throw new InitializerServiceException("No value for "
                                                    + attributeId);
            }
            Object[] e = {attributeName, attributeType, value};
            result.add(e);
            rs2.close();
            stmt2.close();
          }
          rs.close();
          stmt.close();
          return (Object[][]) result.toArray(new Object[result.size()][]);
        } finally {
          conn.close();
        }
      } catch (SQLException e) {
        throw new InitializerServiceException(e);
      }
    }

    /**
     * Get the relationships of an agent. Each relationship is
     * represented by a 6-tuple of the roleName, itemId, typeId,
     * otherAgentId, start time, and end time.
     **/
    public String[][] getAgentRelationships(String agentName)
      throws InitializerServiceException
    {
      try {
        Connection conn = DBConnectionPool.getConnection(database, username, password);
        substitutions.put(":agent_name", agentName);
        try {
          Statement stmt = conn.createStatement();
          String query = dbp.getQuery(AGENT_RELATION_QUERY, substitutions);
          ResultSet rs = stmt.executeQuery(query);
          List result = new ArrayList();
          while (rs.next()) {
            String[] v = {
              getNonNullString(rs, 1, query),
              getNonNullString(rs, 2, query),
              getNonNullString(rs, 3, query),
              getNonNullString(rs, 4, query),
              rs.getString(5),
              rs.getString(6),
            };
            result.add(v);
          }
          rs.close();
          stmt.close();
          return (String[][]) result.toArray(new String[result.size()][]);
        } finally {
          conn.close();
        }
      } catch (SQLException e) {
        throw new InitializerServiceException(e);
      }
    }
  }
}
