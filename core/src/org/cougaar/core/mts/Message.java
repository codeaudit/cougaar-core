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

package org.cougaar.core.mts;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

/**
 *   Class Is Basic Structure for Alp Message
 *   <p>
 *   All forms of Messages in the Alp system are derived from this base class
 *   originally Message
 **/
public abstract class Message 
  implements Serializable 
{

  private static final MessageAddress sink = MessageAddress.NULL_SYNC;
  /** This is a Reference target Object **/
  private MessageAddress theOriginator;
  /** This is a Reference target Object **/
  private MessageAddress theTarget;
  /** This is the sequence number **/
  private int theSequenceNumber = 0;

  /**
   *   Default Constructor for factory.
   **/
  public Message() {
    this( sink, sink, 0 );
  }

  /**
   *   Constructor with just the addresses
   *   <p>
   *   @param aSource The creator of this message used to consruct the super class
   *   @param aTarget The target for this message
   **/
  public Message(MessageAddress aSource, MessageAddress aTarget) {
    this(aSource, aTarget, 0);
  }

  /**
   *   Constructor with a full parameter list
   *   <p>
   *   @param aSource The creator of this message used to consruct the super class
   *   @param aTarget The target for this message
   *   @param anId Primative int value for message id used to create message
   **/
  public Message(MessageAddress aSource, MessageAddress aTarget, int anId) {
    setOriginator(aSource);
    setTarget(aTarget);
    setContentsId(anId);
  }
    
  /**
   *   Constructor for constructing a message form another message.
   *   <p>
   *   @param aMessage The message to use as the data source for construction.
   **/
  public Message(Message aMessage) {
    this(aMessage.getOriginator(),
         aMessage.getTarget(),
	 aMessage.getContentsId());
  }

  /**
   *    Accessor Method for theContentsId Property
   *    @return int the value of the standard message with intrinsics
   **/
  public final int getContentsId() {
    return theSequenceNumber;
  }

  /**
   *   Accessor Method for theOriginator Property
   *   @return Object Returns theOriginator object
   **/
  public final MessageAddress getOriginator() { return theOriginator; }
 
  /**
   *   Accessor Method for theTarget Property
   *   @return Object Returns the target object
   **/
  public final MessageAddress getTarget() { return theTarget; }

  /**
   *   Modify Method for theContentsId Property
   *   @param aContentsId The modifies theContentsId variable with the int primative
   **/
  public final void setContentsId(int aContentsId) {
    theSequenceNumber = aContentsId;
  }

  /**
   *   Modify Method for theOriginator Property
   *   @param aSource The modifies theOriginator variable with the Object object
   **/
  public final void setOriginator(MessageAddress aSource) { theOriginator = aSource; }

  /**
   *   Modify Method for theTarget Property
   *   @param aTarget The modifies theTarget variable with the Object object
   **/
  public final void setTarget(MessageAddress aTarget) { theTarget = aTarget; }

  /**
   *   Overide the toString implemntation for all message classes
   *   @return String Formatted string for displayying all the internal data of the message.
   **/
  public String toString()
  {
    try {
      return "The source: " + getOriginator().toString() +
        " The Target: " + getTarget().toString() +
        " The Message Id: " + getContentsId();
    } catch (NullPointerException npe) {
      String output = "a Malformed Message: ";
      if ( getOriginator() != null )
        output += " The source: " + getOriginator().toString();
      else
        output += " The source: NULL";
      if ( getTarget() != null )
        output += "The Target: " + getTarget().toString();
      else  
        output += " The Target: NULL";

      return output;
    }
  }

  // externalizable support
  // we don't actually implement the Externalizable interface, so it is
  // up to subclasses to call these methods.
  public void writeExternal(ObjectOutput out) throws IOException {
    out.writeObject(theOriginator);
    out.writeObject(theTarget);
    out.writeInt(theSequenceNumber);
  }

  public void readExternal(ObjectInput in) throws ClassNotFoundException, IOException {
    theOriginator=(MessageAddress)in.readObject();
    theTarget=(MessageAddress)in.readObject();
    theSequenceNumber = in.readInt();
  }
}

