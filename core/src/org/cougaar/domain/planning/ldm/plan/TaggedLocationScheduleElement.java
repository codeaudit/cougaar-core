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
