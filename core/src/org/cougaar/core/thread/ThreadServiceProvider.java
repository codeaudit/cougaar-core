/*
 * <copyright>
 *  
 *  Copyright 1997-2004 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects
 *  Agency (DARPA).
 * 
 *  You can redistribute this software and/or modify it under the
 *  terms of the Cougaar Open Source License as published on the
 *  Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 *  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 *  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 *  A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 *  OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 *  DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 *  THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *  OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  
 * </copyright>
 */

package org.cougaar.core.thread;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.cougaar.core.component.BindingSite;
import org.cougaar.core.component.Component;
import org.cougaar.core.component.ServiceBroker;
import org.cougaar.core.component.ServiceProvider;
import org.cougaar.core.mts.MessageAddress;
import org.cougaar.core.node.NodeControlService;
import org.cougaar.core.node.NodeIdentificationService;
import org.cougaar.core.service.AgentIdentificationService;
import org.cougaar.core.service.ThreadControlService;
import org.cougaar.core.service.ThreadListenerService;
import org.cougaar.core.service.ThreadService;
import org.cougaar.util.GenericStateModelAdapter;

/**
 * The ServiceProvider for ThreadService and ThreadControlService.
 *
 * @property org.cougaar.thread.scheduler specifies the class of
 * scheduler to use.  The default is PropagatingScheduler.  The only
 * other reasonable choice at the moment would be
 * 'org.cougaa.core.thread.Scheduler'
 */
public final class ThreadServiceProvider 
    extends GenericStateModelAdapter
    implements ServiceProvider, Component
{

    private static final String SCHEDULER_CLASS_PROPERTY = 
	"org.cougaar.thread.scheduler";

    private static final String TRIVIAL_PROPERTY = 
	"org.cougaar.thread.trivial";

    private static ThreadPool[] Pools;
    private static int[] Lane_Sizes = new int[ThreadService.LANE_COUNT];

    private static synchronized void makePools() 
    {
	if (Pools != null) return;

	Pools = new ThreadPool[ThreadService.LANE_COUNT];
	int initializationCount = 10; // could be a param
	for (int i=0; i<Pools.length; i++)
	    Pools[i] = new ThreadPool(Lane_Sizes[i], initializationCount,
				      "Pool-"+i);
    }

    private ServiceBroker my_sb;
    private boolean isRoot;
    private ThreadListenerProxy listenerProxy;
    private ThreadControlService controlProxy;
    private ThreadServiceProxy proxy;
    private ThreadStatusService statusProxy;
    private String name;
    private int laneCount = ThreadService.LANE_COUNT;

    public ThreadServiceProvider() 
    {
    }

    public void load() 
    {
	super.load();
	
	makePools();

	ServiceBroker sb = my_sb;
	isRoot = !sb.hasService(ThreadService.class);

	if (Boolean.getBoolean(TRIVIAL_PROPERTY)) {
	    if (isRoot)	new TrivialThreadServiceProvider().makeServices(sb);
	    return;
	}

	// check if this component was added with parameters
        if (name == null) {
	    // Make default values from position in containment hierarcy
	    AgentIdentificationService ais = (AgentIdentificationService)
		sb.getService(this, AgentIdentificationService.class, null);
	    MessageAddress agentAddr = ais.getMessageAddress();
	    sb.releaseService(this, AgentIdentificationService.class, ais);
	    
	    NodeIdentificationService nis = (NodeIdentificationService)
		sb.getService(this, NodeIdentificationService.class, null);
	    MessageAddress nodeAddr = nis.getMessageAddress();
	    sb.releaseService(this, NodeIdentificationService.class, nis);

	    name = 
		isRoot ?
		"Node "+nodeAddr :
		"Agent_"+agentAddr;
        }

	if (isRoot) {
	    // use the root service broker
	    NodeControlService ncs = (NodeControlService)
		my_sb.getService(this, NodeControlService.class, null);
	    sb = ncs.getRootServiceBroker();

	}
	
	ThreadService parent = (ThreadService) 
	    sb.getService(this, ThreadService.class, null);
	final TreeNode node = makeProxies(parent);
	provideServices(sb);
	if (isRoot) {
	    statusProxy = new ThreadStatusService() {
		    public List getStatus() {
			List result = new ArrayList();
			node.listQueuedThreads(result);	
			node.listRunningThreads(result);
			return result;
		    }
		};
	    sb.addService(ThreadStatusService.class, this);
	}
    }

    private void setParameterFromString(String property) 
    {
	int sepr = property.indexOf('=');
	if (sepr < 0) return;
	String key = property.substring(0, sepr);
	String value = property.substring(++sepr);
	int lane_index, lane_max;

	if (key.equals("name")) {
	    name = value;
	} else if (key.equals("isRoot")) {
	    isRoot = value.equalsIgnoreCase("true");
	} else if (key.equals("BestEffortAbsCapacity")) {
	    lane_index = ThreadService.BEST_EFFORT_LANE;
	    lane_max = Integer.parseInt(value);
	    Lane_Sizes[lane_index] = lane_max;
	} else if (key.equals("WillBlockAbsCapacity")) {
	    lane_index = ThreadService.WILL_BLOCK_LANE;
	    lane_max = Integer.parseInt(value);
	    Lane_Sizes[lane_index] = lane_max;
	} else if (key.equals("CpuIntenseAbsCapacity")) {
	    lane_index = ThreadService.CPU_INTENSE_LANE;
	    lane_max = Integer.parseInt(value);
	    Lane_Sizes[lane_index] = lane_max;
	} else if (key.equals("WellBehavedAbsCapacity")) {
	    lane_index = ThreadService.WELL_BEHAVED_LANE;
	    lane_max = Integer.parseInt(value);
	    Lane_Sizes[lane_index] = lane_max;
	} // add more later
    }

    public void setParameter(Object param) 
    {
	if (param instanceof List) {
	    Iterator itr = ((List) param).iterator();
	    while(itr.hasNext()) {
		setParameterFromString((String) itr.next());
	    }
	} else if (param instanceof String) {
	    setParameterFromString((String) param);
	}
    }

    private Scheduler makeScheduler(Constructor constructor, 
				    Object[] args,
				    int lane)
				   
    {
	Scheduler scheduler = null;
	if (constructor != null) {
	    try {
		return (Scheduler) constructor.newInstance(args);
	    } catch (Exception ex) {
		ex.printStackTrace();
	    }
	}
	
	if (scheduler == null)
	    scheduler = new PropagatingScheduler(listenerProxy);

	scheduler.setLane(lane);
	scheduler.setAbsoluteMax(Lane_Sizes[lane]);
	return scheduler;
    }

    private TreeNode makeProxies(ThreadService parent) 
    {
	listenerProxy = new ThreadListenerProxy(laneCount);

	Class[] formals = { ThreadListenerProxy.class};
	Object[] actuals = { listenerProxy };
	String classname = System.getProperty(SCHEDULER_CLASS_PROPERTY);
	Constructor constructor = null;
	if (classname != null) {
	    try {
		Class s_class = Class.forName(classname);
		constructor = s_class.getConstructor(formals);
	    } catch (Exception ex) {
		ex.printStackTrace();
	    }
	}
	Scheduler[] schedulers = new Scheduler[laneCount];
	for (int i=0; i<schedulers.length; i++) {
	    schedulers[i] = makeScheduler(constructor, actuals, i);
	}
	

	ThreadServiceProxy parentProxy = (ThreadServiceProxy) parent;
	TreeNode node = new TreeNode(schedulers, Pools, name, parentProxy);
	proxy = new ThreadServiceProxy(node);
	controlProxy = new ThreadControlServiceProxy(node);
	listenerProxy.setTreeNode(node);
	return node;
    }

    private void provideServices(ServiceBroker sb) 
    {
	sb.addService(ThreadService.class, this);
	sb.addService(ThreadControlService.class, this);
	sb.addService(ThreadListenerService.class, this);
    }


    public Object getService(ServiceBroker sb, 
			     Object requestor, 
			     Class serviceClass) 
    {
	if (serviceClass == ThreadService.class) {
	    return proxy;
	} else if (serviceClass == ThreadControlService.class) {
	    // Later this will be tightly restricted
	    return controlProxy;
	} else if (serviceClass == ThreadListenerService.class) {
	    return listenerProxy;
	} else if (serviceClass == ThreadStatusService.class) {
	    return statusProxy;
	} else {
	    return null;
	}
    }

    public void releaseService(ServiceBroker sb, 
			       Object requestor, 
			       Class serviceClass, 
			       Object service)
    {
    }

 
    public final void setBindingSite(BindingSite bs) 
    {
	my_sb = bs.getServiceBroker();
    }

}

