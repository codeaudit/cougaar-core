/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.society.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

import java.util.*;
  
/** actual RMI remote interface for RMI Nameserver objects.
 **/

public interface NS extends Remote {
  void clear(String directory) throws RemoteException;

  boolean containsKey(String key) throws RemoteException;

  Collection entrySet(String directory) throws RemoteException;
  
  /** Look up an object in the NameService directory **/
  Object get(String name) throws RemoteException;

  boolean isEmpty(String directory) throws RemoteException;

  Collection keySet(String directory) throws RemoteException;

  /** add an object to the directory **/
  Object put(String name, Object o) throws RemoteException;

  /** remove an object (and name) from the directory **/
  Object remove(String name) throws RemoteException;

  int size(String directory) throws RemoteException;

  /** @return all objects in the specified directory **/
  Collection values(String directory) throws RemoteException;
}
