/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

// Stub class generated by rmic, do not edit.
// Contents subject to change without notice.

package org.cougaar.core.society;

public final class NodeControllerImpl_Stub
    extends java.rmi.server.RemoteStub
    implements org.cougaar.core.society.NodeController
{
    private static final long serialVersionUID = 2;
    
    private static java.lang.reflect.Method $method_addClusters_0;
    private static java.lang.reflect.Method $method_getClusterIdentifiers_1;
    private static java.lang.reflect.Method $method_getHostName_2;
    private static java.lang.reflect.Method $method_getNodeIdentifier_3;
    
    static {
	try {
	    $method_addClusters_0 = org.cougaar.core.society.NodeController.class.getMethod("addClusters", new java.lang.Class[] {org.cougaar.util.PropertyTree.class});
	    $method_getClusterIdentifiers_1 = org.cougaar.core.society.NodeController.class.getMethod("getClusterIdentifiers", new java.lang.Class[] {});
	    $method_getHostName_2 = org.cougaar.core.society.NodeController.class.getMethod("getHostName", new java.lang.Class[] {});
	    $method_getNodeIdentifier_3 = org.cougaar.core.society.NodeController.class.getMethod("getNodeIdentifier", new java.lang.Class[] {});
	} catch (java.lang.NoSuchMethodException e) {
	    throw new java.lang.NoSuchMethodError(
		"stub class initialization failed");
	}
    }
    
    // constructors
    public NodeControllerImpl_Stub(java.rmi.server.RemoteRef ref) {
	super(ref);
    }
    
    // methods from remote interfaces
    
    // implementation of addClusters(PropertyTree)
    public void addClusters(org.cougaar.util.PropertyTree $param_PropertyTree_1)
	throws java.rmi.RemoteException
    {
	try {
	    ref.invoke(this, $method_addClusters_0, new java.lang.Object[] {$param_PropertyTree_1}, -1381132250102308575L);
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (java.rmi.RemoteException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
    
    // implementation of getClusterIdentifiers()
    public java.util.List getClusterIdentifiers()
	throws java.rmi.RemoteException
    {
	try {
	    Object $result = ref.invoke(this, $method_getClusterIdentifiers_1, null, 6662145639362458000L);
	    return ((java.util.List) $result);
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (java.rmi.RemoteException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
    
    // implementation of getHostName()
    public java.lang.String getHostName()
	throws java.rmi.RemoteException
    {
	try {
	    Object $result = ref.invoke(this, $method_getHostName_2, null, -8858142821358724604L);
	    return ((java.lang.String) $result);
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (java.rmi.RemoteException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
    
    // implementation of getNodeIdentifier()
    public org.cougaar.core.society.NodeIdentifier getNodeIdentifier()
	throws java.rmi.RemoteException
    {
	try {
	    Object $result = ref.invoke(this, $method_getNodeIdentifier_3, null, -4094756630766336033L);
	    return ((org.cougaar.core.society.NodeIdentifier) $result);
	} catch (java.lang.RuntimeException e) {
	    throw e;
	} catch (java.rmi.RemoteException e) {
	    throw e;
	} catch (java.lang.Exception e) {
	    throw new java.rmi.UnexpectedException("undeclared checked exception", e);
	}
    }
}
