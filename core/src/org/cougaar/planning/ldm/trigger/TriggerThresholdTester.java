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


package org.cougaar.planning.ldm.trigger;

/**
 * Abstract Threshold trigger tester to fire if a given
 * computed parameter value exceeds a given threshold. 
 * Comparison sense of greater than/less than is settable.
 */

public abstract class TriggerThresholdTester implements TriggerTester {
  
  private double my_threshold;
  private boolean my_fire_if_exceeds;

  // Constructor : save threshold and fire_if_exceeds flag
  TriggerThresholdTester(double threshold, boolean fire_if_exceeds) 
  { 
    my_threshold = threshold; 
    my_fire_if_exceeds = fire_if_exceeds; 
  }

  // Abstract method to compute threshold value
  public abstract double ComputeValue(Object[] objects);

  // Tester Test function : Compare compare computed value with threshold
  public boolean Test(Object[] objects) { 
    double value = ComputeValue(objects);
    if (my_fire_if_exceeds) 
      return value > my_threshold;
    else
      return value < my_threshold;
  }


  

}


