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

package org.cougaar.core.service;

import org.cougaar.core.component.Service;

/**
 * Service that allows the client to request a JVM-wide stack
 * dump.
 * <p>
 * Access to this service will likely be restricted to a
 * limited subset of node-agent components.  Typical plugin 
 * developers should not use this service.
 */
public interface JvmStackDumpService extends Service {

  /**
   * Request that the JVM dump its stack to standard-out.
   * <p>
   * This is equivalent to typing "CTRL-BREAK" on Windows or
   * or "CTRL-\" on Unix.
   * <p>
   * For notes on the format of the JVM stack dump, see:
   * <a href="http://java.sun.com/j2se/1.4.1/changes.html#deadlock"
   * >http://java.sun.com/j2se/1.4.1/changes.html#deadlock</a>.
   *
   * @return true if the stack was dumped
   */
  public boolean dumpStack();

}

