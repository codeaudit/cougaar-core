/*
 * <copyright>
 * Copyright 2000-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.component;

import java.util.*;
import org.cougaar.util.GenericStateModelAdapter;

/** A basic implementation of a Container.
 **/
public abstract class ContainerSupport
  extends GenericStateModelAdapter
  implements Container 
{
  protected final ComponentFactory componentFactory = specifyComponentFactory();
  /** this is the prefix that all subcomponents must have as a prefix **/
  protected final String containmentPrefix = specifyContainmentPoint()+".";
  protected ServiceBroker childServiceBroker = null;

  /** The actual set of child BoundComponent loaded. 
   * @see BoundComponent
   **/
  protected final ArrayList boundComponents = new ArrayList(11);

  /** a Sorted Collection of child BinderFactory components.
   * Note that since BinderFactory.comparator does not conform to
   * java's bogus ordering protocol, this is not a true set in the 
   * java collections set, since non-equal members can sort equally.
   **/
  protected final TreeSet binderFactories = new TreeSet(BinderFactory.comparator);

  protected ContainerSupport() {
    ServiceBroker sb = specifyChildServiceBroker();
    if (sb != null) setChildServiceBroker(sb);
  }

  public void setBindingSite(BindingSite bs) {
    //System.err.println("setBindingSite of "+this+" to "+bs);
  }

  /** override to specify a different component factory class. 
   * Called once during initialization.
   **/
  protected ComponentFactory specifyComponentFactory() {
    return new ComponentFactory() {};
  }
  /** override to specify insertion point of this component, 
   * the parent insertion point which sub Components must match,
   * e.g. "Node.AgentManager.Agent.PluginManager"
   * this is called once during initialization.
   **/
  protected abstract String specifyContainmentPoint();

  /** override to specify a the ServiceBroker object to use for children. 
   * this is called once during initialization.
   * Note that this value might be only part of the process for 
   * actually finding the services for children and/or peers.
   * <p>
   * Either this method must be overridden to return a non-null value,
   * or the subclass must call setChildServiceBroker exactly once with 
   * a non-null value.
   **/
  protected ServiceBroker specifyChildServiceBroker() {
    return null;
  }
  
  protected final void setChildServiceBroker(ServiceBroker sb) {
    if (sb == null) {
      throw new IllegalArgumentException("Specified ServiceBroker must not be null");
    }
    if (childServiceBroker != null) {
      throw new IllegalArgumentException("ServiceBroker already set");
    }
    System.err.println("Setting ServiceBroker of "+this+" to "+sb);
    childServiceBroker = sb;
  }

  /** satisfy ContainerAPI extends BindingSite and provides access to the (local) ServiceBroker **/
  public ServiceBroker getServiceBroker() {
    return childServiceBroker;
  }


  //
  // implement collection
  //

  
  public int size() {
    synchronized(boundComponents) {
      return boundComponents.size();
    }
  }
  public boolean isEmpty() {
    synchronized(boundComponents) {
      return boundComponents.isEmpty();
    }
  }
  public boolean contains(Object o) {
    synchronized(boundComponents) {
      int l = boundComponents.size();
      for (int i=0; i<l; i++) {
        BoundComponent bc = (BoundComponent) boundComponents.get(i);
        if (bc.getComponent().equals(o)) return true;
      }
      return false;
    }
  }

  public Iterator iterator() {
    synchronized(boundComponents) {
      int l = boundComponents.size();
      ArrayList tmp = new ArrayList(l);
      for (int i=0; i<l; i++) {
        BoundComponent bc = (BoundComponent) boundComponents.get(i);
        tmp.add(bc.getComponent());
      }
      return tmp.iterator();
    }
  }
  
  public boolean add(Object o) {
    if (o instanceof ComponentDescription) {
      ComponentDescription cd = (ComponentDescription)o;
      String ip = cd.getInsertionPoint();
      if (ip == null) return false;
      if (ip.startsWith(containmentPrefix)) {
        // match! - now do we load it here or below - look for any more dots beyond 
        // the one trailing the prefix...
        String tail = ip.substring(containmentPrefix.length());
        if ("Binder".equals(tail) || "BinderFactory".equals(tail)) {
          return loadBinderFactory(cd);
        } else {
          int subi = tail.indexOf(".");
          if (subi == -1) {
            // no more dots: insert here
            return loadComponent(cd);
          } else {
            // more dots: try inserting in subcomponents
            synchronized (boundComponents) {
              int l = boundComponents.size();
              for (int i=0; i<l; i++) {
                Object p = boundComponents.get(i);
                if (p instanceof Container) {
                  // try loading into this guy.
                  if (((Container)p).add(o)) {
                    return true;    // someone claimed it!
                  }
                }
              }
            }
          }
          return false;
        }
      } else {
        // wrong insertion point!
        return false;
      }
    } else if (o instanceof BinderFactory) {
      return attachBinderFactory((BinderFactory)o);
    } else if (o instanceof Component) {
      return loadComponent( o);
    } else {
      // not a clue.
      return false;
    }
  }

  public boolean remove(Object o) {
    throw new UnsupportedOperationException();
  }

  public void clear() {
    throw new UnsupportedOperationException();
  }

  // unsupported Collection ops
  public Object[] toArray() {
    throw new UnsupportedOperationException();
  }
  public Object[] toArray(Object[] ignore) {
    throw new UnsupportedOperationException();
  }
  public boolean containsAll(Collection c) {
    throw new UnsupportedOperationException();
  }
  public boolean addAll(Collection c) {
    throw new UnsupportedOperationException();
  }
  public boolean removeAll(Collection c) {
    throw new UnsupportedOperationException();
  }
  public boolean retainAll(Collection c) {
    throw new UnsupportedOperationException();
  }

  /** load a component into our set.  We are sure that this is
   * the requested level, but might not be certain how much we trust
   * it as of yet.  In particular, we may need to treat different classes
   * of Components differently.
   *<P>
   * The component (and the binder tree) should be loaded and started
   * when this loadComponent complete successfully.
   *
   * @return true on success.
   **/
  protected boolean loadComponent(Object c) {
    try {
      Binder b = bindComponent(c);
      if (b != null) {
        BoundComponent bc = new BoundComponent(b,c);
        synchronized (boundComponents) {
          boundComponents.add(bc);
        }
        b.load();
        b.start();
        return true;
      } else {
        return false;
      }
    } catch (RuntimeException e) {
      e.printStackTrace();
      return false;
    }
  }

  /* getState()recurses through the boundcomponents binders from the Agent level, 
   * returning a huge array of the Component's ComponentDescription. Pass-through 
   * code implemented in Binder. 
   * blackboard and scheduler override getState() to handle their states.
   
   parent recurses down through binders of bound components
   iterate through arraylist of boundcomponents
   get the next one
   get state of next one - recursive call 
   if null, stop
   
   in binder: getstate() adds to component description
   
   */

  public Object getState() {
    
    synchronized (boundComponents) {
      int l = boundComponents.size();
      ComponentDescription[] descs = new ComponentDescription[l];
      for (int i=0; i<l; i++) {
	Object p = boundComponents.get(i);
	if (p instanceof BoundComponent) {
	  BoundComponent bc = (BoundComponent)p;
	  Binder b = bc.getBinder();
	  Object comp = bc.getComponent();
	  if (comp instanceof ComponentDescription) {
	    ComponentDescription cd = (ComponentDescription)comp;
	    Object state = b.getState();
            ComponentDescription dupCD = 
	      new ComponentDescription(
				       cd.getName(),
				       cd.getInsertionPoint(),
				       cd.getClassname(),
				       cd.getCodebase(),
				       cd.getParameter(),
				       cd.getCertificate(),
				       cd.getLeaseRequested(),
				       cd.getPolicy(),
				       state);
	    descs[i] = dupCD;
          } else {
	    // error?
          }
	} else {
          // error?
	}
      }
      return descs;
    } 
  }

  public void setState(Object state) {
    if (state instanceof ComponentDescription[]) {
      ComponentDescription[] descs = 
	(ComponentDescription[])state;
      for (int i = 0; i < descs.length; i++) {
        add(descs[i]);
      }
    } else {
      // error?
    }
  }
  
  /**  These BinderFactories
   * are used to generate the primary containment
   * binders for the child components.  If the child
   * component is the first BinderFactory, then we'll
   * call bindBinderFactory after failing to find a binder.
   * <p>
   * A Component is initialized (but not loaded) s a side-effect of binding 
   **/
  protected Binder bindComponent(Object c) {
    //System.err.println("Binding: "+c);
    synchronized (binderFactories) {
      ArrayList wrappers = null;
      Binder b = null;
      for (Iterator i = binderFactories.iterator(); i.hasNext(); ) {
        BinderFactory bf = (BinderFactory) i.next();
        if (bf instanceof BinderFactoryWrapper) {
          if (wrappers==null) wrappers=new ArrayList(1);
          wrappers.add(bf);
        } else {
          b = bf.getBinder(c);
          if (b != null) {
            //System.err.println("Bound by: "+b);
            break;
          }
        }
      }

      // now apply any wrappers.
      if (wrappers != null) {
        int l = wrappers.size();
        for (int i=l-1; i>=0; i--) { // last ones innermost
          BinderFactoryWrapper bf = (BinderFactoryWrapper) wrappers.get(i);
          Binder w = bf.getBinder((b==null)?c:b);
          if (w!= null) {
            //System.err.println("Wrapped by: "+w);
            b = w;
          }
        }
      }

      if (b != null) {
        //System.err.println("setting Binder for "+c);
        BindingUtility.setBindingSite(b, getContainerProxy());
        BindingUtility.setServices(b, getServiceBroker());
        //System.err.println("Setting Binder state for "+c);
	if (c instanceof ComponentDescription) {
	  Object state = ((ComponentDescription)c).getState();
          if (state != null) {
	    b.setState(state);
	  }
	}
        //System.err.println("Initializing Binder for "+c);
        BindingUtility.initialize(b);
        // done
        return b;
      } else {
        System.err.println("No binder found for "+c);
        return null;
      }
    }    
  }

  /** Called when a componentDescription insertion point ends in .Binder or .BinderFactory 
   *
   **/
  protected boolean loadBinderFactory(ComponentDescription cd) {
    if (checkBinderFactory(cd)) {
      try {
        Component bfc = componentFactory.createComponent(cd);
        if (bfc instanceof BinderFactory) {
          return attachBinderFactory((BinderFactory)bfc);
        } else {
          System.err.println("Not a BinderFactory: "+bfc);
        }
      } catch (ComponentFactoryException cfe) {
        cfe.printStackTrace();
      }
    } else {
      System.err.println("Failed BinderFactory test: "+cd);
    }
    return false;
  }

  /** @return true iff the binderfactory is trusted enought to load **/
  protected boolean checkBinderFactory(ComponentDescription cd) {
    return true;
  }

  /** Activate a binder factory
   **/
  protected boolean attachBinderFactory(BinderFactory c) {
    synchronized (binderFactories) {
      binderFactories.add(c);
    }
    return BindingUtility.activate(c, getContainerProxy(), getServiceBroker());
  }

  /** Specifies an object to use as the "parent" proxy object
   * for otherwise unbound BinderFactory instances.
   * This will be either be the Container itself (this) or a
   * simple proxy for the container so that BinderFactory instances
   * cannot downcast the object to get additional privileges.
   **/
  abstract protected ContainerAPI getContainerProxy();

}
