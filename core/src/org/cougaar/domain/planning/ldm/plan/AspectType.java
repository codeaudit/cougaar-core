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
  
  /** The extent to which a task has been satisfactorily completed **/
  static final int READINESS = 12;

  /** The last core-defined aspect type **/
  static final int N_CORE_ASPECTS = 12;

  static final int _LAST_ASPECT = READINESS;
  static final int _ASPECT_COUNT = _LAST_ASPECT+1;

  static final int[] _STANDARD_ASPECTS = {0,1,2,3,4,5,6,7,8,9,10,11,12};
  
  
  // extended AspectTypes that are NOT handled by default
  // AllocationResultAggregators or AllocationResultDistributors
  
  /** The point of debarkation of a task **/
  static final int POD = 13;         

  /** The time at which a task should arrive at the POD **/
  static final int POD_DATE = 14;

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
    "READINESS",
    "POD",
    "POD_DATE",
  };
}
