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
/* hand generated!
 */

package org.cougaar.domain.planning.ldm.asset;

import org.w3c.dom.Element;
import org.w3c.dom.Document;

import org.cougaar.domain.planning.ldm.asset.*;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.util.*;
import org.cougaar.domain.planning.ldm.plan.RoleSchedule;
import org.cougaar.domain.planning.ldm.plan.Schedule;
import org.cougaar.domain.planning.ldm.plan.PlanElement;
import org.cougaar.domain.planning.ldm.plan.AssetTransfer;
import org.cougaar.core.util.XMLizable;
import org.cougaar.core.society.UniqueObject;
import org.cougaar.core.society.UID;
import org.cougaar.core.cluster.ClusterContext;
import org.cougaar.core.cluster.ClusterContextTable;
import org.cougaar.core.cluster.ClusterIdentifier;
import org.cougaar.core.cluster.Publishable;
import org.cougaar.core.cluster.ChangeReport;
import org.cougaar.domain.planning.ldm.LDMServesPlugIn;
import org.cougaar.domain.planning.ldm.Factory;
import org.cougaar.domain.planning.ldm.RootFactory;

// only for transition period
import org.cougaar.domain.planning.ldm.plan.RoleScheduleImpl;

import org.cougaar.core.util.XMLize;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
       
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.cougaar.core.society.MessageAddress;

import java.beans.PropertyDescriptor;
import java.beans.IndexedPropertyDescriptor;
import java.beans.IntrospectionException;

/**
 * Base class for all instantiable assets.
 **/

public class Asset extends org.cougaar.domain.planning.ldm.asset.AssetSkeleton
  implements Cloneable, XMLizable, UniqueObject, Publishable {
  private transient RoleSchedule roleschedule;
  private static final String AGGREGATE_TYPE_ID = "AggregateAsset" ;
  


  public Asset() {
    myPrototype = null;         // no prototype, by default

    myItemIdentificationPG = PropertyGroupFactory.newItemIdentificationPG();
    myTypeIdentificationPG = PropertyGroupFactory.newTypeIdentificationPG();
    initRoleSchedule();
  }


  protected Asset(Asset prototype) {
    super(prototype);
    // my prototype is who we're based on
    myPrototype = prototype;
    // I always get my own item ID which starts out as a copy of me.
    myItemIdentificationPG=(ItemIdentificationPG)prototype.getItemIdentificationPG().copy();
    // instead, lets share the itemID, but make sure it is locked.
    //myItemIdentificationPG=(ItemIdentificationPG)prototype.getItemIdentificationPG().lock();
    // share the typeID with proto
    myTypeIdentificationPG=prototype.getTypeIdentificationPG();
    // new roleschedule
    initRoleSchedule();
  }

  /** Create an instance of a prototype asset **/
  public Asset createInstance() {
    return new Asset(this);
  }

  /** Create an instance of a prototype asset, giving the new instance
   * an itemid of id.
   **/
  public final Asset createInstance(String id) {
    Asset a = createInstance();
    ((NewItemIdentificationPG)a.getItemIdentificationPG()).setItemIdentification(id);
    return a;
  }

  private UID uid= null;
  public UID getUID() { return uid; }
  public void setUID(UID uid) { this.uid = uid; }

  /** return all the properties as a new vector */
  public Vector fetchAllProperties() {
    Vector v = new Vector();
    fillAllPropertyGroups(v);
    return v;
  }

  /** return all the properties as a new vector */
  public Vector fetchAllProperties(long time) {
    Vector v = new Vector();
    fillAllPropertyGroups(v, time);
    return v;
  }

  protected void fillAllPropertyGroups(Vector v) {
    // stick ours on the front - all others add to the end.
    v.addElement(myItemIdentificationPG);
    v.addElement(myTypeIdentificationPG);
    super.fillAllPropertyGroups(v);
  }

  protected void fillAllPropertyGroups(Vector v, long time) {
    // stick ours on the front - all others add to the end.
    v.addElement(myItemIdentificationPG);
    v.addElement(myTypeIdentificationPG);
    super.fillAllPropertyGroups(v, time);
  }


  /** For internal use only - use Factory.copyInstance() instead.
   * extended
   **/
  public Asset copy() {
    try {
      return (Asset) clone();
    } catch (CloneNotSupportedException cnse) {
      cnse.printStackTrace();
      return null;
    }
  }

  /** For internal use only - use Factory.copyInstance() instead.
   * extended
   **/
  public Object clone() throws CloneNotSupportedException {
    Asset a = instanceForCopy();
    
    // bind the new asset to our LDM
    a.bindToLDM(getBoundLDM());

    a.privatelySetPrototype(getPrototype());
    // set the itemID to a copy of the original
    a.setItemIdentificationPG(getItemIdentificationPG().copy());
    // share the typeid
    a.setTypeIdentificationPG(getTypeIdentificationPG().lock());
    // share the uid (for now, at least!)
    a.setUID(getUID());

    // handle assetskeleton params immediately
    for (Enumeration ops = getOtherProperties(); ops.hasMoreElements();) {
      Object p = ops.nextElement();
      
      if (p instanceof PropertyGroup) {
        a.addOtherPropertyGroup(((PropertyGroup) p).lock());
      } else {
        a.addOtherPropertyGroupSchedule((PropertyGroupSchedule) ((PropertyGroupSchedule) p).clone());
      }
        
    }

    return a;
  }

  /** creates an instance of the the right class, suitable for filling
   * with properties by clone() methods.
   *
   **/

  public Asset instanceForCopy() {
    //return new Asset();
    try {
      return (Asset) this.getClass().newInstance();
    } catch (Exception e) {
      return null;
    }
  }

  // myPrototype used to default property values for subs
  // so we don't have to copy links or property groups.
  protected transient Asset myPrototype;

  public Asset getPrototype() {
    return myPrototype;
  }

  public Asset setPrototype(Asset arg_Prototype) {
    throw new RuntimeException("It is illegal to set the asset prototype explicitly");
    /*
    myPrototype= arg_Prototype;
    return arg_Prototype;
    */
  }

  /** used internally **/
  void privatelySetPrototype(Asset arg) {
    myPrototype= arg;
  }
    

  private transient ItemIdentificationPG myItemIdentificationPG;

  public ItemIdentificationPG getItemIdentificationPG() {
    return myItemIdentificationPG;
  }

  public void setItemIdentificationPG(PropertyGroup arg_ItemIdentificationPG) {
    if (!(arg_ItemIdentificationPG instanceof ItemIdentificationPG))
      throw new IllegalArgumentException("setItemIdentificationPG requires a ItemIdentificationPG argument.");
    myItemIdentificationPG= (ItemIdentificationPG) arg_ItemIdentificationPG;
  }

  private transient TypeIdentificationPG myTypeIdentificationPG;

  public TypeIdentificationPG getTypeIdentificationPG() {
    return myTypeIdentificationPG;
  }

  public void setTypeIdentificationPG(PropertyGroup arg_TypeIdentificationPG) {
    if (!(arg_TypeIdentificationPG instanceof TypeIdentificationPG))
      throw new IllegalArgumentException("setTypeIdentificationPG requires a TypeIdentificationPG argument.");
    myTypeIdentificationPG= (TypeIdentificationPG) arg_TypeIdentificationPG;
  }

  // for allocation/roleschedule stuff
  
  /** @return RoleSchedule - the RoleSchedule object associated with this Asset
    * Note that the old Enumeration getRoleSchedule() is no longer used in asset
    * use asset.getRoleSchedule().getRoleSchedule for the enumeration of the roleschedule
    **/
  public RoleSchedule getRoleSchedule() {
    return roleschedule;
  }

  //method should ONLY be called by the constructor and serialization
  private void initRoleSchedule() {
    roleschedule = new RoleScheduleImpl(this);
  }
  
  //end of disposition container / roleschedule stuff

  // aggregate creation support
  public Asset createAggregate(int quantity) {
    AggregateAsset aa = new AggregateAsset();
    aa.setAsset(this);
    aa.setQuantity(quantity);
    NewTypeIdentificationPG tip = PropertyGroupFactory.newTypeIdentificationPG();
    tip.setTypeIdentification(AGGREGATE_TYPE_ID);
    tip.setNomenclature(AGGREGATE_TYPE_ID+" aggregating quantity " + quantity + " of "+this);
    aa.setTypeIdentificationPG(tip);
    return aa;
  }

  
  // compile-compatibility with org.cougaar.domain.planning.ldm.plan.asset
  public Enumeration getCapabilities() { return null; }
  public String getName() { return null; }


  public PropertyGroupSchedule searchForPropertyGroupSchedule(Class c) {
    return super.searchForPropertyGroupSchedule(c);
  }



  // Keep track of the enclosing LDM so that we can resolve
  // late bindings and such.
  private transient LDMServesPlugIn _ldm = null;
  
  /** Called by readObject and factory to tell an asset where it
   * is resident to allow such things as late binding of PGs.
   **/
  public final void bindToLDM(LDMServesPlugIn ldm) {
    _ldm = ldm;
  }

  /** Called by Asset factories to register the asset with the LDM.
   * This assigns a UID to the Asset and binds the instance to
   * the ldm.
   **/
  public final void registerWithLDM(LDMServesPlugIn ldm) {
    setUID(ldm.getUIDServer().nextUID());
    bindToLDM(ldm);
  }

  /** @return the LDM that this Asset is bound to.  The value itself 
   * should <em>never</em> be used by anything other than the asset itself, 
   * though this method may be called to determine if the Asset is correctly
   * bound (e.g. to the current LDM).
   **/
  public final LDMServesPlugIn getBoundLDM() {
    return _ldm;
  }

  // serialization

  /** keep track of clusters that we've sent this asset to **/
  private transient HashSet _sentTo = new HashSet(3);
  private transient String _tid = null;
  
  // default protection!
  /** was this asset sent (as a prototype) to address **/
  boolean wasSentTo(MessageAddress address) {
    //return false;               // HACK!! always fail so that we always send the proto
    synchronized (_sentTo) {
      return _sentTo.contains(address);
    }
  }
  // default protection!
  /** this asset was sent (as a prototype) to address **/
  void addSentTo(MessageAddress address) {
    synchronized (_sentTo) {
      _sentTo.add(address);
    }
  }

  private void writeObject(ObjectOutputStream out) throws IOException {
    ClusterContextTable.ContextState cs = ClusterContextTable.getContextState();
    if (cs instanceof ClusterContextTable.MessageContext) {
      ClusterContextTable.MessageContext c = (ClusterContextTable.MessageContext)cs;
      MessageAddress dest = c.getToAddress();
      boolean protoSentP = (myPrototype==null)?false:(myPrototype.wasSentTo(dest));

      out.defaultWriteObject();

      out.writeObject(myTypeIdentificationPG);
      out.writeObject(myItemIdentificationPG);

      // send the proto.TIP as the proto description
      if (myPrototype != null) {
        // only send the first time.
        if (myPrototype.wasSentTo(dest)) {
          TypeIdentificationPG tipg = myPrototype.getTypeIdentificationPG();
          out.writeObject(tipg);
        } else {
          //myPrototype.addSentTo(dest);
          out.writeObject(myPrototype);
          myPrototype.addSentTo(dest);
        }
      } else {
        out.writeObject(null);
      }

    } else {
      // "Network" serialization

      //System.err.println("Default serialization of "+this);
      out.defaultWriteObject();

      out.writeObject(myTypeIdentificationPG);
      out.writeObject(myItemIdentificationPG);
      out.writeObject(myPrototype);

      if (out instanceof org.cougaar.core.cluster.persist.PersistenceOutputStream) {
        out.writeObject(roleschedule.getAvailableSchedule());
      }
    }      // End Network serialization
  } 

  private void readObject(ObjectInputStream in) throws ClassNotFoundException, IOException {
    _sentTo = new HashSet(3);

    ClusterContextTable.ContextState cs = ClusterContextTable.getContextState();
    if (cs instanceof ClusterContextTable.MessageContext) {
      ClusterContextTable.MessageContext c = (ClusterContextTable.MessageContext)cs;
      ClusterContext cc = cs.getClusterContext();
      LDMServesPlugIn ldm = cc.getLDM();
      bindToLDM(ldm);

      in.defaultReadObject();

      myTypeIdentificationPG = (TypeIdentificationPG) in.readObject();
      myItemIdentificationPG = (ItemIdentificationPG) in.readObject();

      MessageAddress dest = c.getToAddress();

      Object proto = in.readObject();

      if (proto != null) {

        if (proto instanceof Asset) {
          Asset pa = (Asset) proto;
          TypeIdentificationPG ptip = pa.getTypeIdentificationPG();
          String tid = ptip.getTypeIdentification();

          myPrototype = ldm.getPrototype(tid);
          // if we found it, we'll just drop what was sent on the floor.
          if (myPrototype != null) {
            // ok - all done.
          } else {
            // didn't find it.
            // So - add extra (local properties - strange but possible)
            ldm.fillProperties(pa);
            // cache it so we'll find it next time...
            ldm.cachePrototype(tid,pa);
            // set it and continue.
            myPrototype=pa;
          }
        } else if (proto instanceof TypeIdentificationPG) {
        // proto was sent as only the TIP 

          TypeIdentificationPG protoTIP = (TypeIdentificationPG) proto;

          // get the proto from OUR ldm...
          // for now, we just look at the TId of the TIP sent.
          String tid = protoTIP.getTypeIdentification();
          myPrototype = ldm.getPrototype(tid);
          // if no proto found, we're on our own,
          // so we'll create a proto with the tid and no other
          // props.
          if (myPrototype == null) {
            RootFactory ldmf = ldm.getFactory();
            Asset prot = ldmf.createPrototype(Asset.class, tid);
            // even though there is no prototype provider, there
            // might be property providers that can handle it, so
            // we'll expose our new instance to them.
            ldm.fillProperties(prot);
            // might as well register it, since we're unlikely to get
            // a proto provider later.
            ldm.cachePrototype(tid,prot);
            myPrototype=prot;
          } else {
          }
        } else {
          System.err.println("\nWARNING!!! Deserialized an unknown prototype class: "+proto);
        }
      } else {
        myPrototype = null;
      }

      initRoleSchedule();
    } else {
      ClusterContext cc = ClusterContextTable.getClusterContext();
      if (cc != null) {
        bindToLDM(cc.getLDM());
      }

      // plain serialization
      in.defaultReadObject();

      myTypeIdentificationPG=(TypeIdentificationPG)in.readObject();
      myItemIdentificationPG=(ItemIdentificationPG)in.readObject();
      myPrototype=(Asset)in.readObject();
      if (cc == null) {
        System.err.println("Warning! Contextless deserialization of "+this);
        Thread.dumpStack();
      }

      initRoleSchedule();
      if (in instanceof org.cougaar.core.cluster.persist.PersistenceInputStream) {
        Schedule schedule = (Schedule) in.readObject();
        ((RoleScheduleImpl)roleschedule).setAvailableSchedule(schedule);
      }
    }      // End Network serialization

  }

  private transient String cachedToString = null;
  public String toString() {
    if (cachedToString == null) {
      String cn = this.getClass().getName();
      int p = cn.lastIndexOf('.');
      if (p>=0) cn = cn.substring(p+1);

      String ti = myTypeIdentificationPG.getTypeIdentification();
      String ii = myItemIdentificationPG.getItemIdentification();

      boolean bogus = false;
      
      if (ti == null) {
        ti = "?";
        bogus = true;
      }
      if (ii == null) {
        ii = "#"+hashCode();
      }

      String whole = "<"+cn+" "+ti+" "+ii+">";
      if (bogus) return whole;
      cachedToString = whole;
    }
    return cachedToString;
  }

  void recacheToString() { cachedToString=null; }

  // Implement conversion to XML, mainly for UI developers
  public Element getXML(Document doc) {
    return XMLize.getPlanObjectXML(this,doc);
  }

  //dummy PropertyChangeSupport for the Jess Interpreter.
  public transient PropertyChangeSupport pcs = new PropertyChangeSupport(this);

  public void addPropertyChangeListener(PropertyChangeListener pcl) {
      pcs.addPropertyChangeListener(pcl);
  }

  public void removePropertyChangeListener(PropertyChangeListener pcl)   {
      pcs.removePropertyChangeListener(pcl);
  }

  /** this is a table of AssetClass->settermap
   * where settermap is a table of settername->method
   **/
  private static HashMap _classSetters = new HashMap(13);

  private static final Class[] _pgClassArgs = new Class[] { PropertyGroup.class };

  /**
   * Introspection-based propertygroup setter.  The property will be
   * added to either the built-in-property slots or the Other property set
   * as appropriate to the actual class of the asset.
   **/
  public void setPropertyGroup(NewPropertyGroup property) {
    try {
      String pS = property.getAssetSetMethod();
      Method m;
      HashMap _setterTable;
      synchronized (_classSetters) {
        _setterTable = (HashMap) _classSetters.get(this.getClass());
        if (_setterTable == null) {
          _setterTable = new HashMap(3);
          _classSetters.put(this.getClass(), _setterTable);
        }
      }
      synchronized (_setterTable) {
        m = (Method) _setterTable.get(pS);
        if (m == null) {
          Class assetC = this.getClass();
          try {
            m = assetC.getMethod(pS, _pgClassArgs);
          } catch (NoSuchMethodException e) {
            // add-on property.  sigh.
            try {
              m = assetC.getMethod("addOtherPropertyGroup", _pgClassArgs);
            } catch (NoSuchMethodException e1) {
              System.err.println("Couldn't find addOtherPropertyGroup in "+this);
              e1.printStackTrace();
            }
          }
          _setterTable.put(pS, m);
          /*
          if (m != null) {
            System.err.println("\n"+this.getClass()+"."+pS+"() found "+m);
          }
          */
        }
      }
      if (m != null) {
        // we could sync the calls to avoid consing the arglist
        m.invoke(this, new Object[] {property});
      }
    } catch (Exception e) {
      System.err.println("setPropertyGroup problem: "+e);
      e.printStackTrace();
    }
  }

  private static PropertyDescriptor properties[];

  static {
    try {
      properties = new PropertyDescriptor[5];
      properties[0] = new PropertyDescriptor("roleSchedule", Asset.class, "getRoleSchedule", null);
      properties[1] = new PropertyDescriptor("UID", Asset.class, "getUID", null);
      properties[2] = new PropertyDescriptor("typeIdentificationPG", Asset.class, "getTypeIdentificationPG", null);
      properties[3] = new PropertyDescriptor("itemIdentificationPG", Asset.class, "getItemIdentificationPG", null);
      properties[4] = new PropertyDescriptor("class", Asset.class, "getClass", null);
    } catch (IntrospectionException ie) {}
  }

  public PropertyDescriptor[] getPropertyDescriptors() {
    return properties;
  }

  private transient int _hc = 0;
  public int hashCode() {
    if (_hc != 0) return _hc;
    String tid = getTypeIdentificationPG().getTypeIdentification();
    String iid = getItemIdentificationPG().getItemIdentification();
    int hc = 1;
    if (tid != null) hc+=tid.hashCode();
    if (iid != null) hc+=iid.hashCode();
    _hc = hc;
    return hc;
  }

  /** Equals for assets is defined as being exactly the same class
   * and having equals TypeIdentification and ItemIdentification codes.
   **/

  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null) return false;
    if (!(getClass() == o.getClass())) return false;
    Asset oa = (Asset) o;
    TypeIdentificationPG tpg = getTypeIdentificationPG();
    TypeIdentificationPG opg = oa.getTypeIdentificationPG();
    if ( tpg == null || opg == null) return false;
    String ttid = tpg.getTypeIdentification();
    String otid = opg.getTypeIdentification();
    if (ttid == null || otid == null || !(ttid.equals(otid))) return false;
    ItemIdentificationPG tipg = getItemIdentificationPG();
    ItemIdentificationPG oipg = oa.getItemIdentificationPG();
    if ( tipg == null || oipg == null) return false;
    String tiid = tipg.getItemIdentification();
    String oiid = oipg.getItemIdentification();

    return compareStrings(tiid,oiid);
    /*
    if (tiid == null || oiid == null || !(tiid.equals(oiid))) return false;
    return true;
    */
  }

  private final boolean compareStrings(String s1, String s2) {
    if (s1 == s2) return true;  // catches null==null, x==x

    if (s1 != null) {
      // x==null, x==y, x==x
      return s1.equals(s2); 
    } else {
      // null==*
      return false;
    }
  }

  //
  // new PG resolution support
  //

  // search methods inherited from AssetSkeletonBase


  /** return the value of the specified PG if it is 
   * already present in a slot.
   **/
  protected PropertyGroup getLocalPG(Class pgc, long t) {
    if (ItemIdentificationPG.class.equals(pgc)) 
      return myItemIdentificationPG;
    if (TypeIdentificationPG.class.equals(pgc)) 
      return myTypeIdentificationPG;
    return super.getLocalPG(pgc,t);
  }

  /** Set the apropriate slot in the asset to the specified pg.
   * Scheduled PGs have the time range in them, so the time (range)
   * should not be specified in the arglist.
   **/
  protected void setLocalPG(Class pgc, PropertyGroup pg) {
    if (ItemIdentificationPG.class.equals(pgc)) {
      myItemIdentificationPG = (ItemIdentificationPG)pg;
    }
    else if (TypeIdentificationPG.class.equals(pgc)) {
      myTypeIdentificationPG = (TypeIdentificationPG)pg;
    } 
    else {
      super.setLocalPG(pgc,pg);
    }
  }


  /** get and possibly cache a PG value.
   * The information can come from a number of places:
   *   a local slot 
   *   a late binding
   *   the prototype (recurse to resolve on the prototype)
   *   a default value
   *
   * Will return Null_PG instances if present.
   * Defined as abstract in AssetSkeletonBase
   **/
  public final PropertyGroup resolvePG(Class pgc, long t) {
    // check local slots - this call never sets
    PropertyGroup pg = getLocalPG(pgc, t); 
    if (pg != null) return pg;  // return it - already set

    // check late binding
    if ((pg = lateBindPG(pgc, t)) != null) {
      //setLocalPG(pgc, pg);
      return pg;
    }

    // check the prototype
    if (myPrototype != null) {
      // recurse
      if ((pg = myPrototype.resolvePG(pgc, t)) != null) {
        // should we cache the prototype's PG value?
        // Let's not.
        return pg;
      }
    }

    // possibly default
    pg = generateDefaultPG(pgc);

    return pg;
  }

  /** request late binding from the LDM for this asset/PGClass.
   * Late binders should set the asset's PG as appropriate in 
   * addition to returning the PG.
   * Implements an abstract method from AssetSkeletonBase.
   * @return null or a PropertyGroup instance.
   */
  protected final PropertyGroup lateBindPG(Class pgc, long t) {
    if (_ldm != null) {
      // Pass along the requested time in case we can get just
      // a single time slice late-bound.  This brings up other
      // issues which we will just hand wave about for now...
      return _ldm.lateFillPropertyGroup(this, pgc, t);
    }
    else {
      System.err.println("Asset "+this+" is not bound to an LDM instance!");
    }
    return null;
  }

  // 
  // implement Publishable
  //
  public boolean isPersistable() { return true; }

  private transient TypeItemKey myKey = null;

  public Object getKey() {
    if (myKey == null) {
      myKey = new TypeItemKey(this);
    }
    
    return myKey;
  }

  private static class TypeItemKey {
    private String myTypeString = null;
    private String myItemString = null;
    private int myHashCode;
    
    public TypeItemKey(Asset asset) {
      TypeIdentificationPG tipg = asset.getTypeIdentificationPG();
      if (tipg != null) {
        myTypeString = tipg.getTypeIdentification();
      }
      if (myTypeString == null) {
        myTypeString = "";
      }
      
      ItemIdentificationPG iipg = asset.getItemIdentificationPG();
      if (iipg != null) {
        myItemString = iipg.getItemIdentification();
      }
      if (myItemString == null) {
        myItemString = "";
      }

      if ((myTypeString.equals("")) && (myItemString.equals(""))) {
        System.err.println("Unable to create unique key for asset " + asset +
                           " - type and item identification are not set.");
      }

      myHashCode = myTypeString.hashCode() + myItemString.hashCode();
    }

    public int hashCode() {
      return myHashCode;
    }
    
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }

      if (o instanceof TypeItemKey) {
        TypeItemKey that = (TypeItemKey) o;
        return (this.myTypeString.equals(that.myTypeString) && 
                this.myItemString.equals(that.myItemString));
      } 

      return false;
    }

    public String toString() {
      return "<" + myTypeString + " " + myItemString + ">";
    }
  }

  // ChangeReport tracking
  //
  public interface AssetChangeReport extends ChangeReport {}
}




