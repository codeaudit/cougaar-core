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

/** SecureMessage is marker class for securing the contents of 
 * a message using cryptography.
 *
 * Note that there is no additional public api for getting at the contents of 
 * the object.  This access is granted only to the MessageSecurityManager 
 * implementation.
 * @see MessageSecurityManager
 **/

public interface SecureMessage
{
}
