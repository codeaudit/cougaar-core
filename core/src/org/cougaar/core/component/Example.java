/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.core.component;

import java.beans.*;
import java.beans.beancontext.*;
import java.util.*;
import java.net.URL;


/** Trivial case of nesting components and binding plugins **/
public class Example {
  public static final void main(String arg[]) {
    try {
      BeanContextServices bcs = new BeanContextServicesSupport();
      NamedContainer pm = new NamedContainer("Manager");
      bcs.add(pm);
      PluginService ps = (PluginService) bcs.getService(pm,pm,PluginService.class,null,new BCSRevokedL());
      NamedPlugin p1 = new Plugin1();
      ps.add(p1);
      NamedPlugin p2 = new Plugin2();
      ps.add(p2);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}

class BCSRevokedL implements BeanContextServiceRevokedListener {
  public void serviceRevoked(BeanContextServiceRevokedEvent e) {
  }
}

class NamedContainer extends SimpleContainingComponent {
  String name;
  public NamedContainer(String name) { this.name = name; }
  public String toString() { return name; }
}

class NamedPlugin
  extends ComponentSupport
{
  String name;
  public NamedPlugin(String name) { this.name = name; }
  public String toString() { return name; }
}
  
class Plugin1 extends NamedPlugin {
  public Plugin1() {
    super("Plugin1");
  }
}

class Plugin2 extends NamedPlugin {
  public Plugin2() {
    super("Plugin2");
  }
}
