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

import java.util.Enumeration;
import java.io.File;

/**
*   This class is responsible for handling all configuration issues for the Node.
*   This is the class resposible changing the persistent state of a node.
**/
public final class NodeProfiler extends CitizenProfiler{
    /**
    *   Chained Constructor.
    *	<p>
    *	@param aNodeAlias The String object containing the alias for the node.
    *	Use the alias and the .ini ending to construct the next level of parameters.
    **/
    public NodeProfiler( String aNodeAlias ){
    	this( aNodeAlias, aNodeAlias + ".ini" );
    }
    
    /**
    *   Chained Constructor.
    *	<p>
    *	@param aNodeAlias The String object conatining the alias for the node.
    *	Use the alias and the .ini ending to construct the next level of parameters.
    *	@param aFileName The String object containing the filename to use in constructing the profile object.
    **/
    public NodeProfiler( String aNodeAlias, String aFileName){
    	this( aNodeAlias, aFileName,  (NodeProfile)null );
    }
    
    /**
    *	Constructor with a persistent profile.
    *	<p>
    *	@param aNodeAlias The String object conatining the alias for the node.
    *	Use the alias and the .ini ending to construct the next level of parameters.
    *	@param aFileName The String object containing the filename to use in constructing the profile object.
    *	@param aProfile  The profile object to use with this profiler.
    **/
    public NodeProfiler( String aNodeAlias, String aFileName, NodeProfile aProfile ){
    	super();
    	if( aProfile == null )
    		aProfile = createProfile( aNodeAlias, aFileName );
   		setProfile( aProfile );
   	}

    /**
    *   Method to add a new cluster to the node's configuration
    *   String must contain the full java refernce to the class ( ie java.lan.String )
    *   <p><PRE>
    *       PRE CONDITION:    New Cluster class name to add
    *       POST CONDITION:   New cluster class name added and made persistent
    *       INVARIANCE:
    *   </PRE>
    *   @param aCluster the String object to add to the collection
    **/
    public final void addCluster( String aCluster ){
        getNodeProfile().getClusterNames().addElement(aCluster);
    }

    /**
    *   Method to add a new process to the node's configuration
    *   String must contain the full java refernce to the class ( ie java.lan.String )
    *   <p><PRE>
    *       PRE CONDITION:    New process class name to add
    *       POST CONDITION:   New process class added and made persistent
    *       INVARIANCE:
    *   </PRE>
    *   @param aProcess the String object to add to the collection
    **/
    public final void addProcess( String aProcess ){
        getNodeProfile().getProcessClassNames().addElement(aProcess);
    }

    /**
    *   Create a new Node profile if thier is no persistent copy located
    *   <p><PRE>
    *       PRE CONDITION:    Two states persistent profile exists or does not exists
    *       POST CONDITION:   If it exists bring it into the program,
    *                         If not then create it and make it persistent
    *       INVARIANCE:       Profile is not altered
    *   </PRE>
    *	@param aNodeAlias A String object hat contians the Node's alias.
    *	@return NodeProfile Either the persistent version of NodeProfile or create anew one from the .ini files.
    **/
    private NodeProfile createProfile( String aNodeAlias, String fileName){
        NodeProfile myProfile = null;
        if(myProfile != null) { //check the db for copy
        }
        else{
            myProfile = new NodeProfile( aNodeAlias, fileName );
            //make it persistent
        }

        return myProfile;
	}

    /**
    *   Accessor method for theProfile
    *   <p>
    *   @return NodeProfile The object pointed to by theProfile variable
    **/
    public final NodeProfile getNodeProfile() { return (NodeProfile)super.getProfile(); }

    /**
    *   Method to remove a new cluster to the node's configuration
    *   String must contain the full java refernce to the class ( ie java.lan.String )
    *   <p><PRE>
    *       PRE CONDITION:    cluster class file name to remove
    *       POST CONDITION:   cluster class file name removed and made persistent
    *       INVARIANCE:
    *   </PRE>
    *   @param aCluster the String object to remove from the collection
    **/
    public final void removeCluster( String aCluster ){
        getNodeProfile().getClusterNames().removeElement(aCluster);
    }

    /**
    *   Method to remove a new process to the node's configuration
    *   String must contain the full java refernce to the class ( ie java.lan.String )
    *   <p><PRE>
    *       PRE CONDITION:    AlpProcess  to remove
    *       POST CONDITION:   AlpProcess removed and made persistent
    *       INVARIANCE:
    *   </PRE>
    *   @param aProcess the AlpProcess object to remove from the collection
    **/
    public final void removeProcess( String aProcess ){
        getNodeProfile().getProcessClassNames().removeElement(aProcess);
    }

}


