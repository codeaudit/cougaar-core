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

/** A basic implementation of a Container.
 **/
public abstract class ContainerSupport
  implements Container 
{
  protected final ComponentFactory componentFactory = specifyComponentFactory();
  /** this is the prefix that all subcomponents must have as a prefix **/
  protected final String containmentPrefix = specifyContainmentPoint()+".";
  protected final ServiceBroker childServiceBroker = specifyChildServiceBroker();
  protected final Class childBindingSite = specifyChildBindingSite();

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
   **/
  protected abstract ServiceBroker specifyChildServiceBroker();
  
  /** Define to specify the BindingSite used to bind child components.
   **/
  protected abstract Class specifyChildBindingSite();

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
        int subi = ip.indexOf('.',containmentPrefix.length());
        if (subi == -1) {
          // no more dots: insert here
          try {
            Component c = componentFactory.createComponent(cd);
            return loadComponent(c);
          } catch (ComponentFactoryException cfe) {
            cfe.printStackTrace();
            return false;
          }
        } else {
          // more dots: try inserting in subcomponents
          synchronized (boundComponents) {
            int l = boundComponents.size();
            for (int i=0; i<l; i++) {
              Object p = boundComponents.get(i);
              if (p instanceof Container) {
                // try loading into this guy.
                if (((Container)p).add(o)) return true;    // someone claimed it!
              }
            }
          }
          return false;
        }
      } else {
        // wrong insertion point!
        return false;
      }
    } else if (o instanceof Component) {
      return loadComponent((Component) o);
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
   *
   * @return true on success.
   **/
  protected boolean loadComponent(Component c) {
    Binder b = bindComponent(c);
    if (b != null) {
      BoundComponent bc = new BoundComponent(b,c);
      synchronized (boundComponents) {
        boundComponents.add(bc);
      }
      // should we sync(this) to avoid this gap?
      if (c instanceof BinderFactory) {
        synchronized (binderFactories) {
          binderFactories.add(c);
        }
      }
      return true;
    } else {
      return false;
    }
  }

  /**  These BinderFactories
   * are used to generate the primary containment
   * binders for the child components.  If the child
   * component is the first BinderFactory, then we'll
   * call bindBinderFactory after failing to find a binder.
   **/
  protected Binder bindComponent(Component c) {
    synchronized (binderFactories) {
      ArrayList wrappers = null;
      Binder b = null;
      for (Iterator i = binderFactories.iterator(); i.hasNext(); ) {
        BinderFactory bf = (BinderFactory) i.next();
        if (bf instanceof BinderFactoryWrapper) {
          if (wrappers==null) wrappers=new ArrayList(1);
          wrappers.add(bf);
        } else {
          b = bf.getBinder(childBindingSite, c);
          if (b != null) break;
        }
      }

      // now apply any wrappers.
      if (wrappers != null) {
        int l = wrappers.size();
        for (int i=l-1; i>=0; i--) { // last ones innermost
          BinderFactoryWrapper bf = (BinderFactoryWrapper) wrappers.get(i);
          Binder w = bf.getBinder(childBindingSite, (b==null)?((Object)c):((Object)b));
          if (w!= null) b = w;
        }
      }
      
      // chicken-and-egg case for BinderFactory
      if (b == null && c instanceof BinderFactory) {
        b = bindBinderFactory((BinderFactory) c);
      }

      // done
      return b;
    }    
  }

  /** implements an extra, fall-through case for BinderFactories
   * which may be acceptable even if no existing Factory will bind it.
   * The default method accepts all BinderFactory components, binding
   * them with a simple Binder which has only a link to the object
   * returned by getProxyForBinderFactory().
   **/
  protected Binder bindBinderFactory(BinderFactory c) {
    // if there are already any BFs, bail out
    synchronized (binderFactories) {
      if (binderFactories.size()>0) return null;

      return new BinderFactoryBinder(getContainerProxy(), c);
    }
  }

  private static class BinderFactoryBinder implements Binder {
    BinderFactoryBinder(Object parentProxy, BinderFactory c) {
      c.setParentComponent(parentProxy);
      // don't bother to keep a link around.
    }
    public ServiceBroker getServiceBroker() { return null; }
    public void requestStop() { }
  }

  /** Specifies an object to use as the "parent" proxy object
   * for otherwise unbound BinderFactory instances.
   * This will be either be the Container itself (this) or a
   * simple proxy for the container so that BinderFactory instances
   * cannot downcast the object to get additional privileges.
   **/
  abstract protected ContainerAPI getContainerProxy();

}
