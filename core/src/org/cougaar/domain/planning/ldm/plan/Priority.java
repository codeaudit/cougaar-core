/*
 * <copyright>
 *  Copyright 1997-2000 Defense Advanced Research Projects
 *  Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 *  Raytheon Systems Company (RSC) Consortium).
 *  This software to be used only in accordance with the
 *  COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.domain.planning.ldm.plan;

/* Constant names for different 'priorities'.  Note that priorities
 * should only be set on tasks created by the same customer.
 * @author  ALPINE <alpine-software@bbn.com>
 * @version $Id: Priority.java,v 1.1 2000-12-15 20:16:44 mthome Exp $
 */
public interface Priority {
  static final byte UNDEFINED = 0;
  
  static final byte VERY_LOW = 1;
    
  static final byte LOW = 2;
    
  static final byte MEDIUM = 3;
    
  static final byte HIGH = 4;
    
  static final byte VERY_HIGH = 5;
  
}