/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.cluster;

/**
 * This is a convenience base class for test classes that are
 * SubscriptionClients that don't need to know their name.
 */
public class DummySubscriptionClient {
  private static int nameCounter = 0;
  private String name;
  protected DummySubscriptionClient() {
    synchronized (getClass()) {
      name = "" + nameCounter++;
    }
  }
  public String getSubscriptionClientName() {
    return name;
  }
}

