/* 
 * <copyright>
 *  Copyright 2001-2002 BBNT Solutions, LLC
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
package org.cougaar.multicast;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.agent.ClusterServesLogicProvider;
import org.cougaar.core.agent.ClusterImpl;
import org.cougaar.core.blackboard.LogPlanLogicProvider;
import org.cougaar.core.blackboard.MessageLogicProvider;
import org.cougaar.core.blackboard.EnvelopeLogicProvider;
import org.cougaar.core.blackboard.LogPlanServesLogicProvider;
import org.cougaar.core.blackboard.EnvelopeTuple;
import org.cougaar.core.blackboard.SubscriberException;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.service.LoggingService;
import org.cougaar.core.util.UniqueObject;
import org.cougaar.planning.ldm.plan.Directive;


/**
 * LP to transfer <code>ABM</code>s between Agents.
 * Part of the <code>ABMDomain</code>. <code>ABM</code>'s 
 * received from remote have their content published, if not already present.
 * Locally sent <code>ABM</code>s, are delivered locally
 * if that is the intent, delivered to the given <code>ClusterIdentifier</code>
 * if the destination is explicit, or are expanded. <code>ABMAddress</code>
 * destinations are expanded via the YellowPages into a list of
 * actual destinations. New <code>ABM</code>s are created for these,
 * and they are sent.  Then the original <code>ABM</code> is logplan.remove()ed 
 */
public class ABMTransportLP extends LogPlanLogicProvider implements MessageLogicProvider, EnvelopeLogicProvider {

  private LoggingService logger;
  private ABMFactory myfact;
  
  /**
   * Constructor obtains factory or reports it is null. 
   *
   * @param logplan a <code>LogPlanServesLogicProvider</code> 
   * @param cluster a <code>ClusterServesLogicProvider</code>
   **/
  public ABMTransportLP(LogPlanServesLogicProvider logplan, ClusterServesLogicProvider cluster) {
    super(logplan, cluster);
    myfact = (ABMFactory)cluster.getFactory("abm");
    if( myfact == null ) {
      System.err.println("ABMTransportLP constructed with null factory at " + cluster.getClusterIdentifier());
    }
    
    logger = (LoggingService)((ClusterImpl)cluster).getServiceBroker().getService(this, LoggingService.class, null);
    if(logger == null)
      System.err.println("Error obtaining LoggingService at " + cluster.getClusterIdentifier() + "'s " + this.getClass());    
  }
  
  /**
   * If it is a Directive of the appropriate type, with a destination, send it remotely
   *
   * @param obj an <code>Object</code> to send remotely
   * @param changes a <code>Collection</code>
   */
  private void examine(Object obj, Collection changes) {    
    if (! (obj instanceof ABM)) return;
    ABM dir = (ABM)obj;
    // get the destination
    MessageAddress destination = dir.getDest();
    if (destination == null) return;
    // Ensure that the destination is not the local cluster
    if (destination.equals(cluster.getClusterIdentifier())) {
      if(logger.isDebugEnabled()) {
	logger.debug("LP at " + cluster.getClusterIdentifier().toString() + " sees a LOCAL:" + dir);
      }
      execute(dir, null);
      return;
    }

    // to be sent out to cluster directly
    if (destination instanceof ClusterIdentifier) {
      if(logger.isDebugEnabled()) {
	logger.debug("LP at " + cluster.getClusterIdentifier().toString() + " sees a ClusterID:" + dir);
      }
      logplan.sendDirective(dir, changes);
      logplan.remove(dir);
      return;
    }

    /**
     * If MessageAddress reflects ABMAddress, obtain
     * a List of interested agents and send out 
     * ABM directives to them. 
     **/    
    if (destination instanceof ABMAddress) {
      if(logger.isDebugEnabled()) {
	logger.debug("LP at " + cluster.getClusterIdentifier().toString() + " sees a ABMAddress:" + dir);
      }

      if( myfact == null ) {
	if(logger.isErrorEnabled()) {
          logger.error("Error getting destinations for ABM with destination ABMAddress - null factory.");
	}
	// dont want to continue with directive
	return;
      }

      // get list of message addresses
      List mas = myfact.getYP().getDestinations((ABMAddress)destination);
      // iterate over those & send out as directives
      if( mas!=null )
        {
          ListIterator iter = mas.listIterator();
          while(iter.hasNext()) 
            {
	      MessageAddress curr = (MessageAddress)iter.next();
	      if(logger.isDebugEnabled()) {
		logger.debug("For directive " + dir  + ", got MessageAddress " + curr);
	      }
	      if(curr != null) {
		// This assumes we get back deliverable
		// addresses. The alternative would be
		// to recurse on this method.
		logplan.sendDirective(myfact.newABM((ABM)dir, curr), changes);
		if(logger.isDebugEnabled()) {
		  logger.debug("LP at " + cluster.getClusterIdentifier().toString() + " sending directive to: " + curr + "\n");
		}
	      }
	      else { 
		if(logger.isErrorEnabled()) {
		  logger.error("Error sending ABM, expanded MessageAddress is null.");
		}
	      }
            } // end of loop over expanded addresses
        } else {
	if (logger.isDebugEnabled()) {
	  logger.debug("YP returning null list of MessageAddrs for directive." + dir);
	}
      }
    } // end of loop to handle ABMAddress destinations

    // What about destinations that are neither
    // a ClusterIdentifier or a ABMAddress? Are there such things?
    // Should I just try to send those?
    // FIXME!

    // Done handling this ABM. Remove it.
    // In future, might do this only if the ABM says is is not persistable,
    // allowing persistable extensions that are not removed.
    logplan.remove(dir);
  }
  
  /**
   * Handle one EnvelopeTuple. Call examine to check for objects that
   * are <code>ABM</code> related and go to a remote Agent.
   *
   * @param dir a <code>EnvelopeTuple</code> to receive
   * @param changes a <code>Collection</code>
   **/
  public void execute(EnvelopeTuple o, Collection changes) {
    Object obj = o.getObject();
    if (o.isAdd()) {
      examine(obj, changes);
    } else if (o.isBulk()) {
      Collection c = (Collection) obj;
      for (Iterator e = c.iterator(); e.hasNext(); ) {
        examine(e.next(), changes);
      }
    }
  }

  /**
   * If the incoming Directive is an appropriate type, add it's 
   * contents to the local logplan. 
   * Only add it if it is not already there.
   *
   * @param dir a <code>Directive</code> to receive
   * @param changes a <code>Collection</code>
   */
  public void execute(Directive dir, Collection changes) {
    // figure out what type it is:
    if (!(dir instanceof ABM)) return;
    
    ContextWrapper content = ((ABM)dir).getContent();
    
    // make sure it isn't already there? Otherwise...
    UniqueObject exists = logplan.findUniqueObject(((UniqueObject)content).getUID());
    if (exists != null && exists.equals(content)) {
      // it already exists in the logplan. for us, do nothing
      if (logger.isDebugEnabled()) {
	logger.debug("LP did not add to logplan in agent " + cluster.getClusterIdentifier() + ": " + content);
      }
      return;
    }
    
    if (logger.isDebugEnabled()) {
      logger.debug("LP at " + cluster.getClusterIdentifier().toString() + " adding to logplan: " + content + "\n");
    }
    
    try {
      logplan.add(content);
    } catch (SubscriberException se) {
      if(logger.isErrorEnabled()) {
        logger.error("Could not add ContextWrapper to logplan: " + content, se);}
    }
  }
  
} // ABMTransportLP.java


