/*
 * <copyright>
 *  Copyright 2000-2001 BBNT Solutions, LLC
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

