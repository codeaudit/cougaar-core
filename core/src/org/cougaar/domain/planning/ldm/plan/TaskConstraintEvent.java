/* Implementation of SettableConstraintEvent. New
   API to be used as utility functions for resolving
   constraints
*/

package org.cougaar.domain.planning.ldm.plan;

import org.cougaar.domain.planning.ldm.plan.*;

public abstract class TaskConstraintEvent implements SettableConstraintEvent
{
  public static class Constrained extends TaskConstraintEvent {
    public Constrained(NewTask t, int aspect) {
      super(t, aspect, NOVALUE);
    }

    public boolean isConstraining() {
      return false;
    }
  }

  public static class Constraining extends TaskConstraintEvent {
    public Constraining(NewTask t, int aspect) {
      super(t, aspect, NOVALUE);
    }

    public boolean isConstraining() {
      return true;
    }
  }

  private NewTask task; /* constrained or constraining task */
  private int event; /* constrained or constraining aspect */
  private double value = NOVALUE; /* A value to use if a constraining task has NOVALUE */
    
  protected TaskConstraintEvent(NewTask t, int aspect, double value) {
    task = t;
    event = aspect;
    this.value = value;
  }
	
  public int getAspectType() {
    return event;
  }
	                                    
  public Task getTask() {
    return task;
  }

  public abstract boolean isConstraining();
	
  /**
   * getValue()
   * @return the allocation result of particular aspect if
   * isConstraining else returns the preference on that aspect.
   **/
  public double getValue() {
    if (isConstraining()) {
      PlanElement pe = task.getPlanElement();
      if (pe == null) return value;
      AllocationResult ar = pe.getEstimatedResult();
      if (ar == null) return value;
      return ar.getValue(event);
    } else {
      return task.getPreferredValue(event);
    }
  }

  /**
   * setValue sets the aspect value. To be used on a Constrained task
   * to set the aspect value to a value that will satisfy constraints
   **/
  public void setValue(double value, int constraintOrder, double slope) {
    this.value = value;  // local copy
    if (isConstraining()) {
      throw new RuntimeException("setValue only applies to a constrained event");
    }
    // set preference on the constrained task
    ScoringFunction sf;
    switch (constraintOrder) {
    case Constraint.BEFORE: // Same as LESSTHAN
      sf = ScoringFunction.createNearOrBelow(new AspectValue(event, value), slope);
      break;
    case Constraint.AFTER: // Same as GREATERTHAN
      sf = ScoringFunction.createNearOrAbove(new AspectValue(event, value), slope);
      break;
    case Constraint.COINCIDENT: // Same as EQUALTO
      sf = ScoringFunction.createStrictlyAtValue(new AspectValue(event, value));
      break;
    default:
      return;
    }
    Preference pref = new PreferenceImpl(event, sf);
    task.setPreference(pref);
  }
}
