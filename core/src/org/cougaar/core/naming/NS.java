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

import javax.naming.*;

/** actual remote interface for NameServer objects.
 **/

public interface NS extends Remote {
  String DirSeparator = "/";
  
  void clear(NSKey nsKey) throws RemoteException, NameNotFoundException;

  
  NSKey createSubDirectory(NSKey nsKey, 
                           String subDirName,
                           Collection attribute) 
    throws RemoteException, NamingException, NameAlreadyBoundException;

  NSKey createSubDirectory(NSKey nsKey, 
                           String subDirName) 
    throws RemoteException, NamingException, NameAlreadyBoundException;
  
  void destroySubDirectory(NSKey nsKey, String subDirName) 
    throws RemoteException, ContextNotEmptyException, NameNotFoundException, NamingException;
  
  Collection entrySet(NSKey nsKey) 
    throws RemoteException, NameNotFoundException, NamingException;
  
  String fullName(NSKey nsKey, String name) 
    throws RemoteException, NameNotFoundException, NamingException;

  /** Look up an object in the NameService directory **/
  Object get(NSKey nsKey, String name) 
    throws RemoteException, NameNotFoundException, NamingException;

  Collection getAttributes(NSKey nsKey, 
                           String name) 
    throws RemoteException, NameNotFoundException, NamingException;

  NSKey getRoot() throws RemoteException;
 
  boolean isEmpty(NSKey nsKey) 
    throws RemoteException, NamingException, NameNotFoundException;

  Collection keySet(NSKey nsKey) 
    throws RemoteException, NamingException, NameNotFoundException;

  /** add an object to the directory **/
  Object put(NSKey nsKey, String name, Object o, boolean overwriteOkay) 
    throws RemoteException, NamingException, NameNotFoundException, NameAlreadyBoundException;
  Object put(NSKey nsKey, String name, Object o, Collection attributes, boolean overwriteOkay) 
    throws RemoteException, NamingException, NameNotFoundException, NameAlreadyBoundException;

  void putAttributes(NSKey nsKey, String name, 
                           Collection attributes) 
    throws RemoteException, NamingException, NameNotFoundException;

  /** remove an object (and name) from the directory **/
  Object remove(NSKey nsKey, String name) 
    throws RemoteException, NamingException, NameNotFoundException, 
      OperationNotSupportedException;

  Object rename(NSKey nsKey, String oldName, String newName) 
    throws RemoteException, NamingException, NameAlreadyBoundException, 
      NameNotFoundException, OperationNotSupportedException;

  int size(NSKey nsKey) 
    throws RemoteException, NameNotFoundException, NamingException;

  /** @return all objects in the specified directory **/
  Collection values(NSKey nsKey) 
    throws RemoteException, NameNotFoundException, NamingException;
}






