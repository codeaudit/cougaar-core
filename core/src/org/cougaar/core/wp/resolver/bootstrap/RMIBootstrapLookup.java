/*
 * <copyright>
 *  Copyright 2002-2003 BBNT Solutions, LLC
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

package org.cougaar.core.wp.resolver.bootstrap;

import java.net.URI;
import java.rmi.ConnectException;
import java.rmi.Remote;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.RMISocketFactory;
import org.cougaar.core.mts.SocketFactory;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.wp.Timestamp;

/**
 * RMI-specific implementation of a bootstrap lookup.
 * <p>
 * This class watches for MTS "-RMI" binds and places the
 * resolved addresses in an RMI registry.
 * <p>
 * For example, if the bootstrap table contains an entry
 * for:<pre>
 *    (NodeX, -RMI_REG, rmi://localhost:8000/NodeX)
 * </pre>
 * this class will watch the RMI registry on "localhost:8000"
 * for a NodeX entry.  If NodeX starts on this node and the
 * MTS binds an entry for the RMI link protocol:<pre>
 *    (NodeX, -RMI, rmi://1.2.3.4:9876/oidA)
 * </pre>
 * then this class will attempt to register the entry in the
 * rmi registry.
 * <p>
 * If the bootstrap table contains an entry where the name
 * doesn't match the path:<pre>
 *    (Foo, -RMI_REG, rmi://localhost:8000/NodeX)
 * </pre>
 * then this class will treat the path as the agent id
 * and create an alias when it resolves the agent id,
 * e.g.<pre>
 *    (Foo, alias, name://NodeX)
 * </pre>
 * This is handy if you want multiple nodes to race to
 * become the alias entry based upon which node is first to
 * launch and register in the rmi registry.
 * <p>
 * For now this happens to use the same SocketFactory implementation
 * as the MTS.
 *
 * @property org.cougaar.core.wp.resolver.bootstrap.rmi.useSSL
 *   Boolean-valued property which controls whether or not ssl is used
 *   in communication to the RMI registry.  Defaults to 'false'.
 *
 * @property org.cougaar.core.naming.useSSL
 *   Backwards compatibility for the
 *   "org.cougaar.core.wp.resolver.bootstrap.rmi.useSSL"
 *   system property.
 */
public class RMIBootstrapLookup
extends BootstrapLookupBase
{

  // all RMI registry lookups will prefix the name
  // with this path prefix, to keep the registry tidy
  private static final String DIR_PREFIX = "COUGAAR_WP/";

  // pause between RMI registry lookups
  private static final long DELAY_FOR_LOOKUP = 
    Long.parseLong(
        System.getProperty(
          "org.cougaar.core.wp.resolver.bootstrap.rmi.lookup",
          "30000"));

  // pause between alias re-lookup if we find a conflicting
  // alias and `allowAliasChange()` is false.
  private static final long DELAY_FOR_RETRY_ALIAS = 
    Long.parseLong(
        System.getProperty(
          "org.cougaar.core.wp.resolver.bootstrap.rmi.retryAlias",
          "120000"));

  // pause between verification of successful lookups
  private static final long DELAY_FOR_VERIFY =
    Long.parseLong(
        System.getProperty(
          "org.cougaar.core.wp.resolver.bootstrap.rmi.verify",
          "120000"));

  // if an alias is created, and a subsequent verification
  // fails, should we allow a new alias name?
  private static final String ALLOW_ALIAS_CHANGE_PROP =
    "org.cougaar.core.wp.resolver.bootstrap.rmi.allowAliasChange";
  private static final boolean ALLOW_ALIAS_CHANGE =
    "true".equals(System.getProperty(
          ALLOW_ALIAS_CHANGE_PROP, "true"));

  private static final String USE_SSL_PROP =
    "org.cougaar.core.wp.resolver.bootstrap.rmi.useSSL";
  private static final String OLD_USE_SSL_PROP =
    "org.cougaar.core.naming.useSSL";

  private static final Object socFacLock = new Object();
  private static RMISocketFactory socFac;

  private static RMISocketFactory getRMISocketFactory() {
    synchronized (socFacLock) {
      if (socFac == null) {
        boolean useSSL =
          Boolean.getBoolean(USE_SSL_PROP) ||
          Boolean.getBoolean(OLD_USE_SSL_PROP);
        socFac = new SocketFactory(useSSL, false);
      }
      return socFac;
    }
  }

  protected long getDelayForLookup() {
    return DELAY_FOR_LOOKUP;
  }

  protected long getDelayForRetryAlias() {
    return DELAY_FOR_RETRY_ALIAS;
  }

  protected long getDelayForVerify() {
    return DELAY_FOR_VERIFY;
  }

  protected boolean allowAliasChange() {
    return ALLOW_ALIAS_CHANGE;
  }

  protected boolean isBootEntry(AddressEntry entry) {
    String type = entry.getType();
    String scheme = entry.getURI().getScheme();
    return
      ("-RMI_REG".equals(type) &&
       "rmi".equals(scheme));
  }

  protected boolean isBindEntry(AddressEntry entry) {
    String type = entry.getType();
    String scheme = entry.getURI().getScheme();
    return
      (("-RMI".equals(type) || "-RMISSL".equals(type)) &&
       "rmi".equals(scheme));
  }

  protected LookupTimer createLookupTimer(AddressEntry bootEntry) {
    return new RMILookupTimer(bootEntry);
  }

  private class RMILookupTimer
    extends LookupTimer {

      // set to true if the bootEntry host is localhost
      private boolean setShouldBind;
      private boolean shouldBind;

      // this is the active RMI RemoteObject that was found
      // in the RMI registry upon successful LOOKUP.
      private RMIAccess rmiAccess;

      public RMILookupTimer(AddressEntry bootEntry) {
        super(bootEntry);
      }

      protected AddressEntry doLookup() {
        String name = bootEntry.getName();
        URI uri = bootEntry.getURI();
        String host = uri.getHost();
        int port = uri.getPort();

        if (!setShouldBind) {
          // we can bind our bindEntry if:
          //   the rmi registry is on our host
          //
          // we also check for a non-null bindEntry, since
          // that changes dynamically based upon the local
          // WP bind/unbind activity
          shouldBind = isLocalHost(host);
        }

        // lookup/create registry
        Registry r = lookupRegistry(
            host,
            port,
            (shouldBind && bindEntry != null));
        if (r == null) {
          if (logger.isInfoEnabled()) {
            long now = System.currentTimeMillis();
            long delay = now+getDelayForLookup();
            logger.info(
                "Unable to lookup"+
                (shouldBind ? "/create" : "")+
                " rmi registry on "+host+":"+port+
                ", will attempt another lookup at "+
                Timestamp.toString(delay,now));
          }
          logger.printDot("!");
          return null;
        }

        // lookup/create exported rmi object
        RMIAccess newAccess = lookupAccess(r, name);
        if (newAccess == null) {
          if (shouldBind &&
              bindEntry != null &&
              ((ALLOW_ALIAS_CHANGE || aliasId == null) ||
               aliasId.equals(id))) {
            newAccess = bindAccess(r, name, bindEntry);
          }
          if (newAccess == null) {
            if (logger.isInfoEnabled()) {
              long now = System.currentTimeMillis();
              long delay = now+getDelayForLookup();
              logger.info(
                  "Unable to lookup"+
                  (shouldBind ? "/bind" : "")+
                  " rmi remote object for (name="+
                  name+", entry="+bindEntry+") in the "+
                  " rmi registry on "+host+":"+port+
                  ", will attempt another lookup "+
                  Timestamp.toString(delay,now));
            }
            return null;
          }
        }

        // lookup the entry in the rmi object
        AddressEntry newFound = lookupEntry(newAccess);
        if (newFound == null) {
          if (logger.isInfoEnabled()) {
            long now = System.currentTimeMillis();
            long delay = now+getDelayForLookup();
            logger.info(
                "Unable to lookup entry in the rmi"+
                " remote object ("+newAccess+
                ") found under name ("+name+
                ") in the rmi registry on "+host+":"+port+
                ", will attempt another lookup "+
                Timestamp.toString(delay,now));
          }
          return null;
        }

        // save our remote object for later verification
        rmiAccess = newAccess;

        return newFound;
      }

      protected AddressEntry doVerify() {
        // make sure our remote object is working
        return lookupEntry(rmiAccess);
      }

      //
      // rmi-specific utility methods:
      //

      private Registry lookupRegistry(
          String host, int port, boolean autoStart) {
        RMISocketFactory rsf = getRMISocketFactory();

        if (logger.isDebugEnabled()) {
          logger.debug(
              "Lookup registry (host="+host+
              ", port="+port+", autoStart="+autoStart+")");
        }

        Registry r = null;
        try {
          r = LocateRegistry.getRegistry(host, port, rsf);

          // test that this is a real registry
          try {
            r.list();
          } catch (Exception e) {
            r = null;
            throw e;
          }

          if (logger.isDebugEnabled()) {
            logger.debug("Found existing registry: "+r);
          }
        } catch (Exception e) {
          if (logger.isDebugEnabled()) {
            logger.debug(
                "Unable to access registry on "+host+":"+port+" ("+
                (e instanceof java.rmi.ConnectException ?
                 "possibly doesn't exist" :
                 "unknown exception")+")", e);
          }
          if (r == null && autoStart && isLocalHost(host)) {
            if (logger.isDebugEnabled()) {
              logger.debug("Create registry on "+host+":"+port);
            }
            try {
              r = LocateRegistry.createRegistry(port, rsf, rsf);
            } catch (Exception e2) {
              if (logger.isErrorEnabled()) {
                boolean isMultipleRegistryBug =
                  ((e2 instanceof ExportException) &&
                   "internal error: ObjID already in use".equals(
                     e2.getMessage()));
                logger.error(
                    "Unable to create RMI registry on "+host+":"+port+
                    (isMultipleRegistryBug ?
                     ", is another RMI registry running"+
                     " on this JVM (Sun bug 4267864)" :
                     ""), e2);
              }
            }
          }
        }

        if (logger.isDebugEnabled()) {
          logger.debug(
              "Located registry (host="+host+", port="+port+"): "+r);
        }
        return r;
      }

      private RMIAccess lookupAccess(Registry r, String id) {
        if (logger.isDebugEnabled()) {
          logger.debug("Lookup rmi object "+id+" in "+r);
        }

        RMIAccess rObj = null;
        if (r != null) {
          String path = DIR_PREFIX + id;
          try {
            rObj = (RMIAccess) r.lookup(path);
          } catch (Exception e) {
            if (logger.isDebugEnabled()) {
              logger.debug(
                  "Failed rmi object lookup ("+
                  "id="+id+
                  ", path="+path+
                  ", reg="+r+")", e);
            }
          }
        }

        if (logger.isDebugEnabled()) {
          logger.debug("Located rmi object "+id+": "+rObj);
        }
        return rObj;
      }

      private RMIAccess bindAccess(
          Registry r, String id, AddressEntry entry) {
        if (logger.isDebugEnabled()) {
          logger.debug(
              "Bind remote object (id="+id+", entry="+entry+")");
        }

        RMIAccess rObj = null;
        if (entry != null) {
          String path = DIR_PREFIX + id;
          try {
            RMISocketFactory rsf = getRMISocketFactory();
            RMIAccess t = new RMIAccessImpl(entry, rsf, rsf);
            r.bind(path, t);
            rObj = t;
          } catch (Exception e) {
            if (logger.isDebugEnabled()) {
              logger.debug(
                  "Failed rmi object bind ("+
                  "id="+id+
                  ", entry="+entry+
                  ", path="+path+
                  ", reg="+r+")", e);
            }
          }
        }

        if (logger.isDebugEnabled()) {
          logger.debug("Bound rmi object "+id+": "+rObj);
        }
        return rObj;
      }

      private AddressEntry lookupEntry(RMIAccess rObj) {
        if (logger.isDebugEnabled()) {
          logger.debug("Lookup entry in remote object "+rObj);
        }

        AddressEntry entry = null;
        if (rObj != null) {
          try {
            entry = rObj.getAddressEntry();
          } catch (Exception e) {
            if (logger.isDebugEnabled()) {
              logger.debug(
                  "Failed remote object "+rObj+" invocation", e);
            }
          }
        }

        if (logger.isDebugEnabled()) {
          logger.debug("Found remote object "+rObj+" entry: "+entry);
        }
        return entry;
      }

      public String toString() {
        return
          "RMILookupTimer {"+
          super.toString()+
          "\n  shouldBind: "+
          (setShouldBind ?
           (shouldBind ? "true" : "false") :
           "not_set") +
          "\n  rmiAccess: "+rmiAccess+
          "\n}";
      }
    }
}
