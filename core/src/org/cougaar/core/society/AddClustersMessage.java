/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.society;

import org.cougaar.util.PropertyTree;

/**
 * Requests that the Node add one or more Clusters.
 * <p>
 * The loading procedure for multiple clusters is:</pre>
 *   1) load empty Clusters
 *   2) load plugins for those Clusters
 *   3) start those Clusters</pre>.
 */
public class AddClustersMessage 
  extends NodeMessage
{
  /** @see #getPropertyTree() */
  private PropertyTree pt;

  /** 
   * no-arg Constructor.
   */
  public AddClustersMessage() {
    super();
  }

  /**
   * The PropertyTree should have a "clusters" entry that maps to
   * a List, where each entry in this "clusters" List should be either:<pre>
   *    1) the name of a Cluster, which matches a local ".ini" file
   * </pre>
   * or<pre>
   *    2) a PropertyTree for a parsed Cluster specification, as defined
   *       in <code>ClusterINIParser.parse(BufferedReader)</code>.
   * </pre>.
   *
   * Additional properties of the top-level PropertyTree may be added 
   * at a future time.
   */
  public PropertyTree getPropertyTree() {
    return pt;
  }

  /**
   * @see #getPropertyTree()
   */
  public void setPropertyTree(PropertyTree pt) {
    this.pt = pt;
  }

  public String toString() {
    return super.toString()+" clusters["+
      ((pt != null) ? pt.size() : 0)+"]";
  }
}
