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

package org.cougaar.planning.ldm.plan;

import org.cougaar.core.blackboard.ActiveSubscriptionObject;

import org.cougaar.core.blackboard.Subscriber;

import org.cougaar.core.blackboard.PublishableAdapter;

import org.w3c.dom.Element;
import org.w3c.dom.Document;

import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.core.blackboard.LogPlanServesLogicProvider;

import org.cougaar.core.blackboard.Transaction;
import org.cougaar.core.agent.*;
import org.cougaar.core.util.XMLizable;

import org.cougaar.core.util.*;
import org.cougaar.util.*;
import org.cougaar.util.log.*;
import java.util.*;

import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import org.cougaar.core.util.UID;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.core.plugin.Annotation;
 
/** Implementation of Task.  Instances of Tasks
 * are created by the Expander Plug-ins.  All Tasks
 * are attached to a workflow.
 */

public class TaskImpl extends DirectiveImpl
  implements Task, NewTask, Cloneable, XMLizable, ActiveSubscriptionObject, java.io.Serializable
{
  private static Logger logger = Logging.getLogger(TaskImpl.class);

  private Verb verb;
  private transient Asset directObject;  // changed to transient : Persistence
  private transient ArrayList phrases = null; // changed to transient : Persistence
  private transient Workflow workflow; // changed to transient : Persistence 
  private transient ArrayList preferences = null;
  private byte priority = Priority.UNDEFINED;
  private UID parentUID;
  private UID uid = null;
  // plan elements don't cross cluster boundaries
  private transient PlanElement myPE;
  // initialize to null unitl we fully implement
  private long commitmenttime = 0;
  //private Date commitmentdate = null;
  private boolean deleted = false;// Set to true when deletion occurs
  private transient Set observableAspects;
  // initialize with one slot = -1 in case its never filled in.
  private int[] auxqtypes = {-1};

  public ClusterIdentifier getOwner() { return source; }
  public UID getUID() { return uid; }
  public void setUID(UID uid) {
    if (this.uid != null) throw new IllegalArgumentException("UID already set");
    this.uid = uid;
  }

  /** Constructor that takes no args */
  public TaskImpl(UID uid) {
    this.uid = uid;
  }
  
  /** empty constructor used by clone and externalizable */
  public TaskImpl() {
  }

  
  /** @return Verb verb or action of task (move from move 152 tanks)*/
  public Verb getVerb() {
    return verb;
  }
  /** @param aVerb set the verb or action of a task*/
  public void setVerb( Verb aVerb ) {
    verb = aVerb;
    decacheTS();
  }
  
  /** @return Asset - directObject of the task */
  public Asset getDirectObject() {
    return directObject;
  }
  
  /** @param dobj - set the directObject*/
  public void setDirectObject(Asset dobj) {
    directObject = dobj;
    decacheTS();
  }
        
  /** @return Enum{PrepositionalPhrase} - The prepositional phrase(s) of the task */
  public Enumeration getPrepositionalPhrases() {
    if (phrases == null || phrases.size()==0)
      return Empty.enumeration;
    else
      return new Enumerator(phrases);
  }
  
  public PrepositionalPhrase getPrepositionalPhrase(String preposition) {
    if (phrases == null || preposition == null)
      return null;

    //preposition = preposition.intern(); // so we can use == below

    int l = phrases.size();
    for (int i = 0; i<l; i++) {
      PrepositionalPhrase pp = (PrepositionalPhrase) phrases.get(i);
      String op = pp.getPreposition();
      //if (preposition==op) return pp;
      if (preposition.equals(op)) return pp;
    }
    return null;
  }

  /**
   * Note that any previous values will be dropped. 
   * @param enumOfPrepPhrase - set the prepositional phrases
   */
  public void setPrepositionalPhrases(Enumeration enumOfPrepPhrase) {
    if (phrases == null) {
      if (enumOfPrepPhrase.hasMoreElements()) // don't make one if there aren't elements
        phrases = new ArrayList(2);
    } else {
      phrases.clear();
    }

    if (enumOfPrepPhrase == null) {
      throw new IllegalArgumentException("Task.setPrepositionalPhrases(Enum e): e must be an Enumeration");
    }

    while (enumOfPrepPhrase.hasMoreElements()) {
      PrepositionalPhrase pp = (PrepositionalPhrase) enumOfPrepPhrase.nextElement();
      if (pp instanceof PrepositionalPhrase) {
        phrases.add(pp);
      } else {
        //buzzzzzzz... wrong answer - tryed to pass in a null!
        throw new IllegalArgumentException("Task.setPrepositionalPhrases(Enum e): all elements of e must be PrepositionalPhrases");
      }
    }

    Transaction.noteChangeReport(this,new Task.PrepositionChangeReport());

    decacheTS();
  
  }
  
  /**
   * Set the prepositional phrase (note singularity)
   * Note that any previous values will be dropped 
   * @param aPrepPhrase 
   * @deprecated Use setPrepositionalPhrases(PrepositionalPhrase) or addPrepositionalPhrase(PrepositionalPhrase) instead.
   */
  public void setPrepositionalPhrase(PrepositionalPhrase aPrepPhrase) {
    setPrepositionalPhrases(aPrepPhrase);
  }

  /**
   * Set the prepositional phrase (note singularity)
   * Note that any previous values will be dropped 
   * @param aPrepPhrase 
   */
  public void setPrepositionalPhrases(PrepositionalPhrase aPrepPhrase) {
    if (phrases == null)
      phrases = new ArrayList(1);
    else
      phrases.clear();

    if (aPrepPhrase == null) return;

    Transaction.noteChangeReport(this,new Task.PrepositionChangeReport());
    phrases.add(aPrepPhrase);
    decacheTS();
  }

  /**
   * Adds a PrepositionalPhrase to the list of PrepositionalPhrases.
   * @param aPrepPhrase 
   */
  public void addPrepositionalPhrase(PrepositionalPhrase aPrepPhrase) {
    if (aPrepPhrase == null) 
      throw new IllegalArgumentException("addPrepositionalPhrase requires a non-null argument.");

    if (phrases == null)
      phrases = new ArrayList(1);

    String prep = aPrepPhrase.getPreposition();

    boolean found = false;
    for (ListIterator it = phrases.listIterator(); it.hasNext(); ) {
      PrepositionalPhrase pp = (PrepositionalPhrase) it.next();
      if (prep.equals(pp.getPreposition())) {
        found = true;
        it.set(aPrepPhrase);
        break;
      }
    }
    if (!found) {
      phrases.add(aPrepPhrase);
    }

    Transaction.noteChangeReport(this,new Task.PrepositionChangeReport());
    decacheTS();
  }


  /** @return Workflow that this task is a part of*/
  public Workflow getWorkflow() {
    return workflow;
  }
  /** @param aWorkflow setWorkflow */
  public void setWorkflow(Workflow aWorkflow) {
    workflow = aWorkflow;
    decacheTS();
  }
  
  /** @return Task  - return parent task*/
  public UID getParentTaskUID() {
    return parentUID;
  }
  /** @param pt  */
  public void setParentTask(Task pt) {
    if (pt == null) {
      parentUID = null;
    } else {
      parentUID = pt.getUID();
    }
    //decacheTS();   // no need since toString doesnt use parent
  }
  public void setParentTaskUID(UID uid) {
    parentUID = uid;
    //decacheTS();   // no need since toString doesnt use parent
  }
  
  /**
   * Get the preferences on this task. We assume that if the caller
   * has synchronized the task that he will enumerate the preferences
   * safely so we return an ordinary Enumberation. Otherwise, we
   * return an Enumeration backed by a copy of the preferences to
   * avoid ConcurrentModificationExceptions. With debug logging turned
   * on, callers that haven't synchronized the tasks are printed the
   * first time they getPreferences.
   * @return Enumeration{Preference}
   **/
  private static Set badCallers = new HashSet();
  public Enumeration getPreferences() {
    boolean useBackedEnumerator = !Thread.holdsLock(this);
    if (useBackedEnumerator && logger.isDebugEnabled()) {
      Throwable t = new Throwable();
      StackTraceElement caller = t.getStackTrace()[1];
      synchronized (badCallers) {
        if (badCallers.add(caller)) {
          logger.debug("Unsafe call to Task.getPreferences from " + caller);
        }
      }
    }
    synchronized (this) {
      if (preferences != null && preferences.size()  > 0) {
        // if we need extra protection...
        if (useBackedEnumerator) {
          return new BackedEnumerator(preferences);
        } else{
          return new Enumerator(preferences);
        }
      } else {
        return Empty.enumeration;
      }
    }
  }
  
  /** return the preference for the given aspect type
   * will return null if there is not a preference defined for this aspect type
   * @param aspect_type The Aspect referenced by the preference
   * @return Preference
   **/
  public synchronized Preference getPreference(int aspect_type) {
    if (preferences == null) return null;
    int l = preferences.size();
    for (int i=0; i<l; i++) {
      Preference testpref = (Preference) preferences.get(i);
      if ( testpref.getAspectType() == aspect_type) {
        return testpref;
      }
    }
    return null;
  }
  
  /** return the preferred value for a given aspect type
    * from the defined preference (and scoring function)
    * will return -1 if there is not a preference defined for this aspect type
    * @param aspect_type The Aspect referenced by the preference
    * @return double
    */
  public double getPreferredValue(int aspect_type) {
    double valueresult = -1;
    Preference matchpref = this.getPreference(aspect_type);
    if (matchpref != null) {
      valueresult = matchpref.getScoringFunction().getBest().getValue();
    }
    return valueresult;
  }
  
  /** Get a list of the requested AuxiliaryQueryTypes (int).
    * Note:  if there are no types set, this will return an
    * an array with one element equal to -1 
    * @return int[]
    * @see org.cougaar.planning.ldm.plan.AuxiliaryQueryType
    */
  public int[] getAuxiliaryQueryTypes() {
    // return a copy
    //return (int[])auxqtypes.clone();
    return auxqtypes;           // reduce consing at a cost of object security
  }
  
  /** Set the collection of AuxiliaryQueryTypes that the task is
    * requesting information on.  This information will be returned in
    * the AllocationResult of this task's disposition.
    * Note that this method clears all previous types.
    * @param int[]  A collection of defined AuxiliaryQueryTypes
    * @see org.cougaar.planning.ldm.plan.AuxiliaryQueryType
    */
  public void setAuxiliaryQueryTypes(int[] thetypes) {
    // check the array values
    for(int cit = 0; cit < thetypes.length; cit++) {
      int checktype = thetypes[cit];
      if ( (checktype < -1) || (checktype > AuxiliaryQueryType.LAST_AQTYPE) ) {
        throw new IllegalArgumentException("Task.setAxiliaryQueryTypes(int[] thetypes) " +
                                           "expects a collection of defined types (int) from org.cougaar.planning.ldm.plan.AuxiliaryQueryType");
      }
    }
    //auxqtypes = (int[])thetypes.clone();
    auxqtypes = thetypes;        // reduce consing at a cost of object security
    decacheTS();
  }
  
  
  /** Get the priority of this task.
    * Note that this should only be used when there are competing tasks
    * from the SAME customer.
    * @return byte  The priority of this task
    * @see org.cougaar.planning.ldm.plan.Priority
    */
  public byte getPriority() {
    return priority;
  }
  
  /** set the preferences on this task.
    * @param thepreferences
    */
  public synchronized void setPreferences(Enumeration thepreferences) {
    boolean hadold = false;

    // clear prefs
    if (preferences == null) {
      if (thepreferences.hasMoreElements()) // do we actually need storage?
        preferences = new ArrayList(2);
    } else {
      if (preferences.size()>0) {
        hadold = true;
      }

      preferences.clear();
    }

    while (thepreferences.hasMoreElements()) {
      Preference p = (Preference) thepreferences.nextElement();
      //preferences.add(p.clone());
      preferences.add(p);       // MT
      Transaction.noteChangeReport(this,new Task.PreferenceChangeReport(p.getAspectType()));
    }

    Transaction.noteChangeReport(this,new Task.PreferenceChangeReport());
    decacheTS();
  }


  /** ONLY for infrastructure!  Compare the preferences from two
   * tasks, updating this.preferences to match that.preferences
   * only if needed.
   * @return true IFF the preferences were changed in this.
   **/
  public synchronized boolean private_updatePreferences(TaskImpl that) {
    // this synchronization is scary, but it should be ok since we
    // should not have preference updates in both directions.
    if (this == that) return false;// if eq, cannot do anything useful.
    synchronized (that) {
      ArrayList fps = that.preferences;
      if (fps == preferences) return false; // if prefs are eq, bail out now.
      if (preferences == null) {
        // don't have to test for null, since we'd have caught it
        // above in the prefs == test.
        int l = fps.size();
        preferences = new ArrayList(l);
        for (Iterator i=fps.iterator(); i.hasNext(); ) {
          //preferences.add(((Preference)i.next()).clone());
          preferences.add((Preference)i.next());
        }
        Transaction.noteChangeReport(this,new Task.PreferenceChangeReport());
        return true;
      } else {
        if (fps==null || fps.isEmpty()) {
          if (preferences.isEmpty()) {
            return false;
          } else {
            preferences.clear();
            return true;
          }
        } else {
          // hard case - see if they are equal first
          if (preferences.equals(fps)) {
            // they have the same elements in the same order
            return false;
          } else {
            // they are different.
            preferences.clear();
            int l = fps.size();
            preferences.ensureCapacity(l);
            for (Iterator i=fps.iterator(); i.hasNext(); ) {
              //preferences.add(((Preference)i.next()).clone());
              preferences.add((Preference)i.next());
            }
            Transaction.noteChangeReport(this,new Task.PreferenceChangeReport());
            return true;
          }
        }
      }
    }
  }

  /** Set just one preference in the task's preference list **/
  public synchronized void setPreference(Preference p) {
    if (preferences == null) preferences = new ArrayList(2);

    int at = p.getAspectType();
    Preference old = (Preference) Filters.findElement(preferences, 
                                                      PreferencePredicate.get(at));
    if (old != null) {
      preferences.remove(old);
    }
    // p = p.clone();
    preferences.add(p);
    Transaction.noteChangeReport(this, new Task.PreferenceChangeReport(at,old));
    decacheTS();
  }

  /** add a preference to the already existing preference list
    * @param aPreference
    */
  public void addPreference(Preference aPreference) {
    setPreference(aPreference);
  }
  
  /** Set the priority of this task.
    * Note that this should only be used when there are competing tasks
    * from the SAME customer.
    * @param thepriority
    * @see org.cougaar.planning.ldm.plan.Priority
    */
  public void setPriority(byte thepriority) {
    priority = thepriority;
    decacheTS();
  }
  
  /** WARNING: This date may be null if it is undefined
    * Get the Commitment date of this task.
    * After this date, the task is not allowed to be rescinded
    * or re-planned (change in preferences).
    * @return Date
    */
  public Date getCommitmentDate() {
    if (commitmenttime == 0) return null;
    if (commitmentdate == null) 
      commitmentdate = new Date(commitmenttime);
    return commitmentdate;
  }
  
  private transient Date commitmentdate = null;

  /** 
    * Check to see if the current time is before the Commitment date.
    * Will return true if we have not reached the commitment date.
    * Will return true if the commitment date is undefined (null)
    * Will return false if we have passed the commitment date.
    * @param currentdate  The current date.
    * @return boolean
    */
  public boolean beforeCommitment(Date currentdate) {
    long currenttime = currentdate.getTime();
    if (commitmenttime > 0) {
      if ( currenttime < commitmenttime) {
        return true;
      }
    } else {
      // if the commitmentdate is not defined (null) return true
      return true;
    }
    // if we made it to here the current time is after the commit time.
    return false;
  }
  
  /** Set the Commitment date of this task.
    * After this date, the task is not allowed to be rescinded
    * or re-planned (change in preferences).
    * @param commitDate
    */
  public void setCommitmentDate(Date commitDate) {
    commitmentdate = commitDate;
    commitmenttime = commitDate.getTime();
    decacheTS();
  }

  public boolean isDeleted() {
    return deleted;
  }

  public void setDeleted(boolean newDeleted) {
    deleted = newDeleted;
  }
  
  public void addObservableAspect(int aspectType) {
    if (observableAspects == null) observableAspects = new HashSet();
    observableAspects.add(new Integer(aspectType));
  }

  public Enumeration getObservableAspects() {
    if (observableAspects == null) return Empty.enumeration;
    return new Enumerator(observableAspects.iterator());
  }

  private void decacheTS() { cachedTS=null; }
  private transient String cachedTS = null;

  // String that has the main slots of the task
  public String toString() {
    if (cachedTS != null) return cachedTS;

    Object o;
    StringBuffer buf = new StringBuffer();
    buf.append("<Task src=");
    buf.append((o=getSource())==null?"null":o.toString());
    buf.append(" uid=");
    buf.append((o=getUID())==null?"null":o.toString());
    buf.append(" pUid=");
    buf.append((o=getParentTaskUID())==null?"null":o.toString());
    buf.append(" verb=");
    buf.append((o=getVerb())==null?"null":o.toString());
    buf.append(" dObj=");
    buf.append(directObject==null?"null":directObject.toString());

    if (phrases!=null && phrases.size()>0) {
      buf.append(" ");
      buf.append(phrases.toString());
    }
    if (priority != Priority.UNDEFINED) {
      buf.append(" ");
      buf.append(priority);
    }
    if (commitmenttime != 0) {
      buf.append(" ");
      buf.append(getCommitmentDate().toString());
    }
    if (preferences != null && preferences.size()!=0) {
      buf.append(" ");
      buf.append(preferences.toString());
    }
    if (auxqtypes != null) {
      int l = auxqtypes.length;
      if (l > 0 && !(l==1 && auxqtypes[0]==-1)) {
        buf.append(" (");
        buf.append(l);
        buf.append(" AQ)");
      }
    }
    buf.append(">");
    
    String ts = buf.toString();
    cachedTS = ts;
    return ts;
  }
 
  public static void writeArrayList(ObjectOutputStream s, ArrayList l)
    throws IOException
  {
    Object[] objects = (l == null) ? null : l.toArray();
    // Write out element count
    s.writeObject(objects);
  }

  public static ArrayList readArrayList(ObjectInputStream s)
    throws IOException, ClassNotFoundException
  {
    Object[] objects = (Object[]) s.readObject();
    if (objects == null) return null;
    ArrayList l = new ArrayList(objects.length);
    for (int i = 0; i < objects.length; i++) {
      l.add(objects[i]);
    }
    return l;
  }

  /** serialize tasks making certain that references to other tasks and
   * workflows are appropriately proxied.
   */
  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.defaultWriteObject();

    stream.writeObject(directObject);
    writeArrayList(stream, phrases);
    stream.writeObject(workflow);
    if (stream instanceof org.cougaar.core.persist.PersistenceOutputStream) {
      stream.writeObject(myAnnotation);
      stream.writeObject(observableAspects);
    } else {
      stream.writeObject(myPE);    // this should be derived, not persisted!
    }

    synchronized (this) {     //  make sure the prefs aren't changing while writing
      stream.writeObject(preferences);
    }
  }

  private void readObject(ObjectInputStream stream) throws ClassNotFoundException, IOException {
    stream.defaultReadObject();
    directObject = (Asset) stream.readObject();
    phrases = readArrayList(stream);
    workflow = (Workflow) stream.readObject();
    if (stream instanceof org.cougaar.core.persist.PersistenceInputStream) {
      myAnnotation = (Annotation) stream.readObject();
      observableAspects = (HashSet) stream.readObject();
    } else {
      myPE = (PlanElement) stream.readObject();
    }
    preferences = (ArrayList) stream.readObject();

    pcs = new PropertyChangeSupport(this);
  }

  /**
   * Returns PlanElement that this Task is associated with.  
   * Can be used to discern between expandable and non-expandable
   * Tasks.  If Task has no PlanElement associated with it, will 
   * return null.
   */
  public PlanElement getPlanElement() { return myPE; }

  /**
   * This method sets the PlanElement associated with this Task.
   * **Note to Plugin developers: You do not need to call this method.
   * It is done automatically when PlanElement.setTask() is called.
   */
  public void privately_setPlanElement( PlanElement pe ) { 
    if (myPE != null) {
      synchronized (System.err) {
        System.err.println("Warning: re-disposing "+this+" from "+myPE+" to "+pe+".");
        Thread.dumpStack();
      }
    }
    if (pe == null) {
      synchronized (System.err) {
        System.err.println("Warning: setting "+this+".planElement to null.");
        Thread.dumpStack();
      }
    } 
    myPE = pe;
  }

  public void privately_resetPlanElement() {
    //System.err.println("\n!!!!!!!!!!!!task.privately_resetPlanElement() called\n");
    myPE = null;
  }

  public boolean equals(Object ob) {
    if (ob == this) return true;
    if (ob instanceof Task) {
      return uid.equals(((Task)ob).getUID());
    } else {
      return false;
    }
  }

  public int hashCode()
  {
    // just use the hashcode of the UID.  
    // this means Don't mix UIDs and Tasks in the same hash table...
    return getUID().hashCode();
  }

  public void setSource(ClusterIdentifier asource) {
    ClusterIdentifier old = getSource();
    if (old != null) {
      if (! asource.equals(old)) {
        System.err.println("Bad task.setSource("+asource+") was "+old+":");
        Thread.dumpStack();
      }
    } else {
      super.setSource(asource);
    }
  }

  public void setDestination(ClusterIdentifier dest) {
    super.setDestination(dest);
    if (! dest.equals(getSource())) {
        System.err.println("Suspicious task.setDestination("+dest+") != "+getSource()+":");
        Thread.dumpStack();
    }
  }
  // Private setter without destination check
  public void privately_setDestination(ClusterIdentifier dest) {
    super.setDestination(dest);
  }
      
  public synchronized Object clone() {
    // make sure the clone gets a new oid.

    TaskImpl nt = new TaskImpl();

    // directiveimpl
    nt.setSource(getSource());
    nt.setDestination(getDestination());
    //nt.setPlan(getPlan()); 

    // duplicate the immutable parts
    nt.setVerb(getVerb());
    nt.setDirectObject(getDirectObject());
    nt.setPrepositionalPhrases(getPrepositionalPhrases());
    nt.setPreferences(getPreferences());
    nt.setPriority(getPriority());
    nt.setAuxiliaryQueryTypes(getAuxiliaryQueryTypes());
    nt.setContext(getContext());

    // parent of the clone is our parent.
    nt.setParentTaskUID(getParentTaskUID());

    nt.setWorkflow(null);
    nt.privately_resetPlanElement();

    return nt;
  }

  // new property reading methods returned by TaskImplBeanInfo
  public String getParentTaskID() {
    return (parentUID == null) ? null : parentUID.toString();
  }

  public String getVerbName() {
    return getVerb().toString();
  }

  public String getPlanName() {
    return getPlan().getPlanName();
  }

  private static final PrepositionalPhrase[] emptyPhrases = new PrepositionalPhrase[0];

  public PrepositionalPhrase[] getPrepositionalPhrasesAsArray() {
    if ( phrases == null || phrases.size() == 0) {
      return emptyPhrases;
    }
    int l = phrases.size();
    PrepositionalPhrase p[] = new PrepositionalPhrase[l];
    for (int i = 0; i < l; i++) {
      p[i]=(PrepositionalPhrase)phrases.get(i);
    }
    return p;
  }

  public PrepositionalPhrase getPrepositionalPhraseFromArray(int i) {
    if (phrases == null)
      return null;
    return (PrepositionalPhrase) phrases.get(i);
  }

  private static final Preference[] emptyPreferences = new Preference[0];

  public synchronized Preference[] getPreferencesAsArray() {
    int l;
    if (preferences == null) return emptyPreferences;
    if ((l = preferences.size()) == 0) return emptyPreferences;

    Preference p[] = new Preference[l];
    for (int i=0; i<l; i++) {
      p[i] = (Preference)preferences.get(i);
    }
    return p;
 }

  public synchronized Preference getPreferenceFromArray(int i) {
    if (preferences == null)
      return null;
    return (Preference) preferences.get(i);
  }

  public UID getPlanElementID() {
    if (myPE != null)
      return myPE.getUID();
    return null;
  }

  // ActiveSubscriptionObject
  public boolean addingToLogPlan(Subscriber s) {
    return true;
  }
  public boolean changingInLogPlan(Subscriber s) {
    // execution monitoring / commitment time checks
    if (commitmenttime > 0) {
      if ( s.getClient().currentTimeMillis() > commitmenttime ) {
        // its after the commitment time, don't publish the change and return false
        return false;
      }
    }
    return true;
  }
  public boolean removingFromLogPlan(Subscriber s) {
    NewWorkflow wf = (NewWorkflow) getWorkflow();
    if (wf != null) {
      for (Enumeration tasks = wf.getTasks(); tasks.hasMoreElements(); ) {
        if (tasks.nextElement() == this) {
          if (logger.isDebugEnabled()) {
            logger.debug("Illegal publishRemove subtask still in a workflow: " + this, new Throwable());
          } else if (logger.isWarnEnabled()) {
            logger.warn("Illegal publishRemove subtask still in a workflow: " + this);
          }
          wf.removeTask(this);
        }
      }
    }
    /*
    synchronized(System.err) {
      System.err.println("taskRemoved = "+this);
      Thread.dumpStack();
    }
    */
    return true;
  }


  // 
  // XMLizable method for UI, other clients
  //
  public Element getXML(Document doc) {
      return XMLize.getPlanObjectXML(this,doc);
  }


  //dummy PropertyChangeSupport for the Jess Interpreter.
  public transient PropertyChangeSupport pcs = new PropertyChangeSupport(this);

  public void addPropertyChangeListener(PropertyChangeListener pcl) {
      pcs.addPropertyChangeListener(pcl);
  }

  public void removePropertyChangeListener(PropertyChangeListener pcl)   {
      pcs.removePropertyChangeListener(pcl);
  }

  private Context myContext = null;
  public void setContext(Context context) {
    myContext = context;
  }
  public Context getContext() {
    return myContext;
  }

  private transient Annotation myAnnotation = null;
  public void setAnnotation(Annotation pluginAnnotation) {
    myAnnotation = pluginAnnotation;
  }
  public Annotation getAnnotation() {
    return myAnnotation;
  }

}
 
