/* 
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
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
implements Proc {

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

