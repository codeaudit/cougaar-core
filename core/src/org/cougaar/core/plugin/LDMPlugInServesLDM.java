/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.plugin;

import org.cougaar.domain.planning.ldm.asset.Asset;
import java.util.Enumeration;

/**
 *  PlugIn side of LDM-PlugIn interface.  All LDM plugins must implement
 * this marker class.  In addition, they may (also) implement
 * PrototypeProvider and/or PropertyProvider.
 *
 * @see org.cougaar.component.LDMServesPlugIn
 * @author  ALPINE <alpine-software@bbn.com>
 **/

public interface LDMPlugInServesLDM extends PlugInServesCluster {
}
