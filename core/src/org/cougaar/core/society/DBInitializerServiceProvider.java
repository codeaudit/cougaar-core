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
  public static final String QUERY_FILE = "DBInitializer.q";

  private DBProperties dbp;
  private String database;
  private String username;
  private String password;
  private Map substitutions = new HashMap();
  private PrintWriter log;

  /**
   * Constructor creates a DBProperties object from the
   * DBInitializer.q query control file and sets up variables for referencing the database.
   * @param experimentId the identifier of the experiment. Used in the
   * "queryExperiment" to extract the societyId, laydownId, and
   * oplanIds.
   * @param node the name of this node used to extract the information
   * pertinent to this node.
   **/
  public DBInitializerServiceProvider(String trialId)
    throws SQLException, IOException
  {
    dbp = DBProperties.readQueryFile(QUERY_FILE);
    database = dbp.getProperty("database");
    username = dbp.getProperty("username");
    password = dbp.getProperty("password");
    substitutions.put(":trial_id:", trialId);
//      log = new PrintWriter(new FileWriter("DBInitializerServiceProviderQuery.log"));
    try {
      String dbtype = dbp.getDBType();
      String driverParam = "driver." + dbtype;
      String driverClass = Parameters.findParameter(driverParam);
      if (driverClass == null) {
        // this is likely a "cougaar.rc" problem.
        // Parameters should be modified to help generate this exception:
        throw new SQLException(
            "Unable to find driver class for \""+
            driverParam+"\" -- check your \"cougaar.rc\"");
      }
      Class.forName(driverClass);
      Connection conn = DBConnectionPool.getConnection(database, username, password);
      try {
        Statement stmt = conn.createStatement();
        String query = dbp.getQuery("queryExperiment", substitutions);
        ResultSet rs = executeQuery(stmt, query);
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
        substitutions.put(":assemblyMatch:", assemblyMatch.toString());
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

  /**
   * Wrap a string in quotes. Does not double quotes within the string.
   **/
  private static String sqlQuote(String s) {
    if (s == null) return "null";
    return "'" + s + "'";
  }

  private ResultSet executeQuery(Statement stmt, String query) throws SQLException {
    try {
      long startTime = 0L;
      if (log != null)
        startTime = System.currentTimeMillis();
      ResultSet rs = stmt.executeQuery(query);
      if (log != null) {
        long endTime = System.currentTimeMillis();
        log.println((endTime - startTime) + " " + query);
      }
      return rs;
    } catch (SQLException sqle) {
      System.err.println("SQLException query: " + query);
      throw sqle;
    }
  }

  private class InitializerServiceImpl implements InitializerService {
    public ComponentDescription[]
      getComponentDescriptions(String parentName, String containerInsertionPoint)
      throws InitializerServiceException
    {
      if (parentName == null) throw new IllegalArgumentException("parentName cannot be null");
      // append a dot to containerInsertionPoint if not already there
      if (!containerInsertionPoint.endsWith(".")) containerInsertionPoint += ".";
      substitutions.put(":parent_name:", parentName);
      substitutions.put(":container_insertion_point:", containerInsertionPoint);
      try {
        Connection conn = DBConnectionPool.getConnection(database, username, password);
        try {
          Statement stmt = conn.createStatement();
          String query = dbp.getQuery("queryComponents",  substitutions);
          ResultSet rs = executeQuery(stmt, query);
          List componentDescriptions = new ArrayList();
          while (rs.next()) {
            String componentName = getNonNullString(rs, 1, query);
            String componentClass = getNonNullString(rs, 2, query);
            String componentId = getNonNullString(rs, 3, query);
            String insertionPoint = getNonNullString(rs, 4, query);
            Statement stmt2 = conn.createStatement();
            substitutions.put(":component_id:", componentId);
            String query2 = dbp.getQuery("queryComponentParams",  substitutions);
            ResultSet rs2 = executeQuery(stmt2, query2);
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
          if (false) {
            for (int i = 0; i < result.length; i++) {
              StringBuffer buf = new StringBuffer();
              buf.append(result[i].getInsertionPoint());
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
        e.printStackTrace();
        throw new InitializerServiceException(e);
      }
    }

    public String getAgentPrototype(String agentName)
      throws InitializerServiceException
    {
      substitutions.put(":agent_name:", agentName);
      try {
        Connection conn = DBConnectionPool.getConnection(database, username, password);
        try {
          Statement stmt = conn.createStatement();
          String query = dbp.getQuery("queryAgentPrototype",  substitutions);
          ResultSet rs = executeQuery(stmt, query);
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
      substitutions.put(":agent_name:", agentName);
      try {
        Connection conn = DBConnectionPool.getConnection(database, username, password);
        try {
          Statement stmt = conn.createStatement();
          String query = dbp.getQuery("queryAgentPGNames",  substitutions);
          ResultSet rs = executeQuery(stmt, query);
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
        substitutions.put(":agent_name:", agentName);
        substitutions.put(":pg_name:", pgName);
        try {
          Statement stmt = conn.createStatement();
          String query = dbp.getQuery("queryLibProperties",  substitutions);
          ResultSet rs = executeQuery(stmt, query);
          List result = new ArrayList();
          while (rs.next()) {
            String attributeName = getNonNullString(rs, 1, query);
            String attributeType = getNonNullString(rs, 2, query);
            boolean collection = !rs.getString(3).equals("SINGLE");
            Object attributeId = rs.getObject(4);
            Statement stmt2 = conn.createStatement();
            substitutions.put(":pg_attribute_id:", attributeId);
            String query2 = dbp.getQuery("queryAgentProperties",  substitutions);
            ResultSet rs2 = executeQuery(stmt2, query2);
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
        substitutions.put(":agent_name:", agentName);
        try {
          Statement stmt = conn.createStatement();
          String query = dbp.getQuery("queryAgentRelation",  substitutions);
          ResultSet rs = executeQuery(stmt, query);
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
          String[][] ary = (String[][]) result.toArray(new String[result.size()][]);
          if (false) {
            StringBuffer buf = new StringBuffer();
            buf.append(System.getProperty("line.separator"));
            for (int i = 0; i < ary.length; i++) {
              String[] ary2 = ary[i];
              buf.append("Relationship of ");
              buf.append(agentName);
              buf.append(": ");
              for (int j = 0; j < ary2.length; j++) {
                if (j > 0) buf.append('\t');
                buf.append(ary2[j]);
              }
              buf.append(System.getProperty("line.separator"));
            }
            System.out.println(buf);
          }
          return ary;
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
     * separated by semi-colon. Backslash can quote a character. The
     * query may be in a different database. If so, then the dbp
     * should contain properties named by concatenating the query
     * name with .database, .username, .password describing the
     * database to connect to.
     * @param type is the "data type" of the attribute value and
     * names a query that should be done to obtain the actual
     * value. 
     * @return a two-element array of attribute type and value.
     **/
    public Object[] translateAttributeValue(String type, String key)
      throws InitializerServiceException
    {
      substitutions.put(":key:", key);
      try {
        String db = dbp.getProperty(type + ".database", database);
        String un = dbp.getProperty(type + ".username", username);
        String pw = dbp.getProperty(type + ".password", password);
        Connection conn = DBConnectionPool.getConnection(db, un, pw);
        try {
          Statement stmt = conn.createStatement();
          String query = dbp.getQueryForDatabase(type, substitutions, type + ".database");
          ResultSet rs = executeQuery(stmt, query);
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
