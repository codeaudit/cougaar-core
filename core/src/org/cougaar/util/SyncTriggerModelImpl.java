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

// soon to move to "org.cougaar.util.Trigger":
import org.cougaar.util.Trigger;

/**
 * A standard <code>TriggerModel</code> that batches "trigger()"
 * requests for a single-threaded client <code>Trigger</code>,
 * plus provides support for "suspend()/resume()" and 
 * "start()/stop()".
 * <p>
 * Only one "clientTrigger.trigger()" will be run at a time,
 * so the client Trigger can be non-synchronized.  Trigger
 * requests are batched -- for example, the first client
 * request for "this.trigger()" calls the trigger-registry's 
 * "trigger()" (queue), and further "this.trigger()" requests 
 * are ignored until the trigger-registry runs the client's 
 * trigger.
 * <p>
 * If the "clientTrigger.trigger()" throws an exception then
 * "halt()" is called -- the model is suspended and stopped,
 * and the exception is passed back to the trigger-registry.
 * <p>
 * Note that "suspend()" will wait for a running 
 * "clientTrigger.trigger()" to complete, and if the "suspend()"
 * is interrupted then the behavior is as if the "suspend()" was
 * never requested.  The same logic goes for "stop()".
 *
 * @see TriggerModel
 */
public final class SyncTriggerModelImpl implements TriggerModel {

  /**
   * "Debug" flag; set to "true" for verbose output.
   */
  private static final boolean DEBUG = false;

  /**
   * "Profile" flag; set to "true" for trigger/run statistics
   * to be printed every five seconds.
   */
  private static final boolean PROFILE = false;

  private static final ProfileStats profileStats =
    (PROFILE ? (new ProfileStats()) : null);

  static {
    if (PROFILE) {
      Runnable r = 
        new Runnable() {
          public void run() {
            while (true) {
              System.out.println(profileStats);
              try {
                Thread.sleep(5000);
              } catch (Exception e) {
              }
            }
          }
        };
      Thread t = new Thread(r);
      t.start();
    }
  }

  private final TriggerRegistry triggerRegistry;
  private final Trigger clientTrigger;
  private Trigger registryTrigger;

  private final Trigger innerTrigger = 
    new Trigger() {
      public void trigger() {
        runInnerTrigger();
      }
      public String toString() {
        return SyncTriggerModelImpl.this.toString();
      }
    };

  private final Object stateLock = new Object();
  private int state;

  // state flags:
  private static final int TRIGGERED = (1 << 0);
  private static final int QUEUED    = (1 << 1);
  private static final int RUNNING   = (1 << 2);
  private static final int SUSPENDED = (1 << 3);
  private static final int STOPPED   = (1 << 4);

  public SyncTriggerModelImpl(
      TriggerRegistry triggerRegistry,
      Trigger clientTrigger) {
    this.clientTrigger = clientTrigger;
    this.triggerRegistry = triggerRegistry;
    // null-check
    if ((clientTrigger == null) ||
        (triggerRegistry == null)) {
      throw new NullPointerException();
    }
    // begin as "stopped", pending "start()"
    state = STOPPED;
  }

  // for the client's use
  public void trigger() {
    boolean enQ = false;
    synchronized (stateLock) {
      int ostate = state;
      if (state == 0) {
        state = (TRIGGERED | QUEUED);
        enQ = true;
      } else {
        state |= TRIGGERED;
      }
      if (DEBUG) {
        System.out.println(
            this+" trigger "+getState(ostate)+" -> "+
            getState(state)+", enQ="+enQ);
      } else {
        // the "ostate" is unused if (DEBUG==false), so the 
        // "int ostate = state" should be optimized away.
      }
    }
    if (PROFILE) {
      profileStats.addTrigger();
    }
    if (enQ) {
      registryTrigger.trigger();
    }
  }

  public void initialize() {
    // no-op
  }

  public void load() {
    // no-op
  }

  public void start() {
    boolean enQ = false;
    synchronized (stateLock) {
      if ((state & STOPPED) == 0) {
        if (DEBUG) {
          System.out.println(
              this+" <skip> start "+getState(state));
        }
        return;
      }
      int ostate = state;
      if ((state & RUNNING) == 0) {
        // assert (registryTrigger == null);
        registryTrigger = triggerRegistry.register(innerTrigger);
        if (registryTrigger == null) {
          throw new NullPointerException(
              "Unable to register "+clientTrigger+
              " in registry "+triggerRegistry);
        }
        state &= ~STOPPED;
        if (state == TRIGGERED) {
          state = (TRIGGERED | QUEUED);
          enQ = true;
        }
      } else {
        state &= ~STOPPED;
      }
      if (DEBUG) {
        System.out.println(
            this+" start "+getState(ostate)+" -> "+
            getState(state)+", enQ="+enQ);
      }
    }
    if (enQ) {
      registryTrigger.trigger();
    }
  }

  // for trigger-registry callback's use, called from "innerTrigger"
  private final void runInnerTrigger() {
    synchronized (stateLock) {
      // assert ((state & TRIGGERED) != 0);
      int ostate = state;
      if (state == (TRIGGERED | QUEUED)) {
        state = RUNNING;
      } else {
        if ((state & (SUSPENDED | STOPPED)) != 0) {
          state &= ~QUEUED;
          if (DEBUG) {
            System.out.println(
                this+" <skip> run "+getState(ostate)+" -> "+
                getState(state));
          }
          return;
        }
        state &= ~(TRIGGERED | QUEUED);
        state |= RUNNING;
      }
      if (DEBUG) {
        System.out.println(
            this+" run "+getState(ostate)+" -> "+
            getState(state));
      }
    }
    if (PROFILE) {
      profileStats.addRun();
    }
    try {
      clientTrigger.trigger();
    } catch (Throwable die) {
      synchronized (stateLock) {
        state &= ~RUNNING;
        halt();
      }
      if (die instanceof RuntimeException) {
        throw (RuntimeException) die;
      } else {
        throw new RuntimeException(die.getMessage());
      }
    }
    boolean enQ = false;
    synchronized (stateLock) {
      int ostate = state;
      if (state == RUNNING) {
        state = 0;
      } else if ((state & (SUSPENDED | STOPPED)) == 0) {
        state &= ~RUNNING;
        if (state == TRIGGERED) {
          state = (TRIGGERED | QUEUED);
          enQ = true;
        }
      } else {
        state &= ~RUNNING;
        if ((state & STOPPED) != 0) {
          // assert (registryTrigger != null);
          triggerRegistry.unregister(innerTrigger);
          registryTrigger = null;
        }
        if (DEBUG) {
          System.out.println(
            this+" notify "+getState(ostate)+" -> "+getState(state));
        }
        stateLock.notifyAll();
      }
      if (DEBUG) {
        System.out.println(
            this+" ran "+getState(ostate)+" -> "+
            getState(state)+", enQ="+enQ);
      }
    }
    if (enQ) {
      registryTrigger.trigger();
    }
  }

  public void suspend() {
    synchronized (stateLock) {
      if ((state & SUSPENDED) != 0) {
        if (DEBUG) {
          System.out.println(
              this+" <skip> suspend "+getState(state));
        }
        return;
      }
      int ostate = state;
      state |= SUSPENDED;
      if (DEBUG) {
        System.out.println(
            this+" suspend "+getState(ostate)+" -> "+
            getState(state));
      }
      while ((state & RUNNING) != 0) {
        try {
          if (DEBUG) {
            System.out.println(
                this+" waiting for suspend "+getState(state));
          }
          stateLock.wait();
          if (DEBUG) {
            System.out.println(
                this+" wake for suspend "+getState(state));
          }
        } catch (InterruptedException ie) {
          if ((state & RUNNING) != 0) {
            state &= ~SUSPENDED;
            System.err.println(
                clientTrigger+" \"suspend()\" interrupted,"+
                " cancelling the \"suspend()\"");
          }
          break;
        }
      }
    }
  }

  public void resume() {
    boolean enQ = false;
    synchronized (stateLock) {
      int ostate = state;
      if ((state & SUSPENDED) == 0) {
        if (DEBUG) {
          System.out.println(
              this+" <skip> resume "+getState(state));
        }
        return;
      }
      state &= ~SUSPENDED;
      if (state == TRIGGERED) {
        state = (TRIGGERED | QUEUED);
        enQ = true;
      }
      if (DEBUG) {
        System.out.println(
            this+" resume "+getState(ostate)+" -> "+
            getState(state)+", enQ="+enQ);
      }
    }
    if (enQ) {
      registryTrigger.trigger();
    }
  }

  public void stop() {
    synchronized (stateLock) {
      if ((state & STOPPED) != 0) {
        if (DEBUG) {
          System.out.println(
              this+" <skip> stop "+getState(state));
        }
        return;
      }
      int ostate = state;
      state |= STOPPED;
      if ((state & RUNNING) == 0) {
        // assert (registryTrigger != null);
        triggerRegistry.unregister(innerTrigger);
        registryTrigger = null;
      } else {
        while ((state & RUNNING) != 0) {
          try {
            if (DEBUG) {
              System.out.println(
                  this+" waiting for stop "+getState(state));
            }
            stateLock.wait();
            if (DEBUG) {
              System.out.println(
                  this+" wake for stop "+getState(state));
            }
          } catch (InterruptedException ie) {
            if ((state & RUNNING) != 0) {
              state &= ~STOPPED;
              System.err.println(
                  clientTrigger+" \"stop()\" interrupted,"+
                  " cancelling the \"stop()\"");
            }
            break;
          }
        }
      }
      if (DEBUG) {
        System.out.println(
            this+" stop "+getState(ostate)+" -> "+
            getState(state));
      }
    }
  }

  public void halt() {
    synchronized (stateLock) {
      suspend();
      stop();
    }
  }

  public void unload() {
    // no-op
  }

  public int getModelState() {
    synchronized (stateLock) {
      if ((state & RUNNING) != 0) {
        return ACTIVE;
      } else if ((state & STOPPED) != 0) {
        return LOADED;
      } else if ((state & SUSPENDED) != 0) {
        return IDLE;
      } else {
        return ACTIVE;
      }
    }
  }

  private static String getState(int s) {
    if (s == 0) {
      return "I";  // Idle
    } else {
      StringBuffer buf = new StringBuffer(5);
      if ((s & TRIGGERED) != 0) {
        buf.append("T"); // Triggered
      }
      if ((s & QUEUED) != 0) {
        buf.append("Q"); // Queued
      }
      if ((s & RUNNING) != 0) {
        buf.append("R"); // Running
      }
      if ((s & SUSPENDED) != 0) {
        buf.append("S"); // Suspended
      }
      if ((s & STOPPED) != 0) {
        buf.append("D"); // Dead
      }
      return buf.toString();
    }
  }

  public String toString() {
    int s = state;
    return 
      getState(s)+":"+
      clientTrigger+":"+
      registryTrigger;
  }

  // for internal PROFILE use:
  private static class ProfileStats {
    private int trigCounter = 1;
    private int runCounter = 1;
    public ProfileStats() {
    }
    public synchronized void addTrigger() {
      trigCounter++;
    }
    public synchronized void addRun() {
      runCounter++;
    }
    public synchronized String toString() {
      return
        "\nTRIGGER-PROFILE {"+
        "\n  #triggers:  "+trigCounter+
        "\n  #runs:      "+runCounter+
        "\n  runs/trigs: "+
        (((double) runCounter)/trigCounter)+
        "\n}";
    }
  }
}
