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

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.lang.reflect.Constructor;

import org.cougaar.core.society.rmi.SimpleRMIMessageTransport;
import org.cougaar.core.society.rmi.RMIMessageTransport;

/**
 * A factory which instantiates all MessageTransports.  It will always
 * make at leats two transports: one for local message
 * (LoopbackMessageTransport) and one for remote messages (either an
 * RMIMessageTransport or a SimpleRMIMessageTransport, depending on
 * the value of the property org.cougaar.core.society.UseSimpleRMI.
 * It may also make other transports, one per class, as listed in the
 * property org.cougaar.message.transportClasses.
 *
 * If the property org.cougaar.message.transportClass is set, the
 * transport of that class will always be preferred over any others.  */
public final class MessageTransportFactory 
{
    private ArrayList transports;
    private String id;
    private MessageTransport defaultTransport, loopbackTransport;
    private MessageTransportRegistry registry;
    private ReceiveQueue recvQ;
    private NameSupport nameSupport;

    public MessageTransportFactory(String id, 
				   MessageTransportRegistry registry,
				   NameSupport nameSupport)
    {
	this.id = id;
	this.registry = registry;
	this.nameSupport = nameSupport;
    }

    void setRecvQ(ReceiveQueue recvQ) {
	this.recvQ = recvQ;
    }


    private MessageTransport makeTransport(String classname) {
	// Assume for now all transport classes have a constructor of
	// one argument (the id string).
	Class[] types = { String.class };
	Object[] args = { registry.getIdentifier() };
	MessageTransport transport = null;
	try {
	    Class transport_class = Class.forName(classname);
	    Constructor constructor = 
		transport_class.getConstructor(types);
	    transport = (MessageTransport) constructor.newInstance(args);
	} catch (Exception xxx) {
	    xxx.printStackTrace();
	    return null;
	}
	transport.setRecvQ(recvQ);
	transport.setRegistry(registry);
	transport.setNameSupport(nameSupport);
	transports.add(transport);
	return transport;
    }


    private void makeOtherTransports() {
	String property = "org.cougaar.message.transportClasses";
	String transport_classes = System.getProperty(property);
	if (transport_classes == null) return;

	StringTokenizer tokenizer = 
	    new StringTokenizer(transport_classes, ",");
	while (tokenizer.hasMoreElements()) {
	    String classname = tokenizer.nextToken();
	    makeTransport(classname);
	}
    }

    public  ArrayList getTransports() {
	if (transports != null) return transports;

	transports = new ArrayList();


	String prop = "org.cougaar.message.transportClass";
	String preferredClassname = System.getProperty(prop);
	if (preferredClassname != null) {
	    MessageTransport transport = makeTransport(preferredClassname);
	    if (transport != null) {
		// If there's a preferred transport, never use any
		// others.
		defaultTransport = transport;
		loopbackTransport = transport;
		return transports;
	    }
	}

	// No preferred transport, make all the usual ones.

	loopbackTransport = new LoopbackMessageTransport();
	loopbackTransport.setRecvQ(recvQ);
	loopbackTransport.setRegistry(registry);
	loopbackTransport.setNameSupport(nameSupport);
	transports.add(loopbackTransport);
	
	if (Boolean.getBoolean("org.cougaar.core.society.UseSimpleRMI"))
	    defaultTransport = new SimpleRMIMessageTransport(id);
	else
	    defaultTransport = new RMIMessageTransport(id);
	defaultTransport.setRecvQ(recvQ);
	defaultTransport.setRegistry(registry);
	defaultTransport.setNameSupport(nameSupport);
	transports.add(defaultTransport);


	makeOtherTransports();

	return transports;
    }

    MessageTransport getDefaultTransport() {
	return defaultTransport;
    }

    MessageTransport getLoopbackTransport() {
	return loopbackTransport;
    }



}
