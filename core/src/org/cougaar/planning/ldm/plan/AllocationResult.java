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

import java.io.Serializable;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.util.*;

/**
 * The "result" of allocating a task.
 **/

public class AllocationResult
  implements AspectType, AuxiliaryQueryType, Serializable, Cloneable
{
                                    
  private boolean isSuccess;
  private float confrating;
  private AspectValue[] avResults = null;
  private ArrayList phasedavrs = null;      // A List of AspectValue[], null if not phased
  private String[] auxqueries = null;
  
  /** Constructor that takes a result in the form of AspectValues (NON-PHASED).
   * Subclasses of AspectValue, such as TypedQuantityAspectValue are allowed.
   * @param rating The confidence rating of this result.
   * @param success  Whether the allocationresult violated any preferences.
   * @param aspectvalues  The AspectValues(can be aspectvalue subclasses) that represent the results.  
   * @note Prior to Cougaar 10.0, there was a similar constructor which took an int[] and double[] instead of 
   * the current AspectValue[].  This change is required because most ApectValue types
   * are not longer represented by int/double pairs.  This constructor may be made 
   * private at some point in the future.
   */
  public AllocationResult(double rating, boolean success, AspectValue[] aspectvalues) {
    isSuccess = success;
    setAspectValueResults((AspectValue[]) aspectvalues.clone());
    confrating = (float) rating;
  }

  /** Factory that takes a result in the form of AspectValues (NON-PHASED).
   * Subclasses of AspectValue, such as TypedQuantityAspectValue are allowed.
   * @param rating The confidence rating of this result.
   * @param success  Whether the allocationresult violated any preferences.
   * @param avs  The AspectValues(can be aspectvalue subclasses) that represent the results.  
   */
  public static AllocationResult newAllocationResult(double rating, boolean success, AspectValue[] avs) {
    return new AllocationResult(rating, success, avs);
  }

  /** @deprecated Use #AllocationResult(double,boolean,AspectValue[]) instead because
   * AspectValues are not all describable by double values.
   **/
  public AllocationResult(double rating, boolean success, int[] keys, double[] values) {
    this(rating, success, convertToAVA(keys,values));
  }

  /** Simple Constructor for a PHASED result
   * @param rating The confidence rating of this result.
   * @param success  Whether the allocationresult violated any preferences.
   * @param rollupavs  The Summary (or rolled up) AspectValues that represent the results.
   * @param allresults  An Enumeration of either AspectValue[]s or Collection<AspectValue>s.
   * @note Prior to Cougaar 10.0, this constructor took an int[] and double[] instead of 
   * the current AspectValue[].  This change is required because most ApectValue types
   * are not longer represented by int/double pairs.
   * @deprecated Use #AllocationResult(double, boolean, AspectValue[], Collection) instead.
   */
  public AllocationResult(double rating, boolean success, AspectValue[] rollupavs, Enumeration allresults)
  {
    isSuccess = success;
    setAspectValueResults((AspectValue[]) rollupavs.clone());
    setPhasedResults(allresults);
    confrating = (float) rating;
  }

  /** @deprecated Use #AllocationResult(double,boolean,AspectValue[],Collection) instead because
   * AspectValues are not all describable by double values.
   **/
  public AllocationResult(double rating, boolean success, int[] keys, double[] values, Enumeration allresults) {
    this(rating, success, convertToAVA(keys,values), allresults);
  }


  /** Constructor that takes a PHASED result in the form of AspectValues.
   * Subclasses of AspectValue, such as TypedQuantityAspectValue are allowed.
   * @param rating The confidence rating of this result.
   * @param success  Whether the allocationresult violated any preferences.
   * @param rollupavs  The Summary (or rolled up) AspectValues that represent the results.
   * @param phasedresults  A Collections of the phased results. The Collection should contain
   * one Collection or AspectValue[] of AspectValues for each phase of the results.  
   * @note The associated factory is preferred as the constructor may be made private in
   * a future version.
   */
  public AllocationResult(double rating, boolean success, AspectValue[] rollupavs, Collection phasedresults) {
    isSuccess = success;
    setAspectValueResults((AspectValue[]) rollupavs.clone());
    setPhasedResults(phasedresults);
    confrating = (float) rating;
  }

  /** AllocationResult factory that takes a PHASED result in the form of AspectValues.
   * Subclasses of AspectValue, such as TypedQuantityAspectValue are allowed.
   * @param rating The confidence rating of this result.
   * @param success  Whether the allocationresult violated any preferences.
   * @param rollupavs  The Summary (or rolled up) AspectValues that represent the results.
   * @param phasedresults  A Collections of the phased results. The Collection should contain
   * one Collection or AspectValue[] of AspectValues for each phase of the results.  
   */
  public static AllocationResult newAllocationResult(double rating, boolean success, AspectValue[] rollupavs, Collection phasedresults) {
    return new AllocationResult(rating, success, rollupavs, phasedresults);
  }

  /**
   * Construct a merged AllocationResult containing AspectValues from
   * two AllocationResults. If both arguments have the same aspects,
   * the values from the first (dominant) result are used. The result
   * is never phased.
   * @note The associated factory is preferred as the constructor may be made private in
   * a future version.
   **/
  public AllocationResult(AllocationResult ar1, AllocationResult ar2) {
    int len1 = ar1.avResults.length;
    int len2 = ar2.avResults.length;
    List mergedavs = new ArrayList(len1 + len2);
  outer:
    for (int i = 0; i < len2; i++) {
      AspectValue av2 = ar2.avResults[i];
      int aspectType = av2.getAspectType();
      for (int j = 0; j < len1; j++) {
        if (aspectType == ar1.avResults[j].getAspectType()) {
          continue outer;       // Already have this AspectType
        }
      }
      mergedavs.add(av2);
    }
    mergedavs.addAll(Arrays.asList(ar1.avResults));
    int nAspects = mergedavs.size();
    avResults = (AspectValue[]) mergedavs.toArray(new AspectValue[nAspects]);
    confrating = (ar1.confrating * len1 + ar2.confrating * (nAspects - len1)) / nAspects;

    if (ar1.auxqueries != null) {
      auxqueries = (String[]) ar1.auxqueries.clone();
    }
    if (ar2.auxqueries != null) {
      String[] mergedQueries = assureAuxqueries();
      for (int i = 0; i < AQTYPE_COUNT; i++) {
        if (mergedQueries[i] == null) mergedQueries[i] = ar2.auxqueries[i];
      }
    }
    // checkAVResults();
    isSuccess = ar1.isSuccess() || ar2.isSuccess();
  }

  /**
   * Construct a merged AllocationResult containing AspectValues from
   * two AllocationResults. If both arguments have the same aspects,
   * the values from the first (dominant) result are used. The result
   * is never phased.
   **/
  public static AllocationResult newAllocationResult(AllocationResult ar1, AllocationResult ar2) {
    return new AllocationResult(ar1, ar2);
  }

  public Object clone() {
    return new AllocationResult(this);
  }

  private AllocationResult(AllocationResult ar) {
    confrating = ar.confrating;
    isSuccess = ar.isSuccess;
    avResults = (AspectValue[]) ar.avResults.clone();
    if (ar.phasedavrs != null) {
      phasedavrs = new ArrayList(ar.phasedavrs.size());
      for (Iterator i = ar.phasedavrs.iterator(); i.hasNext(); ) {
        AspectValue[] av = (AspectValue[]) i.next();
        phasedavrs.add(av.clone());
      }
    }
    if (ar.auxqueries != null) auxqueries = (String[]) ar.auxqueries.clone();
  }


  private int getIndexOfType(int aspectType) {
    if (aspectType == _lasttype) return _lastindex; // Use memoized value
    for (int i = 0 ; i < avResults.length; i++) {
      if (avResults[i].getAspectType() == aspectType) return i;
    }
    return -1;
  }

  //AllocationResult interface implementation.

  /** Get the result with respect to a given AspectType. 
   * If the AllocationResult is phased, this method will return
   * the summary value of the given AspectType.
   * <P> Warning!!! Not all AspectValues can be simply represented as
   * a double. Use of this method with such AspectValues is undefined.
   * @param aspectType
   * @return double The result of a given dimension. For example, 
   * getValue(AspectType.START_TIME) returns the Task start time.
   * Note : results are not required to contain data in each dimension - 
   * check the array of defined aspecttypes or ask if a specific
   * dimension is defined.  If there is a request for a value of an
   * undefined aspect, an IllegalArgumentException will be thrown.
   * @see org.cougaar.planning.ldm.plan.AspectType
   */
  public double getValue(int aspectType) {
    synchronized (avResults) {
      if (_lasttype == aspectType) 
        return avResults[_lastindex].getValue(); // return memoized value
      int i = getIndexOfType(aspectType);
      if (i >= 0)
        return avResults[i].getValue();
    }
    // didn't find it.
    throw new IllegalArgumentException("AllocationResult.getValue(int "
                                       + aspectType
                                       + ") - The AspectType is not defined by this Result.");
  }


  /** Get the AspectValue of the result with the specified type **/
  public AspectValue getAspectValue(int aspectType) {
    synchronized (avResults) {
      if (_lasttype == aspectType) 
        return avResults[_lastindex];
      int i = getIndexOfType(aspectType);
      if (i >= 0)
        return avResults[i];
    }
    // didn't find it.
    throw new IllegalArgumentException("AllocationResult.getAspectValue(int "
                                       + aspectType
                                       + ") - The AspectType is not defined by this Result.");
  }

  /** Quick check to see if one aspect is defined as opposed to
    * looking through the AspectType array.
    * @param aspectType  The aspect you are checking
    * @return boolean Represents whether this aspect is defined
    * @see org.cougaar.planning.ldm.plan.AspectType
    */
  public boolean isDefined(int aspectType) {
    int i = getIndexOfType(aspectType);
    if (i >= 0) {
      _lasttype = aspectType;
      _lastindex = i; // memoize lookup
      return true;
    }
    return false;
  }
    
          
  /** @return boolean Represents whether or not the allocation 
   * was a success. If any Constraints were violated by the 
   * allocation, then the isSuccess() method returns false 
   * and the Plugin that created the subtask should
   * recognize this event. The Expander may re-expand, change the 
   * Constraints or Preferences, or indicate failure to its superior. 
   * The AspectValues of a failed allocation may be set by the Allocator
   * with values that would be more likely to be successful. 
   * The Expander can use these new values as suggestions when 
   * resetting the Preferences
   */
  public boolean isSuccess() {
    return isSuccess;
  }

  /** @return boolean Represents whether or not the allocation
   * result is phased.
   */
  public boolean isPhased() {
    return phasedavrs != null;
  }

  // Memoized variables
  private transient int[] _ats = null;// Array of aspect types
  private transient int _lasttype=-1; // Type of last type to index conversion
  private transient int _lastindex=-1; // Index of last type to index conversion
  private transient double[] _rs = null;

  private synchronized void clearMemos() {
    _ats = null;
    _lasttype=-1;
    _lastindex=-1;
    _rs = null;
  }

  /** A Collection of AspectTypes representative of the type and
   * order of the aspects in each the result.
   * @return int[]  The array of AspectTypes
   * @see org.cougaar.planning.ldm.plan.AspectType   
   */
  public synchronized int[] getAspectTypes() {
    synchronized (avResults) {
      if (_ats != null) return _ats;
      _ats = new int[avResults.length];
      for (int i = 0; i < avResults.length; i++) {
        _ats[i] = avResults[i].getAspectType();
      }
      return _ats;
    }
  }
  
  /** A collection of doubles that represent the result for each
   * AspectType.  If the result is phased, the results are 
   * summarized.
   * <P> Warning!!! Not all AspectValues can be simply represented as
   * a double. Use of this method with such AspectValues is undefined.
   * @return double[]
   */
  public double[] getResult() {
    return convertToDouble(avResults);
  }

  private double[] convertToDouble(AspectValue[] avs) {
    double[] result = new double[avs.length];
    for (int i = 0; i < avs.length; i++) {
      result[i] = avs[i].getValue();
    }
    return result;
  }
  
  /** A collection of AspectValues that represent the result for each
   * preference.  Note that subclasses of AspectValue such as
   * TypedQuantityAspectValue may be used.  If this was not
   * defined through a constructor, one will be built from the result
   * and aspecttype arrays.  In this case only true AspectValue
   * objects will be build (no subclasses of AspectValues).
   * The AspectValues of a failed allocation may be set by the Allocator
   * with values that would be more likely to be successful. 
   * The Expander can use these new values as suggestions when 
   * resetting the Preferences.
   * @note Will always return a new AspectValue[]
   **/
  public AspectValue[] getAspectValueResults() {
    // go ahead and return it if its defined, otherwise create it
    return (AspectValue[]) avResults.clone();
  }
        
  /** A collection of arrays that represents each phased result.
   * If the result is not phased, use AllocationResult.getResult()
   * <P> Warning!!! Not all AspectValues can be simply represented as
   * a double. Use of this method with such AspectValues is undefined.
   * @return Enumeration<AspectValue[]> 
   */  
  public Enumeration getPhasedResults() {
    if (!isPhased()) throw new IllegalArgumentException("Not phased");
    return new Enumeration() {
      Iterator iter = phasedavrs.iterator();
      public boolean hasMoreElements() {
        return iter.hasNext();
      }
      public Object nextElement() {
        AspectValue[] avs = (AspectValue[]) iter.next();
        return convertToDouble(avs);
      }
    };
  }
  
  /** A List of Lists that represent each phased result in the form
   * of AspectValues.
   * If the result is not phased, use getAspectValueResults()
   * @return A List<AspectValue[]>. If the AllocationResult is not phased, will return null.
   */
  public List getPhasedAspectValueResults() {
    if (phasedavrs == null) {
      return null;
    } else {
      return new ArrayList(phasedavrs);
    }
  }
        
  /** @return double The confidence rating of this result. */
  public double getConfidenceRating() {
    return confrating;
  }
  
  /** Return the String representing the auxilliary piece of data that 
   *  matches the query type given in the argument.  
   *  @param aqtype  The AuxiliaryQueryType you want the data for.
   *  @return String  The string representing the data matching the type requested 
   *  Note: may return null if nothing was defined
   *  @see org.cougaar.planning.ldm.plan.AuxiliaryQueryType
   *  @throws IllegalArgumentException if the int passed in as an argument is not a defined
   *  AuxiliaryQueryType
   **/
  public String auxiliaryQuery(int aqtype) {
    if ( (aqtype < 0) || (aqtype > LAST_AQTYPE) ) {
      throw new IllegalArgumentException("AllocationResult.auxiliaryQuery(int) expects an int "
        + "that is represented in org.cougaar.planning.ldm.plan.AuxiliaryQueryType");
    }
    if (auxqueries == null)
      return null;
    else
      return auxqueries[aqtype];
  }
  
  
  //NewAllocationResult interface implementations
  
  /** Set a single AuxiliaryQueryType and its data (String).
   *  @param aqtype The AuxiliaryQueryType
   *  @param data The string associated with the AuxiliaryQueryType
   *  @see org.cougaar.planning.ldm.plan.AuxiliaryQueryType
   **/
  public void addAuxiliaryQueryInfo(int aqtype, String data) {
    if ( (aqtype < 0) || (aqtype > LAST_AQTYPE) ) {
      throw new IllegalArgumentException("AllocationResult.addAuxiliaryQueryInfo(int, String) expects an int "
        + "that is represented in org.cougaar.planning.ldm.plan.AuxiliaryQueryType");
    }
    assureAuxqueries();
    auxqueries[aqtype] = data;
  }
  
  
  /** @param success Represents whether or not the allocation 
   * was a success. If any Constraints were violated by the 
   * allocation, then the isSuccess() method returns false 
   * and the Plugin that created the subtask should
   * recognize this event. The Expander may re-expand, change the 
   * Constraints or Preferences, or indicate failure to its superior. 
   */
  private  void setSuccess(boolean success) {
    isSuccess = success;
  }
  
  /** Set the aspectvalues results and also split apart the array to set
   * the aspecttype and result array.
   * @param aspectvalues  The AscpectValues representing the result of each aspect.
   */
  private void setAspectValueResults(AspectValue[] avresult) {
    avResults = avresult;
    checkAVResults();
    clearMemos();
  }

  private void checkAVResults() {
    for (int i = 0; i < avResults.length; i++) {
      if (avResults[i] == null) throw new NullPointerException("null AspectValue at index " + i);
    }
  }
        
  /** Set the phased results
   * @param theresults  An Enumeration of AspectValue[]
   */
  private void setPhasedResults(Enumeration theResults) {
    phasedavrs = new ArrayList();
    while (theResults.hasMoreElements()) {
      phasedavrs.add(convertAVO(theResults.nextElement()));
    }
    phasedavrs.trimToSize();
  }
  
  /** Set the results based on a collection of AspectValue[] representing each phase of the result
   * @param theresults  
   */
  private void setPhasedResults(Collection theResults) {
    int l = theResults.size();
    phasedavrs = new ArrayList(l);
    if (theResults instanceof List) {
      List trl = (List) theResults;
      for (int i=0; i<l; i++) {
        phasedavrs.add(convertAVO(trl.get(i)));
      }
    } else {
      for (Iterator it = theResults.iterator(); it.hasNext(); ) {
        phasedavrs.add(convertAVO(it.next()));
      }
    }
  }

  /** Convert an logical array of AspectValues to an actual AspectValue[], if needed **/
  // fixes bug 1968
  private AspectValue[] convertAVO(Object o) {
    if (o instanceof AspectValue[]) {
      return (AspectValue[]) o;
    } else if (o instanceof Collection) {
      return (AspectValue[]) ((Collection)o).toArray(new AspectValue[((Collection)o).size()]);
    } else {
      throw new IllegalArgumentException("Each element of a PhaseResult must be in the form of an AspectValue[] or a Collection of AspectValues, but got: "+o);
    }
  }
    
                
  /** @param rating The confidence rating of this result. */
  private void setConfidenceRating(double rating) {
    confrating = (float) rating;
  }
  
  /** checks to see if the AllocationResult is equal to this one.
     * @param anAllocationResult
     * @return boolean
     */
  public boolean isEqual(AllocationResult that) {
    if (this == that) return true; // quick success
    if (that == null) return false; // quick fail
    if (!(this.isSuccess() == that.isSuccess() &&
          this.isPhased() == that.isPhased() &&
          this.getConfidenceRating() == that.getConfidenceRating())) {
      return false;
    }
       
    //check the real stuff now!
    //check the aspect types
    //check the summary results
    synchronized (avResults) {
      if (!AspectValue.nearlyEquals(this.avResults, that.avResults)) return false;
      // check the phased results
      if (isPhased()) {
        Iterator i1 = that.phasedavrs.iterator();
        Iterator i2 = this.phasedavrs.iterator();
        while (i1.hasNext()) {
          if (!i2.hasNext()) return false;
          if (!AspectValue.nearlyEquals((AspectValue[]) i1.next(), (AspectValue[]) i2.next())) return false;
        }
        if (i2.hasNext()) return false;
      }
    }

    // check the aux queries
    
    String[] taux = that.auxqueries;
    if (auxqueries != taux) {
      if (!Arrays.equals(taux, auxqueries)) return false;
    }

    // must be equals...
    return true;
  }

  // added to support AllocationResultBeanInfo

  public String[] getAspectTypesAsArray() {
    String[] aspectStrings = new String[avResults.length];
    for (int i = 0; i < aspectStrings.length; i++)
      aspectStrings[i] =  AspectValue.aspectTypeToString(avResults[i].getAspectType());
    return aspectStrings;
  }

  public String getAspectTypeFromArray(int i) {
    synchronized (avResults) {
      if (i < 0 || i >= avResults.length)
        throw new IllegalArgumentException("AllocationResult.getAspectType(int " + i + " not defined.");
      return AspectValue.aspectTypeToString(avResults[i].getAspectType());
    }
  }

  public String[] getResultsAsArray() {
    synchronized (avResults) {
      String[] resultStrings = new String[avResults.length];
      for (int i = 0; i < resultStrings.length; i++) {
        resultStrings[i] = getResultFromArray(i);
      }
      return resultStrings;
    }
  }

  public String getResultFromArray(int i) {
    synchronized (avResults) {
      if (i < 0 || i >= avResults.length)
        throw new IllegalArgumentException("AllocationResult.getAspectType(int " + i + " not defined.");
      int type = avResults[i].getAspectType();
      double value = avResults[i].getValue();
      if (type == AspectType.START_TIME || 
	  type == AspectType.END_TIME) {
	Date d = new Date((long) value);
	return d.toString();
      } else {
        return String.valueOf(value);
      }
    }
  }

  /**
   * Return phased results.
   * <P> Warning!!! Not all AspectValues can be simply represented as
   * a double. Use of this method with such AspectValues is undefined.
   * @return an array of an array of doubles
   **/
  public double[][] getPhasedResultsAsArray() {
    int len = (isPhased() ? phasedavrs.size() : 0);
    double[][] d = new double[len][];
    for (int i = 0; i < len; i++) {
      AspectValue[] avs = (AspectValue[]) phasedavrs.get(i);
      d[i] = convertToDouble(avs);
    }
    return d;
  }

  /**
   * Return a particular phase of a phased result as an array of doubles.
   * <P> Warning!!! Not all AspectValues can be simply represented as
   * a double. Use of this method with such AspectValues is undefined.
   * @return the i-th phase as double[]
   **/
  public double[] getPhasedResultsFromArray(int i) {
    if (!isPhased()) return null;
    if (i < 0 || i >= phasedavrs.size()) return null;
    return convertToDouble((AspectValue[]) phasedavrs.get(i));
  }
    
  private String[] assureAuxqueries() {
    if (auxqueries == null) {
      auxqueries = new String[AQTYPE_COUNT];
    }
    return auxqueries;
  }

  private void appendAVS(StringBuffer buf, AspectValue[] avs) {
    buf.append('[');
    for (int i = 0; i < avs.length; i++) {
      if (i > 0) buf.append(",");
      buf.append(avs[i]);
    }
    buf.append(']');
  }

  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("AllocationResult[isSuccess=");
    buf.append(isSuccess);
    buf.append(", confrating=");
    buf.append(confrating);
    appendAVS(buf, avResults);
    if (isPhased()) {
      for (int i = 0, n = phasedavrs.size(); i < n; i++) {
        buf.append("Phase ");
        buf.append(i);
        buf.append("=");
        appendAVS(buf, (AspectValue[]) phasedavrs.get(i));
      }
    }
    buf.append("]");
    return buf.toString();
  }


  /*
   * The AspectValues of a failed allocation may be set by the Allocator
   * with values that would be more likely to be successful. 
   * The Expander can use these new values as suggestions when 
   * resetting the Preferences. This method tells which AspectValues
   * have been changed or added by the Allocator.
   *
   * @param prefs the preference from the task corresponding to this allocation
   * @return the aspect values in the allocation that are different
   * from the the preference.
   */
  public AspectValue[] difference(Preference[] prefs) {
    AspectValue[] diffs = new AspectValue[avResults.length];
    int diffCount = 0;

    for (int i=0; i<avResults.length; i++) {
      boolean found = false;
      AspectValue prefAV = null;

      for (int j=0; j<prefs.length; j++) {
	prefAV = prefs[j].getScoringFunction().getBest().getAspectValue();
	if (prefAV.getAspectType() == avResults[i].getAspectType()) {
	  found = true;
	  break;
	}
      }
      if (!found) {
	diffs[diffCount++] = avResults[i];
      } else if (prefAV.getValue() != avResults[i].getValue()) {
	diffs[diffCount++] = avResults[i];
      }
    }
    
    AspectValue[] returnDiff = new AspectValue[diffCount];
    for (int i=0; i<diffCount; i++) {
      returnDiff[i] = diffs[i];
    }

    return returnDiff;
  }

  /** Utility method to help conversion of old code to new usage
   **/
  public static AspectValue[] convertToAVA(int[] types, double[] values) {
    int l = types.length;
    AspectValue[] ava = new AspectValue[l];
    for (int i=0; i<l; i++) {
      ava[i] = AspectValue.newAspectValue(types[i], values[i]);
    }
    return ava;
  }

  private void readObject(ObjectInputStream ois)
    throws IOException, ClassNotFoundException
  {
    ois.defaultReadObject();
    clearMemos();
  }
}

