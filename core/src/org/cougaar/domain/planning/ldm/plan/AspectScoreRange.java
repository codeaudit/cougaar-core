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

/*
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: AspectScoreRange.java,v 1.2 2001-04-05 19:27:13 mthome Exp $
 */
 
public class AspectScoreRange {
  private AspectScorePoint start;
  private AspectScorePoint end;

  public AspectScoreRange(AspectScorePoint start,AspectScorePoint end) {
    this.start=start;
    this.end=end;
  }
  
  /* @return AspectScorePoint The starting point of the range. */
  public AspectScorePoint getRangeStartPoint() {
    return start;
  }
   
  /* @return AspectScorePoint The starting point of the range. */
  public AspectScorePoint getRangeEndPoint() {
    return end;
  }
   
}
