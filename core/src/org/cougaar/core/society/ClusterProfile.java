/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.society;

import java.io.*;

import java.util.Vector;
import org.cougaar.util.ConfigFileFinder;

/**
*  This class server as the persistent data structure that defines the current running
*   configuration of the Cluster.
*   <p>
*   Each Cluster has a profile that is persistent and a copy that is currently in use.
*
**/
public final class ClusterProfile extends CitizenProfile{

  /** The Alias name for the Cluster that owns this profile.**/
  private String theUIC;
  /**  plugin registry for the cluster.  */
  private Vector thePluginRegistry;
  /**  plugin registry for the cluster.  */
  private Vector theComponentList;
  /** The cluster full dot notated class name. **/
  private String theClusterClassName;
  /** The constext object to use for this cluster. **/
  private boolean theClonedFlag;
	
   
  /**
   *   Constructor no parametes.  Creates a Clusterprofile with the
   *   @see org.cougaar.core.cluster.ClusterImpl
   **/
  public ClusterProfile( String anAlias ){
    super( anAlias );
    setAlias( anAlias );
    buildProfile();
  }

  /*
   *	The method to build a profile object when there is no object in the db.
   *	for this interation we will simply parse the data from a text file called <NODE ALIAS>.ini
   *	<p>
   *	In the future construction and storage of profiles will become the responsibility of the SANode.
   **/
  private void buildProfile() {
    setComponentList( new Vector() );
    setClonedFlag( false );
    setPluginRegistry( new Vector() );
    parseIniFile();
  }

  /** Accessor method for theClonedFlag attribute.
   *   <p>
   *   @return boolean The boolean primative that is referenced by theClonedFlag attribute.   */
  public final boolean getClonedFlag() { return theClonedFlag; }

  /** Accessor method for theClusterClassName attribute.
   *   <p>
   *   @return String The String object that is referenced by theClusterClassName attribute .   */
  public final String getClusterClassName() { return theClusterClassName; }

  /** Accessor method for theComponentList attribute.
   *   <p>
   *   @return Vector The Vector object that is referenced by theComponentList attribute .  */
  public final Vector getComponentList() { return theComponentList; } 
    
  /** Accessor method for thePluginRegistry attribute.
   *   <p>
   *   @return Vector The Vector object that is referenced by thePluginRegistry attribute.   */
  public final Vector getPluginRegistry() { return thePluginRegistry; } 

  /** Accessor method for theUIC attribute.
   *   <p>
   *   @return String The String object that is referenced by theUIC attribute.   */
  public final String getUIC() { return theUIC; }

  /**
   *	Method to parse the values out of the INI file and construct the node profile.
   *	<p>
   *	This is a temporary method that enables the data to be stored in a simple text file until 
   *	we construct the SANode and profile construction objects.
   **/
  private void parseIniFile(){
    //System.out.println( " ClusterProfile::parseIniFile -- enrty point" );
    try {
      BufferedReader in = new BufferedReader( new InputStreamReader( ConfigFileFinder.open( getAlias() + ".ini" ) ) );
      String sCheck;
      while( (sCheck = in.readLine()) != null ) {
        int nIndex = sCheck.indexOf( "=" );
        if (nIndex != -1)
          setValue( sCheck.substring( 0, nIndex ).trim(), sCheck.substring( nIndex + 1 ).trim() );
      }
      in.close();
    }
    catch(IOException e) {
      System.out.println("Error: " + e);
    }		

  }

  /** Modifier method for theClonedFlag attribute.
   *   <p>
   *   @param aClonedFlag The boolean primative to store in the theClonedFlag attribute.   */
  public final void setClonedFlag( boolean aClonedFlag ) { theClonedFlag = aClonedFlag; }

  /** Modifier method for theClusterClassName attribute.
   *   <p>
   *   @param aClusterClassName The String object to reference with theClusterClassName attribute.   */
  public final void setClusterClassName( String aClusterClassName ) { theClusterClassName = aClusterClassName; }

  /** Modifier method for theComponentList attribute.
   *   <p>
   *   @param Vector The Vector object to reference with theComponentList attribute .  */
  private final void setComponentList( Vector aComponentList ) { theComponentList = aComponentList; }

  /** Modifier method for thePluginRegistry attribute.
   *   <p>
   *   @param aPluginRegistry The Vector object to reference with thePluginRegistry attribute.   */
  private final void setPluginRegistry( Vector aPluginRegistry ) { thePluginRegistry = aPluginRegistry; }

  /** Modifier method for theUIC attribute.
   *   <p>
   *   @param anUIC The String object to reference with theUIC attribute.   */
  public final void setUIC( String anUIC ) { theUIC = anUIC; }

  /**
   *	The abstracted modifier method that takes a key and a value from the .ini file and
   *	maps it to the correct modifier method.  Ultimate this should be part of the application level
   *	configuration component and we should merely ask the running application for these values.
   *	<p>
   *	@param sKey  The String object that contains the key value from the ini file.
   *	@param sValue  The String object that contains the key value from the ini file.
   *	We should update tthis tot use the LDM factory when we complete this.
   **/
  protected void setValue( String sKey, String sValue ) {
    if( sKey.equals("class") )
      setClusterClassName( sValue );
    else if( sKey.equals( "cloned" ) )
      setClonedFlag( (sValue.equals("true") ? true : false) ) ;
    else if( sKey.equals( "plugin" ) )
      getPluginRegistry().addElement( sValue );
    else if( sKey.equals( "component" ) ) {
      //getComponentList().addElement( sValue );
      System.err.println("Warning: cluster "+getAlias()+" ignoring addComponent "+sValue);
    }
    else if( sKey.equals( "uic" ) )
      setUIC( sValue );
    else
      super.setValue( sKey, sValue );
  }
    
}


