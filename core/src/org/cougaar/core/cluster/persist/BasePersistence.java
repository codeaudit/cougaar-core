/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.cluster.persist;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.cougaar.core.cluster.ClusterContext;
import org.cougaar.core.cluster.ClusterContextTable;
import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.core.cluster.Envelope;
import org.cougaar.core.cluster.EnvelopeTuple;
import org.cougaar.core.cluster.BulkEnvelopeTuple;
import org.cougaar.core.cluster.NoResponseException;
import org.cougaar.core.cluster.PersistenceEnvelope;
import org.cougaar.core.cluster.Subscriber;
import org.cougaar.core.cluster.MessageManager;
import org.cougaar.core.cluster.MessageManagerImpl;
import org.cougaar.domain.planning.ldm.asset.Asset;
import org.cougaar.domain.planning.ldm.plan.Allocation;
import org.cougaar.domain.planning.ldm.plan.AssetTransfer;
import org.cougaar.domain.planning.ldm.plan.Expansion;
import org.cougaar.domain.planning.ldm.plan.Plan;
import org.cougaar.domain.planning.ldm.plan.PlanElement;
import org.cougaar.domain.planning.ldm.plan.RoleScheduleImpl;
import org.cougaar.domain.planning.ldm.plan.Task;
import org.cougaar.domain.planning.ldm.plan.TaskImpl;
import org.cougaar.domain.planning.ldm.plan.Workflow;
import org.cougaar.core.cluster.persist.PersistMetadata;
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
 * values from later versions of the objects.  */
public abstract class BasePersistence implements Persistence {
  private static List clusters = new ArrayList();

  private static boolean debug = Boolean.getBoolean("org.cougaar.core.cluster.persistence.debug");

  static class SequenceNumbers {
    int first = 0;
    int current = 0;
    public SequenceNumbers() {
    }
    public SequenceNumbers(int first, int current) {
      this.first = first;
      this.current = current;
    }
    public SequenceNumbers(SequenceNumbers numbers) {
      this(numbers.first, numbers.current);
    }
    public String toString() {
      return first + ".." + current;
    }
  }

  static interface PersistenceCreator {
    public BasePersistence create() throws PersistenceException;
  }

  protected abstract SequenceNumbers readSequenceNumbers();

  protected abstract void cleanupOldDeltas(SequenceNumbers cleanupNumbers);

  protected abstract ObjectOutputStream openObjectOutputStream(int deltaNumber)
    throws IOException;

  protected abstract void abortObjectOutputStream(SequenceNumbers retainNumbers,
						  ObjectOutputStream currentOutput);

  protected abstract void closeObjectOutputStream(SequenceNumbers retainNumbers,
						  ObjectOutputStream currentOutput);

  protected abstract ObjectInputStream openObjectInputStream(int deltaNumber)
    throws IOException;

  protected abstract void closeObjectInputStream(int deltaNumber,
						 ObjectInputStream currentInput);

  protected abstract PrintWriter getHistoryWriter(int deltaNumber, String prefix) throws IOException;

  protected abstract void deleteOldPersistence();

  static {
    PersistenceInputStream.checkSuperclass();
  }

  public static Persistence find(ClusterContext context)
    throws PersistenceException
  {
    try {
      String persistenceClassName =
        System.getProperty("org.cougaar.core.cluster.persistence.class",
                           FilePersistence.class.getName());
      Class persistenceClass = Class.forName(persistenceClassName);
      Method findMethod =
        persistenceClass.getMethod("find", new Class[] {ClusterContext.class});
      return (Persistence) findMethod.invoke(null, new Object[] {context});
    }
    catch (InvocationTargetException e) {
      Throwable targetException = e.getTargetException();
      e.printStackTrace();
      if (targetException instanceof PersistenceException) {
        throw (PersistenceException) targetException;
      }
      throw new PersistenceException(targetException.toString());
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new PersistenceException(e);
    }
  }
    
  public static Persistence findOrCreate(ClusterContext clusterContext,
					 PersistenceCreator creator)
    throws PersistenceException {
    synchronized (clusters) {
      for (Iterator iter = clusters.iterator(); iter.hasNext(); ) {
	BasePersistence p = (BasePersistence) iter.next();
	if (p.clusterContext.equals(clusterContext)) return p;
      }
      BasePersistence p = creator.create();
      clusters.add(p);
      return p;
    }
  }

  /**
   * Keeps all associations of objects that have been persisted.
   */
  private IdentityTable identityTable = new IdentityTable();

  private SequenceNumbers sequenceNumbers = null;
  private SequenceNumbers cleanupSequenceNumbers = null;
  private ObjectOutputStream currentOutput;
  private ClusterContext clusterContext;
  private Plan reality = null;
  private MessageManager messageManager = null;
  private List objectsToPersist = new ArrayList();
  private PrintWriter history;
  private PrintWriter rehydrationLog;
  private boolean writeDisabled = false;

  protected BasePersistence(ClusterContext clusterContext) throws PersistenceException {
    this.clusterContext = clusterContext;
    rehydrationLogNameFormat =
      new SimpleDateFormat("'Rehydration_" +
                           clusterContext.getClusterIdentifier().getAddress() +
                           "_'yyyyMMddHHmmss'.log'");
  }

  /**
   * Rehydrate a persisted cluster. Reads all the deltas in
   * order keeping the latest (last encountered) values from
   * every object.
   * @param oldObjects Changes recorded in all but the last delta.
   * @param newObjects Changes recorded in the last delta
   * @return List of all envelopes that have not yet been distributed
   */
  public List rehydrate(PersistenceEnvelope oldObjects) {
    synchronized (identityTable) {
      identityTable.setRehydrationCollection(new ArrayList());
      if (debug) {
        try {
          rehydrationLog = new PrintWriter(new FileWriter(rehydrationLogNameFormat.format(new Date())));
        }
        catch (IOException ioe) {
          System.err.println(ioe);
        }
      }
      try {
        if (Boolean.getBoolean("org.cougaar.core.cluster.persistence.clear")) {
          deleteOldPersistence();
        }
        SequenceNumbers rehydrateNumbers = readSequenceNumbers();
        if (rehydrateNumbers != null) { // Deltas exist
          System.out.println("Rehydrating " + clusterContext.getClusterIdentifier() +
                             " " + rehydrateNumbers.toString());
          if (rehydrationLog != null) {
            printRehydrationLog("Rehydrating " + clusterContext.getClusterIdentifier() +
                                " " + rehydrateNumbers.toString());
            flushRehydrationLog();
          }
          cleanupSequenceNumbers = new SequenceNumbers(rehydrateNumbers);
          try {
            List undistributedEnvelopes;

            try {
              ClusterContextTable.enterContext(clusterContext);

              while (rehydrateNumbers.first < rehydrateNumbers.current - 1) {
                rehydrateOneDelta(rehydrateNumbers.first++, false);
              }
              undistributedEnvelopes= rehydrateOneDelta(rehydrateNumbers.first++, true);
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
            if (rehydrationLog != null) {
              printIdentityTable("");
              printRehydrationLog("OldObjects");
              logEnvelopeContents(oldObjects);
              printRehydrationLog("Undistributed Envelopes");
              for (Iterator ii = undistributedEnvelopes.iterator(); ii.hasNext(); ) {
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
              flushRehydrationLog();
            }
            history = rehydrationLog;
            if (history != null) {
              print(rehydrationSubscriberStates.size() + " subscribers");
            }

            clusterContext.getUIDServer().setPersistenceState(uidServerState);

            for (Iterator iter = identityTable.iterator(); iter.hasNext(); ) {
              PersistenceAssociation pAssoc = (PersistenceAssociation) iter.next();
              Object obj = pAssoc.getObject();
              if (obj instanceof PlanElement) {
                PlanElement pe = (PlanElement) obj;
                if (history != null) {
                  print("Rehydrated " + pAssoc);
                }
                TaskImpl task = (TaskImpl) pe.getTask();
                if (task != null) {
                  PlanElement taskPE = task.getPlanElement();
                  if (taskPE != pe) {
                    if (taskPE != null) {
                      if (history != null) {
                        print("Bogus plan element for task: " + hc(task));
                      }
                      task.privately_resetPlanElement();
                    }
                    task.privately_setPlanElement(pe); // These links can get severed during rehydration
                    if (history != null) {
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
                if (history != null) {
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
            history = null;
            return undistributedEnvelopes;
          }
          catch (Exception e) {
            e.printStackTrace();
            cleanupSequenceNumbers = null; // Leave tracks
            if (rehydrationLog != null) {
              rehydrationLog.close();
            }
            System.exit(13);
          }
        }
        return null;
      }
      finally {
        if (rehydrationLog != null) {
          rehydrationLog.close();
          rehydrationLog = null;
        }
        identityTable.setRehydrationCollection(null);
      }
    }
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

  private List rehydrateOneDelta(int deltaNumber, boolean lastDelta)
    throws IOException, ClassNotFoundException
  {
    ObjectInputStream currentInput =
      openObjectInputStream(deltaNumber);
    try {
      identityTable.setNextId(currentInput.readInt());
      PersistMetadata meta = (PersistMetadata) currentInput.readObject();
      uidServerState = meta.getUIDServerState();

      int length = currentInput.readInt();
      if (history != null) {
        print("Reading " + length + " objects");
      }
      PersistenceReference[][] referenceArrays = new PersistenceReference[length][];
      for (int i = 0; i < length; i++) {
	referenceArrays[i] = (PersistenceReference []) currentInput.readObject();
      }
      byte[] bytes = (byte[]) currentInput.readObject();
      PersistenceInputStream stream = new PersistenceInputStream(bytes);
      if (debug) {
        history = getHistoryWriter(deltaNumber, "restore_");
        writeHistoryHeader(history);
        stream.setHistoryWriter(history);
      }
      stream.setClusterContext(clusterContext);
      stream.setIdentityTable(identityTable);
      try {
	for (int i = 0; i < referenceArrays.length; i++) {
	  stream.readAssociation(referenceArrays[i]);
	}
	if (lastDelta) {
          List undistributedEnvelopes = (List) stream.readObject();
	  int nStates = stream.readInt();
	  rehydrationSubscriberStates = new ArrayList(nStates);
	  for (int i = 0; i < nStates; i++) {
	    rehydrationSubscriberStates.add(stream.readObject());
	  }
	  messageManager = (MessageManager) stream.readObject();
          return undistributedEnvelopes;
        }
      }
      finally {
	stream.close();
        history = null;
      }
      return null;
    }
    catch (IOException e) {
      System.err.println("IOException reading " + (lastDelta ? "last " : " ") + "delta " + deltaNumber);
      throw e;
    }
    finally {
      closeObjectInputStream(deltaNumber, currentInput);
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
            if (history != null) {
              print("Found " + pSubscriber);
            }
            return pSubscriber;
          }
        }
        if (history != null) {
          print("Failed to find " + new PersistenceSubscriberState(subscriber));
        }
        return null;
      }
      return null;
    }
  }

  private static Envelope[] emptyInbox = new Envelope[0];

  private void initSequenceNumbers() {
    sequenceNumbers = readSequenceNumbers();
    if (sequenceNumbers == null) {
      sequenceNumbers = new SequenceNumbers(0, 0);
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
  public void persist(List epochEnvelopes,
                      List undistributedEnvelopes,
                      List subscriberStates) {
    synchronized (getMessageManager()) {
      getMessageManager().advanceEpoch();
      if (writeDisabled) return;
      synchronized (identityTable) {
        if (sequenceNumbers == null) {
          initSequenceNumbers();
        }
        try {
          objectsToPersist.clear();
          anyMarks(identityTable.iterator());
          if (sequenceNumbers.current - sequenceNumbers.first >= 10 && sequenceNumbers.current % 10 == 0) {
            cleanupSequenceNumbers = new SequenceNumbers(sequenceNumbers);
            sequenceNumbers.first = sequenceNumbers.current;
            System.out.println("Consolidating deltas " + cleanupSequenceNumbers);
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
          beginTransaction();
          try {
            PersistenceOutputStream stream = new PersistenceOutputStream();
            if (debug) {
              history = getHistoryWriter(sequenceNumbers.current, "history_");
              writeHistoryHeader(history);
              stream.setHistoryWriter(history);
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
                  if (history != null) {
                    print("Persisting " + pAssoc);
                  }
                  referenceArrays[i] = stream.writeAssociation(pAssoc);
                }
                stream.writeObject(undistributedEnvelopes);
                if (history != null) {
                  print("Writing " + subscriberStates.size() + " subscriber states");
                }
                stream.writeInt(subscriberStates.size());
                for (Iterator iter = subscriberStates.iterator(); iter.hasNext(); ) {
                  Object obj = iter.next();
                  if (history != null) {
                    print("Writing " + obj);
                  }
                  stream.writeObject(obj);
                }
                clearMarks(objectsToPersist.iterator());
                stream.writeObject(getMessageManager());

                currentOutput.writeInt(identityTable.getNextId());
                PersistMetadata meta = new PersistMetadata();
                meta.setUIDServerState(clusterContext.getUIDServer().getPersistenceState());
                currentOutput.writeObject(meta);
                currentOutput.writeInt(referenceArrays.length);
                for (int i = 0; i < referenceArrays.length; i++) {
                  currentOutput.writeObject(referenceArrays[i]);
                }
                currentOutput.writeObject(stream.getBytes());
              }
              finally {
                stream.close();
                history = null;
              }
            }
            commitTransaction();
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
    }
  }

  /** The format of timestamps in the log **/
  private static DateFormat logTimeFormat =
    new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS");

  private DateFormat rehydrationLogNameFormat;

  private void writeHistoryHeader(PrintWriter history) {
    if (history != null) {
      history.println(logTimeFormat.format(new Date(System.currentTimeMillis())));
    }
  }

  private boolean needCleanup() {
    return cleanupSequenceNumbers != null;
  }

  private void doCleanup() {
    cleanupOldDeltas(cleanupSequenceNumbers);
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
    flushRehydrationLog();
  }

  void flushRehydrationLog() {
    if (rehydrationLog != null) {
      rehydrationLog.flush();
    }
  }

  void printRehydrationLog(String message) {
    if (rehydrationLog != null) {
      rehydrationLog.println(message);
    }
  }

  void print(String message) {
    if (history != null) {
      history.println(message);
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

  public void disableWrite() {
    writeDisabled = true;
  }

  public boolean isWriteDisabled() {
    return writeDisabled;
  }

  public MessageManager getMessageManager() {
    if (messageManager == null) {
      messageManager = new MessageManagerImpl(true);
    }
    return messageManager;
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

  private void beginTransaction() throws IOException {
    currentOutput = openObjectOutputStream(sequenceNumbers.current);
  }

  private void rollbackTransaction() {
    abortObjectOutputStream(sequenceNumbers, currentOutput);
    currentOutput = null;
  }

  private void commitTransaction() {
    sequenceNumbers.current += 1;
    closeObjectOutputStream(sequenceNumbers, currentOutput);
    currentOutput = null;
  }
}
