/*
 * <copyright>
 *  Copyright 1997-2001 Mobile Intelligence Corp
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

import java.util.*;
import java.io.*;
import java.io.StringReader;
import java.sql.*;

import org.cougaar.util.DBConnectionPool;
import org.cougaar.util.DBProperties;
import org.cougaar.util.Parameters;

import org.cougaar.util.ConfigFinder;

import org.xml.sax.XMLReader;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;

import javax.naming.*;
import javax.naming.directory.*;

/**
 * Static methods for retrieving Community configurations from XML file or
 * database.
 */
public class CommunityConfigUtils {

  private static final String QUERY_FILE = "DBInitializer.q";
  //private static Map communities = new HashMap();

  private static Collection loadCommunitiesFromFile(String fname) {
    try {
      XMLReader xr = new org.apache.xerces.parsers.SAXParser();
      SaxHandler myHandler = new SaxHandler();
      xr.setContentHandler(myHandler);
      InputSource is = new InputSource(new FileReader(fname));
      xr.parse(is);
      return myHandler.getCommunityConfigs();
    } catch (Exception ex) {
      System.out.println("Exception parsing Community XML definition, " + ex);
    }
    return new Vector();
  }

  /**
   * Get Collection of all CommunityConfig objects from XML file.  Uses
   * standard Cougaar config finder to locate XML file.
   */
  public static Collection getCommunityConfigsFromFile(String xmlFileName) {
    File communityFile = ConfigFinder.getInstance().locateFile(xmlFileName);
    if (communityFile != null) {
      return loadCommunitiesFromFile(communityFile.getAbsolutePath());
    } else {
      System.err.println("Error: Could not find file '" +
        xmlFileName + "' on config path");
    }
    return new Vector();
  }

  /**
   * Get Collection of CommunityConfig objects for named entity.  Uses
   * standard Cougaar config finder to locate XML file.
   */
  public static Collection getCommunityConfigsFromFile(String xmlFileName, String entityName) {
    Collection allCommunities = getCommunityConfigsFromFile(xmlFileName);
    Collection communitiesWithEntity = new Vector();
    for (Iterator it = allCommunities.iterator(); it.hasNext();) {
      CommunityConfig cc = (CommunityConfig)it.next();
      if (cc.hasEntity(entityName)) {
        communitiesWithEntity.add(cc);
      }
    }
    return communitiesWithEntity;
  }

  /**
   * Get Collection of CommunityConfig objects for named entity.
   * Uses standard cougaar.rc to connect to DB server.
   */
  public static Collection getCommunityConfigsFromDB(String entityName) {
    try {
      DBProperties dbp = DBProperties.readQueryFile(QUERY_FILE);
      String dbStyle = dbp.getDBType();
      checkDriverClass(dbStyle);
      String database = dbp.getProperty("database");
      String username = dbp.getProperty("username");
      String password = dbp.getProperty("password");
      Connection conn = DBConnectionPool.getConnection(database, username, password);
      return getParentCommunities(conn, entityName);
    } catch(Exception e) {
      e.printStackTrace();
    }
    return new Vector();
  }

  /**
   * Set up available JDBC driver
   */
  private static void checkDriverClass(String dbtype) throws SQLException, ClassNotFoundException {
    String driverParam = "driver." + dbtype;
    String driverClass = Parameters.findParameter(driverParam);
    if (driverClass == null) {
      // this is likely a "cougaar.rc" problem.
      // Parameters should be modified to help generate this exception:
      throw new SQLException("Unable to find driver class for \""+
                             driverParam+"\" -- check your \"cougaar.rc\"");
    }
    Class.forName(driverClass);
  }

  public static javax.naming.directory.Attributes getCommunityAttributes(Connection conn, String communityName)
    throws SQLException {
    Statement s = conn.createStatement();
    ResultSet rs = s.executeQuery("select * from community_attribute");
    javax.naming.directory.Attributes attrs = new BasicAttributes();
    while(rs.next()) {
      if (rs.getString(1).equals(communityName)) {
        String attrId = rs.getString(2);
        String attrValue = rs.getString(3);
        Attribute attr = attrs.get(attrId);
        if (attr == null) {
          attr = new BasicAttribute(attrId);
          attrs.put(attr);
        }
        if (!attr.contains(attrValue)) attr.add(attrValue);
      }
    }
    return attrs;
  }

  public static void addEntityAttribute(Map configMap, String communityName, String entityName,
    String attrId, String attrValue) {
    CommunityConfig cc = (CommunityConfig)configMap.get(communityName);
    EntityConfig entity = cc.getEntity(entityName);
    if (entity == null) {
      entity = new EntityConfig(entityName);
      cc.addEntity(entity);
    }
    entity.addAttribute(attrId, attrValue);
  }

  public static Collection getParentCommunities(Connection conn, String entityName)
    throws SQLException {

    Statement s = conn.createStatement();
    ResultSet rs = s.executeQuery("select * from community_entity_attribute");
    Map configMap = new HashMap();

    while(rs.next()) {
      if (rs.getString(2).equals(entityName)) {
        String communityName = rs.getString(1);
        if (!configMap.containsKey(communityName)) {
          CommunityConfig cc = new CommunityConfig(communityName);
          cc.setAttributes(getCommunityAttributes(conn, communityName));
          configMap.put(communityName, cc);
        }
        addEntityAttribute(configMap, communityName, entityName, rs.getString(3), rs.getString(4));
      }
    }
    return configMap.values();
  }

  /**
   * For testing.  Loads CommunityConfigs from XML File or database
   * and prints to screen.
   * @param args
   */
  public static void main(String args[]) {
    //System.setProperty("org.cougaar.config.path", "$CWD;$CWD/test/data;$INSTALL/configs/common");
    //Collection configs = getCommunityConfigsFromFile("communities.xml");
    Collection configs = getCommunityConfigsFromDB("OSC");
    System.out.println("<Communities>");
    for (Iterator it = configs.iterator(); it.hasNext();) {
      System.out.println(((CommunityConfig)it.next()).toString());
    }
    System.out.println("</Communities>");
  }

  /**
   * SAX Handler for parsing Community XML files
   */
  static class SaxHandler extends DefaultHandler {

    public SaxHandler () {}

    private Map communityMap = null;

    private CommunityConfig community = null;
    private EntityConfig entity = null;

    public void startDocument() {
      communityMap = new HashMap();
    }

    public Collection getCommunityConfigs() {
      return communityMap.values();
    }

    public void startElement(String uri, String localname, String rawname,
        Attributes p3) {
      try {
        if (localname.equals("Community")){
          String name = null;
          for (int i = 0; i < p3.getLength(); i++) {
            if (p3.getLocalName(i).equals("Name")) {
              name = p3.getValue(i).trim();
            }
          }
          community = new CommunityConfig(name);
        } else if (localname.equals("Entity")) {
          String name = null;
          for (int i = 0; i < p3.getLength(); i++) {
            if (p3.getLocalName(i).equals("Name")) {
              name = p3.getValue(i).trim();
            }
          }
          entity = new EntityConfig(name);
        } else if (localname.equals("Attribute")) {
          String id = null;
          String value = null;
          for (int i = 0; i < p3.getLength(); i++) {
            if (p3.getLocalName(i).equals("ID")) {
              id = p3.getValue(i).trim();
            } else if (p3.getLocalName(i).equals("Value")) {
              value = p3.getValue(i).trim();
            }
          }
          if (id != null && value != null) {
            if (entity != null)
              entity.addAttribute(id, value);
            else
              community.addAttribute(id, value);
          }
        }
      } catch (Exception ex ){
        ex.printStackTrace();
      }
    }

    public void endElement(String uri, String localname, String qname) {
      try {
        if (localname.equals("Community")){
          communityMap.put(community.getName(), community);
          community = null;
        } else if (localname.equals("Entity")){
          community.addEntity(entity);
          entity = null;
        }
      } catch (Exception ex ){
        ex.printStackTrace();
      }
    }

  }
}
