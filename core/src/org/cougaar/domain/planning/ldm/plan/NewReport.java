/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm.plan;

import java.util.Date;

/** NewReport interface
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: NewReport.java,v 1.2 2001-04-05 19:27:17 mthome Exp $
 **/

public interface NewReport extends Report
{

  /**
   * setText - Set report text
   *
   * @param String reportText report text
   **/
  void setText(String reportText);

  /**
   * setDate - Set creation date for the object
   *
   * @param Date creation date for the object
   **/
  void setDate(Date reportDate);

}
