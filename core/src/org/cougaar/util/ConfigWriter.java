/*
 * <copyright>
 *  Copyright 1999-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.util;

import java.util.*;
import java.sql.*;
import java.io.*;

/**
 * ConfigWriter program : 
 * Read ALP Organization from database and create
 * <Node>.ini, <Cluster>.ini and <Cluster>-prototype-ini.dat files
 *
 * Note that the file can be read from any JDBC driver for this schema, 
 * including any JDBC driver.
 * In particular, intended for MS EXCEL/ACCESS files using ODBC/JDBC
 *
 * Database Schema described in comments per parse routine
 * File formats described in comments per dump routine
 **/

class ConfigWriter {

  // Top level 'main' test program
  // Usage : java ConfigWriter <driver_name> <db_url> <db_user> <db_password>
  // Default arguments refer to ODBC connection named 'MSEXCEL'
  public static void main(String args[]) throws SQLException
  {
    String driver_name = 
      (args.length >= 1 ? args[0] : "sun.jdbc.odbc.JdbcOdbcDriver");
    String database_url = 
      (args.length >= 2 ? args[1] : "jdbc:odbc:MSEXCEL");
    String database_user = 
      (args.length >= 3 ? args[2] : "");
    String database_password = 
      (args.length >= 4 ? args[3] : "");
    System.out.println("ConfigWriter : " + 
		       driver_name + "/" + database_url + "/" + 
		       database_user + "/" + database_password);
    new ConfigWriter().parseConfigData
      (driver_name, database_url, database_user, database_password);
  }

  // Parse configuration data from given database
  // Read info and write appropriate .ini and .dat files
  // Given classname of JDBC driver, URL of database and user/password
  protected void parseConfigData(String driver_classname,
				 String datasource_url,
				 String username,
				 String password)
  {

    //    DriverManager.setLogWriter(new PrintWriter(System.out));

    System.out.println("Loading driver " + driver_classname);

    try
      {
	Class driver = Class.forName (driver_classname);
      } 
    catch (ClassNotFoundException e)
      {
	System.out.println("Could not load driver : " + driver_classname);
	e.printStackTrace();
      }

    System.out.println("Connecting to the datasource : " + datasource_url);
    try 
      {
	Connection conn = 
	  DriverManager.getConnection(datasource_url, username, password);

	Statement stmt = conn.createStatement();

	// Maintain a hashtable of NodeName => Vector of ClusterNames
	Hashtable all_nodes = new Hashtable();
	parseNodeInfo(all_nodes, stmt);
	dumpNodeInfo(all_nodes);

	// Maintain a hashtable of ClusterName => Vector of Plugins
	Hashtable all_clusters = new Hashtable();
	parseClusterInfo(all_clusters, stmt);
	dumpClusterInfo(all_clusters);

	// Maintain a hashtable of OrganizationName => OrganizationData
	Hashtable all_organizations = new Hashtable();
	parseOrganizationInfo(all_organizations, stmt);
	dumpOrganizationInfo(all_organizations);

	conn.close();
      }
    catch (IOException ioe)
      {
	System.out.println("IO Exception");
	ioe.printStackTrace();
      }
    catch (SQLException sqle)
      {
	System.out.println("SQL Exception");
	sqle.printStackTrace();
      }
  }

  // Parse Node Info from into hash table of all node info
  // Schema : 
  // create table Nodes (
  //     NodeName String,  
  //     Cluster  String
  // );
  private void parseNodeInfo(Hashtable all_nodes, Statement stmt)
       throws SQLException
  {
    // Query for all Node/Cluster info and populate 'all_nodes' hashtable
    ResultSet rset = 
      stmt.executeQuery("select NodeName, Cluster from Nodes");
    while(rset.next())
      {
	String node_name = rset.getString(1);
	String cluster_name = rset.getString(2);
	Vector current_node_clusters = (Vector) all_nodes.get(node_name);
	if (current_node_clusters == null) {
	  current_node_clusters = new Vector();
	  all_nodes.put(node_name, current_node_clusters);
	}
	current_node_clusters.addElement(cluster_name);
      }
  }

  // Generate files for given node
  // Given Hashtable mapping node_name to Vector of cluster names
  // <Node>.ini File format:
  // [ Clusters ]
  // cluster = <clustername>
  // ...
  private void dumpNodeInfo(Hashtable all_nodes) throws IOException
  {
    // Iterate over hashtable of nodes and write <Node>.ini file for each
    for(Enumeration e = all_nodes.keys();e.hasMoreElements();) {
      String node_name = (String)(e.nextElement());
      PrintWriter node_file = createPrintWriter(node_name + ".ini");
      node_file.println("[ Clusters ]");
      Vector clusters = (Vector)all_nodes.get(node_name);
      for(Enumeration c = clusters.elements();c.hasMoreElements();) {
	String cluster_name = (String)(c.nextElement());
	node_file.println("cluster = " + cluster_name);
	}
      node_file.close();
    }
  }

  // Parse cluster info from Clusters table and 
  // place all cluster=>plugin info into hashtable 
  // Schema :
  // create table Clusters (
  //    Cluster String,
  //    Plugin  String
  // );
  private void parseClusterInfo(Hashtable all_clusters, Statement stmt)
       throws SQLException
  {
    // Query for all Cluster/Plugin info and populate 'all_clusters' hashtable
    ResultSet rset = 
      stmt.executeQuery("Select Cluster, Plugin from Clusters");
    while(rset.next())
      {
	String cluster_name = rset.getString(1);
	String plugin = rset.getString(2);
	Vector current_cluster_plugins = 
	  (Vector)all_clusters.get(cluster_name);
	if (current_cluster_plugins == null) {
	  current_cluster_plugins = new Vector();
	  all_clusters.put(cluster_name, current_cluster_plugins);
	}
	current_cluster_plugins.addElement(plugin);
      }
  }

  // Write <Cluster>.ini file
  // Given Hashtable mapping cluster name to Vector of plugin names
  // <Cluster>.ini File format:
  // [ Cluster ]
  // class = org.cougaar.core.cluster.ClusterImpl
  // uic = <Clustername>
  // cloned = false
  // [ PlugIns ]
  // plugin = <pluginname>
  // ...
  // 
  private void dumpClusterInfo(Hashtable all_clusters) throws IOException
  {
    // Dump hashtable of clusters
    for(Enumeration e = all_clusters.keys();e.hasMoreElements();) {
      String cluster_name = (String)e.nextElement();
      PrintWriter cluster_file = createPrintWriter(cluster_name + ".ini");
      cluster_file.println("[ Cluster ]");
      cluster_file.println("class = org.cougaar.core.cluster.ClusterImpl");
      cluster_file.println("uic = " + cluster_name);
      cluster_file.println("cloned = false");
      cluster_file.println("[ PlugIns ]");
      Vector plugins = (Vector)(all_clusters.get(cluster_name));
      for(Enumeration p = plugins.elements();p.hasMoreElements();) {
	String plugin = (String)(p.nextElement());
	cluster_file.println("plugin = " + plugin);
	}
      cluster_file.close();
    }
  }

  // Inner class to hold organization data (including roles, relationships)
  private class OrganizationData {
    public OrganizationData(String Name, String UIC, String UTC, String SRC,
			    String Superior, int Echelon, String Agency,
			    String Service, String Nomenclature,
			    String Prototype)
    {
      myName = Name;
      myUIC = UIC;
      myUTC = UTC;
      mySRC = SRC;
      mySuperior = Superior;
      myEchelon = Echelon;
      myAgency = Agency;
      myNomenclature = Nomenclature;
      myPrototype = Prototype;
      myRoles = new Vector();
      mySupportRelations = new Vector();
      myAdditionalPGs = new Hashtable(); // Hash : PGName => Vector of PGData
    }
    public String myName;
    public String myUIC;
    public String myUTC;
    public String mySRC;
    public String mySuperior;
    public int myEchelon;
    public String myAgency;
    public String myService;
    public String myNomenclature;
    public String myPrototype;
    public Vector myRoles;  // List of roles for organization
    public Vector mySupportRelations;  // List of OrganizationSupportRelations
    public Hashtable myAdditionalPGs; // Map PGName => Vector (PGData)
    public void addRole(String role) { myRoles.addElement(role); }
    public boolean isCivilan() { return myPrototype.equals("Civilian"); }
    public void addSupportRelation(SupportRelation rel) 
    { 
      mySupportRelations.addElement(rel); 
    }
    public String toString() { 
      String roles_image = "";
      for(Enumeration e = myRoles.elements();e.hasMoreElements();)
	{
	  roles_image = roles_image + "/" + (String)e.nextElement();
	}
      String relations_image  = "";
      for(Enumeration e = mySupportRelations.elements();e.hasMoreElements();)
	{
	  relations_image = relations_image + "/" + (SupportRelation)e.nextElement();
	}
      return "#<Organization " + myName + " " + 
	myUIC + " "  + myUTC + " " + mySRC + " " + mySuperior + " " + 
	myEchelon + " " + myAgency + " " + myService + " "+ 
	myNomenclature + " " + myPrototype + " " + 
	roles_image + " " + relations_image +  ">"; 
    }
  }

  // Private inner class to hold SupportRelation info (organization, role)
  private class SupportRelation {
    public SupportRelation(String Name, String SupportedOrganization, 
			   String Role)
    { 
      myName = Name;
      mySupportedOrganization = SupportedOrganization; 
      myRole = Role;
    }
    public String myName;
    public String mySupportedOrganization;
    public String myRole;
    public String toString() { 
      return "#<Relation " + myName + " " + mySupportedOrganization + " " + 
	myRole + ">"; 
    }
  }

  // Class to hold PG (PropertyGroup) info 
  private class PGInfo {
    public PGInfo(String property, String type, Object value, String units)
    {
      myProperty = property; myType = type; myValue = value; myUnits = units;
    }
    public String myProperty;
    public String myType;
    public Object myValue;
    public String myUnits;
  }

  // Parse database tables to create organization info, including 
  // organization details (from Organizations table)
  // roles (from Roles table)
  // support relationships (from Relationships table)
  // Schema:
  // create table Organizations (
  //   Name String,
  //   UIC String,
  //   UTC String,
  //   SRC String,
  //   Superior String,
  //   Echelon String,
  //   Agency String,
  //   Service String,
  //   Nomenclature String,
  //   Prototype String,
  // );
  // create table Roles (
  //    Organization String,
  //    Role String -- Capable Role for Organization
  // );
  // create table Relationships (
  //    Organization String,
  //    Supported String, -- Supported Organization
  //    Role String -- Support Role
  // );
  // create table AdditionalPGs (
  //    Organization String,
  //    PGName String,
  //    Property String,
  //    PropertyType String,
  //    PropertyValue String,
  //    PropertyUnit String
  // );
  private void parseOrganizationInfo(Hashtable all_organizations, 
				     Statement stmt)
       throws SQLException
  {
    ResultSet rset = 
      stmt.executeQuery("Select Name, UIC, UTC, SRC, Superior, " + 
			"Echelon, Agency, Service, Nomenclature, " + 
			"Prototype from Organizations");

    while(rset.next()) {

      String current_organization = rset.getString(1);
      OrganizationData org_data = 
	new OrganizationData(current_organization,
			     rset.getString(2), // UIC
			     rset.getString(3), // UTC
			     rset.getString(4), // SRC
			     rset.getString(5), // Superior
			     rset.getInt(6), // Echelon
			     rset.getString(7), // Agency
			     rset.getString(8), // Service
			     rset.getString(9), // Nomenclature
			     rset.getString(10)); // Prototype
      all_organizations.put(current_organization, org_data);

    }

    // Query for all Organization/Role info
    rset = stmt.executeQuery("Select Organization, Role from Roles");
    while(rset.next())
      {
	String current_organization = (String)rset.getString(1);
	OrganizationData org_data = (OrganizationData)
	  all_organizations.get(current_organization);
	if (org_data == null) {
	  System.out.println("No organization defined : " + 
			     current_organization);
	  System.exit(0);
	}
	org_data.addRole(rset.getString(2)); // Role
      }

    // Query for all Organization/Supported/Role Relationship info
    rset = stmt.executeQuery("Select Organization, Supported, Role from Relationships");
    while(rset.next())
      {
	String current_organization = (String)rset.getString(1);
	OrganizationData org_data = (OrganizationData)
	  all_organizations.get(current_organization);
	if (org_data == null) {
	  System.out.println("No organization defined : " + 
			     current_organization);
	  System.exit(0);
	}
	SupportRelation support = 
	  new SupportRelation(current_organization, 
			      rset.getString(2),  // Supported Org
			      rset.getString(3)); // Role
	org_data.addSupportRelation(support);
      }

    // Query for all AdditionalPG information
    rset = stmt.executeQuery("Select Organization, PGName, Property, PropertyType, PropertyValue, PropertyUnit from AdditionalPGs");
    while (rset.next()) {
      String current_organization = (String)rset.getString(1);
      String pg_name = (String)rset.getString(2);
      OrganizationData org_data = (OrganizationData)
	all_organizations.get(current_organization);
      if (org_data == null) {
	System.out.println("No organization defined : " + current_organization);
	System.exit(0);
      }
      Vector pg_info_list = (Vector)org_data.myAdditionalPGs.get(pg_name);
      if (pg_info_list == null) {
	pg_info_list = new Vector();
	org_data.myAdditionalPGs.put(pg_name, pg_info_list);
      }
      String property_name = rset.getString(3);
      String value_type = rset.getString(4);
      String value = rset.getString(5);
      // Strip off bounding quotes if present
      // May be needed to force numbers to parse as strings
      if ((value.charAt(0) == '"') && (value.charAt(value.length()-1) == '"'))
	value = value.substring(1, value.length()-1);
      String units = rset.getString(6);
      PGInfo pg_info = 
	new PGInfo(property_name,  // Property
		   value_type, // Type
		   value, // Value
		   units); // Units
      pg_info_list.addElement(pg_info);
    }
  }

  // Print <Cluster>-prototype-ini.dat file
  // File format:
  // [Prototype] CombatOrganization|CivilanOrganization
  // [UniqueId] "UTC/CombatOrg"
  // [UIC] "UIC/<OrganizationName>
  // [Relationship]
  // Superior  <Superior> ""
  // Support   <Supported> <Role>
  // [TypeIdentificationPG]
  // TypeIdentification String "UTC/RTOrg"
  // Nomenclature String <Nomenclature>
  // AlternateTypeIdentification String "SRC/<SRC>"
  // [ClusterPG]
  // ClusterIdentifier String <OrganizationName>
  // [OrganizationPG]
  // Roles Collection<Role> <Role>
  // [MilitaryOrgPG]
  // UIC String <UIC>
  // Echelon String <Echelon>
  // UTC String <UTC>
  // SRC String <SRC>
  // 
  private void dumpOrganizationInfo(Hashtable all_organizations) 
       throws IOException 
  {
    for(Enumeration e = all_organizations.keys(); e.hasMoreElements();)
      {
	String org_name = (String)e.nextElement();
	OrganizationData org_data = 
	  (OrganizationData)all_organizations.get(org_name);
	PrintWriter org_file = 
	  createPrintWriter(org_name + "-prototype-ini.dat");
	org_file.println("[Prototype] " + 
			 (org_data.isCivilan() ? 
			  "CivilianOrganization" : 
			  "MilitaryOrganization"));
	org_file.println("[UniqueId] " + '"' + "UTC/CombatOrg" + '"');
	org_file.println("[UIC] " + '"' + "UIC/" + org_name + '"');

	// Write out Superior/Support Relationships
	org_file.println("[Relationship]");
	if (org_data.mySuperior != null) {
	  org_file.println("Superior " + '"' + 
			   org_data.mySuperior + '"' + " " + '"' + '"');
	}
	for(Enumeration rels = org_data.mySupportRelations.elements();
	    rels.hasMoreElements();)
	  {
	    SupportRelation suprel = (SupportRelation)rels.nextElement();
	    org_file.println("Supporting " + '"' + 
			     suprel.mySupportedOrganization + 
			     '"' + " " + '"' + 
			     suprel.myRole + '"');
	  }

	// Print TypeIdentificationPG fields
	org_file.println("[TypeIdentificationPG]");
	org_file.println("TypeIdentification String " + 
			 '"' + "UTC/RTOrg" + '"');
	org_file.println("Nomenclature String " + '"' + 
			 org_data.myNomenclature + '"');
	org_file.println("AlternateTypeIdentification String " + 
			 '"' + "SRC/" + org_data.mySRC + '"');
	
	// Print ClusterPG info
	org_file.println("[ClusterPG]");
	org_file.println("ClusterIdentifier String " + '"' + org_name + '"');
	
	// Print OrganizationPG (Roles) info
	org_file.println("[OrganizationPG]");
	org_file.print("Roles Collection<Role> " + '"');
	boolean is_first = true;
	for(Enumeration roles = org_data.myRoles.elements();
	    roles.hasMoreElements();)
	  {
	    String role = (String)roles.nextElement();
	    if (!is_first) {
	      org_file.print(", ");
	    }
	    org_file.print(role);
	    is_first = false;
	  }
	org_file.println('"');

	// Print MilitaryOrgPG info
	org_file.println("[MilitaryOrgPG]");
	org_file.println("UIC String " + '"' + org_data.myUIC + '"');
	org_file.println("Echelon String " + '"' + org_data.myEchelon + '"');
	org_file.println("UTC String " + '"' + '"');
	org_file.println("SRC String " + '"' + org_data.mySRC + '"');

	// Print AdditionalPG info
	for(Enumeration k = org_data.myAdditionalPGs.keys();
	    k.hasMoreElements(); ) {
	  String pg_name = (String)k.nextElement();
	  org_file.println("[" + pg_name + "]");
	  Vector all_properties = 
	    (Vector)org_data.myAdditionalPGs.get(pg_name);
	  for(Enumeration p = all_properties.elements();p.hasMoreElements();) {
	    PGInfo pg_info = (PGInfo)p.nextElement();
	    String units_image = 
	      (pg_info.myUnits != null) ? pg_info.myUnits.toString() : "";
	    org_file.println(pg_info.myProperty + "\t" + 
			     pg_info.myType + "\t" + 
			     '"' + pg_info.myValue + '"' + "\t" + 
			     units_image);
	  }
	}
	org_file.close();

      }
  }

  // Utility functions

  // Create a PrintWriter class for given filename
  private PrintWriter createPrintWriter(String filename) throws IOException
  {
    return 
      new PrintWriter(new OutputStreamWriter(new FileOutputStream(filename)));
  }

}
