/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.cluster;

import org.cougaar.core.society.Message;
import java.io.Serializable;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;


/**
 * A org.cougaar.core.cluster.ClusterMessage  provides a basic implementation of 
 *  ClusterMessage
 */

public class ClusterMessage 
  extends Message
{
  protected long theIncarnationNumber;

  /**
   *   Constructor
   *   <p>
   *   @param source The ClusterIdentifier of creator cluster 
   *		@param destination The ClusterIdentifier of the target cluster
   *   @return org.cougaar.core.cluster.ClusterMessage
   **/
  public ClusterMessage(ClusterIdentifier s, ClusterIdentifier d, long incarnationNumber) {
    super( s, d );
    theIncarnationNumber = incarnationNumber;
  }
    
  /** 
   *	no-arg Constructor.
   * 	This is not generally allowed in 1.1 event handling because EventObject requires
   *	a source object during construction.  Base class does not support this type of 
   *	construction so it cannot be done here.
   *  	@return org.cougaar.core.cluster.ClusterMessage
   */
  public ClusterMessage() {
    super();
  }

  public long getIncarnationNumber() {
    return theIncarnationNumber;
  }

  public void setIncarnationNumber(long incarnationNumber) {
    theIncarnationNumber = incarnationNumber;
  }

  /**
   *  We provide the translation from the object version.  Unfortunately we cannot return
   *	a different type in java method overloading so the method signature is changed.
   *  Mark it final to allow the compilier to inline optimize the function.
   * @return ClusterIdentifier Identifies the originator of this directive
   */
  public final ClusterIdentifier getSource(){
    return (ClusterIdentifier)getOriginator();
  }

  /**
   *	We provide the translation from the Object version in Message to the 
   *	Type sepecific version for the Cluster messageing subsystem.
   *  Mark it final to allow the compilier to inline optimize the function.
   *	@return ClusterIdentifier Identifies the reciever of the directive
   */
  public final ClusterIdentifier getDestination() {
    return (ClusterIdentifier)getTarget();
  }
  
  /*
   *  Source is stored as na object so that message can service all objects.
   *  Mark it final to allow the compilier to inline optimize the function.
   * @param asource - Set the ClusterIdentifier of the originator of this message
   */
  public final void setSource(ClusterIdentifier asource) {
    setOriginator( asource );
  }
  
  /*
   *  Target is stored as na object so that message can service all objects.
   *  Mark it final to allow the compilier to inline optimize the function.
   * @param adestination - Set the ClusterIdentifier of the receiver of this message
   */
  public final void setDestination(ClusterIdentifier adestination) {
    setTarget( adestination );
  }

  public String toString() {
    return "<ClusterMessage "+getSource()+" - "+getDestination()+">";
  }

  public void writeExternal(ObjectOutput out) throws IOException {
    super.writeExternal(out);
    out.writeLong(theIncarnationNumber);
  }

  public void readExternal(ObjectInput in) throws ClassNotFoundException, IOException {
    super.readExternal(in);
    theIncarnationNumber = in.readLong();
  }
}
