/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 *
 *  THIS SOFTWARE IS MODIFIED FOR TESTING QUO_ULTRALLOG INTEGRATION
 */
package org.cougaar.lib.quo.performance;
//package org.cougaar.lib.quo;

import org.cougaar.core.plugin.SimplePlugIn;
import org.cougaar.core.cluster.IncrementalSubscription;
import org.cougaar.domain.planning.ldm.asset.*;
import org.cougaar.domain.planning.ldm.plan.*;
import java.util.*;

/**
 * This COUGAAR PlugIn creates and publishes "CODE" tasks and if allocationResult is a success
 * it keeps producing more task
 * It also reads the PlugIn arguments and may alter MessageSize or slurp CPU
 */
public class ManagerPlugIn extends SimplePlugIn {

    // Two assets to use as direct objects for the CODE tasks
  protected Asset what_to_code;
  protected IncrementalSubscription allocations;   // My allocations
  protected int CPUCONSUME=-1;
  protected int MESSAGESIZE=-1;
  private Date startTime;
  private Task t;
  private double start_month;
    private  int count = 0;
    private long minDelta=0;
  /**
   * parsing the plugIn arguments and setting the values for CPUCONSUME and MESSAGESIZE
   */
  protected void parseParameter(){
    Vector p = getParameters();
    for(int i = 0; i < p.size(); i++){
      String s = (String)p.elementAt(i);
      if (s.indexOf("SLURPFACTOR") != -1){
	s = s.substring(s.indexOf("=")+1, s.length());
	CPUCONSUME = Integer.parseInt(s);
      }
      if (s.indexOf("MESSAGESIZE") != -1){
	s = s.substring(s.indexOf("=")+1, s.length());
	MESSAGESIZE = Integer.parseInt(s);
      }
    }
  }

  /**
   * Using setupSubscriptions to create the initial CODE tasks
   */
  protected void setupSubscriptions() {
    // Create a task to code the next killer app
    //modified to create a lots of tasks if allocation is a success
    parseParameter(); //read the plugIn arguments
    addTask();
    allocations = (IncrementalSubscription)subscribe(new myAllocationPredicate());
  }


  /**
   * This PlugIn has no subscriptions so this method does nothing
   */
  protected void execute () {
    //System.out.println("ManagerPlugIn::execute()");
    allocateChangedtasks(); // Process changed allocations

  }

  protected void addTask() {
    what_to_code = theLDMF.createPrototype("AbstractAsset", "The Next Killer App");
    NewItemIdentificationPG iipg = 
      (NewItemIdentificationPG)theLDMF.createPropertyGroup("ItemIdentificationPG");
    iipg.setItemIdentification("e-somthing java");
    what_to_code.setItemIdentificationPG(iipg);
    publishAdd(what_to_code); //need to add a different task else a exception --so publishChange used
    
    t = makeTask(what_to_code);
    // System.out.println("\nManagerPlugIn::Adding task " + t); 
    
    startTime = new Date();
    // Add a start_time and end_time strict preference
    ScoringFunction scorefcn = ScoringFunction.createStrictlyAtValue
      (new AspectValue(AspectType.START_TIME, start_month));
    Preference pref =
      theLDMF.newPreference(AspectType.START_TIME, scorefcn);
    //preferences.add(pref);
    ((NewTask) t).setPreference(pref);
    publishAdd(t);
  }
    
  /**
   * Create a CODE task.
   * @param what the direct object of the task
   */
  protected Task makeTask(Asset what) {
    NewTask new_task = theLDMF.newTask();
    new_task.setVerb(new Verb("CODE"));// Set the verb as given
    new_task.setPlan(theLDMF.getRealityPlan());// Set the reality plan for the task
    new_task.setDirectObject(what);

    NewPrepositionalPhrase npp = theLDMF.newPrepositionalPhrase();
    npp.setPreposition("USING_LANGUAGE");
    if (MESSAGESIZE == -1)
      npp.setIndirectObject(alterMessageSize(0));
    else
      npp.setIndirectObject(alterMessageSize(MESSAGESIZE));
	new_task.setPrepositionalPhrase(npp);
	return new_task;
    }

    protected void   changeTasks(){
      if(CPUCONSUME != -1)  //i.e. cpuconsume passed to plugin as a arg
	consumeCPU(CPUCONSUME);
      //t = makeTask(what_to_code);
      // Add a start_time and end_time strict preference
      start_month++;
      //System.out.println("\nManagerPlugIn::Changing task " + start_month + " ---- "  + t); 
      ScoringFunction scorefcn = ScoringFunction.createStrictlyAtValue
	(new AspectValue(AspectType.START_TIME, start_month));
      Preference pref =
	theLDMF.newPreference(AspectType.START_TIME, scorefcn);
      //preferences.add(pref);
      ((NewTask) t).setPreference(pref);
       startTime = new Date();
      publishChange(t);
    }

    protected void  allocateChangedtasks(){
      AllocationResult est, rep;
      Enumeration allo_enum = allocations.getChangedList();
      while (allo_enum.hasMoreElements()) {
	Allocation alloc = (Allocation)allo_enum.nextElement() ;
	est=null; rep=null;
	est = alloc.getEstimatedResult();
	rep = alloc.getReportedResult();
	if (rep!=null){
	  Date endTime = new Date();
	  long delta = endTime.getTime() - startTime.getTime();
	  count++;
	  if (count == 1)
	      minDelta = delta;
	  else
	      minDelta = Math.min(minDelta, delta);
	  System.out.println(count+":"+delta+":"+minDelta);
	  //publishRemove(t);
	  //allocations.clear();
	    try {
  	    Thread.sleep(1000);
  	  } catch (InterruptedException e) {
  	    System.out.println(e);
  	  }
	  changeTasks();
	}
      }
    }
    
  /**
   * consume CPU cycles by the argument passed as  parameter
   */
  private void consumeCPU(int cyclesConsumed) {
    //Just using CPU computations    
    int slurp = 0;
    for(int i= 0; i < cyclesConsumed; i++){
      slurp++;
    }
  }
    
  /**
   * Changing message size  by the argument passed as  parameter
   */
  private byte[] alterMessageSize(int val) {
      //Byte b = new Byte (Byte.MAX_VALUE);
      byte[] bytes = new byte[val];
      for(int i = 0; i < val; i++) bytes[i] = 42;
      return bytes;
  }
}




