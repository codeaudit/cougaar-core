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

package org.cougaar.domain.planning.ldm.plan;

import java.io.*;
import java.util.*;

/**
 * Verb is the action part of a Task.
 *
 */

public final class Verb implements Serializable {
  // verb cache - needs to be declared before the statics below
  private static HashMap verbs = new HashMap(29);
  	
  private String name;
	
  /** Constructor takes a String that represents the verb */
  public Verb(String v) {
    if (v == null) throw new IllegalArgumentException();
    name = v.intern();
    cacheVerb(this);
  }
	
  /** @return String toString returns the String that represents the verb */
  public String toString() {
    return name;
  }
	
  /** Capabilities are equal IFF they encapsulate the same string
   */
  public boolean equals(Object v) {
    // use == since verb strings are interned
    return (this == v || 
            (v instanceof Verb && name == ((Verb)v).name) ||
            (v instanceof String && name.equals((String) v))
            );
  }
  
  /** convenience method for verb testing */
  public boolean equals(String v) {
    return ( name==v || name.equals(v));
  }

  private transient int _hc = 0;
  public int hashCode()
  {
    if (_hc == 0) _hc = name.hashCode();
    return _hc;
  }

  private void readObject(ObjectInputStream stream)
                throws ClassNotFoundException, IOException
  {
    stream.defaultReadObject();
    //if (name != null) name = name.intern();
  }

  // replace with an interned variation
  protected Object readResolve() {
    return getVerb(name);
  }

  // verb hash

  public static Verb getVerb(String vs) {
    vs = vs.intern();
    synchronized (verbs) {
      Verb v = (Verb) verbs.get(vs);
      if (v != null) 
        return v;
      else
        return new Verb(vs);    // calls cacheVerb
    }
  }

  public static void cacheVerb(Verb v) {
    String vs = v.toString();
    synchronized (verbs) {
      verbs.put(vs, v);
    }
  }
}
