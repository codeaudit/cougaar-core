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

import java.io.Serializable;

/**
 * This is a simple wrapper around an object that adds a String
 * name.
 * <p>
 * This is used to tag a request (e.g. a Record-based modify)
 * with the name of the agent requesting the action.  Most
 * clients will simply remove the tag to get to the wrapped
 * object.
 */
public final class NameTag implements Serializable {

  private final String name;
  private final Object obj;

  public NameTag(String name, Object obj) {
    this.name = name;
    this.obj = obj;
    // validate
    String s =
      ((name == null) ? "null name" :
       null);
    if (s != null) {
      throw new IllegalArgumentException(s);
    }
  }

  /**
   * @return the name 
   */
  public String getName() {
    return name;
  }

  /**
   * @return the wrapped object
   */
  public Object getObject() {
    return obj;
  }

  public String toString() {
    return "(tag name="+name+" value="+obj+")";
  }
}
