/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */

package org.cougaar.core.util;

import java.io.OutputStream;
import java.io.PrintStream;

import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import org.cougaar.domain.planning.ldm.plan.Location;
import org.cougaar.domain.planning.ldm.plan.Verb;

/**
 * <code>Ascii Printer</code>.
 * <p>
 * Works with the <code>SelfPrinter</code>interface -- see
 * getObjectInfo for information about supported Object Classes.
 * <p>
 * @see AbstractPrinter
 */

public abstract class AsciiPrinter extends AbstractPrinter {

  public static void main(String[] args) {
    AbstractPrinter.main(args);
  }

  /** public **/

  public AsciiPrinter(OutputStream out) {
    super(out);
  }

  public void printObject(Object o) {
    beforeFirstPrint();
    print(o, "object");
    afterLastPrint();
  }

  /**
   * Only <code>SelfPrinter</code>'s "print" method should call
   * this!  Use "printObject" for all other cases!!!!!
   */
  public void print(Object o, String name) {
    if (o == null) {
    } else if (o instanceof SelfPrinter) {
      print((SelfPrinter)o, name);
    } else if (o instanceof java.util.Collection) {
      print((java.util.Collection)o, name);
    } else {
      ObjectInfo oi = getObjectInfo(o);
      if (oi instanceof StringObjectInfo) {
        print(oi.getClassName(), name, ((StringObjectInfo)oi).getValue());
      } else if (oi instanceof ListObjectInfo) {
        printListObjectInfo((ListObjectInfo)oi, name);
      } else
        throw new RuntimeException("Never!");
    }
  }

  public abstract void print(boolean z, String name);
  public abstract void print(byte b, String name);
  public abstract void print(char c, String name);
  public abstract void print(short s, String name);
  public abstract void print(int i, String name);
  public abstract void print(long l, String name);
  public abstract void print(float f, String name);
  public abstract void print(double d, String name);

  /** protected **/

  protected void beforeFirstPrint() {}
  protected void afterLastPrint() {}

  // callers should pass (Object)x.
  protected abstract void print(String x, String name);

  protected abstract void printBegin(String type, String name);

  protected abstract void printEnd(String name);

  protected void printBeginCollection(String type, String name, int size) {
    printBegin(type, name);
  }

  protected void printBeginElement() {}
  protected void printEndElement() {}

  protected void printEndCollection(String name) {
    printEnd(name);
  }

  protected final void print(SelfPrinter sp, String name) {
    if (sp != null) {
      printBegin(sp.getClass().getName(), name);
      sp.printContent(this);
      printEnd(name);
    }
  }

  protected final void print(java.util.Collection col, String name) {
    if (col != null) {
      printBeginCollection(col.getClass().getName(), name, col.size());
      java.util.Iterator iter = col.iterator();
      while (iter.hasNext())
        printElement(iter.next());
      printEndCollection(name);
    }
  }

  protected final void printListObjectInfo(ListObjectInfo loi, String name) {
    printBegin(loi.getClassName(), name);
    Iterator iter = loi.getNameVals();
    while (iter.hasNext()) {
      NameVal ns = (NameVal)iter.next();
      print(ns.val, ns.name);
    }
    printEnd(name);
  }

  protected void printElement(StringObjectInfo soi) {
    print(soi.getClassName(), "element", soi.getValue());
  }

  protected void printElement(Object o) {
    if (o == null) 
      return;
    printBeginElement();
    if (o instanceof SelfPrinter) {
      print((SelfPrinter)o, "element");
    } else if (o instanceof java.util.Collection) {
      print((java.util.Collection)o, "element");
    } else {
      ObjectInfo oi = getObjectInfo(o);
      if (oi instanceof StringObjectInfo) {
	printElement((StringObjectInfo)oi);
      } else if (oi instanceof ListObjectInfo) {
        printListObjectInfo((ListObjectInfo)oi, "element");
      } else
        throw new RuntimeException("Never!");
    }
    printEndElement();
  }

  protected void print(String type, String name, String x) {
    print(x, name);
  }

  /**
   * <b>ALL</b> objects that might be printed <b>MUST</b> be listed here,
   * with these exceptions:<br>
   * <ul>
   *   <li>null</li>
   *   <li>instanceof SelfPrinter</li>
   *   <li>java.lang.String</li>
   *   <li>instanceof java.util.Collection</li>
   * </ul><br>
   */
  protected static final ObjectInfo getObjectInfo(Object o) {
    if (o instanceof Date) {
      ListObjectInfo objInfo = new ListObjectInfo("java.util.Date");
      objInfo.addNameVal("Time", Long.toString(((Date)o).getTime()));
      return objInfo;
    } else if (o instanceof Verb) {
      return new StringObjectInfo("org.cougaar.domain.planning.ldm.plan.Verb", ((Verb)o).toString());
    }
    /*
    else if (o instanceof Position) {
      ListObjectInfo objInfo = new ListObjectInfo("org.cougaar.domain.planning.ldm.plan.Position");
      Position pos = (Position)o;
      if (pos.getLatitude() != null)
        objInfo.addNameVal("Latitude", 
                           pos.getLatitude().getDegrees()+"degrees");
      if (pos.getLongitude() != null)
        objInfo.addNameVal("Longitude", 
                           pos.getLongitude().getDegrees()+"degrees");
      if (o instanceof NamedPosition) {
        objInfo.setClassName("org.cougaar.domain.planning.ldm.plan.NamedPosition");
        NamedPosition npos = (NamedPosition)o;
        objInfo.addNameVal("Name", npos.getName());
        if (o instanceof GeolocLocation) {
          objInfo.setClassName("org.cougaar.domain.planning.ldm.plan.GeolocLocation");
          GeolocLocation loc = (GeolocLocation)o;
          objInfo.addNameVal("GeolocCode", loc.getGeolocCode());
          objInfo.addNameVal("InstallationTypeCode", 
                             loc.getInstallationTypeCode());
          objInfo.addNameVal("CountryStateCode", loc.getCountryStateCode());
          objInfo.addNameVal("CountryStateName", loc.getCountryStateName());
          objInfo.addNameVal("IcaoCode", loc.getIcaoCode());
        }
      }
      return objInfo;
    } 
    */
    else if ((o instanceof String) ||
               (o instanceof Integer) ||
	       (o instanceof Boolean) ||
               (o instanceof Character) ||
               (o instanceof Byte) ||
               (o instanceof Short) ||
               (o instanceof Long) ||
               (o instanceof Float) ||
               (o instanceof Class) ||
               (o instanceof Double)) {
      return new StringObjectInfo(o.getClass().getName(), o.toString());
    } else {
      throw new RuntimeException(
        "Unhandled Object type in AbstractPrinter.getObjectInfo("+
        o.getClass().getName()+")!");
    }
  }

  /**
   * Simple data structure for non-SelfPrinter ObjectInfo
   */
  protected static class ObjectInfo {
    protected String className;
    private ObjectInfo() {}
    public ObjectInfo(String s) {
      className = s; 
    }
    public void setClassName(String s) {className = s;}
    public String getClassName() {return className;}
  }

  protected static class StringObjectInfo extends ObjectInfo {
    protected String value;
    public StringObjectInfo(String s, String v) {
      super(s);
      value = v;
    }
    public void setValue(String v) {value = v;}
    public String getValue() {return value;}
  }

  protected static class ListObjectInfo extends ObjectInfo {
    protected Collection nameVals;
    public ListObjectInfo(String s) {
      super(s);
      nameVals = new java.util.Vector();
    }
    public void addNameVal(String name, String val) {
      if (val != null)
        nameVals.add(new NameVal(name, val));
    }
    public Iterator getNameVals() {return nameVals.iterator();}
  }

  protected static class NameVal {
    public String name;
    public String val;
    public NameVal(String n, String v) {name=n; val=v;}
  }

}
