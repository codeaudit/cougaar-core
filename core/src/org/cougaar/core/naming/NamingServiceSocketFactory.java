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

package org.cougaar.core.naming;

import org.cougaar.core.mts.SocketFactory;

/**
 * Static factory to get the the NamingService's SocketFactory.
 *
 * @property org.cougaar.core.naming.useSSL
 * Boolean-valued property which controls whether or not ssl is used
 * in communication to the NameServer.  Defaults to 'false'.
 */
public final class NamingServiceSocketFactory {

  // NamingService hooks
  private final static String NS_USE_SSL_PROP = 
    "org.cougaar.core.naming.useSSL";
  private final static String NS_USE_SSL_DFLT = "false";
  private final static boolean NS_UseSSL =
    System.getProperty(NS_USE_SSL_PROP, NS_USE_SSL_DFLT).equals("true");

  private static SocketFactory nsInstance;

  private NamingServiceSocketFactory() { }

  /**
   * Get the NamingService socket factory.
   */
  public synchronized static SocketFactory getNameServiceSocketFactory() {
    if (nsInstance == null) {
      nsInstance = new SocketFactory(NS_UseSSL, false);
    }
    return nsInstance;
  }
}
