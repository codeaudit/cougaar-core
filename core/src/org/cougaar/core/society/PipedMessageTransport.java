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

import java.util.*;

import org.cougaar.core.society.Message;
import org.cougaar.core.society.MessageAddress;
import org.cougaar.core.society.MulticastMessageAddress;
import org.cougaar.core.society.MessageTransportServer;
import org.cougaar.core.society.MessageTransportClient;
import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.core.cluster.ClusterMessage;
import org.cougaar.domain.planning.ldm.plan.Notification;
import org.cougaar.domain.planning.ldm.plan.Directive;
import org.cougaar.core.cluster.DirectiveMessage;
import org.cougaar.core.cluster.ClusterServesClusterManagement;
import org.cougaar.core.cluster.RemoteClusterMetrics;

import org.cougaar.core.cluster.ClusterContext;
import java.io.*;

import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import org.cougaar.util.PipedInputStream;
import org.cougaar.util.PipedOutputStream;

import org.cougaar.util.ReusableThreadPool;
import org.cougaar.util.ReusableThread;
import org.cougaar.domain.planning.ldm.plan.*;

/**
 * A single-vm MessageTransport which sends all messages through a single 
 * serialization pipe, useful for debugging serialization code.
 *
 * Additional Logging points are:
 *   reQ	resubmitted message - means that the target doesn't yet exist.
 *
 * Additional System Properties:
 * org.cougaar.message.useDirectDelivery=false : messages are queued, but not serialized.
 * org.cougaar.message.useLocalDelivery=true : if true, cluster->self messages are short-circuited.
 *
 * Note: since all messages are local (direct calls), no messages are secured.
 **/

public class PipedMessageTransport extends MessageTransportClassic
{
    private static boolean useDirectDelivery = false;
    private static boolean useByteCounter = true;
    private static boolean useLocalDelivery = false;

    private ArrayList deadletters = new ArrayList();

    static {
	Properties props = System.getProperties();
	useDirectDelivery = (Boolean.valueOf(props.getProperty("org.cougaar.message.useDirectDelivery",
							       "false"))).booleanValue();
	useLocalDelivery = (Boolean.valueOf(props.getProperty("org.cougaar.message.useLocalDelivery",
							      "true"))).booleanValue();
	useByteCounter = (Boolean.valueOf(props.getProperty("org.cougaar.message.useByteCounter",
							    "false"))).booleanValue();

    }

    public PipedMessageTransport(String identifier) {
	super();
    }

    public boolean addressKnown(MessageAddress a) {
	return (findLocalClient(a) != null) ;
    }

    /**
     * Local clients are not accessible in this way in the new
     * component model.  But since we're not planning to use this
     * transport anyway, just put a dummy method in to keep the
     * compiler happy for now.
     */
    private MessageTransportClient findLocalClient(MessageAddress addr) {
	return null; 
    }

    /** send a message to the society by shoving through a pipe.
     * requeue anything that cannot be sent right away.
     * This is horribly inefficient, since it'll effectively busywait for 
     * clients to appear.  A better implementation would add messages to
     * a deadletter bin to be revisited each time a new client is registered.
     **/
    protected void sendMessageToSociety(Message m) {
	MessageAddress address = m.getTarget();

	if (address instanceof MulticastMessageAddress) {
	    // if we're multicasting, let the final step handle it, since
	    // all our clients are, by definition, local with this
	    // transport
	    pipeMessage(m);
	} else {
	    MessageTransportClient dest = findLocalClient(address);
	    if (dest == null) {
		//System.err.print("^");
		if (isLogging) log("deadQ", m.toString());
		deadletters.add(m);
	    } else {
		pipeMessage(m);
	    }
	}
    }

    public void registerClient(MessageTransportClient client) {
	// search the deadLetter box for messages to this client
	MessageAddress ca = client.getMessageAddress();
	for (Iterator it = deadletters.iterator(); it.hasNext();) {
	    Message m = (Message) it.next();
	    if (ca.equals(m.getTarget())) {
		//System.err.print("v");
		routeMessage(m);
		if (isLogging) log("ReQ", m.toString());
		it.remove();
	    }
	}
    }

    /** direct delivery or serialize? **/
    private void pipeMessage(Message m) {
	serialize(m);
    }
    

    /** Start the dispatch threads and connect to the FDS registry
     **/
    public void start() {
	assurePipe();    // start serialization pipe
	super.start();

    }

    public NameServer getNameServer() {
	return new NameServer() {
		public Object get(Object name) { return null;}
		public Object put(Object name, Object o) { return null;}
		public void clear() {}
		public boolean containsKey(Object key) { return false;}
		public boolean containsValue(Object value) {return false;}
		public Set entrySet() {return null; }
		public Set entrySet(Object directory) { return null;}
		public boolean isEmpty() { return true; }
		public boolean isEmpty(Object directory) {return true; }
		public Set keySet() {return null;}
		public Set keySet(Object directory) {return null;}
		public void putAll(Map t) { }
		public Object remove(Object name) {return null;}
		public int size() { return 0;}
		public int size(Object directory) {return 0;}
		public Collection values() {return null;}
		public Collection values(Object directory) {return null;}
	    };
    }

    static class MessageQueue {
	private Vector q = new Vector();
	public synchronized void add(Message m) { 
	    q.addElement(m);
	    this.notify();             // new stuff
	}
	public synchronized Vector flush() {
	    //System.err.println("Called MQ.flush()");
	    if (q.size() == 0) {
		try {
		    //System.err.println("Waiting for messages...");
		    this.wait();           // wait for stuff
		    //System.err.println("waked for messages");
		} catch (Exception e) {
		    e.printStackTrace();
		}
	    }
	    // swap vectors and return the old one
	    Vector n = q;
	    q = new Vector();
	    return n;
	}
    }

    private final MessageQueue messageQueue = new MessageQueue();

    class PipeReceiver implements Runnable {
	private PipedInputStream pipeIn;
	private ObjectInputStream in;

	public PipeReceiver(PipedInputStream pin) {
	    pipeIn = pin;
	}
	public void run() {
	    try {
		in = new ObjectInputStream(new BufferedInputStream(pipeIn));
	    } catch (Exception ex) {
		System.err.println("Couldn't open ObjectInputStream:"+ex);
		ex.printStackTrace();
	    }

	    while(true) {
		//checkpipe();
		try {
		    Object o = null;
		    o = in.readObject();
		    if (o instanceof Message) {
			Message m = (Message) o;
			// deliver via inq
			rerouteMessage(m);
		    } else {
			throw new RuntimeException("PipedMessageTransport serialization stream corrupted!");
		    }
		} catch (Exception e) {
		    System.err.println("Message delivery failure: "+e);
		    e.printStackTrace();
		}
		System.err.flush();
	    }
	}
    }

    private int counter = 0;

    class PipeSender implements Runnable {
	private PipedOutputStream pipeOut;
	private ObjectOutputStream out;

	public PipeSender(PipedOutputStream pout) {
	    pipeOut = pout;
	    try {
		out = new ObjectOutputStream(new BufferedOutputStream(pipeOut));
		//out.flush();
	    } catch (Exception e) {
		System.err.println("Couldn't open Output Stream: "+e);
	    }
	}

	public void run() {
	    while(true) {
		//checkpipe();
		try {
		    Vector mq = messageQueue.flush(); // will block if nothing there
		    if (mq != null && mq.size() > 0) {
			Enumeration ms = mq.elements();
			while (ms.hasMoreElements()) {
			    Object o = ms.nextElement();
			    if (o instanceof Message) {
				out.writeObject(o);
				out.flush();
				if (useByteCounter)
				    collectStatistics((CountingOutputStream) pipeOut, o);
			    }
			}
		    }
		} catch (Exception e) {
		    System.err.println("Problem Sending queued messages: "+e);          
		}
	    }
	}
    }

    private Thread sender = null;
    private Thread receiver = null;

    public void checkpipe() {
	//System.err.println("!");
	if (sender != null && !sender.isAlive())
	    System.err.println("Sender died.");
	if (receiver != null && !receiver.isAlive())
	    System.err.println("Receiver died.");
    }
    private void assurePipe() {
	try {
	    PipedOutputStream pipeOut;

	    if (useByteCounter) 
		pipeOut = new CountingOutputStream();
	    else
		pipeOut = new PipedOutputStream();

	    PipedInputStream pipeIn = new PipedInputStream();
	    pipeOut.connect(pipeIn);
	    sender = new Thread(new PipeSender(pipeOut), "PipeSender");
	    sender.start();
	    receiver = new Thread(new PipeReceiver(pipeIn), "PipeReceiver");
	    receiver.start();

	    //checkpipe();

	} catch (Exception e) {
	    System.err.println("Failure to start Serialization Pipe:"+e);
	    e.printStackTrace();
	    System.exit(1);
	}
    }

    private synchronized void serialize(Message m)  {
	messageQueue.add(m);      // may block and wait for send.
    }

    //////////////
    // Support for useByteCounter boolean
    //////////////

    private class CountingOutputStream extends PipedOutputStream {
	private long count = 0;
	public long getCount() { return count; }
	public void resetCount() { count = 0; }
	public CountingOutputStream() { 
	    super(); 
	}
	public void write(int b)  throws IOException {
	    super.write(b);
	    count++;
	}
	public void write(byte b[], int off, int len) throws IOException {
	    super.write(b, off, len);
	    count+=len;
	}
	public void close()  throws IOException {
	    Thread.dumpStack();
	    super.close();
	}

    }

    private class HistEntry {
	private String name;
	private long max = 0;
	private long min = Long.MAX_VALUE;
	private long total = 0;
	private long n = 0;
	public synchronized void note(long x) {
	    n++;
	    total+=x;
	    if (x < min) min = x;
	    if (x > max) max = x;
	}
	public String toString() {
	    double mean = ((double) total)/n;
	    return name+" #"+n+" mean="+mean+" range="+min+"-"+max;
	}
	public HistEntry(String s) {
	    name = s;
	}

	public long getN() { return n; }
    }

    private HistEntry allHE = new HistEntry("All");
    private HistEntry dmHE = new HistEntry("DirectiveMessages");
    private HistEntry oHE = new HistEntry("Other");

    void collectStatistics(CountingOutputStream s, Object o) {
	long count = s.getCount();
	s.resetCount();

	allHE.note(count);
	if (o instanceof DirectiveMessage) 
	    dmHE.note(count);
	else 
	    oHE.note(count);

	long n = allHE.getN();
	if (n % 10 == 0) {
	    synchronized (System.err) {
		System.err.println("Message Statistics: ");
		System.err.println("\t"+allHE);
		System.err.println("\t"+dmHE);
		System.err.println("\t"+oHE);
	    }
	}
    }
}
