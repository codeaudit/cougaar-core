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

import java.util.Collection;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.ConnectIOException;
import java.net.SocketTimeoutException;
import javax.naming.*;
import org.cougaar.util.log.*;

public class NSRetryWrapper implements NS {
  public final static int MAXTRIES = 10;
  NS ns;
  Logger log;
  public NSRetryWrapper(NS ns) {
    this.ns = ns;
    log = LoggerFactory.getInstance().createLogger(getClass());
    if (log.isInfoEnabled())
        log.info("RetryWrapper logging on");
  }

  private void logInfo(String methodName, String arg) {
      if (log.isDebugEnabled()) {
        log.debug("Nameserver access: " + methodName+" arg = "+arg, new Throwable());
      }
      else if (log.isInfoEnabled()) {
        log.info("Nameserver access: " + methodName+" arg = "+arg);
      }
  }
  
  private void logSuccess(String methodName) {
    log.warn("Successful retry during " + methodName);
  }

  private void handleException(RemoteException re, String methodName, int ntries, boolean isReadOnly)
    throws RemoteException
  {
    String msg = re.getMessage();
    /* Let's retry any read-only access */
    boolean doRetry = msg.startsWith("Connection refused") || isReadOnly;
    if (!doRetry) {
      doRetry = (isReadOnly &&
                 re instanceof ConnectIOException &&
                 re.getCause() instanceof SocketTimeoutException);
    }
    if (!doRetry) throw re;

    if (ntries >= MAXTRIES) {
      if (log.isWarnEnabled()) {
        log.warn("Too many retries during " + methodName);
      }
      throw re;
    }
    if (ntries == 1) {
      if (log.isWarnEnabled()) {
        log.warn("Connection refused during " + methodName + " retrying...");
      }
    }
    try {
      long delay = 250L << Math.min(ntries, 8);
      Thread.sleep(delay);
    } catch (InterruptedException ie) {
    }
  }

  public NSKey createSubDirectory(NSKey nsKey, 
                                  String subDirName,
                                  Collection attributes)
    throws RemoteException, NamingException, NameAlreadyBoundException
  {
    int ntries = 0;
    while (true) {
      try {
        NSKey ret = ns.createSubDirectory(nsKey, subDirName, attributes);
        if (ntries > 0 && log.isWarnEnabled()) logSuccess("createSubDirectory");
        return ret;
      } catch (RemoteException re) {
        handleException(re, "createSubDirectory", ++ntries, false);
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
        NSKey ret = ns.createSubDirectory(nsKey, subDirName);
        if (ntries > 0 && log.isWarnEnabled()) logSuccess("createSubDirectory");
        return ret;
      } catch (RemoteException re) {
        handleException(re, "createSubDirectory", ++ntries, false);
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
        if (ntries > 0 && log.isWarnEnabled()) logSuccess("destroySubDirectory");
        return;
      } catch (RemoteException re) {
        handleException(re, "destroySubDirectory", ++ntries, false);
      }
    }
  }
  
  public Collection entrySet(NSKey nsKey) 
    throws RemoteException, NameNotFoundException, NamingException
  {
    int ntries = 0;
    while (true) {
      try {
        Collection ret = ns.entrySet(nsKey);
        if (ntries > 0 && log.isWarnEnabled()) logSuccess("entrySet");
        return ret;
      } catch (RemoteException re) {
        handleException(re, "entrySet", ++ntries, true);
      }
    }
  }
  
  public String fullName(NSKey nsKey, String name)
    throws RemoteException, NameNotFoundException, NamingException
  {
    int ntries = 0;
    while (true) {
      try {
        String ret = ns.fullName(nsKey, name);
        if (ntries > 0 && log.isWarnEnabled()) logSuccess("fullName");
        return ret;
      } catch (RemoteException re) {
        handleException(re, "fullName", ++ntries, true);
      }
    }
  }

  public Object getKey(NSKey nsKey, String name)
    throws RemoteException, NameNotFoundException, NamingException
  {
    int ntries = 0;
    while (true) {
      try {
        Object ret = ns.getKey(nsKey, name);
        if (ntries > 0 && log.isWarnEnabled()) logSuccess("getKey");
        return ret;
      } catch (RemoteException re) {
        handleException(re, "getKey", ++ntries, true);
      }
    }
  }
        
  public Object get(NSKey nsKey, String name) 
    throws RemoteException, NameNotFoundException, NamingException
  {
    int ntries = 0;
    while (true) {
      try {
        Object ret = ns.get(nsKey, name);
        if (ntries > 0 && log.isWarnEnabled()) logSuccess("get");
        return ret;
      } catch (RemoteException re) {
        handleException(re, "get", ++ntries, true);
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
        Collection ret = ns.getAttributes(nsKey, name);
        if (ntries > 0 && log.isWarnEnabled()) logSuccess("getAttributes");
        return ret;
      } catch (RemoteException re) {
        handleException(re, "getAttributes", ++ntries, true);
      }
    }
  }

  public NSKey getRoot() throws RemoteException {
    int ntries = 0;
    logInfo("getRoot", "");
    while (true) {
      try {
        NSKey ret = ns.getRoot();
        if (ntries > 0 && log.isWarnEnabled()) logSuccess("getRoot");
        return ret;
      } catch (RemoteException re) {
        handleException(re, "getRoot", ++ntries, true);
      }
    }
  }
 
  public boolean isEmpty(NSKey nsKey) 
    throws RemoteException, NamingException, NameNotFoundException
  {
    int ntries = 0;
    if (log.isInfoEnabled()) logInfo("isEmpty", nsKey.toString());
    while (true) {
      try {
        boolean ret = ns.isEmpty(nsKey);
        if (ntries > 0 && log.isWarnEnabled()) logSuccess("isEmpty");
        return ret;
      } catch (RemoteException re) {
        handleException(re, "isEmpty", ++ntries, true);
      }
    }
  }

  public Object put(NSKey nsKey, String name, Object o, boolean overwriteOkay) 
    throws RemoteException, NamingException, NameNotFoundException, NameAlreadyBoundException
  {
    int ntries = 0;
    logInfo("put", name);

    while (true) {
      try {
        Object ret = ns.put(nsKey, name, o, overwriteOkay);
        if (ntries > 0 && log.isWarnEnabled()) logSuccess("put");
        return ret;
      } catch (RemoteException re) {
        handleException(re, "put", ++ntries, false);
      }
    }
  }

  public Object put(NSKey nsKey, String name, Object o, Collection attributes, boolean overwriteOkay) 
    throws RemoteException, NamingException, NameNotFoundException, NameAlreadyBoundException
  {
    int ntries = 0;
    logInfo("put", name);
    while (true) {
      try {
        Object ret = ns.put(nsKey, name, o, attributes, overwriteOkay);
        if (ntries > 0 && log.isWarnEnabled()) logSuccess("put");
        return ret;
      } catch (RemoteException re) {
        handleException(re, "put", ++ntries, false);
      }
    }
  }

  public void putAttributes(NSKey nsKey, String name, 
                            Collection attributes) 
    throws RemoteException, NamingException, NameNotFoundException
  {
    int ntries = 0;
    logInfo("putAttributes", name);
    while (true) {
      try {
        ns.putAttributes(nsKey, name, attributes);
        if (ntries > 0 && log.isWarnEnabled()) logSuccess("putAttributes");
        return;
      } catch (RemoteException re) {
        handleException(re, "putAttributes", ++ntries, false);
      }
    }
  }

  public Object remove(NSKey nsKey, String name) 
    throws RemoteException, NamingException, NameNotFoundException, 
           OperationNotSupportedException
  {
    int ntries = 0;
    logInfo("remove", name);
    while (true) {
      try {
        Object ret = ns.remove(nsKey, name);
        if (ntries > 0 && log.isWarnEnabled()) logSuccess("remove");
        return ret;
      } catch (RemoteException re) {
        handleException(re, "remove", ++ntries, false);
      }
    }
  }

  public Object rename(NSKey nsKey, String oldName, String newName) 
    throws RemoteException, NamingException, NameAlreadyBoundException, 
           NameNotFoundException, OperationNotSupportedException
  {
    int ntries = 0;
    if (log.isInfoEnabled()) logInfo("rename", oldName+":"+newName);
    while (true) {
      try {
        Object ret = ns.rename(nsKey, oldName, newName);
        if (ntries > 0 && log.isWarnEnabled()) logSuccess("rename");
        return ret;
      } catch (RemoteException re) {
        handleException(re, "rename", ++ntries, false);
      }
    }
  }

  public int size(NSKey nsKey)
    throws RemoteException, NameNotFoundException, NamingException
  {
    int ntries = 0;
    if (log.isInfoEnabled()) logInfo("size", nsKey.toString());
    while (true) {
      try {
        int ret = ns.size(nsKey);
        if (ntries > 0 && log.isWarnEnabled()) logSuccess("size");
        return ret;
      } catch (RemoteException re) {
        handleException(re, "size", ++ntries, true);
      }
    }
  }

  public Collection values(NSKey nsKey) 
    throws RemoteException, NameNotFoundException, NamingException
  {
    int ntries = 0;
    if (log.isInfoEnabled()) logInfo("values", nsKey.toString());

    while (true) {
      try {
        Collection ret = ns.values(nsKey);
        if (ntries > 0 && log.isWarnEnabled()) logSuccess("values");
        return ret;
      } catch (RemoteException re) {
        handleException(re, "values", ++ntries, true);
      }
    }
  }

  public void registerInterest(NSKey nsKey, String[] names, NSCallback.Id cbid)
    throws RemoteException, NamingException
  {
    int ntries = 0;
    if (log.isInfoEnabled()) logInfo("registerInterest:"+names.length, nsKey.toString()+":"+names[0]);
    while (true) {
      try {
        ns.registerInterest(nsKey, names, cbid);
        if (ntries > 0 && log.isWarnEnabled()) logSuccess("registerInterest");
        return;
      } catch (RemoteException re) {
        handleException(re, "registerInterest", ++ntries, false);
      }
    }
  }

  public void unregisterInterest(NSKey dirKey, NSCallback.Id cbid)
    throws RemoteException, NamingException
  {
    int ntries = 0;
    if (log.isInfoEnabled()) logInfo("unRegisterInterest", dirKey.toString());
    while (true) {
      try {
        ns.unregisterInterest(dirKey, cbid);
        if (ntries > 0 && log.isWarnEnabled()) logSuccess("unregisterInterest");
        return;
      } catch (RemoteException re) {
        handleException(re, "unregisterInterest", ++ntries, false);
      }
    }
  }
}
