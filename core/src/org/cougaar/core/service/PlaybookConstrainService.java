package org.cougaar.core.service;

import org.cougaar.core.component.Service;
import org.cougaar.core.adaptivity.OperatingModePolicy;

/**  
 * Part of the PlaybookService which manages plays and playbooks. 
 * This part of the service is used to alter the plays in the playbook
 * by adding, modifying, or removing OperatingMode Policies.
 * 
 *  
 *   Play - if THREATCON > 3 && ENCLAVE == THEATER
 *          then ENCRYPT = {56, 128}  - the first value is preferred
 *
 *   Policy - if THREATCON > 3 && CPU > 90
 *            then ENCRYPT >= 128
 *
 *
 *  Plays formed by calling constrain(Policy)
 *
 *        if (THREATCON > 3 && ENCLAVE == THEATER) 
 *           && (THREATCON >3 && CPU > 90)
 *        then ENCRYPT = {128}
 *
 *        if (THREATCON > 3 && ENCLAVE == THEATER) 
 *           && !(THREATCON >3 && CPU > 90)
 *        then ENCRYPT = {56, 128}
 *
 * This play can constrain this policy because their "then" clauses
 * are operating on the same knobs.
 *
 * Two plays result from constraining the original play with the
 * policy. The "if" clause of the first new play has the "if" clause
 * of the policy tacked on to it, and the knob on the "then" clause
 * must be set to 128. The "if" clause on the second new play has 
 * the -negated- "if" clause of the policy tacked on. The "then"
 * clause is untouched.
 * 
 **/

public interface PlaybookConstrainService extends Service {

  /* constrain and unconstrain called by OperatingModePolicyManager */

  /**
   * might replace one play with two in current playbook 
   * @param OperatingModePolicy may constrain the play
   */
  void constrain(OperatingModePolicy omp);

  /**
   * might replace two plays with one in current playbook 
   * @param OperatingModePolicy 
   */
  void unconstrain(OperatingModePolicy omp);

}

