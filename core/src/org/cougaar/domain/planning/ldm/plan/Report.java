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

/** Report interface
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: Report.java,v 1.2 2001-04-05 19:27:19 mthome Exp $
 **/

/**
 * Report - Informational message.
 *
 * Any part of the system can generate these.
 */


public interface Report 
{

  /**
   * getText - Informational text
   *
   * @return String informational text
   **/
  String getText();


  /**
   * getDate - Date object was injected into the system
   *
   * @return Date creation date for Report
   **/
  Date getDate();
}




