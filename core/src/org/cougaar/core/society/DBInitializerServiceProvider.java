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
import org.cougaar.domain.planning.plugin.AssetDataReader;
import org.cougaar.domain.planning.plugin.AssetDataDBReader;

public class DBInitializerServiceProvider implements ServiceProvider {
  public static final String DATABASE = "org.cougaar.configuration.database";
  public static final String QUERY_FILE = "DBInitializer.q";
  public static final String NODE_COMPONENT_TYPE = "NODE";
  public static final String AGENT_COMPONENT_TYPE = "AGENT";
  public static final String QUERY_EXPERIMENT = "queryExperiment";
  public static final String QUERY_AGENT_NAMES = "queryAgentNames";
  public static final String QUERY_COMPONENTS = "queryComponents";
  public static final String QUERY_COMPONENT_PARAMS = "queryComponentParams";
  public static final String QUERY_AGENT_PROTOTYPE = "queryAgentPrototype";
  public static final String QUERY_PLUGIN_NAMES = "queryPluginNames";
  public static final String QUERY_PLUGIN_PARAMS = "queryPluginParams";
  public static final String QUERY_AGENT_PG_NAMES = "queryAgentPGNames";
  public static final String QUERY_LIB_PROPERTIES = "queryLibProperties";
  public static final String QUERY_AGENT_PROPERTIES = "queryAgentProperties";
  public static final String QUERY_AGENT_RELATION = "queryAgentRelation";
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
   * QUERY_EXPERIMENT to extract the societyId, laydownId, and
   * oplanIds.
   * @param node the name of this node used to extract the information
   * pertinent to this node.
   **/
  public DBInitializerServiceProvider(String trialId)
    throws SQLException, IOException
  {
    dbp = DBProperties.readQueryFile(DATABASE, QUERY_FILE);
//      dbp.setDebug(true);
    database = dbp.getProperty("database");
    username = dbp.getProperty("username");
    password = dbp.getProperty("password");
    substitutions.put(":trial_id", trialId);
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
        String query = dbp.getQuery(QUERY_EXPERIMENT, substitutions);
        ResultSet rs = stmt.executeQuery(query);
        boolean first = true;
        StringBuffer assemblyMatch = new StringBuffer();
        assemblyMatch.append("in (");
        while (rs.next()) {
          if (first) {
            first = false;
          } else {
            assemblyMatch.append(", ");
          }
          assemblyMatch.append("'");
          assemblyMatch.append(getNonNullString(rs, 1, query));
          assemblyMatch.append("'");
        }
        assemblyMatch.append(")");
        substitutions.put(":assemblyMatch", assemblyMatch.toString());
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
    public ComponentDescription[]
      getComponentDescriptions(String parentName, String insertionPoint)
      throws InitializerServiceException
    {
      substitutions.put(":parent_name", parentName);
      substitutions.put(":insertion_point", insertionPoint);
      try {
        Connection conn = DBConnectionPool.getConnection(database, username, password);
        try {
          Statement stmt = conn.createStatement();
          String query = dbp.getQuery(QUERY_COMPONENTS, substitutions);
          ResultSet rs = stmt.executeQuery(query);
          List componentDescriptions = new ArrayList();
          while (rs.next()) {
            String componentName = getNonNullString(rs, 1, query);
            String componentClass = getNonNullString(rs, 2, query);
            String componentId = getNonNullString(rs, 3, query);
            Statement stmt2 = conn.createStatement();
            substitutions.put(":component_id", componentId);
            String query2 = dbp.getQuery(QUERY_COMPONENT_PARAMS, substitutions);
            ResultSet rs2 = stmt2.executeQuery(query2);
            Vector vParams = new Vector();
            while (rs2.next()) {
              vParams.addElement(getNonNullString(rs2, 1, query2));
            }
            ComponentDescription desc =
              new ComponentDescription(componentName,
                                       insertionPoint,
                                       componentClass,
                                       null,  // codebase
                                       vParams,
                                       null,  // certificate
                                       null,  // lease
                                       null); // policy
            componentDescriptions.add(desc);
            rs2.close();
            stmt2.close();
          }
          int len = componentDescriptions.size();
          ComponentDescription[] result = new ComponentDescription[len];
          result = (ComponentDescription[])
            componentDescriptions.toArray(result);
//            for (int i = 0; i < result.length; i++) {
//              StringBuffer buf = new StringBuffer();
//              buf.append(result[i].getInsertionPoint());
//              buf.append("=");
//              buf.append(result[i].getClassname());
//              Vector params = (Vector) result[i].getParameter();
//              int n = params.size();
//              if (n > 0) {
//                for (int j = 0; j < n; j++) {
//                  if (j == 0)
//                    buf.append("(");
//                  else
//                    buf.append(", ");
//                  buf.append(params.elementAt(j));
//                }
//                buf.append(")");
//              }
//              System.out.println(buf);
//            }
          return result;
        } finally {
          conn.close();
        }
      } catch (Exception e) {
        e.printStackTrace();
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
          String query = dbp.getQuery(QUERY_AGENT_PROTOTYPE, substitutions);
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
          String query = dbp.getQuery(QUERY_AGENT_PG_NAMES, substitutions);
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
          String query = dbp.getQuery(QUERY_LIB_PROPERTIES, substitutions);
          ResultSet rs = stmt.executeQuery(query);
          List result = new ArrayList();
          while (rs.next()) {
            String attributeName = getNonNullString(rs, 1, query);
            String attributeType = getNonNullString(rs, 2, query);
            boolean collection = !rs.getString(3).equals("SINGLE");
            Object attributeId = rs.getObject(4);
            Statement stmt2 = conn.createStatement();
            substitutions.put(":pg_attribute_id", attributeId);
            String query2 = dbp.getQuery(QUERY_AGENT_PROPERTIES, substitutions);
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
              continue;         // Skip missing properties
//                throw new InitializerServiceException("No value for " + attributeId);
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
          String query = dbp.getQuery(QUERY_AGENT_RELATION, substitutions);
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

    public AssetDataReader getAssetDataReader() {
      return new AssetDataDBReader(InitializerServiceImpl.this);
    }

    /**
     * Translate the value of a "query" attribute type. The "key"
     * should be one or more query substitutions. Each substitution is
     * an equals separated key and value. Multiple substitutions are
     * separated by semi-colon. Backslash can quote a character
     * @return a two-element array of attribute type and value.
     **/
    public Object[] translateAttributeValue(String type, String key)
      throws InitializerServiceException
    {
//        StringBuffer buf = new StringBuffer();
//        boolean inKey = true;
//        String key = null;
//        String val = null;
//        try {
//          for (int i = 0, n = s.length(); i <= n; i++) {
//            char c = i == n ? ';' : s.charAt(i);
//            switch (c) {
//            case '\\':
//              buf.append(s.charAt(++i)); // Quote next character
//              break;
//            case '=':
//              key = buf.substring(0).trim();
//              buf.setLength(0);
//              if (key.length() == 0)
//                throw new InitializerServiceException("Bad key in " + s);
//              break;
//            case ';':
//              val = buf.substring(0).trim();
//              buf.setLength(0);
//              if (key == null) break;
//              substitutions.put(key, val);
//              break;
//            default:
//              buf.append(c);
//              break;
//            }
//          }
//        } catch (InitializerServiceException ise) {
//          throw ise;
//        } catch (Throwable t) {
//          throw new InitializerServiceException("Parse failure: " + s);
//        }
      substitutions.put(":key", key);
      try {
        Connection conn = DBConnectionPool.getConnection(database, username, password);
        try {
          Statement stmt = conn.createStatement();
          String query = dbp.getQuery(type, substitutions);
          ResultSet rs = stmt.executeQuery(query);
          Object[] result = new Object[2];
          if (rs.next()) {
            result[0] = rs.getString(1);
            result[1] = rs.getObject(2);
          }
          rs.close();
          stmt.close();
          return result;
        } finally {
          conn.close();
        }
      } catch (SQLException sqle) {
        throw new InitializerServiceException(sqle);
      }
    }
  }
}
