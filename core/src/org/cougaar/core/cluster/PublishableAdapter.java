/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.cluster;

import java.util.*;

/** 
 * A useful base class for objects which implement the Publishable
 * interface.  We implement all the methods of Publishable without
 * the class itself advertising the fact so that it can be used
 * for base classes which have a mix of publishable and not. <p>
 * The overhead is a single transient private data member.<p>
 * This silliness wouldn't be neccessary if Java allowed even a limited
 * form of multiple inheritence.
 **/

public class PublishableAdapter
  // implements Publishable  // but we dont want to advertise the fact.
{

  /** default version always returns true **/
  public boolean isPersistable() { return true; }
}
