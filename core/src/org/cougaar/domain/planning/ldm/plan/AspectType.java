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

/** Constant names for different 'score' dimensions in which to report
 * allocation consequences.
 **/
public interface AspectType {
  
  /** Undefined value for an aspect type **/
  static final int UNDEFINED = -1;

  /** Start time of given Task **/
  static final int START_TIME = 0;
    
  /** End time of given Task **/
  static final int END_TIME = 1;
  
  /** Duration time of the Task **/
  static final int DURATION = 2;
    
  /** Cost (in $) of allocating given Task **/
  static final int COST = 3;
    
  /** Probability of loss of assets associated with allocation **/
  static final int DANGER = 4;
    
  /** Probability of failure of the Mission **/
  static final int RISK = 5;
    
  /** Quantities associated with allocation (number of elements sourced, e.g.) **/
  static final int QUANTITY = 6;
  
  /** For repetitive tasks - specify the amount of time(milliseconds) 
   * in between deliveries 
   **/
  static final int INTERVAL = 7;
  
  /**For repetitive tasks the total sum quantity of item for the time span **/
  static final int TOTAL_QUANTITY = 8;
  
  /** For repetitive tasks the total number of shipments requested across 
   * the time span.
   * This should be used in association with the interval aspect.
   **/
  static final int TOTAL_SHIPMENTS = 9;

  /**   **/
  static final int CUSTOMER_SATISFACTION = 10;
  
  /** Used to represent an Asset/Quantity relationship
   * @see org.cougaar.domain.planning.ldm.plan.TypedQuantityAspectValue
   **/
  static final int TYPED_QUANTITY = 11;         
  
  static final int _LAST_ASPECT = TYPED_QUANTITY;
  static final int _ASPECT_COUNT = _LAST_ASPECT+1;

  static final int[] _STANDARD_ASPECTS = {0,1,2,3,4,5,6,7,8,9,10,11};
  
  
  // extended AspectTypes that are NOT handled by default
  // AllocationResultAggregators or AllocationResultDistributors
  
  /** The point of debarkation of a task **/
  static final int POD = 12;         

  /** The time at which a task should arrive at the POD **/
  static final int POD_DATE = 13;

  /** The last core-defined aspect type **/
  static final int N_CORE_ASPECTS = 14;

  public static final String[] ASPECT_STRINGS = {
    "START_TIME",
    "END_TIME",
    "DURATION", 
    "COST",
    "DANGER",
    "RISK",
    "QUANTITY",
    "INTERVAL",
    "TOTAL_QUANTITY",
    "TOTAL_SHIPMENTS",
    "CUSTOMER_SATISFACTION",
    "TYPED_QUANTITY",
    "POD",
    "POD_DATE",
  };
}
