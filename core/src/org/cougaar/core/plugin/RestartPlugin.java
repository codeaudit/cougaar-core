/*
 * <copyright>
 *  Copyright 2000-2002 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */

package org.cougaar.core.plugin;

import java.util.*;

//import org.cougaar.core.plugin.*;
import java.io.*;

import org.cougaar.tools.server.*;
import org.cougaar.tools.server.system.ProcessStatus;

/*
 * Starts up an empty node on a host from a passed-in directive. 
 */

public class RestartPlugin extends ComponentPlugin {
  
  long killMillis = 20*1000;
  
  String hostName = "localhost";
  int controlPort = 8484;
  String nodeName = "MiniNode";
  String namingAddr = hostName+":8888";
  
  String procId = nodeName;
  
  public void setupSubscriptions () {
    System.out.println(
		       RestartPlugin.class.getName()+" {"+
		       "\n  killMillis: "+killMillis+
		       "\n  hostName: "+hostName+
		       "\n  controlPort: "+controlPort+
		       "\n  nodeName: "+nodeName+
		       "\n  namingAddr: "+namingAddr+
		       "\n  procId: "+procId+
		       "\n}");
    
  }
  
  /**
   * Called every time this component is scheduled to run.
   */
  protected void execute() {
    
    try { 
    // create a remote-host-registry instance
    RemoteHostRegistry hostReg = 
      RemoteHostRegistry.getInstance();
    
    // contact the host
    RemoteHost rhost = 
      hostReg.lookupRemoteHost(hostName, controlPort, true);

    // just to test, let's try a "ping()"
    ping(rhost);

    // list the running processes on the server
    listProcs(rhost);

    // define the process
    ProcessDescription desc = 
      createProcessDescription(procId, nodeName, namingAddr);

    // launch process
    RemoteProcess proc = createProcess(rhost, desc);

    // wait a couple seconds
    Thread.sleep(2*1000);

    // list the processes on the server
    listProcs(rhost);

    // let it run for a while
    System.out.println(
        "Created process \""+procId+"\", run "+
        ((killMillis <= 0) ? 
         "forever" : 
         "for "+killMillis+"milliseconds")+
        "\n"+
        "***************************************************");
    // list running processes ("ps")
    //listProcessStatus(proc, false);

    if (killMillis > 0) {
      Thread.sleep(killMillis);

      // kill the process
      System.out.println(
          "****************************************************\n"+
          "Kill process \""+procId+"\"");
      rhost.killRemoteProcess(desc.getName());

      // list the processes on the server
      listProcs(rhost);

      Thread.sleep(2*1000);

      // re-launch process
      System.out.println(
          "****************************************************\n"+
          "Re-launch process \""+procId+"\"");
      RemoteProcess proc2 = createProcess(rhost, desc);

      System.out.println(
          "****************************************************\n"+
          "Run for 5 seconds");
      Thread.sleep(5*1000);

      System.out.println("exit, force server-side cleanup.");

      // force exit -- RMI-threads would keep this running
      System.exit(0);
    }
    }catch(Exception e){
      System.out.println("Caught rmi exception" + e);
    }
  }

   private static void ping(
      RemoteHost rhost) throws Exception {
    long t1 = System.currentTimeMillis();
    long t2 = rhost.ping();
    long t3 = System.currentTimeMillis();
    System.out.println(
        "Ping (millis): "+
        "\n  sent: "+t1+
        "\n  ret:  "+t2+
        "\n  recv: "+t3+
        "\n  (recv - sent): "+(t3 - t1));
  }

  private static void listProcs(
      RemoteHost rhost) throws Exception {
    // list the running processes on the server
    List runningProcs = 
      rhost.listProcessDescriptions();
    int nRunningProcs = 
      ((runningProcs != null) ? runningProcs.size() : 0);
    System.out.println(
        "Running processes["+nRunningProcs+"]: ");
    for (int i = 0; i < nRunningProcs; i++) {
      ProcessDescription pdi = 
        (ProcessDescription) runningProcs.get(i);
      System.out.println(
          "  ["+i+" / "+nRunningProcs+"]: "+pdi);
    }
  }

  private static ProcessDescription createProcessDescription(
      String procId,
      String nodeName,
      String namingAddr) throws Exception {
    // build a configuration
    Properties javaProps = new Properties();
    javaProps.put("org.cougaar.node.name", nodeName);
    javaProps.put("org.cougaar.name.server", namingAddr);
    return
      new ProcessDescription(
          procId,
          "group-for-"+procId,
          javaProps,
          null);
  }

  private static RemoteProcess createProcess(
      RemoteHost rhost,
      final ProcessDescription desc) throws Exception {
    // create an output listener
    OutputListener ol = 
      new OutputListener() {
        public void handleOutputBundle(
            OutputBundle ob) {
          // just write std-out and std-err
          DualStreamBuffer dsb = ob.getDualStreamBuffer();
          try {
            dsb.writeTo(System.out, System.err);
          } catch (IOException ioe) {
            ioe.printStackTrace();
          }
        }
      };

    // create the output filter/buffer config
    OutputPolicy op = new OutputPolicy(20);

    // create a listener config
    RemoteListenableConfig rlc = 
      new RemoteListenableConfig(ol, op);

    // create the node
    try {
      return rhost.createRemoteProcess(desc, rlc);
    } catch (Exception e) {
      System.err.println("Unable to create process:");
      e.printStackTrace();
      throw e;
    }
  }

  private static void listProcessStatus(
      RemoteProcess proc,
      boolean showAll) throws Exception {
    // query
    ProcessStatus[] psa = proc.listProcesses(showAll);
    // display
    int n = ((psa != null) ? psa.length : 0);
    System.out.println(
        ((showAll) ? "All" : "Process's")+
        " ProcessStatus["+n+"] for Process \""+
        proc.getProcessDescription().getName()+"\":");
    for (int i = 0; i < n; i++) {
      ProcessStatus pi = psa[i];
      // show terse details
      System.out.println("  "+pi.toString(false));
    }
  }

}
