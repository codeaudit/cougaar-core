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
import java.io.Serializable;

/**
 * Any object implementing this interface is uniquely-identifiable 
 * in the society by the object's UID.  This is similar to OIDs and
 * URLs, but is society-based.  We might eventually just use URLs
 * rather than UIDs.
 *
 * This interface does not promise that such objects are 
 * retrievable via any specific api, though such functionality
 * may be provided by some other facility.
 **/

public interface UniqueObject extends Serializable {
  /** @return the UID of a UniqueObject.  If the object was created
   * correctly (e.g. via a Factory), will be non-null.
   **/
  UID getUID();

  /** set the UID of a UniqueObject.  This should only be done by
   * an LDM factory.  Will throw a RuntimeException if
   * the UID was already set.
   **/
  void setUID(UID uid);
}



