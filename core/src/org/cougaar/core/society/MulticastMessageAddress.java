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

/**
 * A marker class for multicasting messages.
 * Used by constant addresses in MessageAddress.
 **/

public class MulticastMessageAddress extends MessageAddress {

  /** for Externalizable use only **/
  public MulticastMessageAddress() {}

  public MulticastMessageAddress( String address ) {
    super(address);
  }

}
