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

import org.cougaar.core.adaptivity.OMCRange;
import org.cougaar.core.adaptivity.OMCRangeList;
import org.cougaar.util.log.Logger;

/**
 * Adapter to simplify writing PersistencePlugin implementations.
 * Implements several methods in the PersistencePlugin API that often
 * need not be specialized in individual implementaions.
 **/
public abstract class PersistencePluginAdapter implements PersistenceNames {
  protected static final String[] emptyStringArray = new String[0];
  protected static final OMCRangeList emptyOMCRangeList =
    new OMCRangeList(new OMCRange[0]);
  protected static String[] controlNames = {
    PERSISTENCE_ARCHIVE_COUNT_NAME
  };
  protected PersistencePluginSupport pps;

  protected String name;
  protected String[] params;
  protected int archiveCount;     // The number of archives to keep
  protected int consolidationPeriod;
  protected long persistenceInterval;
  protected boolean writable = true;

  protected void init(PersistencePluginSupport pps, String name, String[] params) {
    this.pps = pps;
    this.name = name;
    this.params = params;
    try {
      archiveCount = Integer.parseInt(System.getProperty(PERSISTENCE_ARCHIVE_COUNT_PROP));
    } catch (Exception e) {
      if (Boolean.getBoolean("org.cougaar.core.persistence.archivingDisabled")) {
        archiveCount = 0;
      } else {
        archiveCount = Integer.MAX_VALUE;
      }
    }
    Logger logger = pps.getLogger();
    for (int i = 0; i < params.length; i++) {
      String param = params[i];
      try {
        if (param.startsWith(PERSISTENCE_ARCHIVE_COUNT_PREFIX)) {
          archiveCount = Integer.parseInt(param.substring(PERSISTENCE_ARCHIVE_COUNT_PREFIX.length()));
          if (logger.isDebugEnabled()) logger.debug("archiveCount=" + archiveCount);
          continue;
        }
        if (param.startsWith(PERSISTENCE_INTERVAL_PREFIX)) {
          persistenceInterval = Long.parseLong(param.substring(PERSISTENCE_INTERVAL_PREFIX.length()));
          if (logger.isDebugEnabled()) logger.debug("persistenceInterval=" + persistenceInterval);
          continue;
        }
        if (param.startsWith(PERSISTENCE_CONSOLIDATION_PERIOD_PREFIX)) {
          consolidationPeriod = Integer.parseInt(param.substring(PERSISTENCE_CONSOLIDATION_PERIOD_PREFIX.length()));
          if (logger.isDebugEnabled()) logger.debug("consolidationPeriod=" + consolidationPeriod);
          continue;
        }
        if (param.startsWith(PERSISTENCE_DISABLE_WRITE_PREFIX)) {
          writable = !"true".equals(param.substring(PERSISTENCE_DISABLE_WRITE_PREFIX.length()));
          if (logger.isDebugEnabled()) logger.debug("writable=" + writable);
          continue;
        }
        handleParameter(param);
      } catch (Exception e) {
        pps.getLogger().error("Parse error " + param);
      }
    }
  }

  protected abstract void handleParameter(String param);

  protected String parseParamValue(String param, String key) {
    if (param.startsWith(key)) {
      return param.substring(key.length());
    }
    return null;
  }

  public String getName() {
    return name;
  }

  public boolean isWritable() {
    return writable;
  }

  public void setWritable(boolean newWritable) {
    writable = newWritable;
  }

  public long getPersistenceInterval() {
    return persistenceInterval;
  }

  public void setPersistenceInterval(long newInterval) {
    persistenceInterval = newInterval;
  }

  public int getConsolidationPeriod() {
    return consolidationPeriod;
  }

  public void setConsolidationPeriod(int newPeriod) {
    consolidationPeriod = newPeriod;
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
    if (PERSISTENCE_ARCHIVE_COUNT_NAME.equals(controlName)) {
      return new OMCRangeList(new Integer(0), new Integer(Integer.MAX_VALUE));
    }
    return emptyOMCRangeList;   // Should never be called
  }
  
  public void setControl(String controlName, Comparable newValue) {
    if (PERSISTENCE_ARCHIVE_COUNT_NAME.equals(controlName)) {
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
