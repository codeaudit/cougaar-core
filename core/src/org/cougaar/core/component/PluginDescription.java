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
 * <p>
 * The Description is interpreted and evaluated to varying degrees
 * as it is passed through the hierarchy until the insertion point
 * is found.  In particular, the description will be evaluated for
 * trust attributes at each level before instantiation or pass-through.
 * <p>
 * Perhaps this might be better named ComponentDescription.
 **/
public interface PluginDescription {
  /** The point (ContainerComponent) at which the Plugin 
   * should be inserted.  This is the name of a class which 
   * could be loaded in the core (e.g. not in the plugin's
   * codebase). It is used by the component hierarchy to
   * determine the container component the plugin should be
   * added.  This point is interpreted individually by each
   * (parent) component as it propagates through the container
   * hierarchy - it may be interpreted any number of times along
   * the way to the final insertion point.
   */
  String getInsertionPoint();

  /** the name of the class to instantiate, relative to the
   * codebase url.  The class will not be loaded or instantiated
   * until the putative parent component has been found and has
   * had the opportunity to verify the plugin's identity and 
   * authorization.
   **/
  String getClassname();

  /** Where the code for classname should be loaded from.
   * Will be evaulated for trust before any classes are loaded
   * from this location.
   **/
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
