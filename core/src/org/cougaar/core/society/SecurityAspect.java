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

import org.cougaar.core.society.rmi.MT;

import java.beans.Beans;

/**
 * First attempt at a security aspect.  The message is secured by a
 * RemoteProxy aspect delegate.  It should probably be unsecured by a
 * RemoteImpl aspect delegate, but this turns out to be tricky, since
 * the class of that object is (for now) unknown. Instead, unsecure as
 * it's about to added to the ReceiveQueue.
 * */
public class SecurityAspect implements MessageTransportAspect
{
    private static MessageSecurityManager msm = null; 

    private static synchronized MessageSecurityManager ensure_msm() {
	if (msm != null) return msm;

	String name = System.getProperty("org.cougaar.message.security");
	if (name != null && (!name.equals("")) &&(!name.equals("none"))) {
	    try {
		// Object raw = Beans.instantiate(null, name);
		Object raw = Class.forName(name).newInstance();
		System.out.println("====== Beans returned object of class " +
				   raw.getClass());
		msm = (MessageSecurityManager) raw;
	    } catch (Exception ex) {
		ex.printStackTrace();
	    }
	}
	return msm;
    }



    private boolean enabled = false;

    public SecurityAspect() {
	enabled = ensure_msm() != null;
    }

    public boolean isEnabled() {
	return enabled;
    }

    // Temporarily package access, rather than private, until we get
    // rid of MessageTransportClassic
    Message secure(Message message) {
	if (msm != null) {
	    System.out.println("Securing message " + message);
	    return msm.secureMessage(message);
	} else {
	    return message;
	}
    }

    // Temporarily package access, rather than private, until we get
    // rid of MessageTransportClassic
    Message unsecure(Message message) {
	if (!(message instanceof SecureMessage)) return message;


	if (msm == null) {
	    System.err.println("MessageTransport "+this+
			       " received SecureMessage "+message+
			       " but has no MessageSecurityManager.");
	    return null;
	} else {
	    System.out.println("Unsecuring message " + message);
	    Message msg = msm.unsecureMessage((SecureMessage) message);
	    if (msg == null) {
		System.err.println("MessageTransport "+this+
				   " received an unverifiable SecureMessage "
				   +message);
	    }
	    return msg;
	}
    }


    public Object getDelegate(Object delegate, int cutpoint) {
	switch (cutpoint) {
	case RemoteProxy:
	    return new SecureProxy((MT) delegate);

	case ReceiveQueue:
	    return new SecureReceiver((ReceiveQueue) delegate);

	default:
	    return null;
	}
    }
    


    private class SecureProxy implements MT {
	private MT rmi_proxy;

	private SecureProxy(MT rmi_proxy) {
	    this.rmi_proxy = rmi_proxy;
	}

	public void rerouteMessage(Message m) 
	    throws java.rmi.RemoteException
	{
	    rmi_proxy.rerouteMessage(secure(m));
	}

	public MessageAddress getMessageAddress() 
	    throws java.rmi.RemoteException
	{
	    return rmi_proxy.getMessageAddress();
	}

    }



    private class SecureReceiver implements ReceiveQueue {
	private ReceiveQueue queue;

	private SecureReceiver(ReceiveQueue queue) {
	    this.queue = queue;
	}

	public void deliverMessage(Message message) {
	    queue.deliverMessage(unsecure(message));
	}

	public boolean matches (String name) {
	    return queue.matches(name);
	}
    }



}
