/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */
 
package org.cougaar.domain.planning.ldm.plan;

/**
  * Constraint Interface
  * A Constraint is part of a Workflow.
  * Constraints provide pair-wise precedence
  * relationship information about the Tasks
  * contained in the Workflow.  A Task can have
  * more than one applicable Constraint.
  *
  * @author  ALPINE <alpine-software@bbn.com>
  * @version $Id: Constraint.java,v 1.1 2000-12-15 20:16:44 mthome Exp $
  **/
public interface Constraint
{
//    /** @deprecated **/
//    public static final int INITIATED = 0;
//    /** @deprecated **/
//    public static final int COMPLETED = 1;

  public static final int COINCIDENT = 0;
  public static final int BEFORE = -1;
  public static final int AFTER = 1;
  public static final int GREATERTHAN = 1;
  public static final int LESSTHAN = -1;
  public static final int EQUALTO = 0;

  /** 
   * <PRE> Task mytask = myconstraint.getConstrainingTask(); </PRE>
   * @return Task  Returns the Task which is constraining another event or Task.
   **/
  Task getConstrainingTask();

  /** 
   * <PRE> Task mytask = myconstraint.getConstrainedTask(); </PRE>
   * @return Task  Returns a Task which is constrained by another event or Task.
   **/
  Task getConstrainedTask();
	
  /** 
   * Returns an int which represents the
   * order of the Constraint.  
   * <PRE> int myorder = myconstraint.getConstraintOrder(); </PRE>
   * @return int  The int value
   * will be equal to "0" (COINCIDENT), "-1" (BEFORE) or "1" (AFTER).
   * There are also order analogues for constraints on non-temporal aspects.
   * These are "1" (GREATERTHAN), "-1" (LESSTHAN) or "0" (EQUALTO).
   **/
  int getConstraintOrder();

  /**
   * Returns the aspect type of the constraint for the constraining task
   * For non temporal constraints, constraining aspect and constrained aspect
   * will be the same. For temporal constraints, they can be different.
   * Eg (START_TIME and END_TIME)
   **/
  int getConstrainingAspect();

   /**
   * Returns the aspect type of the constraint for the constrained task
   * For non temporal constraints, constraining aspect and constrained aspect
   * will be the same. For temporal constraints, they can be different.
   * Eg (START_TIME and END_TIME)
   **/
  int getConstrainedAspect();

  /** Return the ConstraintEvent object for the constraining task
   */
  ConstraintEvent getConstrainingEventObject();
  
  /** Return the ConstraintEvent object for the constrained task
   */
  ConstraintEvent getConstrainedEventObject();

  /** 
   * Returns a double which represents the offset
   * of the Constraint. 
   * @return the value to be added to the constraining value before
   * comparing to the constrained value.
   **/
  double getOffsetOfConstraint();

  /* Calculate a value from constraining event, offset and order
   * to alleviate constraint violation on constrained event
   * Note that the current implementation only computes for 
   * temporal constraint aspects.
   */
  double computeValidConstrainedValue();
	
}
		
		
