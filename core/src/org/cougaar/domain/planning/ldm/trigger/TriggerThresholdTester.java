/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */


package org.cougaar.domain.planning.ldm.trigger;

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


