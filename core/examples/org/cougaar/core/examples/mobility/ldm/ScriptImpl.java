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
import java.util.List;
import org.cougaar.core.mobility.Ticket;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;

/**
 * Package-private implementation for a script.
 */
class ScriptImpl
implements Script, Serializable {

  protected final UID uid;
  protected final List entries;

  public ScriptImpl(
      UID uid, String text) {
    this.uid = uid;
    // parse it all now; could delay until actual
    // use and only validate at creation time
    this.entries = ScriptParser.parse(text);
    if ((uid == null) ||
        (entries == null)) {
      throw new IllegalArgumentException(
          "null uid/text");
    }
  }

  public UID getUID() {
    return uid;
  }
  public void setUID(UID uid) {
    throw new UnsupportedOperationException();
  }

  public int getSize() {
    return entries.size();
  }

  public Entry getEntry(int idx) {
    return (Entry) entries.get(idx);
  }

  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof ScriptImpl)) {
      return false;
    } else {
      UID u = ((ScriptImpl) o).uid;
      return uid.equals(u);
    }
  }
  public int hashCode() {
    return uid.hashCode();
  }
  
  public String toString() {
    return "script "+uid+" "+entries;
  }
}
