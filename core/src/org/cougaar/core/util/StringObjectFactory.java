/*
 * <copyright>
 *  Copyright 1997-2001 BBNT Solutions, LLC
 *  under sponsorship of the Defense Advanced Research Projects Agency (DARPA).
 * 
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the Cougaar Open Source License as published by
 *  DARPA on the Cougaar Open Source Website (www.cougaar.org).
 * 
 *  THE COUGAAR SOFTWARE AND ANY DERIVATIVE SUPPLIED BY LICENSOR IS
 *  PROVIDED 'AS IS' WITHOUT WARRANTIES OF ANY KIND, WHETHER EXPRESS OR
 *  IMPLIED, INCLUDING (BUT NOT LIMITED TO) ALL IMPLIED WARRANTIES OF
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE, AND WITHOUT
 *  ANY WARRANTIES AS TO NON-INFRINGEMENT.  IN NO EVENT SHALL COPYRIGHT
 *  HOLDER BE LIABLE FOR ANY DIRECT, SPECIAL, INDIRECT OR CONSEQUENTIAL
 *  DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE OF DATA OR PROFITS,
 *  TORTIOUS CONDUCT, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 *  PERFORMANCE OF THE COUGAAR SOFTWARE.
 * </copyright>
 */
package org.cougaar.core.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.util.*;

import org.cougaar.core.domain.Factory;
import org.cougaar.util.StringUtility;
import org.cougaar.util.Reflect;

/**
 * String-based Object factory, using reflection and COUGAAR factories.
 * <p>
 * Pruned from <code>OrgRTDataPlugin</code>'s parsing and reflective 
 * field setters.  <b>Needs work!</b>  OrtRTDataPlugin should be
 * modifed to use this class!  Should support nesting of objects!
 * <p>
 */
public class StringObjectFactory {


  /**
   * Public methods
   */

  public static Object parseObject(Factory ldmf, String type, String arg) {
    Class cl;

    try {
      cl = findClass(type);
    } catch (RuntimeException noSuchClassE) {
      // unable to find class -- maybe a "parse" type.
      int i;
      //System.err.println("Parsing '"+type+"/"+arg+"'");
      if ((i = type.indexOf("<")) >= 0) {
        int j = type.lastIndexOf(">");
        String ctype = type.substring(0,i);
        String etype = type.substring(i+1, j);
        Collection c = null;
        if (ctype.equals("Collection") || ctype.equals("List")) {
          c = new ArrayList();
        } else {
          throw new RuntimeException("Unparsable collection type: "+type);
        }
        Vector l = StringUtility.parseCSV(arg);
        for (Iterator it = l.iterator(); it.hasNext(); ) {
          c.add(parseObject(ldmf, etype,(String) it.next()));
        }
        return c;
      } else if ((i = type.indexOf("/")) >= 0) {
        String m = type.substring(0,i);
        String mt = type.substring(i+1);
        double qty = Double.valueOf(arg).doubleValue();
        return createMeasureObject(m, qty, mt);
      } else {
        System.err.println("Exception finding class "+type+":");
        noSuchClassE.printStackTrace();
        throw new RuntimeException("Construction problem "+noSuchClassE);
      }
    }

    if (classClass.isAssignableFrom(cl)) {
      return cl;
    }

    try {
      if (javaUtilCollectionClass.isAssignableFrom(cl)) {
        if ((arg == null) || (arg.length() <= 0))
          return cl.newInstance();
        else
          throw new RuntimeException("Not expecting a Collection!");
      } else if (cl.isInterface()) {
        // interface means try the COF
        return parseWithCOF(ldmf, cl, arg);
      } else {
        Constructor cons = getArgClass(cl).getConstructor(stringArgSpec);
        Object[] args = {arg};
        return cons.newInstance(args);
      }
    } catch (Exception constructE) {
      System.err.println("Exception constructing "+type+" from \""+arg+"\":");
      constructE.printStackTrace();
      throw new RuntimeException("Construction problem "+constructE);
    }
  }

  public static Object callFactoryMethod(Factory ldmf, String ifcname) {
    // look up a zero-arg factory method in the ldmf
    Class ldmfc = ldmf.getClass();
    String newname = "new"+ifcname;
    try {
      Method fm = ldmfc.getMethod(newname,nullClassList);
      return fm.invoke(ldmf, nullArgList);
    } catch (Exception e) {
      throw new RuntimeException (
          "Couldn't find a factory method for "+ifcname);
    }
  }

  public static Class findClass(String name) {
    synchronized (classes) {
      Class c = (Class) classes.get(name);
      // try the cache
      if (c != null) return c;

      for (Iterator i = packages.iterator(); i.hasNext(); ) {
        String pkg = (String) i.next();
        try {                   // Oh so ugly!
          c = Class.forName(pkg+name);
          if (c != null) {        // silly
            classes.put(name, c);
            return c;
          }
        } catch (ClassNotFoundException e) {}; // sigh
      }
      throw new RuntimeException("Could not find a class for '"+name+"'.");
    }
  }

  /**
   * Creates and calls the appropriate "setter" method for the classInstance
   * which is of type className.
   */
  public static void createAndCallSetter(
      Object classInstance, String className,
      String setterName, String type, Object value) {
    Class parameters[] = new Class[1];
    Object arguments[] = new Object[] {value};
    
    try {
      parameters[0] = findClass(type);
      Class propertyClass = Class.forName(className);
      //Method meth = propertyClass.getMethod(setterName, parameters);
      Method meth = findMethod(propertyClass, setterName, parameters);
      meth.invoke(classInstance, arguments);
    } catch (Exception e) {
      System.out.println("ODPI Exception in createAndCallSetter: " + e);
      e.printStackTrace();
      //System.exit(-1);
    }
  }

  /**
   * <code>findMethod</code> uses a cache.
   */
  public static Method findMethod(Class c, String name) {
    synchronized (methods) {
      // see if in cache
      Method foundMethod = findInCache(methods, c, name, null);
      if (foundMethod != null)
	return foundMethod;
      // look at all methods. watch for duplicate names
      Method[] allMeths = Reflect.getMethods(c);
      for (int i = allMeths.length-1; i >= 0; i--) {
        Method m = allMeths[i];
        if (name.equals(m.getName())) {
          if (foundMethod == null) {
            foundMethod = m;
          } else {
            throw new RuntimeException(
              "Ambiguous method "+name+"(?) of class "+c);
          }
        }
      }
      // see if we found it
      if (foundMethod != null)
	addToCache(methods, c, foundMethod);
      return foundMethod;
    }
  }

  /**
   * Similar to other <code>findMethod</code>, except checks all
   * the parameters.
   * <p>
   * <b>Note</b>: Does simple "isAssignable" checking for parameters, so
   * may be confused in some cases, e.g. class has two methods:
   * <pre>
   *   public void fooMethod(Object o) {..}
   *   public void fooMethod(String s) {..}
   * </pre>
   * and you look up ("fooMethod", {String}).
   */
  public static Method findMethod(Class c, String name, Class params[]) {
    synchronized (methods) {
      // see if in cache
      Method foundMethod = findInCache(methods, c, name, params);
      if (foundMethod != null)
	return foundMethod;
      // look at all methods. add to cache if found
      int params_length;
      try {
        params_length = params.length;
      } catch (NullPointerException npe) {
        // call the right one next time!
        return findMethod(c, name);
      }
      Method[] allMeths = c.getMethods();
      foundMethod = null;
      for (int i = allMeths.length-1; i >= 0; i--) {
        Method m = allMeths[i];
        if (name.equals(m.getName())) {
	  // want to add all methods with matching name to cache,
	  // to make non-param case work correctly.
	  addToCache(methods, c, m);
          // check parameters
          Class mps[] = m.getParameterTypes();
          if (mps.length == params_length) {
            for (int j = 0; ; j++) {
              if (j >= params_length) {
                // params match.  found the method.  keep going to
                // add all methods with given name for no-param case.
                foundMethod = m;
                break;
              }
              if (!(mps[j].isAssignableFrom(params[j])))  {
                // params don't match.  try next method.
                break;
              }
            }
          }
        }
      }
      // return what we found
      return foundMethod;
    }
  }

  /**
   * Protected fields and methods
   */

  protected static final Class javaUtilCollectionClass;
  protected static final Class classClass;
  protected static HashMap classes;
  protected static HashMap methods;
  protected static final Collection packages;

  static {
    Class forNameClass;
    try {
      forNameClass = Class.forName("java.util.Collection");
    } catch (Exception e) {
      forNameClass = null; //never!
    }
    javaUtilCollectionClass = forNameClass;

    try {
      forNameClass = Class.forName("java.lang.Class");
    } catch (Exception e) {
      forNameClass = null; //never!
    }
    classClass = forNameClass;

    // initialize packages:
    packages = new ArrayList();
    packages.add("");
    packages.add("org.cougaar.planning.ldm.measure.");
    packages.add("org.cougaar.planning.ldm.plan.");
    packages.add("org.cougaar.planning.ldm.asset.");
    packages.add("org.cougaar.planning.ldm.oplan.");
    packages.add("java.lang.");  // extras for fallthrough
    packages.add("java.util.");


    // initialize the classmap with some common ones
    classes = new HashMap(89);
    // precache some builtins
    classes.put("long", Long.TYPE);
    classes.put("int", Integer.TYPE);
    classes.put("integer", Integer.TYPE);
    classes.put("boolean", Boolean.TYPE);
    classes.put("float", Float.TYPE);
    classes.put("double", Double.TYPE);
    // and some java.lang
    classes.put("Double", Double.class);
    classes.put("String", String.class);
    classes.put("Integer", Integer.class);
    // and some java.util
    classes.put("Collection", Collection.class);
    classes.put("List", List.class);
    // COUGAAR-specific stuff will be looked for

    methods = new HashMap(13);
  }

  protected static Class[] stringArgSpec = {String.class};

  protected static Class[][] argClasses = {
    {Integer.TYPE, Integer.class},
    {Double.TYPE, Double.class},
    {Boolean.TYPE, Boolean.class},
    {Float.TYPE, Float.class},
    {Long.TYPE, Long.class},
    {Short.TYPE, Short.class},
    {Byte.TYPE, Byte.class},
    {Character.TYPE, Character.class}};
                                     
  protected static Class getArgClass(Class c) {
    if (! c.isPrimitive()) return c;
    for (int i = 0; i < argClasses.length; i++) {
      if (c == argClasses[i][0])
        return argClasses[i][1];
    }
    throw new IllegalArgumentException("Class "+c+" is an unknown primitive.");
  }

  protected static String getType(String type) {
    int i;
    if ((i = type.indexOf("<")) > -1) { // deal with collections 
      int j = type.lastIndexOf(">");
      return getType(type.substring(0,i)); // deal with measures
    } else if ((i= type.indexOf("/")) > -1) {
      return getType(type.substring(0,i));
    } else {
      return type;
    }
  }
    

  protected static Object parseWithCOF(Factory ldmf, Class cl, String val) {
    String name = cl.getName();
    int dot = name.lastIndexOf('.');
    if (dot != -1) name = name.substring(dot+1);

    try {
      // lookup method on ldmf
      Object o = callFactoryMethod(ldmf, name);

      Vector svs = org.cougaar.util.StringUtility.parseCSV(val);
      // svs should be a set of strings like "slot=value" or "slot=type value"
      for (Enumeration sp = svs.elements(); sp.hasMoreElements();) {
        String ss = (String) sp.nextElement();

        int eq = ss.indexOf('=');
        String slotname = ss.substring(0, eq);
        String vspec = ss.substring(eq+1);
        
        int spi = vspec.indexOf(' ');
        Object v;
        if (spi == -1) {
          v = vspec;
        } else {
          String st = vspec.substring(0, spi);
          String sv = vspec.substring(spi+1);
          v = parseObject(ldmf, st, sv);
        }
        callSetMethod(o, slotname, v);
      }
      return o;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  protected static final Class nullClassList[] = {};
  protected static final Object nullArgList[] = {};

  protected static void callSetMethod(Object o, String slotname, Object value) {
    Class oc = o.getClass();
    String setname = "set"+slotname;
    Class vc = value.getClass();

    try {
      Method ms[] = oc.getMethods();
      for (int i = 0; i<ms.length; i++) {
        Method m = ms[i];
        if (setname.equals(m.getName())) {
          Class mps[] = m.getParameterTypes();
          if (mps.length == 1 &&
              mps[0].isAssignableFrom(vc)) {
            Object args[] = {value};
            m.invoke(o, args);
            return;
          }
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(
          "Couldn't find set"+slotname+" for "+o+", value "+value);
    }

    throw new RuntimeException(
        "Couldn't find set"+slotname+" for "+o+", value "+value);
  }

  /**
   * Currently have a hashmap of classes (<code>classMethCache</code>) with 
   * each entry a hashmap of method name (<code>methCache</code>.  Under 
   * each hashed method name (<code>entry</code>) it has a either a 
   * Method or ArrayList of Methods (<code>alist</code>).
   * <p>
   * Might want to switch to lookup class in hashmap to get ArrayList of 
   * used methods, then search methods by methodname.  Or maybe lookup 
   * (Class, Method) pair by (classname+methodname).intern() and then 
   * look for (wantedClass, wantedMethod).  
   * <p>
   * Some time/space tradeoff here...
   */
  protected static Method findInCache(
      HashMap classMethCache, Class c, String name, Class[] params) {
    HashMap methCache = (HashMap)classMethCache.get(c);
    if (methCache != null) {
      Object entry = methCache.get(name);
      if (entry instanceof Method) {
        return (Method)entry;
      } else if (entry instanceof ArrayList) {
	if (params == null) {
          throw new RuntimeException(
            "Ambiguous method "+name+"(?) of class "+c);
	} else {
          int params_length = params.length;
          ArrayList alist = (ArrayList)entry;
          int alist_size = alist.size();
          // look for method.
          for (int i = 0; i < alist_size; i++) {
            Method m = (Method)alist.get(i);
            // check parameters
            Class mps[] = m.getParameterTypes();
            if (mps.length == params_length) {
              for (int j = 0; ; j++) {
                if (j >= params_length) {
                  // params match.  found the method in the cache
                  // TAKE THE FIRST MATCH!!!!
                  return m;
                }
                if (!(mps[j].isAssignableFrom(params[j]))) {
                  // params don't match.  try next method.
                  break;
                }
              }
            }
          }
        }
      } else {
        // not listed
      }
    }
    return null;
  }

   /** @see #findInCache(HashMap,Class,name,Class[]) **/
  protected static void addToCache(HashMap classMethCache, Class c, Method m) {
    // found the method.  add to cache
    HashMap methCache = (HashMap)classMethCache.get(c);
    if (methCache == null) {
      methCache = new HashMap(13);
      classMethCache.put(c, methCache);
    }
    String name = m.getName();
    Object entry = methCache.get(name);
    if (entry instanceof Method) {
      ArrayList alist = new ArrayList();
      alist.add((Method)entry);
      alist.add(m);
      methCache.put(name, alist);
    } else if (entry instanceof ArrayList) {
      ((ArrayList)entry).add(m);
    } else { 
      methCache.put(name, m);
    } 
  }

  /**
   * Returns the integer value for the appropriate
   * unitOfMeasure field in the measureClass
   */
  protected static int getMeasureUnit(
      String measureClass, String unitOfMeasure) {
    try {
      String fullClassName = "org.cougaar.planning.ldm.measure." + measureClass;
      Field f = Class.forName(fullClassName).getField(unitOfMeasure);
      return f.getInt(null);
    } catch (Exception e) {
      System.out.println("ODPI Exception in getMeasureUnit for type: " +
          unitOfMeasure);
      System.out.println(e);
      e.printStackTrace();
      //System.exit(-1);
    }
    return -1;
  }

  /**
   * Returns a measure object which is an instance of className and has
   * a quantity of unitOfMeasure
   */
  protected static Object createMeasureObject(
      String className, double quantity, String unitOfMeasure) {
    try {
      Class classObj = Class.forName("org.cougaar.planning.ldm.measure." + className);
      String methodName = "new" + className;
      Class parameters[] = {double.class, int.class};
      Method meth = classObj.getMethod(methodName, parameters);
      Object arguments[] = {new Double(quantity), 
        new Integer(getMeasureUnit(className, unitOfMeasure))};
      return meth.invoke(classObj, arguments); // static method call
    } catch (Exception e) {
      System.out.println("ODPI Error in createMeasureObject: " + e);
      e.printStackTrace();
      //System.exit(-1);
    }
    return null;
  }

  /* Sample usage 
  public static void main(String[] args) {
    Factory ldmf = new RootFactory(null, null);
    String dataType = "GeolocLocation";
    String dataVal ="GeolocCode=HKUZ"+
      ", Name=Fort Stewart"+
      ", Latitude=Latitude 31.85degrees"+
      ", Longitude=Longitude -81.5degrees";
    Object arg = null;
    try {
      arg = parseObject(ldmf, dataType, dataVal);
    } catch (RuntimeException e) {
      System.out.println("Exception:");
      e.printStackTrace();
    }
    if (!(arg instanceof org.cougaar.planning.ldm.plan.GeolocLocation)) {
      System.out.println("FAILED: ");
      System.out.println("  Type: "+dataType);
      System.out.println("  Value: "+dataVal);
      System.out.println("  Result: "+arg);
    } else {
      org.cougaar.planning.ldm.plan.GeolocLocation loc = (org.cougaar.planning.ldm.plan.GeolocLocation)arg;
      System.out.println("SUCCESS: ");
      System.out.println("  Code: "+loc.getGeolocCode());
      System.out.println("  Name: "+loc.getName());
      System.out.println("  Lat: "+loc.getLatitude().getDegrees());
      System.out.println("  Lon: "+loc.getLongitude().getDegrees());
    }
  }
  */

}
