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



import java.io.*;
import java.util.Date;

/** A vacuous MessageSecurityManager.  DMSM looks like a MSM, but
 * doesn't actually add any real security at all.  Instead, it 
 * merely wraps each "secure" message inside another message for transmission.
 *
 * For debugging use, it prints '{' for each message "encoded" and
 * '}' for each message decoded.
 **/

public class DummyMessageSecurityManager implements MessageSecurityManager 
{
    public Message secureMessage(Message m) {
	System.err.print("{");
	return new DummySecureMessage(m);
    }

    public Message unsecureMessage(SecureMessage m) {
	if (m instanceof DummySecureMessage) {
	    System.err.print("}");
	    return ((DummySecureMessage)m).getMessage();
	} else {
	    return null;
	}
    }

 
    private static class DummySecureMessage 
	extends Message implements SecureMessage 
    {
	private Message message;
	Message getMessage() { return message; }
	DummySecureMessage(Message m) {
	    message=m;
	}
    }

}
