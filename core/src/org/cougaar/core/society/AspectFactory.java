package org.cougaar.core.society;

import java.util.ArrayList;
import java.util.Iterator;
import java.lang.reflect.Constructor;

abstract public class AspectFactory
{
    private static final boolean debug =
	Boolean.getBoolean("org.cougaar.core.society.transport.DebugTransport");
    private ArrayList aspects;

    protected AspectFactory(ArrayList aspects) {
	this.aspects = aspects;
    }

    public Object attachAspects(Object delegate, Class iface) {
	if (aspects != null) {
	    Iterator itr = aspects.iterator();
	    while (itr.hasNext()) {
		MessageTransportAspect aspect = 
		    (MessageTransportAspect) itr.next();
		Object candidate = aspect.getDelegate(delegate, iface);
		if (candidate != null) delegate = candidate;
		if (debug) System.out.println("======> " + delegate);
	    }
	}
	return delegate;
    }

}
