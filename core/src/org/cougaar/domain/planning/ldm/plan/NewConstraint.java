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
 
/**
 * NewConstraint extends Constraint and provides
 * setter methods for building valid Constraint objects.
 *
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: NewConstraint.java,v 1.2 2001-04-05 19:27:16 mthome Exp $
 **/
public interface NewConstraint extends Constraint {
     /**
      * setConstrainingTask allows you to set the constraining task.
      * This should be done exactly once for a particular constraint
      * that is not an absolute constraint and must be done before the
      * constraint is added to the workflow.
      * @param task the task that is constraining another task.
      **/
     void setConstrainingTask(Task task);

     /**
      * setConstrainingAspect allows you to set the constraining
      * aspect. For non-temporal constraints both constraining and
      * constrained aspects must be the same For temporal aspects they
      * can be different (as long as they are both temporal aspects).
      * This should be done exactly once for a particular constraint
      * that is not an absolute constraint and must be done before the
      * constraint is added to the workflow.
      * @param aspect the aspect of the constraining task.
      **/
     void setConstrainingAspect(int aspect);

     /**
      * Set an absolute value as the constraining aspect value. The
      * constrained task's preferences can be adjusted to fulfill the
      * constraint. The constraining value of an absolute constraint
      * may be changed at any time. The new value will be used in
      * subsequent constraint violation checks and in setting the
      * preferences of the constrained task. The constraining value is
      * added to the offset value, but it is not necessary to use both
      * features.
      *
      * The use of this method is mutually exclusive of using
      * setConstrainingTask. Use one or the other, not both for a
      * particular Constraint.
      * @param value the value against which the constrained value is tested.
      **/
     void setAbsoluteConstrainingValue(double value);

     /**
      * setConstrainedTask allows you to set the constrained task.
      * This should be done exactly once for a particular constraint
      * and must be done before the constraint is added to the
      * workflow.
      * @param task is the task that is to be adjusted to satisfy the
      * constraint.
      **/
     void setConstrainedTask(Task task);

     /**
      * setConstrainedAspect allows you to set the constrained
      * aspect. For non-temporal constraints both constraining and
      * constrained aspects must be the same. For temporal aspects
      * they can be different, but must be temporal aspects.
      * This method should be called once before the constraint is
      * added to the workflow.
      * @param aspect the aspect type of the allocation result to be
      * tested.
      **/
     void setConstrainedAspect(int aspect);
     
     /**
      * Specifies how the aspect values should be constrained. The
      * order should be COINCIDENT, BEFORE, or AFTER for temporal
      * constraints, GREATERTHAN, LESSTHAN, or EQUALTO for
      * non-temporal constraints.
      * <PRE> mynewconstraint.setConstraintOrder(BEFORE); </PRE>
      * @param order - Should be COINCIDENT, BEFORE, AFTER,
      * GREATERTHAN, LESSTHAN or EQUALTO.
      **/
     void setConstraintOrder(int order);
 	
     /**
      * setOffsetOfConstraint allows you to set the offset of the
      * Constraint.  The offset is added to the constraining aspect
      * value before it is compared to the constrained aspect value.
      * <PRE> mynewconstraint.setOffsetOfConstraint(-2000); </PRE>
      * @param offset - of the Constraint
      **/
     void setOffsetOfConstraint(double offset);
 	
 }
