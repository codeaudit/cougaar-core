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
package org.cougaar.domain.planning.ldm.measure;
import java.lang.reflect.*;

/** A generic Scalar per Duration class that can be interpreted in a 
 * variety of ways depending on context.
 **/

public class Capacity extends AbstractMeasure {
  private Scalar quantity;
  private Duration period;

  /** Construct a capacity object which represents
   * a Scalar Measure (e.g. Volume, Distance, etc) divided by a
   * Duration (time).  Example: Miles per hour, hours per day, gallons per second.
   * May also be used
   **/
  public Capacity(Scalar quantity, Duration period) {
    this.quantity=quantity;
    this.period = period;
  }

  /** String constructor.  Allowed syntax is:
   * "Scalarmeasure=ValueUnits Duration=ValueUnits"
   * Example: "Count=20units Duration=1weeks"
   * For instantaneous: "Count=20units"
   **/
  public Capacity(String s) {
    int i = s.indexOf(' ');
    String qs;
    String ps;
    if (i == -1) {
      qs = s;
      ps = null;
    } else {
      qs = s.substring(0,i);
      ps = s.substring(i+1);
    }
    
    try {
      Class[] sa = new Class[1];
      sa[0] = String.class;

      int qse = qs.indexOf('=');
      String qsn = qs.substring(0,qse);
      Class qsc = Class.forName("org.cougaar.domain.planning.ldm.measure."+qsn);
      Constructor qscc = qsc.getConstructor(sa);
      Object[] qargs = new Object[1];
      qargs[0] = qs.substring(qse+1);
      quantity = (Scalar) qscc.newInstance(qargs);

      if (ps != null) {
        int pse = ps.indexOf('=');
        String psn = ps.substring(0,pse);
        Class psc = Class.forName("org.cougaar.domain.planning.ldm.measure."+psn);
        Constructor pscc = psc.getConstructor(sa);
        Object[] pargs = new Object[1];
        pargs[0] = ps.substring(pse+1);
        period = (Duration) pscc.newInstance(pargs);
      } else {
        period = null;
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("Exception: "+e);      
    }
  }

  /** @return an illegal value, since Capacity is a tuple **/
  public int getCommonUnit() { return -1; }
  public int getMaxUnit() { return -1; }
  /** @return null, since Capacity is a tuple **/
  public String getUnitName(int i) { return null; }

  public double getValue(int unit) { throw new IllegalArgumentException(); }

  /** Construct an "instantaneous" capacity object - e.g. 
   * the capactity of "holding 10000 Gallons" or "having 48 Het-equivalents".
   **/
  public Capacity(Scalar quantity) {
    this.quantity=quantity;
    this.period = null;
  }
  
  /** @return the numerator of the Capacity **/
  public Scalar getQuantity() { return quantity; }

  /** @return the denominator of the Capacty **/
  public Duration getPeriod() { return period; }

  /** is this a representation of instantaneous capacity? **/
  public boolean isInstantaneous() { return period==null; }

  /** compute the figure that the capacity represents. E.g.
   * Capacity cap = new Capacity(new Volume("10 liters"), new Duration("1 seconds"));
   * double rate = cap.getCapacity(Volume.GALLONS, Duration.HOURS);
   *
   * Note that to get the quantityUnits correct, you need to know
   * what the concrete type of the quantity is.
   *
   * @exception UnknownUnitException if a bad unit in either argument.
   * @exception DivideByZero if duration is 0.
   * @exception NullPointerException if duration is null.
   **/
  public double getRate(int quantityUnits, int periodUnits) {
    if (isInstantaneous()) 
      throw new IllegalArgumentException("Cannot compute rate of an instantaneous Capacity");
    return quantity.getValue(quantityUnits) /
      period.getValue(periodUnits);
  }

  public String toString() {
    if (isInstantaneous()) {
      return "Capacity of "+quantity;
    } else {
      return quantity+" per "+period;
    }
  }

  public boolean equals(Object o) {
    if (o instanceof Capacity) {
      Capacity oc = (Capacity) o;
      Duration ocp = oc.getPeriod();
      if ((period == null) != (ocp == null)) return false;
      if (period == null) 
        return quantity.equals(oc.getQuantity());
      else
        return (getRate(0,0) == oc.getRate(0,0));
    } else
      return false;
  }

  public int hashCode() {
    int qh = quantity.hashCode();
    if (period != null)
      qh = (qh<<2) + period.hashCode();
    return qh;
  }

}
