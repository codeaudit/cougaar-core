/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.society;

import java.io.*;

import java.util.Vector;
import org.cougaar.util.ConfigFileFinder;

/**
*  This class server as the persistent data structure that defines the current running
*   configuration of the node.
*   <p>
*   Each node has a profile that is persistent and a copy that is currently in use.
*
**/
public final class NodeProfile extends CitizenProfile{

  /** The Alias name for the node that owns this profile. **/
  private String theFileName;
  /**  The reference to the Set of all cluster class names.
   *   Names are Strings of the clusters alias for example 3rd ID. **/
  private Vector theClusterNames;
  /**  The reference to the Set of all processes class names.
   *   Names are Strings that include the complete java name (ie "java.lang.String"
   *   for the String class.**/
  private Vector theProcessClassNames;
  /** The primative value for the required amount of RAM in bytes 2MB?? 800000 bytes. **/
  private long theRAMSize;
  /** The primative value for the required free disk space in bytes 2MB??**/
  private long theFreeDiskSize;

  /**
   *   Consructor .
   *   <p>
   *   Pass the default values of tfor the filename to the next chained constructor.
   *	@param anAlias The alias name for the node.
   **/
  public NodeProfile( String anAlias){
    this( anAlias, anAlias );
  }

  /**
   *   Chained consructor .
   *   <p>
   *   pass the default values of the RAM and Disk Space to the chained constructor.
   *	@param anAlias The alias name for the node.
   *	@param fileName The file or persistent alias name to use to consruct this object.
   **/
  public NodeProfile( String anAlias, String fileName){
    this( anAlias, fileName, 126000, 2097152 );
  }

  /**
   *   Chained Constructor.  End of the chain this constructer is always executed.
   *   <p>
   *   @param anAlias The alias name for the node.
   *	@param fileName The file or persistent alias name to use to consruct this object.
   *	@param aRAMSize The RAM size required to operate the node.
   *	@param aDiskSize Teh disk size requires to support this node.
   **/
  public NodeProfile(String anAlias, String fileName, long aRAMSize, long aDiskSize ){
    super( anAlias );
    setFileName( fileName );
    setRAMSize( aRAMSize );
    setFreeDiskSize( aDiskSize );
    buildProfile();
  }
	
  /*
   *	The method to build a profile object when there is no object in the db.
   *	for this interation we will simply parse the data from a text file called <NODE ALIAS>.ini
   *	<p>
   *	In the future construction and storage of profiles will become the responsibility of the SANode.
   **/
  private void buildProfile() {
    setClusterNames( new Vector() );
    setProcessClassNames( new Vector() );
    parseIniFile();
  }

  /**
   *   Accessor method for theClusterClassNames OrderedSet property.
   *   <p>
   *   @return ListOfString The odmg 2.0 compliant container of cluste class names.
   **/
  public final Vector getClusterNames() { return theClusterNames; }

  /**   Accessor method for theFileName attribute.
   *   <p>
   *   @return String The String object that is referenced by theFileName attribute .  */
  public final String getFileName() { return theFileName; }
    
  /**
   *   Accessor method for theFreeDiskSize long property.
   *   <p>
   *   @return long primative that hold the required amount of free disk space.
   **/
  public final long getFreeDiskSize() { return theFreeDiskSize; }

  /**
   *   Accessor method for theProcessClassNames OrderedSet property.
   *   <p>
   *   @return Vector The odmg 2.0 compliant container of Process Class names.
   **/
  public final Vector getProcessClassNames() { return theProcessClassNames; }

  /**
   *   Accessor method for theRAMSize long property.
   *   <p>
   *   @return long primative that hold the required amount of free RAM space.
   **/
  public final long getRAMSize() { return theRAMSize; }

  /**
   *	Method to parse the values out of the INI file and construct the node profile.
   *	<p>
   *	This is a temporary method that enables the data to be stored in a simple text file until 
   *	we construct the SANode and profile construction objects.
   **/
  private void parseIniFile(){
    try {
      BufferedReader in = new BufferedReader( new InputStreamReader( ConfigFileFinder.open( getFileName() ) ) );
      String sCheck;
      while( (sCheck = in.readLine()) != null ) {
        int nIndex = sCheck.indexOf( "=" );
        if( nIndex != -1)
          setValue( sCheck.substring( 0, nIndex ).trim(), sCheck.substring( nIndex + 1 ).trim() );
      }
      in.close();
    }
    catch(IOException e) {
      System.out.println("Error: " + e);
    }		

  }
	
  /**
   *   Modifier method for theClusterNames OrderedSet property.
   *   <p>
   *   @param someClusterNames The odmg 2.0 compliant container of Cluster names objects.
   **/
  public final void setClusterNames( Vector someClusterNames ) { theClusterNames = someClusterNames; }

  /** Modifier method for theFileName attribute.
   *   <p>
   *   @param aFileName The String object to reference with theFileName attribute.   */
  private final void setFileName( String aFileName ) { theFileName = aFileName; }
    
  /**
   *   Modifier method for theFreeDiskSize long property.
   *   <p>
   *   @param aDiskSize  The long primative value for amount of free disk space in bytes.
   **/
  public final void setFreeDiskSize( long aDiskSize ) { theFreeDiskSize = aDiskSize; }

  /**
   *   Modifier method for theProcessClassNames OrderedSet property.
   *   <p>
   *   @param anAdminList The odmg 2.0 compliant container of Alp Process class names objects.
   **/
  public final void setProcessClassNames( Vector someProcessesClassNames ) { theProcessClassNames = someProcessesClassNames; }

  /**
   *   Modifier method for theRAMSize long property.
   *   <p>
   *   @param aRAMSize  The long primative value for amount of free disk space in bytes.
   **/
  public final void setRAMSize( long aRAMSize ) { theRAMSize = aRAMSize; }


  /**
   *	The abstracted modifier method that takes a key and a value from the .ini file and
   *	maps it to the correct modifier method.  Ultimate this should be part of the application level
   *	configuration component and we should merely ask the running application for these values
   *	<p>
   *	@param sKey  The String object that contains the key value from the ini file.
   *	@param sValue  The String object that contains the key value from the ini file.
   **/
  protected void setValue( String sKey, String sValue ) {
    //System.out.println( "Registry::setValue - enrty. Key = " + sKey + " Value = " + sValue );
    if( sKey.equals("cluster") )
      getClusterNames().addElement( sValue );
    else if( sKey.equals( "process" ) )
      getProcessClassNames().addElement( sValue ) ;
    else 
      super.setValue( sKey, sValue );
        	
  }
 
}


