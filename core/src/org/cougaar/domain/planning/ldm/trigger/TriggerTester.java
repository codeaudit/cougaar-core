/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */


package org.cougaar.domain.planning.ldm.trigger;

/**
 * A TriggerTester determines a particular condition exists on a particular
 * set of objects.
 */

public interface TriggerTester extends java.io.Serializable {

  /** 
   * @param objects  The objects to test.
   * @return boolean  If the objects tested true.
   */
  boolean Test(Object[] objects);

}

