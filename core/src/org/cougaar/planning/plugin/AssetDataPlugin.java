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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

import java.text.DateFormat;

import java.util.*;

import org.cougaar.util.StateModelException;

import org.cougaar.core.agent.ClusterIdentifier;
import org.cougaar.core.blackboard.IncrementalSubscription;
import org.cougaar.core.plugin.SimplePlugin;
import org.cougaar.core.service.DomainService;

import org.cougaar.planning.Constants;
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
import org.cougaar.planning.ldm.plan.AspectValue;
import org.cougaar.planning.ldm.plan.HasRelationships;
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
public class AssetDataPlugin extends SimplePlugin {
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

 
  public void load(Object object) throws StateModelException {
    super.load(object);
    if (!didRehydrate()) {
      try {
        System.out.println(getClusterIdentifier().toString() + ": processing assets in load");
        openTransaction();
        processAssets();
      } catch (Exception e) {
        synchronized (System.err) {
          System.err.println(getClusterIdentifier().toString()+"/"+this+" caught "+e);
          e.printStackTrace();
        }
      } finally {
        closeTransactionDontReset();
      }
    }
  }

  protected void setupSubscriptions() {
    getSubscriber().setShouldBePersisted(false);

    /*
    if (!didRehydrate()) {
      processAssets();	// Objects should already exist after rehydration
      }*/
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

    AspectValue startTAV = 
      TimeAspectValue.create(AspectType.START_TIME, startTime);
    ScoringFunction startScoreFunc = 
      ScoringFunction.createStrictlyAtValue(startTAV);
    Preference startPreference = 
      getFactory().newPreference(AspectType.START_TIME, startScoreFunc);

    AspectValue endTAV = 
      TimeAspectValue.create(AspectType.END_TIME, endTime);
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
    // Nomenclature defaults to itemIdentification
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
          } else if (dataItem.equals("[LocationSchedulePG]")) {
            // parser language is currently incapable of expressing a 
            // complex schedule, so here we hack in some minimal support.
            newVal = 
              setLocationSchedulePG(
                myLocalAsset, dataItem, newVal, tokens);
          } else if (dataItem.substring(0, 1).equals("[")) {
            // We've got a property or capability
            newVal = setPropertyForAsset(myLocalAsset, dataItem, newVal, tokens);
          } else {
            // if The token you read is not one of the valid
            // choices from above
            System.err.println("AssetDataPlugin Incorrect token: " + 
                               dataItem);
            throw new RuntimeException("Format error in \""+filename+"\".");
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
                long time  = myDateFormat.parse(vspec).getTime();
                if (slotname.equals("startTime")) {
                  startTime = time;
                } else if (slotname.equals("endTime")) {
                  endTime = time;
                }
              } catch (java.text.ParseException pe) {
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
        System.err.println("AssetDataPlugin: Exception constructing "+type+" from \""+arg+"\":");
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
    
    DomainService domainService = 
      (DomainService) getDelegate().getServiceBroker().getService(this, DomainService.class, null);

    if (domainService == null) {
      throw new RuntimeException ("Unable to get DomainService");
    }

    List factories = domainService.getFactories();
    for (Iterator i = factories.iterator(); i.hasNext(); ) {
      try {
        Class ldmfc = i.next().getClass();
        Method fm = ldmfc.getMethod(newname,nullClassList);
        return fm.invoke(getFactory(), nullArgList);
      } catch (NoSuchMethodException nsme) {
        // This is okay - just try the next factory
      } catch (Exception e) { 
        synchronized (System.err) {
          System.err.println("Problem loading Domain Factory");
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
   * Creates the property, fills in the slots based on what's in the prototype-ini file
   * and then sets it for (or adds it to) the asset
   */
  protected int setPropertyForAsset(Asset asset, String prop, int newVal, StreamTokenizer tokens) {
    String propertyName = prop.substring(1, prop.length()-1).trim();
    if (asset != null) {
      NewPropertyGroup property = null;
      try {
	property = 
          (NewPropertyGroup)getFactory().createPropertyGroup(propertyName);
      } catch (Exception e) {
	System.err.println("AssetDataPlugin: Unrecognized keyword for a prototype-ini file: [" + propertyName + "]");
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
            Object [] args = new Object[] {parseExpr(dataType, tokens.sval)};
            callSetter(property, "set" + member, getType(dataType), args);
	    newVal = tokens.nextToken();
	    member = tokens.sval;
	  } else {
	    // Reached a left bracket "[", want to exit block
	    break;
	  }
	} //while

	// Add the property to the asset
	asset.addOtherPropertyGroup(property);
      } catch (Exception e) {
        e.printStackTrace();
        throw new RuntimeException("AssetDataPlugin: unable to parse " + 
                                   getFileName(getClusterIdentifier().getAddress()));
      }
    } else {
      System.err.println("AssetDataPlugin Error: asset is null");
    }
    return newVal;
  }


  /**
   * Hack to attach a LocationSchedulePG to an Asset.
   * <pre>
   * For now we only support a single LocationScheduleElementImpl
   * which has a LatLonPointImpl as it's Location.  The TimeSpan
   * is hard-coded to TimeSpan.MIN_VALUE .. TimeSpan.MAX_VALUE.
   * In the future this can be enhanced to support full location
   * schedules, but that would likely require a new file format.
   * 
   * The format is:
   *   "FixedLocation \"(" + LATITUDE + ", " + LONGITUDE + ")\""
   *
   * For example, all of time at latitude 12.3 longitude -45.6:
   *   FixedLocation "(12.3, -45.6)"
   * </pre>
   */
  protected int setLocationSchedulePG(
      Asset asset, String prop, int newVal, StreamTokenizer tokens) {

    // check asset
    if (asset == null) {
      System.err.println("AssetDataPlugin Error: asset is null");
      return newVal;
    }

    // read two strings
    String firstStr;
    String secondStr;
    try {
      newVal = tokens.nextToken();
      if ((newVal == StreamTokenizer.TT_EOF) ||
          (tokens.sval.substring(0,1).equals("["))) {
        // Reached a left bracket "[", want to exit block
        return newVal;
      }
      firstStr = tokens.sval;

      newVal = tokens.nextToken();
      if ((newVal == StreamTokenizer.TT_EOF) ||
          (tokens.sval.substring(0,1).equals("["))) {
        // Reached a left bracket "[", want to exit block
        return newVal;
      }
      secondStr = tokens.sval;

      newVal = tokens.nextToken();
    } catch (java.io.IOException ioe) {
      return StreamTokenizer.TT_EOF;
    }

    // skip "FixedLocation " string
    if (!(firstStr.equals("FixedLocation"))) {
      System.err.println(
          "Expecting: FixedLocation \"(LAT, LON)\"\n"+
          "Not: "+firstStr+" .. ");
      return newVal;
    }

    // parse single Location
    org.cougaar.planning.ldm.plan.Location loc;
    if ((!(secondStr.startsWith("("))) ||
        (!(secondStr.endsWith(")"))))  {
      System.err.println(
          "Expecting: FixedLocation \"(LAT, LON)\"\n"+
          "Not: FixedLocation "+secondStr+" ..");
      System.err.println("SWith(: "+secondStr.startsWith("("));
      System.err.println("EWith): "+secondStr.endsWith(")"));
      return newVal;
    }
    String locStr = 
      secondStr.substring(
          1, secondStr.length()-1);
    try {
      int sepIdx = locStr.indexOf(",");

      String latStr = locStr.substring(0, sepIdx).trim();
      org.cougaar.planning.ldm.measure.Latitude lat = 
        org.cougaar.planning.ldm.measure.Latitude.newLatitude(
            latStr);

      String lonStr = locStr.substring(sepIdx+1).trim();
      org.cougaar.planning.ldm.measure.Longitude lon = 
        org.cougaar.planning.ldm.measure.Longitude.newLongitude(
            lonStr);

      loc = 
        new org.cougaar.planning.ldm.plan.LatLonPointImpl(
            lat, lon);
    } catch (RuntimeException e) {
      System.err.println("Invalid LatLonPoint: "+locStr);
      return newVal;
    }

    // create LocationScheduleElementImpl
    LocationScheduleElement locSchedElem =
      new LocationScheduleElementImpl(
          TimeSpan.MIN_VALUE, TimeSpan.MAX_VALUE, loc);

    // add to schedule
    LocationSchedulePG locSchedPG = asset.getLocationSchedulePG();
    if (locSchedPG == null) {
      locSchedPG = new LocationSchedulePGImpl();
      asset.setLocationSchedulePG(locSchedPG);
    }
    Schedule locSched = locSchedPG.getSchedule();
    if (locSched == null) {
      locSched = new ScheduleImpl();
      ((NewLocationSchedulePG)locSchedPG).setSchedule(locSched);
    }
    locSched.add(locSchedElem);

    // done
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
          
          for (int i = 0; i < 6; i++) {
            if ((tokens.sval.length()) > 0  &&
                (tokens.sval.substring(0,1).equals("["))) {
              throw new RuntimeException("Unexpected character: " + 
                                         tokens.sval);
            }
            
            switch (i) {
            case 0:
              roleName = tokens.sval.trim();
              break;

            case 1:
              itemID = tokens.sval.trim();
              break;

            case 2:
              typeID = tokens.sval.trim();
              break;

            case 3:
              clusterID = tokens.sval.trim();
              break;

            case 4:
              if (!tokens.sval.equals("")) {
                try {
                  start = myDateFormat.parse(tokens.sval).getTime();
                } catch (java.text.ParseException pe) {
                  System.out.println("Unable to parse: " + tokens.sval + 
                                     ". Start time defaulting to " + 
                                     getDefaultStartTime());
                }
              }
              break;

            case 5:
              if (!tokens.sval.equals("")) {
                try {
                  end = myDateFormat.parse(tokens.sval).getTime();
                } catch (java.text.ParseException pe) {
                  System.out.println("Unable to parse: " + tokens.sval + 
                                     ". End time defaulting to " + 
                                     getDefaultEndTime());
                }
              }
              break;

            }

            newVal = tokens.nextToken();
          }

	  // Parse [Relationship] part of prototype-ini file
          Asset otherAsset = getAsset(myAssetClassName, itemID, typeID, clusterID);

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
      System.err.println("AssetDataPlugin.fillRelationships: local asset is null");
    }

    return newVal;
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
      System.err.println("AssetDataPlugin Exception: for measure unit: " + 
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
      meth.invoke(classInstance, arguments);
    } catch (Exception e) {
      System.err.println("AssetDataPlugin Exception: callSetter("+classInstance.getClass().getName()+", "+setterName+", "+type+", "+arguments+" : " + e);
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




