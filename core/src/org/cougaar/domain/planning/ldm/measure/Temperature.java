/*
 * <copyright>
 * Copyright 1997-2001 Defense Advanced Research Projects
 * Agency (DARPA) and ALPINE (a BBN Technologies (BBN) and
 * Raytheon Systems Company (RSC) Consortium).
 * This software to be used only in accordance with the
 * COUGAAR licence agreement.
 * </copyright>
 */
/** Immutable implementation of Temperature.
 **/

// should be machine generated - do not edit!

package org.cougaar.domain.planning.ldm.measure;

public final class Temperature extends AbstractMeasure 
{
  private static final Conversion CELSIUS_TO_CELSIUS = new Conversion() {
    public double convert(double from) { return from; }};

  private static final Conversion CELSIUS_TO_FAHRENHEIT = new Conversion() {
    public double convert(double from) { return (from*1.8)+32.0; }};
  private static final Conversion FAHRENHEIT_TO_CELSIUS = new Conversion() {
    public double convert(double from) { return (from-32.0)/1.8; }};

  // basic unit is Celsius
  private double theValue;

  // private constructor
  private Temperature(double v) {
    theValue = v;
  }

  public Temperature(String s) {
    int i = indexOfType(s);
    if (i < 0) throw new UnknownUnitException();
    double n = Double.valueOf(s.substring(0,i).trim()).doubleValue();
    String u = s.substring(i).trim().toLowerCase();
    if (u.equals("celsius")) 
      theValue=n;
    else if (u.equals("fahrenheit")) 
      theValue=FAHRENHEIT_TO_CELSIUS.convert(n);
    else 
      throw new UnknownUnitException();
  }

  public int getCommonUnit() { return FAHRENHEIT; }
  public int getMaxUnit() { return 1; }
  public String getUnitName(int i) { 
    if (i ==0) return "celcius";
    else if (i == 1) return "fahrenheit";
    else throw new IllegalArgumentException();
  }

  // TypeNamed factory methods
  public static Temperature newCelsius(double v) {
    return new Temperature(v);
  }
  public static Temperature newCelsius(String s) {
    return new Temperature((Double.valueOf(s).doubleValue()));
  }
  public static Temperature newFahrenheit(double v) {
    return new Temperature(FAHRENHEIT_TO_CELSIUS.convert(v));
  }
  public static Temperature newFahrenheit(String s) {
    return new Temperature(FAHRENHEIT_TO_CELSIUS.convert(Double.valueOf(s).doubleValue()));
  }

  // Index Typed factory methods
  private static final Conversion convFactor[]={
    // conversions to base units
    CELSIUS_TO_CELSIUS,
    FAHRENHEIT_TO_CELSIUS,
    // conversions from base units
    CELSIUS_TO_CELSIUS,
    CELSIUS_TO_FAHRENHEIT
  };
  // indexes into factor array
  public static int CELSIUS = 0;
  public static int FAHRENHEIT = 1;
  private static int MAXUNIT = 1;
  
  // Index Typed factory methods
  public static Temperature newTemperature(double v, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return new Temperature(convFactor[unit].convert(v));
    else
      throw new UnknownUnitException();
  }

  public static Temperature newTemperature(String s, int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return new Temperature(convFactor[unit].convert(Double.valueOf(s).doubleValue()));
    else
      throw new UnknownUnitException();
  }

  // Support for AbstractMeasure-level constructor
  public static AbstractMeasure newMeasure(String s, int unit) {
    return newTemperature(s, unit);
  }
  public static AbstractMeasure newMeasure(double v, int unit) {
    return newTemperature(v, unit);
  }

  // Unit-based Reader methods
  public double getCelsius() {
    return (theValue);
  }
  public double getFahrenheit() {
    return (CELSIUS_TO_FAHRENHEIT.convert(theValue));
  }

  public double getValue(int unit) {
    if (unit >= 0 && unit <= MAXUNIT)
      return convFactor[MAXUNIT+1+unit].convert(theValue);
    else
      throw new UnknownUnitException();
  }

  public static Conversion getConversion(final int from, final int to) {
    if (from >= 0 && from <= MAXUNIT &&
        to >= 0 && to <= MAXUNIT ) {
      return new Conversion() {
        public double convert(double value) {
          return convFactor[MAXUNIT+1+to].convert(convFactor[from].convert(value));
        }
      };
    } else
      throw new UnknownUnitException();
  }

  public boolean equals(Object o) {
    return ( o instanceof Temperature &&
             theValue == ((Temperature) o).getCelsius());
  }
  public String toString() {
    return Double.toString(theValue) + "c";
  }
  public int hashCode() {
    return (new Double(theValue)).hashCode();
  }

  
} // end Temperature
