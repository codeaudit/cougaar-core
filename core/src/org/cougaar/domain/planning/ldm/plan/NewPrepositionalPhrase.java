/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm.plan;

/** NewPrepositionalPhrase Interface
  * Provides setters for proper object building
  **/
	
public interface NewPrepositionalPhrase extends PrepositionalPhrase {
	
  /** 
   * @param apreposition Set the String representation of the Preposition 
   * @see org.cougaar.domain.planning.ldm.plan.Preposition for a list of valid values.
   */
  void setPreposition(String apreposition);
	
  /** @param anindirectobject - Set the IndirectObject of the PrepositionalPhrase */
  void setIndirectObject(Object anindirectobject);
}
