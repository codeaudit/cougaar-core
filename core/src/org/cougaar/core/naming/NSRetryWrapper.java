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

import java.util.Collection;
import java.rmi.Remote;
import java.rmi.RemoteException;
import javax.naming.*;

public class NSRetryWrapper implements NS {
  public final static int MAXTRIES = 10;
  NS ns;
  public NSRetryWrapper(NS ns) {
    this.ns = ns;
  }

  public NSKey createSubDirectory(NSKey nsKey, 
                                  String subDirName,
                                  Collection attributes)
    throws RemoteException, NamingException, NameAlreadyBoundException
  {
    int ntries = 0;
    while (true) {
      try {
        return ns.createSubDirectory(nsKey, subDirName, attributes);
      } catch (RemoteException re) {
        if (!re.getMessage().startsWith("Connection refused") || ++ntries >= MAXTRIES) throw re;
      }
      try {
        Thread.sleep(500);
      } catch (InterruptedException ie) {
      }
    }
  }

  public NSKey createSubDirectory(NSKey nsKey, 
                                  String subDirName) 
    throws RemoteException, NamingException, NameAlreadyBoundException
  {
    int ntries = 0;
    while (true) {
      try {
        return ns.createSubDirectory(nsKey, subDirName);
      } catch (RemoteException re) {
        if (!re.getMessage().startsWith("Connection refused") || ++ntries >= MAXTRIES) throw re;
      }
      try {
        Thread.sleep(500);
      } catch (InterruptedException ie) {
      }
    }
  }
  
  public void destroySubDirectory(NSKey nsKey, String subDirName)
    throws RemoteException, ContextNotEmptyException, NameNotFoundException, NamingException
  {
    int ntries = 0;
    while (true) {
      try {
        destroySubDirectory(nsKey, subDirName);
        return;
      } catch (RemoteException re) {
        if (!re.getMessage().startsWith("Connection refused") || ++ntries >= MAXTRIES) throw re;
      }
      try {
        Thread.sleep(500);
      } catch (InterruptedException ie) {
      }
    }
  }
  
  public Collection entrySet(NSKey nsKey) 
    throws RemoteException, NameNotFoundException, NamingException
  {
    int ntries = 0;
    while (true) {
      try {
        return ns.entrySet(nsKey);
      } catch (RemoteException re) {
        if (!re.getMessage().startsWith("Connection refused") || ++ntries >= MAXTRIES) throw re;
      }
      try {
        Thread.sleep(500);
      } catch (InterruptedException ie) {
      }
    }
  }
  
  public String fullName(NSKey nsKey, String name)
    throws RemoteException, NameNotFoundException, NamingException
  {
    int ntries = 0;
    while (true) {
      try {
        return fullName(nsKey, name);
      } catch (RemoteException re) {
        if (!re.getMessage().startsWith("Connection refused") || ++ntries >= MAXTRIES) throw re;
      }
      try {
        Thread.sleep(500);
      } catch (InterruptedException ie) {
      }
    }
  }

  public Object getKey(NSKey nsKey, String name)
    throws RemoteException, NameNotFoundException, NamingException
  {
    int ntries = 0;
    while (true) {
      try {
        return ns.getKey(nsKey, name);
      } catch (RemoteException re) {
        if (!re.getMessage().startsWith("Connection refused") || ++ntries >= MAXTRIES) throw re;
      }
      try {
        Thread.sleep(500);
      } catch (InterruptedException ie) {
      }
    }
  }
        
  public Object get(NSKey nsKey, String name) 
    throws RemoteException, NameNotFoundException, NamingException
  {
    int ntries = 0;
    while (true) {
      try {
        return ns.get(nsKey, name);
      } catch (RemoteException re) {
        if (!re.getMessage().startsWith("Connection refused") || ++ntries >= MAXTRIES) throw re;
      }
      try {
        Thread.sleep(500);
      } catch (InterruptedException ie) {
      }
    }
  }

  public Collection getAttributes(NSKey nsKey, 
                                  String name) 
    throws RemoteException, NameNotFoundException, NamingException
  {
    int ntries = 0;
    while (true) {
      try {
        return ns.getAttributes(nsKey, name);
      } catch (RemoteException re) {
        if (!re.getMessage().startsWith("Connection refused") || ++ntries >= MAXTRIES) throw re;
      }
      try {
        Thread.sleep(500);
      } catch (InterruptedException ie) {
      }
    }
  }

  public NSKey getRoot() throws RemoteException {
    int ntries = 0;
    while (true) {
      try {
        return ns.getRoot();
      } catch (RemoteException re) {
        if (!re.getMessage().startsWith("Connection refused") || ++ntries >= MAXTRIES) throw re;
      }
      try {
        Thread.sleep(500);
      } catch (InterruptedException ie) {
      }
    }
  }
 
  public boolean isEmpty(NSKey nsKey) 
    throws RemoteException, NamingException, NameNotFoundException
  {
    int ntries = 0;
    while (true) {
      try {
        return ns.isEmpty(nsKey);
      } catch (RemoteException re) {
        if (!re.getMessage().startsWith("Connection refused") || ++ntries >= MAXTRIES) throw re;
      }
      try {
        Thread.sleep(500);
      } catch (InterruptedException ie) {
      }
    }
  }

  public Object put(NSKey nsKey, String name, Object o, boolean overwriteOkay) 
    throws RemoteException, NamingException, NameNotFoundException, NameAlreadyBoundException
  {
    int ntries = 0;
    while (true) {
      try {
        return ns.put(nsKey, name, o, overwriteOkay);
      } catch (RemoteException re) {
        if (!re.getMessage().startsWith("Connection refused") || ++ntries >= MAXTRIES) throw re;
      }
      try {
        Thread.sleep(500);
      } catch (InterruptedException ie) {
      }
    }
  }

  public Object put(NSKey nsKey, String name, Object o, Collection attributes, boolean overwriteOkay) 
    throws RemoteException, NamingException, NameNotFoundException, NameAlreadyBoundException
  {
    int ntries = 0;
    while (true) {
      try {
        return ns.put(nsKey, name, o, attributes, overwriteOkay);
      } catch (RemoteException re) {
        if (!re.getMessage().startsWith("Connection refused") || ++ntries >= MAXTRIES) throw re;
      }
      try {
        Thread.sleep(500);
      } catch (InterruptedException ie) {
      }
    }
  }

  public void putAttributes(NSKey nsKey, String name, 
                            Collection attributes) 
    throws RemoteException, NamingException, NameNotFoundException
  {
    int ntries = 0;
    while (true) {
      try {
        ns.putAttributes(nsKey, name, attributes);
        return;
      } catch (RemoteException re) {
        if (!re.getMessage().startsWith("Connection refused") || ++ntries >= MAXTRIES) throw re;
      }
      try {
        Thread.sleep(500);
      } catch (InterruptedException ie) {
      }
    }
  }

  public Object remove(NSKey nsKey, String name) 
    throws RemoteException, NamingException, NameNotFoundException, 
           OperationNotSupportedException
  {
    int ntries = 0;
    while (true) {
      try {
        return ns.remove(nsKey, name);
      } catch (RemoteException re) {
        if (!re.getMessage().startsWith("Connection refused") || ++ntries >= MAXTRIES) throw re;
      }
      try {
        Thread.sleep(500);
      } catch (InterruptedException ie) {
      }
    }
  }

  public Object rename(NSKey nsKey, String oldName, String newName) 
    throws RemoteException, NamingException, NameAlreadyBoundException, 
           NameNotFoundException, OperationNotSupportedException
  {
    int ntries = 0;
    while (true) {
      try {
        return ns.rename(nsKey, oldName, newName);
      } catch (RemoteException re) {
        if (!re.getMessage().startsWith("Connection refused") || ++ntries >= MAXTRIES) throw re;
      }
      try {
        Thread.sleep(500);
      } catch (InterruptedException ie) {
      }
    }
  }

  public int size(NSKey nsKey)
    throws RemoteException, NameNotFoundException, NamingException
  {
    int ntries = 0;
    while (true) {
      try {
        return ns.size(nsKey);
      } catch (RemoteException re) {
        if (!re.getMessage().startsWith("Connection refused") || ++ntries >= MAXTRIES) throw re;
      }
      try {
        Thread.sleep(500);
      } catch (InterruptedException ie) {
      }
    }
  }

  public Collection values(NSKey nsKey) 
    throws RemoteException, NameNotFoundException, NamingException
  {
    int ntries = 0;
    while (true) {
      try {
        return ns.values(nsKey);
      } catch (RemoteException re) {
        if (!re.getMessage().startsWith("Connection refused") || ++ntries >= MAXTRIES) throw re;
      }
      try {
        Thread.sleep(500);
      } catch (InterruptedException ie) {
      }
    }
  }

  public void registerInterest(NSKey nsKey, String[] names, NSCallback.Id cbid)
    throws RemoteException, NamingException
  {
    int ntries = 0;
    while (true) {
      try {
        ns.registerInterest(nsKey, names, cbid);
        return;
      } catch (RemoteException re) {
        if (!re.getMessage().startsWith("Connection refused") || ++ntries >= MAXTRIES) throw re;
      }
      try {
        Thread.sleep(500);
      } catch (InterruptedException ie) {
      }
    }
  }

  public void unregisterInterest(NSKey dirKey, NSCallback.Id cbid)
    throws RemoteException, NamingException
  {
    int ntries = 0;
    while (true) {
      try {
        ns.unregisterInterest(dirKey, cbid);
        return;
      } catch (RemoteException re) {
        if (!re.getMessage().startsWith("Connection refused") || ++ntries >= MAXTRIES) throw re;
      }
      try {
        Thread.sleep(500);
      } catch (InterruptedException ie) {
      }
    }
  }
}
