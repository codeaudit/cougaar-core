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


import java.beans.Beans;        // used to instantiate MSM

import org.cougaar.core.cluster.ClusterServesClusterManagement;

import org.cougaar.core.cluster.ClusterContext;
import java.io.*;

public abstract class MessageTransportClassic extends MessageTransport
{

  /** are we logging? **/
  public static boolean isLogging = false;

  /** are we collecting statistics on msms? **/
  protected static boolean statMSM = false;

  protected static String msmName = null;

  /** holder for our MSM instance, if specified **/ 
  private static MessageSecurityManager msm = null; 

  // initialize static vars from system properties.
  static {
    Properties props = System.getProperties();
    isLogging = (Boolean.valueOf(props.getProperty("org.cougaar.message.isLogging", "false"))).booleanValue();
    msmName = props.getProperty("org.cougaar.message.security");
    // "org.cougaar.core.society.SignedMessageSecurityManager
    statMSM = (Boolean.valueOf(props.getProperty("org.cougaar.message.msmStats", "false"))).booleanValue();
  }

  static double secureTotal = 0;
  static double secureIter = 0;
  static double unsecTotal = 0;
  static double unsecIter = 0;


  protected abstract void sendMessageToSociety(Message m);

    public void routeMessage(Message message) {
	sendMessageToSociety(message);
    }

    

  /** secure a message iff we have a MSM **/
  protected Message secure(Message m) {
    if (msm != null) {
      long t = 0;
      if (statMSM) t = System.currentTimeMillis();
      Message sm = msm.secureMessage(m);
      if (statMSM) {
        t = System.currentTimeMillis()-t;
        if (secureTotal==0.0) System.err.println("First secure = "+t);
        secureTotal = secureTotal+t;
        secureIter++;
      }
      return sm;
    } else {
      return m;
    }
  }
  /** unsecure a message if it needs it **/
  protected Message unsecure(Message m) {
    if (m instanceof SecureMessage) {
      if (msm == null) {
        System.err.println("MessageTransport "+this+" received SecureMessage "+m+
                           " but has no MessageSecurityManager.");
        m = null;
      } else {
        long t =0;
        if (statMSM) t = System.currentTimeMillis();

        Message n = msm.unsecureMessage((SecureMessage) m);
        if (n == null) {
          System.err.println("MessageTransport "+this+" received an unverifiable SecureMessage "+m);
        }
        m = n;

        if (statMSM) {
          t = System.currentTimeMillis()-t;
          if (unsecTotal==0.0) System.err.println("First unsecure = "+t);
          unsecTotal += t;
          unsecIter++;
        }
      }
    }
    return m;
  }


    
  /** Start the dispatch threads and connect to the FDS registry
   **/
  public void start() {
    if (isLogging)  getLog();
    //System.err.println("Started MessageTransport for "+c);
  }



  // logging support
  private PrintWriter logStream = null;

  private PrintWriter getLog() {
    if (logStream == null) {
      try {
	  String id = registry.getIdentifier();
	logStream = new PrintWriter(new FileWriter(id+".cml"), true);
      } catch (Exception e) {
	e.printStackTrace();
	System.err.println("Logging required but not possible - exiting");
	System.exit(1);
      }
    }
    return logStream;
  }

  protected void log(String key, String info) {
      String id = registry.getIdentifier();
      getLog().println(id+"\t"+System.currentTimeMillis()+"\t"+key+"\t"+info);
  }

  //


  private boolean disableRetransmission = false;

  public void setDisableRetransmission(boolean disableRetransmission) {
    this.disableRetransmission = disableRetransmission;
  }

  public boolean isDisableRetransmission() {
    return disableRetransmission;
  }


  //
  // specialized message transport serialization layer
  //

  /**
   * Accumulate total bytes sent using ObjectOutputStream. This
   * variable should be accessed only while synchronized on this
   * MessageTransport object.
   **/
  protected long statisticsTotalBytes = 0L;
  protected long[] messageLengthHistogram = null;

  /** Keep statistics if true **/
  public static boolean keepStatistics =
    (Boolean.valueOf(System.getProperty("org.cougaar.message.keepStatistics", "false"))).booleanValue();

  public class StatisticsStreamWrapper extends OutputStream {
    OutputStream wrapped;
    int byteCount = 0;
    public StatisticsStreamWrapper(OutputStream wrapped) {
      this.wrapped = wrapped;
      if (messageLengthHistogram == null) {
        messageLengthHistogram = new long[MessageStatistics.NBINS];
      }
    }
    public void close() throws IOException {
      wrapped.close();
    }
    public void flush() throws IOException {
      wrapped.flush();
      int bin = 0;
      int maxBin = MessageStatistics.NBINS - 1;
      while (bin < maxBin && byteCount >= MessageStatistics.BIN_SIZES[bin]) {
        bin++;
      }
      messageLengthHistogram[bin]++;
      statisticsTotalBytes += byteCount;
      byteCount = 0;
    }
    public void write(int b) throws IOException {
      wrapped.write(b);
      synchronized (MessageTransportClassic.this) {
        byteCount += 1;
      }
    }
    public void write(byte[] b) throws IOException {
      wrapped.write(b);
      synchronized (MessageTransportClassic.this) {
        byteCount += b.length;
      }
    }
    public void write(byte[] b, int o, int len) throws IOException {
      wrapped.write(b, o, len);
      synchronized (MessageTransportClassic.this) {
        byteCount += len;
      }
    }
  }

  /** Heartbeat to print statistics on 
   **/
  private class Heartbeat implements Runnable {
    public void run() {
      while (true) {
        try {
          Thread.sleep(5*1000); // sleep for (at least) 5s
        } catch (InterruptedException ie) {}

        double secMean = secureTotal;
        if (secureIter>0) secMean/=secureIter;

        double unsecMean = unsecTotal;
        if (unsecIter>0) unsecMean/=unsecIter;

        System.err.println("{"+secMean+" "+unsecMean+"}");
      }
    }
  }

  private static Thread heartbeat = null;
  private static Object heartbeatLock = new Object();

  private void startHeartbeat() {
    synchronized (heartbeatLock) {
      if (heartbeat == null) {
        heartbeat = new Thread(new Heartbeat(), "Heartbeat");
        heartbeat.setPriority(Thread.MIN_PRIORITY);
        heartbeat.start();
      }
    }
  }
}
