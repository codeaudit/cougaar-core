/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.component;

import java.beans.*;
import java.beans.beancontext.*;
import java.util.*;
import java.net.URL;

import org.cougaar.util.GenericStateModel;

/**
 * Binder for a child that is a Container.
 * <p>
 * Allows the parent Container to pass "add(o)" requests
 * to it's children, based on the insertion point.
 */
public interface ContainerBinder
  extends Binder
{
  public boolean add(Object o);

  public boolean remove(Object o);
}

