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

package org.cougaar.core.persist;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import org.cougaar.core.adaptivity.OMCRangeList;
import org.cougaar.core.agent.ClusterContextTable;
import org.cougaar.core.blackboard.BulkEnvelopeTuple;
import org.cougaar.core.blackboard.Envelope;
import org.cougaar.core.blackboard.EnvelopeTuple;
import org.cougaar.core.blackboard.MessageManager;
import org.cougaar.core.blackboard.PersistenceEnvelope;
import org.cougaar.core.blackboard.Publishable;
import org.cougaar.core.blackboard.Subscriber;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.component.ServiceRevokedListener;
import org.cougaar.core.logging.LoggingServiceWithPrefix;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.DataProtectionKey;
import org.cougaar.core.service.DataProtectionKeyEnvelope;
import org.cougaar.core.service.DataProtectionService;
import org.cougaar.core.service.DataProtectionServiceClient;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.PersistenceControlService;
import org.cougaar.core.service.UIDService;
import org.cougaar.core.util.UID;
import org.cougaar.util.LinkedByteOutputStream;
import org.cougaar.util.StringUtility;

/**
 * This persistence class is the base for several persistence
 * classes. It manages everything except the actual storage of
 * persistence deltas.
 *
 * As the distributor is about to about to distribute the objects in a
 * set of envelopes, those envelopes are passed to an instance of this
 * class.  The contents of those envelopes are serialized into a
 * storage medium. These objects may refer to other plan objects that
 * have not changed.  These objects are not in any of the envelopes,
 * but they must have been stored in earlier deltas.  Instead of
 * rewriting those objects to the new delta, references to the earlier
 * objects are stored instead.
 *
 * Restoring the state from this series is a bit problematic in that a
 * given object may have been written to several deltas; only the
 * latest copy is valid and all references from other objects must be
 * made to this latest copy and all others should be ignored.  This is
 * handled by overwriting the value of the earlier objects with newer
 * values from later versions of the objects.  
 * @property org.cougaar.core.persistence.debug Enable persistence debugging.
 * @property org.cougaar.core.persistence.class Specify the
 * persistence classes to be used. The value consists of one or more
 * elements separated by commas. Each element specifies one
 * persistence plugin and consists of the name of the class of the
 * plugin, its name, and zero or more parameters all separated by
 * colons. The interpretation of the parameters depends on the plugin
 * so see the documentation of the individual plugin classes for
 * details.
 * @property org.cougaar.core.persistence.archivingDisabled Set true to discard archival deltas
 * @property org.cougaar.core.persistence.clear Set true to discard all deltas on startup
 * @property org.cougaar.core.persistence.consolidationPeriod
 * The number of incremental deltas between full deltas (default = 10)
 * @property org.cougaar.core.persistence.lazyInterval specifies the
 * interval in milliseconds between the generation of persistence
 * deltas. Default is 300000 (5 minutes). This will be overridden if
 * the persistence control and adaptivity engines are running.
 * @property org.cougaar.core.persistence.DataProtectionServiceStubEnabled set to true to enable 
 * a debugging implementation of DataProtectionService if no real one is found.
 */
public class BasePersistence
  implements Persistence, PersistencePluginSupport, ServiceProvider
{
  private static final String PERSISTENCE_INTERVAL_CONTROL_NAME = "interval";
  private static final String PERSISTENCE_ENCRYPTION_CONTROL_NAME = "encryption";
  private static final String PERSISTENCE_SIGNING_CONTROL_NAME = "signing";
  private static final String PERSISTENCE_CONSOLIDATION_PERIOD_NAME = "consolidationPeriod";
  private static final long MIN_PERSISTENCE_INTERVAL = 5000L;
  private static final long MAX_PERSISTENCE_INTERVAL = 1200000L; // 20 minutes max
  private static final String DEFAULT_PERSISTENCE_ENCRYPTION = "OFF";
  private static final String DEFAULT_PERSISTENCE_SIGNING = "OFF";
  private static final String DUMMY_MEDIA_NAME = "dummy";
  private static final String FILE_MEDIA_NAME = "file";
  private static final String PROP_LAZY_PERSIST_INTERVAL = "org.cougaar.core.persistence.lazyInterval";
  private static final long DEFAULT_LAZY_PERSIST_INTERVAL = 300000L;
  private static final long LAZY_PERSIST_INTERVAL =
    Long.getLong(PROP_LAZY_PERSIST_INTERVAL, DEFAULT_LAZY_PERSIST_INTERVAL).longValue();
  private static final int CONSOLIDATION_PERIOD =
    Integer.getInteger("org.cougaar.core.persistence.consolidationPeriod", 10).intValue();

  private static class RehydrationSet {
    PersistencePlugin ppi;
    SequenceNumbers sequenceNumbers;
    RehydrationSet(PersistencePlugin ppi, SequenceNumbers sn) {
      this.ppi = ppi;
      this.sequenceNumbers = sn;
    }
  }

  private class PersistencePluginInfo {
    PersistencePlugin ppi;
    long persistenceInterval = LAZY_PERSIST_INTERVAL;
    long nextPersistenceTime = LAZY_PERSIST_INTERVAL + System.currentTimeMillis();
    int consolidationPeriod = CONSOLIDATION_PERIOD;
    int deltaCount = 0;
    boolean encryption = false;
    boolean signing = false;
    SequenceNumbers cleanupSequenceNumbers = null;

    PersistencePluginInfo(PersistencePlugin ppi) {
      this.ppi = ppi;
    }
    long getBehind(long now) {
      return now - nextPersistenceTime;
    }

    /**
     * This is complicated because we want this plugin to be ahead or
     * behind to the same degree it was ahead or behind before the
     * change. To do this, we get how much we are currently behind and
     * multiply that by the ratio of the new and old intervals. The
     * new base time and delta count are set so that we appear to be
     * behind by the adjusted amount.
     **/
    void setInterval(long newInterval) {
      long now = System.currentTimeMillis();
      long behind = getBehind(now);
      long newBehind = behind * newInterval / persistenceInterval;
      nextPersistenceTime = now - newBehind;
      persistenceInterval = newInterval;
      if (logger.isDebugEnabled()) {
        logger.debug(ppi.getName() + " persistenceInterval = " + persistenceInterval);
        logger.debug(ppi.getName() + " nextPersistenceTime = " + persistenceInterval);
      }
    }
    void setConsolidationPeriod(int newPeriod) {
      consolidationPeriod = newPeriod;
      if (logger.isDebugEnabled()) {
        logger.debug(ppi.getName() + " consolidationPeriod = " + consolidationPeriod);
      }
    }
    void setEncryption(boolean newEncryption) {
      encryption = newEncryption;
      if (logger.isDebugEnabled()) {
        logger.debug(ppi.getName() + " encryption = " + encryption);
      }
    }
    void setSigning(boolean newSigning) {
      signing = newSigning;
      if (logger.isDebugEnabled()) {
        logger.debug(ppi.getName() + " signing = " + signing);
      }
    }
  }

  private DataProtectionServiceClient dataProtectionServiceClient =
    new DataProtectionServiceClient() {
      public Iterator iterator() {
        return getDataProtectionKeyIterator();
      }
      public MessageAddress getAgentIdentifier() {
        return BasePersistence.this.getMessageAddress();
      }
    };

  private static class PersistenceKeyEnvelope implements DataProtectionKeyEnvelope {
    PersistencePlugin ppi;
    int deltaNumber;
    DataProtectionKey theKey = null;

    public PersistenceKeyEnvelope(PersistencePlugin ppi, int deltaNumber) {
      this.deltaNumber = deltaNumber;
      this.ppi = ppi;
    }
    public void setDataProtectionKey(DataProtectionKey aKey) throws IOException {
      theKey = aKey;
      ppi.storeDataProtectionKey(deltaNumber, aKey);
    }
    public DataProtectionKey getDataProtectionKey() throws IOException {
      if (theKey == null) {
        theKey = ppi.retrieveDataProtectionKey(deltaNumber);
      }
      return theKey;
    }
  }

  private Iterator getDataProtectionKeyIterator() {
    List envelopes = new ArrayList();
    RehydrationSet[] rehydrationSets = getRehydrationSets("");
    for (int i = 0; i < rehydrationSets.length; i++) {
      SequenceNumbers sequenceNumbers = rehydrationSets[i].sequenceNumbers;
      final PersistencePlugin ppi = rehydrationSets[i].ppi;
      for (int seq = sequenceNumbers.first; seq < sequenceNumbers.current; seq++) {
        envelopes.add(new PersistenceKeyEnvelope(ppi, seq));
      }
    }
    return envelopes.iterator();
  }

  private PersistencePluginInfo getPluginInfo(String pluginName) {
    PersistencePluginInfo ppio = (PersistencePluginInfo) plugins.get(pluginName);
    if (ppio != null) return ppio;
    throw new IllegalArgumentException("No such persistence medium: "
                                       + pluginName);
  }

  private String getAgentName() {
    return getMessageAddress().toString();
  }

  /**
   * Disable all plugins (by setting their interval to max
   **/
  private void disablePlugins() {
    for (Iterator i = plugins.values().iterator(); i.hasNext(); ) {
      PersistencePluginInfo ppio = (PersistencePluginInfo) i.next();
      ppio.setInterval(MAX_PERSISTENCE_INTERVAL);
    }
  }

  public class PersistenceControlServiceImpl
    implements PersistenceControlService
  {
    public String[] getControlNames() {
      return new String[0];
    }

    public OMCRangeList getControlValues(String controlName) {
      throw new IllegalArgumentException("No such control: " + controlName);
    }

    public void setControlValue(String controlName, Comparable newValue) {
      throw new IllegalArgumentException("No such control: " + controlName);
    }

    public String[] getMediaNames() {
      return (String[]) plugins.keySet().toArray(new String[plugins.size()]);
    }

    public String[] getMediaControlNames(String mediaName) {
      PersistencePluginInfo ppio = getPluginInfo(mediaName); // Test existence
      return new String[] {
        PERSISTENCE_INTERVAL_CONTROL_NAME,
        PERSISTENCE_CONSOLIDATION_PERIOD_NAME,
        PERSISTENCE_ENCRYPTION_CONTROL_NAME,
        PERSISTENCE_SIGNING_CONTROL_NAME,
      };
    }

    public OMCRangeList getMediaControlValues(String mediaName, String controlName) {
      PersistencePluginInfo ppio = getPluginInfo(mediaName);
      if (controlName.equals(PERSISTENCE_INTERVAL_CONTROL_NAME)) {
        return new OMCRangeList(new Long(MIN_PERSISTENCE_INTERVAL),
                                new Long(MAX_PERSISTENCE_INTERVAL));
      }
      if (controlName.equals(PERSISTENCE_CONSOLIDATION_PERIOD_NAME)) {
        return new OMCRangeList(new Integer(1),
                                new Integer(20));
      }
      if (controlName.equals(PERSISTENCE_ENCRYPTION_CONTROL_NAME)) {
        return new OMCRangeList(new Comparable[] {"OFF", "ON"});
      }
      if (controlName.equals(PERSISTENCE_SIGNING_CONTROL_NAME)) {
        return new OMCRangeList(new Comparable[] {"OFF", "ON"});
      }
      throw new IllegalArgumentException(mediaName + " has no control named: " + controlName);
    }

    public void setMediaControlValue(String mediaName,
                                     String controlName,
                                     Comparable newValue)
    {
      PersistencePluginInfo ppio = getPluginInfo(mediaName);
      if (controlName.equals(PERSISTENCE_INTERVAL_CONTROL_NAME)) {
        ppio.setInterval(((Number) newValue).longValue());
        recomputeNextPersistenceTime = true;
        return;
      }
      if (controlName.equals(PERSISTENCE_CONSOLIDATION_PERIOD_NAME)) {
        ppio.setConsolidationPeriod(((Number) newValue).intValue());
        return;
      }
      if (controlName.equals(PERSISTENCE_ENCRYPTION_CONTROL_NAME)) {
        ppio.setEncryption(newValue.equals("ON"));
        return;
      }
      if (controlName.equals(PERSISTENCE_SIGNING_CONTROL_NAME)) {
        ppio.setSigning(newValue.equals("ON"));
        return;
      }
      throw new IllegalArgumentException(mediaName + " has no control named: " + controlName);
    }
  }

  interface PersistenceCreator {
    BasePersistence create() throws PersistenceException;
  }

  static {
    PersistenceInputStream.checkSuperclass();
  }

  /**
   * Find a Persistence implementation. With the advent of persistence
   * plugins instead of extensions of this class as a base class, this
   * method always returns in instance of this class embellished with
   * a PersistencePlugin. The persistence.class property now specifies
   * the class of the plugin.
   **/
  public static Persistence find(ServiceBroker sb)
    throws PersistenceException
  {
    try {
      BasePersistence result = new BasePersistence(sb);
      String defaultPlugin;
      boolean disabled = System.getProperty("org.cougaar.core.persistence.enable", "false").equals("false");
      String persistenceClasses =
        System.getProperty("org.cougaar.core.persistence.class");
      if (persistenceClasses == null) {
        if (disabled) {
          persistenceClasses = DummyPersistence.class.getName() + ":" + DUMMY_MEDIA_NAME;
        } else {
          persistenceClasses = FilePersistence.class.getName() + ":" + FILE_MEDIA_NAME;
        }
      }
      Vector pluginTokens =
        StringUtility.parseCSV(persistenceClasses, 0, persistenceClasses.length(), ',');
      for (Iterator i = pluginTokens.iterator(); i.hasNext(); ) {
        String pluginSpec = (String) i.next();
        Vector paramTokens = StringUtility.parseCSV(pluginSpec, 0, pluginSpec.length(), ':');
        if (paramTokens.size() < 1) {
          throw new PersistenceException("No plugin class specified: " + pluginSpec);
        }
        if (paramTokens.size() < 2) {
          throw new PersistenceException("No plugin name: " + pluginSpec);
        }
        Class pluginClass = Class.forName((String) paramTokens.get(0));
        String pluginName = (String) paramTokens.get(1);
        String[] pluginParams = new String[paramTokens.size() - 2];
        for (int j = 0; j < pluginParams.length; j++) {
          pluginParams[j] = (String) paramTokens.get(j + 2);
        }
        PersistencePlugin ppi = (PersistencePlugin) pluginClass.newInstance();
        ppi.init(result, pluginName, pluginParams);
        result.addPlugin(ppi);
      }
      if (disabled) {
        result.disablePlugins();
      }
      return result;
    }
    catch (PersistenceException e) {
      throw e;
    }
    catch (Exception e) {
      throw new PersistenceException(e);
    }
  }
    
  /**
   * Keeps all associations of objects that have been persisted.
   **/
  private IdentityTable identityTable = new IdentityTable();

  private SequenceNumbers sequenceNumbers = null;
  protected boolean archivingEnabled =
    !Boolean.getBoolean("org.cougaar.core.persistence.archivingDisabled");
  private ObjectOutputStream currentOutput;
  private MessageAddress agentId;
  private UIDService uidService;
  private List objectsToPersist = new ArrayList();
  private LoggingService logger;
  private DataProtectionService dataProtectionService;
  private boolean writeDisabled = false;
  private String sequenceNumberSuffix = "";
  private Map plugins = new HashMap();
  /**
   * The current PersistencePlugin being used to generate persistence
   * deltas. This is changed just prior to generating a full delta if
   * there is a different plugin having priority.
   **/
  private PersistencePluginInfo currentPersistPluginInfo;

  private long previousPersistenceTime = System.currentTimeMillis();

  private long nextPersistenceTime;

  private boolean recomputeNextPersistenceTime = true;

  private List rehydrationSubscriberStates = null;

  private Object rehydrationSubscriberStatesLock = new Object();

  private PersistenceState uidServiceState = null;

  protected BasePersistence(ServiceBroker sb)
    throws PersistenceException
  {
    AgentIdentificationService agentIdService = (AgentIdentificationService)
      sb.getService(this, AgentIdentificationService.class, null);
    agentId = (MessageAddress) agentIdService.getMessageAddress();
    uidService = (UIDService) sb.getService(this, UIDService.class, null);
    logger = (LoggingService) sb.getService(this, LoggingService.class, null);
    logger = LoggingServiceWithPrefix.add(logger, getAgentName() + ": ");
    dataProtectionService = (DataProtectionService)
      sb.getService(dataProtectionServiceClient, DataProtectionService.class, null);
    if (dataProtectionService == null) {
      if (logger.isInfoEnabled()) logger.info("No DataProtectionService Available.");
      if (Boolean.getBoolean("org.cougaar.core.persistence.DataProtectionServiceStubEnabled")) {
        dataProtectionService = new DataProtectionServiceStub();
      }
    } else {
      if (logger.isInfoEnabled()) logger.info("DataProtectionService is "
                                              + dataProtectionService.getClass().getName());
    }
  }

  public void addPlugin(PersistencePlugin ppi) {
    plugins.put(ppi.getName(), new PersistencePluginInfo(ppi));
  }

  public MessageAddress getMessageAddress() {
    return agentId;
  }

  protected UIDService getUIDService() {
    return uidService;
  }

  public boolean archivingEnabled() {
    return archivingEnabled;
  }

  /**
   * Gets the system time when persistence should be performed. We do
   * persistence periodically with a period such that all the plugins
   * will, on the average create persistence deltas with their
   * individual periods. The average frequence of persistence is the
   * sum of the individual media frequencies. Frequency is the
   * reciprocal of period. The computation is:<p>
   *
   * &nbsp;&nbsp;T = 1/(1/T1 + 1/T2 + ... + 1/Tn)
   * <p>
   * @return the time of the next persistence delta
   **/
  public long getPersistenceTime() {
    if (recomputeNextPersistenceTime) {
      double sum = 0.0;
      for (Iterator i = plugins.values().iterator(); i.hasNext(); ) {
        PersistencePluginInfo ppio = (PersistencePluginInfo) i.next();
        sum += 1.0 / ppio.persistenceInterval;
      }
      long interval = (long) (1.0 / sum);
      nextPersistenceTime = previousPersistenceTime + interval;
      recomputeNextPersistenceTime = false;
      if (logger.isDebugEnabled()) logger.debug("persistence interval=" + interval);
    }
    return nextPersistenceTime;
  }

  /**
   * Rehydrate a persisted cluster. Reads all the deltas in
   * order keeping the latest (last encountered) values from
   * every object.
   * @param oldObjects Changes recorded in all but the last delta.
   * @param newObjects Changes recorded in the last delta
   * @return List of all envelopes that have not yet been distributed
   */
  public RehydrationResult rehydrate(PersistenceEnvelope oldObjects, Object state) {
    RehydrationResult result = null;
    final PersistenceObject pObject = (PersistenceObject) state;
    synchronized (identityTable) {
      identityTable.setRehydrationCollection(new ArrayList());
      try {
        if (Boolean.getBoolean("org.cougaar.core.persistence.clear")
            || pObject != null) {
          if (logger.isInfoEnabled()) {
            logger.info("Clearing old persistence data");
          }
          for (Iterator i = plugins.values().iterator(); i.hasNext(); ) {
            PersistencePluginInfo p = (PersistencePluginInfo) i.next();
            p.ppi.deleteOldPersistence();
          }
        }
        final RehydrationSet[] rehydrationSets = getRehydrationSets(sequenceNumberSuffix);
        if (pObject != null || rehydrationSets.length > 0) { // Deltas exist
          try {
            final RehydrationResult[] resultPtr = new RehydrationResult[1];
            Runnable thunk = new Runnable() {
                public void run() {
                  try {
                    if (pObject != null) {
                      if (logger.isInfoEnabled()) {
                        logger.info("Rehydrating " + getMessageAddress()
                                    + " from " + pObject);
                      }
                      resultPtr[0] = rehydrateFromBytes(pObject.getBytes());
                    } else {
                      // Loop through the available RehydrationSets and
                      // attempt to rehydrate from each one until no errors
                      // occur. This will normally happen on the very first
                      // set, but might fail if the data has been corrupted
                      // in some way.
                      boolean success = false;
                      for (int i = 0; i < rehydrationSets.length; i++) {
                        SequenceNumbers rehydrateNumbers = rehydrationSets[i].sequenceNumbers;
                        PersistencePlugin ppi = rehydrationSets[i].ppi;
                        if (logger.isInfoEnabled()) {
                          logger.info("Rehydrating "
                                      + getMessageAddress()
                                      + " "
                                      + rehydrateNumbers.toString());
                        }
                        try {
                          while (rehydrateNumbers.first < rehydrateNumbers.current - 1) {
                            rehydrateOneDelta(ppi, rehydrateNumbers.first++, false);
                          }
                          resultPtr[0] = rehydrateOneDelta(ppi, rehydrateNumbers.first++, true);
                          success = true;
                          break;      // Successful rehydration
                        } catch (Exception e) { // Rehydration failed
                          logger.error("Rehydration from " + rehydrationSets[i] + " failed: ", e);
                          resetRehydration();
                          continue;   // Try next RehydrationSet
                        }
                      }
                      if (!success) {
                        logger.error("Rehydration failed. Starting over from scratch");
                      }
                    }
                  } catch (Exception ioe) {
                    throw new RuntimeException("withClusterContext", ioe);
                  }
                }};

            ClusterContextTable.withClusterContext(getMessageAddress(), thunk);
            result = resultPtr[0];
          
            for (Iterator iter = identityTable.iterator(); iter.hasNext(); ) {
              PersistenceAssociation pAssoc = (PersistenceAssociation) iter.next();
              if (pAssoc.isActive()) {
                Object obj = pAssoc.getObject();
                oldObjects.addObject(obj);
              }
            }
            clearMarks(identityTable.iterator());
            if (logger.isDebugEnabled()) {
              printIdentityTable("");
              logger.debug("OldObjects");
              logEnvelopeContents(oldObjects);
              if (result.undistributedEnvelopes == null) {
                logger.debug("Undistributed Envelopes is null");
              } else {
                logger.debug("Undistributed Envelopes");
                for (Iterator ii = result.undistributedEnvelopes.iterator();
                     ii.hasNext(); ) {
                  Envelope env = (Envelope) ii.next();
                  logEnvelopeContents(env);
                }
              }
              logger.debug(rehydrationSubscriberStates.size() + " Subscriber States");
              for (Iterator si = rehydrationSubscriberStates.iterator(); si.hasNext(); ) {
                PersistenceSubscriberState ss = (PersistenceSubscriberState) si.next();
                if (ss.pendingEnvelopes == null) {
                  logger.debug("Subscriber not persisted " + ss.getKey());
                } else {
                  logger.debug("Pending envelopes of " + ss.getKey());
                  for (int i = 0, n = ss.pendingEnvelopes.size(); i < n; i++) {
                    logEnvelopeContents((Envelope) ss.pendingEnvelopes.get(i));
                  }
                  logger.debug("Transaction envelopes of " + ss.getKey());
                  if (ss.transactionEnvelopes != null) {
                    for (int i = 0, n = ss.transactionEnvelopes.size(); i < n; i++) {
                      logEnvelopeContents((Envelope) ss.transactionEnvelopes.get(i));
                    }
                  } else {
                    logger.debug("None");
                  }
                }
              }
              logger.debug(rehydrationSubscriberStates.size() + " subscribers");
            }

            if (uidServiceState != null) {
              // For some reason this is sometimes null. Avoid NPE.
              // Restoring uidServiceState is not essential and could
              // be eliminated entirely.
              getUIDService().setPersistenceState(uidServiceState);
            }

            for (Iterator iter = identityTable.iterator(); iter.hasNext(); ) {
              PersistenceAssociation pAssoc = (PersistenceAssociation) iter.next();
              Object obj = pAssoc.getObject();
              if (obj instanceof ActivePersistenceObject) {
                ((ActivePersistenceObject) obj).postRehydration(logger);
              }
            }
            getUIDService().setPersistenceState(uidServiceState);
          } catch (Exception e) {
            logger.error("Error during rehydration", e);
            result = null;
          }

        }
      }
      finally {
        identityTable.setRehydrationCollection(null);
      }
    }
    if (result == null) return new RehydrationResult();
    return result;
  }

  /**
   * Loop through all plugins and get their available sequence number
   * sets. Create RehydrationSets from the sequence numbers sets and
   * sort them by timestamp.
   * @return the sorted RehydrationSets
   **/
  private RehydrationSet[] getRehydrationSets(String suffix) {
    List result = new ArrayList();
    for (Iterator i = plugins.values().iterator(); i.hasNext(); ) {
      PersistencePluginInfo ppio = (PersistencePluginInfo) i.next();
      SequenceNumbers[] pluginNumbers = ppio.ppi.readSequenceNumbers(suffix);
      for (int j = 0; j < pluginNumbers.length; j++) {
        result.add(new RehydrationSet(ppio.ppi, pluginNumbers[j]));
      }
    }
    Collections.sort(result, new Comparator() {
        public int compare(Object o1, Object o2) {
          RehydrationSet rs1 = (RehydrationSet) o1;
          RehydrationSet rs2 = (RehydrationSet) o2;
          int diff = rs1.sequenceNumbers.compareTo(rs1.sequenceNumbers);
          if (diff != 0) return -diff;
          return rs1.ppi.getName().compareTo(rs2.ppi.getName());
        }
      });
    return (RehydrationSet[]) result.toArray(new RehydrationSet[result.size()]);
  }

  private void logEnvelopeContents(Envelope env) {
    for (Iterator cc = env.getAllTuples(); cc.hasNext(); ) {
      EnvelopeTuple t = (EnvelopeTuple) cc.next();
      String action = "";
      switch(t.getAction()) {
      case Envelope.ADD: action = "ADD"; break;
      case Envelope.REMOVE: action = "REMOVE"; break;
      case Envelope.CHANGE: action = "CHANGE"; break;
      case Envelope.BULK: action = "BULK"; break;
      }
      logger.debug(action + " " + t.getObject());
    }
  }

  public static String hc(Object o) {
    return (Integer.toHexString(System.identityHashCode(o)) +
            " " +
            (o == null ? "<null>" : o.toString()));
  }

  /**
   * Erase all the effects of a failed rehydration attempt. Three
   * variables are set or altered during a rehydration attempt. The
   * identityTable may be partially filled in, so we replace it with a
   * fresh one. The uidService has been set and would be overwritten by
   * the subsequent attempt, but we nullify it for luck. Finally, the
   * rehydrationSubscriberStates is also nullified
   **/
  private void resetRehydration() {
    identityTable = new IdentityTable();
    uidServiceState = null;
    rehydrationSubscriberStates = null;
  }

  private RehydrationResult rehydrateOneDelta(PersistencePlugin ppi, int deltaNumber, boolean lastDelta)
    throws IOException, ClassNotFoundException
  {
    InputStream is = ppi.openInputStream(deltaNumber);
    if (dataProtectionService != null) {
      PersistenceKeyEnvelope keyEnvelope = new PersistenceKeyEnvelope(ppi, deltaNumber);
      is = dataProtectionService.getInputStream(keyEnvelope, is);
    }
    ObjectInputStream ois = new ObjectInputStream(is);
    try {
      return rehydrateFromStream(ois, deltaNumber, lastDelta);
    } finally {
      ois.close();
      ppi.finishInputStream(deltaNumber);
    }
  }

  private RehydrationResult rehydrateFromBytes(byte[] bytes)
    throws IOException, ClassNotFoundException
  {
    ByteArrayInputStream bs = new ByteArrayInputStream(bytes);
    return rehydrateFromStream(new ObjectInputStream(bs), 0, true);
  }

  private RehydrationResult rehydrateFromStream(ObjectInputStream currentInput,
                                                int deltaNumber, boolean lastDelta)
    throws IOException, ClassNotFoundException
  {
    RehydrationResult result = new RehydrationResult();
    try {
      identityTable.setNextId(currentInput.readInt());
      PersistMetadata meta = (PersistMetadata) currentInput.readObject();
      uidServiceState = meta.getUIDServerState();

      int length = currentInput.readInt();
      if (logger.isDebugEnabled()) {
        logger.debug("Reading " + length + " objects");
      }
      PersistenceReference[][] referenceArrays = new PersistenceReference[length][];
      for (int i = 0; i < length; i++) {
	referenceArrays[i] = (PersistenceReference []) currentInput.readObject();
      }
//        byte[] bytes = (byte[]) currentInput.readObject();
//        PersistenceInputStream stream = new PersistenceInputStream(bytes);
      PersistenceInputStream stream = new PersistenceInputStream(currentInput, logger);
      if (logger.isDebugEnabled()) {
        writeHistoryHeader();
      }
      stream.setIdentityTable(identityTable);
      try {
	for (int i = 0; i < referenceArrays.length; i++) {
	  stream.readAssociation(referenceArrays[i]);
	}
	if (lastDelta) {
          result.undistributedEnvelopes = (List) stream.readObject();
	  int nStates = stream.readInt();
	  rehydrationSubscriberStates = new ArrayList(nStates);
	  for (int i = 0; i < nStates; i++) {
	    rehydrationSubscriberStates.add(stream.readObject());
	  }
	  result.messageManager = (MessageManager) stream.readObject();
          return result;
        }
      }
      finally {
	stream.close();
      }
      return result;
    }
    catch (IOException e) {
      logger.error("IOException reading " + (lastDelta ? "last " : " ") + "delta " + deltaNumber);
      throw e;
    }
  }

  public boolean hasSubscriberStates() {
    synchronized (rehydrationSubscriberStatesLock) {
      return rehydrationSubscriberStates != null;
    }
  }

  public void discardSubscriberState(Subscriber subscriber) {
    synchronized (rehydrationSubscriberStatesLock) {
      if (rehydrationSubscriberStates != null) {
        for (Iterator subscribers = rehydrationSubscriberStates.iterator();
             subscribers.hasNext(); )
          {
            PersistenceSubscriberState pSubscriber =
              (PersistenceSubscriberState) subscribers.next();
            if (pSubscriber.isSameSubscriberAs(subscriber)) {
              subscribers.remove();
              if (rehydrationSubscriberStates.size() == 0) {
                rehydrationSubscriberStates = null;
              }
              return;
            }
          }
      }
    }
  }

  public PersistenceSubscriberState getSubscriberState(Subscriber subscriber) {
    synchronized (rehydrationSubscriberStatesLock) {
      if (rehydrationSubscriberStates != null) {
        for (Iterator subscribers = rehydrationSubscriberStates.iterator(); subscribers.hasNext(); ) {
          PersistenceSubscriberState pSubscriber = (PersistenceSubscriberState) subscribers.next();
          if (pSubscriber.isSameSubscriberAs(subscriber)) {
            if (logger.isDebugEnabled()) {
              logger.debug("Found " + pSubscriber);
            }
            return pSubscriber;
          }
        }
        if (subscriber.shouldBePersisted() && logger.isInfoEnabled()) {
          logger.info("Failed to find " + new PersistenceSubscriberState(subscriber));
        }
        return null;
      }
      return null;
    }
  }

  private static Envelope[] emptyInbox = new Envelope[0];

  /**
   * Get highest sequence numbers recorded on any medium
   **/
  private int getHighestSequenceNumber() {
    int best = 0;
    for (Iterator i = plugins.values().iterator(); i.hasNext(); ) {
      PersistencePluginInfo ppio = (PersistencePluginInfo) i.next();
      SequenceNumbers[] availableNumbers = ppio.ppi.readSequenceNumbers("");
      for (int j = 0; j < availableNumbers.length; j++) {
        SequenceNumbers t = availableNumbers[j];
        best = Math.max(best, t.current);
      }
    }
    return best;
  }

  private void initSequenceNumbers() {
    int highest = getHighestSequenceNumber();
    sequenceNumbers = new SequenceNumbers(highest, highest, System.currentTimeMillis());
  }

  /**
   * Persist a List of Envelopes. First the objects in the envelope
   * are entered into the persistence identityTable. Then the objects
   * are serialized to an ObjectOutputStream preceded with their
   * reference id.  Other references to objects in the identityTable
   * are replaced with reference objects.
   */
  private boolean nonEmpty(List subscriberStates) {
    for (Iterator iter = subscriberStates.iterator(); iter.hasNext(); ) {
      PersistenceSubscriberState subscriberState = (PersistenceSubscriberState) iter.next();
      if (subscriberState.pendingEnvelopes.size() > 0) return true;
      if (subscriberState.transactionEnvelopes.size() > 0) return true;
    }
    return false;
  }

  private boolean isPersistable(Object o) {
    if (o instanceof NotPersistable) return false;
    if (o instanceof Publishable) {
      Publishable pbl = (Publishable) o;
      return pbl.isPersistable();
    }
    return true;
  }

  private void addObjectToPersist(Object object, boolean changeActive, boolean newActive) {
    if (!isPersistable(object)) return;
    PersistenceAssociation pAssoc = identityTable.findOrCreate(object);
    if (changeActive) {
      if (newActive) {
        pAssoc.setActive();
      } else {
        pAssoc.setInactive();
      }
    }
    addAssociationToPersist(pAssoc);
  }

  private void addAssociationToPersist(PersistenceAssociation pAssoc) {
    if (pAssoc.isMarked()) return; // Already scheduled to be written
    pAssoc.setMarked(true);
    objectsToPersist.add(pAssoc);
  }

  private void clearMarks(Iterator iter) {
    while (iter.hasNext()) {
      PersistenceAssociation pAssoc = (PersistenceAssociation) iter.next();
      pAssoc.setMarked(false);
    }
  }

  private void anyMarks(Iterator iter) {
    while (iter.hasNext()) {
      PersistenceAssociation pAssoc = (PersistenceAssociation) iter.next();
      if (pAssoc.isMarked()) {
        if (logger.isWarnEnabled()) logger.warn("Already marked: " + pAssoc);
        pAssoc.setMarked(false);
      }
    }
  }

  private ArrayList copyAndRemoveNotPersistable(List v) {
    if (v == null) return null;
    ArrayList result = new ArrayList(v.size());
    for (Iterator iter = v.iterator(); iter.hasNext(); ) {
      Envelope e = (Envelope) iter.next();
      Envelope copy = null;
      for (Iterator tuples = e.getAllTuples(); tuples.hasNext(); ) {
        EnvelopeTuple tuple = (EnvelopeTuple) tuples.next();
        Object o = tuple.getObject();
        if (isPersistable(o)) {
          if (copy == null) copy = new Envelope();
          copy.addTuple(tuple);
        } else {
          if (logger.isDebugEnabled()) {
            logger.debug("Removing not persistable " + o);
          }
        }
      }
      if (copy != null) result.add(copy);
    }
    return result;
  }

  private void addEnvelopes(List envelopes, boolean old) {
    for (Iterator iter = envelopes.iterator(); iter.hasNext(); ) {
      Envelope e = (Envelope) iter.next();
      Iterator envelope = e.getAllTuples();
      while (envelope.hasNext()) {
        EnvelopeTuple tuple = (EnvelopeTuple) envelope.next();
        switch (tuple.getAction()) {
        case Envelope.BULK:
          Collection collection = ((BulkEnvelopeTuple)tuple).getCollection();
          for (Iterator iter2 = collection.iterator(); iter2.hasNext(); ) {
            addObjectToPersist(iter2.next(), old, true);
          }
          break;
        case Envelope.ADD:
        case Envelope.CHANGE:
          addObjectToPersist(tuple.getObject(), old, true);
          break;
        case Envelope.REMOVE:
          addObjectToPersist(tuple.getObject(), old, false);
          break;
        }
      }
    }
  }

  /**
   * Select the next plugin to use for persistence. This is only
   * possible when the delta that is about to be generated will have
   * the full state. For each plugin, we keep track of the
   * nextPersistenceTime based on its persistenceInterval. We select
   * the plugin with the earliest nextPersistenceTime. If the
   * nextPersistenceTime of the selected plugin differs significantly
   * from now, the nextPersistenceTimes of all plugins are adjusted to
   * eliminate that difference.
   **/
  private void selectNextPlugin() {
    PersistencePluginInfo best = null;
    for (Iterator i = plugins.values().iterator(); i.hasNext(); ) {
      PersistencePluginInfo ppio = (PersistencePluginInfo) i.next();
      if (best == null || ppio.nextPersistenceTime < best.nextPersistenceTime) {
        best = ppio;
      }
    }
    long adjustment = System.currentTimeMillis() - best.nextPersistenceTime;
    if (Math.abs(adjustment) > 10000L) {
      for (Iterator i = plugins.values().iterator(); i.hasNext(); ) {
        PersistencePluginInfo ppio = (PersistencePluginInfo) i.next();
        ppio.nextPersistenceTime += adjustment;
      }
    }
    currentPersistPluginInfo = best;
  }

  /**
   * Because PersistenceAssociations have WeakReference objects, the
   * actual object in the association may be garbage collected if
   * there are no other references to it. This list is used to hold a
   * reference to such objects to avoid an NPE when it is not known
   * that a reference exists. Only used during the first delta after
   * rehydration.
   **/
  private ArrayList objectsThatMightGoAway = new ArrayList();

  private final static Object vmPersistLock = new Object();

  /**
   * End a persistence epoch by generating a persistence delta.
   * @param epochEnvelopes All envelopes from this epoch that have
   * been distributed. The effect of these envelopes has already been
   * captured in the subscriber inboxes or in the consequential
   * outboxes which are in undistributedEnvelopes.
   * @param undistributedEnvelopes Envelopes that have not yet been distributed
   * @param subscriberStates The subscriber states to record
   **/
  public Object persist(List epochEnvelopes,
                        List undistributedEnvelopes,
                        List subscriberStates,
                        boolean returnBytes,
                        boolean full,
                        MessageManager messageManager)
  {
    long startCPU = 0L;
    long startTime = 0L;
    if (logger.isInfoEnabled()) {
      //startCPU = CpuClock.cpuTimeMillis();
      startCPU = 0l;
      startTime = System.currentTimeMillis();
      logger.info("Persist started");
    }
    int bytesSerialized = 0;
    recomputeNextPersistenceTime = true;
    if (writeDisabled) return null;
    PersistenceObject result = null; // Return value if wanted
    synchronized (identityTable) {
      if (sequenceNumbers == null) {
        initSequenceNumbers();
      }
      try {
        objectsToPersist.clear();
        anyMarks(identityTable.iterator());
        full |= returnBytes;
        // The following fixes an edge condition. The very first delta
        // (seqno = 0) is always a full delta whether we want it to be
        // or not because there are no previous ones. Setting full =
        // true removes any ambiguity about its fullness.
        if (sequenceNumbers.current == 0) full = true;
        // Every so often generate a full delta to consolidate and
        // prevent the number of deltas from increasing without bound.
        if (!full &&
            currentPersistPluginInfo!= null &&
            currentPersistPluginInfo.deltaCount >= currentPersistPluginInfo.consolidationPeriod &&
            currentPersistPluginInfo.deltaCount % currentPersistPluginInfo.consolidationPeriod == 0)
          {
            full = true;
          }
        if (full || currentPersistPluginInfo == null) {
          if (currentPersistPluginInfo != null) {
            currentPersistPluginInfo.cleanupSequenceNumbers = // Cleanup the existing since the full replaces them
              new SequenceNumbers(sequenceNumbers);
            logger.info("Consolidating deltas " + currentPersistPluginInfo.cleanupSequenceNumbers);
            if (archivingEnabled) {
              currentPersistPluginInfo.cleanupSequenceNumbers.first++; // Don't clean up the base delta
              if (currentPersistPluginInfo.cleanupSequenceNumbers.first == currentPersistPluginInfo.cleanupSequenceNumbers.current)
                currentPersistPluginInfo.cleanupSequenceNumbers = null; // Nothing to cleanup
            }
          }
          sequenceNumbers.first = sequenceNumbers.current;
          selectNextPlugin();
        }
        currentPersistPluginInfo.deltaCount++;
        if (sequenceNumbers.current == sequenceNumbers.first) {
          // First delta of this running
          for (Iterator iter = identityTable.iterator(); iter.hasNext(); ) {
            PersistenceAssociation pAssoc = (PersistenceAssociation) iter.next();
            if (!pAssoc.isMarked()) {
              Object object = pAssoc.getObject();
              if (object != null) {
                objectsThatMightGoAway.add(object);
                addAssociationToPersist(pAssoc);
              }
            }
          }
        }
        epochEnvelopes = copyAndRemoveNotPersistable(epochEnvelopes);
        undistributedEnvelopes = copyAndRemoveNotPersistable(undistributedEnvelopes);
        addEnvelopes(epochEnvelopes, true);
        beginTransaction(full);
        try {
          if (currentOutput == null && !returnBytes) {
            // Only doing dummy persistence
          } else {
            PersistenceOutputStream stream = new PersistenceOutputStream(logger);
            PersistenceReference[][] referenceArrays;
            PersistMetadata meta = new PersistMetadata();
            if (logger.isDebugEnabled()) {
              writeHistoryHeader();
            }
            stream.setIdentityTable(identityTable);
            // One agent at a time to avoid inter-agent deadlock due to shared objects
            try {
              if (logger.isInfoEnabled()) {
                logger.info("Obtaining JVM persist lock");
              }
              synchronized (vmPersistLock) {
                if (logger.isInfoEnabled()) {
                  logger.info("Obtained JVM persist lock, serializing");
                }
                int nObjects = objectsToPersist.size();
                referenceArrays = new PersistenceReference[nObjects][];
                for (int i = 0; i < nObjects; i++) {
                  PersistenceAssociation pAssoc =
                    (PersistenceAssociation) objectsToPersist.get(i);
                  if (logger.isDebugEnabled()) {
                    logger.debug("Persisting " + pAssoc);
                  }
                  referenceArrays[i] = stream.writeAssociation(pAssoc);
                }
                stream.writeObject(undistributedEnvelopes);
                if (logger.isDebugEnabled()) {
                  logger.debug("Writing " + subscriberStates.size() + " subscriber states");
                }
                stream.writeInt(subscriberStates.size());
                for (Iterator iter = subscriberStates.iterator(); iter.hasNext(); ) {
                  PersistenceSubscriberState ss = (PersistenceSubscriberState) iter.next();
                  ss.pendingEnvelopes = copyAndRemoveNotPersistable(ss.pendingEnvelopes);
                  ss.transactionEnvelopes = copyAndRemoveNotPersistable(ss.transactionEnvelopes);
                  if (logger.isDebugEnabled()) {
                    logger.debug("Writing " + ss);
                  }
                  stream.writeObject(ss);
                }
                stream.writeObject(messageManager);
                bytesSerialized = stream.size();
                meta.setUIDServerState(getUIDService().getPersistenceState());
                if (logger.isInfoEnabled()) {
                  logger.info(
                      "Serialized "+bytesSerialized+
                      " bytes to buffer, releasing lock");
                }
              } // Ok to let other agents persist while we write out our data
            } finally {
              stream.close();
            } // End of stream protection try-catch
            if (returnBytes) {
              int estimatedSize = (int)(1.2 * bytesSerialized);
              LinkedByteOutputStream returnByteStream = new LinkedByteOutputStream(estimatedSize);
              ObjectOutputStream returnOutput = new ObjectOutputStream(returnByteStream);
              writeFinalOutput(returnOutput, meta, referenceArrays, stream);
              returnOutput.close();
              result = new PersistenceObject("Persistence state "
                                             + sequenceNumbers.current,
                                             returnByteStream.toByteArray());
              if (logger.isInfoEnabled()) {
                logger.info(
                    "Copied persistence snapshot to memory buffer"+
                    " for return to state-capture caller");
              }
            }
            if (currentOutput != null) {
              writeFinalOutput(currentOutput, meta, referenceArrays, stream);
              currentOutput.close();
              if (logger.isInfoEnabled()) {
                logger.info(
                    "Wrote persistence snapshot to output stream");
              }
            }
          } // End of non-dummy persistence
          clearMarks(objectsToPersist.iterator());
          commitTransaction(full);
          System.err.print("P");
          if (currentPersistPluginInfo.cleanupSequenceNumbers != null) {
            currentPersistPluginInfo.ppi.cleanupOldDeltas(currentPersistPluginInfo.cleanupSequenceNumbers);
            currentPersistPluginInfo.cleanupSequenceNumbers = null;
          }
        } catch (Exception e) { // Transaction protection
          rollbackTransaction();
          if (logger.isErrorEnabled()) {
            logger.error("Persist failed", e);
          }
          System.err.print("X");
          throw e;
        }
        objectsThatMightGoAway.clear();
      }
      catch (Exception e) {
        logger.error("Error writing persistence snapshot", e);
      }
      // set persist time to persist completion + epsilon
      previousPersistenceTime = System.currentTimeMillis();
      currentPersistPluginInfo.nextPersistenceTime =
        previousPersistenceTime +
        currentPersistPluginInfo.persistenceInterval;
    }
    if (logger.isInfoEnabled()) {
      //long finishCPU = CpuClock.cpuTimeMillis();
      long finishCPU = 0l;
      long finishTime = System.currentTimeMillis();
      logger.info(
          "Persisted "+
          bytesSerialized+" bytes in "+
          (finishTime-startTime)+" MS");
      // +", cpu="+(finishCPU - startCPU)
    }
    return result;
  }

  private void writeFinalOutput(ObjectOutputStream s,
                                PersistMetadata meta,
                                PersistenceReference[][] referenceArrays,
                                PersistenceOutputStream stream)
    throws IOException
  {
    s.writeInt(identityTable.getNextId());
    s.writeObject(meta);
    s.writeInt(referenceArrays.length);
    for (int i = 0; i < referenceArrays.length; i++) {
      s.writeObject(referenceArrays[i]);
    }
    stream.writeBytes(s);
    s.flush();
  }

  /** The format of timestamps in the log **/
  private static DateFormat logTimeFormat =
    new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");

  private static DecimalFormat deltaFormat = new DecimalFormat("_00000");

  public static String formatDeltaNumber(int deltaNumber) {
    return deltaFormat.format(deltaNumber);
  }

  private void writeHistoryHeader() {
    if (logger.isDebugEnabled()) {
      logger.debug(logTimeFormat.format(new Date(System.currentTimeMillis())));
    }
  }

  void printIdentityTable(String id) {
    logger.debug("IdentityTable begins");
    for (Iterator iter = identityTable.iterator(); iter.hasNext(); ) {
      PersistenceAssociation pAssoc =
        (PersistenceAssociation) iter.next();
      logger.debug(id + pAssoc);
    }
    logger.debug("IdentityTable ends");
  }

  public LoggingService getLoggingService() {
    return logger;
  }

  static String getObjectName(Object o) {
    return o.getClass().getName() + "@" + System.identityHashCode(o);
  }

  public void disableWrite(String sequenceNumberSuffix) {
    this.sequenceNumberSuffix = sequenceNumberSuffix;
    writeDisabled = true;
  }

  public boolean isWriteDisabled() {
    return writeDisabled;
  }

  private void beginTransaction(boolean full) throws IOException {
    int deltaNumber = sequenceNumbers.current;
    OutputStream os = currentPersistPluginInfo.ppi.openOutputStream(deltaNumber, full);
    if (os != null) {
      if (dataProtectionService != null) {
        PersistenceKeyEnvelope keyEnvelope =
          new PersistenceKeyEnvelope(currentPersistPluginInfo.ppi, deltaNumber);
        os = dataProtectionService.getOutputStream(keyEnvelope, os);
      }
      currentOutput = new ObjectOutputStream(os);
    } else {
      currentOutput = null;
    }
  }

  private void rollbackTransaction() {
    currentPersistPluginInfo.ppi.abortOutputStream(sequenceNumbers);
    currentOutput = null;
  }

  private void commitTransaction(boolean full) {
    sequenceNumbers.current += 1;
    currentPersistPluginInfo.ppi.finishOutputStream(sequenceNumbers, full);
    currentOutput = null;
  }

  public java.sql.Connection getDatabaseConnection(Object locker) {
    return currentPersistPluginInfo.ppi.getDatabaseConnection(locker);
  }

  public void releaseDatabaseConnection(Object locker) {
    currentPersistPluginInfo.ppi.releaseDatabaseConnection(locker);
  }

  // ServiceProvider implementation
  public Object getService(ServiceBroker sb, Object requestor, Class cls) {
    if (cls != PersistenceControlService.class) {
      throw new IllegalArgumentException("Unknown service class");
    }
    return new PersistenceControlServiceImpl();
  }
  public void releaseService(ServiceBroker sb, Object requestor, Class cls, Object svc) {
    if (cls != PersistenceControlService.class) {
      throw new IllegalArgumentException("Unknown service class");
    }
  }

  // More Persistence implementation
  public ServiceProvider getServiceProvider() {
    return this;
  }

  public String toString() {
    return "Persist(" + getAgentName() + ")";
  }
}
