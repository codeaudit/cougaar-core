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

import org.cougaar.core.society.MessageSecurityManager;
import org.cougaar.core.society.SecureMessage;
import org.cougaar.core.society.Message;
import org.cougaar.core.society.MessageAddress;
import org.cougaar.core.society.MessageTransportServer;

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

    public void setMessageTransport(MessageTransport mts) {}


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
