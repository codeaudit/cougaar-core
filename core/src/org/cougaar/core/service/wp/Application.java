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

package org.cougaar.core.service.wp;

import java.io.Serializable;
import java.util.HashMap;

/**
 * An address entry must specify an application.
 */
public final class Application implements Serializable {

  private static final HashMap apps = new HashMap(13);
       
  private String name;
  private transient int _hc;
       
  public static Application getApplication(String as) {
    as = as.intern();
    synchronized (apps) {
      Application a = (Application) apps.get(as);
      if (a == null) {
        a = new Application(as);
        apps.put(as, a);
      }
      return a;
    }
  }

  /** @see #getApplication(String) */
  private Application(String a) {
    this.name = a;
    // assert (a.intern() == a);
  }
       
  public String toString() {
    return name;
  }
       
  public boolean equals(Object a) {
    return
      (this == a ||
       (a instanceof Application &&
        name == ((Application)a).name));
  }
 
  public int hashCode() {
    if (_hc == 0) _hc = name.hashCode();
    return _hc;
  }

  private Object readResolve() {
    // replace with an interned variation
    return getApplication(name);
  }
}
