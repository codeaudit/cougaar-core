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
package org.cougaar.planning.plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.node.InitializerService;
import org.cougaar.core.plugin.SimplePlugIn;
import org.cougaar.planning.Constants;
import org.cougaar.core.domain.DomainManager;
import org.cougaar.core.domain.RootFactory;
import org.cougaar.planning.ldm.asset.Asset;
import org.cougaar.planning.ldm.asset.ClusterPG;
import org.cougaar.planning.ldm.asset.ItemIdentificationPGImpl;
import org.cougaar.planning.ldm.asset.LocationSchedulePG;
import org.cougaar.planning.ldm.asset.LocationSchedulePGImpl;
import org.cougaar.planning.ldm.asset.NewClusterPG;
import org.cougaar.planning.ldm.asset.NewItemIdentificationPG;
import org.cougaar.planning.ldm.asset.NewLocationSchedulePG;
import org.cougaar.planning.ldm.asset.NewPropertyGroup;
import org.cougaar.planning.ldm.asset.NewRelationshipPG;
import org.cougaar.planning.ldm.asset.NewTimePhasedPropertyGroup;
import org.cougaar.planning.ldm.asset.NewTypeIdentificationPG;
import org.cougaar.planning.ldm.asset.PropertyGroup;
import org.cougaar.planning.ldm.asset.PropertyGroupSchedule;
import org.cougaar.planning.ldm.asset.RelationshipBG;
import org.cougaar.planning.ldm.asset.TimePhasedPropertyGroup;
import org.cougaar.planning.ldm.measure.Latitude;
import org.cougaar.planning.ldm.measure.Longitude;
import org.cougaar.planning.ldm.plan.AspectType;
import org.cougaar.planning.ldm.plan.HasRelationships;
import org.cougaar.planning.ldm.plan.LatLonPointImpl;
import org.cougaar.planning.ldm.plan.LocationScheduleElement;
import org.cougaar.planning.ldm.plan.LocationScheduleElementImpl;
import org.cougaar.planning.ldm.plan.NewLocationScheduleElement;
import org.cougaar.planning.ldm.plan.NewPrepositionalPhrase;
import org.cougaar.planning.ldm.plan.NewRoleSchedule;
import org.cougaar.planning.ldm.plan.NewSchedule;
import org.cougaar.planning.ldm.plan.NewTask;
import org.cougaar.planning.ldm.plan.Preference;
import org.cougaar.planning.ldm.plan.Relationship;
import org.cougaar.planning.ldm.plan.Role;
import org.cougaar.planning.ldm.plan.Schedule;
import org.cougaar.planning.ldm.plan.ScheduleImpl;
import org.cougaar.planning.ldm.plan.ScoringFunction;
import org.cougaar.planning.ldm.plan.TimeAspectValue;
import org.cougaar.planning.ldm.plan.Verb;
import org.cougaar.util.ConfigFinder;
import org.cougaar.util.Reflect;
import org.cougaar.util.TimeSpan;

/**
 * Generic part of plugin to create local asset and the Report tasks
 * associated with all the local asset's relationships. Local asset must have ClusterPG and 
 * RelationshipPG, Presumption is that the 'other' assets in all the 
 * relationships have both Cluster and Relationship PGs.
 * Currently assumes that each Cluster has exactly 1 local asset.
 *
 * Extensions of this class load this information from specific media such as files.
 **/
public abstract class AssetDataPlugInBase extends SimplePlugIn {
  public static final String SELF = ("Self");

  protected static TrivialTimeSpan ETERNITY = 
    new TrivialTimeSpan(TimeSpan.MIN_VALUE,
                        TimeSpan.MAX_VALUE);

  protected InitializerService initializerService;

  public void setInitializerService(InitializerService is) {
    initializerService = is;
  }

  public long getDefaultStartTime() {
    return TimeSpan.MIN_VALUE;
  }

  public long getDefaultEndTime() {
    return TimeSpan.MAX_VALUE;
  }

  protected DateFormat myDateFormat = DateFormat.getInstance(); 

  protected String myAssetClassName = null;
  protected List myRelationships = new ArrayList();
  protected Map myOtherAssets = new HashMap();
  protected Asset myLocalAsset = null;

  protected void setupSubscriptions() {
    getSubscriber().setShouldBePersisted(false);

    if (!didRehydrate()) {
      processAssets();	// Objects should already exist after rehydration
    }
  }

  public void execute() {
  }
                       

  /**
   * Parses the prototype-ini file and in the process sets up
   * the relationships with pairs of "relationship"/"asset
   */

  protected void processAssets() {
    try {
      String cId = getClusterIdentifier().getAddress();
      AssetDataReader assetDataReader = initializerService.getAssetDataReader();
      assetDataReader.readAsset(cId, new AssetDataCallbackImpl());
//        System.out.println("Property Groups: ");
//        Vector all = myLocalAsset.fetchAllProperties();
//        try {
//          for (Iterator i = all.iterator(); i.hasNext(); ) {
//            Object o = i.next();
//            if (!(o instanceof PropertyGroup)) continue;
//            PropertyGroup pg = (PropertyGroup) o;
//            Class pgc = pg.getClass();
//            System.out.println(pgc.getName());
//            Method[] methods = pgc.getMethods();
//            for (int j = 0; j < methods.length; j++) {
//              Method method = methods[j];
//              if (method.getName().startsWith("get") && method.getParameterTypes().length == 0) {
//                Object value = methods[j].invoke(pg, new Object[0]);
//                System.out.println("  " + method.getName() + " = " + value);
//              }
//            }
//          }
//        } catch (Exception e) {
//          e.printStackTrace();
//        }
      publishAdd(myLocalAsset);

      // Put the assets for this cluster into array
      for (Iterator iterator = myRelationships.iterator(); 
             iterator.hasNext();) {
        Relationship relationship = (Relationship) iterator.next();
        report(relationship);
      } 
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  protected void createMyLocalAsset(String assetClassName) {
    myAssetClassName = assetClassName;
    myLocalAsset = getFactory().createAsset(myAssetClassName);
    // set up this asset's available schedule
    NewSchedule availsched = 
      getFactory().newSimpleSchedule(getDefaultStartTime(), 
                                     getDefaultEndTime());
    // set the available schedule
    ((NewRoleSchedule)myLocalAsset.getRoleSchedule()).setAvailableSchedule(availsched);
            
    // initialize the relationship info
    NewRelationshipPG pg = 
      (NewRelationshipPG) myLocalAsset.getRelationshipPG();
    RelationshipBG bg = 
      new RelationshipBG(pg, (HasRelationships) myLocalAsset);
    // this asset is local to the cluster
    pg.setLocal(true);
  }

  protected void report(Relationship relationship) {
    Asset sendTo = 
      (((Asset) relationship.getA()).getKey().equals(myLocalAsset.getKey())) ?
      getFactory().cloneInstance((Asset) relationship.getB()) :
      getFactory().cloneInstance((Asset) relationship.getA());
    
    Asset localClone = getFactory().cloneInstance(myLocalAsset);

    List roles = new ArrayList(1);
    Role role = 
      (((Asset) relationship.getA()).getKey().equals(myLocalAsset.getKey())) ?
      relationship.getRoleA() : relationship.getRoleB();
    roles.add(role);
    
    NewTask reportTask =
      createReportTask(localClone, sendTo, roles, 
                       relationship.getStartTime(),
                       relationship.getEndTime());
    publishAdd(reportTask);
  
  }

  protected void setLocationSchedule(String latStr, String lonStr) {
    Latitude lat = Latitude.newLatitude(latStr);

    Longitude lon = Longitude.newLongitude(lonStr);

    LatLonPointImpl loc = new LatLonPointImpl(lat, lon);

    // create LocationScheduleElementImpl
    LocationScheduleElement locSchedElem =
      new LocationScheduleElementImpl(
          TimeSpan.MIN_VALUE, TimeSpan.MAX_VALUE, loc);

    // add to schedule
    LocationSchedulePG locSchedPG = myLocalAsset.getLocationSchedulePG();
    if (locSchedPG == null) {
      locSchedPG = new LocationSchedulePGImpl();
      myLocalAsset.setLocationSchedulePG(locSchedPG);
    }
    Schedule locSched = locSchedPG.getSchedule();
    if (locSched == null) {
      locSched = new ScheduleImpl();
      ((NewLocationSchedulePG)locSchedPG).setSchedule(locSched);
    }
    locSched.add(locSchedElem);
  }

  protected void addRelationship(String typeId, String itemId,
                                 String otherClusterId, String roleName,
                                 long start, long end) {
    Asset otherAsset =
      getAsset(myAssetClassName, itemId, typeId, otherClusterId);
    Relationship relationship = 
      getFactory().newRelationship(Role.getRole(roleName),
                                   (HasRelationships) myLocalAsset,
                                   (HasRelationships) otherAsset,
                                   start,
                                   end);
            
    myRelationships.add(relationship);
  }

  //create the Report task to be sent to myself which will result in an asset 
  //transfer of the copyOfMyself being sent to the cluster I am supporting.
  protected NewTask createReportTask(Asset reportingAsset,
                                     Asset sendto,
                                     Collection roles,
                                     long startTime,
                                     long endTime) {
    NewTask reportTask = getFactory().newTask();
    reportTask.setDirectObject(reportingAsset);

    Vector prepPhrases = new Vector(2);
    NewPrepositionalPhrase newpp = getFactory().newPrepositionalPhrase();
    newpp.setPreposition(Constants.Preposition.FOR);
    newpp.setIndirectObject(sendto);
    prepPhrases.add(newpp);

    newpp = getFactory().newPrepositionalPhrase();
    newpp.setPreposition(Constants.Preposition.AS);
    newpp.setIndirectObject(roles);
    prepPhrases.add(newpp);
    reportTask.setPrepositionalPhrases(prepPhrases.elements());

    reportTask.setPlan(getFactory().getRealityPlan());
    reportTask.setSource(getClusterIdentifier());

    TimeAspectValue startTAV = 
      new TimeAspectValue(AspectType.START_TIME, startTime);
    ScoringFunction startScoreFunc = 
      ScoringFunction.createStrictlyAtValue(startTAV);
    Preference startPreference = 
      getFactory().newPreference(AspectType.START_TIME, startScoreFunc);

    TimeAspectValue endTAV = 
      new TimeAspectValue(AspectType.END_TIME, endTime);
    ScoringFunction endScoreFunc = 
      ScoringFunction.createStrictlyAtValue(endTAV);    
    Preference endPreference = 
      getFactory().newPreference(AspectType.END_TIME, endScoreFunc );

    Vector preferenceVector = new Vector(2);
    preferenceVector.addElement(startPreference);
    preferenceVector.addElement(endPreference);

    reportTask.setPreferences(preferenceVector.elements());
    
    reportTask.setVerb(getReportVerb(roles)); 

    return reportTask;
  }
  
  protected Verb getReportVerb(Collection roles) {
    return Constants.Verb.Report;
  }

  protected Asset getAsset(String className, String itemIdentification,
                           String typeIdentification, String clusterName) {

    Asset asset = getFactory().createAsset(className);
  	
    ((NewTypeIdentificationPG)asset.getTypeIdentificationPG()).setTypeIdentification(typeIdentification);

    NewItemIdentificationPG itemIdProp = 
      (NewItemIdentificationPG)asset.getItemIdentificationPG();
    itemIdProp.setItemIdentification(itemIdentification);
    itemIdProp.setNomenclature(itemIdentification);
    
    NewClusterPG cpg = (NewClusterPG)asset.getClusterPG();
    cpg.setClusterIdentifier(ClusterIdentifier.getClusterIdentifier(clusterName));
    
    Asset saved = (Asset) myOtherAssets.get(asset.getKey());
    if (saved == null) {
      myOtherAssets.put(asset.getKey(), asset);
      saved = asset;
    }
    return saved;
  }

  public long parseDate(String dateString) throws ParseException {
    return myDateFormat.parse(dateString).getTime();
  }

  protected Object parseExpr(String type, String arg) {
    int i;

    type = type.trim();
    arg = arg.trim();

    if ((i = type.indexOf("<")) >= 0) {
      int j = type.lastIndexOf(">");
      String ctype = type.substring(0, i).trim();
      String etype = type.substring(i + 1, j).trim();
      Collection c = null;
      if (ctype.equals("Collection") || ctype.equals("List")) {
        c = new ArrayList();
      } else {
        throw new RuntimeException("Unparsable collection type: "+type);
      }

      Vector l = org.cougaar.util.StringUtility.parseCSV(arg);
      for (Iterator it = l.iterator(); it.hasNext();) {
        c.add(parseExpr(etype,(String) it.next()));
      }
      return c;
    } else if ((i = type.indexOf("/")) >= 0) {
      String m = type.substring(0, i).trim();
      String mt = type.substring(i + 1).trim();
      double qty = Double.valueOf(arg).doubleValue();
      return createMeasureObject(m, qty, mt);
    } else {
      Class cl = findClass(type);

      try {
        if (cl.isInterface()) {
          if (TimeSpan.class.isAssignableFrom(cl)) {
            Vector svs =  org.cougaar.util.StringUtility.parseCSV(arg);
            long startTime = getDefaultStartTime();
            long endTime = getDefaultEndTime();
            for (Enumeration sp = svs.elements(); sp.hasMoreElements();) {
              String ss = (String) sp.nextElement();

              int eq = ss.indexOf('=');
              String slotname = ss.substring(0, eq).trim();
              String vspec = ss.substring(eq + 1).trim();
              try {
                long time  = parseDate(vspec);
                if (slotname.equals("startTime")) {
                  startTime = time;
                } else if (slotname.equals("endTime")) {
                  endTime = time;
                }
              } catch (ParseException pe) {
              }
            }
            return new TrivialTimeSpan(startTime, endTime);
          } else {
            // interface means try the COF
            return parseWithCOF(cl, arg);
          }
        } else {
          Class ac = getArgClass(cl);
          Object[] args = {arg};
          Constructor cons = Reflect.getConstructor(ac,stringArgSpec);
          if (cons != null) {
            // found a constructor - use it
            return cons.newInstance(args);
          } else {
            Method fm = Reflect.getMethod(ac, "create", stringArgSpec);
            if (fm == null) {
              String n = ac.getName();
              // remove the package prefix
              n = n.substring(n.lastIndexOf('.') + 1).trim();
              fm = Reflect.getMethod(ac, "create"+n, stringArgSpec);
              if (fm == null) 
                fm = Reflect.getMethod(ac, "get"+n, stringArgSpec);
            }
            if (fm == null) {
              throw new RuntimeException("Couldn't figure out how to construct "+type);
            }
            return fm.invoke(null,args);
          }
        }
      } catch (Exception e) {
        System.err.println("AssetDataPlugIn: Exception constructing "+type+" from \""+arg+"\":");
        e.printStackTrace();
        throw new RuntimeException("Construction problem "+e);
      }
    }
  }

  private static Class[] stringArgSpec = {String.class};

  private static Class[][] argClasses = {{Integer.TYPE, Integer.class},
                                         {Double.TYPE, Double.class},
                                         {Boolean.TYPE, Boolean.class},
                                         {Float.TYPE, Float.class},
                                         {Long.TYPE, Long.class},
                                         {Short.TYPE, Short.class},
                                         {Byte.TYPE, Byte.class},
                                         {Character.TYPE, Character.class}};

                                     
  private static Class getArgClass(Class c) {
    if (! c.isPrimitive()) return c;
    for (int i = 0; i < argClasses.length; i++) {
      if (c == argClasses[i][0])
        return argClasses[i][1];
    }
    throw new IllegalArgumentException("Class "+c+" is an unknown primitive.");
  }

  protected String getType(String type) {
    int i;
    if ((i = type.indexOf("<")) > -1) { // deal with collections 
      int j = type.lastIndexOf(">");
      return getType(type.substring(0, i).trim()); // deal with measures
    } else if ((i = type.indexOf("/")) > -1) {
      return getType(type.substring(0, i).trim());
    } else {
      return type;
    }
  }
    

  protected Object parseWithCOF(Class cl, String val) {
    String name = cl.getName();
    int dot = name.lastIndexOf('.');
    if (dot != -1) 
      name = name.substring(dot + 1).trim();

    try {
      // lookup method on ldmf
      Object o = callFactoryMethod(name);

      Vector svs = org.cougaar.util.StringUtility.parseCSV(val);
      // svs should be a set of strings like "slot=value" or "slot=type value"
      for (Enumeration sp = svs.elements(); sp.hasMoreElements();) {
        String ss = (String) sp.nextElement();

        int eq = ss.indexOf('=');
        String slotname = ss.substring(0, eq).trim();
        String vspec = ss.substring(eq + 1).trim();
        
        int spi = vspec.indexOf(' ');
        Object v;
        if (spi == -1) {
          v = vspec;
        } else {
          String st = vspec.substring(0, spi).trim();
          String sv = vspec.substring(spi + 1).trim();
          v = parseExpr(st, sv);
        }
        callSetMethod(o, slotname, v);
      }
      return o;
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  private Object callFactoryMethod(String ifcname) {
    // look up a zero-arg factory method in the ldmf
    String newname = "new"+ifcname;
    
    Collection keys = DomainManager.keySet();
    for (Iterator i = keys.iterator(); i.hasNext(); ) {
      String key = (String) i.next();
      try {
        getFactory(key);
        Class ldmfc = getFactory(key).getClass();
        Method fm = ldmfc.getMethod(newname,nullClassList);
        return fm.invoke(getFactory(), nullArgList);
      } catch (NoSuchMethodException nsme) {
        // This is okay - just try the next factory
        //System.out.println("Unable to find " + newname + " method in factory for " + key);
      } catch (Exception e) { 
        synchronized (System.err) {
          System.err.println("Problem loading Domain Factory \""+key+"\": ");
          e.printStackTrace(); 
        }
      }
    }
      
    throw new RuntimeException ("Couldn't find a factory method for "+ifcname);
  }
  private static final Class nullClassList[] = {};
  private static final Object nullArgList[] = {};

  private void callSetMethod(Object o, String slotname, Object value) {
    Class oc = o.getClass();
    String setname = "set"+slotname;
    Class vc = value.getClass();

    try {
      Method ms[] = Reflect.getMethods(oc);
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
      throw new RuntimeException("Couldn't find set"+slotname+" for "+o+", value "+value);
    }

    throw new RuntimeException("Couldn't find set"+slotname+" for "+o+", value "+value);
  }

  /**
   * Returns the integer value for the appropriate
   * unitOfMeasure field in the measureClass
   */
  protected int getMeasureUnit(String measureClass, String unitOfMeasure) {
    try {
      String fullClassName = "org.cougaar.planning.ldm.measure." + measureClass;
      Field f = Class.forName(fullClassName).getField(unitOfMeasure);
      return f.getInt(null);
    } catch (Exception e) {
      System.err.println("AssetDataPlugIn Exception: for measure unit: " + 
                         unitOfMeasure);
      e.printStackTrace();
    }
    return -1;
  }

  /**
   * Returns a measure object which is an instance of className and has
   * a quantity of unitOfMeasure
   */
  protected Object createMeasureObject(String className, double quantity, String unitOfMeasure) {
    try {
      Class classObj = Class.forName("org.cougaar.planning.ldm.measure." + className);
      String methodName = "new" + className;
      Class parameters[] = {double.class, int.class};
      Method meth = classObj.getMethod(methodName, parameters);
      Object arguments[] = {new Double(quantity), new Integer(getMeasureUnit(className, unitOfMeasure))};
      return meth.invoke(classObj, arguments); // static method call
    } catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  private static HashMap classes;
  protected static Collection packages = new ArrayList();

  static {
    // initialize packages:
    packages.add("org.cougaar.planning.ldm.measure");
    packages.add("org.cougaar.planning.ldm.plan");
    packages.add("org.cougaar.planning.ldm.asset");
    packages.add("org.cougaar.planning.ldm.oplan");

    packages.add("java.lang");  // extras for fallthrough
    packages.add("java.util");

    // initialize the classmap with some common ones
    classes = new HashMap();

    classes.put("ClusterIdentifier", ClusterIdentifier.class);

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
    classes.put("TimeSpan", TimeSpan.class);
                                           
    // COUGAAR-specific stuff will be looked for
  }

  private Class findClass(String name) {
    synchronized (classes) {
      Class c = (Class) classes.get(name);
      // try the cache
      if (c != null) return c;

      for (Iterator i = packages.iterator(); i.hasNext();) {
        String pkg = (String) i.next();
        try {                   // Oh so ugly!
          c = Class.forName(pkg+"."+name);
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
  protected void callSetter(Object classInstance, String setterName, String type, Object []arguments) {
    Class parameters[] = new Class[1];
    
    try {
      parameters[0] = findClass(type);
      Method meth = findMethod(classInstance.getClass(), setterName, parameters);
      if (meth == null) {
        StringBuffer msg = new StringBuffer();
        msg.append("AssetDataPlugIn method not found: ");
        msg.append(setterName);
        msg.append("(");
        for (int i = 0; i < parameters.length; i++) {
          if (i > 0) msg.append(", ");
          msg.append(parameters[i].getName());
        }
        msg.append(")");
        System.err.println(msg);
        return;
      }
      meth.invoke(classInstance, arguments);
    } catch (Exception e) {
      System.err.println("AssetDataPlugIn Exception: callSetter("+classInstance.getClass().getName()+", "+setterName+", "+type+", "+arguments+" : " + e);
      e.printStackTrace();
    }
  }

  private static Method findMethod(Class c, String name, Class params[]) {
    Method ms[] = Reflect.getMethods(c);
    int pl = params.length;
    for (int i = 0; i < ms.length; i++) {
      Method m = ms[i];
      if (name.equals(m.getName())) {
        Class mps[] = m.getParameterTypes();
        if (mps.length == pl) {
          int j;
          for (j = 0; j < pl; j++) {
            if (!(mps[j].isAssignableFrom(params[j]))) 
              break;            // j loop
          }
          if (j==pl)            // all passed
            return m;
        }
      }
    }
    return null;
  }
  
  private static class TrivialTimeSpan implements TimeSpan {
    long myStart;
    long myEnd;

    public TrivialTimeSpan(long start, long end) {
      myStart = start;
      myEnd = end;
    }

    public long getStartTime() {
      return myStart;
    }

    public long getEndTime() {
      return myEnd;
    }
  }

  private class AssetDataCallbackImpl implements AssetDataCallback {
    public ConfigFinder getConfigFinder() {
      return AssetDataPlugInBase.this.getConfigFinder();
    }
    public void createMyLocalAsset(String assetClassName) {
      AssetDataPlugInBase.this.createMyLocalAsset(assetClassName);
    }
    public Asset getMyLocalAsset() {
      return myLocalAsset;
    }
    public RootFactory getFactory() {
      return AssetDataPlugInBase.this.getFactory();
    }
    public Object parseExpr(String dataType, String value) {
      return AssetDataPlugInBase.this.parseExpr(dataType, value);
    }
    public long parseDate(String dateString) throws ParseException {
      return AssetDataPlugInBase.this.parseDate(dateString);
    }
    public String getType(String type) {
      return AssetDataPlugInBase.this.getType(type);
    }
    public void callSetter(Object classInstance, String setterName,
                           String type, Object[] arguments)
    {
      AssetDataPlugInBase.this.callSetter(classInstance, setterName, type, arguments);
    }
    public void setLocationSchedule(String latStr, String lonStr) {
      AssetDataPlugInBase.this.setLocationSchedule(latStr, lonStr);
    }
    public long getDefaultStartTime() {
      return AssetDataPlugInBase.this.getDefaultStartTime();
    }
    public long getDefaultEndTime() {
      return AssetDataPlugInBase.this.getDefaultEndTime();
    }
    public void addRelationship(String typeId, String itemId,
                                String otherClusterId, String roleName,
                                long start, long end)
    {
      AssetDataPlugInBase.this.addRelationship(typeId, itemId, otherClusterId, roleName, start, end);
    }
  }
}
