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


// MEIWriter program : 
// Read MEI database file and create XML prototype files

// Note that the file can be read from any JDBC driver for this schema, 
// including any JDBC driver.
// In particular, intended for MS EXCEL/ACCESS files using ODBC/JDBC

// Database Schema:
// create table MEI (
//   Description String,
//   NSN String,
//   Alternate String,
//   Length Double,
//   Width Double,
//   Height Double,
//   FootprintArea Double,
//   Volume Double,
//   Mass Double,
//   Moveable Boolean,
//   CargoCategoryCode String
// );

class MEIWriter {

  // Top level 'main' test program
  // Usage : java MEIWriter <driver_name> <db_url> <db_user> <db_password>
  // Default arguments refer to ODBC connection named 'MEI'
  public static void main(String args[]) throws SQLException
  {
    String driver_name = 
      (args.length >= 1 ? args[0] : "sun.jdbc.odbc.JdbcOdbcDriver");
    String database_url = 
      (args.length >= 2 ? args[1] : "jdbc:odbc:MEI");
    String database_user = 
      (args.length >= 3 ? args[2] : "");
    String database_password = 
      (args.length >= 4 ? args[3] : "");
    System.out.println("MEIWriter : " + 
		       driver_name + "/" + database_url + "/" + 
		       database_user + "/" + database_password);
    new MEIWriter().parseMEIData
      (driver_name, database_url, database_user, database_password);
  }

  // Parse MEI data from given database
  // Read info and write appropriate .ini and .dat files
  // Given classname of JDBC driver, URL of database and user/password
  protected void parseMEIData(String driver_classname,
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

	ResultSet rset = 
	  stmt.executeQuery("select Description, NSN, Alternate, " + 
			    "Length, Width, " + 
			    "Height, FootprintArea, Volume, Mass, " + 
			    "Moveable, CargoCategoryCode from MEI");

	
	while(rset.next()) {
	  desc = rset.getString(1);
	  nsn = rset.getString(2);
	  alternate = rset.getString(3);
	  length = rset.getDouble(4);
	  width = rset.getDouble(5);
	  height = rset.getDouble(6);
	  footprint_area = rset.getDouble(7);
	  volume = rset.getDouble(8);
	  mass = rset.getDouble(9);
	  moveable = rset.getBoolean(10);
	  cargo_category_code = rset.getString(11);

	  String filename  = generateFileName(nsn);
	  System.out.println("Generating XML prototype file : " + 
			     filename + " for " + desc);

	  PrintWriter pw = createPrintWriter(filename);

	  writePrototype(pw);

	  pw.close();
	}

	conn.close();
      }
    catch (IOException ie) 
      {
	System.out.println("IO Exception");
	ie.printStackTrace();
      }
    catch (SQLException sqle)
      {
	System.out.println("SQL Exception");
	sqle.printStackTrace();
      }
  }

  private void writePrototype(PrintWriter pw)
  {
    print(pw, "<prototype name = " + '"' + nsn + '"' + ">", 0);
    print(pw, "<object class = " + '"' + "org.cougaar.domain.planning.ldm.asset.Asset" + '"' + ">", 2);
    writeTypeIdentificationPG(pw);
    writePhysicalPG(pw);
    if (moveable) {
      writeMovabilityPG(pw);
    }
    print(pw, "</object>", 2);
    print(pw, "</prototype>", 0);
  }

  private void writeTypeIdentificationPG(PrintWriter pw)
  {
    writeComplexFieldStart(pw, "TypeIdentificationPG", 
			   "org.cougaar.domain.planning.ldm.asset.NewTypeIdentificationPG");
    writeSimpleField(pw, "TypeIdentification", "String", desc);
    writeSimpleField(pw, "Nomenclature", "String", 
		     "NSN/" + removeChar(nsn, '-'));
    writeSimpleField(pw, "AlternateTypeIdentification", "String", alternate);
    writeComplexFieldEnd(pw);
  }

  private void writePhysicalPG(PrintWriter pw) 
  {
    writeComplexFieldStart(pw, "PhysicalPG", "org.cougaar.domain.planning.ldm.asset.NewPhysicalPG");
    writeMeasureField(pw, "Length", "org.cougaar.domain.planning.ldm.measure.Distance", 
		      "Inches", length);
    writeMeasureField(pw, "Width", "org.cougaar.domain.planning.ldm.measure.Distance", 
		      "Inches", width);
    writeMeasureField(pw, "Height", "org.cougaar.domain.planning.ldm.measure.Distance", 
		      "Inches", height);
    writeMeasureField(pw, "Mass", "org.cougaar.domain.planning.ldm.measure.Mass", 
		      "Pounds", mass);
    writeMeasureField(pw, "FootprintArea", "org.cougaar.domain.planning.ldm.measure.Area", 
		      "SquareFeet", footprint_area);
    writeMeasureField(pw, "Volume", "org.cougaar.domain.planning.ldm.measure.Volume", 
		      "CubicFeet", volume);
    writeComplexFieldEnd(pw);
  }

  private void writeMovabilityPG(PrintWriter pw) 
  {
    writeComplexFieldStart(pw, "MovabilityPG", 
			   "org.cougaar.domain.planning.ldm.asset.NewMovabilityPG");
    writeSimpleField(pw, "Moveable", "boolean", (moveable ? "True" : "False"));
    writeSimpleField(pw, "CargoCategoryCode", "String", cargo_category_code);
    writeComplexFieldEnd(pw);

  }

  private void writeComplexFieldStart(PrintWriter pw,
				      String type_name,
				      String class_name)
  {
    print(pw, "<field name=" + '"' + type_name + '"' + 
	       " type = " + '"' + "object" + '"' + ">", 4);
    print(pw, "<value>", 6);
    print(pw, "<object class = " + '"' + class_name + '"' + ">", 8);
  }

  private void writeComplexFieldEnd(PrintWriter pw)
  {
    print(pw, "</object>", 8);
    print(pw, "</value>", 6);
    print(pw, "</field>", 4);
  }

  private void writeMeasureField(PrintWriter pw,
				 String field_name,
				 String measure_class,
				 String units,
				 double value)
    {
      print(pw, "<field name = " + '"' + field_name + '"' + 
		 " type=" + '"' + "object" + '"' + ">", 4);
      print(pw, "<value>", 6);
      print(pw, "<object class=" + '"' + measure_class + '"' + ">", 8);
      print(pw, "<field name=" + '"' + units + '"' + 
		 " type=" + '"' + "double" + '"' + ">", 10);
      print(pw, "<value>" + value + "</value>", 12);
      print(pw, "</field>", 10);
      print(pw, "</object>", 8);
      print(pw, "</value>", 6);
      print(pw, "</field>", 4);
    }

  private void writeSimpleField(PrintWriter pw,
				String field_name,
				String field_type,
				String field_value)
  {
    print(pw, "<field name=" + '"' + field_name + '"' + 
	  " type = " + '"' + field_type + '"' + ">", 10);
    print(pw, "<value>" + field_value + "</value>", 12);
    print(pw, "</field>", 10);
  }


  // Utility functions

  // Create a PrintWriter class for given filename
  private PrintWriter createPrintWriter(String filename) throws IOException
  {
    return 
      new PrintWriter(new OutputStreamWriter(new FileOutputStream(filename)));
  }

  // Print a string with given indentation
  private void print(PrintWriter pw, String text, int indent)
  {
    String indent_string = "";
    for(int i = 0; i < indent; i++) indent_string = indent_string + ' ';
    pw.println(indent_string + text);
  }

  private String removeChar(String original, char to_remove) 
  {
    String stripped = "";
    for(int i = 0; i < original.length(); i++) {
      char c = original.charAt(i);
      if (c != to_remove)
	stripped = stripped + c;
    }
    return stripped;
  }

  // Turn an nsn into a filename by removing hyphens, 
  // adding 'NSN-' prefix, and '.xml suffix
  private String generateFileName(String nsn) 
       
  {
    return "NSN-" + removeChar(nsn, '-') + ".xml";
  }

    // private instance variables for storing information from table rows
  String desc;
  String nsn;
  String alternate;
  double length;
  double width;
  double height;
  double footprint_area;
  double volume;
  double mass;
  boolean moveable;
  String cargo_category_code;

}









