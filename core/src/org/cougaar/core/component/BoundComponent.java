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

/** A pair of Binder and Component, useful for keeping 
 * state in Containers.
 **/
public class BoundComponent
{
  private final Binder b;
  private final Component c;

  public BoundComponent(Binder b, Component c) {
    this.b = b;
    this.c = c;
  }

  public final Binder getBinder() { return b; }
  public final Component getComponent() { return c; }
}
