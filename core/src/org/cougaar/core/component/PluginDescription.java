package org.cougaar.core.component;

import java.beans.*;
import java.beans.beancontext.*;
import java.util.*;
import java.net.URL;

/** A description of a loadable component (i.e. a plugin).
 * We may want several levels of description and protection, 
 * starting here and ending up at an uninitialized instance.  
 * This could be done either as a sequence of classes or
 * as a single class with instantiation state (e.g. Description,
 * Classloaded, Instantiated, Loaded (into a component), Active).
 * <p>
 * The word "Plugin" should be interpreted as "Runtime loaded
 * Component" which implements the Plugin interface.
 **/
public interface PluginDescription {
  /** the name of the class to instantiate **/
  String getClassname();

  /** Where the code for classname should be loaded from **/
  URL getCodebase();

  /** a parameter supplied to the constructor of classname,
   * often some sort of structured object (xml document, etc).
   * <p>
   * This is defined as just an Object, but we will likely 
   * have to impose additional restrictions (e.g. Serializable) 
   * for safety reasons.
   * <p>
   * Rovers and other "mobile" components will include accumulated
   * state or an instance of themselves in the parameter.  This allows
   * receiving agents more flexibility in determining trustability
   * of the component being sent.
   **/
  Object getParameter();

  /**
   * Assurance that the Plugin is trustworth enough to instantiate.
   * The type is specified as Object until we decide what really
   * should be here.
   **/
  Object getCertificate();

  /**
   * Lease information - how long should the plugin live in the agent?
   * We need some input on what this should look like.
   * It is possible that this could be merged with Policy.
   **/
  Object getLeaseRequested();

  /**
   * High-level Policy information.  Allows plugin policy/techspec 
   * to contribute to the component hookup process before it is 
   * actually instantiated.  Perhaps this is overkill, and instance-level
   * policy is sufficient.
   **/
  Object getPolicy();
}
