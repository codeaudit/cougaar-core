/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.naming;

import java.rmi.Remote;
import java.rmi.RemoteException;

import java.util.*;

import org.cougaar.core.society.NameServer;
  
/** actual remote interface for NameServer objects.
 **/

public interface NS extends Remote {
  public static final String DirSeparator = "/";
  
  void clear(String directory) throws RemoteException;
  void clear(NameServer.Directory directory) throws RemoteException;

  boolean containsKey(String key) throws RemoteException;
  boolean containsKey(NameServer.Directory directory, String key) throws RemoteException;

  
  NameServer.Directory createSubDirectory(NameServer.Directory directory, 
                                          String subDirName) throws RemoteException;
 
  void destroySubDirectory(NameServer.Directory directory) throws RemoteException;
 
  Collection entrySet(String directory) throws RemoteException;
  Collection entrySet(NameServer.Directory directory) throws RemoteException;
  
  String fullName(NameServer.Directory directory, String name) throws RemoteException;

  /** Look up an object in the NameService directory **/
  Object get(String name) throws RemoteException;
  Object get(NameServer.Directory directory, String name) throws RemoteException;

  Collection getAttributes(String name) throws RemoteException;
  Collection getAttributes(NameServer.Directory directory, 
                           String name) throws RemoteException;

  NameServer.Directory getRoot() throws RemoteException;
 
  boolean isEmpty(String directory) throws RemoteException;
  boolean isEmpty(NameServer.Directory directory) throws RemoteException;

  Collection keySet(String directory) throws RemoteException;
  Collection keySet(NameServer.Directory directory) throws RemoteException;

  /** add an object to the directory **/
  Object put(String name, Object o) throws RemoteException;
  Object put(String name, Object o, Collection attributes) throws RemoteException;
  Object put(NameServer.Directory directory, String name, Object o) throws RemoteException;
  Object put(NameServer.Directory directory, String name, Object o, Collection attributes) throws RemoteException;

  void putAttributes(String name, Collection attributes) throws RemoteException;
  void putAttributes(NameServer.Directory directory, String name, 
                           Collection attributes) throws RemoteException;

  /** remove an object (and name) from the directory **/
  Object remove(String name) throws RemoteException;
  Object remove(NameServer.Directory directory, String name) throws RemoteException;

  Object rename(NameServer.Directory directory, String oldName, String newName) throws RemoteException;

  int size(String directory) throws RemoteException;
  int size(NameServer.Directory directory) throws RemoteException;

  /** @return all objects in the specified directory **/
  Collection values(String directory) throws RemoteException;
  Collection values(NameServer.Directory directory) throws RemoteException;
}






