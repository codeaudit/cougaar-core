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

package org.cougaar.core.cluster;

import java.io.IOException;
import java.io.NotActiveException;
import java.io.ObjectInputStream;
import java.util.Properties;
import org.cougaar.util.PropertyParser;

/** 
 * Keep track of who is Disposing of me. <p>
 * Extends PublishableAdapter so that subclasses may
 * implicitly collect changes.
 * @property org.cougaar.core.cluster.claim.isLoud If set to true, will do additional 
 * checking for Claim conflicts.
 **/

public class ClaimableImpl 
  extends PublishableAdapter    // sigh.
  implements Claimable 
{
  private transient Object claimer = null;
  
  private transient Throwable claimerStack = null; // @debug

  private static final Object postRehydrationClaimer = new Object();

  public final boolean isClaimed() { return (claimer!=null); }

  public final Object getClaim() { return claimer; }

  public final String getClaimClassName() { 
    // for beanInfo use
    return ((claimer != null) ? claimer.getClass().getName() : "null"); 
  }

  public final void setClaim(Object pch) {
    doClaim(pch, pch, "setClaim", " to ");
  }

  public final synchronized boolean tryClaim(Object pch) {
    if (claimer == null) {
      // got the claim
      _claim(pch);
      return true;
    } else if (pch == claimer) {
      // already owned the claim - probably bogus, but...
      return true;              
    } else {
      return false;
    }
  }

  public final void resetClaim(Object pch) {
    doClaim(pch, null, "resetClaim", " from ");
  }

  private synchronized void doClaim(Object pch, Object newClaimer, String verb, String prep) {
    if (pch instanceof PrivilegedClaimant) {
      // PrivilegedClaimant can do what he wants
    } else if (claimer == postRehydrationClaimer) {
      // Actual claimer lost thru rehydration, allow anything
    } else if (pch == null) {
      // Must have a valid pch
      complain("Tried to " + verb + " of " + this + prep + "null.");
    } else if (pch == claimer) {
      // Current claimer can do what he wants
    } else if (claimer != null) {
      // Already claimed by somebody else
      complain("Tried to " + verb + " of " + this + prep + pch +
               "\n\tbut it was " + claimer + ".");
    }
    _claim(newClaimer);
  }

  // must be calling within a synchronized block
  private void _claim(Object newClaimer) {
    // Always carry out the request even if complaints were issued
    claimer = newClaimer;
    // Remember how we got here for debugging.
    claimerStack = new Throwable();
  }
    

  // warning quieting
  /** true iff we should complain with stack traces every time **/
  private static boolean isLoud = false;
  /** true when we've complained once and told the user how to enable loud mode. 
   * Only used when in non-loud mode.
   **/
  private static boolean hasComplained = false;

  static {
    isLoud = PropertyParser.getBoolean("org.cougaar.core.cluster.claim.isLoud", isLoud);
  }

  private void complain(String complaint) {
    synchronized (System.err) { 
      System.err.println(complaint);
      if (isLoud) {
        System.err.println("Current stack:"); 
        Thread.dumpStack();
        System.err.println("Claimer stack:");
        if (claimerStack != null)
          claimerStack.printStackTrace();
        else
          System.err.println("(Never been claimed)");
      } else {
        if (! hasComplained) {
          System.err.println("(Set system property org.cougaar.core.cluster.claim.isLoud=true for details)");
          hasComplained=true;
        }
      }
    }
  }

  private void readObject(ObjectInputStream is)
    throws NotActiveException, ClassNotFoundException, IOException {
    is.defaultReadObject();
    claimer = postRehydrationClaimer;
  }


}
