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

package org.cougaar.core.naming;

import java.rmi.Remote;
import java.rmi.RemoteException;

import java.util.*;

import org.cougaar.core.society.NameServer;
  
/** actual remote interface for NameServer objects.
 **/

public interface NS extends Remote {
  String DirSeparator = "/";
  
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






