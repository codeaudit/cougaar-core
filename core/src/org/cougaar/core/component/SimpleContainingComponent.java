package org.cougaar.core.component;

import java.beans.*;
import java.beans.beancontext.*;
import java.util.*;
import java.net.URL;

/** A standard Containing Component suitable for use as
 * something like a plugin manager.
 **/
public class SimpleContainingComponent 
  extends BeanContextServicesSupport
  implements ContainingComponent 
{
  private PluginService pluginService = new PluginServiceImpl();

  public SimpleContainingComponent() {
    addFactory(new SimpleContainmentBinderFactory());
  }

  public void setBeanContext(BeanContext bc) 
    throws PropertyVetoException 
  {
    super.setBeanContext(bc);
    if (bc instanceof BeanContextServices) {
      BeanContextServices bcs = (BeanContextServices) bc;

      // let our container know we can handle plugins
      final Object pis = new PluginServiceImpl();
      bcs.addService(PluginService.class,
                     new BeanContextServiceProvider() {
                         public Iterator getCurrentServiceSelectors(BeanContextServices xbcs, Class sc) {
                           return null;
                         }
                         public Object getService(BeanContextServices xbcs, Object r, Class sc, Object ss) {
                           if (sc.equals(PluginService.class))
                             return pis;
                           else
                             return null;
                         }
                         public void releaseService(BeanContextServices xbcs, Object requestor, Object service) {
                         }
                       });
    }
  }

  // implement Component

  /** default empty policy **/
  public Object getPolicy() { return null; }

  /** @return true IFF the named service should be treated as
   * private, that is, restricted to our use and not republished
   * to children.  By default, private means a server which is not 
   * a BindingSite.
   **/
  protected boolean isServicePrivate(Class serviceClass) {
    return !(serviceClass.isAssignableFrom(BindingSite.class));
  }
  
  // implement a binder and binder factory for containment.

  /** implement a factory for binding plugins (sub components) **/
  class SimpleContainmentBinderFactory 
    implements BinderFactory 
  {
    public int getPriority() { return 5; }
    public Binder getBinder(Class serviceClass, Object client) {
      if (client instanceof Component && // bind components
          serviceClass.isAssignableFrom(ContainmentBindingSite.class) // requesting component service
          ) {
        return new SimpleContainmentBinder((Component) client);
      } else {
        return null;
      }
    }
  }

  class SimpleContainmentBinder 
    extends BeanContextServicesSupport
    implements Binder, ContainmentBindingSite
  {
    private Component plugin;

    SimpleContainmentBinder(Component plugin) {
      super(SimpleContainingComponent.this); // delegate BeanContextServices to the outer instance
      this.plugin = plugin;
    }

    public Object getPlugin() { return plugin; }

    // implement the Containing side
    // run model.

    // implement the client (plugin) side: most comes from BeanContextServices

    /** prevents children from getting private services **/
    public Object getService(BeanContextChild bcc, Object requestor, Class serviceClass, Object selector, BeanContextServiceRevokedListener sl)
      throws TooManyListenersException
    {
      if (isServicePrivate(serviceClass)) {
        return null;
      } else {
        return super.getService(bcc,requestor,serviceClass,selector,sl);
      }
    }

    public void requestStop() {
      if (plugin instanceof Plugin) {
        ((Plugin)plugin).stop();
      }
    }
  }
    
  // implement plugin service
  class PluginServiceImpl 
    implements PluginService 
  {
    public boolean add(Object o) {
      Log.logMethod(this, "add", o);
      return addPlugin(o);
    }
    public boolean remove(Object o) {
      Log.logMethod(this, "remove", o);
      return removePlugin(o);
    }
  }

  private boolean addPlugin(Object o) {
    Log.logMethod(this, "addPlugin", o);
    if (o instanceof BinderFactory) {
      return addFactory((BinderFactory)o);
    } else if (o instanceof Component) {
      return addComponent((Component) o);
    } else {
      return false;
    }
  }

  private boolean removePlugin(Object o) {
    if (o instanceof BinderFactory) {
      return removeFactory((BinderFactory) o);
    } else if (o instanceof Component) {
      return removeComponent((Component) o);
    } else {
      return false;
    }
  }
    
  // factories are maintained as a separate list
  private List binderFactories = new ArrayList();
  /** add a factory to the factory list.  This implementation ignores priority **/
  protected boolean addFactory(BinderFactory bf) {
    Log.logMethod(this, "addFactory", bf);
    synchronized (binderFactories) {
      return binderFactories.add(bf);
    }
  }
  /** remove a factory immediately.  This will prevent new use of the removed
   * factory but will not effect any existing Binders.
   **/
  protected boolean removeFactory(BinderFactory bf) {
    synchronized (binderFactories) {
      return binderFactories.remove(bf);
    }
  }
    
  /** get a binder for a new client by delegating to the binderFactory list. **/
  protected BindingSite bindClient(Class serviceClass, Object client) {
    Log.logMethod(this, "bindClient", serviceClass.toString()+", "+client);
    synchronized (binderFactories) {
      for (Iterator i = binderFactories.iterator(); i.hasNext();) {
        BinderFactory bf = (BinderFactory) i.next();
        BindingSite bs = bf.getBinder(serviceClass, client);
        if (bs != null) {
          Log.logResult(this, "bindClient", bs);
          return bs;
        }
      }
      Log.logResult(this, "bindClient", null);
      return null;
    }
  }


  // child components are maintained as Collection members
  // via the BeanContextSupport superclass.
  protected boolean addComponent(Component c) {
    Log.logMethod(this, "addComponent", c);
    // see if we know how to plug it in
    ContainmentBindingSite bs = (ContainmentBindingSite) bindClient(ContainmentBindingSite.class, c);
    if (bs != null) {
      return add(bs);
    }
    return false;
  }

  /** remove the correct binder by doing a search and remove **/
  protected boolean removeComponent(Component c) {
    synchronized(BeanContext.globalHierarchyLock) {
      for (Iterator i = this.iterator(); i.hasNext(); ) {
        Object o = i.next();
        if (o instanceof Binder) {
          if (c == ((Binder)o).getPlugin()) {
            i.remove();
            return true;
          }
        }
      }
    }
    return false;
  }
}
