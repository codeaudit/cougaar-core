package org.cougaar.core.adaptivity;
import org.cougaar.core.plugin.PluginBase;

/** 
 * The role of a Policy Manager is to
 * - Subscribe to particular policy types
 * - Expand them to other lower-level policies
 * - Where appropriate, direct these sub-policies to other components
 * - Manage the conflicts among policies
 * - Manage the enforcement of policy goals
 *
 * The product of expansion could be new policies or PolicyConstraints.
 * 
 */

interface PolicyManager extends PluginBase {

}




