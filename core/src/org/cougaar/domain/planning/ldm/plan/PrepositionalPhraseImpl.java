/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
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

package org.cougaar.domain.planning.ldm.plan;

import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.NotSerializableException;

/**
 * Setters for PrepositionalPhrase. 
 */

public class PrepositionalPhraseImpl 
  implements PrepositionalPhrase, NewPrepositionalPhrase, java.io.Serializable
{

  private String preposition;
  private transient Object indirectobject; // changed to transient : Persistence

  // no-arg constructor
  public PrepositionalPhraseImpl() { }

  /** @return Answer with a nicely formatted string representation. */
  public String toString()
  {
    if ( indirectobject == null && preposition == null)
      return "[ a null valued prepositional phrase]";
    else if ( indirectobject == null )
      return  preposition + " <null>";
    else
      return preposition + " " + indirectobject.toString() ;
  }

  /** PrepositionalPhrase interface implementations */
	
  /**@return String - String representation of the Preposition */
  public String getPreposition() {
    return preposition;
  }
	
  /** @return Object - the IndirectObject */
  public Object getIndirectObject() {
    return indirectobject;
  }
  
  /** NewPrepositionalPhrase interface implementations */
  	
  /**@param apreposition - Set the String representation of the Preposition */
  public void setPreposition(String apreposition) {
    if (apreposition != null) apreposition = apreposition.intern();
    preposition = apreposition;
  }
	
  /** @param anindirectobject - Set the IndirectObject of the PrespositionalPhrase */
  public void setIndirectObject(Object anindirectobject ) {
    indirectobject = anindirectobject;
  }
	
  /** PP are equals() IFF prepositions are the same and 
   * indirectobjects are .equals()
   */
  public boolean equals(Object o) {
    if (this == o) return true;
    if (preposition == null) return false;
    if (o instanceof PrepositionalPhraseImpl) {
      PrepositionalPhraseImpl ppi = (PrepositionalPhraseImpl) o;
      String ppip = ppi.preposition;
      return (preposition == ppip) &&
        ((indirectobject==null)?
         (ppi.indirectobject==null):(indirectobject.equals(ppi.indirectobject)));
    } else {
      return false;
    }
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
 
    stream.defaultWriteObject();

    try {
      stream.writeObject(indirectobject);
    } catch (NotSerializableException nse) {
      System.err.println(nse + " for indirectobject of " + preposition + ": " + indirectobject);
      throw nse;
    }
 }

  private void readObject(ObjectInputStream stream)
                throws ClassNotFoundException, IOException
  {

    stream.defaultReadObject();
    if (preposition != null) preposition = preposition.intern();
    indirectobject = stream.readObject();
  }
}
