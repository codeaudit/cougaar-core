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

package org.cougaar.core.wp;

import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.mts.MessageAttributes;
import org.cougaar.core.mts.SimpleMessageAttributes;

/**
 * Utility code to attach an MTS timeout attribute to a message
 * address.
 */
public final class MessageTimeoutUtils {

  /**
   * Attribute constants, to be moved into the core MTS (bug 3213).
   */
  private static final String SEND_TIMEOUT  = "MessageSendTimeout";
  private static final String SEND_DEADLINE = "MessageSendDeadline";


  private MessageTimeoutUtils() {}

  /**
   * Get the absolute deadline on an address, for example
   * <code>1060280361356</code> milliseconds.
   */
  public static long getDeadline(MessageAddress addr) {
    Number value = get(addr, SEND_DEADLINE);
    return (value == null ? -1 : value.longValue());
  }

  /** Tag an address with a relative timeout. */
  public static MessageAddress setDeadline(
      MessageAddress addr,
      long deadline) {
    return
      (deadline <= 0 ?
       (addr) :
       set(addr, SEND_DEADLINE, new Long(deadline)));
  }

  /**
   * Get the relative timeout on an address, for example
   * <code>5000</code> milliseconds. 
   */
  public static long getTimeout(MessageAddress addr) {
    Number value = get(addr, SEND_TIMEOUT);
    return (value == null ? -1 : value.longValue());
  }

  /** Tag an address with a relative timeout. */
  public static MessageAddress setTimeout(
      MessageAddress addr,
      long timeout) {
    int t = (int) timeout;
    return
      (t <= 0 ?
       (addr) :
       set(addr, SEND_TIMEOUT, new Integer(t)));
  }

  // get a number attribute
  private static Number get(
      MessageAddress addr,
      String name) {
    if (addr == null) {
      return null;
    }
    MessageAttributes attrs = addr.getMessageAttributes();
    if (attrs == null) {
      return null;
    }
    Object o = attrs.getAttribute(name);
    if (!(o instanceof Number)) {
      return null;
    }
    return ((Number) o);
  }

  // set a number attribute
  private static MessageAddress set(
      MessageAddress addr,
      String name,
      Number value) {
    if (addr == null) {
      return null;
    }
    MessageAttributes attrs = addr.getMessageAttributes();
    if (attrs == null) {
      attrs = new SimpleMessageAttributes();
      addr = MessageAddress.getMessageAddress(addr, attrs);
    }
    attrs.setAttribute(name, value);
    return addr;
  }
}
