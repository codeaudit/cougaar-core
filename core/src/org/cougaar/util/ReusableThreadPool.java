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

package org.cougaar.util;

import java.util.Properties;

/**
 * Provide a pool of reusable threads to reduce the overhead of 
 * construction/destruction of large numbers of standard thread,
 * particularly on VM implementation which use OS-level thread 
 * implementations. 
 *
 * The general contract is that wherever you might have used
 * "new Thread()", you use "ReusableThread.newThread()" instead.
 * Most of the Thread constructors are provided as factory methods -
 * missing are all constructors using ThreadGroup arguments.
 * @property org.cougaar.ReusableThread.initialPoolSize Sets the inital pool size for 
 * the thread pool utility (defaults to 32).
 * @property org.cougaar.ReusableThread.maximumPoolSize Sets the maximum pool size for
 * the thread pool utility (defaults to 64).
 *
 **/

public class ReusableThreadPool {
  public static ReusableThreadPool defaultPool = null;
  
  /** initial number of ReusableThreads in the pool **/
  private static int defaultInitialPoolSize;
  /** maximum number of unused ReusableThreads to keep in the pool **/
  private static int defaultMaximumPoolSize;

  /** initialize initialPoolSize and maximumPoolSize from system,
   * properties and create the default ThreadPool from these
   * values.
   */
  static {
    Properties prop = System.getProperties();
    defaultInitialPoolSize = PropertyParser.getInt("org.cougaar.ReusableThread.initialPoolSize", 32);

    defaultMaximumPoolSize = PropertyParser.getInt("org.cougaar.ReusableThread.maximumPoolSize", 64);

  }

  /** The ThreadGroup of the pool - all threads in the pool must be
   * members of the same threadgroup.
   **/
  private ThreadGroup group;
  /** The maximum number of unused threads to keep around in the pool.
   * anything beyond this may be destroyed or GCed.
   **/
  private int maximumSize;
  /** the number of unused threads currently in the pool. **/
  private int poolSize;
  /** the actual pool **/
  private ReusableThread pool[];

  public ThreadGroup getThreadGroup() { return group; }
  public int getMaximumSize() { return maximumSize; }
  public int size() { return poolSize; }

  private int totalThreads = 0;
  /** Return the total number of threads created.  This can be larger than
   * the maximum pool size because the pool is usually 
   * allowed to create additional threads which will not be reclaimed.
   **/
  public int getAllocatedThreads() { return totalThreads; }

  public ReusableThreadPool(ThreadGroup group, int initial, int maximum) {
    this.group = group;

    if (initial > maximum) 
      initial = maximum;

    maximumSize = maximum;

    pool = new ReusableThread[maximum];
      
    for (int i = 0 ; i < initial; i++) {
      pool[i] = constructReusableThread();
    }
    poolSize = initial;
  }

  public ReusableThreadPool(int initial, int maximum) {
    this(Thread.currentThread().getThreadGroup(), initial, maximum);
  }


  public ReusableThread getThread() {
    return getThread(null, "ReusableThread");
  }
  public ReusableThread getThread(String name) {
    return getThread(null, name);
  }
  public ReusableThread getThread(Runnable runnable) {
    return getThread(runnable, "ReusableThread");    
  }
  
  public ReusableThread getThread(Runnable runnable, String name) {
    ReusableThread t = null;

    synchronized (this) {
      if (poolSize>0) {
        poolSize--;
        t = pool[poolSize];
        pool[poolSize] = null;  // clear for gc
      }
    }
    if (t == null) {
      t = constructReusableThread();
    }

    t.setRunnable(runnable);
    t.setName(name);

    //System.err.println("getThread pool="+poolSize+" = "+t);
    return t;
  }
  
  /** actually construct a new ReusableThread **/
  protected ReusableThread constructReusableThread() {
    totalThreads++;
    return new ReusableThread(this);
  }

  /** return a reusableThread to our pool.  package protected
   * so we don't get the wrong thread in our pool.
   **/
  void reclaimReusableThread(ReusableThread t) {
    synchronized (this) {
      if (poolSize < maximumSize) {
        // the pool has space - reuse it
        pool[poolSize] = t;
        poolSize++;
        //System.err.println("reclaimThread pool="+poolSize+" = "+t);
      } else {
        // the pool is already full - drop it.
        // t.destroy();
        //System.err.println("reclaimThread dropped "+t);
      }
    }
  }

  
  public static ReusableThreadPool getDefaultThreadPool() {
    if (defaultPool == null) {
      defaultPool = new ReusableThreadPool(defaultInitialPoolSize, defaultMaximumPoolSize);
    }
    return defaultPool;
  }

  //
  // regression test
  //
  
  public static class Counter {
    private int value = 0;
    public void incr() { value++; }
    public String toString() { return new Integer(value).toString(); }
    public int getValue() { return value; }
    public Counter() { }
    public Counter(int v) { value = v; }
  }

  public static class TestThread extends ReusableThread {
    static final Counter count = new Counter();

    Counter mycount;
    int myinvoke = 0;
    public TestThread(ReusableThreadPool p) {
      super(p);
      count.incr();
      mycount = new Counter(count.getValue());
      System.err.println("Created "+this);
    }
    public Runnable getRunnable() {
      myinvoke++;
      System.err.println("Invoking "+this);
      return super.getRunnable();
    }
    protected void reclaim() {
      System.err.println("Reclaiming "+this);
      super.reclaim();
    }
    public String toString() { 
      return "<"+getName()+" "+mycount+"("+myinvoke+")>";
    }

    public static int getCount() { return count.getValue(); }
  }

  public static class TestPool extends ReusableThreadPool {
    public TestPool(int init, int max) {
      super(init,max);
    }

    protected ReusableThread constructReusableThread() {
      ReusableThread t = new TestThread(this);
      return t;
    }
  }

  public static void main(String args[]) {
    TestPool pool = new TestPool(5, 20);

    Runnable sleep5 = new Runnable() {
      public void run() { 
        try {
          Thread.sleep((System.currentTimeMillis())%5000); 
        } catch (InterruptedException ie) {}
        System.err.println("Done with "+Thread.currentThread());
      }};

    for (int i = 0 ; i< 100; i++) {
      Thread t = pool.getThread(sleep5, "Iteration "+i);

      t.start();

      try {
        Thread.sleep(100); 
      } catch (InterruptedException ie) {}

    }
    System.err.println("Created total of "+TestThread.getCount()+" threads");
  }
}
