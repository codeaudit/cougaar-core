/*
 * <copyright>
 *  Copyright 1997-2003 BBNT Solutions, LLC
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
public class PersistencePluginAdapter {
  protected static final String[] emptyStringArray = new String[0];
  protected static final OMCRangeList emptyOMCRangeList =
    new OMCRangeList(new OMCRange[0]);
  protected static final String ARCHIVE_COUNT =
    PersistencePlugin.ARCHIVE_COUNT_PARAM.substring(0, PersistencePlugin.ARCHIVE_COUNT_PARAM.length() - 1);
  protected static String[] controlNames = {
    ARCHIVE_COUNT
  };
  protected PersistencePluginSupport pps;

  protected String name;
  protected String[] params;
  protected int archiveCount;     // The number of archives to keep

  protected void init(PersistencePluginSupport pps, String name, String[] params) {
    this.pps = pps;
    this.name = name;
    this.params = params;
    try {
      archiveCount = Integer.parseInt(System.getProperty("org.cougaar.core.persistence.archiveCount"));
    } catch (Exception e) {
      if (Boolean.getBoolean("org.cougaar.core.persistence.archivingDisabled")) {
        archiveCount = 0;
      } else {
        archiveCount = Integer.MAX_VALUE;
      }
    }
    for (int i = 0; i < params.length; i++) {
      String param = params[i];
      if (param.startsWith(PersistencePlugin.ARCHIVE_COUNT_PARAM)) {
        try {
          archiveCount = Integer.parseInt(param.substring(PersistencePlugin.ARCHIVE_COUNT_PARAM.length()));
          break;
        } catch (Exception e) {
          pps.getLogger().error("Parse error " + param);
        }
      }
    }
    pps.getLogger().debug("archiveCount=" + archiveCount);
  }

  protected String parseParamValue(String param, String key) {
    if (param.startsWith(key)) {
      return param.substring(key.length());
    }
    return null;
  }

  public String getName() {
    return name;
  }

  public int getParamCount() {
    return params.length;
  }

  public String getParam(int i) {
    return params[i];
  }

  public String[] getControlNames() {
    return controlNames;
  }

  public OMCRangeList getControlValues(String controlName) {
    if (ARCHIVE_COUNT.equals(controlName)) {
      return new OMCRangeList(new Integer(0), new Integer(Integer.MAX_VALUE));
    }
    return emptyOMCRangeList;   // Should never be called
  }
  
  public void setControl(String controlName, Comparable newValue) {
    if (ARCHIVE_COUNT.equals(controlName)) {
      archiveCount = ((Integer) newValue).intValue();
    }
  }

  public java.sql.Connection getDatabaseConnection(Object locker) {
    throw new UnsupportedOperationException("FilePersistence.getDatabaseConnection not supported");
  }

  public void releaseDatabaseConnection(Object locker) {
    throw new UnsupportedOperationException("FilePersistence.releaseDatabaseConnection not supported");
  }

  public boolean checkOwnership() {
    return true;
  }

  public void lockOwnership() {
  }

  public void unlockOwnership() {
  }
}
