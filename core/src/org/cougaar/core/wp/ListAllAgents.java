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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.cougaar.core.service.wp.WhitePagesService;

public class ListAllAgents {

  /**
   * Get a Set of all agent names.
   * <p>
   * @deprecated not scalable!
   */
  public static Set listAllAgents(
      WhitePagesService wps) throws Exception {
    Set toSet = new HashSet();
    recurse(toSet, wps, ".", 0);
    return toSet;
  }

  /**
   * URLEncode and sort a set of Strings.
   * <p>
   * @deprecated only for "listAllAgents" use
   */
  public static List encodeAndSort(Set s) {
    // URLEncode the names and sort
    ArrayList l = new ArrayList(s.size());
    for (Iterator iter = s.iterator(); iter.hasNext(); ) {
      String tmp = (String) iter.next();
      try {
        tmp = URLEncoder.encode(tmp, "UTF-8");
      } catch (UnsupportedEncodingException uee) {
        throw new RuntimeException("No UTF-8?", uee);
      }
      l.add(tmp);
    }
    Collections.sort(l);
    return l;
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
        toSet.add(s);
      }
    }
  }
}
