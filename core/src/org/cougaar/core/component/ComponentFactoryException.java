/*
 * <copyright>
 * Copyright 2000-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.component;

import java.util.*;

/** Exception thrown by ComponentFactory
 **/
public class ComponentFactoryException extends Exception
{
  private final String explanation;
  private final ComponentDescription cd;
  private final Exception nestedException;
  public ComponentFactoryException(String explanation, ComponentDescription cd, Exception nestedException) {
    this.explanation = explanation;
    this.cd = cd;
    this.nestedException = nestedException;
  }
  
  public ComponentFactoryException(String explanation, ComponentDescription cd) {
    this.explanation = explanation;
    this.cd = cd;
    this.nestedException = null;
  }

  public String toString() {
    return "<ComponentFactoryException: "+explanation+" for "+cd+">";
  }

  public Exception getNestedException() { 
    return nestedException;
  }
}

