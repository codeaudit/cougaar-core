/*
 * <copyright>
 *  Copyright 2000-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm.plan;

import org.cougaar.domain.planning.ldm.plan.NewLocationScheduleElement;
import org.cougaar.domain.planning.ldm.plan.LocationScheduleElement;
import org.cougaar.domain.planning.ldm.plan.Location;
import java.util.Date;

/**
 * A LocationScheduleElement which is additionally tagged with 
 * an additional piece of information, normally the "owner" of the
 * schedule information.  It is up to (non-abstract) subclasses to decide
 * how to implement getOwner().
 **/

public abstract class TaggedLocationScheduleElement
  extends LocationScheduleElementImpl
{
  public TaggedLocationScheduleElement(long t0, long t1, Location loc) {
    super(t0,t1,loc);
  }

  /** @return the "owner" or source of this location schedule element.
   * Implementations are encouraged to have this method return the UID 
   * some first-class logplan object.
   **/
  public abstract Object getOwner();
} 
