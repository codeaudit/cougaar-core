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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.CharArrayWriter;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.cougaar.core.agent.ClusterContext;
import org.cougaar.core.agent.ClusterContextTable;
import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.agent.NoResponseException;
import org.cougaar.core.blackboard.Envelope;
import org.cougaar.core.blackboard.EnvelopeTuple;
import org.cougaar.core.blackboard.BulkEnvelopeTuple;
import org.cougaar.core.blackboard.PersistenceEnvelope;
import org.cougaar.core.blackboard.Subscriber;
import org.cougaar.core.blackboard.MessageManager;
import org.cougaar.core.blackboard.MessageManagerImpl;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.service.LoggingService;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.plan.Allocation;
import org.cougaar.planning.ldm.plan.AssetTransfer;
import org.cougaar.planning.ldm.plan.Expansion;
import org.cougaar.planning.ldm.plan.Plan;
import org.cougaar.planning.ldm.plan.PlanElement;
import org.cougaar.planning.ldm.plan.RoleScheduleImpl;
import org.cougaar.planning.ldm.plan.Task;
import org.cougaar.planning.ldm.plan.TaskImpl;
import org.cougaar.planning.ldm.plan.Workflow;
import org.cougaar.core.persist.PersistMetadata;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * This persistence class is the base for several persistence
 * classes. It manages everything except the actual storage of
 * persistence deltas.
 *
 * As the Distributor is about to about to distribute the objects in a
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
 * @property org.cougaar.core.persistence.class Specify the persistence class to be used.
 * @property org.cougaar.core.persistence.archivingDisabled Set true to discard archival deltas
 * @property org.cougaar.core.persistence.clear Set true to discard all deltas on startup
 * @property org.cougaar.core.persistence.consolidationPeriod
 * The number of incremental deltas between full deltas (default = 10)
 */
public class BasePersistence implements Persistence, PersistencePluginSupport {
//    private static List clusters = new ArrayList();

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
  public static Persistence find(ClusterContext context, ServiceBroker sb)
    throws PersistenceException
  {
    try {
      String persistenceClassName =
        System.getProperty("org.cougaar.core.persistence.class",
                           FilePersistence.class.getName());
      Class persistenceClass = Class.forName(persistenceClassName);
      PersistencePlugin ppi = (PersistencePlugin) persistenceClass.newInstance();
      BasePersistence result = new BasePersistence(context, sb, ppi);
      ppi.init(result);
      return result;
    }
    catch (PersistenceException e) {
      throw e;
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new PersistenceException(e);
    }
  }
    
  /**
   * Keeps all associations of objects that have been persisted.
   */
  private IdentityTable identityTable = new IdentityTable();

  private int consolidationPeriod =
    Integer.getInteger("org.cougaar.core.persistence.consolidationPeriod", 10).intValue();
  private SequenceNumbers sequenceNumbers = null;
  private SequenceNumbers cleanupSequenceNumbers = null;
  protected boolean archivingEnabled =
    !Boolean.getBoolean("org.cougaar.core.persistence.archivingDisabled");
  private ObjectOutputStream currentOutput;
  private ClusterContext clusterContext;
  private Plan reality = null;
  private List objectsToPersist = new ArrayList();
  private LoggingService logger;
  private boolean writeDisabled = false;
  private String sequenceNumberSuffix = "";
  private PersistencePlugin ppi;

  protected BasePersistence(ClusterContext clusterContext, ServiceBroker sb, PersistencePlugin ppi)
    throws PersistenceException
  {
    this.clusterContext = clusterContext;
    logger = (LoggingService) sb.getService(this, LoggingService.class, null);
    System.out.println("logger = " + logger);
    this.ppi = ppi;
  }

  public ClusterIdentifier getClusterIdentifier() {
    return clusterContext.getClusterIdentifier();
  }

  public boolean archivingEnabled() {
    return archivingEnabled;
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
    PersistenceObject pObject = (PersistenceObject) state;
    synchronized (identityTable) {
      identityTable.setRehydrationCollection(new ArrayList());
      try {
        if (Boolean.getBoolean("org.cougaar.core.persistence.clear")
            || pObject != null) {
          if (logger.isInfoEnabled()) {
            print("Clearing old persistence data");
          }
          ppi.deleteOldPersistence();
        }
        SequenceNumbers rehydrateNumbers = getBestSequenceNumbers(sequenceNumberSuffix);
        if (pObject != null || rehydrateNumbers != null) { // Deltas exist
          if (pObject != null) {
            if (logger.isInfoEnabled()) {
              printRehydrationLog("Rehydrating " + clusterContext.getClusterIdentifier()
                                  + " from " + pObject);
            }
          } else {
            if (logger.isInfoEnabled()) {
              printRehydrationLog("Rehydrating "
                                  + clusterContext.getClusterIdentifier()
                                  + " "
                                  + rehydrateNumbers.toString());
            }
            if (!archivingEnabled)
              cleanupSequenceNumbers = new SequenceNumbers(rehydrateNumbers);
          }
          try {
            try {
              ClusterContextTable.enterContext(clusterContext);

              if (pObject != null) {
                result = rehydrateFromBytes(pObject.getBytes());
              } else {
                while (rehydrateNumbers.first < rehydrateNumbers.current - 1) {
                  rehydrateOneDelta(rehydrateNumbers.first++, false);
                }
                result = rehydrateOneDelta(rehydrateNumbers.first++, true);
              }
            } finally {
              ClusterContextTable.exitContext();
            }

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
              printRehydrationLog("OldObjects");
              logEnvelopeContents(oldObjects);
              printRehydrationLog("Undistributed Envelopes");
              for (Iterator ii = result.undistributedEnvelopes.iterator();
                   ii.hasNext(); ) {
                Envelope env = (Envelope) ii.next();
                logEnvelopeContents(env);
              }
              printRehydrationLog(rehydrationSubscriberStates.size() + " Subscriber States");
              for (Iterator si = rehydrationSubscriberStates.iterator(); si.hasNext(); ) {
                PersistenceSubscriberState ss = (PersistenceSubscriberState) si.next();
                if (ss.pendingEnvelopes == null) {
                  printRehydrationLog("Subscriber not persisted " + ss.getKey());
                } else {
                  printRehydrationLog("Pending envelopes of " + ss.getKey());
                  for (int i = 0, n = ss.pendingEnvelopes.size(); i < n; i++) {
                    logEnvelopeContents((Envelope) ss.pendingEnvelopes.get(i));
                  }
                  printRehydrationLog("Transaction envelopes of " + ss.getKey());
                  if (ss.transactionEnvelopes != null) {
                    for (int i = 0, n = ss.transactionEnvelopes.size(); i < n; i++) {
                      logEnvelopeContents((Envelope) ss.transactionEnvelopes.get(i));
                    }
                  } else {
                    printRehydrationLog("None");
                  }
                }
              }
              print(rehydrationSubscriberStates.size() + " subscribers");
            }

            clusterContext.getUIDServer().setPersistenceState(uidServerState);

            for (Iterator iter = identityTable.iterator(); iter.hasNext(); ) {
              PersistenceAssociation pAssoc = (PersistenceAssociation) iter.next();
              Object obj = pAssoc.getObject();
              if (obj instanceof PlanElement) {
                PlanElement pe = (PlanElement) obj;
                if (logger.isDebugEnabled()) {
                  print("Rehydrated " + pAssoc);
                }
                TaskImpl task = (TaskImpl) pe.getTask();
                if (task != null) {
                  PlanElement taskPE = task.getPlanElement();
                  if (taskPE != pe) {
                    if (taskPE != null) {
                      if (logger.isDebugEnabled()) {
                        print("Bogus plan element for task: " + hc(task));
                      }
                      task.privately_resetPlanElement();
                    }
                    task.privately_setPlanElement(pe); // These links can get severed during rehydration
                    if (logger.isDebugEnabled()) {
                      print("Fixing " + pAssoc.getActive() + ": " + hc(task) + " to " + hc(pe));
                    }
                  }
                }
                if (pe instanceof Allocation) {
                  fixAsset(((Allocation)pe).getAsset(), pe);
                } else if (pe instanceof AssetTransfer) {
                  fixAsset(((AssetTransfer)pe).getAsset(), pe);
                  fixAsset(((AssetTransfer)pe).getAssignee(), pe);
                }
                if (logger.isDebugEnabled()) {
                  if (pe instanceof Expansion) {
                    Expansion exp = (Expansion) pe;
                    Workflow wf = exp.getWorkflow();
                    for (Enumeration enum = wf.getTasks(); enum.hasMoreElements(); ) {
                      Task subtask = (Task) enum.nextElement();
                      PlanElement subtaskPE = subtask.getPlanElement();
                      if (subtaskPE == null) {
                        print("Subtask " + subtask.getUID() + " not disposed");
                      } else {
                        print("Subtask " + subtask.getUID() + " disposed " + hc(subtaskPE));
                      }
                    }
                  }
                }
              }
            }
            clusterContext.getUIDServer().setPersistenceState(uidServerState);
          }
          catch (Exception e) {
            e.printStackTrace();
            cleanupSequenceNumbers = null; // Leave tracks
            System.exit(13);
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
      printRehydrationLog(action + " " + t.getObject());
    }
  }

  public static String hc(Object o) {
    return (Integer.toHexString(System.identityHashCode(o)) +
            " " +
            (o == null ? "<null>" : o.toString()));
  }

  private void fixAsset(Asset asset, PlanElement pe) {
    // Compute role-schedules
    RoleScheduleImpl rsi = (RoleScheduleImpl) asset.getRoleSchedule();
    rsi.add(pe);
  }

  private RehydrationResult rehydrateOneDelta(int deltaNumber, boolean lastDelta)
    throws IOException, ClassNotFoundException
  {
    return rehydrateFromStream(ppi.openObjectInputStream(deltaNumber),
                               deltaNumber, lastDelta);
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
      uidServerState = meta.getUIDServerState();

      int length = currentInput.readInt();
      if (logger.isDebugEnabled()) {
        print("Reading " + length + " objects");
      }
      PersistenceReference[][] referenceArrays = new PersistenceReference[length][];
      for (int i = 0; i < length; i++) {
	referenceArrays[i] = (PersistenceReference []) currentInput.readObject();
      }
//        byte[] bytes = (byte[]) currentInput.readObject();
//        PersistenceInputStream stream = new PersistenceInputStream(bytes);
      PersistenceInputStream stream = new PersistenceInputStream(currentInput);
      if (logger.isDebugEnabled()) {
        writeHistoryHeader();
      }
      stream.setClusterContext(clusterContext);
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
      System.err.println("IOException reading " + (lastDelta ? "last " : " ") + "delta " + deltaNumber);
      throw e;
    }
    finally {
      ppi.closeObjectInputStream(deltaNumber, currentInput);
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
              print("Found " + pSubscriber);
            }
            return pSubscriber;
          }
        }
        if (logger.isDebugEnabled()) {
          print("Failed to find " + new PersistenceSubscriberState(subscriber));
        }
        return null;
      }
      return null;
    }
  }

  private static Envelope[] emptyInbox = new Envelope[0];

  private SequenceNumbers getBestSequenceNumbers(String sequenceNumberSuffix) {
    SequenceNumbers[] availableNumbers =
      ppi.readSequenceNumbers(sequenceNumberSuffix);
    SequenceNumbers best = null;
    for (int i = 0; i < availableNumbers.length; i++) {
      SequenceNumbers t = availableNumbers[i];
      if (best == null || t.timestamp > best.timestamp) {
        best = t;
      }
    }
    return best;
  }

  private void initSequenceNumbers() {
    sequenceNumbers = getBestSequenceNumbers("");
    if (sequenceNumbers == null) {
      sequenceNumbers = new SequenceNumbers();
    } else {
      sequenceNumbers.first = sequenceNumbers.current;
    }
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

  private void addObjectToPersist(Object object, boolean changeActive, boolean newActive) {
    if (object instanceof NotPersistable) return;
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
        System.out.println("Already marked: " + pAssoc);
        System.exit(13);
      }
    }
  }

  private ArrayList copyAndRemoveNotPersistable(List v) {
    ArrayList result = new ArrayList(v.size());
    for (Iterator iter = v.iterator(); iter.hasNext(); ) {
      Envelope e = (Envelope) iter.next();
      if (e instanceof NotPersistable) continue;
      result.add(e);
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
        if (sequenceNumbers.current - sequenceNumbers.first >= consolidationPeriod && sequenceNumbers.current % consolidationPeriod == 0) {
          full = true;
        }
        if (full) {
          cleanupSequenceNumbers = 			// Cleanup the existing since the full replaces them
            new SequenceNumbers(sequenceNumbers);
          System.out.println("Consolidating deltas " + cleanupSequenceNumbers);
          if (archivingEnabled) {
            cleanupSequenceNumbers.first++; // Don't clean up the base delta
            if (cleanupSequenceNumbers.first == cleanupSequenceNumbers.current)
              cleanupSequenceNumbers = null; // Nothing to cleanup
          }
          sequenceNumbers.first = sequenceNumbers.current;
        }
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
          PersistenceOutputStream stream = new PersistenceOutputStream();
          if (logger.isDebugEnabled()) {
            writeHistoryHeader();
          }
          stream.setClusterContext(clusterContext);
          stream.setIdentityTable(identityTable);
          synchronized (vmPersistLock) {
            try {
              int nObjects = objectsToPersist.size();
              PersistenceReference[][] referenceArrays = new PersistenceReference[nObjects][];
              for (int i = 0; i < nObjects; i++) {
                PersistenceAssociation pAssoc =
                  (PersistenceAssociation) objectsToPersist.get(i);
                if (logger.isDebugEnabled()) {
                  print("Persisting " + pAssoc);
                }
                referenceArrays[i] = stream.writeAssociation(pAssoc);
              }
              stream.writeObject(undistributedEnvelopes);
              if (logger.isDebugEnabled()) {
                print("Writing " + subscriberStates.size() + " subscriber states");
              }
              stream.writeInt(subscriberStates.size());
              for (Iterator iter = subscriberStates.iterator(); iter.hasNext(); ) {
                Object obj = iter.next();
                if (logger.isDebugEnabled()) {
                  print("Writing " + obj);
                }
                stream.writeObject(obj);
              }
              clearMarks(objectsToPersist.iterator());
              stream.writeObject(messageManager);
              ObjectOutputStream[] streams;
              ByteArrayOutputStream returnByteStream = null;
              if (returnBytes) {
                returnByteStream = new ByteArrayOutputStream();
                streams = new ObjectOutputStream[] {
                  new ObjectOutputStream(returnByteStream),
                  currentOutput,
                };
              } else {
                streams = new ObjectOutputStream[] {
                  currentOutput
                };
              }
              PersistMetadata meta = new PersistMetadata();
              meta.setUIDServerState(clusterContext.getUIDServer()
                                     .getPersistenceState());
              for (int j = 0; j < streams.length; j++) {
                streams[j].writeInt(identityTable.getNextId());
                streams[j].writeObject(meta);
                streams[j].writeInt(referenceArrays.length);
                for (int i = 0; i < referenceArrays.length; i++) {
                  streams[j].writeObject(referenceArrays[i]);
                }
                stream.writeBytes(streams[j]);
                streams[j].flush();
              }
              if (returnBytes) {
                result = new PersistenceObject("Persistence state "
                                               + sequenceNumbers.current,
                                               returnByteStream.toByteArray());
              }
            }
            finally {
              stream.close();
            }
          }
          commitTransaction(full);
          if (needCleanup()) {
            doCleanup();
          }
        }
        catch (Exception e) {
          e.printStackTrace();
          rollbackTransaction();
        }
        objectsThatMightGoAway.clear();
        System.err.print("P");
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
    return result;
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

  private boolean needCleanup() {
    return cleanupSequenceNumbers != null;
  }

  private void doCleanup() {
    ppi.cleanupOldDeltas(cleanupSequenceNumbers);
    cleanupSequenceNumbers = null;
  }

  void printIdentityTable(String id) {
    printRehydrationLog("IdentityTable begins");
    for (Iterator iter = identityTable.iterator(); iter.hasNext(); ) {
      PersistenceAssociation pAssoc =
        (PersistenceAssociation) iter.next();
      printRehydrationLog(id + pAssoc);
    }
    printRehydrationLog("IdentityTable ends");
  }

  void printRehydrationLog(String message) {
    if (logger.isDebugEnabled()) {
      logger.debug(message);
    }
  }

  public LoggingService getLoggingService() {
    return logger;
  }

  public void print(String message) {
    if (logger.isDebugEnabled()) {
      logger.debug(message);
    }
//      String clusterName = clusterContext.getClusterIdentifier().getAddress();
//      System.out.println(clusterName + " -- " + message);
  }

  static String getObjectName(Object o) {
    return o.getClass().getName() + "@" + System.identityHashCode(o);
  }

  public Plan getRealityPlan() {
    return reality;
  }

  public void disableWrite(String sequenceNumberSuffix) {
    this.sequenceNumberSuffix = sequenceNumberSuffix;
    writeDisabled = true;
  }

  public boolean isWriteDisabled() {
    return writeDisabled;
  }

  public void setRealityPlan(Plan plan) {
    synchronized (identityTable) {
      if (reality == null) {
	reality = plan;
	identityTable.findOrCreate(plan);
      } else if (reality != null) {
	throw new IllegalArgumentException("attempt to change reality plan");
      }
    }
  }

  ClusterContext getClusterContext() {
    return null;
  }

  private List rehydrationSubscriberStates = null;

  private Object rehydrationSubscriberStatesLock = new Object();

  private Set objectsPendingRemoval = new HashSet();

  private PersistenceState uidServerState = null;

  private void beginTransaction(boolean full) throws IOException {
    currentOutput = ppi.openObjectOutputStream(sequenceNumbers.current, full);
  }

  private void rollbackTransaction() {
    ppi.abortObjectOutputStream(sequenceNumbers, currentOutput);
    currentOutput = null;
  }

  private void commitTransaction(boolean full) {
    sequenceNumbers.current += 1;
    ppi.closeObjectOutputStream(sequenceNumbers, currentOutput, full);
    currentOutput = null;
  }

  public java.sql.Connection getDatabaseConnection(Object locker) {
    return ppi.getDatabaseConnection(locker);
  }

  public void releaseDatabaseConnection(Object locker) {
    ppi.releaseDatabaseConnection(locker);
  }
}
