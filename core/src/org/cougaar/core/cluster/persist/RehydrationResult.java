/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
/**
 * The public interface for persistence
 */
package org.cougaar.core.cluster.persist;

import java.util.List;
import org.cougaar.core.cluster.MessageManager;

/**
 * Rehydration has two results to return, the little class simply
 * bundles them together.
 **/
public class RehydrationResult {
    public List undistributedEnvelopes;
    public MessageManager messageManager;
}
