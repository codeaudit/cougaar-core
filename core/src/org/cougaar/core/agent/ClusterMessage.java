/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.core.agent;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import org.cougaar.core.mts.Message;
import org.cougaar.core.mts.MessageAddress;

/**
 * A org.cougaar.core.agent.ClusterMessage  provides a basic implementation of 
 *  ClusterMessage
 */

public class ClusterMessage 
  extends Message
{
  protected long theIncarnationNumber;

  /**
   * @param s The MessageAddress of creator cluster 
   * @param d The MessageAddress of the target cluster
   **/
  public ClusterMessage(MessageAddress s, MessageAddress d, long incarnationNumber) {
    super( (MessageAddress)s, (MessageAddress)d );
    theIncarnationNumber = incarnationNumber;
  }

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
   * @return Identifies the originator of this directive
   */
  public final MessageAddress getSource(){
    return getOriginator();
  }

  /**
   *	We provide the translation from the Object version in Message to the 
   *	Type sepecific version for the Cluster messageing subsystem.
   *  Mark it final to allow the compilier to inline optimize the function.
   *	@return Identifies the reciever of the directive
   */
  public final MessageAddress getDestination() {
    return getTarget();
  }
  
  /*
   *  Source is stored as na object so that message can service all objects.
   *  Mark it final to allow the compilier to inline optimize the function.
   * @param asource Set the MessageAddress of the originator of this message
   */
  public final void setSource(MessageAddress asource) {
    setOriginator( (MessageAddress)asource );
  }
  
  /*
   *  Target is stored as na object so that message can service all objects.
   *  Mark it final to allow the compilier to inline optimize the function.
   * @param adestination Set the MessageAddress of the receiver of this message
   */
  public final void setDestination(MessageAddress adestination) {
    setTarget( (MessageAddress)adestination );
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
