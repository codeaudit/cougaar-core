/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm.plan;

import org.cougaar.domain.planning.ldm.plan.Aggregation;
import org.cougaar.domain.planning.ldm.plan.AllocationResult;
import org.cougaar.domain.planning.ldm.plan.Composition;
import org.cougaar.domain.planning.ldm.plan.Plan;
import org.cougaar.domain.planning.ldm.plan.Task;
import org.cougaar.domain.planning.ldm.plan.MPTask;
import org.cougaar.core.cluster.Subscriber;

import java.util.*;
import java.beans.*;

import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;


/** AggregationImpl.java
 * Implementation for aggregation
 */
 
public class AggregationImpl extends PlanElementImpl 
  implements Aggregation
{
 
  private transient Composition comp;  // changed to transient : Persistence
  
  public AggregationImpl() {}
 	
  /* Constructor that takes the composition and
   * assumes that there was no estimate.
   * @param p Plan
   * @param t Task
   * @param composition
   * @return Aggregation
   */
  public AggregationImpl(Plan p, Task t, Composition composition)  {
    super(p, t);
    this.comp = composition;
  }
  
  /* Constructor that takes the composition and an estimated result
   * @param t
   * @param p
   * @param composition
   * @param estimatedresult
   * @return Aggregation
   */
  public AggregationImpl(Plan p, Task t, Composition composition, AllocationResult estimatedresult)  {
    super(p, t);
    this.comp = composition;
    this.estAR = estimatedresult;
  }  

  /** Returns the Composition created by the aggregations of the task.
    * @see org.cougaar.domain.planning.ldm.plan.Composition
    * @return Composition
    **/
   public Composition getComposition() {
     return comp;
   }  


  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();
    stream.writeObject(comp);
 }

  private void readObject(ObjectInputStream stream)
                throws ClassNotFoundException, IOException
  {
    /** ----------
      *    READ handlers common to Persistence and
      *    Network serialization.  NOte that these
      *    cannot be references to Persistable objects.
      *    defaultReadObject() is likely to belong here...
      * ---------- **/
    stream.defaultReadObject();

    comp = (Composition)stream.readObject();
  }

  public String toString() {
    return "[Aggregation of " + getTask().getUID() + " to " + comp + "]";
  }
  
  // ActiveSubscription code
  // override PlanElementImpls remove stuff
  public boolean removingFromLogPlan(Subscriber s) {
    Task t = getTask();
    ((TaskImpl)t).privately_resetPlanElement();
    Composition c = getComposition();

    if (c == null) return true; // if already disconnected...

    if (c.isPropagating() ) { // if we're auto-propagating
      CompositionImpl ci = (CompositionImpl) c;

      // check to make sure we haven't already done the mass cleanup
      if (ci.doingCleanUp()) { // atomic check and clear
        // since we will get "notified" for every aggregation we rescinded
        // during the mass cleanup
        // rescind every aggregation planelement associated with the composition
        
        List aggpes = ci.clearAggregations();    // atomic get and clear the list
        ListIterator it = aggpes.listIterator();
        while (it.hasNext()) {
          Aggregation anagg = (Aggregation) it.next();
          if (anagg != this)       // dont recurse on ourselves...
            s.publishRemove(anagg);
        }

        // rescind the combined task
        s.publishRemove(c.getCombinedTask());
      }  
    } else {      // we're not auto-propagating
      // clean up the references to this pe and its task in 
      // the composition and the combined MPTask
      ((CompositionImpl)c).removeAggregation(this);
      MPTask combtask = (MPTask) c.getCombinedTask();
      ((MPTaskImpl)combtask).removeParentTask(t);
      // if this happens to be the last parent of this combined task
      // rescind the combined task since its no longer valid.
      Enumeration parentsleft = combtask.getParentTasks();
      if (! parentsleft.hasMoreElements()) {
        s.publishRemove(combtask);
      }
    }
    return true;
  }

  // beaninfo
  protected void addPropertyDescriptors(Collection c) throws IntrospectionException {
    super.addPropertyDescriptors(c);
    c.add(new PropertyDescriptor("composition", AggregationImpl.class, "getComposition", null));
  }
}
