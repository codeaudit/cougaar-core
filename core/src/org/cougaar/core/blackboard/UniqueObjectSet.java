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

package org.cougaar.core.blackboard;

import java.util.Map;
import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.util.KeyedSet;

/**
 * UniqueObjectSet is a custom container which maintains a hashtable-like
 * association between UniqueObject.getUID() and object.
 **/

public class UniqueObjectSet 
  extends KeyedSet
{
  protected Object getKey(Object o) {
    if (o instanceof UniqueObject) {
      return ((UniqueObject) o).getUID();
    } else {
      return null;
    }
  }

  public UniqueObject findUniqueObject(Object o) {
    if (o instanceof UniqueObject) {
      UID uid = ((UniqueObject)o).getUID();
      return (UniqueObject) inner.get(uid);
    } else {
      return null;
    }
  }

  public UniqueObject findUniqueObject(UniqueObject o) {
    UID uid = o.getUID();
    return (UniqueObject) inner.get(uid);
  }

  public UniqueObject findUniqueObject(UID uid) {
    return (UniqueObject) inner.get(uid);
  }

}
