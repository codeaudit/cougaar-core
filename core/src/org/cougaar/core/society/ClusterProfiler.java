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

import org.cougaar.core.cluster.ClusterServesClusterManagement;
import java.util.Enumeration;

/**
*   This class is responsible for handling all configuration issues for the Cluster.
*   This is the class resposible changing the persistent state of a cluster.
**/
public final class ClusterProfiler extends CitizenProfiler {

  /**
   *   chained Constructor.
   *	<p>
   *	@param aClusterAlias The string object that contains the alias for this Cluster in the Registry.
   **/
  public ClusterProfiler( String aClusterAlias ){
    this( aClusterAlias, (ClusterProfile)null );
  }
    
  /**
   *	Constructor with a persistent profile.
   *	<p>
   *	@param aClusterAlias The string object that contains the alias for this Cluster in the Registry.
   *	@param aProfile The ClusterProfile object used to construct this cluster.
   **/
  public ClusterProfiler( String aClusterAlias, ClusterProfile aProfile ){
    super();
    if( aProfile == null )
      aProfile = createProfile( aClusterAlias );
    setProfile( aProfile );
  }

  /**  This method enables the ClusterManagement to instatiate the Cluster and add it
   *   as a ClusterAsset.  This is accomplished from the Class class methods and the
   *   data present in the profiler.
   *   <p><PRE>
   *       PRE CONDITION:    Cluster object is not created.
   *       POST CONDITION:   Creates the cluster object.
   *       INVARIANCE:       Profile is not altered.
   *   </PRE>
   *	@return ClusterServesClusterManagement The reference to the new cluster doencasted to its interface.
   *	@exception ClassNotFoundException If Class is not found by the ClassLoader in the current CLASSPATH.
   *	@exception InstantiationException If the Class can not be instantiated by indirection.
   *	@exception IllegalAccessException If the class is illegally accessed during creation.
   */
  public ClusterServesClusterManagement createCluster() throws ClassNotFoundException, InstantiationException, IllegalAccessException  {
    ClusterServesClusterManagement myCluster = null;
    Class myClass = Class.forName( getClusterProfile().getClusterClassName() );
    Object myObject = myClass.newInstance();
    if( myObject instanceof ClusterServesClusterManagement ) 
      myCluster = (ClusterServesClusterManagement)myObject;
     
    if( myCluster == null )
      throw new ClassNotFoundException();
       
    return myCluster;
  }

  /**
   *   Create a new Cluster profile if there is no persistent copy located.
   *   <p><PRE>
   *       PRE CONDITION:    Two states persistent profile exists or does not exists.
   *       POST CONDITION:   If it exists bring it into the program,.
   *                         If not then create it and make it persistent.
   *       INVARIANCE:       Profile is not altered.
   *   </PRE>
   *	We are currently only supporting the non persistent state until the DB is added.
   *	@param aClusterAlias The String object containing the Clusters Alias.
   *	@return ClusterProfile  Either the peristed profile object or the newly constructed object.
   **/
  private ClusterProfile createProfile( String aClusterAlias ){
    ClusterProfile myProfile = new ClusterProfile( aClusterAlias );
    //  add the check to the DB for a persistent copy of the profile object.
    return myProfile;
  }

  /**
   *   Retreive the enumeration of Component List.
   *   <p><PRE>
   *       PRE CONDITION:    Profile exisits.
   *       POST CONDITION:   Returns the enumeration of the list.
   *       INVARIANCE:       List is not altered.
   *   </PRE>
   *	@return Enumeration The Enumeration object of all the components in the profile object.
   **/
  public final Enumeration enumerateComponents( ){
    return getClusterProfile().getComponentList().elements();
  }
  /**
   *   Retreive the enumeration of PlugIn Registry
   *   <p><PRE>
   *       PRE CONDITION:    Profile exisits
   *       POST CONDITION:   Returns the enumeration of the list
   *       INVARIANCE:       List is not altered
   *   </PRE>
   *	@return Enumeration The Enumeration object of all the plugins in the profile object.
   **/
  public final Enumeration enumeratePlugins( ){
    return getClusterProfile().getPluginRegistry().elements();
  }
    
  /**
   *   Accessor method for theProfile
   *   <p>
   *   @return NodeProfile The object pointed to by theProfile variable
   **/
  public final ClusterProfile getClusterProfile() { return (ClusterProfile)super.getProfile(); }


}


