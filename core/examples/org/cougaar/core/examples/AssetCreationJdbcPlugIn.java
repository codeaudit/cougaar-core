package org.cougaar.util;

/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */


import java.sql.*;
import java.util.*;
import org.cougaar.domain.planning.ldm.asset.*;

// Simple plugin to read a JDBC datasource
// and create instances of specified prototypes

// Data table schema:
// create table Assets (
//    Organization String,
//    Prototype String,
//    UniqueID String,
//    Quantity Int  -- If > 1, autogenerate ID's using UniqueID as prefix
// );
//
public class AssetCreationJdbcPlugIn extends org.cougaar.core.plugin.SimplePlugIn
{
  // Perform the initial query for instance directives, and create objects
  // accordingly
  public void setupSubscriptions()
  {
    // Set up database access parameters
    Vector params = getParameters();

    String driver_classname = 
      (params.size() >= 1 ? (String)getParameters().elementAt(1) : 
       "sun.jdbc.odbc.JdbcOdbcDriver");
    String datasource_url = 
      (params.size() >= 2 ? (String)getParameters().elementAt(2) :
       "jdbc:odbc:ASSETS");
    String datasource_username = 
      (params.size() >= 3 ? (String)getParameters().elementAt(3) : "");
    String datasource_password = 
      (params.size() >= 4 ? (String)getParameters().elementAt(4) : "");

    createAssets(driver_classname, datasource_url, 
		 datasource_username, datasource_password);

  }

  /**
   * Create assets by connecting to 'Assets' table (as above) 
   * specified by given driver/url/user/password
   * Create instance for each prototype listed for this organization
   * Auto-generate ID's from given if quantity > 1
   */
  private void createAssets(String driver_classname, 
			    String datasource_url,
			    String datasource_username, 
			    String datasource_password)
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
	  DriverManager.getConnection(datasource_url, 
				      datasource_username, 
				      datasource_password);

	Statement stmt = conn.createStatement();

	String myOrgName = getClusterIdentifier().getAddress();

	String query = 
	  "select Organization, Prototype, UniqueID, Quantity from Assets"; 
	ResultSet rset = stmt.executeQuery(query);

	while(rset.next()) {
	  String Organization = rset.getString(1);
	  String Prototype = rset.getString(2);
	  String UniqueID = rset.getString(3);
	  int Quantity = rset.getInt(4);

	  if (Organization.equals(myOrgName)) {
	    for(int i = 1; i<=Quantity; i++) {
	      String newID = UniqueID;

	      // Generate a new ID for each element in quantity, using
	      // UniqueID as prefix.
	      // '0-pad' the string based on string length of quantity value
	      if (Quantity > 1) {
		int quantity_length = Integer.toString(Quantity).length();
		String count = Integer.toString(i);
		while(count.length() < quantity_length) 
		  count = '0' + count;
		newID = UniqueID + count;
	      }

	      // Create instance from prototype and ID
	      Asset asset = theLDMF.createInstance(Prototype, newID);
	      //	      System.out.println("Asset = " + asset);
	      publishAdd(asset);
	    }
	  }
	}

	conn.close();
      }
    catch (SQLException sqle)
      {
	System.out.println("SQL Exception");
	sqle.printStackTrace();
      }
    
  }

  // This plugin doesn't need to run in real-time, just at startup time
  public void execute()
  {
  }

}

