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
/* AbsoluteConstraintEvent objects are used to describe the fixed
 * (non-task) side of constraints involving one task */

package org.cougaar.domain.planning.ldm.plan;

import org.cougaar.domain.planning.ldm.plan.ConstraintEvent;
import org.cougaar.domain.planning.ldm.plan.AspectType;

public class AbsoluteConstraintEvent implements ConstraintEvent
{
  /* define an absolute constraint value on some aspect   */
  private int event; /* constraint aspect      */
  private double eventValue; /* aspect value */

  public AbsoluteConstraintEvent(int aspect, double value) {
    event = aspect;
    eventValue = value;
  }

  public Task getTask() {
    return null;
  }

  public double getValue() {
    return eventValue;
  }

  public double getResultValue() {
    return getValue();
  }

  public int getAspectType() {
    return event;
  }

  public boolean isConstraining() {
    return true;
  }
}

