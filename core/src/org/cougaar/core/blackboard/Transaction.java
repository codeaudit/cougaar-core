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

package org.cougaar.core.blackboard;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Abstraction of a Transaction object.
 * Required Transaction functionality tracks the Subscriber
 * and maintains the ChangeReport set.
 *
 * May be extended to add additional functionality to a Subscriber.
 * @see org.cougaar.core.blackboard.Subscriber#newTransaction()
 **/
public class Transaction {
  // instantiable stuff
  protected Subscriber subscriber;

  public Transaction(Subscriber s) {
    subscriber = s;
  }

  /** a map of object to List (of outstanding changes)
   **/
  private Map _map = null;
  private final Map map() {
    if (_map == null) {
      _map = new HashMap(5);
    }
    return _map;
  }

  /** Note a ChangeReport.  May be called by anyone (inside a Transaction) 
   * wishing to publish a detailed ChangeReport on an object.
   **/
  public final static void noteChangeReport(Object o, ChangeReport cr) {
    Transaction t = getCurrentTransaction();
    if (t != null) {
      t.private_noteChangeReport(o,cr);
    } else {
      System.err.println("Warning: ChangeReport added out of transaction on "+o+
                         ":\n\t"+cr);
      Thread.dumpStack();
    }
  }

  /** Note a Collection of ChangeReports on a single object.
   * May be called by anyone (inside a Transaction) wishing to publish a 
   * detailed set of ChangeReports on an object.
   **/
  public final static void noteChangeReport(Object o, Collection c) {
    Transaction t = getCurrentTransaction();
    if (t != null) {
      t.private_noteChangeReport(o,c);
    } else {
      System.err.println("Warning: ChangeReport added out of transaction on "+o+
                         ":\n\t"+c);
      Thread.dumpStack();
    }
  }

  private final synchronized void private_noteChangeReport(Object o, ChangeReport r) {
    Map m = map();
    List changes = (List)m.get(o);
    if (changes == null) {
      changes = new ArrayList(3);
      m.put(o,changes);
    } 
    changes.add(r);
  }

  /** Bulk version of noteChangeReport.
   **/
  
  private final synchronized void private_noteChangeReport(Object o, Collection r) {
    Map m = map();
    List changes = (List)m.get(o);
    if (changes == null) {
      changes = new ArrayList(r.size());
      m.put(o,changes);
    } 
    changes.addAll(r);
  }

  /**
   * Publishable objects may collect descriptions of changes made
   * to them and make it available to the infrastructure via this slot. <p>
   *
   * Calling this method should atomically return the List and
   * clear the stored value.  Implementations may also want to 
   * keep track of the changing thread to avoid changing the object
   * simultaneously in different transactions. <p>
   *
   * Plugins must <em>never</em> call this or changes will not be propagated. <p>
   *
   * The List returned (if any) may not be reused, as the infrastructure
   * can and will modify it for its purposes. <p>
   *
   * @return A List of ChangeReport instances or null.  Implementations
   * are encouraged to return null if no trackable changes were made, rather
   * than an empty List.
   * @see ChangeReport
   **/
  public synchronized final List getChangeReports(Object o) {
    // be careful not to create map unless we need to...
    if (_map== null) return null;
    List l = (List)_map.get(o);
    if(l!=null) {
      _map.remove(o);
    }
    return l;
  }

  /** Called by subscriber to check for changes made to objects which hadn't 
   * actually been publishChanged.
   **/
  synchronized final Map getChangeMap() {
    Map m = _map;
    _map = null;
    return m;
  }

  // Thread-binding to a Transaction instance
  private static final HashMap transactionTable = new HashMap(89);

  /** Register a transaction as open **/
  public final static void open(Transaction t) {
    synchronized (transactionTable) {
      Thread me = Thread.currentThread();
      Object o = transactionTable.get(me);
      if (o != null) {
        throw new RuntimeException("Attempt to open a nested transaction:\n"+
                                   "\tPrevious was: "+o+"\n"+
                                   "\tNext is: "+t);
      }
      transactionTable.put(me, t);
    }
  }

  /** Register a transaction as closed **/
  public final static void close(Transaction t) {
    synchronized (transactionTable) {
      Thread me = Thread.currentThread();
      Object o = transactionTable.get(me);
      if (o != t) {
        throw new RuntimeException("Attempt to close a transaction inappropriately:\n"+
                                   "\tPrevious was: "+o+"\n"+
                                   "\tNext is: "+t);
      }
      // we could do transactionTable.remove(me), but that would lead to more
      // consing of entries than we want.  Instead, we'll have a GC thread which
      // removes empty entries periodically.
      //transactionTable.remove(me);
      transactionTable.put(me, null);
    }
  }

  /** get the current Transaction.  **/
  public final static Transaction getCurrentTransaction() {
    synchronized (transactionTable) {
      return (Transaction) transactionTable.get(Thread.currentThread());
    }
  }

  static {
    Runnable gc = new Runnable() {
        public void run() {
          while (true) {
            try {
              Thread.sleep(10*1000); // run every 10 seconds.
              synchronized (transactionTable) {
                int c = 0;
                int a = 0;
                for (Iterator i = transactionTable.entrySet().iterator(); i.hasNext();) {
                  a++;
                  Map.Entry entry = (Map.Entry) i.next();
                  if (entry.getValue() == null) {
                    i.remove();
                    c++;
                  }
                }
                /*
                if (c>0) {
                  System.err.println("TransactionTable GC reclaimed "+c+"/"+a+" Transaction entries");
                }
                */
              }
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }
      };
    (new Thread(gc, "TransactionTable GC")).start();
  }

  /*
    // old implementation using ThreadLocal

  private static final ThreadLocal theTransaction = new ThreadLocal() {};


  public final static void open(Transaction t) {
    synchronized (theTransaction) {
      Object o = theTransaction.get();
      if (o != null) {
        throw new RuntimeException("Attempt to open a nested transaction:\n"+
                                   "\tPrevious was: "+o+"\n"+
                                   "\tNext is: "+t);
      }
      theTransaction.set(t);
    }
  }


  public final static void close(Transaction t) {
    synchronized (theTransaction) {
      Object o = theTransaction.get();
      if (o != t) {
        throw new RuntimeException("Attempt to close a transaction inappropriately:\n"+
                                   "\tPrevious was: "+o+"\n"+
                                   "\tNext is: "+t);
      }
      theTransaction.set(null);
    }
  }


  public final static Transaction getCurrentTransaction() {
    //No synchronization because it  doesn't buy us any actual safety
    return (Transaction) theTransaction.get();
  }
  */

}
