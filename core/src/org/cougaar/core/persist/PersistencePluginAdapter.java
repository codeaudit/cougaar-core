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

package org.cougaar.core.persist;

import java.sql.Connection;
import org.cougaar.core.adaptivity.OMCRange;
import org.cougaar.core.adaptivity.OMCRangeList;

/**
 * Adapter to simplify writing PersistencePlugin implementations.
 * Implements several methods in the PersistencePlugin API that often
 * need not be specialized in individual implementaions.
 **/
public class PersistencePluginAdapter
{
  protected static final String[] emptyStringArray = new String[0];
  protected static final OMCRangeList emptyOMCRangeList =
    new OMCRangeList(new OMCRange[0]);
  protected PersistencePluginSupport pps;

  protected String name;

  protected void init(PersistencePluginSupport pps, String name) {
    this.pps = pps;
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public String[] getControlNames() {
    return emptyStringArray;
  }

  public OMCRangeList getControlValues(String controlName) {
    return emptyOMCRangeList;   // Should never be called
  }

  public void setControl(String controlName, Comparable newValue) {
  }

  public java.sql.Connection getDatabaseConnection(Object locker) {
    throw new UnsupportedOperationException("FilePersistence.getDatabaseConnection not supported");
  }

  public void releaseDatabaseConnection(Object locker) {
    throw new UnsupportedOperationException("FilePersistence.releaseDatabaseConnection not supported");
  }
}
