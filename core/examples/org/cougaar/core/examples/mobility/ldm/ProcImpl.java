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
package org.cougaar.core.examples.mobility.ldm;

import java.io.Serializable;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;

/**
 * Package-private implementation for a script proc.
 */
class ProcImpl
implements Proc, Serializable {

  private final UID uid;
  private final UID scriptUID;
  private final long startTime;
  private long endTime;
  private UID stepUID;
  private int moveCount;
  private int scriptIndex;

  public ProcImpl(
      UID uid,
      UID scriptUID,
      long startTime) {
    this.uid = uid;
    this.scriptUID = scriptUID;
    this.startTime = startTime;
    if ((uid == null) ||
        (scriptUID == null)) {
      throw new IllegalArgumentException(
          "Null uid/scriptUID");
    }
    scriptIndex = -1;
  }

  public UID getUID() {
    return uid;
  }
  public void setUID(UID uid) {
    throw new UnsupportedOperationException();
  }

  public UID getScriptUID() {
    return scriptUID;
  }

  public long getStartTime() {
    return startTime;
  }

  public long getEndTime() {
    return endTime;
  }

  public int getMoveCount() {
    return moveCount;
  }

  public UID getStepUID() {
    return stepUID;
  }

  public int getScriptIndex() {
    return scriptIndex;
  }

  public void setEndTime(long time) {
    endTime = time;
  }

  public void setMoveCount(int count) {
    moveCount = count;
  }

  public void setStepUID(UID uid) {
    stepUID = uid;
  }

  public void setScriptIndex(int index) {
    scriptIndex = index;
  }

  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof LocalStepImpl)) {
      return false;
    } else {
      UID u = ((LocalStepImpl) o).uid;
      return uid.equals(u);
    }
  }
  public int hashCode() {
    return uid.hashCode();
  }

  public String toString() {
    return 
      "Proc {"+
      "\n uid: "+uid+
      "\n scriptUID: "+scriptUID+
      "\n startTime: "+startTime+
      "\n endTime: "+endTime+
      "\n stepUID: "+stepUID+
      "\n moveCount: "+moveCount+
      "\n scriptIndex: "+scriptIndex+
      "\n}";
  }
}

