package org.cougaar.core.component;

import java.beans.*;
import java.beans.beancontext.*;
import java.util.*;
import java.net.URL;

/** A convenience base class for writing your own Components
 * without having to implement BeanContextChild all by yourself.
 * <p>
 * Includes additional support for basic component and
 * service queries.
 **/

public class ComponentSupport
  extends BeanContextChildSupport
  implements Component, Plugin
{
  /** default empty policy **/
  public Object getPolicy() { return null; }

  /** container holds a reference to the binder of our container.
   **/
  private ContainmentBindingSite container = null;

  /** return a reference to our container.  This reference will not
   * change and need not be synchronized because any use of the 
   * variable should be disallowed by component code 
   * until after it has been loaded.  Standard base classes
   * will enforce this.
   **/
  protected final ContainmentBindingSite getContainer() {
    return container;
  }

  /** called when we are "Loaded" into a containing component. 
   * We will not be started until we've been loaded via this call.
   **/
  public void setBeanContext(BeanContext bc) 
    throws PropertyVetoException 
  {
    if (bc instanceof ContainmentBindingSite) {
      super.setBeanContext(bc);
      container = (ContainmentBindingSite) bc;
    } else {
      throw new PropertyVetoException("Component may only be loaded into a ContainmentBindingSite.", null);
    }
  }

  /** used to complain if some service has been revoked **/
  protected class DummyRevokedListener implements BeanContextServiceRevokedListener {
    public void serviceRevoked(BeanContextServiceRevokedEvent e) {
      System.err.println("Warning: "+this+" ignored "+e);
    }
  }

  /** Forward a request for service to our binder.  This method always
   * makes the request on our own behalf, but another implementation might
   * use 
   **/
  protected Object getService(Class serviceClass, Object selector, BeanContextServiceRevokedListener sl)
  {
    try {
      return container.getService(this, this, serviceClass, selector, sl);
    } catch (TooManyListenersException e) {
      e.printStackTrace();
      return null;
    }
  }
  protected Object getService(Class serviceClass, Object selector)
  {
    return getService(serviceClass, selector, new DummyRevokedListener());
  }
  
  protected Object getService(Class serviceClass)
  {
    return getService(serviceClass, null, new DummyRevokedListener());
  }

  public void initialize() {}
  public void start() {}
  public void stop() {}
}
