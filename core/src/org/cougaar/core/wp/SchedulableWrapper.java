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

package org.cougaar.core.wp;

import java.util.TimerTask;
import org.cougaar.core.service.ThreadService;
import org.cougaar.core.thread.Schedulable;

/**
 * Wrapper for schedulable that supports timer-tasks that
 * run within this runnable, as opposed to the timer's thread.
 * <p>
 * Also adds support for the Scheduled API, which allows the
 * runner to know the schedulable (i.e. "run(thread)").
 * <p>
 * This will likely be merged into the ThreadService (bug TBA)
 * and become the new Schedulable API.
 */
public final class SchedulableWrapper {

  private final ThreadService threadService;
  private final Schedulable thread;

  /**
   * This is the proposed replacement for ThreadService's:<pre>
   *    Schedulable getThread(Object consumer, Runnable runnable, String name);
   * </pre>.
   *
   * @param client either a Scheduled or Runnable
   */
  public static SchedulableWrapper getThread(
      ThreadService threadService,
      Object client,
      String name) {
    return new SchedulableWrapper(threadService, client, name);
  }

  private SchedulableWrapper(
      ThreadService threadService,
      Object client,
      String name) {
    this.threadService = threadService;
    Runnable r;
    if (client instanceof Scheduled) {
      if (client instanceof Runnable) {
        // protect the client from ambiguous usage
        throw new IllegalArgumentException(
            "Client implements both \"Scheduled\" and"+
            " \"Runnable\", which is ambiguous: "+
            client.getClass().getName());
      }
      final Scheduled sched = (Scheduled) client;
      r = new Runnable() {
        public void run() {
          sched.run(SchedulableWrapper.this);
        }
      };
    } else if (client instanceof Runnable) {
      // when this is merged into the ThreadService, the Schedulable
      // should be used within the scheduler and this Runnable will
      // be something like:
      //    final Runnable runner = (Runnable) client;
      //    sched = new Scheduled() {
      //      public void run(SchedulableWrapper thread) {
      //        // ignore the thread
      //        runner.run();
      //      }
      //    };
      r = (Runnable) client;
    } else {
      throw new IllegalArgumentException(
          "Expecting \"Scheduled\" or \"Runnable\", not "+
          (client == null ? "null" : client.getClass().getName()));
    }
    this.thread = threadService.getThread(r, r, name);
  }

  //
  // same as schedulable:
  //

  public void start() {
    thread.start();
  }
  public boolean cancel() {
    return thread.cancel();
  }
  public int getState() {
    return thread.getState();
  }
  public Object getConsumer() {
    return thread.getConsumer();
  }

  //
  // like a TimerTask, but the client runs in a scheduled thread
  // instead of the timer's thread.
  //

  public TimerTask schedule(long delay) {
    TimerTask t = new RestartTask();
    threadService.schedule(t, delay);
    return t;
  }
  public TimerTask schedule(long delay, long interval) {
    TimerTask t = new RestartTask();
    threadService.schedule(t, delay, interval);
    return t;
  }
  public TimerTask scheduleAtFixedRate(long delay, long interval) {
    TimerTask t = new RestartTask();
    threadService.scheduleAtFixedRate(t, delay, interval);
    return t;
  }

  private final class RestartTask extends TimerTask {
    public void run() {
      start();
    }
  }
}
