/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
package org.cougaar.domain.planning.plugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

import java.text.DateFormat;

import java.util.*;

import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.core.cluster.IncrementalSubscription;

import org.cougaar.core.plugin.SimplePlugIn;

import org.cougaar.domain.planning.Constants;

import org.cougaar.domain.planning.ldm.asset.Asset;
import org.cougaar.domain.planning.ldm.asset.ClusterPG;
import org.cougaar.domain.planning.ldm.asset.CommunityPGImpl;
import org.cougaar.domain.planning.ldm.asset.ItemIdentificationPGImpl;
import org.cougaar.domain.planning.ldm.asset.NewClusterPG;
import org.cougaar.domain.planning.ldm.asset.NewCommunityPG;
import org.cougaar.domain.planning.ldm.asset.NewItemIdentificationPG;
import org.cougaar.domain.planning.ldm.asset.NewPropertyGroup;
import org.cougaar.domain.planning.ldm.asset.NewRelationshipPG;
import org.cougaar.domain.planning.ldm.asset.RelationshipBG;
import org.cougaar.domain.planning.ldm.asset.NewTypeIdentificationPG;
import org.cougaar.domain.planning.ldm.asset.PropertyGroup;
import org.cougaar.domain.planning.ldm.asset.PropertyGroupSchedule;

import org.cougaar.domain.planning.ldm.plan.AspectType;
import org.cougaar.domain.planning.ldm.plan.HasRelationships;
import org.cougaar.domain.planning.ldm.plan.NewPrepositionalPhrase;
import org.cougaar.domain.planning.ldm.plan.NewRoleSchedule;
import org.cougaar.domain.planning.ldm.plan.NewSchedule;
import org.cougaar.domain.planning.ldm.plan.NewTask;
import org.cougaar.domain.planning.ldm.plan.Preference;
import org.cougaar.domain.planning.ldm.plan.Relationship;
import org.cougaar.domain.planning.ldm.plan.Role;
import org.cougaar.domain.planning.ldm.plan.Schedule;
import org.cougaar.domain.planning.ldm.plan.ScoringFunction;
import org.cougaar.domain.planning.ldm.plan.TimeAspectValue;

import org.cougaar.util.Reflect;
import org.cougaar.util.TimeSpan;

/**
 * Parses local asset prototype-ini.dat to create local asset and the Report tasks
 * associated with all the local asset's relationships. Local asset must have ClusterPG and 
 * RelationshipPG, Presumption is that the 'other' assets in all the 
 * relationships have both Cluster and Relationship PGs.
 * Currently assumes that each Cluster has exactly 1 local asset.
 *
 * Format:
 * <xxx> - parameter
 * # - comment character - rest of line ignored
 *
 * Skeleton form:
 * [Prototype]
 *  <asset_class_name> # asset class must have both a ClusterPG and a RelationshipPG
 *
 * [Relationship]
 * # <role> specifies Role played by this asset for another asset. 
 * # If start/end be specified as "", they default to 
 * # TimeSpan.MIN_VALUE/TimeSpan.MAX_VALUE
 * <role> <other asset item id> <other asset type id> <other asset cluster id> <relationship start time> <relationship end time>
 *
 * [<PG name>]
 * # <slot type> - one of Collection<data type>, List<data type>, String, Integer, Double, Boolean,
 * #  Float, Long, Short, Byte, Character 
 * 
 * <slot name> <slot type> <slot values>
 *
 * Sample:
 * [Prototype]
 * Entity
 *
 * [Relationship]
 * "Subordinate"   "Headquarters"        "Management"   "HQ"           "01/01/2001 12:00 am"  "01/01/2010 11:59 pm"
 * "PaperProvider" "Beth's Day Care"     "Day Care Ctr" "Beth's Home"  "02/13/2001 9:00 am"   "" 
 *
 * [ItemIdentificationPG]
 * ItemIdentification String "Staples, Inc"
 * Nomenclature String "Staples"
 * AlternateItemIdentification String "SPLS"
 *
 * [TypeIdentificationPG]
 * TypeIdentification String "Office Goods Supplier"
 * Nomenclature String "Big Box"
 * AlternateTypeIdentification String "Stationer"
 * 
 * [ClusterPG]
 * ClusterIdentifier ClusterIdentifier "Staples"
 * 
 * [EntityPG]
 * Roles Collection<Role> "Subordinate, PaperProvider, CrayonProvider, PaintProvider"
 * 
 **/
public class AssetDataPlugIn extends SimplePlugIn {
  public static final String SELF = ("Self");

  private static TrivialTimeSpan ETERNITY = 
    new TrivialTimeSpan(TimeSpan.MIN_VALUE,
                        TimeSpan.MAX_VALUE);

  public long getDefaultStartTime() {
    return TimeSpan.MIN_VALUE;
  }

  public long getDefaultEndTime() {
    return TimeSpan.MAX_VALUE;
  }

  public String getFileName(String clusterId) {
    return clusterId + "-prototype-ini.dat";
  }

  private DateFormat myDateFormat = DateFormat.getInstance(); 

  private String myAssetClassName = null;
  private ArrayList myRelationships = new ArrayList();
  private HashMap myOtherAssets = new HashMap();
  private Asset myLocalAsset = null;

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
      ParsePrototypeFile(cId);


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
  

  protected void report(Relationship relationship) {
    Asset sendTo = 
      (((Asset) relationship.getA()).getKey().equals(myLocalAsset.getKey())) ?
      getFactory().cloneInstance((Asset) relationship.getB()) :
      getFactory().cloneInstance((Asset) relationship.getA());
    
    Asset localClone = getFactory().cloneInstance(myLocalAsset);

    ArrayList roles = new ArrayList(1);
    Role role = 
      (((Asset) relationship.getA()).getKey().equals(myLocalAsset.getKey())) ?
      relationship.getRoleA() : relationship.getRoleB();
    roles.add(role);
    
    publish(createReportTask(localClone, sendTo, roles, 
                             relationship.getStartTime(),
                             relationship.getEndTime()));
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
    
    reportTask.setVerb(Constants.Verb.Report);

    return reportTask;
  }
  
  protected Asset getAsset(String className, String itemIdentification,
                           String typeIdentification, String clusterName) {

    Asset asset = getFactory().createAsset(className);
  	
    ((NewTypeIdentificationPG)asset.getTypeIdentificationPG()).setTypeIdentification(typeIdentification);

    NewItemIdentificationPG itemIdProp = 
      (NewItemIdentificationPG)asset.getItemIdentificationPG();
    itemIdProp.setItemIdentification(itemIdentification);
    
    NewClusterPG cpg = (NewClusterPG)asset.getClusterPG();
    cpg.setClusterIdentifier(ClusterIdentifier.getClusterIdentifier(clusterName));
    
    Asset saved = (Asset) myOtherAssets.get(asset.getKey());
    if (saved == null) {
      myOtherAssets.put(asset.getKey(), asset);
      saved = asset;
    }
    return saved;
  }
  

   
	
  private void publish(Object o) {
    publishAdd(o);
  }


  /**
   * 
   */
  protected void ParsePrototypeFile(String clusterId) {
    String dataItem = "";
    int newVal;

    String filename = getFileName(clusterId);
    BufferedReader input = null;
    Reader fileStream = null;

    try {
      fileStream = 
        new InputStreamReader(getConfigFinder().open(filename));
      input = new BufferedReader(fileStream);
      StreamTokenizer tokens = new StreamTokenizer(input);
      tokens.commentChar('#');
      tokens.wordChars('[', ']');
      tokens.wordChars('_', '_');
      tokens.wordChars('<', '>');      
      tokens.wordChars('/', '/');      
      tokens.ordinaryChars('0', '9');      
      tokens.wordChars('0', '9');      

      newVal = tokens.nextToken();
      // Parse the prototype-ini file
      while (newVal != StreamTokenizer.TT_EOF) {
        if (tokens.ttype == StreamTokenizer.TT_WORD) {
          dataItem = tokens.sval;
          if (dataItem.equals("[Prototype]")) {
            newVal = tokens.nextToken();
            myAssetClassName = tokens.sval;
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

            newVal = tokens.nextToken();
          } else if (dataItem.equals("[Relationship]")) {
            newVal = fillRelationships(newVal, tokens);
          } else if (dataItem.substring(0,1).equals("[")) {
            // We've got a property or capability
            newVal = setPropertyForAsset(myLocalAsset, dataItem, newVal, tokens);
          } else {
            // if The token you read is not one of the valid
            // choices from above
            System.err.println("AssetDataPlugIn Incorrect token: " + dataItem);
          }
        } else {
          System.out.println("ttype: " + tokens.ttype + " sval: " + tokens.sval);
          throw new RuntimeException("Format error in \""+filename+"\".");
        }
      }


      publish(myLocalAsset);

      // Closing BufferedReader
      if (input != null)
	input.close();

      //only generates a NoSuchMethodException for AssetSkeleton because of a coding error
      //if we are successul in creating it here  it then the AssetSkeletomn will end up with two copies
      //the add/search criteria in AssetSkeleton is for a Vecotr and does not gurantee only one instance of 
      //each class.  Thus the Org allocator plugin fails to recognixe the correct set of cpabilities.
      
    } catch (Exception e) {
      e.printStackTrace();
    }
  } 

  private Object parseExpr(String type, String arg) {
    int i;
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

      Vector l = org.cougaar.util.StringUtility.parseCSV(arg);
      for (Iterator it = l.iterator(); it.hasNext();) {
        c.add(parseExpr(etype,(String) it.next()));
      }
      return c;
    } else if ((i = type.indexOf("/")) >= 0) {
      String m = type.substring(0,i);
      String mt = type.substring(i+1);
      double qty = Double.valueOf(arg).doubleValue();
      return createMeasureObject(m, qty, mt);
    } else {
      Class cl = findClass(type);

      try {
        if (cl.isInterface()) {
          // interface means try the COF
          return parseWithCOF(cl, arg);
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
              n = n.substring(n.lastIndexOf('.')+1);
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

  private String getType(String type) {
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
    

  protected Object parseWithCOF(Class cl, String val) {
    String name = cl.getName();
    int dot = name.lastIndexOf('.');
    if (dot != -1) name = name.substring(dot+1);

    try {
      // lookup method on ldmf
      Object o = callFactoryMethod(name);

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
    
    // try the COUGAAR factory
    try {
      Class ldmfc = getFactory().getClass();
      Method fm = ldmfc.getMethod(newname,nullClassList);
      return fm.invoke(getFactory(), nullArgList);
    } catch (Exception e) {
      e.printStackTrace();
    }

    // try the main factory
    try {
      Class ldmfc = getFactory().getClass();
      Method fm = ldmfc.getMethod(newname,nullClassList);
      return fm.invoke(getFactory(), nullArgList);
    } catch (Exception e) {
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
   * Creates the property, fills in the slots based on what's in the prototype-ini file
   * and then sets it for (or adds it to) the asset
   */
  protected int setPropertyForAsset(Asset asset, String prop, int newVal, StreamTokenizer tokens) {
    String propertyName = prop.substring(1, prop.length()-1);
    if (asset != null) {
      NewPropertyGroup property = null;
      try {
	property = (NewPropertyGroup)getFactory().createPropertyGroup(propertyName);
      } catch (Exception e) {
	System.err.println("AssetDataPlugIn: Unrecognized keyword for a prototype-ini file: [" + propertyName + "]");
      }
      try {
	newVal = tokens.nextToken();
	String member = tokens.sval;
	String propName = "New" + propertyName;
	// Parse through the property section of the file
	while (newVal != StreamTokenizer.TT_EOF) {
	  if ((tokens.ttype == StreamTokenizer.TT_WORD) && !(tokens.sval.substring(0,1).equals("["))) {
	    newVal = tokens.nextToken();
	    String dataType = tokens.sval;
	    newVal = tokens.nextToken();
	    // Call appropriate setters for the slots of the property
            Object arg = parseExpr(dataType, tokens.sval);

            createAndCallSetter(property, propName, "set" + member, 
                                getType(dataType), arg);
	    newVal = tokens.nextToken();
	    member = tokens.sval;
	  } else {
	    // Reached a left bracket "[", want to exit block
	    break;
	  }
	} //while

	// Add the property to the asset
        /*
	try {
        */
	asset.addOtherPropertyGroup(property);
        /*
	} catch (Exception e) {
          
	  e.printStackTrace();
          } */
      } catch (Exception e) {
        e.printStackTrace();
      }
    } else {
      System.err.println("AssetDataPlugIn Error: asset is null");
    }
    return newVal;
  }

  /**
   * Fills in myRelationships with arrays of relationship, clusterName and capableroles triples.
   */
  protected int fillRelationships(int newVal, StreamTokenizer tokens) {
    if (myLocalAsset != null) {
      try {
        newVal = tokens.nextToken();
	while ((newVal != StreamTokenizer.TT_EOF) &&
               (!tokens.sval.substring(0,1).equals("["))) {

          String roleName = "";
          String itemID = "";
          String typeID = "";
          String clusterID = "";
          long start = getDefaultStartTime();
          long end = getDefaultEndTime();
          
          System.out.println("Parsing new relationship");
          for (int i = 0; i < 6; i++) {
            System.out.println("Token: " + tokens.sval + " newVal: " + newVal);

            if ((tokens.sval.length()) > 0  &&
                (tokens.sval.substring(0,1).equals("["))) {
              throw new RuntimeException("Unexpected character: " + 
                                         tokens.sval);
            }
            
            switch (i) {
            case 0:
              roleName= tokens.sval;
              break;

            case 1:
              itemID = tokens.sval;
              break;

            case 2:
              typeID = tokens.sval;
              break;

            case 3:
              clusterID = tokens.sval;
              break;

            case 4:
              try {
                start = myDateFormat.parse(tokens.sval).getTime();
              } catch (java.text.ParseException pe) {
                System.out.println("Unable to parse: " + tokens.sval);
                pe.printStackTrace();
              }

            case 5:
              try {
                end = myDateFormat.parse(tokens.sval).getTime();
              } catch (java.text.ParseException pe) {
                System.out.println("Unable to parse: " + tokens.sval);
                pe.printStackTrace();
              }

              break;
            }

            newVal = tokens.nextToken();
          }

	  // Parse [Relationship] part of prototype-ini file
          Asset otherAsset = getAsset(myAssetClassName, itemID, typeID, clusterID);

          System.out.println("itemID: " + itemID + " typeID: " + typeID +
                             " clusterID: " + clusterID + 
                             " role: " + roleName +
                             " start: " + new Date(start) +
                             " end: " + new Date(end));

          Relationship relationship = 
            getFactory().newRelationship(Role.getRole(roleName),
                                         (HasRelationships) myLocalAsset,
                                         (HasRelationships) otherAsset,
                                         start,
                                         end);
            
                                           
          myRelationships.add(relationship);
	} //while
      } catch (java.io.IOException ioe) {
        ioe.printStackTrace();
      } 
    } else {
      System.err.println("AssetDataPlugIn.fillRelationships: local asset is null");
    }

    return newVal;
  }

  /**
   * Returns the integer value for the appropriate
   * unitOfMeasure field in the measureClass
   */
  protected int getMeasureUnit(String measureClass, String unitOfMeasure) {
    try {
      String fullClassName = "org.cougaar.domain.planning.ldm.measure." + measureClass;
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
      Class classObj = Class.forName("org.cougaar.domain.planning.ldm.measure." + className);
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
  private static final Collection packages;

  static {
    // initialize packages:
    packages = new ArrayList();
    packages.add("org.cougaar.domain.planning.ldm.measure");
    packages.add("org.cougaar.domain.planning.ldm.plan");
    packages.add("org.cougaar.domain.planning.ldm.asset");
    packages.add("org.cougaar.domain.planning.ldm.oplan");

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
  protected void createAndCallSetter(Object classInstance, String className, String setterName, String type, Object value) {
    Class parameters[] = new Class[1];
    Object arguments[] = new Object[] {value};
    
    try {
      parameters[0] = findClass(type);
      Class propertyClass = findClass(className);
      //Method meth = propertyClass.getMethod(setterName, parameters);
      Method meth = findMethod(propertyClass, setterName, parameters);
      meth.invoke(classInstance, arguments);
    } catch (Exception e) {
      System.err.println("AssetDataPlugIn Exception: createAndCallSetter("+classInstance.getClass().getName()+", "+className+", "+setterName+", "+type+", "+value+" : " + e);
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

}

