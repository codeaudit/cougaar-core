/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.cluster;

import java.util.*;
import org.cougaar.util.*;

import org.cougaar.core.society.UniqueObject;
import org.cougaar.core.society.UID;

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
