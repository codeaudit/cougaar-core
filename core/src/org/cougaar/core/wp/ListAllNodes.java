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

import java.net.URI;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.Application;
import org.cougaar.core.service.wp.WhitePagesService;

/**
 * @property org.cougaar.wp.list.useShortcut
 *   Assume that each Node's MTS binds under "<i>node</i>(MTS)",
 *   which simplifies the "listAllNodes" WP lookup.
 * @see org.cougaar.core.mts.NameSupportImpl
 */
public final class ListAllNodes {

  private static final boolean USE_MTS_SHORTCUT =
    "true".equals(
        System.getProperty(
          "org.cougaar.core.wp.list.useShortcut", "true"));

  private static Application NODE_APP = 
    Application.getApplication("topology");
  private static String NODE_SCHEME   = "node";

  private ListAllNodes() { }

  /**
   * Get a Set of all node names.
   * <p>
   * @deprecated not scalable!
   */
  public static Set listAllNodes(
      WhitePagesService wps) throws Exception {
    Set toSet = new HashSet();
    recurse(toSet, wps, ".", 0);
    return toSet;
  }

  // recursive!
  private static void recurse(
      Set toSet, 
      WhitePagesService wps,
      String suffix,
      long timeout) throws Exception {
    Set names = wps.list(suffix, timeout);
    for (Iterator iter = names.iterator(); iter.hasNext(); ) {
      String s = (String) iter.next();
      if (s == null) {
      } else if (s.length() > 0 && s.charAt(0) == '.') {
        recurse(toSet, wps, s, timeout);
      } else {
        String node = getNode(wps, s, timeout);
        if (node != null) {
          toSet.add(node);
        }
      }
    }
  }

  private static String getNode(
      WhitePagesService wps,
      String s,
      long timeout) throws Exception {
    if (USE_MTS_SHORTCUT) {
      // we know that each node's MTS binds "<node>(MTS)"
      if (s.endsWith("(MTS)")) {
        return s.substring(0, s.length()-4);
      }
    } else {
      // do a WP lookup for the agent's node
      AddressEntry ae = wps.get(s, NODE_APP, NODE_SCHEME, timeout);
      if (ae != null) {
        URI uri = ae.getAddress();
        return uri.getPath().substring(1);
      }
    }
    return null;
  }
}
