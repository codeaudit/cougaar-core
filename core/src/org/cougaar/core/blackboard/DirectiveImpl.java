/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.core.blackboard;

import org.cougaar.core.mts.MessageAddress;
import java.io.Serializable;

import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;

/**
 * A DirectiveImpl  provides a basic implementation of
 *  Directive for extension purposes only.
 */
public abstract class DirectiveImpl 
extends ClaimableImpl
implements Directive, NewDirective, Serializable
{

  protected MessageAddress source = null;
  protected MessageAddress destination = null;

  /** 
   */
  protected DirectiveImpl() {
    super();
  }
   
  //Directive interface method implementations
		
  /**
   * @return MessageAddress Identifies the originator of this message
   */
  public MessageAddress getSource() {
    return source;
  }

  /*
   *@return MessageAddress Identifies the receiver of the message
   */
  public MessageAddress getDestination() {
    return destination;
  }
  
  /*
   *	Depricated because it is inherited from the base interface Message
   * @param asource - Set the MessageAddress of the originator of this message
   */
  public void setSource(MessageAddress asource) {
    source = asource;
  }
  
  /*
   * @param adestination - Set the MessageAddress of the receiver of this message
   */
  public void setDestination(MessageAddress adestination) {
    destination = adestination;
  }


  //
  // implement read/write object here to provide top-level object stack implementations
  //
  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
  }

  private void readObject(ObjectInputStream stream)
    throws ClassNotFoundException, IOException
  {
    stream.defaultReadObject();
  }
}
