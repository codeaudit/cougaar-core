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

// ConfigWriter program : 
// Read ALP Organization from database and create
// <Node>.ini, <Cluster>.ini and <Cluster>-prototype-ini.dat files

// Note that the file can be read from any JDBC driver for this schema, 
// including MS EXCEL/ACCESS files or TEXT directories using ODBC/JDBC

// Database Schema described in comments per parse routine
// File formats described in comments per dump routine

// slight SQL mods for MS Access DB structure.
// Also replaced getString(n) with getString("Column Name") to
// make it more generic and less reliant on column position.

public class MDBConfigWriter {

  // Top level 'main' test program
  // Assumes hard-coded ODBC connection named 'AlpConfig'
  public static void main(String args[]) throws SQLException
  {
    System.out.println("In ConfigWriter...");
    String community = null;
    String path = null;
    String node = null;

    for (int ind = 0; ind < args.length; ind++)
    {
        if (args[ind].equals("-c"))
        {
            // pull only a certain community
            community = args[++ind];
        } else if (args[ind].equals("-n")) {
            // pull only a certain node
            node = args[++ind];            
        } else if (args[ind].equals("-p")) {
            // put the files in the specified path
            path = args[++ind];
			System.out.println("Path is: "+path);
        } 
    }
    
    
    new MDBConfigWriter().parseConfigData
      ("sun.jdbc.odbc.JdbcOdbcDriver", "jdbc:odbc:AlpConfig", "", "", community, node, path);
  }

  // Parse configuration data from given database
  // Read info and write appropriate .ini and .dat files
  // Given classname of JDBC driver, URL of database and user/password
  protected void parseConfigData(String driver_classname,
				 String datasource_url,
				 String username,
				 String password,
				 String community,
				 String node,
				 String path)
  {

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
	parseNodeInfo(all_nodes, stmt, community, node);
	dumpNodeInfo(all_nodes, path);

	// Maintain a hashtable of ClusterName => Vector of Plugins
	Hashtable all_clusters = new Hashtable();
	parseClusterInfo(all_clusters, stmt, community, node);
	dumpClusterInfo(all_clusters, path);

	// Maintain a hashtable of OrganizationName => OrganizationData
	Hashtable all_organizations = new Hashtable();
	parseOrganizationInfo(all_organizations, stmt, community, node);
	dumpOrganizationInfo(all_organizations, path);

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
  private void parseNodeInfo(Hashtable all_nodes, Statement stmt, String community, 
                            String node)
       throws SQLException
  {
    // Query for all Node/Cluster info and populate 'all_nodes' hashtable

    String sql = "select Node, Name from Organizations";

    if (community != null && node != null)
    {
    sql = sql +" where Community='"+community+"' and Node='"+node+"'";
    } else if (community != null) {
    sql = sql +" where Community='"+community+"'";
    } else if (node != null) {
    sql = sql +" where Node='"+node+"'";
    }

    
    System.out.println(sql);
    ResultSet rset = 
      stmt.executeQuery(sql);
	  int number = 0;
    while(rset.next())
      { number++;
	      String node_name = rset.getString("Node");
	      String cluster_name = rset.getString("Name");
        try {
           Vector current_node_clusters = (Vector) all_nodes.get(node_name);
	         if (current_node_clusters == null) {
	            current_node_clusters = new Vector();
	            all_nodes.put(node_name, current_node_clusters);
	        }
	        current_node_clusters.addElement(cluster_name);
        } catch (Exception nullPointer) {
            System.out.println("WARNING:  Cluster "+cluster_name+" does not have a node!!!");
        }
      }
	  System.out.println("Query returned "+number+" results");
    rset = null;
    System.gc();
  }

  // Generate files for given node
  // Given Hashtable mapping node_name to Vector of cluster names
  // <Node>.ini File format:
  // [ Clusters ]
  // cluster = <clustername>
  // ...
  private void dumpNodeInfo(Hashtable all_nodes, String path) throws IOException
  {
    PrintWriter node_file;
    // Iterate over hashtable of nodes and write <Node>.ini file for each
    for(Enumeration e = all_nodes.keys();e.hasMoreElements();) {
      String node_name = (String)(e.nextElement());
      

      try {
        if (path != null) {
		    node_file = createPrintWriter(path + File.separator+ node_name + ".ini");
        } else {
		    node_file = createPrintWriter(node_name + ".ini");
        }
        node_file.println("[ Clusters ]");
        Vector clusters = (Vector)all_nodes.get(node_name);
        for(Enumeration c = clusters.elements();c.hasMoreElements();) {
	        String cluster_name = (String)(c.nextElement());
	        node_file.println("cluster = " + cluster_name);
	    }
        node_file.close();
      } catch (IOException exc) {
        System.out.println("IOException:  "+exc); 
        System.exit(-1);
      }

    
    }
  }

  // Parse cluster info from Clusters table and 
  // place all cluster=>plugin info into hashtable 
  // Schema :
  // create table Clusters (
  //    Cluster String,
  //    Plugin  String
  // );
  private void parseClusterInfo(Hashtable all_clusters, Statement stmt, String community, 
                                String node)
       throws SQLException
  {

    
    String sql = null;
    ResultSet rset;
    int number = 0;

    // Grab the plugin groups
    Hashtable plugInGroups = new Hashtable();
    sql = "select GroupName, GroupSequence from GroupMaster";

    number  = 0;
    System.out.println(sql);
    
    rset = stmt.executeQuery(sql);
    
    while(rset.next())
    {   number++;
        String groupName = rset.getString("GroupName");
	    System.out.println(groupName);
	    Integer index = new Integer(rset.getInt("GroupSequence"));
	    System.out.println(index);
        plugInGroups.put(index, groupName);
        
    }
    System.out.println("Query returned "+number+" results");

    // Grab the group definitions
    Hashtable groupDefinitions = new Hashtable();
    for(int ind = 0; ind < plugInGroups.size(); ind++)
    {
	
	
      String group = (String)plugInGroups.get(new Integer(ind));
	  if (group != null)
	  {
	    sql = "select InGroupSequence, PlugIn from GroupDefinitions"
		+" where GroupName='"+group+"'";
        rset = stmt.executeQuery(sql);
        System.out.println(sql);
	    groupDefinitions.put(group, new Hashtable());

	    number = 0;
        while(rset.next())
        {
	        number++;
	        Hashtable plugins = (Hashtable)groupDefinitions.get(group);
	        int index = rset.getInt("InGroupSequence");
	        String plugIn = rset.getString("PlugIn");
	        plugins.put(new Integer(index), plugIn);
        }
            System.out.println("Query returned "+number+" results");
      } else {
		  break;
	  } 
    }

    // Add the plugIn groups per cluster to the plugIn vector
    number = 0;
    sql = "select Cluster, PlugInGroup from GroupsInCluster";

    if (community != null && node != null)
	{
		sql = sql +", Organizations where Cluster=Organizations.Name and"
			+" Organizations.Community='"+community+"' and Organizations.Node='"+node+"'";
	}
    else if (community != null) {
		sql = sql +", Organizations where Cluster=Organizations.Name and"
			+" Organizations.Community='"+community+"'";
	}
    else if (node != null) {
		sql = sql + ", Organizations where Organizations.Node='"+node+"'";
    } 
	

    System.out.println(sql);
    
    number = 0;
    rset = stmt.executeQuery(sql);
    while(rset.next())
      {number++;
	        String cluster_name = rset.getString("Cluster");
	        String pluginGroup = rset.getString("PlugInGroup");

	        Vector current_cluster_plugins = (Vector)all_clusters.get(cluster_name);	        
		  if (current_cluster_plugins == null) {
	            current_cluster_plugins = new Vector();
	            all_clusters.put(cluster_name, current_cluster_plugins);
	        }
	      Hashtable groupPlugIns = (Hashtable)groupDefinitions.get(pluginGroup);
		  for (int ind = 0; ind < groupPlugIns.size(); ind++)
		  {
			current_cluster_plugins.addElement(groupPlugIns.get(new Integer(ind)));
		  }
      }
    System.out.println("Query returned "+number+" results");


    // Grab the plugins that go in every community or every cluster within specific communities
    Vector mdbCommunities = new Vector();
    
    sql = "select Community from CommunityMaster";
    number  = 0;
    System.out.println(sql);
    
    rset = stmt.executeQuery(sql);
    
    while(rset.next())
    {number++;
        String community_name = rset.getString("Community");
        mdbCommunities.addElement(community_name);
        
    }
    System.out.println("Query returned "+number+" results");
 

    while(!mdbCommunities.isEmpty())
    {	number = 0;
        String sCommunity = (String) mdbCommunities.firstElement();
        mdbCommunities.removeElementAt(0);
        

        sql = "select Organizations.Name, CommunityPlugIns.Plugin from Organizations, CommunityPlugIns"
            +" where CommunityPlugIns.Community='"+sCommunity+"'";
 
	    if (!sCommunity.equals("Society"))
        {
            if (community != null)
            {
                if (!community.equals(sCommunity))
                {
                    continue;
                }
            }
            sql = sql +" and Organizations.Community='"+sCommunity+"'";
        } else {
            if (community != null)
            {
                sql = sql +" and Organizations.Community='"+community+"'";
            }
        }


        if (node != null)
        {
            sql = sql +" and Organizations.Node='"+node+"'";
        }

        
        System.out.println(sql);
        number = 0;
        rset = stmt.executeQuery(sql);
        while(rset.next())
        {	number++;
	        String cluster_name = rset.getString("Name");
	        String plugin = rset.getString("Plugin");

	        Vector current_cluster_plugins = (Vector)all_clusters.get(cluster_name);
	        if (current_cluster_plugins == null) {
	            current_cluster_plugins = new Vector();
	            all_clusters.put(cluster_name, current_cluster_plugins);
	        }
	            
	        current_cluster_plugins.addElement(plugin);
        }
	    System.out.println("Query returned "+number+" results");
    }    




    // Query for all Cluster/Plugin info and populate 'all_clusters' hashtable

    sql = "select Clusters.Cluster, Clusters.Plugin, Clusters.Parameters from Clusters, Organizations"
        +" where Clusters.Cluster=Organizations.Name";
        
	if (community != null && node != null)
	{
		sql = sql + "and Organizations.Node='"+node+"'"
        +" and Organizations.Community='"+community+"'";
	} else if (community != null) {
		sql = sql + " and Organizations.Community='"+community+"'";
	} else if (node != null) {
		sql = sql + " and Organizations.Node='"+node+"'";
    }

    System.out.println(sql);
    
    number = 0;
    rset = stmt.executeQuery(sql);
    while(rset.next())
      {number++;
	        String cluster_name = rset.getString("Cluster");
	        String plugin = rset.getString("Plugin");
	        String parameters = rset.getString("Parameters");
	        
	        // put the parameters into the plugin
	        if (parameters != null)
	        {
	            plugin = plugin+"("+parameters+")";
	        }
	        
	        
	        Vector current_cluster_plugins = (Vector)all_clusters.get(cluster_name);
	        if (current_cluster_plugins == null) {
	            current_cluster_plugins = new Vector();
	            all_clusters.put(cluster_name, current_cluster_plugins);
	        }
	            
	        current_cluster_plugins.addElement(plugin);
      }
	  	  System.out.println("Query returned "+number+" results");

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
  private void dumpClusterInfo(Hashtable all_clusters, String path) throws IOException
  {
    // Dump hashtable of clusters
    for(Enumeration e = all_clusters.keys();e.hasMoreElements();) {
      String cluster_name = (String)e.nextElement();
      PrintWriter cluster_file;

      try {
        if (path != null)
        {
            cluster_file = createPrintWriter(path+File.separator+ cluster_name + ".ini");
        } else {
            cluster_file = createPrintWriter(cluster_name + ".ini");
        }   

        cluster_file.println("[ Cluster ]");
        cluster_file.println("class = org.cougaar.core.cluster.ClusterImpl");
        cluster_file.println("uic = " + cluster_name);
        cluster_file.println("cloned = false\n");
        cluster_file.println("[ PlugIns ]");
        Vector plugins = (Vector)(all_clusters.get(cluster_name));
        for(Enumeration p = plugins.elements();p.hasMoreElements();) {
	        String plugin = (String)(p.nextElement());
	        cluster_file.println("plugin = " + plugin);
	    }
        cluster_file.close();
      } catch (IOException exc) {
        System.out.println("IOException:  "+exc);
        System.exit(-1);
      }
      
    }
  }


  // Inner class to hold organization data (including roles, relationships)
  private class OrganizationData {
    public OrganizationData(String Name, String UIC, String UTC, String SRC,
			    String Superior, Double Echelon, String Agency,
			    String Service, String Nomenclature,
			    String Prototype, boolean IsReserve)
    {
      myName = myStringInit(Name);
      myUIC = myStringInit(UIC);
      myUTC = myStringInit(UTC);
      mySRC = myStringInit(SRC);
      mySuperior = myStringInit(Superior);
      myEchelon = Echelon;
      myAgency = myStringInit(Agency);
      myNomenclature = myStringInit(Nomenclature);
      myPrototype = myStringInit(Prototype);
	  myIsReserve = IsReserve;
      myRoles = new Vector();
      mySupportRelations = new Vector();
    }
    
    public void initAssignedLoc(String Location,
				String Longitude, String Latitude,
				String GeoLoc,   String InstallCode,
				String CSCode,   String CSName,
				String ICAOCode)
    {

	  myAssignedLocation = myStringInit(Location);
	  myAssignedLongitude = myStringInit(Longitude);
	  myAssignedLatitude = myStringInit(Latitude);
	  myAssignedGeoLoc = myStringInit(GeoLoc);
	  myAssignedInstallCode = myStringInit(InstallCode);
	  myAssignedCSCode = myStringInit(CSCode);
	  myAssignedCSName = myStringInit(CSName);
	  myAssignedICAOCode = myStringInit(ICAOCode);
    }    
    
    public void initHomeLoc(String Location,
				String Longitude, String Latitude,
				String GeoLoc,   String InstallCode,
				String CSCode,   String CSName,
				String ICAOCode)
    {

	  myHomeLocation = myStringInit(Location);
	  myHomeLongitude = myStringInit(Longitude);
	  myHomeLatitude = myStringInit(Latitude);
	  myHomeGeoLoc = myStringInit(GeoLoc);
	  myHomeInstallCode = myStringInit(InstallCode);
	  myHomeCSCode = myStringInit(CSCode);
	  myHomeCSName = myStringInit(CSName);
	  myHomeICAOCode = myStringInit(ICAOCode);
    }    
    
    public String myName;
    public String myUIC;
    public String myUTC;
    public String mySRC;
    public String mySuperior;
    public Double myEchelon;
    public String myAgency;
    public String myService;
    public String myNomenclature;
    public String myPrototype;
	public String myAssignedLocation;
	public String myAssignedLongitude;
	public String myAssignedLatitude;
	public String myAssignedGeoLoc;
	public String myAssignedInstallCode;
	public String myAssignedCSCode;
	public String myAssignedCSName;
	public String myAssignedICAOCode;	
	public String myHomeLocation;
	public String myHomeLongitude;
	public String myHomeLatitude;
	public String myHomeGeoLoc;
	public String myHomeInstallCode;
	public String myHomeCSCode;
	public String myHomeCSName;
	public String myHomeICAOCode;
	public boolean myIsReserve;
    
    public Vector myRoles;  // List of roles for organization
    public Vector mySupportRelations;  // List of OrganizationSupportRelations
	public Vector myCSSCapabilities; // List of CSSCapabilities
    public void addRole(String role) { myRoles.addElement(role); }
    public boolean isCivilan() { return myPrototype.equals("Civilian"); }
    public void addSupportRelation(SupportRelation rel) 
    { 
      mySupportRelations.addElement(rel); 
    }
    public void addCSSCapabilities(CSSCapabilities cap) 
    { 
		if (myCSSCapabilities == null)
		{
			myCSSCapabilities = new Vector();
		}
		myCSSCapabilities.addElement(cap); 
    }

	private String myStringInit(String value)
	{
		if (value == null)
		{
			return "";
		}
		return value;
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

  private class CSSCapabilities
  {
	public String name;
	public String capability;
	public String qty;
	public String period;

	private String myStringInit(String value)
	{
		if (value == null)
		{
			return "";
		}
		return value;
	}

	public CSSCapabilities(String name, String capability, 
			       String qty, String period)
	{
		this.name = myStringInit(name);
		this.capability = myStringInit(capability);
		this.qty = myStringInit(qty);
		this.period = myStringInit(period);
	}

  };

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
  private void parseOrganizationInfo(Hashtable all_organizations, 
				     Statement stmt, String community, String node)
       throws SQLException
  {
    Double echelon;
	int number = 0;
	/*
    String sql = "select Name, UIC, UTC, SRC, Superior, Echelon, Agency, Service, Nomenclature, "
        +"Prototype, Location, Longitude, Latitude, Organizations.GeolocCode, InstallationTypeCode, "
        +"CountryStateCode, CountryStateName, IcaoCode, ReserveOrg from Organizations, "
        +"LocationMaster where Organizations.GeolocCode=LocationMaster.GeolocCode" ;
    */
    

    String sql = "select Name, UIC, UTC, SRC, Superior, Echelon, Agency, Service, Nomenclature, "
        +"Prototype, ReserveOrg from Organizations";
    
    if (community != null && node != null)
    {
        sql = sql +" where Organizations.Community='"+community+"' and Organizations.Node='"+node+"'";
    } else if (community != null) {
        sql = sql +" where Organizations.Community='"+community+"'";
    } else if (node != null) {
        sql = sql +" where Organizations.Node='"+node+"'";
    }
    System.out.println(sql);
    

    ResultSet rset = 
      stmt.executeQuery(sql);
	
    while(rset.next()) {
		number++;
      String current_organization = rset.getString("Name");
      String testEchelon = rset.getString("Echelon");
      if (testEchelon == null)
      {
        echelon = new Double(-1);
      } else {
        echelon = new Double(testEchelon);
      }
      boolean reserve = false;
      int res = rset.getInt("ReserveOrg");
      if (res == 0)
      {
        reserve = false;
      } else {
        reserve = true;
      }
           
	  OrganizationData org_data = 
	new OrganizationData(current_organization,
			     rset.getString("UIC"), // UIC
			     rset.getString("UTC"), // UTC
			     rset.getString("SRC"), // SRC
			     rset.getString("Superior"), // Superior
			     echelon, // Echelon
			     rset.getString("Agency"), // Agency
			     rset.getString("Service"), // Service
			     rset.getString("Nomenclature"), // Nomenclature
			     rset.getString("Prototype"), // Prototype
                 reserve); // isReserve
      all_organizations.put(current_organization, org_data);

    }
    System.out.println("Query returned "+number+" results");

    rset = null;
    System.gc();

    // Query for the Assigned Location
    sql = "select Name, Location, Longitude, Latitude, AssignedLoc, InstallationTypeCode, "
        +"CountryStateCode, CountryStateName, IcaoCode, ReserveOrg from Organizations, "
        +"LocationMaster where Organizations.AssignedLoc=LocationMaster.GeolocCode" ;
    
    if (community != null && node != null)
    {
        sql = sql +" and Organizations.Community='"+community+"' and Organizations.Node='"+node+"'";
    } else if (community != null) {
        sql = sql +" and Organizations.Community='"+community+"'";
    } else if (node != null) {
        sql = sql +" and Organizations.Node='"+node+"'";
    }
    
    System.out.println(sql);
    

    rset = stmt.executeQuery(sql);
	
    while(rset.next()) {
		number++;
      String current_organization = rset.getString("Name");
      OrganizationData data = (OrganizationData)
                    all_organizations.get(current_organization);
      // initialize the assigned location
      data.initAssignedLoc(rset.getString("Location"),
				rset.getString("Longitude"), 
				rset.getString("Latitude"),
				rset.getString("AssignedLoc"),
				rset.getString("InstallationTypeCode"),
				rset.getString("CountryStateCode"),
				rset.getString("CountryStateName"),
				rset.getString("IcaoCode"));
    }
    System.out.println("Query returned "+number+" results");

    rset = null;
    System.gc();
    
    // Query for the home location
    sql = "select Name, Location, Longitude, Latitude, HomeLoc, InstallationTypeCode, "
        +"CountryStateCode, CountryStateName, IcaoCode, ReserveOrg from Organizations, "
        +"LocationMaster where Organizations.HomeLoc=LocationMaster.GeolocCode" ;
    
    if (community != null && node != null)
    {
        sql = sql +" and Organizations.Community='"+community+"' and Organizations.Node='"+node+"'";
    } else if (community != null) {
        sql = sql +" and Organizations.Community='"+community+"'";
    } else if (node != null) {
        sql = sql +" and Organizations.Node='"+node+"'";
    }
    
    System.out.println(sql);
    

    rset = stmt.executeQuery(sql);
	
    while(rset.next()) {
		number++;
      String current_organization = rset.getString("Name");
      OrganizationData data = (OrganizationData) all_organizations.get(current_organization);
      // initialize the Home location
      data.initHomeLoc(rset.getString("Location"),
				rset.getString("Longitude"), 
				rset.getString("Latitude"),
				rset.getString("HomeLoc"),
				rset.getString("InstallationTypeCode"),
				rset.getString("CountryStateCode"),
				rset.getString("CountryStateName"),
				rset.getString("IcaoCode"));
    }
    System.out.println("Query returned "+number+" results");

    rset = null;
    System.gc();
    
    

    // Query for all Organization/Role info
 
    sql = "select Organization, Role from Roles, Organizations"
		+" where Organizations.Name=Roles.Organization ";
    number = 0;
    if (community != null && node != null)
    {
        sql = sql +" and Organizations.Community='"+community+"' and Organizations.Node='"+node+"'";
    } else if (community != null) {
        sql = sql +" and Organizations.Community='"+community+"'";
    } else if (node != null) {
        sql = sql +" and Organizations.Node='"+node+"'";
    }

    System.out.println(sql);
    rset = stmt.executeQuery(sql);
    
    while(rset.next())
      {number++;
      String current_organization = (String)rset.getString("Organization");
      OrganizationData org_data = (OrganizationData)
	  all_organizations.get(current_organization);
      if (org_data == null) {
	        System.out.println("No organization defined : " + 
			current_organization);
	        System.exit(0);
      }
      org_data.addRole(rset.getString("Role")); // Role
      }
    System.out.println("Query returned "+number+" results");

    rset = null;
    System.gc();

    sql = "Select SupportingOrg, SupportedOrg, Role from Relationships, Organizations"
        +" where Relationships.SupportingOrg=Organizations.Name";
        
    if (community != null && node != null)
    {
         sql = sql +" and Organizations.Community='"+community+"'"+" and Organizations.Node='"+node+"'";
    } else if (community != null) {
        sql = sql +" and Organizations.Community='"+community+"'";
            
    } else if (node != null) {
        sql = sql +" and Organizations.Node='"+node+"'";
    }

    System.out.println(sql);
    rset = stmt.executeQuery(sql);
    
	number = 0;
    rset = stmt.executeQuery(sql);
    while(rset.next())
      {number++;
	String current_organization = (String)rset.getString("SupportingOrg");
	OrganizationData org_data = (OrganizationData)
	  all_organizations.get(current_organization);
	if (org_data == null) {
	  System.out.println("No organization defined : " + 
			     current_organization);
	  System.exit(0);
	}
	SupportRelation support = 
	  new SupportRelation(current_organization, 
			      rset.getString("SupportedOrg"),  // Supported Org
			      rset.getString("Role")); // Role
	org_data.addSupportRelation(support);
	 }

   System.out.println("Query returned "+number+" results");

   rset = null;
   System.gc();

   // get the CSSCapabilities
   sql = "select CSSCapability.Cluster, Capability, QTY, Period"
     +" from CSSCapability, Organizations"
     +" where CSSCapability.Cluster=Organizations.Name";

    if (community != null && node != null)
    {
	   sql = sql +" and Organizations.Community='"+community+"' and Organizations.Node='"+node+"'";
	} else if (community != null){
	   sql = sql +" and Organizations.Community='"+community+"'";
	} else if (node != null){
	   sql = sql +" and Organizations.Node='"+node+"'";
	}

    System.out.println(sql);
	number = 0;
    rset = stmt.executeQuery(sql);
    while(rset.next())
      {number++;
	    String current_organization = (String)rset.getString("Cluster");
	    OrganizationData org_data = (OrganizationData)
	      all_organizations.get(current_organization);
	    if (org_data == null) {
	      System.out.println("No organization defined : " + 
			     current_organization);
	      System.exit(0);
	    }
	    CSSCapabilities CSSCap = 
	      new CSSCapabilities(current_organization, 
			      rset.getString("Capability"),  // Capability
			      rset.getString("QTY"), // Count
				  rset.getString("Period"));
	    org_data.addCSSCapabilities(CSSCap);
      }
      System.out.println("Query returned "+number+" results");

      rset = null;
      System.gc();

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
  private void dumpOrganizationInfo(Hashtable all_organizations, String path) 
       throws IOException 
  {


	Hashtable supportedOrgRoles = null;



    for(Enumeration e = all_organizations.keys(); e.hasMoreElements();)
    {
    supportedOrgRoles = new Hashtable();
	String org_name = (String)e.nextElement();
	OrganizationData org_data = 
	  (OrganizationData)all_organizations.get(org_name);
	PrintWriter org_file;

    try {
        if (path != null)
        {
	        org_file = createPrintWriter(path+File.separator+ org_name + "-prototype-ini.dat");
        } else {
	        org_file = createPrintWriter(org_name + "-prototype-ini.dat");
        } 
	    org_file.println("[Prototype] " + 
			    (org_data.isCivilan() ? 
			    "CivilianOrganization" : 
			    "MilitaryOrganization"));
	    org_file.println("\n[UniqueId] " + '"' + "UTC/CombatOrg" + '"');
	    org_file.println("\n[UIC] " + '"' + "UIC/" + org_name + '"');

	    // Write out Superior/Support Relationships
	    org_file.println("\n[Relationship]");
	    if (org_data.mySuperior != null) {
	    org_file.println("Superior " + '"' + 
			    org_data.mySuperior +'"' + " " + '"' + '"');
	    }
    	

	    for (Enumeration rels = org_data.mySupportRelations.elements();rels.hasMoreElements() ; )
	    {
		    SupportRelation suprel = (SupportRelation)rels.nextElement();

            if (!supportedOrgRoles.containsKey(suprel.mySupportedOrganization))
            {
                supportedOrgRoles.put(suprel.mySupportedOrganization, suprel.myRole);
            } else {
                String role = (String)supportedOrgRoles.get(suprel.mySupportedOrganization);
                role = role +", "+suprel.myRole;
                supportedOrgRoles.put(suprel.mySupportedOrganization, role);
            }

	    }


        for (Enumeration roles = supportedOrgRoles.keys(); roles.hasMoreElements();)
        {
            String supportedOrg = (String) roles.nextElement();
            String role = (String)supportedOrgRoles.get(supportedOrg);
            org_file.println("Supporting "+'"'+supportedOrg+'"'+" "+'"'+role+'"');
        }





	    // Print TypeIdentificationPG fields
	    org_file.println("\n[TypeIdentificationPG]");
	    org_file.println("TypeIdentification String " + 
			    '"' + "UTC/RTOrg" + '"');
	    org_file.println("Nomenclature String " + '"' + 
			    org_data.myNomenclature + '"');
	    org_file.println("AlternateTypeIdentification String " + 
			    '"' + "SRC/" + org_data.mySRC + '"');
    	
	    // Print ClusterPG info
	    org_file.println("\n[ClusterPG]");
	    org_file.println("ClusterIdentifier String " + '"' + org_name + '"');
    	
	    // Print OrganizationPG (Roles) info
	    org_file.println("\n[OrganizationPG]");
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
	    org_file.println("\n[MilitaryOrgPG]");
	    org_file.println("UIC String " + '"' + org_data.myUIC + '"');
	    if (org_data.myEchelon.intValue() != -1)
	    {
            org_file.println("Echelon String " + '"' + org_data.myEchelon.intValue() + '"');
        } else {
            org_file.println("Echelon String " + '"'+'"');
        }
        org_file.println("UTC String " + '"' + org_data.myUTC + '"');
        org_file.println("SRC String " + '"' + org_data.mySRC + '"');
        if (org_data.myIsReserve == true)
        {
    	    org_file.println("IsReserve       boolean     true");
    	} else {
    	    org_file.println("IsReserve       boolean     false");
        }    	    
    	    
          
        // Print HomeLocationPG info under Military Org PG
        org_file.println("HomeLocation     GeolocLocation   "+
            "\"GeolocCode="+org_data.myHomeGeoLoc+", InstallationTypeCode="+
            org_data.myHomeInstallCode+", CountryStateCode="+org_data.myHomeCSCode+
            ", CountryStateName="+org_data.myHomeCSName+", IcaoCode="+
            org_data.myHomeICAOCode+", Name="+org_data.myHomeLocation+
            ", Latitude=Latitude "+org_data.myHomeLatitude+"degrees, Longitude=Longitude "
            +org_data.myHomeLongitude+"degrees\"");
          
        /*
        // Print AssignmentPG info
        org_file.println("\n[AssignmentPG]");
        org_file.println("GeolocCode            String    "+'"'+org_data.myAssignedGeoLoc+'"');
        org_file.println("InstallationTypeCode  String    "+'"'+org_data.myAssignedInstallCode+'"');
        org_file.println("CountryStateCode      String    "+'"'+org_data.myAssignedCSCode+'"');
        org_file.println("CountryStateName      String    "+'"'+org_data.myAssignedCSName+'"');
        org_file.println("IcaoCode              String    "+'"'+org_data.myAssignedICAOCode+'"');
        */
        // Print CSSCapabilities info
        if (org_data.myCSSCapabilities != null)
        {
	        is_first = true;
	        org_file.println("\n[CSSCapabilityPG]");
	        org_file.print("Capabilities Collection<CSSCapability> " + '"');
	        for(Enumeration eCap = org_data.myCSSCapabilities.elements();
	            eCap.hasMoreElements();)
	        {
	            CSSCapabilities cssCap = (CSSCapabilities)eCap.nextElement();
	            if (!is_first) {
	            org_file.print(", ");
	            }
	            org_file.print(cssCap.capability);
	            org_file.print(" "+cssCap.qty);
	            if(!cssCap.period.equals(""))
	            {
		        org_file.print(" Duration="+cssCap.period);
	            }
	            is_first = false;
	        }
	        org_file.println('"');
        }

        org_file.close();
        } catch (IOException exc) {
            System.out.println("IOException:  "+exc);
            System.exit(-1);
        }
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
