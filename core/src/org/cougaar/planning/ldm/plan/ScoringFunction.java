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

package org.cougaar.planning.ldm.plan;

import java.util.Enumeration;
import java.util.Vector;
import org.cougaar.util.Empty;
import org.cougaar.util.SingleElementEnumeration;
import java.io.Serializable;

 
/**
 * Base class for functions which compute a Score value given an AspectValue.
 * Score is a double where LOW_THRESHOLD (0.0) is "good"/"optimal" and 
 * HIGH_THRESHOLD (1.0) is "bad" (as bad as it gets).  ScoringFunction domains
 * should be infinite and Range should be 0.0 to 1.0 inclusive.
 * 
 * Instances are immutable.
 *
 * @author  ALPINE <alpine-software@bbn.com>
 **/
public abstract class ScoringFunction implements Serializable, Cloneable {
   
  /** Minimum valid value **/
  public static final double LOW_THRESHOLD = 0.0;
  /** Maximum valid value **/
  public static final double HIGH_THRESHOLD = 1.0;
   
  /** "Best" Score **/
  public final static double BEST = LOW_THRESHOLD;
  /** "Worst" Score **/
  public final static double WORST = HIGH_THRESHOLD;
  
  /** Typical "Satisfactory" value **/
  public final static double OK = 0.5;

  protected int aspectType;

  protected ScoringFunction(int type) {
    this.aspectType = type;
  }

  public abstract Object clone();

  /** Find the/a "Best" value of the function.
   * Can be used as a starting point for an allocator.
   * If not implemented, returns null.
   * @return AspectScorePoint  May be null if uncomputable.
   */
  public abstract AspectScorePoint getBest();

  /** Find the/a "Best" value within a range.
   * Specify AspectValue boundaries within the function and get the
   * minimum value within those boundaries.
   * If not implemented, returns null.
   * @param lowerbound
   * @param upperbound 
   * @return AspectScorePoint May be null if uncomputable.
   */ 
  public abstract AspectScorePoint getMinInRange(AspectValue lowerbound, 
						  AspectValue upperbound);

   
  /** Find the/a "Worst" value within a range.
   * Specify AspectValue boundaries within the function and get the
   * maximum value within those boundaries.
   * If not implemented, returns null.
   * @param lowerbound 
   * @param upperbound
   * @return AspectScorePoint May be null if uncomputable.
   */
  public abstract AspectScorePoint getMaxInRange(AspectValue lowerbound,
						 AspectValue upperbound);

   
  /** Find all non-1.0 (worst) value ranges within boundaries.
   * Specify AspectValue boundaries within the function and get the
   * the valid ranges of values within those boundaries.
   * If not implemented, returns null.
   * @param lowerbound 
   * @param upperbound
   * @return Enumeration{AspectScoreRange} may be null if uncomputable.
   */
  public abstract Enumeration getValidRanges(AspectValue lowerbound,
					     AspectValue upperbound);


  /** @return the range over which the scoring function is defined.
   * Note that "undefined" == "undifferentiated WORST" score.
   * Will always return a non-null value, but one or both values of 
   * the range may an AspectScorePoing infinity, indicating unbounded range.
   * There may be any amount of score variation, including WORST points
   * within this range.
   **/
  public AspectScoreRange getDefinedRange() {
    return new AspectScoreRange(AspectScorePoint.getNEGATIVE_INFINITY(aspectType), 
                                AspectScorePoint.getPOSITIVE_INFINITY(aspectType));
  }

  /** Find the Score at a point.  1.0 is worst, 0.0 is best.
   * @param value
   * @return double  The score given an AspectValue.
   * @see org.cougaar.planning.ldm.plan.AspectValue
   */
  public abstract double getScore(AspectValue value); 
   
   
  // methods that create basic Scoring Functions
   
   
  /** Create a ScoringFunction from a set of AspectScorePoints
   * @param points
   * @return PointScoringFunction
   */
  public static final ScoringFunction createPiecewiseLinearScoringFunction(Enumeration points) {
    return new PiecewiseLinearScoringFunction(points);
  }
   
  /** A single point with straight sides in score space
   * @param value  The single point.
   * @return StrictValueScoringFunction
   */
  public static final ScoringFunction createStrictlyAtValue(AspectValue value) {
    return new StrictValueScoringFunction(value);
  }
  
  /** A single point with slanted sides in score space
   * @param value  The single point.
   * @return PreferredValueScoringFunction
   */
  public static final ScoringFunction createPreferredAtValue(AspectValue value, double slope) {
    return new PreferredValueScoringFunction(value, slope);
  }
  
  /** A flat basin with straight sides.
   * BEST defaults to low point.
   * @param low  The low point.
   * @param high The high point.
   * @return StrictBetweenScoringFunction
   */
  public static final ScoringFunction createStrictlyBetweenValues(AspectValue low, AspectValue high) {
    return new StrictBetweenScoringFunction(low, high);
  }
  
  /** A flat basin with straight sides.
   * Just like StrictBetweenScoringFunction, except
   * that BEST point is specified, even though the 
   * Scores of low, best and high are all actually the same.
   * @param low  The low point.
   * @param low  The preferred point.
   * @param high The high point.
   * @return StrictBetweenWithBest
   */
  public static final ScoringFunction createStrictlyBetweenWithBestValues(AspectValue low, AspectValue best, AspectValue high) {
    return new StrictBetweenWithBest(low, best, high);
  }

  /** A typical V-shaped scoring function.
   * low and high values are OK, best is BEST,
   * score is linear betwen low and best, best and high.
   * anything outside the range is WORST.
   * The best point need not be centered between low and high.
   * @param low  The low point.
   * @param low  The best point.
   * @param high The high point.
   * @return VScoringFunction
   */
  public static final ScoringFunction createVScoringFunction(AspectValue low, AspectValue best, AspectValue high) {
    return new VScoringFunction(low, best, high);
  }
  /** like createVScoringFunction(low,best,high) except allows specification
   * of value of OK value.
   **/
  public static final ScoringFunction createVScoringFunction(AspectValue low, AspectValue best, AspectValue high, double ok) {
    return new VScoringFunction(low, best, high, ok);
  }

  /** A flat basin with slanted sides
   * @param low The low point.
   * @param high The high point.
   * @return PreferredBetweenScoringFunction
   */
  public static final ScoringFunction createPreferredBetweenValues(AspectValue low, AspectValue high, double slope) {
    return new PreferredBetweenScoringFunction(low, high, slope);
  }
  
  /** Prefer as close as possible to value from above
   * The score at the inflection point is BEST
   * @param value The point.
   * @return AboveScoringFunction
   */
  public static final ScoringFunction createNearOrAbove(AspectValue value, double slope) {
    return new AboveScoringFunction(value, slope);
  }
  
  /** Prefer as close as possible to value from below
   * The score at the inflection point is BEST;
   * @param value  The point.
   * @return BelowScoringFunction
   */
  public static final ScoringFunction createNearOrBelow(AspectValue value, double slope) {
    return new BelowScoringFunction(value, slope);
  }
  
  /** Select specific enumerated points where score is allowed : 
   * disallowed (above threshold) at all other points.
   * Note : The implementation ignores range, as enumerations have
   * no sense of comparison or continuity.
   * @param points array of AspectScorePoints of allowable points
   * @return EnumeratedScoringFunction
   */
  public static final ScoringFunction createEnumerated
    (AspectScorePoint []points) {
    return new EnumeratedScoringFunction(points);
  }
  
  /** Step function
   * The score exactly at the inflection point is BEST
   * @param changepoint
   * @param prescore
   * @param postscore
   * @return StepScoringFunction
   */
  public static final ScoringFunction createStepScoringFunction(AspectValue changepoint, double prescore, double postscore) {
    return new StepScoringFunction(changepoint, prescore, postscore);
  }

  /** Constant function
   * always has same score
   * @param score - the score for all values
   * @return ConstantScoringFucntion
  public static final ScoringFunction createConstantScoringFunction(double score){
    return new ConstantScoringFunction(score);
  }

  // 
  // helper functions for below
  //

  /** interpolate a Y given an X and a line segment.  px must be in 
   * the range.
   **/
  final static double _interpY(double px, 
                               double x1, double y1, double x2, double y2 ) {
    if (x1 == x2) return y1;    // eliminate div0
    return y1+((px-x1)*((y2-y1)/(x2-x1)));
  }

  /** return an AspectScorePoint for the minimum y of the segment 
   * ((x1,y1) (x2, y2)) in the range of minx-maxx.
   * @return WORST if out of range.
   **/
  final AspectScorePoint _minY(double minx, double maxx,
                               double x1, double y1, double x2, double y2) {
    if (x1 > maxx || x2 < minx) 
      return new AspectScorePoint(minx, WORST, aspectType);
    
    if (x1 < minx) {
      y1 = _interpY(minx, x1, y1, x2, y2);
      x1 = minx;
    } 
    if (x2 > maxx) {
      y2 = _interpY(maxx, x1, y1, x2, y2);
      x2 = maxx;
    }
    
    // return the lowest y
    if (y2< y1) {
      y1 = y2;
      x1 = x2;
    }

    return new AspectScorePoint(x1, y1, aspectType);
  }

  /** return an AspectScorePoint for the maximum y of the segment 
   * ((x1,y1) (x2, y2)) in the range of minx-maxx.
   * @return BEST if out of range.
   **/
  final AspectScorePoint _maxY(double minx, double maxx,
                               double x1, double y1, double x2, double y2) {
    if (x1 > maxx || x2 < minx) 
      return new AspectScorePoint(minx, BEST, aspectType);
    
    if (x1 < minx) {
      y1 = _interpY(minx, x1, y1, x2, y2);
      x1 = minx;
    } 
    if (x2 > maxx) {
      y2 = _interpY(maxx, x1, y1, x2, y2);
      x2 = maxx;
    }
    
    // return the highest y
    // return the lowest y
    if (y2> y1) {
      y1 = y2;
      x1 = x2;
    }

    return new AspectScorePoint(x1, y1, aspectType);
  }

  //
  // Implementations of the convenience ScoringFunctions above
  //

  public static class PiecewiseLinearScoringFunction extends ScoringFunction {
    protected AspectScorePoint curve[];
  
    public PiecewiseLinearScoringFunction(Enumeration points) {
      super(0);
      if (points == null || ! points.hasMoreElements()) {
        throw new IllegalArgumentException("Enumeration of points must be non-empty");
      }
      Vector v = new Vector();
      while (points.hasMoreElements()) {
        v.addElement(points.nextElement());
      }
      curve = (AspectScorePoint[]) v.toArray(new AspectScorePoint[v.size()]);
      aspectType = curve[0].getAspectType();
    }

    public Object clone() {
      return new PiecewiseLinearScoringFunction(new Enumeration() {
        private int ix = 0;
        public boolean hasMoreElements() {
          return ix < curve.length;
        }
        public Object nextElement() {
          return curve[ix++].clone();
        }
      });
    }
    
    public double getScore(AspectValue value) {
      return getScore(value.getValue());
    }

    protected double getScore(double vp){
      int l = curve.length;
      AspectScorePoint c1 = null;
      for (int i = 0 ; i<l; i++) {
        c1 = curve[i];
        double v1 = c1.getValue();
        double s1 = c1.getScore();

        if (vp<v1) {
          if (i>0) {
            AspectScorePoint c0 = curve[i-1];
            double v0 = c0.getValue();

            if (v1 <= v0) return s1; // second point wins on undefined.

            double s0 = c0.getScore();
            double slope = (s1-s0)/(v1-v0);

            return s0+(vp-v0)*slope;

          } else {
            return s1;
          }
        } // else continue
      }
      // drop through - flat after last point.
      return c1.getScore();
    }
    public AspectScorePoint getMinInRange(AspectValue lowerbound, AspectValue upperbound) {
      AspectScorePoint c0 = curve[0];
      AspectScorePoint c1;
      int l = curve.length;
      if (l == 1) return c0;
      double x0 = c0.getValue();
      double y0 = c0.getScore();

      double minx = lowerbound.getValue();
      double maxx = upperbound.getValue();

      AspectScorePoint bp = null;

      for (int i = 1 ; i<l; i++) {
        c1 = curve[i];
        double x1 = c1.getValue();
        double y1 = c1.getScore();
        AspectScorePoint pp = super._minY(minx, maxx, x0, y0, x1, y1);
        if (bp == null || pp.getScore() <bp.getScore())
          bp = pp;
      }
      if (bp == null || bp.getScore() == WORST) return null;
      return bp;
    }

    public AspectScorePoint getMaxInRange(AspectValue lowerbound, AspectValue upperbound) {
      AspectScorePoint c0 = curve[0];
      AspectScorePoint c1;
      int l = curve.length;
      if (l == 1) return c0;
      double x0 = c0.getValue();
      double y0 = c0.getScore();

      double minx = lowerbound.getValue();
      double maxx = upperbound.getValue();

      AspectScorePoint bp = null;

      for (int i = 1 ; i<l; i++) {
        c1 = curve[i];
        double x1 = c1.getValue();
        double y1 = c1.getScore();
        AspectScorePoint pp = super._maxY(minx, maxx, x0, y0, x1, y1);
        if (bp == null || pp.getScore() > bp.getScore())
          bp = pp;
      }
      if (bp == null || bp.getScore() == BEST) return null;
      return bp;
    }

    protected class DPair {
      public double x0, x1;
      public DPair(double x0, double x1) { this.x0=x0; this.x1=x1; }
    }

    /** return a vector of DPair instances, each one is a
     * range of valid x values.  Some may be points (x = x);
     **/
    protected Vector getAllValidValues(double thresh) {
      AspectScorePoint c0 = curve[0];
      double x0 = c0.getValue();
      double y0 = c0.getScore();
      boolean v0 = (y0<thresh);
      AspectScorePoint c1;
      double x1, y1;
      boolean v1;

      Vector v = new Vector();
      
      int l = curve.length;
      
      if (l == 1) {
        v.addElement(new DPair(x0, x0));
        return v;
      }

      DPair r = null;
      for (int i=1; i<l; i++) {
        c1 = curve[i];
        x1 = c1.getValue();
        y1 = c1.getScore();
        v1 = (y1<thresh);

        // is there a crossing?
        if (v0) {               
          if (v1) {             // both valid
            if (r == null) {
              r = new DPair(x0, x1);
            } else {
              r.x1 = x1;
            }
          } else {              // v0 valid, v1 not
            // find the cross point
            double dx = x1-x0;
            if (dx == 0) {      // discontinuity
              if (r == null) {
                v.addElement(new DPair(x0, x0));
              } else {
                v.addElement(r);
                r = null;
              }
            } else {
              double x = x0+(((x1-x0)*(thresh-y0))/(y1-y0));
              if (r == null) {
                v.addElement(new DPair(x0, x));
              } else {
                r.x1 = x;
                v.addElement(r);
                r= null;
              }                
            }
          }
        } else {
          if (v1) {             // v0 not, v1 valid
            // find the cross point
            double dx = x1-x0;
            if (dx == 0) {      // discontinuity
              if (r == null) {
                r = new DPair(x1, x1);
              } else {
                // shouldn't actually happen, I think
                v.addElement(r);
                r = new DPair(x1, x1);
              }
            } else {
              double x = x0+(((x1-x0)*(thresh-y0))/(y1-y0));
              if (r == null) {
                r = new DPair(x, x1);
              } else {
                // again, shouldn't happen
                r.x1 = x;
                v.addElement(r);
                r= null;
              }                
            }
          } else {              // both invalid
            // just advance
          } 
        }

        c0 = c1;
        x0 = x1;
        y0 = x1;
        v0 = v1;
      }      

      if (r !=null) 
        v.addElement(r);
      return v;
    }

    public Enumeration getValidRanges(AspectValue lowerbound, AspectValue upperbound) {
      double minx = lowerbound.getValue();
      double maxx = upperbound.getValue();

      Vector all = getAllValidValues(WORST);
      
      Vector clipped = new Vector();

      boolean filling = false;

      for (int i = 0; i < all.size(); i++) {
        DPair dp = (DPair) all.elementAt(i);
        double x0 = dp.x0;
        double y0 = getScore(x0); // nasty
        double x1 = dp.x1;
        double y1 = getScore(x1); // evil
        if (x0 < maxx) 
          break;                // seg is past range: stop

        if (x1 > minx)       // seg is in range: start
          filling = true;

        if (filling) {
          // cut by range start?
          if (x0 < minx && x1 > minx) {
            x0 = minx;
            x0 = getScore(x0);  //  horribly inefficient
          }
          // cut by range end?
          if (x0 < maxx && x1 > maxx) {
            x1 = maxx;
            y1 = getScore(x1);  // bad
          }

          // add a valid range to clipped vector
          clipped.addElement(new AspectScoreRange(new AspectScorePoint(x0, y0, aspectType),
                                                  new AspectScorePoint(x1, y1, aspectType)));
        }
      }
      return clipped.elements();
    }

    public AspectScoreRange getDefinedRange() {
      return new AspectScoreRange(curve[0], curve[curve.length - 1]);
    }

    public AspectScorePoint getBest() {
      AspectScorePoint best = curve[0];
      double bs = best.getScore();
      int l = curve.length;
      for (int i = 1; i<l; i++) {
        AspectScorePoint asp = curve[i];
        double s = asp.getScore();
        if (s < bs) {
          best = asp;
          bs = s;
        }
      }
      return best;
    }
    public String toString() { return "<PiecewiseLinear "+curve.length+">"; }

  }
  

  /** utility base class for storing single-point SFs **/

  public static abstract class SinglePointScoringFunction extends ScoringFunction {
    protected AspectValue point;

    protected SinglePointScoringFunction(AspectValue value) {
      super(value.getAspectType());
      point = value;
    }

    public AspectValue getPoint() { 
      return point; 
    }

    public boolean equals(Object o) {
      if (o instanceof SinglePointScoringFunction) {
        SinglePointScoringFunction that = (SinglePointScoringFunction) o;
        return that.point.equals(this.point);
      }
      return false;
    }
  }


  /* A single point with straight sides in score space
   * @param value  The single point.
   * @return StrictValueScoringFunction
   */

  public static class StrictValueScoringFunction extends SinglePointScoringFunction {
    private transient AspectScorePoint basp = null;
    private synchronized AspectScorePoint getB() {
      if (basp == null) {
        basp = new AspectScorePoint(point, BEST);
      }
      return basp;
    }

    public StrictValueScoringFunction(AspectValue value) {
      super(value);
    }

    public boolean equals(Object o) {
      if (o instanceof StrictValueScoringFunction) {
        return super.equals(o);
      }
      return false;
    }

    public Object clone() {
      return new StrictValueScoringFunction((AspectValue) point.clone());
    }
  
    public AspectScorePoint getBest() {
      return getB();
    }

    public AspectScorePoint getMinInRange(AspectValue lowerbound, AspectValue upperbound){
      if (point.isBetween(lowerbound, upperbound)) {
        return getB();
      } else {
        return new AspectScorePoint(lowerbound, WORST);
      }
    }
    public AspectScorePoint getMaxInRange(AspectValue lowerbound, AspectValue upperbound){
      if (point.equals(lowerbound) &&
          point.equals(upperbound)) {
        return getB();
      } else {
        return new AspectScorePoint(lowerbound, WORST);      
      }
    }
    public Enumeration getValidRanges(AspectValue lowerbound, AspectValue upperbound) {
      if (point.isBetween(lowerbound, upperbound)) {
        AspectScorePoint asp = getB();
        return new SingleElementEnumeration(new AspectScoreRange(asp, asp));
      } else {
        return Empty.enumeration;
      }
    }
    public double getScore(AspectValue value){
      if (point.equals(value)) {
        return BEST;
      } else {
        return WORST;
      }
    }
    public String toString() { return "<StrictValue "+point+">"; }
  }

  /** classic symmetric V curve.
   *  Score = min(1.0, | value - point | * slope)
   **/

  public static class PreferredValueScoringFunction extends SinglePointScoringFunction {
    protected double slope;

    private transient AspectScorePoint basp = null;
    private synchronized AspectScorePoint getB() {
      if (basp == null) {
        basp = new AspectScorePoint(point, BEST);
      }
      return basp;
    }

    public boolean equals(Object o) {
      if (o instanceof PreferredValueScoringFunction) {
        PreferredValueScoringFunction that = (PreferredValueScoringFunction) o;
        if (that.slope != this.slope) return false;
        return super.equals(o);
      }
      return false;
    }

    public PreferredValueScoringFunction(AspectValue value, double slope) { 
      super(value);
      this.slope = slope;
    }

    public Object clone() {
      return new PreferredValueScoringFunction((AspectValue) point.clone(), slope);
    }
  
    public AspectScorePoint getBest() {
      return getB();
    }

    public Enumeration getValidRanges(AspectValue lowerbound, AspectValue upperbound){
      double v = point.getValue();
      int t = point.getAspectType();
      double delta = (slope==0.0)?(0.0):(1/slope);
      AspectValue v0 = new AspectValue(t, v-delta);
      AspectValue v1 = new AspectValue(t, v+delta);
      AspectScorePoint p0 = new AspectScorePoint(v0, WORST);
      AspectScorePoint p1 = new AspectScorePoint(v1, WORST);
      return new SingleElementEnumeration(new AspectScoreRange(p0,p1));
    }

    public double getScore(AspectValue value){
      return Math.min(WORST, slope * Math.abs( point.minus(value) ));
    }

    public AspectScorePoint getMinInRange(AspectValue lowerbound, AspectValue upperbound){
      AspectScorePoint asp = null;
      if (point.isBetween(lowerbound, upperbound)) 
	asp = getB();
      else if (upperbound.isLessThan(point))
	asp = new AspectScorePoint(upperbound, getScore(upperbound));
      else
	asp = new AspectScorePoint(lowerbound, getScore(lowerbound));
      return asp;
    }

    public AspectScorePoint getMaxInRange(AspectValue lowerbound, AspectValue upperbound){
      AspectScorePoint asp = null;
      double ubScore = getScore(upperbound);
      double lbScore = getScore(lowerbound);

      if (lbScore > ubScore)
	return new AspectScorePoint(lowerbound, lbScore);
      else 
	return new AspectScorePoint(upperbound, ubScore);
    }

    public String toString() { return "<PreferredValue "+point+" "+slope+">"; }
  }
  


  public static abstract class TwoPointScoringFunction extends ScoringFunction {
    protected AspectValue point1, point2;
    protected TwoPointScoringFunction(AspectValue point1, AspectValue point2) {
      super(point1.getAspectType());
      this.point1=point1;
      this.point2=point2;
    }

    public boolean equals(Object o) {
      if (o instanceof TwoPointScoringFunction) {
        TwoPointScoringFunction that = (TwoPointScoringFunction) o;
        return (this.point1 == that.point1 && this.point2 == that.point2);
      }
      return false;
    }

    public AspectValue getPoint1() { 
      return point1; 
    }
    public AspectValue getPoint2() { 
      return point2; 
    }
  }

  public static class VScoringFunction extends TwoPointScoringFunction {
    protected AspectValue best;
    protected double ok;

    public VScoringFunction(AspectValue low, AspectValue best, AspectValue high, double ok) {
      super(low, high);
      this.best = best;
      this.ok = ok;
    }

    public VScoringFunction(AspectValue low, AspectValue best, AspectValue high) {
      super(low, high);
      this.best = best;
      this.ok = OK;
    }

    public boolean equals(Object o) {
      if (o instanceof VScoringFunction) {
        VScoringFunction that = (VScoringFunction) o;
        if (that.ok != this.ok) return false;
        if (!that.best.equals(this.best)) return false;
        return super.equals(o);
      }
      return false;
    }

    public Object clone() {
      return new VScoringFunction((AspectValue) point1.clone(),
                                  (AspectValue) best.clone(),
                                  (AspectValue) point2.clone(),
                                  ok);
    }

    // take the specified best as best
    public AspectScorePoint getBest() {
      return new AspectScorePoint(best, BEST);
    }

    public Enumeration getValidRanges(AspectValue lowerbound, AspectValue upperbound){
      AspectScorePoint p0 = new AspectScorePoint(point1, ok);
      AspectScorePoint p1 = new AspectScorePoint(point2, ok);
      return new SingleElementEnumeration(new AspectScoreRange(p0,p1));
    }

    public double getScore(AspectValue value){
      if (value.equals(best)) {
        return BEST;
      } else if (value.equals(point1) || value.equals(point2)) {
        return ok;
      } else if (value.isBetween(point1, point2)) {
        double vp = value.getValue();
        double p1 = point1.getValue();
        double p2 = point2.getValue();
        double b = best.getValue();
        double slope;
        
        // already caught the boundary conditions above...
        if (vp < b) {           // between low and best
          slope = (BEST-ok)/(b-p1);
          return (ok+(vp-p1)*slope);
        } else {
          slope = (ok - BEST) / (p2 - b);
          return ( BEST+(p2 -vp)*slope );
        }
      } else {
        return WORST;
      }
    }

    public AspectScorePoint getMaxInRange(AspectValue lowerbound,
					  AspectValue upperbound){
      double ubScore = getScore(upperbound);
      double lbScore = getScore(lowerbound);

      if (ubScore < lbScore)
	return new AspectScorePoint(upperbound, ubScore);
      return new AspectScorePoint(lowerbound, lbScore);
    }

    public AspectScorePoint getMinInRange(AspectValue lowerbound, 
					  AspectValue upperbound){

      // best is in range
      if (best.isBetween(lowerbound, upperbound))
	return getBest();

      // Out of range completely
      if (point1.isGreaterThan(upperbound))
	return new AspectScorePoint(upperbound, WORST);

      // Out of range completely
      if (point2.isLessThan(lowerbound))
	return new AspectScorePoint(lowerbound, WORST);

      // Ends on low
      if (point1.equals(upperbound))
	return new AspectScorePoint(upperbound, ok);

      // Starts on high
      if (point2.equals(lowerbound))
	return new AspectScorePoint(lowerbound, ok);

      // On the downslope 
      if (upperbound.isLessThan(best))
	return new AspectScorePoint(upperbound, getScore(upperbound));

      // On the upslope
      return new AspectScorePoint(lowerbound, getScore(lowerbound));
    }
    public String toString() { return "<V "+point1+"-"+point2+" best="+best+" ok="+ok+">"; }
  }


  public static class StrictBetweenScoringFunction
    extends TwoPointScoringFunction {
    public StrictBetweenScoringFunction(AspectValue low, AspectValue high) {
      super(low,high);
    }

    public boolean equals(Object o) {
      if (o instanceof StrictBetweenScoringFunction) {
        return super.equals(o);
      }
      return false;
    }

    public Object clone() {
      return new StrictBetweenScoringFunction((AspectValue) point1.clone(),
                                              (AspectValue) point2.clone());
    }
  
    //arbitrarily take the low value as best for now
    public AspectScorePoint getBest() {
      return new AspectScorePoint(point1, BEST);
    }

    public Enumeration getValidRanges(AspectValue lowerbound, AspectValue upperbound){
      AspectScorePoint p0 = new AspectScorePoint(point1, WORST);
      AspectScorePoint p1 = new AspectScorePoint(point2, WORST);
      return new SingleElementEnumeration(new AspectScoreRange(p0,p1));
    }
    public double getScore(AspectValue value){
      if (value.isBetween(point1, point2))
        return BEST;
      else 
        return WORST;
    }

    public AspectScorePoint getMaxInRange(AspectValue lowerbound,
					  AspectValue upperbound){

      // Begins out of range
      if (!lowerbound.isBetween(point1, point2))
	  return new AspectScorePoint(lowerbound, WORST);

      // Ends out of range
      if (!upperbound.isBetween(point1, point2))
	  return new AspectScorePoint(upperbound, WORST);
      
      // Arbitrarily send back lowerbound
      return new AspectScorePoint(lowerbound, BEST);
    }

    public AspectScorePoint getMinInRange(AspectValue lowerbound, 
					  AspectValue upperbound){

      // starts between
      if (lowerbound.isBetween(point1, point2))
	return new AspectScorePoint(lowerbound, BEST);

      // ends between
      if (upperbound.isBetween(point1, point2))
	return new AspectScorePoint(upperbound, BEST);

      // surrounds
      if (point1.isBetween(lowerbound, upperbound) &&
	  point2.isBetween(lowerbound, upperbound)) 
	// return a point halfway between point1 and point2
	return new AspectScorePoint(new AspectValue(lowerbound.getAspectType(),
						    (point1.getValue() 
						     + point2.getValue()) / 2),
				    BEST);

      // Not in range at all, arbitrarily send back upperbound
      return new AspectScorePoint(upperbound, WORST);
    }    
    public String toString() { return "<StrictBetween "+point1+"-"+point2+">"; }
  }
  
  public static class StrictBetweenWithBest 
    extends StrictBetweenScoringFunction {

    protected AspectValue best;

    public StrictBetweenWithBest(AspectValue low, AspectValue best, AspectValue high) {
      super(low, high);
      this.best = best;
    }

    public Object clone() {
      return new StrictBetweenWithBest((AspectValue) point1.clone(),
                                       (AspectValue) best.clone(),
                                       (AspectValue) point2.clone());
    }

    // take the specified best as best
    public AspectScorePoint getBest() {
      return new AspectScorePoint(best, BEST);
    }

    public AspectScorePoint getMinInRange(AspectValue lowerbound, 
					  AspectValue upperbound){
      if (best.isBetween(lowerbound, upperbound))
	return getBest();

      return super.getMinInRange(lowerbound, upperbound);
    }
    public String toString() { return "<StrictBetween "+point1+"-"+point2+" best="+best+">"; }
  }


  public static class PreferredBetweenScoringFunction
    extends TwoPointScoringFunction {
    protected double slope;
    public PreferredBetweenScoringFunction(AspectValue low, AspectValue high, double slope) {
      super(low,high);
      this.slope = slope;
    }

    public boolean equals(Object o) {
      if (o instanceof PreferredBetweenScoringFunction) {
        PreferredBetweenScoringFunction that = (PreferredBetweenScoringFunction) o;
        if (that.slope != this.slope) return false;
        return super.equals(o);
      }
      return false;
    }

    public Object clone() {
      return new PreferredBetweenScoringFunction((AspectValue) point1.clone(),
                                                 (AspectValue) point2.clone(),
                                                 slope);
    }
  
    //arbitrarily take the low value as best for now
    public AspectScorePoint getBest() {
      return new AspectScorePoint(point1, BEST);
    }

    public Enumeration getValidRanges(AspectValue lowerbound, AspectValue upperbound){
      int t = point1.getAspectType();
      double delta = (slope==0.0)?(0.0):(1/slope);
      AspectValue v0 = new AspectValue(t, point1.getValue()-delta);
      AspectValue v1 = new AspectValue(t, point1.getValue()+delta);
      AspectScorePoint p0 = new AspectScorePoint(v0, WORST);
      AspectScorePoint p1 = new AspectScorePoint(v1, WORST);
      return new SingleElementEnumeration(new AspectScoreRange(p0,p1));
    }
    public double getScore(AspectValue value){
      if (value.isLessThan(point1)) {
        return Math.min(WORST, slope * point1.minus(value));
      } else if (value.isGreaterThan(point2)) {
        return Math.min(WORST, slope * value.minus(point2));
      } else {
        return BEST;
      }
    }
    public AspectScorePoint getMinInRange(AspectValue lowerbound, 
					  AspectValue upperbound){
      // range spans basin
      if (point1.isBetween(lowerbound, upperbound) &&
	  point2.isBetween(lowerbound, upperbound)){
	// pick halfway point in basin
	AspectValue halfpoint = new AspectValue(lowerbound.getAspectType(),
						(point1.getValue()
						+ point2.getValue()) / 2);
	return new AspectScorePoint(halfpoint, getScore(halfpoint));
      }

      double ubScore = getScore(upperbound);
      double lbScore = getScore(lowerbound);

      // range begins or ends in basin
      if (lowerbound.isBetween(point1, point2))
	return new AspectScorePoint(lowerbound, lbScore);
      if (upperbound.isBetween(point1, point2))
	return new AspectScorePoint(upperbound, ubScore);

      // one of the end points is the min
      if (lbScore < ubScore)
	return new AspectScorePoint(lowerbound, lbScore);
      return new AspectScorePoint(upperbound, ubScore);

    }

    public AspectScorePoint getMaxInRange(AspectValue lowerbound, 
					  AspectValue upperbound){
      double ubScore = getScore(upperbound);
      double lbScore = getScore(lowerbound);
      // max has to be one of the end points
      if (lbScore > ubScore)
	return new AspectScorePoint(lowerbound, lbScore);
      return new AspectScorePoint(upperbound, ubScore);
      
    }
    public String toString() { return "<PreferredBetween "+point1+"-"+point2+" slope="+slope+">"; }
  }
  

  public static class AboveScoringFunction
    extends SinglePointScoringFunction {
    private double slope;
    public AboveScoringFunction(AspectValue value, double slope) {
      super(value);
      this.slope = slope;
    }

    public boolean equals(Object o) {
      if (o instanceof AboveScoringFunction) {
        AboveScoringFunction that = (AboveScoringFunction) o;
        if (that.slope != this.slope) return false;
        return super.equals(o);
      }
      return false;
    }

    public Object clone() {
      return new AboveScoringFunction((AspectValue) point.clone(), slope);
    }
  
    public AspectScorePoint getBest() {
      return new AspectScorePoint(point, BEST);
    }
  
    public AspectScorePoint getMinInRange(AspectValue lowerbound, AspectValue upperbound){
      AspectScorePoint asp = null;
      if (point.isBetween(lowerbound, upperbound)) 
	asp = new AspectScorePoint(point, BEST);
      else if (point.isGreaterThan(upperbound))
	asp = new AspectScorePoint(upperbound, WORST);
      else
	asp = new AspectScorePoint(lowerbound, getScore(lowerbound));
      return asp;
    }

    public AspectScorePoint getMaxInRange(AspectValue lowerbound, AspectValue upperbound){
      AspectScorePoint asp = null;
      double ubScore = getScore(upperbound);
      double lbScore = getScore(lowerbound);
      // One of the endpoints is Max
      if (lbScore > ubScore)
	asp = new AspectScorePoint(lowerbound, lbScore);
      else
	asp = new AspectScorePoint(upperbound, ubScore);
      return asp;
    }

    public double getScore(AspectValue value){
      if (!(value.isLessThan(point))) {
        return Math.min(WORST, slope * value.minus(point));
      } else {
        return WORST;
      }
    }

    public Enumeration getValidRanges(AspectValue lowerbound, 
				      AspectValue upperbound){

      double lbScore = getScore(lowerbound);
      double ubScore = getScore(upperbound);

      // Whole range in safe area
      if ((lbScore < WORST) && (ubScore < WORST)){
	// return range
	  AspectScorePoint p0 = new AspectScorePoint(lowerbound, lbScore);
	  AspectScorePoint p1 = new AspectScorePoint(upperbound, ubScore);
	  return new SingleElementEnumeration(new AspectScoreRange(p0,p1));
      }

      // Whole range in WORST area
      if (!point.isBetween(lowerbound, upperbound) &&
	  (lbScore == WORST) && (ubScore == WORST)){
	// return empty set
	return Empty.enumeration;
      }

      AspectScorePoint p0;
      AspectScorePoint p1;
      // Starts before point
      if (lowerbound.isLessThan(point))
	// use point
	p0 = new AspectScorePoint(point, getScore(point));
      else
	// use lb
	p0 = new AspectScorePoint(lowerbound, lbScore);

      if (ubScore < WORST)
	// use ub
	p1 = new AspectScorePoint(upperbound, ubScore);
      else {
        double delta = (slope==0.0)?(0.0):(1/slope);
	AspectValue v1 = new AspectValue(point.getAspectType(), 
					 point.getValue()+delta);
	p1 = new AspectScorePoint(v1, WORST);
      }
      return new SingleElementEnumeration(new AspectScoreRange(p0,p1));
    }
    public String toString() { return "<Above "+point+" "+slope+">"; }
  }
  
  public static class BelowScoringFunction
    extends SinglePointScoringFunction {
    private double slope;
    public BelowScoringFunction(AspectValue value, double slope) {
      super(value);
      this.slope = slope;
    }

    public boolean equals(Object o) {
      if (o instanceof BelowScoringFunction) {
        BelowScoringFunction that = (BelowScoringFunction) o;
        if (that.slope != this.slope) return false;
        return super.equals(o);
      }
      return false;
    }

    public Object clone() {
      return new BelowScoringFunction((AspectValue) point.clone(), slope);
    }
  
    public AspectScorePoint getBest() {
      return new AspectScorePoint(point, BEST);
    }

    public AspectScorePoint getMinInRange(AspectValue lowerbound, AspectValue upperbound){
      AspectScorePoint asp = null;
      if (point.isBetween(lowerbound, upperbound)) 
	asp = new AspectScorePoint(point, BEST);
      else if (point.isGreaterThan(upperbound))
	asp = new AspectScorePoint(upperbound, getScore(upperbound));
      else
	asp = new AspectScorePoint(lowerbound, getScore(lowerbound));
      return asp;
    }

    public AspectScorePoint getMaxInRange(AspectValue lowerbound, AspectValue upperbound){
      AspectScorePoint asp = null;
      double ubScore = getScore(upperbound);
      double lbScore = getScore(lowerbound);
      // One of the endpoints is Max
      if (lbScore > ubScore)
	asp = new AspectScorePoint(lowerbound, lbScore);
      else
	asp = new AspectScorePoint(upperbound, ubScore);
      return asp;
    }

  
    public double getScore(AspectValue value){
      if (!(value.isGreaterThan(point))) {
        return Math.min(WORST, slope * point.minus(value));
      } else {
        return WORST;
      }
    }

    public Enumeration getValidRanges(AspectValue lowerbound, 
				      AspectValue upperbound){
      double lbScore = getScore(lowerbound);
      double ubScore = getScore(upperbound);

      // Whole range in safe area
      if ((lbScore < WORST) && (ubScore < WORST)){
	// return range
	  AspectScorePoint p0 = new AspectScorePoint(lowerbound, lbScore);
	  AspectScorePoint p1 = new AspectScorePoint(upperbound, ubScore);
	  return new SingleElementEnumeration(new AspectScoreRange(p0,p1));
      }

      // Whole range in WORST area
      if (!point.isBetween(lowerbound, upperbound) &&
	  (lbScore == WORST) && (ubScore == WORST)){
	// return empty set
	return Empty.enumeration;
      }


      AspectScorePoint p0;
      if (lbScore < WORST) {
	// lowerbound in good range
	p0 = new AspectScorePoint(lowerbound, lbScore);
      } else {
        double delta = (slope==0.0)?(0.0):(1/slope);
	AspectValue v0 = new AspectValue(point.getAspectType(), 
					 point.getValue()-delta);
	p0 = new AspectScorePoint(v0, WORST);
      }

      AspectScorePoint p1;
      if (ubScore < WORST)
	// use upperbound
        p1 = new AspectScorePoint(upperbound, ubScore);
      else
	// use point
	p1 = new AspectScorePoint(point, getScore(point));

      return new SingleElementEnumeration(new AspectScoreRange(p0,p1));

    }
    public String toString() { return "<Below "+point+" "+slope+">"; }
  }
  
  public static class StepScoringFunction extends SinglePointScoringFunction {
    double v0;
    double v1;
    public StepScoringFunction(AspectValue changepoint, double prescore, double postscore) {
      super(changepoint);
      v0 = prescore;
      v1 = postscore;
    }

    public boolean equals(Object o) {
      if (o instanceof StepScoringFunction) {
        StepScoringFunction that = (StepScoringFunction) o;
        if (that.v0 != this.v0) return false;
        if (that.v1 != this.v1) return false;
        return super.equals(o);
      }
      return false;
    }

    public Object clone() {
      return new StepScoringFunction((AspectValue) point.clone(), v0, v1);
    }

    public double getScore(AspectValue value){
      if (value.isLessThan(point)) 
        return v0;
      else 
        return v1;
    }
    public AspectScorePoint getMinInRange(AspectValue lowerbound, 
					  AspectValue upperbound){

      double lbScore = getScore(lowerbound);
      double ubScore = getScore(upperbound);

      if (lbScore < ubScore)
	return new AspectScorePoint(lowerbound, lbScore);
      return new AspectScorePoint(upperbound, ubScore);
    }
    public AspectScorePoint getMaxInRange(AspectValue lowerbound, AspectValue upperbound){
      double lbScore = getScore(lowerbound);
      double ubScore = getScore(upperbound);
      if (lbScore > ubScore)
	return new AspectScorePoint(lowerbound, lbScore);
      return new AspectScorePoint(upperbound, ubScore);
    }

    /**
     * return changepoint if its score is lower, 
     * otherwise return a point -x away from changepoint
     **/
    public AspectScorePoint getBest() {
      // which is lower?
      double lowscore = (v0 < v1) ? v0: v1;

      // _____
      //      |
      //      |
      //      |____
      //      
      if (getScore(point) == lowscore)
	return new AspectScorePoint(point, lowscore);

      // v1 < v0, but point is at zero
      //______
      //
      //
      if (point.getValue() == 0)
	return new AspectScorePoint(point, getScore(point));

      //      ______
      //      |
      //      |
      // _____|
      // arbitrary value in the range between 0 and point
      // what if point<0 ?
      double away = 1.0;
      double pointValue = point.getValue();
      while (pointValue - away <= 0)
	away = away * 0.1;
      
      return new AspectScorePoint(new AspectValue(point.getAspectType(),
						  pointValue - away),
				  lowscore);
      
    }

    public Enumeration getValidRanges(AspectValue lowerbound, 
				      AspectValue upperbound){

      double lbScore = getScore(lowerbound);
      double ubScore = getScore(upperbound);

      // Either the "bad" score is less than 1.0
      // -or-
      // The range is on the "good" side of the point
      if (((v0 < WORST) && (v1 < WORST)) ||
	  ((lbScore < WORST) && (ubScore < WORST))){
	// send back the whole range
	AspectScorePoint p0 = new AspectScorePoint(lowerbound, 
						   lbScore);
	AspectScorePoint p1 = new AspectScorePoint(upperbound, 
						   ubScore);
	return new SingleElementEnumeration(new AspectScoreRange(p0,p1));
      }


      if (point.isBetween(lowerbound, upperbound)) {
	if (v0 == WORST){
	  // return from point to upperbound
	  AspectScorePoint p0 = new AspectScorePoint(point, v1);
	  AspectScorePoint p1 = new AspectScorePoint(upperbound, v1);
	  return new SingleElementEnumeration(new AspectScoreRange(p0,p1));
	} else {
	  // return from lowerbound to point
	  AspectScorePoint p0 = new AspectScorePoint(lowerbound, v0);
	  AspectScorePoint p1 = new AspectScorePoint(point, v1);
	  return new SingleElementEnumeration(new AspectScoreRange(p0,p1));
	}
      }

      // Entire range WORST
      return Empty.enumeration;
    }
    
    public String toString() { return "<Step "+point+" "+v0+" "+v1+">"; }
  }

  /* A scoring function for an enumerated set of AspectScorePoints
   * Ranges are ignored, since enumerations have no sense of comparison or 
   * continuity. All values not in enumeration give a value of WORST.
   */
  public static class EnumeratedScoringFunction extends ScoringFunction {

    // Maintain list of all provided points
    // NOTE : Could implement this as a hash table if lists get long
    private AspectScorePoint []my_points;

    /**
     * Constructor for EnumeratedScoringFunction
     **/
    public EnumeratedScoringFunction(AspectScorePoint []points) {
      super(points[0].getAspectType());
      my_points = points;  
    }

    public Object clone() {
      AspectScorePoint[] newPoints = new AspectScorePoint[my_points.length];
      for (int i = 0; i < newPoints.length; i++) {
        newPoints[i] = (AspectScorePoint) my_points[i].clone();
      }
      return new EnumeratedScoringFunction(newPoints);
    }
  
    // Find aspectscore point with max/min based on flag
    private AspectScorePoint findExtreme(boolean find_max)
    {
      AspectScorePoint current = null;
      for(int i = 0; i < my_points.length; i++) {
        if ((current == null) || 
            ((find_max == false) && 
             (current.getScore() > my_points[i].getScore())) ||
            ((find_max == true) && 
             (current.getScore() < my_points[i].getScore())))
          { 
            current = my_points[i];
          }
      }
      return current;
    }

    /**
     * getBest AspectScorePoint - iterate over all and find lowest score
     */
    public AspectScorePoint getBest() {
      return findExtreme(false);
    }

    /**
     * Find minimum value - ignore range information
     **/
    public AspectScorePoint getMinInRange(AspectValue lowerbound, AspectValue upperbound){
      return findExtreme(true);
    }

    public AspectScorePoint getMaxInRange(AspectValue lowerbound, AspectValue upperbound){
      return getBest();
    }

    /**
     * Enumeration of all valid ranges : in this case, all points
     * represent a discrete range in and of themselves
     **/
    public Enumeration getValidRanges(AspectValue lowerbound, AspectValue upperbound) {
      Vector v = new Vector();
      for (int i = 0; i < my_points.length; i++) {
        v.addElement(new AspectScoreRange(my_points[i], my_points[i]));
      }
      return v.elements();
    }

    /** 
     * Iterate over all points and find if there is a point for this 
     * value. If not, return WORST.
     **/
    public double getScore(AspectValue value){
      for(int i = 0; i < my_points.length; i++) {
        if (my_points[i].getValue() == value.getValue()) {
          return my_points[i].getScore();
        }
      }
      return WORST;
    }
    public String toString() { return "<Enumerated "+my_points.length+">"; }
  }


  public static class ConstantScoringFunction extends ScoringFunction {
    double score=BEST;

    public ConstantScoringFunction(double score, int aspectType) {
      super(aspectType);
      this.score = score;
    }

    public ConstantScoringFunction(int aspectType) {
      super(aspectType);
    }

    public Object clone() {
      return new ConstantScoringFunction(score, aspectType);
    }

    public AspectScorePoint getBest(){
      return new AspectScorePoint(0, score, aspectType);
    }

    public AspectScorePoint getMinInRange(AspectValue lowerbound, 
					  AspectValue upperbound){
      return new AspectScorePoint(lowerbound.getValue(),score, aspectType);
    }      

    public AspectScorePoint getMaxInRange(AspectValue lowerbound, 
					  AspectValue upperbound){
      return new AspectScorePoint(upperbound.getValue(),score, aspectType);
    }

    public Enumeration getValidRanges(AspectValue lowerbound,
				      AspectValue upperbound) {
      AspectScorePoint low = 
	new AspectScorePoint(lowerbound, score);
      AspectScorePoint high = 
	new AspectScorePoint(upperbound, score);
      return new SingleElementEnumeration(new AspectScoreRange(low, high));
    }

    public double getScore(AspectValue value){
      return score;
    }
    public String toString() { return "<Constant "+score+">"; }
  }
}

/*
    public AspectScorePoint getMinInRange(AspectValue lowerbound, AspectValue upperbound) {
    }
    public AspectScorePoint getMaxInRange(AspectValue lowerbound, AspectValue upperbound) {
    }
    public Enumeration getValidRanges(AspectValue lowerbound, AspectValue upperbound) {
    }
    public AspectScoreRange getDefinedRange() {
    }

 */
