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

