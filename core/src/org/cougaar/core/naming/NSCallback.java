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

import java.io.Serializable;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/** Callback interface for NameServer.
 **/

public interface NSCallback extends Remote {
  class Id implements Serializable {
    NSCallback cb;              // The callback channel
    int id;                     // Listener id number
    int interestType;           // Type of interest of the listener

    Id(NSCallback cb, int id, int type) {
      this.cb = cb;
      this.id = id;
      this.interestType = type;
    }

    public int hashCode() {
      return cb.hashCode() + id;
    }

    public boolean equals(Object o) {
      if (o instanceof NSCallback.Id) {
        NSCallback.Id that = (NSCallback.Id) o;
        if (this.id == that.id && this.interestType == that.interestType) {
          return this.cb.equals(that.cb);
        }
      }
      return false;
    }

    public String toString() {
      String idString;
      try {
        idString = cb.getIdString();
      } catch (Exception e) {
        idString = cb.toString();
      }
      return idString + "[" + id + "]";
    }
  }

  int NAMESPACE_INTEREST = 1;
  int OBJECT_INTEREST = 2;

  void dispatch(List events)
    throws RemoteException;
  String getIdString() throws RemoteException;
}
