/*
 * <copyright>
 *  Copyright 2002-2003 BBNT Solutions, LLC
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

package org.cougaar.core.wp.resolver;

import org.cougaar.util.LRUExpireMap;
import org.cougaar.util.log.Logger;
import org.cougaar.core.wp.Timestamp;

/**
 * This is a simple LRU cache watcher that logs the activity.
 */
public class LoggingCacheWatcher
implements LRUExpireMap.Watcher {

  private final Logger logger;

  public LoggingCacheWatcher(Logger logger) {
    this.logger = logger;
  }

  public void noteExpire(
      Object key, Object value, long putTime, long expireTime) {
    if (logger.isDebugEnabled()) {
      logger.debug(
          "Removing expired "+
          format(key, value, putTime, expireTime));
    }
  }
  public void noteEvict(
      Object key, Object value, long putTime, long expireTime) {
    if (logger.isInfoEnabled()) {
      logger.info(
          "Evicting LRU "+
          format(key, value, putTime, expireTime));
    }
  }
  public void noteTrim(int nfreed, int origSize) {
    if (nfreed > 0 && logger.isInfoEnabled()) {
      logger.info("Cache trim expired "+nfreed+" of "+origSize);
    }
  }
  private static final String format(
      Object key, Object value, long putTime, long expireTime) {
    long now = System.currentTimeMillis();
    return 
      "cache entry (key="+key+
      ", putTime="+
      Timestamp.toString(putTime,now)+
      ", expireTime="+
      Timestamp.toString(expireTime,now)+
      ", value="+value+
      ")";
  }
}
