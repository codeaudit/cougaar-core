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

package org.cougaar.core.util;

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
   * a domain factory.  Will throw a RuntimeException if
   * the UID was already set.
   **/
  void setUID(UID uid);
}



