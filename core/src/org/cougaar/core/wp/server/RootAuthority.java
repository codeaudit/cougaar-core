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

package org.cougaar.core.wp.server;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.service.wp.AddressEntry;
import org.cougaar.core.service.wp.WhitePagesProtectionService;
import org.cougaar.core.thread.Schedulable;
import org.cougaar.core.util.UID;
import org.cougaar.core.wp.Timestamp;
import org.cougaar.core.wp.resolver.Lease;
import org.cougaar.core.wp.resolver.LeaseDenied;
import org.cougaar.core.wp.resolver.LeaseNotKnown;
import org.cougaar.core.wp.resolver.NameTag;
import org.cougaar.core.wp.resolver.Record;
import org.cougaar.core.wp.resolver.RecordIsValid;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * This is the single-point white pages server, which is the
 * authority for all naming zones.
 * <p>
 * This class will be split up and enhanced for Cougaar 10.6+.
 */
public class RootAuthority
extends GenericStateModelAdapter
implements Component
{
  // cache successes for 1.5 minutes:
  private static final String DEFAULT_SUCCESS_TTD =  "90000";
  // cache failures for  0.5 minutes:
  private static final String DEFAULT_FAIL_TTD    =  "30000";
  // leases expire after 4.0 minutes:
  private static final String DEFAULT_EXPIRE_TTD  = "240000";

  private static final int LOOKUP  = 0;
  private static final int MODIFY  = 1;
  private static final int FORWARD = 2;
  private static final int FORWARD_ANSWER = 3;

  private RootConfig config;

  private ServiceBroker sb;
  private LoggingService logger;
  private MessageAddress agentId;
  private ThreadService threadService;
  private UIDService uidService;
  private WhitePagesProtectionService protectS;

  private LookupAckService lookupAckService;
  private ModifyAckService modifyAckService;
  private ForwardAckService forwardAckService;
  private ForwardService forwardService;

  private Schedulable expireThread;
  private Schedulable forwardThread;

  private final MyClient myClient = new MyClient();

  private final Object lock = new Object();

  private final DirEntry rootDir = new DirEntry(null);

  private final Map forwardQueue = new HashMap();


  public void setParameter(Object o) {
    this.config = new RootConfig(o);
  }

  public void setBindingSite(BindingSite bs) {
    this.sb = bs.getServiceBroker();
  }

  public void setLoggingService(LoggingService logger) {
    this.logger = logger;
  }

  public void setThreadService(ThreadService threadService) {
    this.threadService = threadService;
  }

  public void setUIDService(UIDService uidService) {
    this.uidService = uidService;
  }

  public void load() {
    super.load();

    if (logger.isDebugEnabled()) {
      logger.debug("Loading server root authority");
    }

    // which agent are we in?
    AgentIdentificationService ais = (AgentIdentificationService)
      sb.getService(this, AgentIdentificationService.class, null);
    agentId = ais.getMessageAddress();
    sb.releaseService(this, AgentIdentificationService.class, ais);

    protectS = (WhitePagesProtectionService)
      sb.getService(this, WhitePagesProtectionService.class, null);
    if (logger.isDebugEnabled()) {
      logger.debug(
          (protectS == null ? "Didn't find" : "Found")+
          " white pages protection service");
    }

    // create forward timer
    Runnable forwardRunner =
      new Runnable() {
        public void run() {
          // assert thread == forwardThread;
          forwardNow();
        }
      };
    forwardThread = threadService.getThread(
      this,
      forwardRunner,
      "White pages server forward leases");
    forwardThread.schedule(config.forwardPeriod);

    // create expiration timer
    Runnable expireRunner =
      new Runnable() {
        public void run() {
          // assert thread == expireThread;
          expireLeases();
        }
      };
    expireThread = threadService.getThread(
      this,
      expireRunner,
      "White pages server expiration checker");
    expireThread.schedule(config.checkExpirePeriod);

    // register for lookups, modifies, forwards, and forward-acks
    lookupAckService = (LookupAckService)
      sb.getService(myClient, LookupAckService.class, null);
    modifyAckService = (ModifyAckService)
      sb.getService(myClient, ModifyAckService.class, null);
    forwardAckService = (ForwardAckService)
      sb.getService(myClient, ForwardAckService.class, null);
    forwardService = (ForwardService)
      sb.getService(myClient, ForwardService.class, null);
    String s =
      (lookupAckService == null  ? "LookupAckService" :
       modifyAckService == null  ? "ModifyAckService" :
       forwardAckService == null ? "forwardAckService" :
       forwardService == null    ? "forwardService" :
       null);
    if (s != null) {
      throw new RuntimeException(
          "Unable to obtain "+s);
    }
  }

  public void unload() {
    // release services
    if (forwardService != null) {
      sb.releaseService(
          myClient, ForwardService.class, forwardService);
      forwardService = null;
    }
    if (forwardAckService != null) {
      sb.releaseService(
          myClient, ForwardAckService.class, forwardAckService);
      forwardAckService = null;
    }
    if (modifyAckService != null) {
      sb.releaseService(
          myClient, ModifyAckService.class, modifyAckService);
      modifyAckService = null;
    }
    if (lookupAckService != null) {
      sb.releaseService(
          myClient, LookupAckService.class, lookupAckService);
      lookupAckService = null;
    }
    if (threadService != null) {
      sb.releaseService(this, ThreadService.class, threadService);
      threadService = null;
    }
    if (uidService != null) {
      sb.releaseService(this, UIDService.class, uidService);
      uidService = null;
    }
    if (logger != null) {
      sb.releaseService(
          this, LoggingService.class, logger);
      logger = null;
    }

    super.unload();
  }

  /**
   * Callback from on of our registered services:<ul>
   *   <li>LookupAckService:  <i>LOOKUP</i></li> 
   *   <li>ModifyAckService:  <i>MODIFY</i></li> 
   *   <li>ForwardAckService: <i>FORWARD</i></li> 
   *   <li>ForwardService:    <i>FORWARD_ANSWER</i></li> 
   * </ul>
   */
  private void handleAll(
      int action,
      MessageAddress clientAddr,
      long clientTime,
      Map m) {
    int n = (m == null ? 0 : m.size());
    if (n == 0) {
      return;
    }
    Map answers = null;
    synchronized (lock) {
      long now = System.currentTimeMillis();
      for (Iterator iter = m.entrySet().iterator();
          iter.hasNext();
          ) {
        Map.Entry me = (Map.Entry) iter.next();
        String name = (String) me.getKey();
        Object sendObj = me.getValue();
        Object answer = handle(action, name, sendObj, now);
        if (answer == null) {
          continue;
        }
        if (n == 1) {
          answers = Collections.singletonMap(name, answer);
        } else {
          if (answers == null) {
            answers = new HashMap();
          }
          answers.put(name, answer);
        }
      }
    }
    if (answers == null) {
      return; 
    }
    // ugly switch, refactor me...
    switch (action) {
      case LOOKUP:
        lookupAckService.lookupAnswer(
            clientAddr,
            clientTime,
            answers);
        break;
      case MODIFY:
        modifyAckService.modifyAnswer(
            clientAddr,
            clientTime,
            answers);
        break;
      case FORWARD:
        forwardAckService.forwardAnswer(
            clientAddr,
            clientTime,
            answers);
        break;
      case FORWARD_ANSWER:
        long maxTTD = findMaxTTD(answers);
        forwardService.forward(
            clientAddr,
            //clientTime,
            answers,
            maxTTD);
        break;
      default:
        throw new IllegalArgumentException(
            "Invalid action: "+action);
    }
  }

  private Object handle(
      int action,
      String name,
      Object sendObj,
      long now) {
    // assert (Thread.holdsLock(lock));
 
    // unwrap
    Object query = sendObj; 
    if (sendObj instanceof NameTag) {
      NameTag nametag = (NameTag) sendObj;
      String agent = nametag.getName();
      Object obj = nametag.getObject();
      if (obj instanceof WhitePagesProtectionService.Wrapper) {
        WhitePagesProtectionService.Wrapper wrapper =
          (WhitePagesProtectionService.Wrapper) obj;
        try {
          if (protectS == null) {
            throw new RuntimeException(
                "No WhitePagesProtectionService");
          }
          query = protectS.unwrap(agent, wrapper);
        } catch (Exception e) {
          if (logger.isErrorEnabled()) {
            logger.error(
                "Unable to unwrap (agent="+agent+
                " name="+name+" query="+obj+")", e);
          }
          query = null;
        }
        if (logger.isDetailEnabled()) {
          logger.detail("unwrapped "+sendObj+" to "+query);
        }
      }
    }

    switch (action) { 
      case LOOKUP:
        return lookup(name, query, now);
      case MODIFY:
        return modifyAndForward(name, query, now);
      case FORWARD:
        return receiveForward(name, query, now);
      case FORWARD_ANSWER:
        return resendForward(name, query, now);
      default:
        throw new IllegalArgumentException(
            "Invalid action: "+action);
    }
  }

  private Object lookup(
      String name,
      Object query,
      long now) {
    // assert (Thread.holdsLock(lock));

    // find the closest DirEntry if it exists
    DirEntry dir = findDir(name);

    // get the record-entry if this is a non-list operation
    RecordEntry rec = 
      (dir == null ?
       (null) :
       dir.getRecordEntry(name));

    Object answer;

    UID queryUID;
    if (query == null) {
      queryUID = null;
    } else if (query instanceof UID) {
      queryUID = (UID) query;
    } else {
      // invalid
      queryUID = null;
    }

    boolean isList = (name.charAt(0) == '.');

    // find current record info
    UID uid;
    long ttd;
    Object data;
    if (isList) {
      if (dir == null) {
        // not listed, so data is null
        uid = uidService.nextUID();
        ttd = config.failTTD;
        data = null;
      } else {
        // copy dir keys
        //
        // we could make this a rarely-modified-set
        uid = dir.getUID();
        ttd = config.successTTD;
        Map entries = dir.getEntries();
        Set keys = entries.keySet();
        data = new HashSet(keys);
      }
    } else {
      if (rec == null) {
        // not listed, so data is null
        uid = uidService.nextUID();
        ttd = config.failTTD;
        data = null;
      } else {
        // return the data
        uid = rec.getUID();
        ttd = config.successTTD;
        data = rec.getData();
      }
    }

    if (queryUID != null && queryUID.equals(uid)) {
      // validated, so we don't send back the data
      answer = new RecordIsValid(uid, ttd);
    } else {
      // return full record
      answer = new Record(uid, ttd, data);
    }

    if (logger.isDetailEnabled()) {
      logger.detail(
          "lookup (name="+name+
          " query="+query+
          ") returning "+answer);
    }

    return answer;
  }

  private Object modify(
      String name,
      Object query,
      long ttd,
      long now) {
    // assert (Thread.holdsLock(lock));

    boolean isList = (name.charAt(0) == '.');
    if (isList) {
      // invalid modify
      if (logger.isErrorEnabled()) {
        logger.error(
            "Modify (name="+name+
            " query="+query+
            ") is invalid, returning null");
      }
      return null;
    }

    // unwrap the query if it's wrapped

    Object queryContent = query;
    if (query instanceof NameTag) {
      queryContent = ((NameTag) query).getObject();
    }

    UID queryUID;
    boolean hasQueryData;
    Object queryData;
    if (queryContent instanceof UID) {
      queryUID = (UID) queryContent;
      hasQueryData = false;
      queryData = null;
    } else if (queryContent instanceof Record) {
      Record record = (Record) queryContent;
      queryUID = record.getUID();
      hasQueryData = true;
      queryData = record.getData();
    } else {
      // invalid
      queryUID = null;
      hasQueryData = false;
      queryData = null;
    }
    if (queryUID == null) {
      // invalid
      return null;
    }

    // find the closest DirEntry, create it if it doesn't exist
    DirEntry dir = findOrCreateDir(name);
    // assert (dir != null);

    // get the record-entry, which may be null
    RecordEntry rec = dir.getRecordEntry(name);

    UID uid = (rec == null ? null : rec.getUID());
    boolean sameUID = queryUID.equals(uid);

    Object answer = null;
    if (sameUID) {
      // successful lease renewal
      //
      // note that the client doesn't need to send the record
      // data to renew a lease.
    } else if (!hasQueryData) {
      // the UIDs don't match, which usually means that the
      // client thinks it's renewing data but the server
      // doesn't know the data.
      //
      // we need the full record to decide if we need to replace
      // our entry or deny this lease.  Either we crashed, or the
      // client was talking to another server that hasn't
      // replicated that data to our server yet (e.g. due to a
      // crash or network partition), or some odd race condition
      // occurred.
      answer = new LeaseNotKnown(queryUID);
    } else if (rec == null) {
      // this is a new record and the client passed us the data
    } else {
      // deconflict
      //
      // extract the version fields (if present)
      Object data = (rec == null ? null : rec.getData());
      long version = getVersion(data);
      long queryVersion = getVersion(queryData);

      if (version == 0) {
        // no version info?
        //
        // simply overwrite?
      } else if (version == queryVersion) {
        // identical versions, use UID comparison
        if (uid.compareTo(queryUID) < 0) {
          // accept the replacement record
        } else {
          // our uid is greater, so deny the request
          Object reason = 
            "Modify uid "+queryUID+
            " is less than the local uid "+uid;
          answer = new LeaseDenied(uid, reason, data);
        }
      } else if (
          0 < version ?
          version < queryVersion :
          queryVersion < version) {
        // new version
        //
        // accept the replacement record
      } else if (
        queryVersion == 0 &&
        (queryData == null ||
         (queryData instanceof Map &&
          ((Map) queryData).isEmpty())) &&
        uid.compareTo(queryUID) < 0) {
        // accept full unbind
      } else {
        // old version
        Object reason = 
          "Modify version "+queryVersion+
          " is "+
          (0 < version ? "less" : "greater")+
          " than the local version "+version;
        answer = new LeaseDenied(uid, reason, data);
      }
    }

    if (answer != null) {
      // lease is either not known or denied
      if (logger.isDetailEnabled()) {
        logger.detail(
            "modify (name="+name+
            " query="+query+
            ") returning "+answer);
      }
      return answer;
    }

    //
    // create or extend a lease
    //

    long ttl = now + ttd;

    if (sameUID) {
      // extend an existing lease
      rec.setTTL(ttl);
    } else if (
        queryData == null ||
        (queryData instanceof Map &&
         ((Map) queryData).isEmpty())) {
      // this is a full unbind
      if (rec == null) {
        // this is an odd case, where the client is telling
        // the server to unbind all its entries and the server
        // never heard of the client.  In this case the
        // client probably doesn't care what the server returns,
        // since it's already discarded its entries.  Still,
        // we must respond somehow...
      } else {
        Map entries = dir.getEntries();
        entries.remove(name);
        // bump dir uid to reflect the removed entry
        //
        // this allows "list" uid-based cache validation
        dir.setUID(uidService.nextUID());
      }
    } else {
      if (rec == null) {
        // create the record
        rec = new RecordEntry(queryUID);
        Map entries = dir.getEntries();
        entries.put(name, rec);
        // bump dir uid to reflect the added entry
        //
        // this allows "list" uid-based cache validation
        dir.setUID(uidService.nextUID());
      }
      rec.setUID(queryUID);
      rec.setTTL(ttl);
      rec.setData(queryData);
    }

    answer = new Lease(queryUID, ttd);

    if (logger.isDetailEnabled()) {
      logger.detail(
          "modify (name="+name+
          " query="+query+
          ") returning "+answer);
    }

    return answer;
  }

  private Object modifyAndForward(
      String name,
      Object query,
      long now) {
    // assert (Thread.holdsLock(lock));
    Object answer = modify(name, query, config.expireTTD, now);

    if (answer instanceof Lease) {
      // forward lease to peers (excluding self and sender)
      //
      // note that the query can be a UID or a Record.  If a UID
      // is sent and a peer doesn't know the UID, then that peer
      // will send us a "forwardAnswer" with a LeaseNotKnown.
      Lease lease = (Lease) answer;
      Object queryContent = 
        (query instanceof NameTag ?
         ((NameTag) query).getObject() :
         query);
      Record record = 
        (queryContent instanceof Record ? 
         ((Record) queryContent) :
         null);
      Forward fwd = new Forward(lease, record);
      // to all
      forwardLater(name, fwd, now);
    }

    return answer;
  }

  private Object receiveForward(
      String name,
      Object query,
      long now) {
    // assert (Thread.holdsLock(lock));
    
    if (!(query instanceof Forward)) {
      // invalid
      if (logger.isErrorEnabled()) {
        logger.error(
            "Invalid forward (name="+name+", query="+query+")");
      } 
      return null; 
    }

    Forward fwd = (Forward) query;
    Lease lease = (Lease) fwd.getLease();
    Record record = (Record) fwd.getRecord();
    Object modQuery;
    if (record == null) {
      modQuery = lease.getUID();
    } else {
      modQuery = record;
    }
    long modTTD = lease.getTTD();

    // warn if our config.expireTTD is << the modTTD ?
    //
    // for consistency we'll accept our peer's ttd, since the
    // client will renew based upon this ttd.  If we use a shorter
    // ttd then the client will expire prematurely.  In practice
    // we expect all the ttds to be equal.

    Object answer = modify(name, modQuery, modTTD, now);

    // filter out leases (they've already been forwarded) and
    // denials (since they're likely a transient race condition)
    //
    // LeaseDenied responses are due to data conflicts.  The
    // assumption is that our local data is better and either we
    // or another peer has already sent the better data to the
    // sender.
    if (!(answer instanceof LeaseNotKnown)) {
      return null;
    }

    // send back lease-not-known responses, since our peer
    // sent us a UID and we lack the data.  The peer should
    // reply by forwarding the Record.
    return answer;
  }

  private Object resendForward(
      String name,
      Object query,
      long now) {
    // assert (Thread.holdsLock(lock));

    // find the lease and send the Record data
    //
    // this is similar to a "lookup" but we only want to find an
    // exact match, plus we need the lease ttl and not a lookup ttl
    UID queryUID = null;
    DirEntry dir = null;
    RecordEntry rec = null;
    UID uid = null;
    long ttd = -1;
    String denied =
      ((!(query instanceof LeaseNotKnown)) ?
       "query is not of type lease-not-known" :
       ((queryUID = ((LeaseNotKnown) query).getUID()) == null) ?
       "query uid is null" :
       (name.charAt(0) == '.') ?
       "name is invalid" :
       ((dir = findDir(name)) == null) ?
       "directory is null" :
       ((rec = dir.getRecordEntry(name)) == null) ?
       "no such record in our directory" :
       ((uid = rec.getUID()) == null) ?
       "local uid is null? "+rec : 
       (!uid.equals(queryUID)) ?
       "our local record has a different uid "+rec :
       ((ttd = rec.getTTL() - now) <= 0) ?
       "our local record has expired" :
       (null));
    if (denied != null) {
      // our local table doesn't contain this entry
      //
      // the non-matching UID case is assumed to be a race between
      // a forward that we've sent and someone asking about the old
      // UID.  Our forward should arrive soon enough.
      if (logger.isDebugEnabled()) {
        logger.debug(
            "Ignoring resendForward for (name="+name+
            ", query="+query+"), "+denied);
      }
      return null;
    }

    Object data = rec.getData();
    Lease lease = new Lease(uid, ttd);
    Record record = new Record(uid, -1, data);
    Forward fwd = new Forward(lease, record);

    // okay, act as if we're forwarding it for the first time,
    // but instead of sending it later we can send it back
    // to the client.
    if (logger.isDebugEnabled()) {
      logger.debug("Resending forward: "+fwd);
    }
    return fwd; 
  }

  private DirEntry findDir(String name) {
    return findOrCreateDir(name, false);
  }
  private DirEntry findOrCreateDir(String name) {
    return findOrCreateDir(name, true);
  }
  private DirEntry findOrCreateDir(String name, boolean create) {

    // extract the dir suffix, e.g.:
    //   "."     -> "."
    //   "a"     -> "."
    //   "a."    -> "."
    //   "a.b"   -> ".b"
    //   "a.b."  -> ".b"
    //   "a.b.c" -> ".b.c"
    //   ".d"    -> ".d"
    //   ".d."   -> ".d"
    //   ".d.e"  -> ".d.e"
    String suffix;
    boolean isRoot;
    int firstDot = name.indexOf('.');
    if (firstDot < 0) {
      suffix = ".";
      isRoot = true;
    } else {
      if (firstDot == 0) {
        suffix = name;
      } else {
        suffix = name.substring(firstDot);
      }
      int n = suffix.length();
      if (n == 1) {
        suffix = ".";
        isRoot = true;
      } else {
        if (suffix.charAt(n-1) == '.') {
          --n;
        }
        suffix = suffix.substring(0, n);
        isRoot = false;
      }
    }
    // assert (suffix.startsWith("."));
    // assert (suffix.equals(".") || !suffix.endsWith("."));

    if (isRoot) {
      return rootDir;
    }

    // subdir, possibly deep
    DirEntry dir = rootDir;
    int i = suffix.lastIndexOf('.');
    while (true) {
      String s = suffix.substring(i);
      Map entries = dir.getEntries();
      DirEntry subdir = (DirEntry) entries.get(s);
      if (subdir == null) {
        // no such dir
        if (!create) {
          dir = null;
          break;
        }
        UID uid = uidService.nextUID();
        subdir = new DirEntry(uid);
        entries.put(s, subdir);
      }
      // recurse down
      dir = subdir;
      if (i == 0) {
        // found dir
        break;
      }
      i = suffix.lastIndexOf('.', i-1);
      // assert (0 <= i : "invalid suffix: "+suffix);
    }

    return dir;
  }

  /**
   * Given a map of AddressEntries, extract the "version"
   * entry's moveId.
   * <p>
   * The version entry format is:<pre>
   *   version:///<i>incarnation</i>[/<i>moveId</i>]
   * </pre>
   * if the moveId is not specified then it's equivalent to the
   * incarnation number.
   *
   * @return zero if the data doesn't contain version information 
   */
  private long getVersion(Object data) {
    if (!(data instanceof Map)) {
      return 0;
    }
    Map m = (Map) data;
    Object v = m.get("version");
    if (!(v instanceof AddressEntry)) {
      return 0;
    }
    AddressEntry ae = (AddressEntry) v;
    URI uri = ae.getURI();
    if (uri == null) {
      return 0;
    }
    String path = uri.getPath();
    if (path == null || path.length() < 1) {
      return 0;
    }
    int sepIdx = path.indexOf('/', 1);
    String s;
    if (sepIdx < 0) {
      s = path.substring(1);
    } else {
      s = path.substring(sepIdx+1);
    }
    long ret;
    try {
      ret = Long.parseLong(s);
    } catch (NumberFormatException nfe) {
      return 0;
    }
    return ret;
  }

  /**
   * Scan a map of Forward objects to find the max lease ttd,
   * which we use to set the message timeout.
   */
  private long findMaxTTD(Map m) {
    long maxTTD = -1;
    for (Iterator iter = m.values().iterator();
        iter.hasNext();
        ) {
      Object o = iter.next();
      if (!(o instanceof Forward)) {
        continue;
      }
      Forward fwd = (Forward) o;
      Lease lease = fwd.getLease();
      long ttd = lease.getTTD();
      if (maxTTD < ttd) {
        maxTTD = ttd;
      }
    }
    return maxTTD;
  }

  /**
   * Batch forwards from ourself.
   * <p>
   * This is simply a performance optimization, since we can
   * batch our replications.  We can't batch for too long
   * relative to our expireTTD, otherwise we might delay a lease
   * renewal past its expiration time and our peers will remove
   * it.  About (0.75*expireTime - deliveryTime) is probably fine.
   */
  private void forwardLater(
      String name,
      Forward fwd,
      long now) {
    // assert (Thread.holdsLock(lock));

    // if the queue already contains a forward with the same uid
    // then we should keep the record data of the old forward.
    // This occurs when we've queued both a new record (with data)
    // and a lease renewal (no data) -- we want to forward the
    // latest lease TTL with the record data, otherwise we won't
    // forward the data and our peers will complain about a
    // "lease-not-known".
    Forward newFwd = fwd;
    Forward oldFwd = (Forward) forwardQueue.get(name);
    if (oldFwd != null) {
      Lease lease = fwd.getLease();
      UID uid = lease.getUID();
      Lease oldLease = oldFwd.getLease();
      UID oldUID = oldLease.getUID();
      if (uid.equals(oldUID)) {
        Record record = fwd.getRecord();
        Record oldRecord = oldFwd.getRecord();
        if (record == null && oldRecord != null) {
          newFwd = new Forward(lease, oldRecord);
        }
      }
    }

    // assert (newFwd != null);
    forwardQueue.put(name, newFwd);

    // schedule the thread if it's not scheduled?
    //
    // the schedulable API makes this tricky, so we'll simply
    // keep a steady schedule
  }

  private void forwardNow() {
    // take the queue
    Map m;
    synchronized (lock) {
      if (forwardQueue.isEmpty()) {
        m = null;
      } else {
        m = new HashMap(forwardQueue);
        m = Collections.unmodifiableMap(m);
        forwardQueue.clear();
      }
    }

    if (m != null) {
      // find the max expire time for these forwards, so we
      // can set the message timeout
      long maxTTD = findMaxTTD(m);
      forwardService.forward(m, maxTTD);
    }

    // run me again later
    forwardThread.schedule(config.forwardPeriod);
  }

  /**
   * Find expired leases and remove them. 
   *
   * @note recursive!
   */ 
  private boolean expireLeases(
      String suffix,
      DirEntry dir,
      long now) {
    // assert (Thread.holdsLock(lock));

    boolean hasChanged = false;
    Map entries = dir.getEntries();
    for (Iterator iter = entries.entrySet().iterator();
        iter.hasNext();
        ) {
      Map.Entry me = (Map.Entry) iter.next();
      String name = (String) me.getKey();
      Object value = me.getValue();
      if (value instanceof DirEntry) {
        // recurse!
        DirEntry subdir = (DirEntry) value;
        expireLeases(name, subdir, now);
        if (subdir.isEmpty()) {
          // all expired
          iter.remove();
          hasChanged = true;
          if (logger.isInfoEnabled()) {
            logger.info("Expired "+subdir.toString(now));
          }
        }
      } else if (value instanceof RecordEntry) {
        RecordEntry re = (RecordEntry) value;
        long ttl = re.getTTL();
        if (ttl < now) {
          // expired
          iter.remove();
          hasChanged = true;
          if (logger.isInfoEnabled()) {
            logger.info("Expired "+re.toString(now));
          }
        } else {
          // okay for now
        }
      } else {
        throw new RuntimeException(
            "Unexpected DirEntry element: ("+name+"="+value+")");
      }
    }

    if (hasChanged && !dir.isEmpty()) {
      // changed the dir "list" contents, so we must change
      // the dir's uid.
      dir.setUID(uidService.nextUID());
    }

    return hasChanged;
  }

  private void expireLeases() {
    synchronized (lock) {
      long now = System.currentTimeMillis();

      if (logger.isDetailEnabled()) {
        StringBuffer buf = new StringBuffer();
        buf.append(
            "##### server entries ##############################");
        rootDir.append(buf, ".", "\n  ", now);
        buf.append(
            "\n"+
            "###################################################");
        logger.detail(buf.toString());
      }

      expireLeases(".", rootDir, now);
    }

    // run me again later
    expireThread.schedule(config.checkExpirePeriod);
  }

  /** implement all the various client APIs */
  private class MyClient implements
    LookupAckService.Client, ModifyAckService.Client,
  ForwardAckService.Client, ForwardService.Client {
    public void lookup(
        MessageAddress clientAddr, long clientTime, Map m) {
      handleAll(LOOKUP, clientAddr, clientTime, m);
    }
    public void modify(
        MessageAddress clientAddr, long clientTime, Map m) {
      handleAll(MODIFY, clientAddr, clientTime, m);
    }
    public void forward(
        MessageAddress clientAddr, long clientTime, Map m) {
      handleAll(FORWARD, clientAddr, clientTime, m);
    }
    public void forwardAnswer(
        MessageAddress clientAddr, long baseTime, Map m) {
      handleAll(FORWARD_ANSWER, clientAddr, baseTime, m);
    }
  }

  /** config options, soon to be parameters/props */
  private static class RootConfig {
    public final AddressEntry[] entries;
    public final long successTTD;
    public final long failTTD;
    public final long expireTTD;
    public final long forwardPeriod = 30*1000;
    public final long checkExpirePeriod = 30*1000;
    public RootConfig(Object o) {
      entries = new AddressEntry[0];
      // FIXME parse!
      successTTD =
        Long.parseLong(
            System.getProperty(
              "org.cougaar.core.wp.server.successTTD",
              DEFAULT_SUCCESS_TTD));
      failTTD =
        Long.parseLong(
            System.getProperty(
              "org.cougaar.core.wp.server.failTTD",
              DEFAULT_FAIL_TTD));
      expireTTD =
        Long.parseLong(
            System.getProperty(
              "org.cougaar.core.wp.server.expireTTD",
              DEFAULT_EXPIRE_TTD));
    }
  }

  private static abstract class Entry {
    private UID uid;

    public Entry(UID uid) {
      this.uid = uid;
    }

    public void setUID(UID uid) {
      this.uid = uid;
    }
    public UID getUID() {
      return uid;
    }

    public String toString() {
      long now = System.currentTimeMillis();
      return toString(now);
    }

    public abstract String toString(long now);
  }

  private static class DirEntry extends Entry {

    // the child entries, which can be a mix of
    // dir-entries and record-entries.
    //
    // The string key for dir-entries always start
    // with a '.', and record-entries never start
    // with a '.'.
    //
    // <String, Entry>
    private final Map entries = new HashMap();

    public DirEntry(UID uid) {
      super(uid);
    }

    public boolean isEmpty() {
      return entries.isEmpty();
    }

    public RecordEntry getRecordEntry(String name) {
      return
        ((name.charAt(0) == '.') ?
         (null) :
         (RecordEntry) entries.get(name));
    }

    // the client can directly modify this map
    public Map getEntries() {
      return entries;
    }

    public String toString(long now) {
      StringBuffer buf = new StringBuffer();
      append(buf, "?", "\n  ", now);
      return buf.toString();
    }

    /** @note recursive! */
    public void append(
        StringBuffer buf,
        String suffix,
        String indent,
        long now) {
      // assert (Thread.holdsLock(lock));
      // assert (indent.startsWith("\n"));

      buf.append(indent).append("suffix=").append(suffix);
      buf.append(indent).append("uid=").append(getUID());

      Map m = this.getEntries();

      // sort
      Object[] keys = m.keySet().toArray();
      Arrays.sort(keys);

      buf.append(indent).append("entries[");
      buf.append(m.size()).append("]={");

      String subindent = indent+"  ";
      for (int i = 0; i < keys.length; i++) {
        String name = (String) keys[i];
        Object value = m.get(name);
        if (value instanceof DirEntry) {
          // recurse!
          DirEntry subdir = (DirEntry) value;
          subdir.append(buf, name, subindent, now);
        } else if (value instanceof RecordEntry) {
          RecordEntry re = (RecordEntry) value;
          buf.append(subindent).append(name).append("=");
          buf.append(re.toString(now));
        } else {
          throw new RuntimeException(
              "Unexpected DirEntry element: ("+name+"="+value+")");
        }
      }

      buf.append(indent).append("}");
    }
  }

  private static class RecordEntry extends Entry {

    private long ttl;
    private Object data;

    public RecordEntry(UID uid) {
      super(uid);
    }

    public void setTTL(long ttl) {
      this.ttl = ttl;
    }
    public long getTTL() {
      return ttl;
    }

    public void setData(Object data) {
      this.data = data;
    }
    public Object getData() {
      return data;
    }

    public String toString(long now) {
      return 
        "(record uid="+getUID()+
        " ttl="+Timestamp.toString(ttl, now)+
        " data="+data+
        ")";
    }
  }
}
