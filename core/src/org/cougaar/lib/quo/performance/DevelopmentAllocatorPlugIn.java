/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
//package org.cougaar.lib.quo.performance;
package org.cougaar.lib.quo.performance;

import org.cougaar.core.cluster.IncrementalSubscription;
import org.cougaar.domain.planning.ldm.plan.*;
import org.cougaar.domain.planning.ldm.asset.Asset;
import org.cougaar.util.UnaryPredicate;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import org.cougaar.lib.quo.performance.assets.*;
import org.cougaar.core.cluster.ChangeReport;
import org.cougaar.core.plugin.Annotation;
/**
 * This COUGAAR PlugIn subscribes to tasks in a workflow and allocates
 * the workflow sub-tasks to programmer assets.
 * @author ALPINE (alpine-software@bbn.com)
 * @version $Id: DevelopmentAllocatorPlugIn.java,v 1.1 2001-08-09 20:56:56 psharma Exp $
 **/
public class DevelopmentAllocatorPlugIn extends org.cougaar.core.plugin.SimplePlugIn
{
  private IncrementalSubscription allCodeTasks;   // Tasks that I'm interested in
  private IncrementalSubscription allProgrammers;  // Programmer assets that I allocate to
  protected int CPUCONSUME=-1;
  protected int MESSAGESIZE=-1;
  protected String MESSAGE = "MESSAGE";
  
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

    static class MyChangeReport implements ChangeReport {
    private byte[] bytes;
    MyChangeReport(byte[] bytes){
	this.bytes = bytes;
    }

}

     
  /**
   * Predicate matching all ProgrammerAssets
   */
  private UnaryPredicate allProgrammersPredicate = new UnaryPredicate() {
      public boolean execute(Object o) {
	return o instanceof ProgrammerAsset;
      }
    };

  /**
   * Predicate that matches all Test tasks
   */
  private UnaryPredicate codeTaskPredicate = new UnaryPredicate() {
      public boolean execute(Object o) {
	if (o instanceof Task)
	  {
	    Task task = (Task)o;
	    return task.getVerb().equals(Verb.getVerb("CODE"));
	  }
	return false;
      }
    };

     
  /**
   * Establish subscription for tasks and assets
   **/
  public void setupSubscriptions() {
    parseParameter();
    allProgrammers =
      (IncrementalSubscription)subscribe(allProgrammersPredicate);
    allCodeTasks =
      (IncrementalSubscription)subscribe(codeTaskPredicate);
  }

  /**
   * Top level plugin execute loop.  Handle changes to my subscriptions.
   **/
  public void execute() {
    //System.out.println("DevelopmentAllocatorPlugIn::execute()");
    // process new and changed tasks
    allocateTasks(allCodeTasks.getAddedList());
    allocateTasks(allCodeTasks.getChangedList());
    //System.out.println("DevelopmentAllocatorPlugIn::execute " + allCodeTasks.size());
  }
  
  private void allocateTasks(Enumeration task_enum) {
    while (task_enum.hasMoreElements()) {
      Task task = (Task)task_enum.nextElement();
      allocateTask(task, startMonth(task));
    }
  }

  /**
   * Extract the start month from a task
   */
  private int startMonth(Task t) {
    return 0;
  }

  /**
   * Find an available ProgrammerAsset for this task.  Task must be scheduled
   * after the month "after"
   */
  private int allocateTask(Task task, int after) {
    if(CPUCONSUME != -1)  //i.e. cpuconsume passed to plugin as a arg
      consumeCPU(CPUCONSUME);
    
    int end = after;
      
    // select an available programmer at random
    Vector programmers = new Vector(allProgrammers.getCollection());
    boolean allocated = false;
    
    while ((!allocated) && (programmers.size() > 0)) {
      int stuckee = (int)Math.floor(Math.random() * programmers.size());
      ProgrammerAsset asset = (ProgrammerAsset)programmers.elementAt(stuckee);
      int duration = 3;  int earliest = 0;
      end = earliest + duration;

      // Create an estimate that reports that we did just what we
      // were asked to do
      int desired_delivery =10; //bogus
      boolean onTime = (end <= desired_delivery);
      int []aspect_types = {AspectType.START_TIME, AspectType.END_TIME, AspectType.DURATION};
      double []results = {earliest, end, duration};
      AllocationResult estAR =  theLDMF.newAllocationResult(1.0, onTime,aspect_types,results);
      
      ChangeReport cr = null;
      if (MESSAGESIZE != -1)
	  cr = new MyChangeReport(alterMessageSize(MESSAGESIZE));
      else 
	  cr = new MyChangeReport(alterMessageSize(0));

      PlanElement pe = task.getPlanElement(); //Allocation  planElement
      if (pe == null) {
	pe = theLDMF.createAllocation(task.getPlan(), task,
				      asset, estAR, Role.ASSIGNED);
	publishAdd(pe);
      } else {
	pe.setEstimatedResult(estAR);
	publishChange(pe, Collections.singleton(cr));
      }
      allocated = true;
    }
    return end;
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










