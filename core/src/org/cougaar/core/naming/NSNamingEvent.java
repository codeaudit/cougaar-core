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

package org.cougaar.core.naming;

import java.io.Serializable;
import javax.naming.Binding;

/**
 * A structure for holding naming event information. Used in two ways:
 * to carry a collection of callbacks to the callbackqueue and to carry a
 * collection of listener ids back to the context.
 **/
class NSNamingEvent implements Serializable {
  NSCallback.Id cbid;
  int type;
  Binding newBinding, oldBinding;
  NSCallback callback;

  public NSNamingEvent(NSCallback.Id cbid, int type, Binding newBinding, Binding oldBinding) {
    this.cbid = cbid;
    this.type = type;
    this.newBinding = newBinding;
    this.oldBinding = oldBinding;
  }

  public NSCallback.Id getCallbackId() {
    return cbid;
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("NSNamingEvent[");
    buf.append(cbid);
    buf.append("] type=")
      .append(type)
      .append(", old=")
      .append(oldBinding)
      .append(", new=")
      .append(newBinding);
    return buf.toString();
  }
}
