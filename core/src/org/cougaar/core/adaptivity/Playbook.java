/* 
 * <copyright>
 * Copyright 2002 BBNT Solutions, LLC
 * under sponsorship of the Defense Advanced Research Projects Agency (DARPA).

 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the Cougaar Open Source License as published by
 * DARPA on the Cougaar Open Source Website (www.cougaar.org).

 * THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 * PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 * IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 * ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 * HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 * DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 * TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.core.adaptivity;

import java.io.InputStream;
import java.io.StreamTokenizer;
import java.util.*;
import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.plugin.ComponentPlugin;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.service.BlackboardService;
import org.cougaar.core.service.PlaybookReadService;
import org.cougaar.core.service.PlaybookConstrainService;
import org.cougaar.util.GenericStateModelAdapter;
import org.cougaar.util.CircularQueue;

/**
 * A container for the active Plays. The plays are initialized from a
 * file specified as a plugin parameter. Playbook users access the
 * plays through two services: {@link PlaybookReadService} and
 * {@link PlaybookConstrainService}. The former returns the constrained
 * plays. The latter constrains the original plays with
 * {@link OperatingModePolicy}s.
 **/
public class Playbook
  extends ComponentPlugin
  implements ServiceProvider
{
  private Play[] originalPlays = new Play[0];
  private Play[] constrainedPlays = new Play[0];
  private List constraints = new ArrayList();
  private LoggingService logger;
  private List listeners = new ArrayList(2);
  private CircularQueue todo = new CircularQueue();

  private class PlaybookReadServiceImpl implements PlaybookReadService {
    private boolean active = true;
    /**
     * Gets an array of the current plays
     * @return an array of the current plays
     **/
    public Play[] getCurrentPlays() {
      if (!active) throw new RuntimeException("Service has been released or revoked");
      return constrainedPlays;
    }

    /**
     * Add a listener to the playbook. The listener will be
     * publishChanged if this Playbook is modified.
     * @param l the Listener
     **/
    public void addListener(Listener l) {
      if (!active) throw new RuntimeException("Service has been released or revoked");
      listeners.add(l);
    }
    /**
     * Remove a listener to the playbook. The listener will no longer
     * be publishChanged if this Playbook is modified.
     * @param l the Listener
     **/
    public void removeListener(Listener l) {
      if (!active) throw new RuntimeException("Service has been released or revoked");
      listeners.remove(l);
    }
  }

  private class PlaybookConstrainServiceImpl implements PlaybookConstrainService {
    private boolean active = true;
    /**
     * Add another OperatingModePolicy constraint. The plays are
     * modified so that in all cases where the if clause of the
     * constraint is true the OperatingMode ranges will all fall
     * within the constraint.
     * @param omp the constraint to add
     **/
    public void constrain(OperatingModePolicy omp) {
      if (!active) throw new RuntimeException("Service has been released or revoked");
      addConstraint(omp);
    }

    /**
     * Remove an OperatingModePolicy constraint. The current plays are
     * recomputed to omit the removed constraint.
     * @param omp the constraint to remove
     **/
    public void unconstrain(OperatingModePolicy omp) {
      if (!active) throw new RuntimeException("Service has been released or revoked");
      removeConstraint(omp);
    }
  }

  /**
   * Override to register the services we provide.
   **/
  public void load() {
    super.load();
    logger = (LoggingService) getServiceBroker().getService(this, LoggingService.class, null);
    getServiceBroker().addService(PlaybookReadService.class, this);
    getServiceBroker().addService(PlaybookConstrainService.class, this);
  }

  /**
   * Override to unregister the services we provide.
   **/
  public void unload() {
    getServiceBroker().revokeService(PlaybookConstrainService.class, this);
    getServiceBroker().revokeService(PlaybookReadService.class, this);
    getServiceBroker().releaseService(this, LoggingService.class, logger);
    super.unload();
  }

  /**
   * Read the plays from a file.
   **/
  public void setupSubscriptions() {
    String playFileName = getParameters().iterator().next().toString();
    try {
      InputStream is = getConfigFinder().open(playFileName);
      try {
        Parser p = new Parser(new StreamTokenizer(is));
        Play[] plays = p.parsePlays();
        setPlays(plays);
      } finally {
        is.close();
      }
    } catch (Exception e) {
      logger.error("Error parsing play file", e);
    }
  }

  /**
   * Handle requests that arrived through our services. These requests
   * all fire listeners. The services cannot themselves do this
   * because of the possibility of a deadlock due to attempts to open
   * two blackboard transactions simultaneously. The requests are
   * placed in a queue and executed here.
   **/
  public void execute() {
    synchronized (todo) {
      while (todo.size() > 0) {
        try {
          ((Runnable) todo.next()).run();
        } catch (RuntimeException e) {
          logger.error("Error running delayed job", e);
        }
      }
    }
  }

  /**
   * Gets (creates) one of our services. Part of the implementation of
   * the ServiceProvider API
   * @param sb the ServiceBroker making the request
   * @param requestor the actual requestor on whose behalf the broker is acting
   * @param serviceClass the class of the Service desired.
   * @return an instance of the requested Service if it one we supply.
   **/
  public Object getService(ServiceBroker sb, Object requestor, Class serviceClass) {
    if (serviceClass == PlaybookReadService.class) {
      return new PlaybookReadServiceImpl();
    }
    if (serviceClass == PlaybookConstrainService.class) {
      return new PlaybookConstrainServiceImpl();
    }
    return null;
  }

  /**
   * Release one of our services. The services use no resources, so
   * there is nothing to do.
   **/
  public void releaseService(ServiceBroker sb, Object requestor, Class
                             serviceClass, Object service) {
    if (service instanceof PlaybookReadServiceImpl) {
      ((PlaybookReadServiceImpl) service).active = false;
      return;
    }
    if (service instanceof PlaybookConstrainServiceImpl) {
      ((PlaybookConstrainServiceImpl) service).active = false;
      return;
    }
    throw new IllegalArgumentException("Not my service: " + service);
  }

  private synchronized void addConstraint(OperatingModePolicy omp) {
    constraints.add(omp);
    constrainPlays(omp);
    fireListenersLater();
  }

  private synchronized void removeConstraint(OperatingModePolicy omp) {
    constraints.remove(omp);
    constrainPlays();
    fireListenersLater();
  }

  private void fireListenersLater() {
    synchronized (todo) {
      todo.add(new Runnable() {
        public void run() {
          fireListeners();
        }
      });
    }
    blackboard.signalClientActivity();
  }

  private void fireListeners() {
    for (Iterator i = listeners.iterator(); i.hasNext(); ) {
      PlaybookReadService.Listener l = (PlaybookReadService.Listener) i.next();
      blackboard.publishChange(l);
    }
    if (logger.isDebugEnabled()) logger.debug("New constrained plays" + System.getProperty("line.separator") + this);
  }

  private synchronized void setPlays(Play[] plays) {
    originalPlays = plays;
    constrainPlays();
    fireListeners();
  }

  /**
   * Compute the intersection of two collections
   **/
  private static Collection intersect(Collection c1, Collection c2) {
    List result = new ArrayList();
    if (c1.size() > c2.size()) {
      // Make c1 the smaller collection
      Collection t = c1;
      c1 = c2;
      c2 = t;
    }
    for (Iterator i = c1.iterator(); i.hasNext(); ) {
      Object ele = i.next();
      if (c2.contains(ele)) {
        result.add(ele);
      }
    }
    return result;
  }

  private void constrainPlays() {
    constrainedPlays = originalPlays;
    for (Iterator i = constraints.iterator(); i.hasNext(); ) {
      constrainPlays((OperatingModePolicy) i.next());
    }
  }

  /**
   * Rewrite the playbook so that all the plays conform to a new
   * OperatingModePolicy. The procedure is as follows: Scan the plays
   * to find those plays that set OperatingModes overlapping with the
   * OperatingModes of the policy. Then check if the conditions under
   * which the policy applies overlap with the conditions under which
   * the play applies. If there is overlap, rewrite the play as
   * follows: Change the if clause of the original play so it applies
   * only when the policy does not. Add a new play that applies when
   * both the original play would have applied and the policy applies.
   * The OperatingModes of the new play are those of the original play
   * but with the ranges of values reduced to those values that the
   * play and the policy have in common. If the set of allowed values
   * for any OperatingMode is empty, then no play is added.
   **/
  private void constrainPlays(OperatingModePolicy omp) {
    List newConstrainedPlays = new ArrayList(constrainedPlays.length);
    ConstrainingClause ompIfClause = omp.getIfClause();
    ConstraintPhrase[] ompConstraints = omp.getOperatingModeConstraints();
    Map ompModes = new HashMap();
    for (int i = 0; i < ompConstraints.length; i++) {
      ConstraintPhrase cp = ompConstraints[i];
      String proxyName = cp.getProxyName();
      ompModes.put(proxyName, cp);
    }
    for (int i = 0; i < constrainedPlays.length; i++) {
      Play play = constrainedPlays[i];
      ConstraintPhrase[] playConstraints = play.getOperatingModeConstraints();
      ConstraintPhrase[] newConstraints = null;
      for (int j = 0; j < playConstraints.length; j++) {
        ConstraintPhrase cp = playConstraints[j];
        String proxyName = cp.getProxyName();
        if (!ompModes.containsKey(proxyName)) {
          if (newConstraints != null) {
            newConstraints[j] = playConstraints[j]; // No conflict here
          }
        } else {
          if (newConstraints == null) {
            newConstraints = new ConstraintPhrase[playConstraints.length];
            System.arraycopy(playConstraints, 0, newConstraints, 0, j);
          }
        }
      }
      if (newConstraints == null) {
        newConstrainedPlays.add(play); // Keep the play as is
      } else {
        // First write a Play that applies when the original play
        // applies, but the constraint policy does not apply
        ConstrainingClause playIfClause = play.getIfClause();
        ConstrainingClause newIfClause = new ConstrainingClause();
        newIfClause.push(playIfClause);
        newIfClause.push(ompIfClause);
        newIfClause.push(BooleanOperator.NOT);
        newIfClause.push(BooleanOperator.AND);
        Play newPlay = new Play(newIfClause, playConstraints);
        newConstrainedPlays.add(newPlay);

        // Now write a Play that applies when both the original play
        // and the constraint policy apply
        newIfClause = new ConstrainingClause();
        newIfClause.push(playIfClause);
        newIfClause.push(ompIfClause);
        newIfClause.push(BooleanOperator.AND);
        for (int j = 0; j < playConstraints.length; j++) {
          if (newConstraints[j] != null) continue; // Ok as is
          ConstraintPhrase cp = playConstraints[j];
          String proxyName = cp.getProxyName();
          ConstraintPhrase ompConstraint = (ConstraintPhrase) ompModes.get(proxyName);
          OMCRangeList intersection =
            cp.getAllowedValues().intersect(ompConstraint.getAllowedValues());
          if (intersection.isEmpty()) {
            // Completely incompatible. This could be bad. It means
            // that a play designed to work well in some conditions
            // has been completely disallowed by the policy over those
            // conditions. We record the name of the operating mode
            // for which no suitable was available and use the value
            // specified by the policy.
            if (logger.isWarnEnabled()) {
              logger.warn("Policy "
                          + omp
                          + " disallows all settings for "
                          + proxyName
                          + " of play "
                          + play);
            }
            newConstraints[j] = ompConstraint;
          } else {
            // Some values are still ok so use them
            newConstraints[j] =
              new ConstraintPhrase(proxyName, cp.getOperator(), intersection);
          }
        }
        newPlay = new Play(newIfClause, newConstraints);
        newConstrainedPlays.add(newPlay);
      }
    }
    constrainedPlays = (Play[]) newConstrainedPlays.toArray(constrainedPlays);
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    Play[] plays = constrainedPlays;
    for (int i = 0; i < plays.length; i++) {
      buf.append(plays[i]).append(System.getProperty("line.separator"));
    }
    return buf.toString();
  }
}
