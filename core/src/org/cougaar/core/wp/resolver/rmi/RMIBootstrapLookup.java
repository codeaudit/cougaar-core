/*
 * <copyright>
 *  
 *  Copyright 2002-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.core.wp.resolver.rmi;

import java.net.URI;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.rmi.server.RMISocketFactory;
import java.util.HashMap;

import org.cougaar.core.service.SocketFactoryService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.wp.Timestamp;
import org.cougaar.core.wp.resolver.BootstrapLookupBase;

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
 *    (Foo, alias, name:///NodeX)
 * </pre>
 * This is handy if you want multiple nodes to race to
 * become the alias entry based upon which node is first to
 * launch and register in the rmi registry.
 * <p>
 * We use the SocketFactoryService (generally supplied by the MTS) to
 * find SocketFactories.
 *
 * @property org.cougaar.core.wp.resolver.rmi.resolveHosts
 *   Boolean-valued property to do an InetAddress resolution of the
 *   bootstrap entry's host name when using the RMI registry.  For
 *   example, if the bootstrap entry was:<pre>
 *    (NodeX, -RMI_REG, rmi://localhost:8000/NodeX)
 *   </pre> then this would resolve "localhost" to "127.0.0.1" and
 *   act as if the bootstrap entry was:<pre>
 *    (NodeX, -RMI_REG, rmi://127.0.0.1:8000/NodeX)
 *   </pre>.  RMI registry access is fairly picky about the host
 *   name, so this defaults to 'true'.
 *  
 * @property org.cougaar.core.wp.resolver.rmi.useSSL
 *   Boolean-valued property which controls whether or not ssl is used
 *   in communication to the RMI registry.  Defaults to 'false'.
 *
 * @property org.cougaar.core.naming.useSSL
 *   Backwards compatibility for the
 *   "org.cougaar.core.wp.resolver.rmi.useSSL"
 *   system property.
 */
public class RMIBootstrapLookup
extends BootstrapLookupBase
{

  // all RMI registry lookups will prefix the name
  // with this path prefix, to keep the registry tidy
  private static final String DIR_PREFIX = "COUGAAR_WP/";

  private static final long MIN_LOOKUP_DELAY = 
    Long.parseLong(
        System.getProperty(
          "org.cougaar.core.wp.resolver.rmi.minLookup",
          "8000"));
  private static final long MAX_LOOKUP_DELAY = 
    Long.parseLong(
        System.getProperty(
          "org.cougaar.core.wp.resolver.rmi.maxLookup",
          "120000"));
  private static final long MIN_VERIFY_DELAY = 
    Long.parseLong(
        System.getProperty(
          "org.cougaar.core.wp.resolver.rmi.minVerify",
          "60000"));
  private static final long MAX_VERIFY_DELAY = 
    Long.parseLong(
        System.getProperty(
          "org.cougaar.core.wp.resolver.rmi.maxVerify",
          "180000"));

  // should we do an InetAddress lookup of the bindEntry's
  // host name?
  private static final String RESOLVE_HOSTS_PROP =
    "org.cougaar.core.wp.resolver.rmi.resolveHosts";
  private static final boolean RESOLVE_HOSTS =
    "true".equals(System.getProperty(
          RESOLVE_HOSTS_PROP, "true"));

  // if an alias is created, and a subsequent verification
  // fails, should we allow a new alias name?
  private static final String ALLOW_ALIAS_CHANGE_PROP =
    "org.cougaar.core.wp.resolver.rmi.allowAliasChange";
  private static final boolean ALLOW_ALIAS_CHANGE =
    "true".equals(System.getProperty(
          ALLOW_ALIAS_CHANGE_PROP, "true"));

  private static final String USE_SSL_PROP =
    "org.cougaar.core.wp.resolver.rmi.useSSL";
  private static final String OLD_USE_SSL_PROP =
    "org.cougaar.core.naming.useSSL";

  private static final Object socFacLock = new Object();
  private static RMISocketFactory socFac;

  private static SocketFactoryService socketFactoryService = null; // also locked by socFacLock to complete a disgusting hack.
  public void setSocketFactoryService(SocketFactoryService socketFactoryService) {
    synchronized (socFacLock) {
      this.socketFactoryService = socketFactoryService;
    }
  }

  private static RMISocketFactory getRMISocketFactory() {
    synchronized (socFacLock) {
      if (socFac == null) {
        boolean useSSL =
          Boolean.getBoolean(USE_SSL_PROP) ||
          Boolean.getBoolean(OLD_USE_SSL_PROP);

        HashMap p = new HashMap(11);
        p.put("ssl", Boolean.valueOf(useSSL));
        p.put("aspects", Boolean.FALSE);
        
        socFac = (RMISocketFactory) socketFactoryService.getSocketFactory(RMISocketFactory.class, p);
      }
      return socFac;
    }
  }

  protected long getMinLookupDelay() { return MIN_LOOKUP_DELAY; }
  protected long getMaxLookupDelay() { return MAX_LOOKUP_DELAY; }
  protected long getMinVerifyDelay() { return MIN_VERIFY_DELAY; }
  protected long getMaxVerifyDelay() { return MAX_VERIFY_DELAY; }

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
      (("-RMI".equals(type) || "-SSLRMI".equals(type)) &&
       "rmi".equals(scheme));
  }

  protected boolean shouldReplace(
      AddressEntry oldEntry,
      AddressEntry newEntry) {
    String oldType = (oldEntry == null ? null : oldEntry.getType());
    String newType = (newEntry == null ? null : newEntry.getType());
    // prefer SSLRMI over RMI
    //
    // FIXME this can get confused upon unbind, since a null is
    // passed instead of the unbound entry.  For now this is fine,
    // since we only unbind these entries on agent unload.
    return
      (!("-SSLRMI".equals(oldType) && "-RMI".equals(newType)));
  }

  protected LookupTimer createLookupTimer(AddressEntry bootEntry) {
    AddressEntry ae = bootEntry;
    if (RESOLVE_HOSTS) {
      try {
        ae = resolveHostAddress(bootEntry);
      } catch (Exception e) {
        if (logger.isWarnEnabled()) {
          logger.warn(
              "Unable to resolve hostname in white pages"+
              " bootstrap entry: "+bootEntry, e);
        }
      }
    }
    if (ae != bootEntry && logger.isDebugEnabled()) {
      logger.debug(
          "Replaced bootstrap host name in "+bootEntry+
          " with IP address "+ae);
    }
    return new RMILookupTimer(ae);
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

        boolean failed = false;

        // lookup/create registry
        Registry r = lookupRegistry(
            host,
            port,
            (shouldBind && bindEntry != null));
        if (r == null) {
          failed = true;
          logger.printDot("!");
        }

        // lookup/create exported rmi object
        RMIAccess newAccess = null;
        if (!failed) {
          newAccess = lookupAccess(r, name);
          if (newAccess == null) {
            if (shouldBind &&
                bindEntry != null &&
                ((ALLOW_ALIAS_CHANGE || aliasId == null) ||
                 aliasId.equals(id))) {
              newAccess = bindAccess(r, name, bindEntry);
            }
            if (newAccess == null) {
              failed = true;
            }
          }
        }

        // lookup the entry in the rmi object
        AddressEntry newFound = null;
        if (!failed) { 
          newFound = lookupEntry(newAccess);
          if (newFound == null) {
            failed = true;
          }
        }

        if (failed) {
          if (logger.isInfoEnabled()) {
            long now = System.currentTimeMillis();
            long d = (delay << 1);
            if (getMaxLookupDelay() < d) {
              d = getMaxLookupDelay();
            }
            long retryTime = now + d;
            logger.info(
                "Unable to lookup"+
                (shouldBind ? 
                 (r == null ? "/create" :
                  newAccess == null ? "/bind" :
                  "") : "")+
                (r == null ? "" : newAccess == null ?
                 (" rmi remote object"+
                  " for (name="+name+", entry="+bindEntry+")"+
                  ") in the") :
                 (" entry in the"+
                  " rmi remote object"+
                  " ("+newAccess+
                  ") found under name ("+name+
                  ") in the"))+
                " rmi registry on "+host+":"+port+
                ((r == null && shouldBind && bindEntry == null) ?
                 (", waiting for a local bind(name="+id+
                  ", type=(-RMI|-SSLRMI)) or timer") :
                 "")+
                ", will attempt another lookup at "+
                Timestamp.toString(retryTime,now));
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
          String eMsg = e.getMessage();
          if (e instanceof java.rmi.ConnectException &&
              eMsg != null &&
              eMsg.startsWith("Connection refused")) {
            if (logger.isDebugEnabled()) {
              logger.debug(
                  "Unable to access registry on "+host+":"+port+
                  " (Connection refused, the registry probably"+
                  "  doesn't exist)");
            }
          } else {
            if (logger.isInfoEnabled()) {
              logger.info(
                  "Unable to access registry on "+host+":"+port+
                  " (unknown exception)", e);
            }
          }
          if (r == null && autoStart && isLocalHost(host)) {
            if (logger.isDebugEnabled()) {
              logger.debug("Create registry on "+host+":"+port);
            }
            try {
              r = LocateRegistry.createRegistry(port, rsf, rsf);
            } catch (Exception e2) {
              String e2Msg = e2.getMessage();
              if (e2 instanceof java.rmi.server.ExportException &&
                  e2Msg != null &&
                  e2Msg.startsWith("Port already in use")) {
                if (logger.isInfoEnabled()) {
                  logger.info(
                      "Unable to create registry on "+host+":"+port+
                      " (possibly another local Node raced ahead and"+
                      " created it and we'll find it later)",
                      e2);
                }
                // we'll try again later...
              } else {
                if (logger.isErrorEnabled()) {
                  boolean isMultipleRegistryBug =
                    (e2 instanceof ExportException &&
                     e2Msg != null &&
                     e2Msg.equals("internal error: ObjID already in use"));
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
          } catch (NotBoundException nbe) {
            if (logger.isDebugEnabled()) {
              logger.debug(
                  "RMI object not bound ("+
                  "id="+id+
                  ", path="+path+
                  ", reg="+r+")");
            }
          } catch (Exception e) {
            if (logger.isInfoEnabled()) {
              logger.info(
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
          "(rmi_bootstrap "+
          super.toString()+
          ", shouldBind="+
          (setShouldBind ?
           (shouldBind ? "true" : "false") :
           "not_set") +
          ", rmiAccess="+rmiAccess+
          ")";
      }
    }
}
